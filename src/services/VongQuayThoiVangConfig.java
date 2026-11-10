package services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import models.Template.ItemTemplate;
import services.func.MocVongQuayService;
import utils.Logger;
import utils.Util;

/**
 * Quản lý & cấu hình Vòng Quay Thỏi Vàng & Mốc Thưởng Thượng Đế.
 * Hỗ trợ chỉnh sửa số lượt mốc và tự do thay thế danh sách trang bị/vật phẩm/chỉ số ở từng mốc.
 */
public class VongQuayThoiVangConfig {

    private static final String CONFIG_PATH = "data/config/vong_quay_thoi_vang.properties";

    // Preset Options thông dụng
    public static final String PRESET_KHOA = "30-0";
    public static final String PRESET_CAI_TRANG_30 = "50-30;77-30;103-30;5-25;14-15;95-12;96-12;97-15;30-0";
    public static final String PRESET_CAI_TRANG_35 = "50-35;77-35;103-35;5-35;14-20;95-18;96-18;97-25;108-20;106-0;30-0";

    // 0. Vật phẩm tiêu hao mỗi lượt quay (Mặc định 821: Vé quay)
    public static int idVatPhamQuay = 821;
    public static int soLuongVatPhamQuay = 1;

    // 1. Tỉ lệ & Mốc mở quà VIP vòng quay
    public static int mocMoQuaHiem = 2000;
    public static int tiLeMoiMonSieuHiem = 1; // 1% mỗi món
    public static int tiLeNhom5 = 5;          // 5%
    public static int tiLeNhom15 = 15;        // 15%
    public static int soLuongHongNgoc = 5;
    public static int vangMin = 5000000;
    public static int vangMax = 15000000;

    // 2. Danh sách ID vật phẩm các nhóm vòng quay
    public static int[] idNhom15 = {20};
    public static int[] idNhom5 = {18, 19};
    public static int[] idNhom5SauMoc = {18, 19, 17};
    public static int[] idNhomSieuHiem = {1357};
    public static int[] idNhomSieuHiemSauMoc = {1357, 1358, 1359};
    public static int[] idVatPhamBoTro = {381, 382, 383, 384, 385};

    // 3. Mốc thưởng Thượng Đế (Số lượt và Chuỗi phần thưởng linh hoạt)
    public static int moc1Spins = 200;
    public static String moc1RewardsStr = "457:100:30-0,1099:5:30-0,1100:5:30-0,1102:5:30-0";

    public static int moc2Spins = 700;
    public static String moc2RewardsStr = "457:200:30-0, 16:5:30-0";

    public static int moc3Spins = 1000;
    public static String moc3RewardsStr = "1241:1:50-30;77-40;103-40;30-0, 1618:5:30-0";

    public static int moc4Spins = 1500;
    public static String moc4RewardsStr = "1989:1:50-25;77-35;103-35;5-40;14-10;30-0, 1591:200:30-0";

    // Legacy fields for backward compatibility
    public static int moc1ThoiVang = 100;
    public static int moc1ItemId = 1618;
    public static int moc1ItemQuantity = 5;

    public static int moc2ThoiVang = 200;
    public static int moc2ItemId = 16;
    public static int moc2ItemQuantity = 5;

    public static int moc3ThoiVang = 3000;
    public static int moc3DaBaoVe = 800;
    public static int moc3ManhKhi = 500;
    public static int moc3RuongMTS = 120;
    public static int moc3CaiTrangId = 1593;

    public static int moc4ThoiVang = 5000;
    public static int moc4DaBaoVe = 1500;
    public static int moc4ManhKhi = 1000;
    public static int moc4RuongMTS = 250;
    public static int moc4CaiTrangId = 1241;

    static {
        load();
    }

    public static void init() {
        load();
    }

    public static synchronized int[] spinPrice() {
        return new int[]{idVatPhamQuay, soLuongVatPhamQuay};
    }

