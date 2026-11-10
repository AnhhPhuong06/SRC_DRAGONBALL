package services.top;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import models.player.Player;

/**
 *
 * @author Administrator
 */
public class TopDauBuoi {

    private static final String DIEM = "CAST(JSON_EXTRACT(daubuoi, '$[0]') AS UNSIGNED)";
    private static final int GIOI_HAN = 100;

    @Getter
    private List<Player> list = new ArrayList<>();
    private static final TopDauBuoi INSTANCE = new TopDauBuoi();

    private long lastTimeLoad = 0;

    public static TopDauBuoi getInstance() {
        return INSTANCE;
    }

    public void load() {
        load(false);
    }

    public synchronized void load(boolean force) {
        if (!force && System.currentTimeMillis() - lastTimeLoad < 30_000 && !list.isEmpty()) {
            return;
        }
        list = TopPlayerLoader.load(DIEM, GIOI_HAN, (player, diem) -> player.daubuoi = diem);
        lastTimeLoad = System.currentTimeMillis();
    }
}
