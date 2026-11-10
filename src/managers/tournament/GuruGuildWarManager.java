package managers.tournament;

import consts.ConstNpc;
import models.clan.Clan;
import models.item.Item;
import models.map.Map;
import models.map.Zone;
import models.player.Player;
import models.tournament.guru_guild_war.GuruGuildWarMatch;
import server.Maintenance;
import services.Service;
import services.ItemService;
import services.map.MapService;
import services.player.InventoryService;
import utils.Logger;
import utils.TimeUtil;
import utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quản lý lịch trình, báo danh và các trận đấu Thách Đấu Bang Hội Đảo Guru
 * @author GameDesigner_Ben
 */
public class GuruGuildWarManager implements Runnable {

    public static final int FEE_GOLD_BARS = 50;      // 50 Thỏi Vàng (ID 457)
    public static final int FEE_GEM = 500_000;       // 500.000 Hồng Ngọc

    public static final int STATUS_CLOSED = 0;
    public static final int STATUS_REGISTER = 1;
    public static final int STATUS_PREPARING = 2;
    public static final int STATUS_WAR = 3;

    private static GuruGuildWarManager instance;

    public static GuruGuildWarManager gI() {
        if (instance == null) {
            instance = new GuruGuildWarManager();
        }
        return instance;
    }

    public int status = STATUS_CLOSED;
    public final List<Clan> registeredClans = new java.util.concurrent.CopyOnWriteArrayList<>();
    public final List<GuruGuildWarMatch> activeMatches = new java.util.concurrent.CopyOnWriteArrayList<>();
    private int matchIdCounter = 1;

