package services.top;

import java.util.ArrayList;
import java.util.List;
import models.player.Player;

/**
 * Top số lần quay vòng quay may mắn ở Thượng Đế. Số lần quay lưu trong Player.lixi,
 * nằm ở cột dien_sukien phần tử thứ 3 ([diemsukien, phaobong, lixi]).
 */
public class TopQuay {

    /** Số lần quay = phần tử thứ 3 của dien_sukien. */
    private static final String DIEM_SO_LAN_QUAY = "CAST(JSON_EXTRACT(dien_sukien, '$[2]') AS UNSIGNED)";
    private static final int GIOI_HAN = 100;

    private static final TopQuay INSTANCE = new TopQuay();

    private List<Player> list = new ArrayList<>();

    private long lastTimeLoad = 0;

    public static TopQuay getInstance() {
        return INSTANCE;
    }

    public List<Player> getList() {
        return list;
    }

    public void load() {
        load(false);
    }

    public synchronized void load(boolean force) {
        if (!force && System.currentTimeMillis() - lastTimeLoad < 30_000 && !list.isEmpty()) {
            return;
        }
        list = TopPlayerLoader.load(DIEM_SO_LAN_QUAY, GIOI_HAN, (player, diem) -> player.lixi = diem);
        lastTimeLoad = System.currentTimeMillis();
    }
}
