package models.matches.pvp;

 
import models.matches.PVP;
import models.matches.TYPE_LOSE_PVP;
import models.matches.TYPE_PVP;
import models.player.Enemy;
import models.player.Player;
import services.Service;
import services.map.ChangeMapService;
import services.map.MapService;
import utils.Util;

public class TraThu extends PVP {

    public TraThu(Player p1, Player p2) {
        super(TYPE_PVP.TRA_THU, p1, p2);
    }

    @Override
    public void start() {
//        if (!p1.zone.equals(p2.zone)) {
//            int mapId = p2.zone.map.mapId;
//            if (MapService.gI().isMapPhoBan(mapId) || ChangeMapService.gI().checkMapCanJoin(p1, p2.zone) == null || ChangeMapService.gI().checkMapCanJoinByYardart(p1, p2.zone) == null) {
//                Service.gI().sendThongBao(p1, "Không thể thực hiện.");
//                return;
//            }
//            p1.changeMapVIP = false;
//            ChangeMapService.gI().changeMap(p1,
//                    p2.zone,
//                    p2.location.x + Util.nextInt(-5, 5), p2.location.y);
//        }
//        Service.gI().sendThongBao(p2, "Có người đang đến tìm bạn để trả thù");
//        Service.gI().chat(p1, "Mày tới số rồi con ạ!");
//        new Thread(() -> {
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//            }
//            super.start();
//        }).start();
    }

    @Override
    public void finish() {

    }

    @Override
    public void update() {

    }

    @Override
    public void reward(Player plWin) {

    }

    @Override
    public void sendResult(Player plLose, TYPE_LOSE_PVP typeLose) {
        if (typeLose == TYPE_LOSE_PVP.RUNS_AWAY) {
            Service.gI().sendThongBao(p1.equals(plLose) ? p1 : p2, "Bạn bị xử thua vì đã bỏ chạy");
        }
        if (typeLose == TYPE_LOSE_PVP.DEAD) {
            if (p2.equals(plLose)) {
                for (Enemy pl : p1.enemies) {
                    if (pl.id == p2.id) {
                        p1.enemies.remove(pl);
                        break;
                    }
                }
            }
        }
    }

}
