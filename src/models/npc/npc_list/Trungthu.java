/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.npc.npc_list;

import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.ShopService;
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class Trungthu extends Npc {

    public Trungthu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Chúc các bạn trung thu vui vẻ", "Cửa hàng", "Đổi thú cưng", "Đổi 99\nCà rốt\n Lấy quà", "Đổi 99\nđuổi khỉ\n Lấy quà");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        ShopService.gI().opendShop(player, String.format("SHOP%d", select), false);
                    }
                    case 1 -> {
                        createOtherMenu(player, 0, "Đổi 20 bánh trung thu Gà quay lấy thỏ xám\nĐổi 10 bánh trung thu Thập cẩm lấy thỏ trắng\nĐổi 10 bánh trung thu Hạt sen lấy Pet mèo đen đuôi vàng\nHạn sử dụng ngẫu nhiên",
                                "Đổi\n thỏ xám", "Đổi\n thỏ trắng", "Đổi\nMèo đen\nđuôi vàng", "Đóng");
                    }
                    case 2, 3 -> {
                        int ID = (select == 2 ? 462 : 1045);
                        Item it = InventoryService.gI().findItemBag(player, ID);
                        if (it != null && it.quantity >= 99) {
                            Item it1 = ItemService.gI().createNewItem((short) Util.nextInt(1099, 1103), 5);
                            it1.itemOptions.add(new Item.ItemOption(86, 0));
                            InventoryService.gI().addItemBag(player, it1);
                            Service.gI().sendThongBao(player, "Bạn vừa nhận được " + it1.template.name);
                            InventoryService.gI().subQuantityItemsBag(player, it, 99);
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Bạn không đủ vật phẩm ");
                        }
                    }

                }
            } else if (player.idMark.getIndexMenu() == 0) {
                switch (select) {
                    case 0, 1, 2 -> {
                        int idCheck = (select == 0 ? 890 : select == 1 ? 891 : 1512);
                        int items = (select == 0 ? 892 : select == 1 ? 893 : 1107);
                        int Quantity = (select == 0 ? 20 : 10);
                        Item it = InventoryService.gI().findItemBag(player, idCheck);
                        if (it == null || it.quantity < Quantity) {
                            Service.gI().sendThongBao(player, "Bạn không đủ vật phẩm");
                            return;
                        }
                        Item it1 = ItemService.gI().createNewItem((short) items, 1);
                        it1.itemOptions.add(new Item.ItemOption(77, 15));
                        it1.itemOptions.add(new Item.ItemOption(103, 14));
                        it1.itemOptions.add(new Item.ItemOption(50, 14));
                        it1.itemOptions.add(new Item.ItemOption(93, 30));
                        it1.itemOptions.add(new Item.ItemOption(30, 0));
                        InventoryService.gI().addItemBag(player, it1);
                        Service.gI().sendThongBao(player, "Bạn vừa nhận được " + it1.template.name);
                        InventoryService.gI().subQuantityItemsBag(player, it, Quantity);
                        InventoryService.gI().sendItemBags(player);
                    } 
                    default -> {
                    return;
                    }
                }
            }
        }
    }
}
