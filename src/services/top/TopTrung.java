package services.top;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import models.player.Player;

/**
 *
 * @author Administrator
 */
public class TopTrung {

    private static final String DIEM = "CAST(JSON_EXTRACT(point_pokemon, '$[0]') AS UNSIGNED)";
    private static final int GIOI_HAN = 100;

    @Getter
    private List<Player> list = new ArrayList<>();
    private static final TopTrung INSTANCE = new TopTrung();

    private long lastTimeLoad = 0;

    public static TopTrung getInstance() {
        return INSTANCE;
    }

    public void load() {
        load(false);
    }

    public synchronized void load(boolean force) {
        if (!force && System.currentTimeMillis() - lastTimeLoad < 30_000 && !list.isEmpty()) {
            return;
        }
        list = TopPlayerLoader.load(DIEM, GIOI_HAN, (player, diem) -> player.point_pokemon = diem);
        lastTimeLoad = System.currentTimeMillis();
    }
}