    public static synchronized void load() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            save();
            return;
        }
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            prop.load(fis);

            mocMoQuaHiem = getInt(prop, "moc_mo_qua_hiem", mocMoQuaHiem);
            idVatPhamQuay = getInt(prop, "id_vat_pham_quay", idVatPhamQuay);
            soLuongVatPhamQuay = getInt(prop, "so_luong_vat_pham_quay", soLuongVatPhamQuay);
            tiLeMoiMonSieuHiem = getInt(prop, "ti_le_moi_mon_sieu_hiem", tiLeMoiMonSieuHiem);
            tiLeNhom5 = getInt(prop, "ti_le_nhom_5", tiLeNhom5);
            tiLeNhom15 = getInt(prop, "ti_le_nhom_15", tiLeNhom15);
            soLuongHongNgoc = getInt(prop, "so_luong_hong_ngoc", soLuongHongNgoc);
            vangMin = getInt(prop, "vang_min", vangMin);
            vangMax = getInt(prop, "vang_max", vangMax);

            idNhom15 = getIntArray(prop, "id_nhom_15", idNhom15);
            idNhom5 = getIntArray(prop, "id_nhom_5", idNhom5);
            idNhom5SauMoc = getIntArray(prop, "id_nhom_5_sau_moc", idNhom5SauMoc);
            idNhomSieuHiem = getIntArray(prop, "id_nhom_sieu_hiem", idNhomSieuHiem);
            idNhomSieuHiemSauMoc = getIntArray(prop, "id_nhom_sieu_hiem_sau_moc", idNhomSieuHiemSauMoc);
            idVatPhamBoTro = getIntArray(prop, "id_vat_pham_bo_tro", idVatPhamBoTro);

            moc1Spins = getInt(prop, "moc1_spins", moc1Spins);
            if (prop.containsKey("moc1_rewards")) {
                moc1RewardsStr = prop.getProperty("moc1_rewards", moc1RewardsStr).trim();
            } else {
                moc1ThoiVang = getInt(prop, "moc1_thoi_vang", moc1ThoiVang);
                moc1ItemId = getInt(prop, "moc1_item_id", moc1ItemId);
                moc1ItemQuantity = getInt(prop, "moc1_item_quantity", moc1ItemQuantity);
                moc1RewardsStr = "457:" + moc1ThoiVang + ", " + moc1ItemId + ":" + moc1ItemQuantity;
            }

            moc2Spins = getInt(prop, "moc2_spins", moc2Spins);
            if (prop.containsKey("moc2_rewards")) {
                moc2RewardsStr = prop.getProperty("moc2_rewards", moc2RewardsStr).trim();
            } else {
                moc2ThoiVang = getInt(prop, "moc2_thoi_vang", moc2ThoiVang);
                moc2ItemId = getInt(prop, "moc2_item_id", moc2ItemId);
                moc2ItemQuantity = getInt(prop, "moc2_item_quantity", moc2ItemQuantity);
                moc2RewardsStr = "457:" + moc2ThoiVang + ", " + moc2ItemId + ":" + moc2ItemQuantity + ":30-0";
            }

            moc3Spins = getInt(prop, "moc3_spins", moc3Spins);
            if (prop.containsKey("moc3_rewards")) {
                moc3RewardsStr = prop.getProperty("moc3_rewards", moc3RewardsStr).trim();
            } else {
                moc3ThoiVang = getInt(prop, "moc3_thoi_vang", moc3ThoiVang);
                moc3DaBaoVe = getInt(prop, "moc3_da_bao_ve", moc3DaBaoVe);
                moc3ManhKhi = getInt(prop, "moc3_manh_khi", moc3ManhKhi);
                moc3RuongMTS = getInt(prop, "moc3_ruong_mts", moc3RuongMTS);
                moc3CaiTrangId = getInt(prop, "moc3_cai_trang_id", moc3CaiTrangId);
                moc3RewardsStr = "457:" + moc3ThoiVang + ", 987:" + moc3DaBaoVe + ", 1277:" + moc3ManhKhi + ", 1177:" + moc3RuongMTS + ", " + moc3CaiTrangId + ":1:" + PRESET_CAI_TRANG_30;
            }

            moc4Spins = getInt(prop, "moc4_spins", moc4Spins);
            if (prop.containsKey("moc4_rewards")) {
                moc4RewardsStr = prop.getProperty("moc4_rewards", moc4RewardsStr).trim();
            } else {
                moc4ThoiVang = getInt(prop, "moc4_thoi_vang", moc4ThoiVang);
                moc4DaBaoVe = getInt(prop, "moc4_da_bao_ve", moc4DaBaoVe);
                moc4ManhKhi = getInt(prop, "moc4_manh_khi", moc4ManhKhi);
                moc4RuongMTS = getInt(prop, "moc4_ruong_mts", moc4RuongMTS);
                moc4CaiTrangId = getInt(prop, "moc4_cai_trang_id", moc4CaiTrangId);
                moc4RewardsStr = "457:" + moc4ThoiVang + ", 987:" + moc4DaBaoVe + ", 1277:" + moc4ManhKhi + ", 1177:" + moc4RuongMTS + ", " + moc4CaiTrangId + ":1:" + PRESET_CAI_TRANG_35;
            }

            syncToModules();
        } catch (Exception e) {
            Logger.error("Lỗi khi load " + CONFIG_PATH + ": " + e.getMessage() + "\n");
        }
    }

    public static synchronized boolean save() {
        Properties prop = new Properties();
        try {
            File dir = new File("data/config");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            prop.setProperty("id_vat_pham_quay", String.valueOf(idVatPhamQuay));
            prop.setProperty("so_luong_vat_pham_quay", String.valueOf(soLuongVatPhamQuay));
            prop.setProperty("moc_mo_qua_hiem", String.valueOf(mocMoQuaHiem));
            prop.setProperty("ti_le_moi_mon_sieu_hiem", String.valueOf(tiLeMoiMonSieuHiem));
            prop.setProperty("ti_le_nhom_5", String.valueOf(tiLeNhom5));
            prop.setProperty("ti_le_nhom_15", String.valueOf(tiLeNhom15));
            prop.setProperty("so_luong_hong_ngoc", String.valueOf(soLuongHongNgoc));
            prop.setProperty("vang_min", String.valueOf(vangMin));
            prop.setProperty("vang_max", String.valueOf(vangMax));

            prop.setProperty("id_nhom_15", intArrayToString(idNhom15));
            prop.setProperty("id_nhom_5", intArrayToString(idNhom5));
            prop.setProperty("id_nhom_5_sau_moc", intArrayToString(idNhom5SauMoc));
            prop.setProperty("id_nhom_sieu_hiem", intArrayToString(idNhomSieuHiem));
            prop.setProperty("id_nhom_sieu_hiem_sau_moc", intArrayToString(idNhomSieuHiemSauMoc));
            prop.setProperty("id_vat_pham_bo_tro", intArrayToString(idVatPhamBoTro));

            prop.setProperty("moc1_spins", String.valueOf(moc1Spins));
            prop.setProperty("moc1_rewards", moc1RewardsStr);

            prop.setProperty("moc2_spins", String.valueOf(moc2Spins));
            prop.setProperty("moc2_rewards", moc2RewardsStr);

            prop.setProperty("moc3_spins", String.valueOf(moc3Spins));
            prop.setProperty("moc3_rewards", moc3RewardsStr);

            prop.setProperty("moc4_spins", String.valueOf(moc4Spins));
            prop.setProperty("moc4_rewards", moc4RewardsStr);

            try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH)) {
                prop.store(fos, "Cau hinh Vong Quay Thoi Vang & Moc Thuong Thuong De (Cap nhat qua Admin Panel)");
            }
            syncToModules();
            return true;
        } catch (Exception e) {
            Logger.error("Lỗi khi lưu " + CONFIG_PATH + ": " + e.getMessage() + "\n");
            return false;
        }
    }


    public static void syncToModules() {
        try {
            MocVongQuayService.reloadMocFromConfig();
        } catch (Exception ignored) {
        }
    }

    public static void resetDefaults() {
        idVatPhamQuay = 821;
        soLuongVatPhamQuay = 1;
        mocMoQuaHiem = 2000;
        tiLeMoiMonSieuHiem = 1;
        tiLeNhom5 = 5;
        tiLeNhom15 = 15;
        soLuongHongNgoc = 5;
        vangMin = 5_000_000;
        vangMax = 15_000_000;

        idNhom15 = new int[]{20, 987, 1338};
        idNhom5 = new int[]{18, 19};
        idNhom5SauMoc = new int[]{18, 19, 1959, 1454, 674, 17};
        idNhomSieuHiem = new int[]{1357};
        idNhomSieuHiemSauMoc = new int[]{1357, 1358, 1359};
        idVatPhamBoTro = new int[]{381, 382, 383, 384, 385};

        moc1Spins = 3000;
        moc1RewardsStr = "457:100, 1618:5";

        moc2Spins = 8000;
        moc2RewardsStr = "457:200, 16:5:30-0";

        moc3Spins = 15000;
        moc3RewardsStr = "457:3000, 987:800, 1277:500, 1177:120, 1593:1:50-30;77-30;103-30;5-25;14-15;95-12;96-12;97-15;30-0";

        moc4Spins = 20000;
        moc4RewardsStr = "457:5000, 987:1500, 1277:1000, 1177:250, 1241:1:50-35;77-35;103-35;5-35;14-20;95-18;96-18;97-25;108-20;106-0;30-0";
    }

    public static List<MocVongQuayService.RewardItem> parseRewards(String str) {
        List<MocVongQuayService.RewardItem> list = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) {
            return list;
        }
        String[] items = str.replace("\n", ",").split(",");
        for (String itemStr : items) {
            String s = itemStr.trim();
            if (s.isEmpty()) continue;
            try {
                String[] parts = s.split(":");
                int tempId = Integer.parseInt(parts[0].trim());
                int quantity = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
                int[][] options = null;
                if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                    String[] optParts = parts[2].trim().split(";");
                    List<int[]> optList = new ArrayList<>();
                    for (String op : optParts) {
                        String opClean = op.trim();
                        if (opClean.isEmpty()) continue;
                        String[] pair = opClean.split("-");
                        int optId = Integer.parseInt(pair[0].trim());
                        int param = pair.length > 1 ? Integer.parseInt(pair[1].trim()) : 0;
                        optList.add(new int[]{optId, param});
                    }
                    if (!optList.isEmpty()) {
                        options = optList.toArray(new int[0][]);
                    }
                }
                list.add(new MocVongQuayService.RewardItem(tempId, quantity, options));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    public static String rewardsToString(List<MocVongQuayService.RewardItem> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            MocVongQuayService.RewardItem item = list.get(i);
            sb.append(item.tempId).append(":").append(item.quantity);
            if (item.options != null && item.options.length > 0) {
                sb.append(":");
                for (int j = 0; j < item.options.length; j++) {
                    if (j > 0) sb.append(";");
                    sb.append(item.options[j][0]).append("-").append(item.options[j][1]);
                }
            }
        }
        return sb.toString();
    }

    public static String getItemName(int tempId) {
        try {
            ItemTemplate temp = ItemService.gI().getTemplate(tempId);
            if (temp != null && temp.name != null && !temp.name.trim().isEmpty()) {
                return temp.name;
            }
        } catch (Exception ignored) {
        }
        return switch (tempId) {
            case 457 -> "Thỏi vàng";
            case 1618 -> "Hộp Ván Bay";
            case 987 -> "Đá bảo vệ";
            case 1277 -> "Mảnh khỉ vàng";
            case 1177 -> "Rương Mảnh Thiên Sứ";
            case 1593 -> "Cải Trang Broly SSJ4";
            case 1241 -> "Cải Trang Broly SSJ4 VIP";
            case 16 -> "Ngọc Rồng 3 Sao";
            case 17 -> "Ngọc Rồng 4 Sao";
            case 18 -> "Ngọc Rồng 5 Sao";
            case 19 -> "Ngọc Rồng 6 Sao";
            case 20 -> "Ngọc Rồng 7 Sao";
            default -> "Vật phẩm [" + tempId + "]";
        };
    }

    public static String getOptionShortName(int optId, int param) {
        return switch (optId) {
            case 50 -> "SĐ +" + param + "%";
            case 77 -> "HP +" + param + "%";
            case 103 -> "KI +" + param + "%";
            case 5 -> "STCM +" + param + "%";
            case 14 -> "TLCM +" + param + "%";
            case 95 -> "Hút HP " + param + "%";
            case 96 -> "Hút KI " + param + "%";
            case 97 -> "Phản ST " + param + "%";
            case 108 -> "Né đòn " + param + "%";
            case 106 -> "Không bị bẫy";
            case 30 -> "Khóa";
            case 0 -> "SĐ +" + param;
            case 6 -> "HP +" + param;
            case 7 -> "KI +" + param;
            case 93 -> "HSD: " + param + " ngày";
            case 86 -> "Không thể ký gửi";
            case 87 -> "Có thể ký gửi";
            default -> "Opt " + optId + (param > 0 ? " (+" + param + ")" : "");
        };
    }

    public static String formatPreviewRewards(String rewardsStr) {
        List<MocVongQuayService.RewardItem> list = parseRewards(rewardsStr);
        if (list.isEmpty()) {
            return "<html><span style='color:#94a3b8; font-style:italic;'>Chưa có vật phẩm thưởng nào được cấu hình.</span></html>";
        }
        StringBuilder sb = new StringBuilder("<html><body style='font-family:Segoe UI, sans-serif; font-size:12px; color:#1e293b;'>");
        sb.append("<b style='color:#059669;'>📦 Xem trước quà thực tế: </b>");
        List<String> itemsDisplay = new ArrayList<>();
        for (MocVongQuayService.RewardItem item : list) {
            String name = getItemName(item.tempId);
            StringBuilder isb = new StringBuilder();
            isb.append("<b style='color:#1e293b;'>x").append(Util.numberToMoney(item.quantity)).append(" ").append(name).append("</b>");
            if (item.options != null && item.options.length > 0) {
                isb.append(" <span style='color:#7c3aed; font-size:11px;'>[");
                List<String> optTexts = new ArrayList<>();
                for (int[] opt : item.options) {
                    optTexts.add(getOptionShortName(opt[0], opt[1]));
                }
                isb.append(String.join(", ", optTexts));
                isb.append("]</span>");
            }
            itemsDisplay.add(isb.toString());
        }
        sb.append(String.join(" <span style='color:#cbd5e1;'>|</span> ", itemsDisplay));
        sb.append("</body></html>");
        return sb.toString();
    }

    public static String intArrayToString(int[] arr) {
        if (arr == null || arr.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    public static int[] parseStringToIntArray(String str, int[] def) {
        if (str == null || str.trim().isEmpty()) return def;
        try {
            String[] parts = str.split(",");
            int[] arr = new int[parts.length];
            int count = 0;
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    arr[count++] = Integer.parseInt(clean);
                }
            }
            return Arrays.copyOf(arr, count);
        } catch (Exception e) {
            return def;
        }
    }

    private static int getInt(Properties prop, String key, int def) {
        if (prop.containsKey(key)) {
            try {
                return Integer.parseInt(prop.getProperty(key).trim());
            } catch (Exception ignored) {
            }
        }
        return def;
    }

    private static int[] getIntArray(Properties prop, String key, int[] def) {
        if (prop.containsKey(key)) {
            return parseStringToIntArray(prop.getProperty(key), def);
        }
        return def;
    }
}
