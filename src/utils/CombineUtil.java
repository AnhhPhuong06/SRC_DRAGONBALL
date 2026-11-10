package utils;

/*
 * @Zalo : 0372.665.345
 */
import models.item.Item;

public class CombineUtil {

    public static boolean isTrangBiPhaLeHoa(Item item) {
        if (item != null && item.isNotNullItem()) {
            int type = item.template.type;
            // Đồ thường (0-4), Cải trang (5), Đeo lưng/cánh (11), Pet (21), Ván bay (23,24). Bông tai (32) không cho đập
            return type <= 5 || type == 11 || type == 21 || type == 23 || type == 24 || type == 32;
        } else {
            return false;
        }
    }

    public static boolean isDaPhaLe(Item item) {
        return item != null && item.isNotNullItem() && (item.template.type == 30 || (item.template.id >= 14 && item.template.id <= 20) || isDaPhaLePhuKien(item));
    }

    public static boolean isDaPhaLePhuKien(Item item) {
        return item != null && item.isNotNullItem() && item.template.id >= 1577 && item.template.id <= 1581;
    }

    public static boolean isPhuKienEpSao(Item trangBi) {
        if (trangBi == null || !trangBi.isNotNullItem()) {
            return false;
        }
        int type = trangBi.template.type;
        return type == 5 || type == 11 || type == 21 || type == 23 || type == 24;
    }

    public static String getEpSaoError(Item trangBi, Item daPhaLe) {
        boolean isPhuKien = isPhuKienEpSao(trangBi);
        boolean isDaVoCuc = isDaPhaLePhuKien(daPhaLe);
        if (isDaVoCuc && !isPhuKien) {
            return "Loại đá này chỉ ép được vào phụ kiện, không ép được vào đồ";
        }
        if (!isDaVoCuc && isPhuKien) {
            return "Phụ kiện chỉ ép được đá Vô Cực";
        }
        return null;
    }

    public static int getGemEpSao(int star) {
        return switch (star) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 25;
            case 5 -> 50;
            case 6 -> 100;
            default -> 0;
        };
    }

    public static int getOptionDaPhaLe(Item daPhaLe) {
        if (daPhaLe.template.type == 30) {
            switch (daPhaLe.template.id) {
                case 441 -> { return 95; }
                case 442 -> { return 96; }
                case 443 -> { return 97; }
                case 444 -> { return 99; }
                case 445 -> { return 98; }
                case 446 -> { return 100; }
                case 447 -> { return 101; }
            }
            Item.ItemOption option = getCrystalOption(daPhaLe);
            return option == null ? -1 : option.optionTemplate.id;
        }
        return switch (daPhaLe.template.id) {
            case 20 -> 77;
            case 19 -> 103;
            case 18 -> 80;
            case 17 -> 81;
            case 16 -> 50;
            case 15 -> 94;
            case 14 -> 108;
            case 1581 -> // Đá Thời Gian: +5% HP
                77;
            case 1579 -> // Đá Sức Mạnh: +3% sức đánh
                50;
            case 1577 -> // Đá Linh Hồn: +5% ki
                103;
            case 1580 -> // Đá Không Gian: +3% sức đánh chí mạng
                5;
            case 1578 -> // Đá Hiện Thực: +2% chí mạng
                14;
            default ->
                -1;
        };
    }

    public static int getParamDaPhaLe(Item daPhaLe) {
        if (daPhaLe.template.type == 30) {
            switch (daPhaLe.template.id) {
                case 441, 442, 443, 446, 447 -> { return 5; }
                case 444, 445 -> { return 3; }
            }
            Item.ItemOption option = getCrystalOption(daPhaLe);
            return option == null ? 0 : option.param;
        }
        return switch (daPhaLe.template.id) {
            case 20 -> 5;
            case 19 -> 5;
            case 18 -> 5;
            case 17 -> 5;
            case 16 -> 3;
            case 15 -> 2;
            case 14 -> 2;
            case 1581 -> // +5% HP
                5;
            case 1579 -> // +3% sức đánh
                3;
            case 1577 -> // +5% ki
                5;
            case 1580 -> // +3% sức đánh chí mạng
                3;
            case 1578 -> // +2% chí mạng
                2;
            default ->
                -1;
        };
    }

    private static Item.ItemOption getCrystalOption(Item item) {
        if (item.itemOptions != null) {
            for (Item.ItemOption option : item.itemOptions) {
                if (option != null && option.optionTemplate != null) {
                    int id = option.optionTemplate.id;
                    if (id != 30 && id != 72 && id != 73) {
                        return option;
                    }
                }
            }
        }
        return null;
    }

    public static int getGoldPhaLeHoa(int star) {
        return switch (star) {
            case 0 -> 5000000;
            case 1 -> 10000000;
            case 2 -> 20000000;
            case 3 -> 30000000;
            case 4 -> 50000000;
            case 5 -> 60000000;
            case 6 -> 90000000;
            case 7 -> 100000000;
            case 8 -> 120000000;
            case 9 -> 120000000;
            default -> 0;
        };
    }

    public static float getRatioPhaLeHoa(int star) {
        return services.CombineRateConfig.getRatePhaLeHoa(star);
    }

    public static int getGemPhaLeHoa(int star) {
        return switch (star) {
            case 0 -> 5;
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            case 4 -> 40;
            case 5 -> 50;
            case 6 -> 60;
            case 7 -> 70;
            case 8 -> 140;
            case 9 -> 280;
            case 10 -> 560;
            case 11 -> 1120;
            case 12 -> 2240;
            default -> 0;
        };
    }

    public static boolean isCoupleItemNangCapCheck(Item trangBi, Item daNangCap) {
        if (trangBi == null || daNangCap == null) {
            return false;
        }
        int stoneId = switch (trangBi.template.type) {
            case 0 -> 223;
            case 1 -> 222;
            case 2 -> 224;
            case 3 -> 221;
            case 4 -> 220;
            default -> -1;
        };
        return stoneId != -1 && daNangCap.template.id == stoneId;
    }

    public static int getCountDaNangCapDo(int level) {
        return switch (level) {
            case 0 -> 3;
            case 1 -> 7;
            case 2 -> 11;
            case 3 -> 17;
            case 4 -> 23;
            case 5 -> 35;
            case 6 -> 50;
            case 7 -> 70;
            default -> 0;
        };
    }

    public static int getCountDaBaoVe(int level) {
        return level + 1;
    }

    public static int getGoldNangCapDo(int level) {
        return switch (level) {
            case 0 -> 10000;
            case 1 -> 70000;
            case 2 -> 300000;
            case 3 -> 1500000;
            case 4 -> 7000000;
            case 5 -> 23000000;
            case 6 -> 100000000;
            case 7 -> 250000000;
            default -> 0;
        };
    }

    public static double getTileNangCapDo(int level) {
        return services.CombineRateConfig.getRateNangCapDo(level);
    }

    public static double getTileNangChanMenh(int id) {
        return switch (id) {
            case 2139 -> 80;
            case 2140 -> 70;
            case 2141 -> 60;
            case 2142 -> 50;
            case 2143 -> 40;
            case 2144 -> 30;
            case 2145 -> 20;
            case 2146 -> 10;
            default -> 0;
        };
    }
}
