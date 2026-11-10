package models.combine.list;

import consts.ConstNpc;
import models.item.Item;
import services.CombineService;
import utils.CombineUtil;
import models.player.Player;
import services.player.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class EpSaoTrangBi {

    public static void showInfoCombine(Player player) {
        synchronized (player.inventory.itemsBag) { showInfoLocked(player); }
    }

    private static void showInfoLocked(Player player) {
        // Đặt đá tăng chỉ số (1364-1368) -> chuyển sang ép đá, không cần lỗ sao pha lê
        if (EpDaTangChiSo.hasDaTangChiSo(player)) {
            EpDaTangChiSo.showInfoCombine(player);
            return;
        }
        if (player.combineNew.itemsCombine.size() == 2 || player.combineNew.itemsCombine.size() == 4) {
            Item trangBi = null;
            Item daPhaLe = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineUtil.isTrangBiPhaLeHoa(item)) {
                    trangBi = item;
                } else if (CombineUtil.isDaPhaLe(item)) {
                    daPhaLe = item;
                }
            }
            int star = 0; //sao pha lê đã ép
            int starEmpty = 0; //lỗ sao pha lê
            int starvip = 0; //lỗ sao pha lê
            if (trangBi != null && daPhaLe != null) {
                EighthStarMaterials extra = EighthStarMaterials.check(player, trangBi, daPhaLe);
                if (extra.error() != null) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, extra.error(), "Đóng");
                    return;
                }
                String epErr = CombineUtil.getEpSaoError(trangBi, daPhaLe);
                if (epErr != null) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, epErr, "Đóng");
                    return;
                }
                for (Item.ItemOption io : trangBi.itemOptions) {
                    switch (io.optionTemplate.id) {
                        case 102 -> star = io.param;
                        case 107 -> starEmpty = io.param;
                        case 230 -> starvip = io.param;
                        default -> {
                        }
                    }
                }
                if (star < starEmpty) {
                    if (starEmpty <= CombineService.MAX_STAR_ITEM || star < CombineService.MAX_STAR_ITEM || starvip == 1) {
                        player.combineNew.gemCombine = CombineUtil.getGemEpSao(star);
                        String npcSay = trangBi.template.name + "\n|2|";
                        for (Item.ItemOption io : trangBi.itemOptions) {
                            if (io.optionTemplate.id != 102) {
                                npcSay += io.getOptionString() + "\n";
                            }
                        }
                        if (daPhaLe.template.type == 30 && !daPhaLe.itemOptions.isEmpty()) {
                            for (Item.ItemOption io : daPhaLe.itemOptions) {
                                if (io != null && io.optionTemplate != null && io.optionTemplate.id != 30 && io.optionTemplate.id != 72 && io.optionTemplate.id != 73) {
                                    npcSay += "|7|" + io.getOptionString() + "\n";
                                }
                            }
                        } else {
                            int optId = CombineUtil.getOptionDaPhaLe(daPhaLe);
                            if (optId != -1) {
                                npcSay += "|7|" + ItemService.gI().getItemOptionTemplate(optId).name.replaceAll("#", CombineUtil.getParamDaPhaLe(daPhaLe) + "") + "\n";
                            }
                        }
                        npcSay += "|1|Cần " + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc" + extra.instructions();
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                                "Nâng cấp\ncần " + player.combineNew.gemCombine + " ngọc");
                    } else {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Vật Phẩm Đã Tới Giới Hạn Vui Lòng Cường Hóa Lỗ Vật Phẩm", "Đóng");
                    }
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào\n" + EighthStarMaterials.REQUIREMENTS, "Đóng");
        }
    }

    public static void epSaoTrangBi(Player player) {
        synchronized (player.inventory.itemsBag) { epSaoLocked(player); }
    }

    private static void epSaoLocked(Player player) {
        // Đặt đá tăng chỉ số (1364-1368) -> chuyển sang ép đá
        if (EpDaTangChiSo.hasDaTangChiSo(player)) {
            EpDaTangChiSo.epDa(player);
            return;
        }
        if (player.combineNew.itemsCombine.size() == 2 || player.combineNew.itemsCombine.size() == 4) {
            Item trangBi = null;
            Item daPhaLe = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineUtil.isTrangBiPhaLeHoa(item)) {
                    trangBi = item;
                } else if (CombineUtil.isDaPhaLe(item)) {
                    daPhaLe = item;
                }
            }
            int star = 0; //sao pha lê đã ép
            int starEmpty = 0; //lỗ sao pha lê
            if (trangBi != null && daPhaLe != null) {
                EighthStarMaterials extra = EighthStarMaterials.check(player, trangBi, daPhaLe);
                if (extra.error() != null) {
                    Service.gI().sendThongBao(player, extra.error());
                    return;
                }
                String epErr = CombineUtil.getEpSaoError(trangBi, daPhaLe);
                if (epErr != null) {
                    Service.gI().sendThongBao(player, epErr);
                    return;
                }
                boolean hadOption30 = trangBi.isHaveOption(30);
                Item.ItemOption optionStar = null;
                for (Item.ItemOption io : trangBi.itemOptions) {
                    if (io.optionTemplate.id == 102) {
                        star = io.param;
                        optionStar = io;
                    } else if (io.optionTemplate.id == 107) {
                        starEmpty = io.param;
                    }
                }
                if (star < starEmpty) {

                    int optionId = CombineUtil.getOptionDaPhaLe(daPhaLe);
                    int param = CombineUtil.getParamDaPhaLe(daPhaLe);
                    if (optionId == -1) {
                        Service.gI().sendThongBao(player, "Viên đá này chưa có chỉ số để ép");
                        return;
                    }
                    int gem = CombineUtil.getGemEpSao(star);
                    if (player.inventory.gem < gem) {
                        Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện");
                        return;
                    }
                    player.inventory.gem -= gem;
                    Item.ItemOption option = null;
                    for (Item.ItemOption io : trangBi.itemOptions) {
                        if (io.optionTemplate.id == optionId) {
                            option = io;
                            break;
                        }
                    }
                    if (option != null) {
                        option.param += param;
                    } else {
                        trangBi.itemOptions.add(new Item.ItemOption(optionId, param));
                    }
                    if (optionStar != null) {
                        optionStar.param++;
                    } else {
                        trangBi.itemOptions.add(new Item.ItemOption(102, 1));
                    }

                    if (hadOption30 && !trangBi.isHaveOption(30)) {
                        trangBi.itemOptions.add(new Item.ItemOption(30, 0));
                    }

                    extra.consume(player);
                    InventoryService.gI().subQuantityItemsBag(player, daPhaLe, 1);
                    CombineService.gI().sendEffectSuccessCombine(player);
                    services.func.GiftProgressService.gI().onCrystalSocketed(player);
                }
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            }
        }
    }
}
