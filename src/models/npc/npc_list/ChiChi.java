package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import server.Manager;
import services.Service;
import services.ShopService;
import services.player.InventoryService;
import utils.Util;

public class ChiChi extends Npc {

    public ChiChi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào ! ChiChi đang độc quyền shop sự kiện bạn muốn xem gì ?",
                    "Cửa hàng\n8/3", "Cửa hàng\n10/3","Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 5 || this.mapId == 13 || this.mapId == 20) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0 ->
                            ShopService.gI().opendShop(player, "SU_KIEN8/3", false);
                        case 1 ->
                            ShopService.gI().opendShop(player, "SU_KIEN_10/3", false);
                    }
                } else if (player.idMark.getIndexMenu() == 20071) {
                    switch (select) {
                        case 0:
//                            Service.gI().showListTop(player, Manager.topsk);
                        default:
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == 2007) {
                    switch (select) {
                        case 0:
                            Item hopquachinchu = InventoryService.gI().findItemBagByTemp(player, 1315);
                            if (hopquachinchu != null && hopquachinchu.quantity < 1) {
                                this.npcChat(player, "Bạn còn thiếu x" + (1 - hopquachinchu.quantity) + " Hộp quà chỉn chu.");
                            } else if (hopquachinchu == null) {
                                this.npcChat(player, "Bạn không có Hộp quà chỉn chu nào.");
                            } else {
                                short[] sk83 = {1150, 1151, 1152, 1153, 1154};
                                short[] sk83_1 = {1316};

                                short itemId;
                                int quantity;

                                if (Util.isTrue(50, 100)) {
                                    itemId = sk83[Util.nextInt(sk83.length)];
                                    quantity = Util.nextInt(1, 5);
                                } else {
                                    itemId = sk83_1[Util.nextInt(sk83_1.length)];
                                    quantity = 1;
                                }

                                Item sukien = Util.hopquachinchu(itemId);

                                InventoryService.gI().subQuantityItemsBag(player, hopquachinchu, 1);

                                sukien.quantity = quantity;
                                InventoryService.gI().addItemBag(player, sukien);
                                player.diemsukien += 1;
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã nhận được " + quantity + "x " + sukien.template.name);
                            }
                            break;
                        case 1: {
                            Item hopquanhenhang = InventoryService.gI().findItemBagByTemp(player, 1314);
                            if (hopquanhenhang != null && hopquanhenhang.quantity < 1) {
                                this.npcChat(player, "Bạn còn thiếu x" + (1 - hopquanhenhang.quantity) + " Hộp quà nhẹ nhàng.");
                            } else if (hopquanhenhang == null) {
                                this.npcChat(player, "Bạn không có Hộp quà nhẹ nhàng nào.");
                            } else {
                                short[] sk83 = {381, 382, 383, 384, 679, 678, 677, 676, 724, 675, 464, 583, 582, 581, 580, 425,
                                    1071, 1072, 1073, 1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083};
                                short itemId = sk83[Util.nextInt(sk83.length)];
                                Item sukien = Util.hopquachinchu(sk83[Util.nextInt(sk83.length)]);
                                InventoryService.gI().subQuantityItemsBag(player, hopquanhenhang, 1);
                                InventoryService.gI().addItemBag(player, sukien);
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã nhận được " + sukien.template.name);
                            }
                        }
                        break;
                        case 2:
                            Item hopquanhenhang = InventoryService.gI().findItemBagByTemp(player, 589);
                            if (hopquanhenhang != null && hopquanhenhang.quantity < 20) {
                                this.npcChat(player, "Bạn còn thiếu x" + (20 - hopquanhenhang.quantity) + " Hộp quà hên hạng.");
                            } else if (hopquanhenhang == null) {
                                this.npcChat(player, "Bạn không có Hộp quà hên hạng nào.");
                            } else {
                                short[] sk83 = {1150, 1151, 1152, 1153, 1154};
                                short itemId = sk83[Util.nextInt(sk83.length)];
                                int quantity = Util.nextInt(1, 5); // Số lượng từ 1-5

                                Item sukien = Util.hopquachinchu(itemId);
                                sukien.quantity = quantity;

                                InventoryService.gI().subQuantityItemsBag(player, hopquanhenhang, 20);
                                InventoryService.gI().addItemBag(player, sukien);
                                InventoryService.gI().sendItemBags(player);

                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã nhận được " + quantity + "x " + sukien.template.name);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }
}
