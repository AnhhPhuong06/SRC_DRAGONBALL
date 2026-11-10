package models.combine.list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import models.item.Item;
import java.util.Objects;
import services.CombineService;
import static services.CombineService.MAX_LEVEL_ITEM;
import utils.CombineUtil;
import models.player.Player;
import server.ServerNotify;
import services.player.InventoryService;
import services.Service;
import utils.Util;

public class NangCapVatPham {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() >= 2 && player.combineNew.itemsCombine.size() < 4) {
            if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.template.type < 5).count() < 1) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Thiếu đồ nâng cấp", "Đóng");
                return;
            }
            if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.template.type == 14).count() < 1) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Thiếu đá nâng cấp", "Đóng");
                return;
            }
            if (player.combineNew.itemsCombine.size() == 3 && player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.template.id == 987).count() < 1) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Thiếu đồ nâng cấp", "Đóng");
                return;
            }
            Item itemDo = null;
            Item itemDNC = null;
            Item itemDBV = null;
            for (int j = 0; j < player.combineNew.itemsCombine.size(); j++) {
                if (player.combineNew.itemsCombine.get(j).isNotNullItem()) {
                    if (player.combineNew.itemsCombine.size() == 3 && player.combineNew.itemsCombine.get(j).template.id == 987) {
                        itemDBV = player.combineNew.itemsCombine.get(j);
                        continue;
                    }
                    if (player.combineNew.itemsCombine.get(j).template.type < 5) {
                        itemDo = player.combineNew.itemsCombine.get(j);
                    } else {
                        itemDNC = player.combineNew.itemsCombine.get(j);
                    }
                }
            }
            if (CombineUtil.isCoupleItemNangCapCheck(itemDo, itemDNC)) {
                int level = 0;
                for (Item.ItemOption io : itemDo.itemOptions) {
                    if (io.optionTemplate.id == 72) {
                        level = io.param;
                        break;
                    }
                }
                if (level < CombineService.MAX_LEVEL_ITEM) {
                    player.combineNew.goldCombine = CombineUtil.getGoldNangCapDo(level);
                    player.combineNew.ratioCombine = (float) CombineUtil.getTileNangCapDo(level);
                    player.combineNew.countDaNangCap = CombineUtil.getCountDaNangCapDo(level);
                    player.combineNew.countDaBaoVe = (short) CombineUtil.getCountDaBaoVe(level);
                    String npcSay = "Hiện tại " + itemDo.template.name + " (+" + level + ")\n";
                    for (Item.ItemOption io : itemDo.itemOptions) {
                        if (io.optionTemplate.id != 72) {
                            npcSay += io.getOptionString() + "\n";
                        }
                    }
                    String option = null;
                    int param = 0;
                    for (Item.ItemOption io : itemDo.itemOptions) {
                        if (io.optionTemplate.id == 47
                                || io.optionTemplate.id == 6
                                || io.optionTemplate.id == 0
                                || io.optionTemplate.id == 7
                                || io.optionTemplate.id == 14
                                || io.optionTemplate.id == 22
                                || io.optionTemplate.id == 23) {
                            option = io.optionTemplate.name;
                            param = io.param + (io.param * 10 / 100);
                            break;
                        }
                    }
                    String previewOption = option == null ? "" : option.replaceAll("#", String.valueOf(param));
                    npcSay += "Sau khi nâng cấp (+" + (level + 1) + ")\n"
                            + previewOption
                            + "\nTỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n"
                            + "Cần " + player.combineNew.countDaNangCap + " " + itemDNC.template.name
                            + "\n"
                            + "Cần " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng";

                    String daNPC = player.combineNew.itemsCombine.size() == 3 && itemDBV != null ? String.format("\nCần tốn %s đá bảo vệ", player.combineNew.countDaBaoVe) : "";
                    if ((level == 2 || level == 4 || level == 6) && !(player.combineNew.itemsCombine.size() == 3 && itemDBV != null)) {
                        npcSay += "\nNếu thất bại sẽ rớt xuống (+" + (level - 1) + ")";
                    }
                    if (player.combineNew.countDaNangCap > itemDNC.quantity) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                npcSay, "Còn thiếu\n" + (player.combineNew.countDaNangCap - itemDNC.quantity) + " " + itemDNC.template.name);
                    } else if (player.combineNew.goldCombine > player.inventory.gold) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                npcSay, "Còn thiếu\n" + Util.numberToMoney((player.combineNew.goldCombine - player.inventory.gold)) + " vàng");
                    } else if (player.combineNew.itemsCombine.size() == 3 && Objects.nonNull(itemDBV) && itemDBV.quantity < player.combineNew.countDaBaoVe) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                npcSay, "Còn thiếu\n" + (player.combineNew.countDaBaoVe - itemDBV.quantity) + " đá bảo vệ");
                    } else {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                                npcSay, "Nâng cấp\n" + Util.numberToMoney(player.combineNew.goldCombine) + " vàng" + daNPC, "Từ chối");
                    }
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Trang bị của ngươi đã đạt cấp tối đa", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hãy chọn 1 trang bị và 1 loại đá nâng cấp", "Đóng");
            }
        } else {
            if (player.combineNew.itemsCombine.size() > 3) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cất đi con ta không thèm", "Đóng");
                return;
            }
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hãy chọn 1 trang bị và 1 loại đá nâng cấp", "Đóng");
        }
    }

    public static void nangCapVatPham(Player player) {
        if (player.combineNew.itemsCombine.size() >= 2 && player.combineNew.itemsCombine.size() < 4) {
            if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.template.type < 5).count() != 1) {
                return;
            }
            if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.template.type == 14).count() != 1) {
                return;
            }
            if (player.combineNew.itemsCombine.size() == 3 && player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.template.id == 987).count() != 1) {
                return; // admin
            }

            Item itemDo = null;
            Item itemDNC = null;
            Item itemDBV = null;
            for (int j = 0; j < player.combineNew.itemsCombine.size(); j++) {
                if (player.combineNew.itemsCombine.get(j).isNotNullItem()) {
                    if (player.combineNew.itemsCombine.size() == 3 && player.combineNew.itemsCombine.get(j).template.id == 987) {
                        itemDBV = player.combineNew.itemsCombine.get(j);
                        continue;
                    }
                    if (player.combineNew.itemsCombine.get(j).template.type < 5) {
                        itemDo = player.combineNew.itemsCombine.get(j);
                    } else {
                        itemDNC = player.combineNew.itemsCombine.get(j);
                    }
                }
            }

            if (CombineUtil.isCoupleItemNangCapCheck(itemDo, itemDNC)) {
                int countDaNangCap = player.combineNew.countDaNangCap;
                int gold = player.combineNew.goldCombine;
                short countDaBaoVe = player.combineNew.countDaBaoVe;
                if (player.inventory.gold < gold) {
                    Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                    return;
                }

                if (itemDNC.quantity < countDaNangCap) {
                    return;
                }
                if (player.combineNew.itemsCombine.size() == 3) {
                    if (Objects.isNull(itemDBV)) {
                        return;
                    }
                    if (itemDBV.quantity < countDaBaoVe) {
                        return;
                    }
                }

                int level = 0;
                Item.ItemOption optionLevel = null;
                for (Item.ItemOption io : itemDo.itemOptions) {
                    if (io.optionTemplate.id == 72) {
                        level = io.param;
                        optionLevel = io;
                        break;
                    }
                }
                if (level < MAX_LEVEL_ITEM) {
                    Item.ItemOption option = null;
                    Item.ItemOption option2 = null;
                    for (Item.ItemOption io : itemDo.itemOptions) {
                        if (io == null || io.optionTemplate == null) {
                            continue;
                        }
                        if (io.optionTemplate.id == 47
                                || io.optionTemplate.id == 6
                                || io.optionTemplate.id == 0
                                || io.optionTemplate.id == 7
                                || io.optionTemplate.id == 14
                                || io.optionTemplate.id == 22
                                || io.optionTemplate.id == 23) {
                            option = io;
                        } else if (io.optionTemplate.id == 27
                                || io.optionTemplate.id == 28) {
                            option2 = io;
                        }
                    }
                    // Món không có dòng chỉ số nào nâng được thì KHÔNG có gì để cộng.
                    // Phải chặn TRƯỚC khi trừ vàng: bản cũ trừ vàng rồi mới văng NPE,
                    // người chơi mất vàng mà đồ không lên gì cả.
                    if (option == null) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Trang bị này không có chỉ số nào để nâng cấp", "Đóng");
                        return;
                    }
                    player.inventory.gold -= gold;
                    if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                        option.param += (option.param * 10 / 100) < 1 ? 1 : (option.param * 10 / 100);
                        if (option2 != null) {
                            option2.param += (option2.param * 10 / 100) < 1 ? 1 : (option2.param * 10 / 100);
                        }
                        if (optionLevel == null) {
                            itemDo.itemOptions.add(new Item.ItemOption(72, 1));
                        } else {
                            optionLevel.param++;
                        }
                        if (optionLevel != null && optionLevel.param >= 5) {
                            ServerNotify.gI().notify("Chúc mừng " + player.name + " vừa nâng cấp "
                                    + "thành công " + itemDo.template.name + " lên +" + optionLevel.param);
                        }
                        CombineService.gI().sendEffectSuccessCombine(player);

                        // Gọi addPointThanhDapDo để tăng điểm
                        player.effect.addPointThanhDapDo();  // Tăng điểm sau mỗi lần nâng cấp thành công
                    } else {
                        if ((level == 2 || level == 4 || level == 6) && (player.combineNew.itemsCombine.size() != 3)) {
                            option.param -= (option.param * 11 / 100) < 1 ? 1 : (option.param * 11 / 100);
                            if (option2 != null) {
                                option2.param -= (option2.param * 11 / 100) < 1 ? 1 : (option2.param * 11 / 100);
                            }
                            optionLevel.param--;
                            if (level == 7) {
                                if (player.playerTask.taskdh.DapDo < 3) {
                                    int required = 3;
                                    int percentDone = (int) ((double) player.playerTask.taskdh.DapDo / required * 100);
                                    player.playerTask.taskdh.DapDo++;
                                    player.playerTask.taskdh.ResetTime = System.currentTimeMillis();
                                    Service.gI().sendThongBao(player, "Tiến độ hiện tại:  " + percentDone + "%");
                                }
                            }
                        }
                        CombineService.gI().sendEffectFailCombine(player);
                    }

                    if (player.combineNew.itemsCombine.size() == 3) {
                        InventoryService.gI().subQuantityItemsBag(player, itemDBV, countDaBaoVe);
                    }
                    InventoryService.gI().subQuantityItemsBag(player, itemDNC, player.combineNew.countDaNangCap);
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendMoney(player);
                    CombineService.gI().reOpenItemCombine(player);
                    player.combineNew.itemsCombine.clear();
                }
            }
        }
    }

}
