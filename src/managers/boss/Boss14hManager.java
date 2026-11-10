package managers.boss;

import models.boss.Boss;
import network.io.Message;
import models.player.Player;
import server.Maintenance;
import services.map.MapService;
import utils.Functions;
import utils.Logger;

public class Boss14hManager extends BossManager {

    private static Boss14hManager instance;

    public static Boss14hManager gI() {
        if (instance == null) {
            instance = new Boss14hManager();
        }
        return instance;
    }
    
    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();
                for (int i = this.bosses.size() - 1; i >= 0; i--) {
                    if (i < this.bosses.size()) {
                        Boss boss = this.bosses.get(i);
                        try {
                            boss.update();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - st), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final int MENU_TYPE = 8;

    public void showListBoss14h(Player player) {
        if (player == null || !player.isAdmin()) {
            return;
        }
        player.idMark.setMenuType(MENU_TYPE);

        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Danh sÃ¡ch Boss 14h");
            var visibleBosses = bosses.stream()
                    .filter(b -> b != null && b.data != null && b.data[0] != null
                            && !MapService.gI().isMapBlackBallWar(b.data[0].getMapJoin()[0]))
                    .toList();

            msg.writer().writeByte(visibleBosses.size());

            for (Boss boss : visibleBosses) {
                var data = boss.data[0];
                msg.writer().writeInt((int) boss.id);
                msg.writer().writeInt((int) boss.id);
                msg.writer().writeShort(data.getOutfit()[0]);
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(data.getOutfit()[1]);
                msg.writer().writeShort(data.getOutfit()[2]);
                msg.writer().writeUTF(data.getName());
                msg.writer().writeUTF(boss.bossStatus.toString());
                msg.writer().writeUTF(
                        boss.zone != null
                                ? boss.zone.map.mapName + " (" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId
                                : "KhÃ´ng á»Ÿ map nÃ o");
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(Boss14hManager.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }
}
