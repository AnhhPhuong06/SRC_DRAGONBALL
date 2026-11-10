package models.combine.list;

import consts.ConstFont;
import consts.ConstItem;
import consts.ConstNpc;
import java.util.ArrayList;
import java.util.List;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.CombineService;
import services.Service;
import services.player.InventoryService;
import utils.Util;


public class NangCapZeno {

    private static final int PET_CAN_VE_ZENO = 1976;
    private static final int CAI_TRANG_ZENO = 1967;
    private static final int THOI_VANG_MOI_MON = 200;
    /** Tỉ lệ đập thành công mỗi cấp, dùng chung cho cả lời thoại NPC lẫn lượt random. */
    public static int TI_LE_THANH_CONG = services.CombineRateConfig.rateZeno; // %
    /** Số cấp tối đa của một món. */
    public static final int CAP_TOI_DA = 5;
    private static final int NGOI_SAO = 1217;
    private static final int NGOI_SAO_TAY_MOI_MON = 1;
    private static final int THOI_VANG_TAY_MOI_MON = 200;
    /** Option 'Số lần nâng cấp còn lại: #' - vừa là mốc đánh dấu, vừa là nơi suy ra cấp. */
    private static final int OPT_LUOT_CON_LAI = 221;
    /**
     * Option ẩn (tên rỗng) giữ số dòng chỉ số đập, đặt ngay sau dòng đếm lượt. Cần vì chỉ
     * số cùng loại được gộp nên số dòng không còn bằng số cấp nữa.
     */
    private static final int OPT_SO_DONG = 206;
    /** Mỗi cấp bốc ngẫu nhiên một trong bốn chỉ số này. */
    private static final int[] CHI_SO_DAP = {50, 77, 103, 5};
    private static final int BONUS_MIN = 5;
    private static final int BONUS_MAX = 13;

    private static final String THIEU_MON_NANG = "Hãy bỏ Pet Cận vệ Zeno hoặc Cải trang Zeno\n"
            + "cùng " + THOI_VANG_MOI_MON + " thỏi vàng vào ô.";
    private static final String THIEU_MON_TAY = "Hãy bỏ Pet Cận vệ Zeno hoặc Cải trang Zeno\n"
            + "đã nâng cấp, cùng " + NGOI_SAO_TAY_MOI_MON + " Ngôi sao và "
            + THOI_VANG_TAY_MOI_MON + " thỏi vàng vào ô.";

    public static void showInfoCombine(Player player) {
        if (!sanSang(player)) {
            return;
        }
        List<Item> dsMon = layMonZeno(player);
        if (dsMon.isEmpty()) {
            nhacBoDo(player, THIEU_MON_NANG);
            return;
        }

        StringBuilder text = new StringBuilder();
        List<Item> canDap = new ArrayList<>();
        for (Item item : dsMon) {
            inTenVaChiSo(text, item);
            if (capHienTai(item) >= CAP_TOI_DA) {
                text.append(ConstFont.BOLD_RED).append("(đã đạt cấp tối đa)\n");
            } else {
                canDap.add(item);
            }
            text.append("\n");
        }

        if (canDap.isEmpty()) {
            moMenu(player, ConstNpc.IGNORE_MENU, text, "Đóng");
            return;
        }

        int canThoiVang = THOI_VANG_MOI_MON * tongSoLuong(canDap);
        int coThoiVang = soLuongTrongO(player, ConstItem.THOI_VANG);

        text.append("Mỗi cấp thành công bốc ngẫu nhiên một trong\n")
                .append(tenChiSo(50)).append(", ").append(tenChiSo(77)).append(", ")
                .append(tenChiSo(103)).append(", ").append(tenChiSo(5))
                .append(" với mức +").append(BONUS_MIN).append("% đến +").append(BONUS_MAX);
        text.append("\n").append(ConstFont.BOLD_RED)
                .append("Tỉ lệ thành công mỗi cấp: ").append(TI_LE_THANH_CONG).append("%\n")
                .append(ConstFont.BOLD_RED).append("Tối đa ").append(CAP_TOI_DA).append(" cấp mỗi món\n")
                .append(ConstFont.BOLD_RED).append("Thất bại vẫn mất thỏi vàng, cấp giữ nguyên\n");
        inDongNguyenLieu(text, "Thỏi vàng", coThoiVang, canThoiVang);

        if (coThoiVang < canThoiVang) {
            moMenu(player, ConstNpc.IGNORE_MENU, text, "Bỏ thêm thỏi vàng vào ô");
            return;
        }
        moMenu(player, ConstNpc.MENU_START_COMBINE, text,
                "Nâng 1 cấp\n" + canThoiVang + " thỏi vàng", "Từ chối");
    }