    private boolean notify1930Sent = false;
    private boolean notify1955Sent = false;
    private boolean notify2000Sent = false;

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                updateSchedule();
                updateMatches();
                Thread.sleep(1000);
            } catch (Exception e) {
                Logger.logException(GuruGuildWarManager.class, e, "Lỗi update GuruGuildWarManager");
            }
        }
    }

    /**
     * Kiểm tra đồng hồ thực tế để chạy theo lịch trình tự động hàng ngày:
     * - 19:30 -> 19:55: Mở báo danh
     * - 19:55 -> 20:00: Ghép cặp
     * - 20:00 -> 20:15: Khai chiến
     */
    private void updateSchedule() {
        int hour = TimeUtil.getCurrHour();
        int min = TimeUtil.getCurrMin();

        // 19:30 - 19:55: Mở báo danh
        if (hour == 19 && min >= 30 && min < 55) {
            if (status != STATUS_REGISTER) {
                openRegistration();
            }
        }
        // 19:55 - 20:00: Chốt sổ và ghép kèo
        else if (hour == 19 && min >= 55) {
            if (status == STATUS_REGISTER) {
                prepareMatches();
            }
        }
        // 20:00 - 20:15: Bắt đầu giao tranh
        else if (hour == 20 && min < 15) {
            if (status == STATUS_PREPARING) {
                startAllMatches();
            }
        }
        // Sau 20:15 hoặc giờ khác: Đóng giải
        else {
            if (status != STATUS_CLOSED && activeMatches.isEmpty()) {
                resetDay();
            }
        }
    }

    private void updateMatches() {
        List<GuruGuildWarMatch> toRemove = new ArrayList<>();
        for (GuruGuildWarMatch match : activeMatches) {
            try {
                match.update();
                if (match.isEnded && match.timeKickOut == 0) {
                    toRemove.add(match);
                }
            } catch (Exception e) {
                Logger.logException(GuruGuildWarManager.class, e, "Lỗi update match Guru");
            }
        }
        activeMatches.removeAll(toRemove);
    }

    public void openRegistration() {
        this.status = STATUS_REGISTER;
        this.registeredClans.clear();
        this.activeMatches.clear();
        Service.gI().sendThongBaoAllPlayer("|1|[ĐẠI CHIẾN ĐẢO GURU]\n"
                + "Cổng báo danh giải đấu Đảo Guru đã mở tại NPC Trọng Tài!\n"
                + "Thời gian báo danh: 19:30 - 19:55.\n"
                + "Lệ phí: 50 Thỏi Vàng + 500k Hồng Ngọc. Bang Chủ/Phó Bang mau tới báo danh!");
    }

    public void prepareMatches() {
        this.status = STATUS_PREPARING;
        if (registeredClans.size() < 2) {
            Service.gI().sendThongBaoAllPlayer("|7|[ĐẠI CHIẾN ĐẢO GURU]\n"
                    + "Không đủ số lượng bang hội báo danh (tối thiểu 2 bang). Giải đấu hôm nay tạm hủy và hoàn phí!");
            // Hoàn phí
            for (Clan c : registeredClans) {
                refundClan(c);
            }
            registeredClans.clear();
            this.status = STATUS_CLOSED;
            return;
        }

        // Xáo trộn ngẫu nhiên để bốc thăm
        List<Clan> pool = new ArrayList<>(registeredClans);
        Collections.shuffle(pool);

        activeMatches.clear();
        Map guruMap = MapService.gI().getMapById(13);

        int zoneIndex = 15; // Phân bổ từ zone 15 trở đi cho các trận đấu Guru War
        while (pool.size() >= 2) {
            Clan c1 = pool.remove(0);
            Clan c2 = pool.remove(0);

            Zone matchZone = null;
            if (guruMap != null && zoneIndex < guruMap.zones.size()) {
                matchZone = guruMap.zones.get(zoneIndex++);
            } else if (guruMap != null && !guruMap.zones.isEmpty()) {
                matchZone = guruMap.zones.get(guruMap.zones.size() - 1);
            }

            if (matchZone != null) {
                GuruGuildWarMatch match = new GuruGuildWarMatch(matchIdCounter++, c1, c2, matchZone);
                activeMatches.add(match);

                String msg = "|1|[ĐẠI CHIẾN GURU] Kèo đấu đã được xếp: [" + c1.name + "] vs [" + c2.name + "]!\n"
                        + "Trận đấu sẽ bùng nổ lúc 20:00 tại Đảo Guru. Hãy sẵn sàng!";
                sendThongBaoClan(c1, msg);
                sendThongBaoClan(c2, msg);
            }
        }

        // Nếu lẻ 1 bang -> hoàn phí
        if (!pool.isEmpty()) {
            Clan odd = pool.remove(0);
            refundClan(odd);
            sendThongBaoClan(odd, "|7|Số lượng bang lẻ, bang của bạn không tìm được đối thủ hôm nay. Đã hoàn lại 100% lệ phí!");
        }

        Service.gI().sendThongBaoAllPlayer("|2|[ĐẠI CHIẾN ĐẢO GURU]\n"
                + "Đã chốt sổ và bốc thăm xong " + activeMatches.size() + " cặp đấu! Đúng 20:00 các chiến thần sẽ xuất trận!");
    }

    public void startAllMatches() {
        this.status = STATUS_WAR;
        for (GuruGuildWarMatch match : activeMatches) {
            try {
                match.startMatch();
            } catch (Exception e) {
                Logger.logException(GuruGuildWarManager.class, e, "Lỗi start match Guru");
            }
        }
        Service.gI().sendThongBaoAllPlayer("|1|[ĐẠI CHIẾN ĐẢO GURU] KHAI CHIẾN!\n"
                + "Toàn bộ thành viên các bang tham chiến đã được dịch chuyển vào Đấu Trường Đảo Guru!");
    }

    public final java.util.Map<Integer, Long> clanRegistrarId = new java.util.concurrent.ConcurrentHashMap<>();

    private void resetDay() {
        this.status = STATUS_CLOSED;
        this.registeredClans.clear();
        this.activeMatches.clear();
        this.clanRegistrarId.clear();
    }

    private void refundClan(Clan clan) {
        if (clan == null) return;
        Player refundTarget = null;

        // 1. Ưu tiên hoàn trực tiếp cho người đã bỏ phí báo danh
        Long registrarId = clanRegistrarId.get(clan.id);
        if (registrarId != null) {
            for (Player p : clan.membersInGame) {
                if (p != null && p.id == registrarId) {
                    refundTarget = p;
                    break;
                }
            }
        }

        // 2. Nếu người báo danh không online, hoàn cho Bang Chủ hoặc Phó Bang Chủ online
        if (refundTarget == null) {
            for (Player p : clan.membersInGame) {
                if (p != null && (clan.isLeader(p) || clan.isDeputy(p))) {
                    refundTarget = p;
                    break;
                }
            }
        }

        if (refundTarget != null) {
            Item tv = ItemService.gI().createNewItem((short) 457, FEE_GOLD_BARS);
            if (!InventoryService.gI().addItemBag(refundTarget, tv)) {
                InventoryService.gI().addItemGift(refundTarget, tv);
                Service.gI().sendThongBao(refundTarget, "Hành trang đầy! 50 Thỏi Vàng hoàn lệ phí đã chuyển vào Rương Phụ.");
            }
            InventoryService.gI().sendItemBags(refundTarget);
            refundTarget.inventory.gem += FEE_GEM;
            Service.gI().sendMoney(refundTarget);
            Service.gI().sendThongBao(refundTarget, "|2|Đã hoàn lại " + FEE_GOLD_BARS + " Thỏi Vàng và " + FEE_GEM + " Hồng Ngọc lệ phí!");
        }
    }

    /**
     * Đăng ký tham gia giải đấu (synchronized để chống race condition trừ trùng phí / duplicate đăng ký)
     */
    public synchronized void register(Player player) {
        if (player.clan == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có bang hội!");
            return;
        }

        Clan clan = player.clan;
        if (!clan.isLeader(player) && !clan.isDeputy(player)) {
            Service.gI().sendThongBao(player, "Chỉ có Bang Chủ hoặc Phó Bang Chủ mới có quyền báo danh!");
            return;
        }

        if (status != STATUS_REGISTER) {
            Service.gI().sendThongBao(player, "Thời gian báo danh là từ 19:30 đến 19:55 hàng ngày!");
            return;
        }

        if (isClanRegistered(clan)) {
            Service.gI().sendThongBao(player, "Bang hội của bạn đã báo danh rồi!");
            return;
        }

        // Kiểm tra phí: 50 Thỏi Vàng + 500k Hồng Ngọc
        if (InventoryService.gI().getNumItemBag(player, 457) < FEE_GOLD_BARS) {
            Service.gI().sendThongBao(player, "Bạn không đủ 50 Thỏi Vàng để báo danh!");
            return;
        }

        if (player.inventory.gem < FEE_GEM) {
            Service.gI().sendThongBao(player, "Bạn không đủ 500.000 Hồng Ngọc để báo danh!");
            return;
        }

        // Trừ phí
        InventoryService.gI().subQuantityItemsBag(player, 457, FEE_GOLD_BARS);
        InventoryService.gI().sendItemBags(player);
        player.inventory.gem -= FEE_GEM;
        Service.gI().sendMoney(player);

        // Lưu người báo danh để hoàn phí chính xác nếu kèo bị lẻ bang
        clanRegistrarId.put(clan.id, player.id);

        // Đưa vào danh sách đăng ký
        registeredClans.add(clan);

        String successMsg = "|1|[BÁO DANH THÀNH CÔNG] " + player.name + " đã đại diện Bang [" + clan.name
                + "] đăng ký tham gia ĐẠI CHIẾN ĐẢO GURU thành công! Hãy tập hợp anh em lúc 19:55!";
        sendThongBaoClan(clan, successMsg);
        Service.gI().sendThongBao(player, "Báo danh thành công! Vui lòng sẵn sàng lúc 19:55.");
    }

    public boolean isClanRegistered(Clan clan) {
        if (clan == null) return false;
        for (Clan c : registeredClans) {
            if (c != null && c.id == clan.id) return true;
        }
        return false;
    }

    public GuruGuildWarMatch getMatchByPlayer(Player player) {
        if (player == null || player.clan == null) return null;
        for (GuruGuildWarMatch match : activeMatches) {
            if (match.clanRed != null && match.clanRed.id == player.clan.id) {
                return match;
            }
            if (match.clanBlue != null && match.clanBlue.id == player.clan.id) {
                return match;
            }
        }
        return null;
    }

    public GuruGuildWarMatch getMatchByZone(Zone zone) {
        if (zone == null) return null;
        for (GuruGuildWarMatch match : activeMatches) {
            if (match.zone != null && match.zone.equals(zone)) {
                return match;
            }
        }
        return null;
    }

    // ==================== CÁC HÀM CHO ADMIN TEST NGAY ====================
    public void adminForceOpenRegister(Player admin) {
        openRegistration();
        Service.gI().sendThongBao(admin, "Admin: Đã mở cổng báo danh Guru War ngay lập tức!");
    }

    public void adminForcePrepare(Player admin) {
        prepareMatches();
        Service.gI().sendThongBao(admin, "Admin: Đã chốt sổ và ghép cặp Guru War!");
    }

    public void adminForceStart(Player admin) {
        startAllMatches();
        Service.gI().sendThongBao(admin, "Admin: Đã bắt đầu tất cả các trận Guru War!");
    }

    public void adminForceEnd(Player admin) {
        for (GuruGuildWarMatch match : activeMatches) {
            match.finishMatch(match.clanRed, match.clanBlue, "ADMIN_FORCE", null);
        }
        Service.gI().sendThongBao(admin, "Admin: Đã kết thúc các trận Guru War!");
    }

    public static void sendThongBaoClan(Clan clan, String text) {
        if (clan == null) return;
        for (Player p : clan.membersInGame) {
            if (p != null) {
                Service.gI().sendThongBao(p, text);
            }
        }
    }
}
