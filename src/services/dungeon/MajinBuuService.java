package services.dungeon;

/*
 * @Author: 0789305260
 *
 */
import consts.BossType;
import consts.ConstNpc;
import models.player.Player;
import services.map.MapService;
import services.Service;
import services.map.ChangeMapService;
import utils.Util;

import java.util.List;
import managers.boss.Boss12hManager;
import managers.boss.BossManager;
import managers.boss.TrungThuEventManager;
import models.map.Zone;
import models.npc.Npc;
import services.map.NpcService;
import utils.TimeUtil;

public class MajinBuuService {

    public static byte HOUR_OPEN_MAP_MABU = 12;

    public static final int AVAILABLE = 7;

    private static MajinBuuService instance;

    public static MajinBuuService gI() {
        if (instance == null) {
            instance = new MajinBuuService();
        }
        return instance;
    }

    public Npc getNpcOsin(Player player) {
        for (Npc npc : player.zone.map.npcs) {
            if (npc.tempId == 44) {
                return npc;
            }
        }
        return null;
    }

    public Npc getNpcBabiday(Player player) {
        for (Npc npc : player.zone.map.npcs) {
            if (npc.tempId == 46) {
                return npc;
            }
        }
        return null;
    }

    public Npc getNpcBabiday(Zone zone) {
        for (Npc npc : zone.map.npcs) {
            if (npc.tempId == 46) {
                return npc;
            }
        }
        return null;
    }

    public void joinMapMabu(Player player) {
        boolean changed = false;
        if (player.clan != null) {
            List<Player> players = player.zone.getPlayers();
            for (Player pl : players) {
                if (pl.clan != null && !player.equals(pl) && player.clan.equals(pl.clan) && !player.isBoss) {
                    Service.gI().changeFlag(player, Util.nextInt(9, 10));
                    changed = true;
                    break;
                }
            }
        }
        if (!changed && !player.isBoss) {
            Service.gI().changeFlag(player, Util.nextInt(9, 10));
        }
    }

    public void xuongTangDuoi(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_XUONG_TANG_DUOI, player.cFlag == 9 ? 4390 : 4388, "Mau Ä‘i vá»›i ta xuá»‘ng táº§ng tiáº¿p theo", "OK");
    }

    public void goHome(Player player) {
        if (player.goHome && Util.canDoWithTime(player.lastUpdateGohomeTime, 3000)) {
            if (player.timeGohome == 30) {
                NpcService.gI().createMenuConMeo(player, -1, 4390, "Tráº­n chiáº¿n Ä‘Ã£ káº¿t thÃºc, chÃºng ta pháº£i rá»i khá»i Ä‘Ã¢y ngay", "OK");
            }
            if (player.timeGohome > 0) {
                Service.gI().sendThongBao(player, "Vá» nhÃ  sau " + player.timeGohome + " giÃ¢y ná»¯a");
            }
            player.timeGohome -= 3;
            if (player.timeGohome <= 0) {
                ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                player.goHome = false;
            }
            player.lastUpdateGohomeTime = System.currentTimeMillis();
        }
    }

    public void update(Player player) {
        if (player.zone != null && player.isPl() && MapService.gI().isMapMaBu(player.zone.map.mapId)) {
            try {
                goHome(player);
                if (!TimeUtil.isMabuOpen()) {
                    if (!player.goHome && !player.isAdmin()) {
                        player.goHome = true;
                        player.timeGohome = 30;
                    }
                    return;
                }
                if (Util.isTrue(1, 100) && player.cFlag == 9) {
                    getNpcBabiday(player).npcChat(player.zone, "Ãšm bala xÃ¬ bÃ¹a " + player.name);
                    Service.gI().sendThongBao(player, "Báº¡n Ä‘Ã£ bá»‹ BabiÄ‘Ã¢y thÃ´i miÃªn");
                    Service.gI().changeFlag(player, 10);
                }
                if (Util.isTrue(1, 50) && player.cFlag == 10) {
                    getNpcOsin(player).npcChat(player.zone, "Ãšm bala xÃ¬ bÃ¹a hÃ³a giáº£i cho " + player.name);
                    Service.gI().sendThongBao(player, "Báº¡n Ä‘Ã£ Ä‘Æ°á»£c Ã”sin giáº£i bÃ¹a mÃª");
                    Service.gI().changeFlag(player, 9);
                }
            } catch (Exception ignored) {
            }
        }
    }

}

