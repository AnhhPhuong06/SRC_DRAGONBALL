package services.top;

import database.AlyraManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import models.player.Player;
import static services.top.TopManager.AUTO_GIFT_TOPS;
import static services.top.TopManager.TOP_TEMPLATE;
import utils.Logger;

/**
 *
 * @author Ts
 */
public class TopAutoService {

    private static TopAutoService I;
    private final List<Timer> timers = new ArrayList<>();
    private volatile long scheduleGeneration;

    public static TopAutoService gI() {
        if (I == null) {
            I = new TopAutoService();
        }
        return I;
    }

    public void reload() {
        AUTO_GIFT_TOPS.forEach(TOPAUTO::dispose);
        AUTO_GIFT_TOPS.clear();
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement("select * from top_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    TopManager.TopTemplate temp = TopService.gI().getTop(rs.getInt("id"));
                    if (temp != null) {
                        AUTO_GIFT_TOPS.add(new TOPAUTO(rs, temp));
                    }
                } catch (Exception e) {
                    Logger.logException(TopAutoService.class, e);
                }
            }
        } catch (Exception e) {
            Logger.logException(TopAutoService.class, e);
        }
    }

    public TOPAUTO getTop(Object value) {
        for (TOPAUTO top : AUTO_GIFT_TOPS) {
            try {
                int id = Integer.parseInt(value.toString());
                if (top.template.id == id) {
                    return top;
                }
            } catch (NumberFormatException e) {
                if (top.template.name.equals(value)) {
                    return top;
                }
            }
        }
        return null;
    }

    public boolean checkTopRunning() {
        return AUTO_GIFT_TOPS.stream().anyMatch(top -> top.isSend);
    }

    public synchronized void activeAuto() {
        long generation = ++scheduleGeneration;
        timers.forEach(Timer::cancel);
        timers.clear();
        for (TOPAUTO top : new ArrayList<>(AUTO_GIFT_TOPS)) {
            if (top.date == null || top.template == null) continue;
            // Timer LẶP LẠI: chạy tại 'date' rồi retry mỗi 5 phút cho tới khi trao đủ (isSendFullTop -> isDone).
            // Vòng trao idempotent (isNonReceive) nên retry an toàn, KHÔNG double-gift.
            // Nhờ retry: top thủ đang OFFLINE lúc trao lần đầu -> lần sau khi online (hoặc offline vào DB) vẫn nhận đủ.
            Timer timer = new Timer("Auto Send Top " + top.template.name, true);
            timers.add(timer);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Guard rẻ (chưa cần DB): chưa tới ngày / đã trao đủ / chưa bật auto / limit sai / chưa cấu hình thưởng
                        if (generation != scheduleGeneration || !top.isToDate() || top.isDone || !top.isAuto || top.limit <= 0 || top.items == null || top.items.isEmpty()) {
                            return;
                        }
                        // Quá hạn 3 ngày mà chưa trao đủ (vd 1 hạng lỗi DB dai dẳng) -> dừng retry, khỏi spin vô hạn
                        if (top.date != null && System.currentTimeMillis() > top.date.getTime() + 3L * 24 * 60 * 60 * 1000) {
                            Logger.logln(Logger.RED, "AutoTop " + top.template.name + ": quá hạn 3 ngày chưa trao đủ -> dừng retry. Xem log lỗi từng hạng.");
                            this.cancel();
                            return;
                        }
                        try (Connection con = AlyraManager.getConnection()) {
                            // Serialize với loadTop định kỳ của TopService.run() (cùng monitor) -> tránh race trên list tops
                            // (clear+rebuild trong lúc mình duyệt -> IndexOutOfBounds / trao nhầm người / getPlayer null).
                            synchronized (TopService.gI()) {
                                if (generation != scheduleGeneration || top.isDone || !top.isAuto || !top.isToDate()) return;
                                TopService.gI().loadTop(con, top.template); // load bảng xếp hạng MỚI trước khi quyết định
                                if (top.template.tops == null || top.template.tops.isEmpty()) {
                                    return; // bảng rỗng (chưa ai xếp hạng / load lỗi tạm) -> chờ retry, KHÔNG đánh dấu done
                                }
                                top.isSend = true; // đang trao -> chặn hiển thị "đang trao"
                                try {
                                    for (int topId = 0; topId < top.limit && topId < top.template.tops.size(); topId++) {
                                        try {
                                            Player pl = top.template.tops.get(topId).getPlayer();
                                            if (pl != null && top.isNonReceive((int) pl.id)) {
                                                top.addItemAutoGift(con, topId + 1, pl); // tự xử lý online (hòm thư) & offline (ghi DB)
                                            }
                                        } catch (Exception e) {
                                            Logger.logException(getClass(), e);
                                        }
                                    }
                                } finally {
                                    top.isSend = false; // LUÔN tắt cờ dù lỗi/Error -> không kẹt "đang trao" vĩnh viễn
                                }
                                top.update(); // lưu tiến độ; trao đủ -> isDone + xóa users
                            }
                        }
                        if (top.isDone) {
                            this.cancel(); // đã trao đủ -> dừng timer (khỏi wake vô ích mỗi 5 phút)
                        }
                    } catch (Exception e) {
                        top.isSend = false;
                        Logger.logException(getClass(), e, "Lỗi Auto Trao Top " + top.template.name + " ID: " + top.template.id);
                    }
                }
            }, new Date(Math.max(System.currentTimeMillis(), top.date.getTime())), 5 * 60 * 1000L);
            System.err.println("Auto Trao Quà Top " + top.template.name + ": lên lịch tại " + top.date + ", retry mỗi 5 phút tới khi trao đủ.");
        }
    }
}
