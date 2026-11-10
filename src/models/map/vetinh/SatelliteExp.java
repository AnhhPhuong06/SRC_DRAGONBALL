package models.map.vetinh;

import models.map.Zone;
import models.player.Player;
import utils.Util;

/**
 *
 * @Creator by Ts
 */
public class SatelliteExp extends Satellite {

    public SatelliteExp(Zone zone, int itemID, int x, int y, Player player) {
        super(zone, itemID, x, y, player);
    }

    @Override
    public void buff(Player pl) {
        if (pl.nPoint != null) {
            pl.nPoint.buffExpSatellite = Util.getDistance(pl.location.x, pl.location.y, x, y) <= range;
        }
    }

}
