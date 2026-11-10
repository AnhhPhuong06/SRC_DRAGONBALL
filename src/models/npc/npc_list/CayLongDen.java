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
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class CayLongDen extends Npc {

    public CayLongDen(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Mỗi lần treo đèn sẽ được 1 món quà xịn sò nhất.", "Treo đèn", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        Item it = InventoryService.gI().findItemBag(player, 1503);
                        if (it == null) {
                            Service.gI().sendThongBao(player, "Bạn không có lồng đèn");
                            return;
                        }
                        Item it1 = ItemService.gI().createNewItem((short) Util.nextInt(1099, 1102), 5);
                        it1.itemOptions.add(new Item.ItemOption(86, 0));
                        InventoryService.gI().addItemBag(player, it1);
                        InventoryService.gI().subQuantityItemsBag(player, it, 1);
                        InventoryService.gI().sendItemBags(player);
                    }
                }
            }
        }
    }
}
