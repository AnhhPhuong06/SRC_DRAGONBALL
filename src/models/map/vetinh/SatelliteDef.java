package models.map.vetinh;

import models.map.Zone;
import models.player.Player;
import utils.Util;

/**
 *
 * @Creator by Ts
 */
public class SatelliteDef extends Satellite {

    public SatelliteDef(Zone zone, int itemID, int x, int y, Player player) {
        super(zone, itemID, x, y, player);
    }

    @Override
    public void buff(Player pl) {
        if (pl.nPoint != null) {
            pl.nPoint.buffDefenseSatellite = Util.getDistance(pl.location.x, pl.location.y, x, y) <= range;
        }
    }

}
