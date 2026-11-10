package services.dungeon;

import models.dungeon.HalloweenDungeon;
import models.clan.Clan;
import models.item.Item;
import models.map.Map;
import models.map.Zone;
import models.player.Player;
import services.ItemTimeService;
import services.Service;
import services.map.ChangeMapService;
import services.map.MapService;
import services.player.InventoryService;
import utils.Util;

public class HalloweenDungeonService {

    public static final int MAP_ID = 181;
    public static final int FEE_GOLD_BAR = 50; // phí vào phó bản: 50 thỏi vàng/lần
    public static final long POWER_REQUIRED = 20_000_000_000L; // yêu cầu 20 tỷ sức mạnh

    private static HalloweenDungeonService instance;

    public static HalloweenDungeonService gI() {
        if (instance == null) {
            instance = new HalloweenDungeonService();
        }
        return instance;
    }

    private HalloweenDungeon[] slots;

    public synchronized void open(Player player) {
        Clan clan = player.clan;
        if (clan == null) {
            Service.gI().sendThongBao(player, "Bạn cần có bang hội để vào phó bản Halloween!");
            return;
        }
        if (player.nPoint.power < POWER_REQUIRED) {
            Service.gI().sendThongBao(player, "Cần 20 tỷ sức mạnh để vào phó bản Halloween!");
            return;
        }
        if (clan.halloweenDungeon != null && clan.halloweenDungeon.isOpened) {
            if (!chargeFee(player)) {
                return;
            }
            teleportIn(player, clan.halloweenDungeon.zone);
            return;
        }
        if (!clan.isLeader(player)) {
            Service.gI().sendThongBao(player, "Chỉ bang chủ mới mở được phó bản Halloween!");
            return;
        }
        // Giới hạn 1 lần/ngày/clan
        if (!Util.isAfterMidnight(clan.lastTimeOpenHalloween)) {
            Service.gI().sendThongBao(player, "Bang bạn đã vào phó bản Halloween hôm nay rồi, hẹn ngày mai!");
            return;
        }
        Map map = MapService.gI().getMapById(MAP_ID);
        if (map == null || map.zones == null || map.zones.isEmpty()) {
            Service.gI().sendThongBao(player, "Chưa cấu hình map " + MAP_ID + "!");
            return;
        }
        if (slots == null) {
            slots = new HalloweenDungeon[map.zones.size()];
        }
        int freeIdx = -1;
        for (int i = 0; i < slots.length && i < map.zones.size(); i++) {
            if (slots[i] == null || !slots[i].isOpened) {
                freeIdx = i;
                break;
            }
        }
        if (freeIdx < 0) {
            Service.gI().sendThongBao(player, "Phó bản Halloween đang đầy, vui lòng thử lại sau!");
            return;
        }
        if (!chargeFee(player)) {
            return;
        }
        Zone zone = map.zones.get(freeIdx);
        HalloweenDungeon dungeon = new HalloweenDungeon(freeIdx);
        slots[freeIdx] = dungeon;
        dungeon.open(player, clan, zone);
        for (Player pl : clan.membersInGame) {
            if (pl != null && pl.nPoint.power >= POWER_REQUIRED) {
                teleportIn(pl, zone);
            }
        }
    }

    private boolean chargeFee(Player player) {
        if (InventoryService.gI().getNumItemBag(player, 457) < FEE_GOLD_BAR) {
            Service.gI().sendThongBao(player, "Bạn cần " + FEE_GOLD_BAR + " thỏi vàng để vào phó bản Halloween!");
            return false;
        }
        InventoryService.gI().subQuantityItemsBag(player, 457, FEE_GOLD_BAR);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Đã trừ " + FEE_GOLD_BAR + " thỏi vàng phí vào phó bản Halloween!");
        return true;
    }

    private void teleportIn(Player pl, Zone zone) {
        if (zone == null) {
            return;
        }
        try {
            ChangeMapService.gI().changeMap(pl, zone, Util.nextInt(300, 900), 300);
            models.task.Task_Kol.tick(pl, 1, 35); // KOL 35 (đi phó bản Halloween 10 lần)
            ItemTimeService.gI().sendTextHalloween(pl);
        } catch (Exception e) {
        }
    }
}
