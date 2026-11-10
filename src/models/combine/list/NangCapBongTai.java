package models.combine.list;


import consts.ConstNpc;
import models.item.Item;
import services.CombineService;
import models.player.Player;
import services.player.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class NangCapBongTai {

    private static final int GOLD_BONG_TAI = 200_000_000;
    private static final int GEM_BONG_TAI = 1_000;
    public static int RATIO_BONG_TAI = services.CombineRateConfig.rateBongTai2;
    private static final int ID_BONG_TAI_CAP_1 = 454;
    private static final int ID_BONG_TAI_CAP_2 = 921;
    private static final int ID_MANH_VO = 933;
    private static final int SO_LUONG_MANH_VO_CAN = 9999;
    private static final int SO_LUONG_MANH_VO_MAT_KHI_THAT_BAI = 99;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item bongTai = null;
            Item manhVo = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == ID_BONG_TAI_CAP_1) {
                    bongTai = item;
                }
                if (item.template.id == ID_MANH_VO) {
                    manhVo = item;
                }
            }

            if (bongTai != null && manhVo != null) {
                int soLuongManhVo = InventoryService.gI().getParam(player, 31, ID_MANH_VO);
                player.combineNew.goldCombine = GOLD_BONG_TAI;
                player.combineNew.gemCombine = GEM_BONG_TAI;
                player.combineNew.ratioCombine = RATIO_BONG_TAI;

                String npcSay = "|2|Bông tai Porata [+2]\n";
                npcSay += "|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n";

                if (soLuongManhVo >= SO_LUONG_MANH_VO_CAN) {
                    npcSay += "|2|Cần " + SO_LUONG_MANH_VO_CAN + " " + manhVo.template.name + "\n";
                } else {
                    npcSay += "|7|Cần " + SO_LUONG_MANH_VO_CAN + " " + manhVo.template.name + "\n";
                }

                if (player.inventory.getGemAndRuby() >= player.combineNew.gemCombine) {
                    npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc\n";
                } else {
                    npcSay += "|7|Cần: " + player.combineNew.gemCombine + " ngọc\n";
                }

                if (player.inventory.gold >= player.combineNew.goldCombine) {
                    npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                } else {
                    npcSay += "|7|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                }

                npcSay += "|7|Thất bại -" + SO_LUONG_MANH_VO_MAT_KHI_THAT_BAI + " " + manhVo.template.name + "\n";

                if (soLuongManhVo < SO_LUONG_MANH_VO_CAN) {
                    npcSay += "Còn thiếu " + (SO_LUONG_MANH_VO_CAN - soLuongManhVo) + " " + manhVo.template.name;
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.getGemAndRuby() < player.combineNew.gemCombine) {
                    npcSay += "Còn thiếu " + (player.combineNew.gemCombine - player.inventory.getGemAndRuby()) + " ngọc";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.gold < player.combineNew.goldCombine) {
                    npcSay += "Còn thiếu " + Util.numberToMoney(player.combineNew.goldCombine - player.inventory.gold) + " vàng";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\n" + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n"
                            + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Bông tai Porata cấp 1 và Mảnh vỡ bông tai", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Bông tai Porata cấp 1 và Mảnh vỡ bông tai", "Đóng");
        }
    }

    public static void nangCapBongTai(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item bongTai = null;
            Item manhVo = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == ID_BONG_TAI_CAP_1) {
                    bongTai = item;
                }
                if (item.template.id == ID_MANH_VO) {
                    manhVo = item;
                }
            }
            if (bongTai != null && manhVo != null) {
                int soLuongManhVo = InventoryService.gI().getParam(player, 31, ID_MANH_VO);
                if (soLuongManhVo >= SO_LUONG_MANH_VO_CAN) {
                    Item bongTaiCap2 = InventoryService.gI().findItemBag(player, ID_BONG_TAI_CAP_2);
                    if (bongTaiCap2 == null) {
                        long goldNeed = player.combineNew.goldCombine;
                        int gemNeed = player.combineNew.gemCombine;
                        if (player.inventory.gold >= goldNeed && player.inventory.getGemAndRuby() >= gemNeed) {
                            player.inventory.gold -= goldNeed;

                            if (player.inventory.gem >= gemNeed) {
                                player.inventory.gem -= gemNeed;
                            } else {
                                int rubyNeed = gemNeed - player.inventory.gem;
                                player.inventory.gem = 0;
                                player.inventory.ruby -= rubyNeed;
                            }
                            if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                                bongTai.template = ItemService.gI().getTemplate(ID_BONG_TAI_CAP_2);
                                bongTai.itemOptions.clear();
                                bongTai.itemOptions.add(new Item.ItemOption(72, 2));
                                InventoryService.gI().subParamItemsBag(player, ID_MANH_VO, 31, SO_LUONG_MANH_VO_CAN);
                                CombineService.gI().sendEffectSuccessCombine(player);
                            } else {
                                InventoryService.gI().subParamItemsBag(player, ID_MANH_VO, 31, SO_LUONG_MANH_VO_MAT_KHI_THAT_BAI);
                                CombineService.gI().sendEffectFailCombine(player);
                            }
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendMoney(player);
                            CombineService.gI().reOpenItemCombine(player);
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Ngươi đã có Bông tai Porata cấp 2 trong hành trang rồi");
                    }
                }
            }
        }
    }
}
