package models.map.vetinh;

import models.map.Zone;
import models.player.Player;
import services.player.PlayerService;
import utils.Util;

/**
 *
 * @Creator by Ts
 */
public class SatelliteHp extends Satellite {

    public SatelliteHp(Zone zone, int itemID, int x, int y, Player player) {
        super(zone, itemID, x, y, player);
    }

    @Override
    public void buff(Player pl) {
        if (Util.getDistance(pl.location.x, pl.location.y, x, y) <= range) {
            PlayerService.gI().hoiPhuc(pl, pl.nPoint.hpMax * 5 / 100, 0);
        }
    }
}
