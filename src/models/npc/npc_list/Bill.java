package models.npc.npc_list;

import consts.ConstNpc;
import models.npc.Npc;
import models.player.Player;
import services.map.NpcService;
import services.map.ChangeMapService;
import services.ShopService;
import services.TaskService;
import services.player.InventoryService;

/**
 *
 * @ByDevMinhDung LH : 0348794313
 * @Link Box :
 */
public class Bill extends Npc {

    public Bill(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            TaskService.gI().checkDoneTaskTalkNpc(player, this);
            if (mapId == 154) {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "...",
                        "Về\nthánh địa\nKaio", "Từ chối");
            } else {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chưa tới giờ thi đấu, xem hướng dẫn để biết thêm chi tiết",
                        "Nói\nchuyện", "Hướng\ndẫn\nthêm", "Từ chối");

            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 48 -> {
                    switch (player.idMark.getIndexMenu()) {
                        case ConstNpc.BASE_MENU -> {
                            switch (select) {
                                case 0 -> {
                                    if (InventoryService.gI().canOpenBillShop(player)) {
                                        createOtherMenu(player, 2,
                                                "Đói bụng quá...ngươi mang cho ta 99 phần đồ ăn\nta sẽ cho một món đồ Hủy Diệt.\nNếu tâm trạng ta vui ngươi có thể nhận trang bị tăng đến 15%",
                                                "OK", "Từ chối");
                                    } else {
                                        createOtherMenu(player, 2,
                                                "Ngươi cần đạt 40 tỷ sức mạnh\nTrang bị đủ bộ 5 món trang bị Thần linh trên người\nvà mang 99 phần đồ ăn tới đây...\nrồi ta nói chuyện tiếp.",
                                                "OK");
                                    }
                                }
                                case 1 ->
                                    NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_BILL);
//                                case 2 -> {
//                                    createOtherMenu(player, 5, "Vào hành trang, chọn loại thức ăn muốn đổi rồi nhấn sử dụng.", "OK", "Từ Chối");
//                                }
//                                case 3 -> {
//                                    ShopService.gI().opendShop(player, "THUC_AN", false);
//                                }
                            }
                        }
                        case 2 -> {
                            if (select == 0 && InventoryService.gI().canOpenBillShop(player) && player.nPoint.power >= 40_000_000_000L) {
                                ShopService.gI().opendShop(player, "BILL", true);
                            }
                        }
                    }
                }
                case 154 -> {
                    if (select == 0) {
                        ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                    }
                }
            }
        }
    }
}
