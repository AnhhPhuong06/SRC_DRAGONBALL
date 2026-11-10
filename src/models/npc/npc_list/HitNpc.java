package models.npc.npc_list;

import models.boss.boss_list.HitEvent.HitPBBoss;
import models.map.Map;
import models.map.Zone;
import models.npc.Npc;
import models.player.Player;
import services.Service;
import services.dungeon.HalloweenDungeonService;
import services.map.ChangeMapService;
import services.map.MapService;
import utils.Util;


public class HitNpc extends Npc {

    public static final int NPC_SUMMON_BOSS = 108;
    private static final int MENU_SUMMON = 11010; // NPC 108: gọi boss
    private static final int MENU_ENTER = 11011;  // NPC 110: vào phó bản

    public HitNpc(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    private boolean isSummonNpc() {
        return this.tempId == NPC_SUMMON_BOSS;
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        int done = player.halloweenLevelDone;
        int next = Math.min(done + 1, HitPBBoss.MAX_LEVEL);
        if (isSummonNpc()) {
            long hp = 100_000_000L * next;
            createOtherMenu(player, MENU_SUMMON,
                    "Phó Bản Hit\nCấp đã qua: " + done + "/" + HitPBBoss.MAX_LEVEL
                    + "\nBoss kế tiếp: cấp " + next + " "
                    + "\nGọi boss ra để tiêu diệt, hạ xong sẽ TỰ lên cấp kế tiếp và rơi đồ!",
                    "Gọi boss", "Đóng");
        } else {
            createOtherMenu(player, MENU_ENTER,
                    "Phó Bản Hit\nVào phó bản để khiêu chiến boss Hit, bắt đầu từ cấp " + next + "."
                    + "\nVào trong tìm NPC Heart để gọi boss, hạ xong sẽ tự lên cấp kế tiếp!",
                    "Vào phó bản", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        int menu = player.idMark.getIndexMenu();
        if (menu == MENU_SUMMON && select == 0) {
            HitPBBoss.summon(player);
        } else if (menu == MENU_ENTER && select == 0) {
            enterDungeon(player);
        }
    }

    private void enterDungeon(Player player) {
        Map map = MapService.gI().getMapById(HalloweenDungeonService.MAP_ID);
        if (map == null || map.zones == null || map.zones.isEmpty()) {
            Service.gI().sendThongBao(player, "Phó bản Hit chưa sẵn sàng, thử lại sau!");
            return;
        }
        Zone zone = null;
        for (Zone z : map.zones) {
            if (z != null && z.getNumOfPlayers() == 0) {
                zone = z;
                break;
            }
        }
        if (zone == null) {
            Service.gI().sendThongBao(player, "Tất cả khu Phó Bản Hit đang có người, vui lòng thử lại sau!");
            return;
        }
        ChangeMapService.gI().changeMap(player, zone, Util.nextInt(400, 800), 300);
        Service.gI().sendThongBao(player, "Đã vào Phó Bản Hit! Tìm NPC Heart để gọi boss.");
    }
}
