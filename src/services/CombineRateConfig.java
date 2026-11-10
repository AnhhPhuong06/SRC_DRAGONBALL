package services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import utils.Logger;

/**
 * Quản lý & cấu hình tỉ lệ đập đồ của Server
 */
public class CombineRateConfig {

    private static final String CONFIG_PATH = "data/config/combine_rates.properties";

    // 1. Nâng cấp trang bị bằng Đá (+1 đến +8)
    public static double[] rateNangCapDo = {80.0, 50.0, 20.0, 10.0, 5.0, 4.0, 3.0, 2.0};

    // 2. Pha lê hóa trang bị (0 = 1 sao, ..., 8 = 9 sao, 9 = 10 sao)
    public static float[] ratePhaLeHoa = {50.0f, 45.0f, 40.0f, 35.0f, 30.0f, 10.0f, 2.0f, 1.0f, 0.5f, 0.5f};

    // 3. Tỉ lệ ra SKH (SKH 1, SKH 2, SKH 3)
    public static int skhVip1 = 45;
    public static int skhVip2 = 10;
    public static int skhThuong = 45;

    // 4. Các loại đập đồ khác
    public static int rateBongTai2 = 50; // %
    public static int rateBongTai3 = 20; // %
    public static int rateTrungDeTu1 = 80; // Trứng Mabư -> Ubu
    public static int rateTrungDeTu2 = 70; // Trứng Ubu -> Black
    public static int rateTrungDeTu3 = 50; // Trứng Black -> Beerus
    public static int rateZeno = 15; // %

    static {
        load();
    }

    public static void init() {
        load();
    }

    public static double getRateNangCapDo(int level) {
        if (level >= 0 && level < rateNangCapDo.length) {
            return rateNangCapDo[level];
        }
        return 0;
    }

    public static float getRatePhaLeHoa(int star) {
        if (star >= 0 && star < ratePhaLeHoa.length) {
            return ratePhaLeHoa[star];
        }
        return 0f;
    }

    public static int getRateTrungDeTu(int matId) {
        return switch (matId) {
            case 568 -> rateTrungDeTu1;
            case 1521 -> rateTrungDeTu2;
            case 1519 -> rateTrungDeTu3;
            default -> 50;
        };
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
            // 1. Nâng cấp đồ (+1 đến +8)
            for (int i = 0; i < rateNangCapDo.length; i++) {
                String val = prop.getProperty("nang_cap_do_c" + (i + 1));
                if (val != null) {
                    rateNangCapDo[i] = Double.parseDouble(val.trim());
                }
            }
            // 2. Pha lê hóa (1 đến 10 sao)
            for (int i = 0; i < ratePhaLeHoa.length; i++) {
                String val = prop.getProperty("pha_le_hoa_sao_" + (i + 1));
                if (val != null) {
                    ratePhaLeHoa[i] = Float.parseFloat(val.trim());
                }
            }
            // 3. SKH
            if (prop.containsKey("skh_vip1")) {
                skhVip1 = Integer.parseInt(prop.getProperty("skh_vip1").trim());
            }
            if (prop.containsKey("skh_vip2")) {
                skhVip2 = Integer.parseInt(prop.getProperty("skh_vip2").trim());
            }
            if (prop.containsKey("skh_thuong")) {
                skhThuong = Integer.parseInt(prop.getProperty("skh_thuong").trim());
            }

            // 4. Khác
            if (prop.containsKey("rate_bong_tai_2")) {
                rateBongTai2 = Integer.parseInt(prop.getProperty("rate_bong_tai_2").trim());
            }
            if (prop.containsKey("rate_bong_tai_3")) {
                rateBongTai3 = Integer.parseInt(prop.getProperty("rate_bong_tai_3").trim());
            }
            if (prop.containsKey("rate_trung_detu_1")) {
                rateTrungDeTu1 = Integer.parseInt(prop.getProperty("rate_trung_detu_1").trim());
            }
            if (prop.containsKey("rate_trung_detu_2")) {
                rateTrungDeTu2 = Integer.parseInt(prop.getProperty("rate_trung_detu_2").trim());
            }
            if (prop.containsKey("rate_trung_detu_3")) {
                rateTrungDeTu3 = Integer.parseInt(prop.getProperty("rate_trung_detu_3").trim());
            }
            if (prop.containsKey("rate_zeno")) {
                rateZeno = Integer.parseInt(prop.getProperty("rate_zeno").trim());
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
            for (int i = 0; i < rateNangCapDo.length; i++) {
                prop.setProperty("nang_cap_do_c" + (i + 1), String.valueOf(rateNangCapDo[i]));
            }
            for (int i = 0; i < ratePhaLeHoa.length; i++) {
                prop.setProperty("pha_le_hoa_sao_" + (i + 1), String.valueOf(ratePhaLeHoa[i]));
            }
            prop.setProperty("skh_vip1", String.valueOf(skhVip1));
            prop.setProperty("skh_vip2", String.valueOf(skhVip2));
            prop.setProperty("skh_thuong", String.valueOf(skhThuong));

            prop.setProperty("rate_bong_tai_2", String.valueOf(rateBongTai2));
            prop.setProperty("rate_bong_tai_3", String.valueOf(rateBongTai3));
            prop.setProperty("rate_trung_detu_1", String.valueOf(rateTrungDeTu1));
            prop.setProperty("rate_trung_detu_2", String.valueOf(rateTrungDeTu2));
            prop.setProperty("rate_trung_detu_3", String.valueOf(rateTrungDeTu3));
            prop.setProperty("rate_zeno", String.valueOf(rateZeno));

            try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH)) {
                prop.store(fos, "CAU HINH TI LE DAP DO - SERVER");
            }
            syncToModules();
            return true;
        } catch (Exception e) {
            Logger.error("Lỗi khi save " + CONFIG_PATH + ": " + e.getMessage() + "\n");
            return false;
        }
    }


    public static void resetDefaults() {
        rateNangCapDo = new double[]{80.0, 50.0, 20.0, 10.0, 5.0, 1.0, 0.5, 0.1};
        ratePhaLeHoa = new float[]{50f, 30f, 20f, 10f, 5f, 1f, 1f, 0.5f, 0.5f, 0.5f};
        skhVip1 = 25;
        skhVip2 = 35;
        skhThuong = 40;
        rateBongTai2 = 50;
        rateBongTai3 = 20;
        rateTrungDeTu1 = 80;
        rateTrungDeTu2 = 70;
        rateTrungDeTu3 = 50;
        rateZeno = 30;
        save();
    }

    public static void syncToModules() {
        try {
            models.combine.list.NangCapBongTai.RATIO_BONG_TAI = rateBongTai2;
            models.combine.list.NangCapZeno.TI_LE_THANH_CONG = rateZeno;
        } catch (Throwable ignored) {
        }
    }
}
