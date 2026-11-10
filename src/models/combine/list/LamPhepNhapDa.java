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
public class LamPhepNhapDa {

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.combineNew.itemsCombine.size() == 2) {
                Item item = player.combineNew.itemsCombine.get(0);
                Item bnp = player.combineNew.itemsCombine.get(1);
                if (bnp != null && bnp.isNotNullItem() && (bnp.template.id == 226)) {
                    if (item != null && item.isNotNullItem() && (item.template.id == 225)) {
                        if (item.quantity >= 99 && bnp.quantity >= 1) {
                            int sl = 1;
                            String npcSay = "|2|Con có muốn biến x99 " + item.template.name + " thành\n"
                                    + "10 đá nâng cấp ngẫu nhiên\n"
                                    + "\n|7|Cần x99 \n" + item.template.name;

                            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, "Làm phép", "Từ chối");

                        } else {
                            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Không đủ mảnh đá vụn rồi", "Đóng");
                        }
                    } else {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Mảnh đá vụn đâu con", "Đóng");
                    }
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Bình nước phép đâu con", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần bình nước phép và 99 mảnh đá vụn", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
        }
    }

    public static void LamPhepNhapda(Player player) {
    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
        if (player.combineNew.itemsCombine.size() == 2) {

            Item item = player.combineNew.itemsCombine.get(0);
            Item bnp = player.combineNew.itemsCombine.get(1);

            if (bnp != null && bnp.isNotNullItem() && bnp.template != null && bnp.template.id == 226) {
                if (item != null && item.isNotNullItem() && item.template != null && item.template.id == 225) {
                    int sl = 1;
                    if (item.template != null && item.quantity >= 99 && bnp.quantity >= 1) {
                        // Randomly select an item ID from 220 to 224 (inclusive)
                        int dnc = Util.nextInt(220, 224);  // This ensures the dnc is between 220 and 224
                        
                        // Create the new item with the selected ID and quantity of 10
                        Item nr = ItemService.gI().createNewItem((short) dnc, 10);
                        byte option = ItemService.gI().optionRac((short) dnc);
                        
                        // Add the new item to the player's inventory
                        InventoryService.gI().addItemBag(player, nr);
                        // Subtract the required quantities of mảnh đá vụn (99) and bình nước phép (1)
                        InventoryService.gI().subQuantityItemsBag(player, item, 99);
                        InventoryService.gI().subQuantityItemsBag(player, bnp, 1);

                        // Refresh the player's inventory
                        InventoryService.gI().sendItemBags(player);
                        
                        // Re-open the combine window
                        CombineService.gI().reOpenItemCombine(player);
                        
                        // Send the visual effect for the item combination
                        if (item.template != null) {
                            CombineService.gI().sendEffectCombineDV(player, item.template.iconID);
                        }
                        
                        // Notify the player about the successful item creation
                        Service.gI().sendThongBao(player, "Nhập mảnh đá vụn thành công nhận  " + ItemService.gI().getTemplate(dnc).name + "");
                    } else {
                        Service.gI().sendThongBao(player, "Không đủ 99 mảnh đá vụn và 1 bình nước phép :3");
                    }

                } else {
                    Service.gI().sendThongBao(player, "Mới chơi đồ à");
                }
            } else {
                Service.gI().sendThongBao(player, "Bình nước phép ");
            }
        } else {
            Service.gI().sendThongBao(player, "Đặt mảnh đá vụn trước rồi tới bình nước phép");
        }
    } else {
        Service.gI().sendThongBao(player, "Cần để trống 1 ô hành trang");
    }
}


}
