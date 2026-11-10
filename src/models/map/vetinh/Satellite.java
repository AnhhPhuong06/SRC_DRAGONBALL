package models.map.vetinh;

import java.io.IOException;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import network.io.Message;
import services.Service;
import services.map.ItemMapService;
import utils.Util;

/**
 *
 * @Creator by Ts
 */
public abstract class Satellite extends ItemMap {

    protected long ownerID;
    protected int clanID;
    protected long lastTimeBuff;
    public long range;

    public Satellite(Zone zone, int itemID, int x, int y, Player player) {
        super(zone, itemID, 1, x, y, player.id);
        this.playerId = -2;
        this.range = 250;
        this.ownerID = player.id;
        this.clanID = player.clan != null ? player.clan.id : -1;
    }
    
    public void sendVeTinh() {
        Message msg = null;
        try {
            msg = new Message(68);
            msg.writer().writeShort(this.itemMapId);
            msg.writer().writeShort(this.itemTemplate.id);
            msg.writer().writeShort(this.x);
            msg.writer().writeShort(this.y);
            msg.writer().writeInt((int) this.playerId);
            msg.writer().writeShort((short) this.range);
            Service.gI().sendMessAllPlayerInMap(zone, msg);
        } catch (IOException exception) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    @Override
    public void update() {
        if (Util.canDoWithTime(createTime, 1800000)) {
            ItemMapService.gI().removeItemMapAndSendClient(this);
        } else {
            if (Util.canDoWithTime(lastTimeBuff, 1000)) {
                lastTimeBuff = System.currentTimeMillis();
                try {
                    if (clanID != -1 && zone.getPlayers().stream().anyMatch(pl -> pl != null && pl.clan != null && pl.clan.id == clanID)) {
                        zone.getPlayers().stream().filter(pl -> pl != null && pl.clan != null && pl.clan.id == clanID).forEach(pl -> buff(pl));
                    } else {
                        buff(zone.getPlayerInMap(ownerID));
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public abstract void buff(Player player);
}
