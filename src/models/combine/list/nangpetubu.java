/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.combine.list;

import consts.ConstNpc;
import models.item.Item;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class nangpetubu {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item trungubu = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == 1520) {
                    trungubu = item;
                }
            }
            if (trungubu != null) {
                String npcSay = "|2|Nâng cấp đệ Uub\n "
                        + "Cần 99 mảnh trứng ubu\n"
                        + "Tỉ lệ thành công: 36%\n"
                        + "Nâng cấp thất bại sẽ mất 99 mảnh trứng ubu";
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, "Nâng cấp", "Từ chối");
            } else {
                Service.gI().sendThongBaoOK(player, "Cần cần đệ tử mabu 60 tỉ sm và 99 mảnh trứng ubu.");
            }
        } else {
            Service.gI().sendThongBaoOK(player, "Cần cần đệ tử mabu 60 tỉ sm và 99 mảnh trứng ubu.");
        }
    }

    public static void NangCap(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item trungubu = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == 1520) {
                    trungubu = item;
                }
            }
            if (trungubu != null && trungubu.quantity >= 99 && checkMabuPetPower(player)) {
                InventoryService.gI().subQuantityItemsBag(player, trungubu, 99);
                if (Util.isTrue(10, 100)) {
                    Item it = ItemService.gI().createNewItem((short)1521, 1);
                    it.itemOptions.add(new Item.ItemOption(30, 0));
                    InventoryService.gI().addItemBag(player, it);
                    CombineService.gI().sendEffectSuccessCombine(player);
                    InventoryService.gI().sendItemBags(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }
                CombineService.gI().reOpenItemCombine(player);
            } else {
                Service.gI().sendThongBaoOK(player, "Cần cần đệ tử mabu 60 tỉ sm và 99 mảnh trứng ubu.");
            }
        }
    }

    public static boolean checkMabuPetPower(Player player) {
        if (player.pet == null) {
            Service.gI().sendThongBao(player, "Bạn chưa sở hữu đệ tử");
            return false;
        }
        if (player.pet.typePet != 1) {
              Service.gI().sendThongBao(player, "Đệ tử của bạn không phải mabu");
            return false;
        }
        return player.pet.nPoint.power >= 60_000_000_000L;
    }
}
