/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.combine.list;

import consts.ConstNpc;
import models.item.Item;
import models.item.Item.ItemOption;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;

/**
 *
 * @author Administrator
 */
public class ChuyenHoaBangVang {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item trangBiGoc = player.combineNew.itemsCombine.get(0);// trangg bị gốc ô 1
            Item trangBiCanChuyenHoa = player.combineNew.itemsCombine.get(1);// trang bị chuyển hóa ô 2

            int levelTrangBi = 0;
            int soLanRotCap = 0;
            int khongthechuyenhoa = 0;

            for (ItemOption io : trangBiGoc.itemOptions) {
                if (io.optionTemplate.id == 72) {
                    levelTrangBi = io.param - 1;
                } else if (io.optionTemplate.id == 230) {
                    soLanRotCap += io.param;
                } else if (io.optionTemplate.id == 30) {
                    khongthechuyenhoa = 1;
                }
            }
            // START Tính chỉ số nhân với số cấp và trừ với số lần rớt cấp //
            int chisogoc = trangBiCanChuyenHoa.itemOptions.get(0).param;
            for (int i = 0; i < levelTrangBi; i++) {
                chisogoc += chisogoc * 0.1;
            }
            chisogoc -= (chisogoc * (soLanRotCap * 0.1));
            // END Tính chỉ số nhân với số cấp và trừ với số lần rớt cấp //

            boolean trangBi_daNangCap_daPhaLeHoa = false;