    public static void nangCap(Player player) {
        if (!sanSang(player)) {
            return;
        }
        List<Item> canDap = locConLuotNang(player);
        if (canDap.isEmpty()) {
            Service.gI().sendThongBaoOK(player,
                    "Hãy bỏ Pet Cận vệ Zeno hoặc Cải trang Zeno chưa đạt cấp "
                    + CAP_TOI_DA + " vào ô.");
            return;
        }

        int canThoiVang = THOI_VANG_MOI_MON * tongSoLuong(canDap);
        if (soLuongTrongO(player, ConstItem.THOI_VANG) < canThoiVang) {
            Service.gI().sendThongBaoOK(player,
                    "Cần " + canThoiVang + " thỏi vàng trong ô để nâng cấp!");
            return;
        }
        truNguyenLieu(player, ConstItem.THOI_VANG, canThoiVang);

        boolean coThanhCong = false;
        StringBuilder ketQua = new StringBuilder();
        for (Item item : canDap) {
            ketQua.append(item.template.name).append(": ");
            if (Util.isTrue(TI_LE_THANH_CONG, 100)) {
                coThanhCong = true;
                int optionId = bocChiSo(item);
                int bonus = Util.nextInt(BONUS_MIN, BONUS_MAX);
                themCap(item, optionId, bonus);
                ketQua.append("lên cấp ").append(capHienTai(item)).append("/").append(CAP_TOI_DA)
                        .append(", ").append(tenChiSo(optionId)).append(" +").append(bonus)
                        .append("% (tổng +").append(tongChiSoDap(item, optionId)).append("%)\n");
            } else {
                ketQua.append("thất bại, giữ cấp ").append(capHienTai(item))
                        .append("/").append(CAP_TOI_DA).append("\n");
            }
            lamMoiItem(item);
        }

        if (coThanhCong) {
            CombineService.gI().sendEffectSuccessCombine(player);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
        }
        ketThuc(player, ketQua);
    }

    public static void showInfoTay(Player player) {
        if (!sanSang(player)) {
            return;
        }
        List<Item> dsMon = layMonZeno(player);
        if (dsMon.isEmpty()) {
            nhacBoDo(player, THIEU_MON_TAY);
            return;
        }

        StringBuilder text = new StringBuilder();
        List<Item> canTay = new ArrayList<>();
        for (Item item : dsMon) {
            text.append(ConstFont.BOLD_BLUE).append(item.template.name).append("\n");
            if (capHienTai(item) <= 0) {
                text.append(ConstFont.BOLD_RED).append("(chưa nâng cấp, không có gì để tẩy)\n\n");
                continue;
            }
            canTay.add(item);
            List<Item.ItemOption> bonus = layBonusDaDap(item);
            if (bonus.isEmpty()) {
                text.append(ConstFont.BOLD_RED).append("(nâng cấp từ bản cũ, chỉ mở lại lượt nâng)\n\n");
                continue;
            }
            text.append("Chỉ số sẽ bị tẩy:\n");
            for (Item.ItemOption io : bonus) {
                text.append("- ").append(tenChiSo(io.optionTemplate.id)).append(": -")
                        .append(io.param).append("%\n");
            }
            text.append("\n");
        }

        if (canTay.isEmpty()) {
            moMenu(player, ConstNpc.IGNORE_MENU, text, "Đóng");
            return;
        }

        int canNgoiSao = NGOI_SAO_TAY_MOI_MON * tongSoLuong(canTay);
        int canThoiVang = THOI_VANG_TAY_MOI_MON * tongSoLuong(canTay);
        int coNgoiSao = soLuongTrongO(player, NGOI_SAO);
        int coThoiVang = soLuongTrongO(player, ConstItem.THOI_VANG);

        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 100%\n");
        inDongNguyenLieu(text, "Ngôi sao", coNgoiSao, canNgoiSao);
        text.append("\n");
        inDongNguyenLieu(text, "Thỏi vàng", coThoiVang, canThoiVang);
        text.append("\n").append(ConstFont.BOLD_RED)
                .append("Tẩy xong món đồ về cấp 0 với chỉ số gốc\nvà nâng lại được từ đầu");

        if (coNgoiSao < canNgoiSao || coThoiVang < canThoiVang) {
            moMenu(player, ConstNpc.IGNORE_MENU, text, "Bỏ thêm nguyên liệu vào ô");
            return;
        }
        moMenu(player, ConstNpc.MENU_START_COMBINE, text,
                "Tẩy chỉ số\n" + canNgoiSao + " ngôi sao\n" + canThoiVang + " thỏi vàng", "Từ chối");
    }

