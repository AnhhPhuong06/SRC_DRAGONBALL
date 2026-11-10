
package models.combine.list;

import consts.ConstNpc;
import models.item.Item;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

public class NangCapBongTai4 {

    private static final int ID_BONG_TAI_CAP3 = 1492;
    private static final int ID_MANH_VO_CAP4 = 1622;
    private static final int ID_BONG_TAI_CAP4 = 1624;

    private static final int SO_MANH_CAN = 20000;
    private static final int SO_MANH_MAT_KHI_THAT_BAI = 5000;
    private static final int CHI_PHI_NGOC = 500;
    private static final int TI_LE_THANH_CONG = 70;

    private static Item timItemCombine(Player player, int id) {
        for (Item item : player.combineNew.itemsCombine) {
            if (item != null && item.isNotNullItem()
                    && item.template.id == id) {
                return item;
            }
        }
        return null;
    }

    // Đếm tất cả các chồng vật phẩm ID 1373 trong hành trang.
    private static int demManhVo(Player player) {
        int tong = 0;

        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.isNotNullItem()
                    && item.template.id == ID_MANH_VO_CAP4) {
                tong += item.quantity;
            }
        }

        return tong;
    }

    // Trừ số lượng vật phẩm theo quantity, không dùng option.
    private static boolean truManhVo(Player player, int soLuong) {
        if (soLuong <= 0 || demManhVo(player) < soLuong) {
            return false;
        }

        int conCanTru = soLuong;

        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item item = player.inventory.itemsBag.get(i);

            if (item == null || !item.isNotNullItem()
                    || item.template.id != ID_MANH_VO_CAP4) {
                continue;
            }

            int tru = Math.min(item.quantity, conCanTru);
            item.quantity -= tru;
            conCanTru -= tru;

            if (item.quantity <= 0) {
                player.inventory.itemsBag.set(i, ItemService.gI().createItemNull());
            }

            if (conCanTru == 0) {
                break;
            }
        }

        return conCanTru == 0;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            hienThiThieuVatPham(player);
            return;
        }

        Item bongTai3 = timItemCombine(player, ID_BONG_TAI_CAP3);
        Item manhVo = timItemCombine(player, ID_MANH_VO_CAP4);

        if (bongTai3 == null || manhVo == null) {
            hienThiThieuVatPham(player);
            return;
        }

        if (bongTai3.itemOptions == null
                || bongTai3.itemOptions.size() < 2) {
            CombineService.gI().baHatMit.createOtherMenu(
                    player, ConstNpc.IGNORE_MENU,
                    "Bông tai Porata cấp 3 chưa được mở chỉ số.",
                    "Đóng");
            return;
        }

        int soLuongManhVo = demManhVo(player);

        player.combineNew.gemCombine = CHI_PHI_NGOC;
        player.combineNew.ratioCombine = TI_LE_THANH_CONG;

        StringBuilder npcSay = new StringBuilder();
        npcSay.append("|2|Nâng cấp Bông tai Porata [+4]\n");
        npcSay.append("|2|Tỉ lệ thành công: ")
                .append(TI_LE_THANH_CONG).append("%\n");
        npcSay.append(soLuongManhVo >= SO_MANH_CAN ? "|2|" : "|7|")
                .append("Cần ").append(SO_MANH_CAN)
                .append(" ").append(manhVo.template.name).append("\n");
        npcSay.append(player.inventory.getGemAndRuby() >= CHI_PHI_NGOC
                ? "|2|" : "|7|")
                .append("Cần: ").append(CHI_PHI_NGOC).append(" ngọc\n");
        npcSay.append("|7|Thất bại mất ")
                .append(SO_MANH_MAT_KHI_THAT_BAI)
                .append(" ").append(manhVo.template.name);

        if (soLuongManhVo < SO_MANH_CAN) {
            npcSay.append("\nCòn thiếu ")
                    .append(SO_MANH_CAN - soLuongManhVo)
                    .append(" ").append(manhVo.template.name);

            CombineService.gI().baHatMit.createOtherMenu(
                    player, ConstNpc.IGNORE_MENU,
                    npcSay.toString(), "Đóng");
        } else if (player.inventory.getGemAndRuby() < CHI_PHI_NGOC) {
            npcSay.append("\nKhông đủ ngọc.");

            CombineService.gI().baHatMit.createOtherMenu(
                    player, ConstNpc.IGNORE_MENU,
                    npcSay.toString(), "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(
                    player, ConstNpc.MENU_START_COMBINE,
                    npcSay.toString(),
                    "Nâng cấp\n" + CHI_PHI_NGOC + " ngọc",
                    "Từ chối");
        }
    }

    public static void nangCapBongTai4(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            Service.gI().sendThongBao(player, "Nguyên liệu không hợp lệ.");
            return;
        }

        Item bongTai3 = timItemCombine(player, ID_BONG_TAI_CAP3);
        Item manhVo = timItemCombine(player, ID_MANH_VO_CAP4);

        if (bongTai3 == null || manhVo == null) {
            Service.gI().sendThongBao(player,
                    "Cần 1 bông tai cấp 3 và mảnh vỡ cấp 4.");
            return;
        }

        if (bongTai3.itemOptions == null
                || bongTai3.itemOptions.size() < 2) {
            Service.gI().sendThongBao(player,
                    "Bông tai Porata cấp 3 chưa được mở chỉ số.");
            return;
        }

        if (demManhVo(player) < SO_MANH_CAN) {
            Service.gI().sendThongBao(player,
                    "Không đủ " + SO_MANH_CAN + " mảnh vỡ cấp 4.");
            return;
        }

        if (player.inventory.getGemAndRuby() < CHI_PHI_NGOC) {
            Service.gI().sendThongBao(player, "Không đủ ngọc.");
            return;
        }

        if (ItemService.gI().getTemplate(ID_BONG_TAI_CAP4) == null) {
            Service.gI().sendThongBao(player,
                    "Không tìm thấy Mảnh vỡ bông tai cấp 4");
            return;
        }

        // Kiểm tra tất cả điều kiện trước khi trừ tài nguyên.
        if (player.inventory.gem >= CHI_PHI_NGOC) {
            player.inventory.gem -= CHI_PHI_NGOC;
        } else {
            int rubyCanTru = CHI_PHI_NGOC - player.inventory.gem;

            if (player.inventory.ruby < rubyCanTru) {
                Service.gI().sendThongBao(player, "Không đủ ngọc.");
                return;
            }

            player.inventory.gem = 0;
            player.inventory.ruby -= rubyCanTru;
        }

        if (Util.isTrue(TI_LE_THANH_CONG, 100)) {
            // Trừ đủ 20.000 mảnh trước khi đổi bông tai.
            if (!truManhVo(player, SO_MANH_CAN)) {
                Service.gI().sendThongBao(player,
                        "Không thể trừ mảnh vỡ, vui lòng thử lại.");
                return;
            }

            bongTai3.template = ItemService.gI()
                    .getTemplate(ID_BONG_TAI_CAP4);

            boolean daCoOptionCap = false;

            for (Item.ItemOption option : bongTai3.itemOptions) {
                if (option.optionTemplate.id == 72) {
                    option.param = 4;
                    daCoOptionCap = true;
                    break;
                }
            }

            if (!daCoOptionCap) {
                bongTai3.itemOptions.add(
                        new Item.ItemOption(72, 4));
            }

            bongTai3.info = bongTai3.getInfo();
            bongTai3.content = bongTai3.getContent();

            CombineService.gI().sendEffectSuccessCombine(player);
        } else {
            // Thất bại mất 5.000 mảnh, giữ nguyên bông tai cấp 3.
            if (!truManhVo(player, SO_MANH_MAT_KHI_THAT_BAI)) {
                Service.gI().sendThongBao(player,
                        "Không thể trừ mảnh vỡ, vui lòng thử lại.");
                return;
            }

            CombineService.gI().sendEffectFailCombine(player);
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static void hienThiThieuVatPham(Player player) {
        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.IGNORE_MENU,
                "Cần 1 Bông tai Porata cấp 3 "
                + "và 20.000 mảnh vỡ bông tai 4",
                "Đóng");
    }
}