            for (int so = 0; so < trangBiCanChuyenHoa.itemOptions.size(); so++) {
                if (trangBiCanChuyenHoa.itemOptions.get(so).optionTemplate.id == 72 || trangBiCanChuyenHoa.itemOptions.get(so).optionTemplate.id == 102) {
                    trangBi_daNangCap_daPhaLeHoa = true;
                    break;
                }
            }
            if (khongthechuyenhoa == 1) {
                Service.gI().sendThongBaoOK(player, "Không thể chuyển hóa đồ không giao dịch");
                return;
            } else if (!CombineService.gI().isTrangBiGoc(trangBiGoc)) {
                Service.gI().sendThongBaoOK(player, "Trang bị gốc phải từ bậc lưỡng long, Jean hoặc Zelot");
                return;
            } else if (levelTrangBi < 3) {
                Service.gI().sendThongBaoOK(player, "Trang bị gốc phải từ [+4] trở lên");
                return;
            } else if (!CombineService.gI().isTrangBiChuyenHoa(trangBiCanChuyenHoa)) {
                Service.gI().sendThongBaoOK(player, "Trang bị chuyển hóa phải là đồ thần linh");
                return;
            } else if (trangBi_daNangCap_daPhaLeHoa) {
                Service.gI().sendThongBaoOK(player, "Trang bị chuyển hóa phải chưa nâng cấp và pha lê hóa trang bị");
                return;
            } else if (!CombineService.gI().isCheckTrungTypevsGender(trangBiGoc, trangBiCanChuyenHoa)) {
                Service.gI().sendThongBaoOK(player, "Trang bị gốc và Trang bị chuyển hóa phải cùng loại và hành tinh");
                return;
            } else {
                String NpcSay = "|2|Hiện tại " + trangBiCanChuyenHoa.getName() + "\n";
                if (trangBiCanChuyenHoa.itemOptions != null) {
                    for (ItemOption io : trangBiCanChuyenHoa.itemOptions) {
                        if (io.optionTemplate.id != 72) {
                            NpcSay += "|0|" + io.getOptionString() + "\n";
                        }
                    }
                }
                NpcSay += "|2|Sau khi nâng cấp (+" + levelTrangBi + 1 + ")\n";
                for (ItemOption io : trangBiCanChuyenHoa.itemOptions) {
                    if (io.optionTemplate.id != 72) {
                        if (io.optionTemplate.id == 0 || io.optionTemplate.id == 47 || io.optionTemplate.id == 6 || io.optionTemplate.id == 7 || io.optionTemplate.id == 14 || io.optionTemplate.id == 22 || io.optionTemplate.id == 23) {
                            NpcSay += "|1|" + io.getOptionString(chisogoc) + "\n";
                        } else {
                            NpcSay += "|1|" + io.getOptionString() + "\n";
                        }
                    }
                }
                for (ItemOption io : trangBiGoc.itemOptions) {
                    if (io.optionTemplate.id != 72 && io.optionTemplate.id != 102 && io.optionTemplate.id != 107 && io.optionTemplate.id != 0 && io.optionTemplate.id != 47 && io.optionTemplate.id != 6 && io.optionTemplate.id != 7 && io.optionTemplate.id != 14 && io.optionTemplate.id != 22 && io.optionTemplate.id != 23) {
                        NpcSay += io.getOptionString() + "\n";
                    } else {
                    }
                }
                NpcSay += "Chuyển qua tất cả sao pha lê\n";
                NpcSay += "|2|Cần 20 tỷ vàng";
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, NpcSay,
                        "Nâng cấp\n20 tỷ\nvàng", "Từ chối");
            }
        } else {
            Service.gI().sendThongBaoOK(player, "Cần 1 trang bị có cấp từ [+4] và 1 trang bị không có cấp nhưng cao hơn 1 bậc");
            return;
        }
    }

    public static void ChuyenHoaBangvang(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                Item trangBiGoc = player.combineNew.itemsCombine.get(0);
                Item trangBiCanChuyenHoa = player.combineNew.itemsCombine.get(1);

                Item trangBiCanChuyenHoa_2 = ItemService.gI().createNewItem(player.combineNew.itemsCombine.get(1).template.id);// item mới được tạo ra
                long goldChuyenHoa = 20_000_000_000L;

                int levelTrangBi = 0;
                int soLanRotCap = 0;
                int khongchuyenhoa = 0;

                for (ItemOption io : trangBiGoc.itemOptions) {
                    if (io.optionTemplate.id == 72) {
                        levelTrangBi = io.param - 1;
                    } else if (io.optionTemplate.id == 230) {
                        soLanRotCap += io.param;
                    } else if (io.optionTemplate.id == 30) {
                        khongchuyenhoa = 1;
                    }
                }
                // START Tính chỉ số nhân với số cấp và trừ với số lần rớt cấp //
                int chisogoc = trangBiCanChuyenHoa.itemOptions.get(0).param;

                for (int i = 0; i < levelTrangBi; i++) {
                    chisogoc += chisogoc * 0.1;
                }
                chisogoc -= chisogoc * (soLanRotCap * 0.1);
                // END Tính chỉ số nhân với số cấp và trừ với số lần rớt cấp //

                if (player.inventory.gold >= goldChuyenHoa) {
                    if (khongchuyenhoa == 1) {
                        Service.gI().sendThongBaoOK(player, "Không thể chuyển hóa vật phẩm không thể giao dịch");
                        return;
                    } else if (!CombineService.gI().isTrangBiGoc(trangBiGoc)) {
                        Service.gI().sendThongBaoOK(player, "Trang bị phải từ bậc lưỡng long, Jean hoặc Zelot trở lên");
                        return;
                    } else if (levelTrangBi < 3) {
                        Service.gI().sendThongBaoOK(player, "Trang bị gốc phải từ [+4] trở lên");
                        return;
                    } else if (!CombineService.gI().isTrangBiChuyenHoa(trangBiCanChuyenHoa)) {
                        Service.gI().sendThongBaoOK(player, "Trang bị chuyển hóa phải là đồ thần linh");
                        return;
                    } else if (!CombineService.gI().isCheckTrungTypevsGender(trangBiGoc, trangBiCanChuyenHoa)) {
                        Service.gI().sendThongBaoOK(player, "Trang bị gốc và Trang bị chuyển hóa phải cùng loại và hành tinh");
                        return;
                    } else {

                        trangBiCanChuyenHoa.itemOptions.get(0).param = chisogoc;// lấy chỉ số HP,KI,tấn công,giáp
                        for (int i = 1; i < trangBiGoc.itemOptions.size(); i++) {
                            trangBiCanChuyenHoa.itemOptions.add(new ItemOption(trangBiGoc.itemOptions.get(i).optionTemplate.id, trangBiGoc.itemOptions.get(i).param));
                        }

                        for (int i = 0; i < trangBiCanChuyenHoa.itemOptions.size(); i++) {
                            trangBiCanChuyenHoa_2.itemOptions.add(new ItemOption(trangBiCanChuyenHoa.itemOptions.get(i).optionTemplate.id, trangBiCanChuyenHoa.itemOptions.get(i).param));
                        }

                        player.inventory.gold -= 20_000_000_000L;
                        Service.gI().sendMoney(player);
                        InventoryService.gI().subQuantityItemsBag(player, trangBiGoc, 1);
                        InventoryService.gI().subQuantityItemsBag(player, trangBiCanChuyenHoa, 1);
                        InventoryService.gI().addItemBag(player, trangBiCanChuyenHoa_2);
                        InventoryService.gI().sendItemBags(player);
                        CombineService.gI().reOpenItemCombine(player);
                        CombineService.gI().sendEffectSuccessCombine(player);
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không đủ vàng!");
                    return;
                }
            } else {
                Service.gI().sendThongBao(player, "Cần 1 ô trống hành trang");
                return;
            }
        }
    }
}