    public static void tayChiSo(Player player) {
        if (!sanSang(player)) {
            return;
        }
        List<Item> canTay = locDaNangCap(player);
        if (canTay.isEmpty()) {
            Service.gI().sendThongBaoOK(player,
                    "Hãy bỏ Pet Cận vệ Zeno hoặc Cải trang Zeno đã nâng cấp vào ô để tẩy.");
            return;
        }

        int canNgoiSao = NGOI_SAO_TAY_MOI_MON * tongSoLuong(canTay);
        int canThoiVang = THOI_VANG_TAY_MOI_MON * tongSoLuong(canTay);
        if (soLuongTrongO(player, NGOI_SAO) < canNgoiSao
                || soLuongTrongO(player, ConstItem.THOI_VANG) < canThoiVang) {
            Service.gI().sendThongBaoOK(player,
                    "Cần " + canNgoiSao + " Ngôi sao và " + canThoiVang + " thỏi vàng trong ô để tẩy!");
            return;
        }
        truNguyenLieu(player, NGOI_SAO, canNgoiSao);
        truNguyenLieu(player, ConstItem.THOI_VANG, canThoiVang);

        StringBuilder ketQua = new StringBuilder();
        for (Item item : canTay) {
            ketQua.append(item.template.name).append(": ");
            int soDong = xoaBonusVaCap(item);
            if (soDong > 0) {
                ketQua.append("đã tẩy ").append(soDong).append(" dòng, về cấp 0\n");
            } else {
                // Món đập từ bản cũ (chỉ số cộng gộp vào dòng gốc) - không tách ra được
                ketQua.append("nâng cấp từ bản cũ, chỉ mở lại lượt nâng\n");
            }
            lamMoiItem(item);
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        ketThuc(player, ketQua);
    }

    /**
     * Cấp hiện tại của món, 0 là chưa nâng. Suy ra từ số lượt còn lại ghi trên item nên
     * không cần thêm option ẩn nào khác. Món nâng từ bản cũ có sẵn dòng này với số lượt 0
     * nên được coi là đã đạt cấp tối đa, không nâng thêm được.
     */
    private static int capHienTai(Item item) {
        int i = viTriOption(item, OPT_LUOT_CON_LAI);
        if (i < 0) {
            return 0;
        }
        int cap = CAP_TOI_DA - item.itemOptions.get(i).param;
        return Math.max(0, Math.min(CAP_TOI_DA, cap));
    }

    /**
     * Vị trí ô đếm số dòng. Chỉ tính là hợp lệ khi nó nằm ngay sau dòng đếm lượt, để món
     * đập theo bản cũ (cũng mang option 206 nhưng ở trước) không bị đọc nhầm.
     */
    private static int viTriDemDong(Item item) {
        int i = viTriOption(item, OPT_LUOT_CON_LAI);
        if (i < 0 || i + 1 >= item.itemOptions.size()) {
            return -1;
        }
        Item.ItemOption io = item.itemOptions.get(i + 1);
        return io != null && io.optionTemplate != null && io.optionTemplate.id == OPT_SO_DONG
                ? i + 1 : -1;
    }

    /**
     * Các dòng chỉ số đã cộng thêm, nằm liền sau ô đếm số dòng. Số dòng lấy từ ô đếm nên
     * đá pha lê hay chỉ số ép thêm về sau không bị tính nhầm. Món đập theo bản cũ không có
     * ô đếm thì lấy tạm theo số cấp, mỗi cấp một dòng như luật cũ.
     */
    private static List<Item.ItemOption> layBonusDaDap(Item item) {
        List<Item.ItemOption> ds = new ArrayList<>();
        int i = viTriOption(item, OPT_LUOT_CON_LAI);
        if (i < 0) {
            return ds;
        }
        int dem = viTriDemDong(item);
        int batDau = (dem >= 0 ? dem : i) + 1;
        int soDong = dem >= 0 ? item.itemOptions.get(dem).param : capHienTai(item);
        for (int k = 0; k < soDong && batDau + k < item.itemOptions.size(); k++) {
            Item.ItemOption io = item.itemOptions.get(batDau + k);
            if (io == null || io.optionTemplate == null || !laChiSoDap(io.optionTemplate.id)) {
                break;
            }
            ds.add(io);
        }
        return ds;
    }

    /**
     * Lên một cấp: bớt một lượt còn lại, chỉ số trùng loại thì cộng dồn vào đúng dòng đang
     * có, loại mới thì thêm một dòng vào cuối khối. Mỗi món nhiều nhất 4 dòng chỉ số đập.
     */
    private static void themCap(Item item, int optionId, int bonus) {
        int i = viTriOption(item, OPT_LUOT_CON_LAI);
        if (i < 0) {
            item.itemOptions.add(new Item.ItemOption(OPT_LUOT_CON_LAI, CAP_TOI_DA - 1));
            item.itemOptions.add(new Item.ItemOption(OPT_SO_DONG, 1));
            item.itemOptions.add(new Item.ItemOption(optionId, bonus));
            return;
        }
        List<Item.ItemOption> bonusHienCo = layBonusDaDap(item);
        for (Item.ItemOption io : bonusHienCo) {
            if (io.optionTemplate.id == optionId) {
                io.param += bonus;
                giamLuot(item);
                return;
            }
        }
        int dem = viTriDemDong(item);
        if (dem < 0) {
            item.itemOptions.add(i + 1, new Item.ItemOption(OPT_SO_DONG, bonusHienCo.size()));
            dem = i + 1;
        }
        // Chèn theo số dòng thật đã kiểm, không theo số ghi trong ô đếm: nếu dữ liệu cũ
        // lệch nhau thì vẫn chèn đúng chỗ thay vì ném IndexOutOfBounds, và ô đếm được ghi
        // lại cho khớp thực tế.
        int soDong = bonusHienCo.size();
        int viTriChen = Math.min(dem + 1 + soDong, item.itemOptions.size());
        item.itemOptions.add(viTriChen, new Item.ItemOption(optionId, bonus));
        item.itemOptions.get(dem).param = soDong + 1;
        giamLuot(item);
    }

    private static void giamLuot(Item item) {
        int i = viTriOption(item, OPT_LUOT_CON_LAI);
        if (i >= 0) {
            Item.ItemOption io = item.itemOptions.get(i);
            io.param = Math.max(0, io.param - 1);
        }
    }

    /** Gỡ toàn bộ dòng chỉ số đã cộng, ô đếm và dòng đếm lượt. Trả về số dòng đã gỡ. */
    private static int xoaBonusVaCap(Item item) {
        int i = viTriOption(item, OPT_LUOT_CON_LAI);
        if (i < 0) {
            return 0;
        }
        int soDong = layBonusDaDap(item).size();
        int dem = viTriDemDong(item);
        int batDau = (dem >= 0 ? dem : i) + 1;
        for (int k = 0; k < soDong; k++) {
            item.itemOptions.remove(batDau);
        }
        if (dem >= 0) {
            item.itemOptions.remove(dem);
        }
        item.itemOptions.remove(i);
        return soDong;
    }

    /**
     * Bốc chỉ số cho cấp mới. Ưu tiên ngẫu nhiên trong những loại món chưa có, để 4 cấp
     * đầu luôn ra 4 loại khác nhau thay vì dồn hết vào một loại. Đủ cả 4 loại rồi (cấp 5)
     * thì bốc đều trong 4 và cộng dồn vào dòng sẵn có.
     */
    private static int bocChiSo(Item item) {
        List<Integer> conThieu = new ArrayList<>();
        for (int optionId : CHI_SO_DAP) {
            if (tongChiSoDap(item, optionId) == 0) {
                conThieu.add(optionId);
            }
        }
        if (conThieu.isEmpty()) {
            return CHI_SO_DAP[Util.nextInt(CHI_SO_DAP.length)];
        }
        return conThieu.get(Util.nextInt(conThieu.size()));
    }

    /** Tổng chỉ số đập của một loại trên món, sau khi đã gộp thì chỉ có một dòng. */
    private static int tongChiSoDap(Item item, int optionId) {
        int tong = 0;
        for (Item.ItemOption io : layBonusDaDap(item)) {
            if (io.optionTemplate.id == optionId) {
                tong += io.param;
            }
        }
        return tong;
    }

    private static boolean laChiSoDap(int optionId) {
        for (int id : CHI_SO_DAP) {
            if (id == optionId) {
                return true;
            }
        }
        return false;
    }

    private static int viTriOption(Item item, int optionId) {
        for (int i = 0; i < item.itemOptions.size(); i++) {
            Item.ItemOption io = item.itemOptions.get(i);
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Chỉ số gốc của món: chỉ tìm trong phần đứng trước dòng đếm lượt, nếu không sẽ đọc
     * nhầm dòng bonus thành chỉ số gốc khi món vốn không có sẵn dòng đó.
     */
    private static int layParamGoc(Item item, int optionId) {
        int moc = viTriOption(item, OPT_LUOT_CON_LAI);
        int het = moc < 0 ? item.itemOptions.size() : moc;
        for (int i = 0; i < het; i++) {
            Item.ItemOption io = item.itemOptions.get(i);
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                return io.param;
            }
        }
        return 0;
    }

    /**
     * Tổng số món thật, không phải số ô. Pet Cận vệ Zeno chồng được (is_up_to_up = 1) nên
     * một ô có thể chứa cả chồng pet - tính theo ô thì bỏ 10 con vào chỉ mất tiền 1 con
     * mà cả chồng cùng lên cấp.
     */
    private static int tongSoLuong(List<Item> ds) {
        int tong = 0;
        for (Item item : ds) {
            tong += Math.max(1, item.quantity);
        }
        return tong;
    }

    private static boolean sanSang(Player player) {
        return player.combineNew != null && player.combineNew.itemsCombine != null;
    }

    private static void ketThuc(Player player, StringBuilder ketQua) {
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, ketQua.toString().trim());
        donODaTieuHet(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static void donODaTieuHet(Player player) {
        List<Item> trongO = player.combineNew.itemsCombine;
        for (int i = trongO.size() - 1; i >= 0; i--) {
            Item item = trongO.get(i);
            if (item == null || !item.isNotNullItem() || !player.inventory.itemsBag.contains(item)) {
                trongO.remove(i);
            }
        }
    }

    private static void lamMoiItem(Item item) {
        item.info = item.getInfo();
        item.content = item.getContent();
    }

    private static void nhacBoDo(Player player, String loiNhac) {
        moMenu(player, ConstNpc.IGNORE_MENU,
                new StringBuilder(ConstFont.BOLD_RED).append(loiNhac), "Đóng");
    }

    private static void moMenu(Player player, int indexMenu, StringBuilder text, String... menuSelect) {
        Npc npc = player.idMark.getNpcChose();
        if (npc == null) {
            npc = CombineService.gI().baHatMit;
        }
        npc.createOtherMenu(player, indexMenu, text.toString(), menuSelect);
    }

    /**
     * In tách hẳn hai phần: chỉ số gốc của món và phần đập thêm, kèm tổng đã đập theo từng
     * loại để người chơi biết mình được thêm bao nhiêu mà không phải tự cộng.
     */
    private static void inTenVaChiSo(StringBuilder text, Item item) {
        int cap = capHienTai(item);
        text.append(ConstFont.BOLD_BLUE).append(item.template.name)
                .append(" [cấp ").append(cap).append("/").append(CAP_TOI_DA).append("]\n");

        text.append(ConstFont.BOLD_DARK).append("Chỉ số gốc:\n");
        for (int optionId : CHI_SO_DAP) {
            text.append("- ").append(tenChiSo(optionId)).append(": +")
                    .append(layParamGoc(item, optionId)).append("%\n");
        }

        List<Item.ItemOption> bonus = layBonusDaDap(item);
        if (bonus.isEmpty()) {
            text.append(ConstFont.BOLD_DARK).append("Chỉ số đập: chưa có\n");
            return;
        }
        text.append(ConstFont.BOLD_GREEN).append("Chỉ số đập:\n");
        for (Item.ItemOption io : bonus) {
            text.append("+ ").append(tenChiSo(io.optionTemplate.id))
                    .append(" +").append(io.param).append("%\n");
        }
    }

    private static void inDongNguyenLieu(StringBuilder text, String ten, int co, int can) {
        text.append(co >= can ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append(ten).append(" ").append(co).append("/").append(can);
    }

    private static List<Item> layMonZeno(Player player) {
        List<Item> ds = layTrongO(player, PET_CAN_VE_ZENO);
        ds.addAll(layTrongO(player, CAI_TRANG_ZENO));
        return ds;
    }

    private static List<Item> locConLuotNang(Player player) {
        List<Item> ds = new ArrayList<>();
        for (Item item : layMonZeno(player)) {
            if (capHienTai(item) < CAP_TOI_DA) {
                ds.add(item);
            }
        }
        return ds;
    }

    private static List<Item> locDaNangCap(Player player) {
        List<Item> ds = new ArrayList<>();
        for (Item item : layMonZeno(player)) {
            if (capHienTai(item) > 0) {
                ds.add(item);
            }
        }
        return ds;
    }

    private static boolean laItem(Item item, int tempId) {
        return item != null && item.isNotNullItem() && item.template.id == tempId;
    }

    private static List<Item> layTrongO(Player player, int tempId) {
        List<Item> ds = new ArrayList<>();
        for (Item item : player.combineNew.itemsCombine) {
            if (laItem(item, tempId) && chuaCo(ds, item)) {
                ds.add(item);
            }
        }
        return ds;
    }

    private static boolean chuaCo(List<Item> ds, Item item) {
        for (Item it : ds) {
            if (it == item) {
                return false;
            }
        }
        return true;
    }

    private static int soLuongTrongO(Player player, int tempId) {
        int tong = 0;
        for (Item item : layTrongO(player, tempId)) {
            tong += item.quantity;
        }
        return tong;
    }

    private static void truNguyenLieu(Player player, int tempId, int can) {
        for (Item item : layTrongO(player, tempId)) {
            if (can <= 0) {
                break;
            }
            int tru = Math.min(can, item.quantity);
            InventoryService.gI().subQuantityItemsBag(player, item, tru);
            can -= tru;
        }
    }

    private static String tenChiSo(int optionId) {
        return switch (optionId) {
            case 50 -> "Sức đánh";
            case 77 -> "HP";
            case 103 -> "KI";
            case 5 -> "SĐCM";
            default -> "?";
        };
    }
}
