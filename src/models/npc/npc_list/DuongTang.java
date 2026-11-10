package models.npc.npc_list;


import consts.ConstItem;
import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import models.player.Inventory;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.ShopService;
import services.map.ChangeMapService;
import services.map.MapService;
import services.player.InventoryService;
import services.player.PlayerService;
import utils.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DuongTang extends Npc {

    private static final long TIME_HO_TONG = 5 * 60 * 1000L;
    private static final int MAX_DISTANCE_X = 500;
    private static final int MAX_DISTANCE_Y = 300;
    private static final long GRACE_FAR_AWAY = 3000L;
    private static final long MIN_TIME_IN_MAP = 3000L;
    private static final int[] MAP_TRAI_DAT = {1, 2, 3, 4, 6, 29, 30, 28, 27};
    private static final int[] MAP_NAMEC   = {8, 9, 10, 11, 12, 13, 33, 34, 32, 31};
    private static final int[] MAP_XAY_DA  = {15, 16, 17, 18, 19, 20, 37, 36, 35, 52};
    private static final String SHOP_TAG = "DUONG_TANG_SHOP";
    private static final Map<Long, long[]> DANG_DAT_DUONG = new ConcurrentHashMap<>();
    private static final Map<Long, models.npc.BotDuongTang> BOT_FOLLOWERS = new ConcurrentHashMap<>();
    private static final Map<Long, Long> COOLDOWN = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1 * 60 * 1000L;

    public DuongTang(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 0 -> menuMap0(player);
                case 123 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Ra khỏi ngôi làng này sẽ gặp ngọn núi ngũ hành sơn",
                        "Về\nLàng Aru", "Đóng");
                case 122 -> menuMap122(player);
                default -> super.openBaseMenu(player);
            }
        }
    }

    private void menuMap0(Player player) {
        long now = System.currentTimeMillis();
        long diem = getDiem(player);

        long[] datDuong = DANG_DAT_DUONG.get(player.id);
        if (datDuong != null && (now - datDuong[0]) < TIME_HO_TONG) {
            long left = (TIME_HO_TONG - (now - datDuong[0])) / 1000;
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Thí chủ đang hộ tống bần tăng tới " + tenMap((int) datDuong[1]) + ".\n"
                            + "Còn " + left + " giây!",
                    "Đóng");
            return;
        }
        if (datDuong != null) DANG_DAT_DUONG.remove(player.id);

        String text = "A mi phò phò! Thí chủ có " + diem + " điểm sự kiện.";
        Long lastFinish = COOLDOWN.get(player.id);
        long leftCd = (lastFinish != null) ? (COOLDOWN_MS - (now - lastFinish)) : 0;
        if (leftCd > 0) {
            text += "\nBần tăng đang nghỉ, còn " + (leftCd / 1000) + " giây mới hộ tống được.";
        } else {
            text += "\n- Hộ tống bần tăng: +1 điểm mỗi lần thành công";
        }
        text += "\n- Đổi điểm sự kiện lấy quà";

        createOtherMenu(player, ConstNpc.BASE_MENU, text,
                "Hộ Tống\nĐường Tăng", "Đổi điểm\nsự kiện", "Từ chối");
    }

    private void menuMap122(Player player) {
        createOtherMenu(player, ConstNpc.BASE_MENU,
                "|0|A mi phò phò, thí chủ hãy thu thập bùa 'giải khai phong ấn'\nmỗi chữ 10 cái\n",
                "Gải\nphong ấn", "Về\nLàng Aru");
    }

    private void openShopDoiDiem(Player player) {
        ShopService.gI().opendShop(player, SHOP_TAG, false);
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;
        if (player.idMark.isBaseMenu()) {
            switch (mapId) {
                case 0 -> confirmMap0(player, select);
                case 123 -> {
                    if (select == 0) {
                        ChangeMapService.gI().changeMapNonSpaceship(player, 0, Util.nextInt(700, 800), 432);
                    }
                }
                case 122 -> confirmMap122(player, select);
            }
        }
    }

    private void confirmMap0(Player player, int select) {
        long now = System.currentTimeMillis();
        long[] datDuong = DANG_DAT_DUONG.get(player.id);
        if (datDuong != null && (now - datDuong[0]) < TIME_HO_TONG) return; // đang hộ tống dở
        switch (select) {
            case 0 -> { // Hộ Tống Đường Tăng
                Long lastFinish = COOLDOWN.get(player.id);
                if (lastFinish != null && (now - lastFinish) < COOLDOWN_MS) {
                    long leftSec = (COOLDOWN_MS - (now - lastFinish)) / 1000;
                    Service.gI().sendThongBao(player,
                            "Bần tăng đang nghỉ ngơi, còn " + leftSec + " giây nữa!");
                    return;
                }
                int curMap = (player.zone != null && player.zone.map != null) ? player.zone.map.mapId : -1;
                int targetMap = randomMapDich(curMap);
                // state: [0]=startTime, [1]=targetMapId, [2]=lastNearTime,
                //        [3]=enterCurMapTime, [4]=(unused), [5]=lastKnownMapId
                DANG_DAT_DUONG.put(player.id, new long[]{now, targetMap, now, now, 0, curMap});
                spawnBotFollower(player);
                Service.gI().sendThongBao(player,
                        "Bần tăng cần đến " + tenMap(targetMap) + " — hãy hộ tống ta tới trong 5 phút!");
            }
            case 1 -> openShopDoiDiem(player); // Mở shop UI đổi điểm
            // 2 = Từ chối → không làm gì
        }
    }

    private void confirmMap122(Player player, int select) {
        // ===== Code gốc: quest thu bùa đổi cải trang Tôn Ngộ Không =====
        if (select == 0) {
            Item chukhai = InventoryService.gI().findItemBag(player, ConstItem.CHU_KHAI);
            Item chugiai = InventoryService.gI().findItemBag(player, ConstItem.CHU_GIAI);
            Item chuan = InventoryService.gI().findItemBag(player, ConstItem.CHU_AN);
            Item chuphong = InventoryService.gI().findItemBag(player, ConstItem.CHU_PHONG);

            if (chukhai != null && chukhai.quantity >= 10 || chugiai != null && chugiai.quantity >= 10
                    || chuan != null && chuan.quantity >= 10 || chuphong != null && chuphong.quantity >= 10) {
                if (chukhai != null) InventoryService.gI().subQuantityItemsBag(player, chukhai, 10);
                if (chugiai != null) InventoryService.gI().subQuantityItemsBag(player, chugiai, 10);
                if (chuan != null) InventoryService.gI().subQuantityItemsBag(player, chuan, 10);
                if (chuphong != null) InventoryService.gI().subQuantityItemsBag(player, chuphong, 10);
                Item selectedItem = null;
                switch (player.gender) {
                    case 0: selectedItem = ItemService.gI().createNewItem((short) ConstItem.CAI_TRANG_TON_NGO_KHONG); break;
                    case 1: selectedItem = ItemService.gI().createNewItem((short) ConstItem.CAI_TRANG_TON_NGO_KHONG_545); break;
                    case 2: selectedItem = ItemService.gI().createNewItem((short) ConstItem.CAI_TRANG_TON_NGO_KHONG_546); break;
                    default: return;
                }
                selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(25, 30)));
                selectedItem.itemOptions.add(new Item.ItemOption(94, Util.nextInt(10, 20)));
                selectedItem.itemOptions.add(new Item.ItemOption(77, Util.nextInt(25, 30)));
                selectedItem.itemOptions.add(new Item.ItemOption(103, Util.nextInt(25, 30)));
                selectedItem.itemOptions.add(new Item.ItemOption(101, Util.nextInt(30, 60)));
                selectedItem.itemOptions.add(new Item.ItemOption(114, Util.nextInt(50, 100)));
                selectedItem.itemOptions.add(new Item.ItemOption(106, 0));
                selectedItem.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 7)));
                selectedItem.itemOptions.add(new Item.ItemOption(174, 2025));
                InventoryService.gI().addItemBag(player, selectedItem);
                InventoryService.gI().sendItemBags(player);
                this.npcChat(player.zone,
                        "A mi phò phò, đa tạ thí chủ tương trợ, xin hãy nhận món quà mọn này, bần tăng sẽ niệm chú giải thoát cho Ngộ Không");
                Service.gI().sendThongBao(player, "Bạn nhận được: " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(player, "Cần đủ 4 loại chữ!");
            }
            return;
        }
        if (select == 1) {
            ChangeMapService.gI().changeMapNonSpaceship(player, 0, Util.nextInt(700, 800), 432);
        }
    }
    public static boolean isDangDatDuong(long playerId) {
        long[] state = DANG_DAT_DUONG.get(playerId);
        return state != null && (System.currentTimeMillis() - state[0]) < TIME_HO_TONG;
    }

    public static void tickCheckArrive(Player player) {
        try {
            if (player == null || player.zone == null || player.zone.map == null) return;
            long[] state = DANG_DAT_DUONG.get(player.id);
            if (state == null) return;
            long now = System.currentTimeMillis();

            // Hết giờ
            if (now - state[0] >= TIME_HO_TONG) {
                huy(player, "đã hết giờ hộ tống Đường Tăng");
                return;
            }

            int curMapId = player.zone.map.mapId;
            
            if (services.map.MapService.gI().isHome(curMapId)) {
                huy(player, "bần tăng không vào nhà ngươi được");
                return;
            }
            if (state[5] != curMapId) {
                long timeInPrevMap = now - state[3];
                state[3] = now;
                state[5] = curMapId;
                if (timeInPrevMap < MIN_TIME_IN_MAP) {
                    huy(player, "chuyển map quá nhanh — nghi dùng mod tele");
                    return;
                }
            }
            // Tới đích
            if (curMapId == (int) state[1]) {
                DANG_DAT_DUONG.remove(player.id);
                disposeBotFollower(player.id);
                COOLDOWN.put(player.id, now); // bắt đầu cooldown 5 phút cho lần hộ tống tiếp theo
                traQuaDatDuong(player);
                return;
            }

            models.npc.BotDuongTang bot = BOT_FOLLOWERS.get(player.id);
            boolean gan;
            if (bot == null || bot.zone == null) {
                gan = true;
            } else if (bot.zone != player.zone) {
                gan = false;
            } else {
                int dx = Math.abs(bot.location.x - player.location.x);
                int dy = Math.abs(bot.location.y - player.location.y);
                gan = dx < MAX_DISTANCE_X && dy < MAX_DISTANCE_Y;
            }
            if (gan) {
                state[2] = now;
            } else if (now - state[2] > GRACE_FAR_AWAY) {
                huy(player, "đi quá xa khỏi bần tăng (không được tele bằng mod)");
            }
        } catch (Exception ignored) {
        }
    }

    public static void cancelIfDatDuong(Player player, String reason) {
    }

    private static void huy(Player player, String reason) {
        DANG_DAT_DUONG.remove(player.id);
        disposeBotFollower(player.id);
        COOLDOWN.put(player.id, System.currentTimeMillis()); // tính cooldown cả khi huỷ để chống spam
        try { Service.gI().sendThongBao(player, "A mi phò phò... " + reason + "!"); } catch (Exception ignored) {}
    }
    private static void traQuaDatDuong(Player player) {
        addDiem(player, 1);
        Service.gI().sendThongBao(player, "A mi phò phò! Nhận: +1 điểm sự kiện");
    }

    private static void addDiem(Player player, int delta) {
        try {
            player.inventory.diemDT = Math.max(0, player.inventory.diemDT + delta);
            if (player.inventory.diemDT >= 100) { // KOL 28 (có 100 điểm Đường Tăng)
                models.task.Task_Kol.complete(player, 28);
            }
        } catch (Exception ignored) {
        }
    }

    private static long getDiem(Player player) {
        try {
            if (player.inventory == null) {
                return 0;
            }
            return player.inventory.diemDT;
        } catch (Exception e) {
            return 0;
        }
    }

    private static void spawnBotFollower(Player player) {
        if (player == null || player.zone == null) return;
        try {
            models.npc.BotDuongTang old = BOT_FOLLOWERS.remove(player.id);
            if (old != null) old.dispose();
            models.npc.BotDuongTang bot = new models.npc.BotDuongTang(player);
            bot.spawnAt(player.zone);
            BOT_FOLLOWERS.put(player.id, bot);
        } catch (Exception ignored) {
        }
    }

    public static void donKhiThoatGame(long playerId) {
        try {
            DANG_DAT_DUONG.remove(playerId);
            disposeBotFollower(playerId);
        } catch (Exception ignored) {
        }
    }
    private static void disposeBotFollower(long playerId) {
        models.npc.BotDuongTang bot = BOT_FOLLOWERS.remove(playerId);
        if (bot != null) {
            try { bot.dispose(); } catch (Exception ignored) {}
        }
    }

    private int randomMapDich(int excludeMapId) {
        int[][] pools = {MAP_TRAI_DAT, MAP_NAMEC, MAP_XAY_DA};
        for (int tries = 0; tries < 20; tries++) {
            int[] pool = pools[Util.nextInt(0, 2)];
            int m = pool[Util.nextInt(0, pool.length - 1)];
            if (m != excludeMapId && m != 0) return m;
        }
        return MAP_TRAI_DAT[0];
    }

    private static String tenMap(int mapId) {
        try {
            models.map.Map m = MapService.gI().getMapById(mapId);
            if (m != null && m.mapName != null && !m.mapName.isEmpty()) {
                return "\"" + m.mapName + "\"";
            }
        } catch (Exception ignored) {
        }
        return "map " + mapId;
    }
}
