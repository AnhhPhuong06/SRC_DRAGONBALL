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
import utils.Util;

public class NhapNgocRong {

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.combineNew.itemsCombine.size() == 1) {
                Item item = player.combineNew.itemsCombine.get(0);
                if (item != null && item.isNotNullItem() && (item.template.id > 14 && item.template.id <= 20) && item.quantity >= 7) {
                    String npcSay = "|2|Con có muốn biến 7 " + item.template.name + " thành\n"
                            + "1 viên " + ItemService.gI().getTemplate((short) (item.template.id - 1)).name + "\n"
                            + "|7|Cần 7 " + item.template.name;

                    // Thêm các lựa chọn "Đập x10" và "Đập x100"
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Làm phép", "Đập x10", "Đập x100", "Từ chối");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 7 viên ngọc rồng 2 sao trở lên", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 7 viên ngọc rồng 2 sao trở lên", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
        }
    }

    public static void nhapNgocRong(Player player, int... numm) {
        if (player.idMark != null && !Util.canDoWithTime(player.idMark.getLastTimeCombine(), 500)) {
            return;
        }
        player.idMark.setLastTimeCombine(System.currentTimeMillis());
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (!player.combineNew.itemsCombine.isEmpty()) {
                Item item = player.combineNew.itemsCombine.get(0);
                if (item != null && item.isNotNullItem() && item.template.id > 14 && item.template.id <= 20) {
                    // Mặc định cho đập x1
                    int requiredQuantity = 7;
                    int multiplier = 1;
                    String errorMessage = "";

                    // Kiểm tra số lượng tham số truyền vào để xác định kiểu đập
                    if (numm.length > 0) {
                        int action = numm[0]; // lấy giá trị đầu tiên trong mảng
                        if (action == 10) { // Đập x10
                            if (item.quantity >= 70) {
                                multiplier = 10;
                                requiredQuantity = 70; // Cần 70 viên để đập x10
                            } else {
                                errorMessage = "Không đủ " + item.template.name + " để đập x10. Cần 70 viên!";
                            }
                        } else if (action == 100) { // Đập x100
                            if (item.quantity >= 700) {
                                multiplier = 100;
                                requiredQuantity = 700; // Cần 700 viên để đập x100
                            } else {
                                errorMessage = "Không đủ " + item.template.name + " để đập x100. Cần 700 viên!";
                            }
                        }
                    }

                    // Nếu không có lỗi, thực hiện đập
                    if (errorMessage.isEmpty()) {
                        CombineService.gI().sendEffectCombineDB(player, item.template.iconID);
                        InventoryService.gI().addItemBag(player, ItemService.gI().createNewItem((short) (item.template.id - 1), multiplier));
                        InventoryService.gI().subQuantityItemsBag(player, item, requiredQuantity);
                        InventoryService.gI().sendItemBags(player);
                        CombineService.gI().reOpenItemCombine(player);
                    } else {
                        // Nếu không đủ, thông báo lỗi
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, errorMessage, "Đóng");
                    }
                }
            }
        }
    }
}
