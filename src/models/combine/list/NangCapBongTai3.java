package models.combine.list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import models.item.Item;
import services.CombineService;
import models.player.Player;
import services.player.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class NangCapBongTai3 {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item bongTai2 = null;
            Item manhVo = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == 921) {
                    bongTai2 = item;
                }
                if (item.template.id == 1493) {
                    manhVo = item;
                }
            }
            if (bongTai2 != null && manhVo != null) {
                if (bongTai2.itemOptions.size() < 2) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Bông tai Porata cấp 2 của ngươi chưa được mở chỉ số, hãy đi mở chỉ số trước rồi quay lại đây", "Đóng");
                    return;
                }
                int soLuongManhVo = InventoryService.gI().getParam(player, 31, 1493);
                player.combineNew.gemCombine = 500;
                player.combineNew.ratioCombine = services.CombineRateConfig.rateBongTai3;

                String npcSay = "|2|Bông tai Porata [+3]\n|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n";

                if (soLuongManhVo >= 20000) {
                    npcSay += "|2|Cần 20000 " + manhVo.template.name + "\n";
                } else {
                    npcSay += "|7|Cần 20000 " + manhVo.template.name + "\n";
                }

                if (player.inventory.getGemAndRuby() >= player.combineNew.gemCombine) {
                    npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc\n";
                } else {
                    npcSay += "|7|Cần: " + player.combineNew.gemCombine + " ngọc\n";
                }

                npcSay += "|7|Thất bại -200 " + manhVo.template.name + "\n";

                if (soLuongManhVo < 20000) {
                    npcSay += "Còn thiếu " + (20000 - soLuongManhVo) + " " + manhVo.template.name;
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.getGemAndRuby() < player.combineNew.gemCombine) {
                    npcSay += "Còn thiếu " + (player.combineNew.gemCombine - player.inventory.getGemAndRuby()) + " ngọc";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\n" + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Bông tai Porata cấp 2 và Mảnh vỡ bông tai cấp 3", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Bông tai Porata cấp 2 và Mảnh vỡ bông tai cấp 3", "Đóng");
        }
    }

    public static void nangCapBongTai3(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item bongTai2 = null;
            Item manhVo = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == 921) {
                    bongTai2 = item;
                }
                if (item.template.id == 1493) {
                    manhVo = item;
                }
            }
            if (bongTai2 != null && manhVo != null) {
                if (bongTai2.itemOptions.size() < 2) {
                    Service.gI().sendThongBao(player, "Bông tai Porata cấp 2 của ngươi chưa được mở chỉ số!");
                    return;
                }
                int soLuongManhVo = InventoryService.gI().getParam(player, 31, 1493);
                if (soLuongManhVo >= 20000) {
                    Item bongTaiCap3 = InventoryService.gI().findItemBag(player, 1492);
                    if (bongTaiCap3 == null) {
                        if (player.inventory.getGemAndRuby() >= 500) {
                            if (player.inventory.gem >= 500) {
                                player.inventory.gem -= 500;
                            } else {
                                int rubyNeed = 500 - player.inventory.gem;
                                player.inventory.gem = 0;
                                player.inventory.ruby -= rubyNeed;
                            }

                            if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                                bongTai2.template = ItemService.gI().getTemplate(1492);
                                for (Item.ItemOption option : bongTai2.itemOptions) {
                                    if (option.optionTemplate.id == 72) {
                                        option.param = 3;
                                        break;
                                    }
                                }
                                InventoryService.gI().subParamItemsBag(player, 1493, 31, 20000);
                                CombineService.gI().sendEffectSuccessCombine(player);
                            } else {
                                InventoryService.gI().subParamItemsBag(player, 1493, 31, 200);
                                CombineService.gI().sendEffectFailCombine(player);
                            }
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendMoney(player);
                            CombineService.gI().reOpenItemCombine(player);
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Ngươi đã có Bông tai Porata cấp 3 trong hành trang rồi");
                    }
                }
            }
        }
    }
}
