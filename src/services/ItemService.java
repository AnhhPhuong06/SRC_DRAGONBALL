package services;

 
import java.util.ArrayList;
import models.Template;
import models.Template.ItemOptionTemplate;
import models.item.Item;
import models.map.ItemMap;
import models.player.Player;
import models.shop.ItemShop;
import server.Manager;
import services.player.InventoryService;
import utils.TimeUtil;
import utils.Util;
import models.item.Item.ItemOption;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import models.map.Zone;
import services.map.MapService;
import services.func.UseItem;
import java.util.function.IntBinaryOperator;

public class ItemService {

    public static final int RANDOM_SDCM_50_200 = 194;
    public static final int RANDOM_SD_HP_KI_1_30 = 195;
    public static final int RANDOM_SD_HP_KI_20_50 = 196;
    public static final int HIDDEN_MAIN_STAT = 197;
    public static final int HIDDEN_SECONDARY_STAT = 198;
    public static final int PERCENT_SD_HP_KI = 199;
    public static final int MAGE_EQUIPMENT_COUNT = 200;
    public static final int MAGE_DAMAGE_PERCENT = 201;
    public static final int MAGE_HP_PERCENT = 202;
    public static final int MAGE_KI_PERCENT = 203;
    public static final int MAGE_CRITICAL_DAMAGE_PERCENT = 204;
    public static final int SUPER_COSTUME_SUCCESS_COUNT = 205;

    public static String resolveOptionName(int id, String name) {
        return switch (id) {
            case RANDOM_SDCM_50_200 -> "Random +50-200% sức đánh chí mạng";
            case RANDOM_SD_HP_KI_1_30 -> "Random +1-30% SĐ, HP, KI";
            case RANDOM_SD_HP_KI_20_50 -> "Random +20-50% SĐ, HP, KI";
            case HIDDEN_MAIN_STAT -> "#% dòng chỉ số ẩn dòng chính";
            case HIDDEN_SECONDARY_STAT -> "#% dòng chỉ số ẩn dòng phụ";
            case PERCENT_SD_HP_KI -> "+#% SĐ, HP, KI";
            case MAGE_EQUIPMENT_COUNT -> "Trang Bị Pháp Sư # lần";
            case MAGE_DAMAGE_PERCENT -> "SĐ Pháp Sư #%";
            case MAGE_HP_PERCENT -> "HP Pháp Sư #%";
            case MAGE_KI_PERCENT -> "KI Pháp Sư #%";
            case MAGE_CRITICAL_DAMAGE_PERCENT -> "SĐCM Pháp Sư #%";
            case SUPER_COSTUME_SUCCESS_COUNT -> "Siêu hóa thành công #/20 lần";
            default -> name;
        };
    }

    private static ItemService i;

    public static ItemService gI() {
        if (i == null) {
            i = new ItemService();
        }
        return i;
    }

    public short getItemIdByIcon(short IconID) {
        for (int i = 0; i < Manager.ITEM_TEMPLATES.size(); i++) {
            if (Manager.ITEM_TEMPLATES.get(i).iconID == IconID) {
                return Manager.ITEM_TEMPLATES.get(i).id;
            }
        }
        return -1;
    }

    public Item createItemNull() {
        Item item = new Item();
        return item;
    }

    public Item randomRacITem() {
        short[] racs = {381, 382, 383, 384, 385, 1099, 1100, 1101, 1102, 1103};
        Item item = createItemSetKichHoat(racs[Util.nextInt(racs.length - 1)], 1);
        if (optionRac(item.template.id) != 0) {
            item.itemOptions.add(new Item.ItemOption(optionRac(item.template.id), 1));
        }
        return item;
    }

    public List<ItemOption> removeAndAddOptionTemplate(List<Item.ItemOption> itemOptions, int removeId) {
        List<ItemOption> addedOptions = new ArrayList<>();
        int id = 0;
        int param = 0;
        Random random = new Random();

        if (removeId == 227) {
            boolean hasOpt227 = itemOptions.stream().anyMatch(io -> io.optionTemplate != null && io.optionTemplate.id == 227);
            if (!hasOpt227) {
                return addedOptions;
            }
            int randomChoice = random.nextInt(100);

            id = (randomChoice < 60) ? 93 : 206;
            param = Util.nextInt(15, 30);

            itemOptions.removeIf(io -> io.optionTemplate != null && io.optionTemplate.id == 227);

            ItemOption newOpt = new ItemOption(id, param);
            itemOptions.add(newOpt);
            addedOptions.add(newOpt);
        }
        if (removeId == 247) {
            boolean hasOpt247 = itemOptions.stream().anyMatch(io -> io.optionTemplate != null && io.optionTemplate.id == 247);
            if (!hasOpt247) {
                return addedOptions;
            }
            int count = itemOptions.stream()
                    .filter(io -> io.optionTemplate != null && io.optionTemplate.id == 247)
                    .map(io -> io.param)
                    .findFirst()
                    .orElse(1);
            if (count <= 0) {
                count = 1;
            }
            itemOptions.removeIf(io -> io.optionTemplate != null && io.optionTemplate.id == 247);

            List<Integer> pool = new ArrayList<>(Arrays.asList(50, 77, 103, 106, 5));
            Collections.shuffle(pool);
            int numToAdd = Math.min(count, pool.size());
            for (int i = 0; i < numToAdd; i++) {
                int optId = pool.get(i);
                int optParam = (optId == 106) ? 1 : Util.nextInt(1, 15);
                ItemOption newOpt = new ItemOption(optId, optParam);
                itemOptions.add(newOpt);
                addedOptions.add(newOpt);
            }
        }
        return addedOptions;
    }

    public Item createItemFromItemShop(ItemShop itemShop) {
        Item item = new Item();
        item.template = itemShop.temp;
        item.quantity = 1;
        item.content = item.getContent();
        item.info = item.getInfo();
        for (Item.ItemOption io : itemShop.options) {
            item.itemOptions.add(new Item.ItemOption(io));
        }
        removeAndAddOptionTemplate(item.itemOptions, 227);
        removeAndAddOptionTemplate(item.itemOptions, 247);
        return item;
    }

    public Item itemSKH(int itemId, int skhId) {
        return itemSKH(itemId, skhId, null);
    }

    /**
     * Tạo món Set Kích Hoạt.
     *
     * @param duPhong chỉ số dùng khi shop KHÔNG có định nghĩa cho vật phẩm này —
     *                thường là chỉ số của món nguyên liệu đem nâng cấp.
     *
     * <p>Chỉ số gốc vốn chỉ lấy từ shop, mà các mẫu đồ dùng làm SKH lại không được bán
     * ở shop nào, nên món tạo ra TRẮNG CHỈ SỐ — người chơi mất nguyên liệu mà nhận đồ
     * vô dụng. Giờ thiếu thì lấy chỉ số của chính món nguyên liệu.
     */
    public Item itemSKH(int itemId, int skhId, List<Item.ItemOption> duPhong) {
        Item item = createItemSetKichHoat(itemId, 1);
        if (item != null) {
            List<Item.ItemOption> chiSo = getListOptionItemShop((short) itemId);
            if (chiSo.isEmpty() && duPhong != null) {
                for (Item.ItemOption io : duPhong) {
                    if (io == null || io.optionTemplate == null) {
                        continue;
                    }
                    // Bỏ các dòng đánh dấu SKH và khoá — bên dưới sẽ gắn lại đúng bộ mới
                    if (laDongDanhDauSKH(io.optionTemplate.id) || io.optionTemplate.id == 30) {
                        continue;
                    }
                    chiSo.add(new Item.ItemOption(io.optionTemplate.id, io.param));
                }
            }
            if (chiSo.isEmpty() && item.template != null) {
                switch (item.template.type) {
                    case 0 -> chiSo.add(new Item.ItemOption(47, 5)); // giáp
                    case 1 -> chiSo.add(new Item.ItemOption(6, 30)); // HP
                    case 2 -> chiSo.add(new Item.ItemOption(0, 5));  // tấn công
                    case 3 -> chiSo.add(new Item.ItemOption(7, 15)); // KI
                    case 4 -> chiSo.add(new Item.ItemOption(14, 1)); // chí mạng
                }
            }
            item.itemOptions.addAll(chiSo);
            item.itemOptions.add(new Item.ItemOption(skhId, 1));
            item.itemOptions.add(new Item.ItemOption(optionIdSKH(skhId), 1));
            item.itemOptions.add(new Item.ItemOption(30, 1));
        }
        return item;
    }

    /** Các dòng chỉ số đánh dấu Set Kích Hoạt (không phải chỉ số cơ bản). */
    private static boolean laDongDanhDauSKH(int id) {
        return (id >= 127 && id <= 144) || id == 212 || id == 213
                || id == 215 || id == 216 || id == 217 || id == 246;
    }

    public int optionIdSKH(int skhId) {
        switch (skhId) {
            case 127:
                return 139;
            case 128:
                return 140;
            case 129:
                return 141;
            case 130:
                return 142;
            case 131:
                return 143;
            case 132:
                return 144;
            case 133:
                return 136;
            case 134:
                return 137;
            case 135:
                return 138;
            case 212:
                return 213;
            case 246:
                return 215;
            case 216:
                return 217;
        }
        return 0;
    }

    public int randTempItemDoSao() {
        // Mảng các vật phẩm được chia theo loại
        int[][] items = {
            // Áo
            {0, 1, 2, 3, 4, 5, 33, 34, 41, 42, 49, 50, 136, 137, 138, 139, 152, 153, 154, 155, 168, 169, 170, 171},
            // Quần
            {6, 7, 8, 9, 10, 11, 35, 36, 43, 44, 51, 52, 140, 141, 142, 143, 156, 157, 158, 159, 172, 173, 174, 175},
            // Găng
            {21, 22, 23, 24, 25, 26, 37, 38, 45, 46, 53, 54, 144, 145, 146, 147, 160, 161, 162, 163, 176, 177, 178, 179},
            // Giày
            {27, 28, 29, 30, 31, 32, 39, 40, 47, 48, 55, 56, 148, 149, 150, 151},
            // Rada
            {12, 57, 58, 59, 184, 185, 186, 187}
        };

        // Lựa chọn loại vật phẩm ngẫu nhiên dựa trên xác suất
        int type;
        if (Util.isTrue(10, 100)) {
            type = 4; // rada
        } else if (Util.isTrue(30, 100)) {
            type = 3; // gang
        } else if (Util.isTrue(50, 100)) {
            type = 1; // quan
        } else if (Util.isTrue(70, 100)) {
            type = 0; // ao
        } else {
            type = 2; // giay
        }

        // Kiểm tra xem `type` có hợp lệ hay không
        if (type < 0 || type >= items.length) {
            return -1; // Trường hợp không hợp lệ cho type
        }

        // Kiểm tra mảng items[type] có phần tử không
        if (items[type].length > 0) {
            // Trả về ID ngẫu nhiên từ mảng tương ứng
            return items[type][Util.nextInt(items[type].length)];
        } else {
            // Nếu mảng không có phần tử hợp lệ, trả về -1 hoặc xử lý theo cách khác
            return -1; // ID mặc định không hợp lệ
        }
    }

    public Item DoThienSu(int itemId, int gender) {
        Item dots = createItemSetKichHoat(itemId, 1);
        List<Integer> ao = Arrays.asList(1048, 1049, 1050);
        List<Integer> quan = Arrays.asList(1051, 1052, 1053);
        List<Integer> gang = Arrays.asList(1054, 1055, 1056);
        List<Integer> giay = Arrays.asList(1057, 1058, 1059);
        List<Integer> nhan = Arrays.asList(1060, 1061, 1062);
        //áo
        if (ao.contains(itemId)) {
            dots.itemOptions.add(new ItemOption(47, Util.highlightsItem(gender == 2, new Random().nextInt(1201) + 2800))); // áo từ 2800-4000 giáp
        }
        //quần
        if (Util.isTrue(80, 100)) {
            if (quan.contains(itemId)) {
                dots.itemOptions.add(new ItemOption(22, Util.highlightsItem(gender == 0, new Random().nextInt(11) + 120))); // hp 120k-130k
            }
        } else {
            if (quan.contains(itemId)) {
                dots.itemOptions.add(new ItemOption(22, Util.highlightsItem(gender == 0, new Random().nextInt(21) + 130))); // hp 130-150k 15%
            }
        }
        //găng
        if (Util.isTrue(80, 100)) {
            if (gang.contains(itemId)) {
                dots.itemOptions.add(new ItemOption(0, Util.highlightsItem(gender == 2, new Random().nextInt(651) + 10350))); // 9350-10000
            }
        } else {
            if (gang.contains(itemId)) {
                dots.itemOptions.add(new ItemOption(0, Util.highlightsItem(gender == 2, new Random().nextInt(1001) + 14500))); // gang 15% 10-11k -xayda 12k1
            }
        }
        //giày
        if (Util.isTrue(80, 100)) {
            if (giay.contains(itemId)) {
                dots.itemOptions.add(new ItemOption(23, Util.highlightsItem(gender == 1, new Random().nextInt(21) + 90))); // ki 90-110k
            }
        } else {
            if (giay.contains(itemId)) {
                dots.itemOptions.add(new ItemOption(23, Util.highlightsItem(gender == 1, new Random().nextInt(21) + 110))); // ki 110-130k
            }
        }
        if (nhan.contains(itemId)) {
            dots.itemOptions.add(new ItemOption(14, Util.highlightsItem(gender == 1, new Random().nextInt(19) + 2))); // nhẫn 19-21
        }
        dots.itemOptions.add(new ItemOption(21, 30));
        dots.itemOptions.add(new ItemOption(30, 1));
        return dots;
    }

    public Item copyItem(Item item) {
        Item it = new Item();
        it.itemOptions = new ArrayList<>();
        it.template = item.template;
        it.info = item.info;
        it.content = item.content;
        it.quantity = item.quantity;
        it.createTime = item.createTime;
        for (Item.ItemOption io : item.itemOptions) {
            it.itemOptions.add(new Item.ItemOption(io));
        }
        return it;
    }

    public Item copyItemGift(Item item) {
        Item it = new Item();
        it.itemOptions = new ArrayList<>();
        it.template = item.template;
        it.info = item.info;
        it.content = item.content;
        it.quantity = item.quantity;
        it.createTime = item.createTime;
        it.itemOptions.addAll(item.itemOptions);
        it.message = item.message;
        return it;
    }

    public Item createNewItem(short tempId) {
        return createNewItem(tempId, 1);
    }

    public Item createNewItem(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        if (tempId == 1430) {
            item.itemOptions.add(new ItemOption(93, 3));
        }
        if (item.isManhVoBongTai()) {
            item.syncManhVoBongTai();
        }
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item createItemFromData(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }
    public void normalizeTicketOptions(Item item) {
        if (item == null || item.itemOptions == null || item.itemOptions.size() < 2) {
            return;
        }
        ArrayList<Item.ItemOption> result = new ArrayList<>();
        for (Item.ItemOption io : item.itemOptions) {
            if (io == null || io.optionTemplate == null) {
                result.add(io);
                continue;
            }
            Item.ItemOption existed = null;
            for (Item.ItemOption kept : result) {
                if (kept != null && kept.optionTemplate != null
                        && kept.optionTemplate.id == io.optionTemplate.id) {
                    existed = kept;
                    break;
                }
            }
            if (existed == null) {
                result.add(io);
            } else if (io.optionTemplate.id == 93 && io.param < existed.param) {
                existed.param = io.param; // HSD: giữ số ngày nhỏ nhất
            }
        }
        if (result.size() != item.itemOptions.size()) {
            item.itemOptions.clear();
            item.itemOptions.addAll(result);
            item.content = item.getContent();
            item.info = item.getInfo();
        }
    }

    public Item createNewItemLock(int tempId) {
        return createNewItemLock(tempId, 1);
    }

    public Item createNewItemLock(int tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        item.content = item.getContent();
        item.info = item.getInfo();
        item.itemOptions.add(new ItemOption(30, 1));
        return item;
    }

    public Item createNewItemPhieuTangNgoc(int tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = 1;
        item.createTime = System.currentTimeMillis();
        item.content = item.getContent();
        item.info = item.getInfo();
        item.itemOptions.add(new ItemOption(31, quantity));
        item.itemOptions.add(new ItemOption(30, 1));
        return item;
    }

    public Item otpts(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        if (item.template.type == 0) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(47, Util.nextInt(2000, 2500)));
        }
        if (item.template.type == 1) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(22, Util.nextInt(150, 200)));
        }
        if (item.template.type == 2) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(0, Util.nextInt(13000, 18000)));
        }
        if (item.template.type == 3) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(23, Util.nextInt(150, 200)));
        }
        if (item.template.type == 4) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(14, Util.nextInt(20, 25)));
        }
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item createItemSetKichHoat(int tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.itemOptions = createItemNull().itemOptions;
        item.createTime = System.currentTimeMillis();
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item createItemFromItemMap(ItemMap itemMap) {
        Item item = createNewItem(itemMap.itemTemplate.id, itemMap.quantity);
        item.itemOptions = new ArrayList<>(itemMap.options);
        return item;
    }

    public ItemOptionTemplate getItemOptionTemplate(int id) {
        return (id < 0 || id >= Manager.ITEM_OPTION_TEMPLATES.size()) ? null : Manager.ITEM_OPTION_TEMPLATES.get(id);
    }

    public Template.ItemTemplate getTemplate(int id) {
        return (id < 0 || id >= Manager.ITEM_TEMPLATES.size()) ? null : Manager.ITEM_TEMPLATES.get(id);
    }

    public int getPercentTrainArmor(Item item) {
        if (item != null) {
            switch (item.template.id) {
                case 529:
                case 534:
                    return 10;
                case 530:
                case 535:
                    return 20;
                case 531:
                case 536:
                    return 30;
                case 1134:
                    return 40;
                case 1583:
                    return 50;
                default:
                    return 0;
            }
        } else {
            return 0;
        }
    }

    public boolean isTrainArmor(Item item) {
        if (item != null) {
            if (item.template != null && item.template.type == 32) {
                return true;
            }
            switch (item.template.id) {
                case 529:
                case 534:
                case 530:
                case 535:
                case 531:
                case 536:
                case 1134:
                case 1583:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public boolean isOutOfDateTime(Item item) {
        if (item == null || item.itemOptions == null) return false;
        for (Item.ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == 93) {
                if (io.param <= 0) {
                    return true;
                }
                long now = System.currentTimeMillis();
                if (item.createTime > 0 && item.createTime < 100_000_000_000L) {
                    item.createTime *= 1000L;
                }
                if (item.createTime <= 0 || item.createTime > now) {
                    item.createTime = now;
                    return false;
                }
                long timePass = now - item.createTime;
                long dayPass = timePass / 86_400_000L;
                if (dayPass > 0) {
                    io.param -= (int) dayPass;
                    if (io.param <= 0) {
                        return true;
                    }
                    item.createTime += dayPass * 86_400_000L;
                }
                return false; // chỉ có 1 option 93 / item
            }
        }
        return false;
    }


    public void OpenCapsuleCaiTrang(Player player, Item itemUse) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }
        Item item = ItemService.gI().createNewItem((short) Util.nextInt(423, 433));
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 19)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 19)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 19)));
        item.itemOptions.add(new Item.ItemOption(93, itemUse.template.id == 1157 ? 5 : 7));
        CombineService.gI().sendEffectOpenItem(player, itemUse.template.iconID, item.template.iconID);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
        InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBags(player);
    }

    private static final int[] RUONG_1009_IDS = {1119, 1120, 1328, 1240, 1235, 1236, 1113, 1115};

    public void OpenRuongTranhHuyenBi(Player player, Item itemUse) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }
        int resultId = RUONG_1009_IDS[Util.nextInt(RUONG_1009_IDS.length)];
        boolean caiTrang = (resultId == 1113 || resultId == 1115);
        Item item = ItemService.gI().createNewItem((short) resultId);
        if (item == null || item.template == null) {
            return; // id lỗi/thiếu template -> không mở, giữ lại rương
        }
        item.itemOptions.add(new Item.ItemOption(50, rollRuongOption(caiTrang)));   // sức đánh %
        item.itemOptions.add(new Item.ItemOption(77, rollRuongOption(caiTrang)));   // HP %
        item.itemOptions.add(new Item.ItemOption(103, rollRuongOption(caiTrang)));  // KI %
        if (caiTrang && Util.nextInt(1, 100) <= 10) { // 10% cải trang: thêm +5-10% sức đánh chí mạng (option 5)
            item.itemOptions.add(new Item.ItemOption(5, Util.nextInt(5, 10)));
        }
        if (Util.nextInt(1, 1000) <= 999) { // 99.9% dính hạn 1-3 ngày, 0.1% vĩnh viễn
            item.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 3)));
        }
        CombineService.gI().sendEffectOpenItem(player, itemUse.template.iconID, item.template.iconID);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
        InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBags(player);
    }

    /**
     * % 1 option cho rương 1009. Mức cao chỉ 1%:
     * - Cải trang: 99% = 20-29%, 1% = 30-35%.
     * - Item thường: 99% = 5-14%, 1% = 15-20%.
     */
    private int rollRuongOption(boolean caiTrang) {
        boolean rare = Util.nextInt(1, 100) == 1; // 1%
        if (caiTrang) {
            return rare ? Util.nextInt(30, 35) : Util.nextInt(20, 29);
        }
        return rare ? Util.nextInt(15, 20) : Util.nextInt(5, 14);
    }

    public void OpenItem736(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 50;
            int ruby = 20;
            int dbv = 10;
            int vb = 10;
            int bh = 5;
            int ct = 5;
            Item item = randomRac();
            if (rd <= rac) {
                item = randomRac();
            } else if (rd <= rac + ruby) {
                item = createItemSetKichHoat(861, 1);
            } else if (rd <= rac + ruby + dbv) {
                item = daBaoVe();
            } else if (rd <= rac + ruby + dbv + vb) {
                item = vanBay2011(true);
            } else if (rd <= rac + ruby + dbv + vb + bh) {
                item = phuKien2011(true);
            } else if (rd <= rac + ruby + dbv + vb + bh + ct) {
                item = caitrang2011(true);
            }
            if (item.template.id == 861) {
                item.quantity = Util.nextInt(1, 10);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            player.inventory.event++;
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem648(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 50;
            int ruby = 20;
            int dbv = 10;
            int vb = 10;
            int bh = 5;
            int ct = 5;
            Item item = randomRac();
            if (rd <= rac) {
                item = randomRac2();
            } else if (rd <= rac + ruby) {
                item = createItemSetKichHoat(77, 1);
            } else if (rd <= rac + ruby + dbv) {
                item = vatphamsk(true);
            } else if (rd <= rac + ruby + dbv + vb) {
                item = vanBayChrimas(true);
            } else if (rd <= rac + ruby + dbv + vb + bh) {
                item = phuKienChristmas(true);
            } else if (rd <= rac + ruby + dbv + vb + bh + ct) {
                item = caitrangChristmas(true);
            }
            if (item.template.id == 77) {
                item.quantity = Util.nextInt(10, 200);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            player.inventory.event++;
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1143(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int vp1 = 20;
            int vp2 = 20;
            int vp3 = 20;
            int vp4 = 20;
            int vp5 = 20;
            Item item = randomRac();
            if (rd <= vp1) {
                item = luoihaihong(true);
            } else if (rd <= vp1 + vp2) {
                item = khunglong(true);
            } else if (rd <= vp1 + vp2 + vp3) {
                item = cachepxuong(true);
            } else if (rd <= vp1 + vp2 + vp3 + vp4) {
                item = vanbayxuong(true);
            } else if (rd <= vp1 + vp2 + vp3 + vp4 + vp5) {
                item = petxuong(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1198(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int vp1 = 20;
            int vp2 = 20;
            int vp3 = 20;
            int vp4 = 20;
            int vp5 = 20;
            Item item = ItemService.gI().createNewItem((short) 16);
            if (rd <= vp1) {
                item = cadic(true);
            } else if (rd <= vp1 + vp2) {
                item = cadicssj2(true);
            } else if (rd <= vp1 + vp2 + vp3) {
                item = cadicssj2(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1199(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int vp1 = 20;
            int vp2 = 20;
            int vp3 = 20;
            int vp4 = 20;
            int vp5 = 20;
            Item item = ItemService.gI().createNewItem((short) 16);
            if (rd <= vp1) {
                item = cadicssj2m(true);
            } else if (rd <= vp1 + vp2) {
                item = cadicssj3(true);
            } else if (rd <= vp1 + vp2 + vp3) {
                item = cadicblue(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1200(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int vp1 = 20;
            int vp2 = 20;
            int vp3 = 20;
            int vp4 = 20;
            int vp5 = 20;
            Item item = ItemService.gI().createNewItem((short) 16);
            if (rd <= vp1) {
                item = gokussj3(true);
            } else if (rd <= vp1 + vp2) {
                item = gokugod(true);
            } else if (rd <= vp1 + vp2 + vp3) {
                item = gokublue(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Item cadic(boolean rating) {
        Item item = createItemSetKichHoat(1192, 1);
        item.itemOptions.add(new Item.ItemOption(50, 19));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 19));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 19));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item cadicssj(boolean rating) {
        Item item = createItemSetKichHoat(1193, 1);
        item.itemOptions.add(new Item.ItemOption(50, 22));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 22));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 22));//ki 25%
        item.itemOptions.add(new Item.ItemOption(106, 0));//k chết rét
        item.itemOptions.add(new Item.ItemOption(5, 22));//stcm 22%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item cadicssj2(boolean rating) {
        Item item = createItemSetKichHoat(1194, 1);
        item.itemOptions.add(new Item.ItemOption(50, 24));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 24));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item cadicssj2m(boolean rating) {
        Item item = createItemSetKichHoat(1195, 1);
        item.itemOptions.add(new Item.ItemOption(50, 24));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 24));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item gokussj3(boolean rating) {
        Item item = createItemSetKichHoat(1203, 1);
        item.itemOptions.add(new Item.ItemOption(50, 24));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 24));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item cadicssj3(boolean rating) {
        Item item = createItemSetKichHoat(1196, 1);
        item.itemOptions.add(new Item.ItemOption(50, 24));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 24));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(19, 20));//ki 25%
        item.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 10)));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item gokugod(boolean rating) {
        Item item = createItemSetKichHoat(1204, 1);
        item.itemOptions.add(new Item.ItemOption(50, 24));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 24));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(19, 20));//ki 25%
        item.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 10)));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item cadicblue(boolean rating) {
        Item item = createItemSetKichHoat(1197, 1);
        item.itemOptions.add(new Item.ItemOption(50, 25));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 25));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 25));//ki 25%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 25));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(19, 20));//ki 25%
        item.itemOptions.add(new Item.ItemOption(108, Util.nextInt(1, 10)));//ki 25%
        item.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 10)));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item gokublue(boolean rating) {
        Item item = createItemSetKichHoat(1205, 1);
        item.itemOptions.add(new Item.ItemOption(50, 25));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 25));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 25));//ki 25%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 25));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(19, 20));//ki 25%
        item.itemOptions.add(new Item.ItemOption(108, Util.nextInt(1, 10)));//ki 25%
        item.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 10)));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    //Cải trang sự kiện 20/11
    public Item caitrang2011(boolean rating) {
        Item item = createItemSetKichHoat(681, 1);
        item.itemOptions.add(new Item.ItemOption(76, 1));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(50, 24));//sd 26%
        item.itemOptions.add(new Item.ItemOption(108, 20));//sd 26%
        item.itemOptions.add(new Item.ItemOption(106, 0));//sd 26%
        if (Util.isTrue(999, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(5) + 1));//hsd
        }
        return item;
    }

    //Cải trang sự kiện giáng sinh
    public Item caitrangChristmas(boolean rating) {
        Item item = createItemSetKichHoat(Util.nextInt(824, 827), 1);
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(15, 25)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(15, 25)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(15, 25)));
        item.itemOptions.add(new Item.ItemOption(95, Util.nextInt(10, 25)));
        item.itemOptions.add(new Item.ItemOption(96, Util.nextInt(10, 25)));
        item.itemOptions.add(new Item.ItemOption(106, 0));//sd 26%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 10));//hsd
        }
        return item;
    }

    public Item luoihaihong(boolean rating) {
        Item item = createItemSetKichHoat(1124, 1);
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 15)));
        if (Util.isTrue(90, 100) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 10));//hsd
        }
        return item;
    }

    public Item khunglong(boolean rating) {
        Item item = createItemSetKichHoat(1125, 1);
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 25)));
        if (Util.isTrue(90, 100) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(10) + 10));//hsd
        }
        return item;
    }

    public Item cachepxuong(boolean rating) {
        Item item = createItemSetKichHoat(1126, 1);
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(12, 23)));
        item.itemOptions.add(new Item.ItemOption(101, Util.nextInt(5, 23)));
        if (Util.isTrue(90, 100) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(7) + 10));//hsd
        }
        return item;
    }

    public Item vanbayxuong(boolean rating) {
        Item item = createItemSetKichHoat(1123, 1);
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 23)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 23)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 23)));
        item.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 15)));
        if (Util.isTrue(90, 100) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(7) + 10));//hsd
        }
        return item;
    }

    public Item petxuong(boolean rating) {
        Item item = createItemSetKichHoat(1127, 1);
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 23)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 23)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 23)));
        item.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 15)));
        if (Util.isTrue(90, 100) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(7) + 10));//hsd
        }
        return item;
    }

    //610 - bong hoa
    //Phụ kiện bó hoa 20/11
    public Item phuKien2011(boolean rating) {
        Item item = createItemSetKichHoat(954, 1);
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(5) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(5) + 5));
        item.itemOptions.add(new Item.ItemOption(147, new Random().nextInt(5) + 5));
        if (Util.isTrue(1, 100)) {
            item.itemOptions.get(Util.nextInt(item.itemOptions.size() - 1)).param = 10;
        }
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    public Item phuKienChristmas(boolean rating) {
        Item item = createItemSetKichHoat(823, 1);
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(7) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(7) + 5));
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(7) + 5));
        if (Util.isTrue(1, 100)) {
            item.itemOptions.get(Util.nextInt(item.itemOptions.size() - 1)).param = 10;
        }
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(999, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    public Item vanBay2011(boolean rating) {
        Item item = createItemSetKichHoat(795, 1);
        item.itemOptions.add(new Item.ItemOption(89, 1));
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(950, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(7) + 1));//hsd
        }
        return item;
    }

    public Item daBaoVe() {
        Item item = createItemSetKichHoat(987, 1);
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        return item;
    }

    public Item randomRac() {
        short[] racs = {20, 19, 18, 17};
        Item item = createItemSetKichHoat(racs[Util.nextInt(racs.length - 1)], 1);
        if (optionRac(item.template.id) != 0) {
            item.itemOptions.add(new Item.ItemOption(optionRac(item.template.id), 1));
        }
        return item;
    }

    public Item randomRac2() {
        short[] racs = {649, 384, 385, 381, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842};
        int idItem = racs[Util.nextInt(racs.length - 1)];
        if (Util.isTrue(1, 100)) {
            idItem = 956;
        }
        Item item = createItemSetKichHoat(idItem, 1);
        if (optionRac(item.template.id) != 0) {
            item.itemOptions.add(new Item.ItemOption(optionRac(item.template.id), 1));
        }
        return item;
    }

    public Item vanBayChrimas(boolean rating) {
        Item item = createItemSetKichHoat(746, 1);
        item.itemOptions.add(new Item.ItemOption(89, 1));
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(950, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    public byte optionRac(short itemId) {
        switch (itemId) {
            case 220:
                return 71;
            case 221:
                return 70;
            case 222:
                return 69;
            case 224:
                return 67;
            case 223:
                return 68;
            default:
                return 0;
        }
    }

    public Item vatphamsk(boolean hsd) {
        int[] itemId = {954, 955, 822, 795, 744};
        byte[] option = {77, 80, 81, 103, 50, 94, 5};
        byte[] option_v2 = {14, 16, 17, 19, 27, 28, 47, 87}; //77 %hp // 80 //81 //103 //50 //94 //5 % sdcm
        byte optionid = 0;
        byte optionid_v2 = 0;
        byte param = 0;
        Item lt = ItemService.gI().createNewItem((short) itemId[Util.nextInt(itemId.length)]);
        lt.itemOptions.clear();
        optionid = option[Util.nextInt(0, 6)];
        param = (byte) Util.nextInt(5, 13);
        lt.itemOptions.add(new Item.ItemOption(optionid, param));
        if (Util.isTrue(1, 100)) {
            optionid_v2 = option_v2[Util.nextInt(option_v2.length)];
            lt.itemOptions.add(new Item.ItemOption(optionid_v2, param));
        }
        if (Util.isTrue(999, 1000) && hsd) {
            lt.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 7)));
        }
        lt.itemOptions.add(new Item.ItemOption(30, 0));
        return lt;
    }

    /**
     * Chỉ số gốc của một vật phẩm, lấy theo định nghĩa trong shop.
     *
     * <p>Trả về BẢN SAO của từng dòng chỉ số. Bản cũ trả về chính đối tượng của shop:
     * mọi vật phẩm tạo ra đều dùng chung dòng chỉ số đó, nên chỉ cần một chỗ sửa param
     * (nâng cấp đồ, boss Cumber random hoá chỉ số khi rơi...) là CHỈ SỐ TRONG SHOP bị
     * đổi theo — và mọi món tạo sau đều thừa hưởng, càng ngày càng lệch.
     */
    public List<Item.ItemOption> getListOptionItemShop(short id) {
        List<Item.ItemOption> goc = new ArrayList<>();
        Manager.SHOPS.forEach(shop -> shop.tabShops.forEach(tabShop -> tabShop.itemShops.forEach(itemShop -> {
            if (itemShop.temp.id == id && goc.isEmpty()) {
                goc.addAll(itemShop.options);
            }
        })));
        List<Item.ItemOption> list = new ArrayList<>();
        for (Item.ItemOption io : goc) {
            if (io != null && io.optionTemplate != null) {
                list.add(new Item.ItemOption(io.optionTemplate.id, io.param));
            }
        }
        return list;
    }

    public int randTempItemKichHoat(int gender) {
        int[][][] items = {{{0, 33}, {1, 41}, {2, 49}}, {{6, 35}, {7, 43}, {8, 51}}, {{27, 30}, {28, 47}, {29, 55}}, {{21, 24}, {22, 46}, {23, 53}}, {{12, 57}, {12, 57}, {12, 57}}};
        // a w j g rd
        int type;
        if (Util.isTrue(30, 100)) {
            type = 4; // rada
        } else if (Util.isTrue(30, 100)) {
            type = 3; // gang
        } else if (Util.isTrue(30, 100)) {
            type = 1; // quan
        } else if (Util.isTrue(30, 100)) {
            type = 0; // ao
        } else {
            type = 2; // giay
        }

        return items[type][gender][Util.nextInt(items[type][gender].length)];
    }


    public ItemMap randDoTL(Zone zone, int quantity, int x, int y, long id) {
        short idTempTL;
        short[] ao = {555, 557, 559};
        short[] quan = {556, 558, 560};
        short[] gang = {562, 564, 566};
        short[] giay = {563, 565, 567};
        int rate = Util.nextInt(100);
        if (rate < 5) { // 5%
            idTempTL = 561;
        } else if (rate < 10) { // 10%
            idTempTL = gang[Util.nextInt(gang.length)];
        } else if (rate < 35) { // 20%
            idTempTL = quan[Util.nextInt(quan.length)];
        } else if (rate < 65) { // 25%
            idTempTL = ao[Util.nextInt(ao.length)];
        } else { // 40%
            idTempTL = giay[Util.nextInt(giay.length)];
        }

        int tiLe = Util.nextInt(100, 120);
        List<ItemOption> itemoptions = new ArrayList<>();

        switch (idTempTL) {
            case 555 ->
                itemoptions.add(new ItemOption(47, 800 * tiLe / 100));
            case 557 ->
                itemoptions.add(new ItemOption(47, 850 * tiLe / 100));
            case 559 ->
                itemoptions.add(new ItemOption(47, 900 * tiLe / 100));
            case 556 -> {
                int chiso = 52000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 558 -> {
                int chiso = 50000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 560 -> {
                int chiso = 48000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 562 ->
                itemoptions.add(new ItemOption(0, 4400 * tiLe / 100));
            case 564 ->
                itemoptions.add(new ItemOption(0, 4300 * tiLe / 100));
            case 566 ->
                itemoptions.add(new ItemOption(0, 4500 * tiLe / 100));
            case 563 -> {
                int chiso = 48000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso / 20));
            }
            case 565 -> {
                int chiso = 50000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso / 20));
            }
            case 567 -> {
                int chiso = 46000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso * 150 / 1000));
            }
            case 561 ->
                itemoptions.add(new ItemOption(14, 14 * tiLe / 100));
        }
        itemoptions.add(new ItemOption(209, 0)); 
        itemoptions.add(new ItemOption(21, Util.nextInt(15, 17)));
        itemoptions.add(new ItemOption(86, 1));
        int star = Util.isTrue(5, 100) ? 3 : Util.nextInt(0, 2);
        if (star > 0) {
            itemoptions.add(new ItemOption(107, star));
        }
        ItemMap it = new ItemMap(zone, idTempTL, quantity, x, y, id);
        it.options.clear();
        it.options.addAll(itemoptions);
        return it;
    }

    public ItemMap randDoKaio(Zone zone, int quantity, int x, int y, long id) {
        short idTempTL, type;
        short[] ao = {232, 236, 240};
        short[] quan = {244, 248, 252};
        short[] gang = {256, 260, 264};
        short[] giay = {268, 272, 276};
        short[] nhan = {280};
        short[] options = {73, 86};
        if (Util.isTrue(10, 100)) {
            idTempTL = nhan[0];
            type = 4; // rada
        } else if (Util.isTrue(20, 100)) {
            idTempTL = gang[Util.nextInt(3)];
            type = 2; // gang
        } else if (Util.isTrue(50, 100)) {
            idTempTL = quan[Util.nextInt(3)];
            type = 1; // quan
        } else if (Util.isTrue(50, 100)) {
            idTempTL = ao[Util.nextInt(3)];
            type = 0; // ao
        } else {
            idTempTL = giay[Util.nextInt(3)];
            type = 3; // giay
        }
        int tiLe = Util.nextInt(100, 100);
        List<ItemOption> itemoptions = new ArrayList<>();
        switch (type) {
            case 0 ->
                itemoptions.add(new ItemOption(47, Util.nextInt(330, 400) * tiLe / 100));
            case 1 -> {
                int chiso = Util.nextInt(20000, 25000) * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso * 125 / 1000));
            }
            case 2 ->
                itemoptions.add(new ItemOption(0, Util.nextInt(1550, 1700) * tiLe / 100));
            case 3 -> {
                int chiso = Util.nextInt(20000, 25000) * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso * 125 / 1000));
            }
            case 4 ->
                itemoptions.add(new ItemOption(14, Util.nextInt(10, 12) * tiLe / 100));
        }
        if (Util.isTrue(1, 100)) {
            itemoptions.add(new ItemOption(options[Util.nextInt(options.length)], 0));
        }
        //   itemoptions.add(new ItemOption(21, 170000000));
        itemoptions.add(new ItemOption(107, Util.nextInt(0, 4)));
        ItemMap it = new ItemMap(zone, idTempTL, quantity, x, y, id);
        it.options.clear();
        it.options.addAll(itemoptions);
        return it;
    }

    public ItemMap randDoLuongLong(Zone zone, int quantity, int x, int y, long id) {
        short idTempTL, type;
        short[] ao = {233, 237, 241};
        short[] quan = {245, 249, 253};
        short[] gang = {257, 261, 265};
        short[] giay = {269, 273, 277};
        short[] nhan = {281};
        short[] options = {73, 86, 87};
        if (Util.isTrue(10, 100)) {
            idTempTL = nhan[0];
            type = 4; // rada
        } else if (Util.isTrue(20, 100)) {
            idTempTL = gang[Util.nextInt(3)];
            type = 2; // gang
        } else if (Util.isTrue(50, 100)) {
            idTempTL = quan[Util.nextInt(3)];
            type = 1; // quan
        } else if (Util.isTrue(50, 100)) {
            idTempTL = ao[Util.nextInt(3)];
            type = 0; // ao
        } else {
            idTempTL = giay[Util.nextInt(3)];
            type = 3; // giay
        }
        int tiLe = Util.nextInt(100, 100);
        List<ItemOption> itemoptions = new ArrayList<>();
        switch (type) {
            case 0 ->
                itemoptions.add(new ItemOption(47, Util.nextInt(450, 500) * tiLe / 100));
            case 1 -> {
                int chiso = Util.nextInt(24000, 30000) * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso * 125 / 1000));
            }
            case 2 ->
                itemoptions.add(new ItemOption(0, Util.nextInt(2250, 2450) * tiLe / 100));
            case 3 -> {
                int chiso = Util.nextInt(24000, 30000) * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso * 125 / 1000));
            }
            case 4 ->
                itemoptions.add(new ItemOption(14, Util.nextInt(11, 13) * tiLe / 100));
        }
        if (Util.isTrue(1, 100)) {
            itemoptions.add(new ItemOption(options[Util.nextInt(options.length)], 0));
        }
        //   itemoptions.add(new ItemOption(21, 1500000000));
        itemoptions.add(new ItemOption(107, Util.nextInt(0, 4)));
        ItemMap it = new ItemMap(zone, idTempTL, quantity, x, y, id);
        it.options.clear();
        it.options.addAll(itemoptions);
        return it;
    }

    public boolean itemHasOption(Item item, int optionId) {
        return item.itemOptions.stream().anyMatch(io -> io.optionTemplate.id == optionId);
    }

    public void OpenSKH(Player player, int itemUseId, int select) throws Exception {
        if (select < 0 || select > 4) {
            return;
        }
        Item itemUse = InventoryService.gI().findItem(player.inventory.itemsBag, itemUseId);
        int[][] items = {{0, 6, 21, 27, 12}, {1, 7, 22, 28, 12}, {2, 8, 23, 29, 12}};
        int[][] options = {{128, 129, 246}, {130, 131, 132}, {133, 135, 134}};
        int[][] optionsNEWTD = {{243, 244, 245}};
        int[][] optionsNEWNM = {{235, 236, 237}};
        int[][] optionsNEWXD = {{239, 240, 241}};
        int skhv1 = 25;// ti le
        int skhv2 = 35;//ti le
        int skhc = 40;//ti le
        int skhId = -1;

        int rd = Util.nextInt(1, 100);
        if (rd <= skhv1) {
            skhId = 0;
        } else if (rd <= skhv1 + skhv2) {
            skhId = 1;
        } else if (rd <= skhv1 + skhv2 + skhc) {
            skhId = 2;
        }
        Item item = null;
        switch (itemUseId) {
            case 1227:
                item = itemSKH(items[0][select], options[0][skhId]);
                break;
            case 1228:
                item = itemSKH(items[1][select], options[1][skhId]);
                break;
            case 1229:
                item = itemSKH(items[2][select], options[2][skhId]);
                break;
        }
        if (item != null && InventoryService.gI().getCountEmptyBag(player) > 0) {
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().sendItemBags(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
        }
    }

    public void OpenSKHVIP(Player player, int itemUseId, int select) throws Exception {
        if (select < 0 || select > 4) {
            return;
        }
        Item itemUse = InventoryService.gI().findItem(player.inventory.itemsBag, itemUseId);
        int[][] items = {{Manager.aotdVip[Util.nextInt(0, 6)], Manager.quantdVip[Util.nextInt(0, 5)], Manager.gangtdVip[Util.nextInt(0, 6)], Manager.giaytdVip[Util.nextInt(0, 6)], Manager.radaSKHVipVip[Util.nextInt(0, 6)]},
        {Manager.aonmVip[Util.nextInt(0, 6)], Manager.quannmVip[Util.nextInt(0, 6)], Manager.gangnmVip[Util.nextInt(0, 6)], Manager.giaynmVip[Util.nextInt(0, 6)], Manager.radaSKHVipVip[Util.nextInt(0, 6)]},
        {Manager.aoxdVip[Util.nextInt(0, 6)], Manager.quanxdVip[Util.nextInt(0, 6)], Manager.gangxdVip[Util.nextInt(0, 6)], Manager.giayxdVip[Util.nextInt(0, 6)], Manager.radaSKHVipVip[Util.nextInt(0, 6)]}};
        int[][] options = {{128, 129, 246}, {130, 131, 132}, {133, 135, 134}};
        int skhv1 = 25;// ti le
        int skhv2 = 35;//ti le
        int skhc = 40;//ti le
        int skhId = -1;

        int rd = Util.nextInt(1, 100);
        if (rd <= skhv1) {
            skhId = 0;
        } else if (rd <= skhv1 + skhv2) {
            skhId = 1;
        } else if (rd <= skhv1 + skhv2 + skhc) {
            skhId = 2;
        }
        Item item = null;
        switch (itemUseId) {
            case 1467:
                if (isDTL(items[0][select])) {
                    item = randomCS(items[0][select], options[0][skhId]);
                } else {
                    item = itemSKH(items[0][select], options[0][skhId]);
                }
                break;
            case 1468:
                if (isDTL(items[1][select])) {
                    item = randomCS(items[1][select], options[1][skhId]);
                } else {
                    item = itemSKH(items[1][select], options[1][skhId]);
                }
                break;
            case 1469:
                if (isDTL(items[2][select])) {
                    item = randomCS(items[2][select], options[2][skhId]);
                } else {
                    item = itemSKH(items[2][select], options[2][skhId]);
                }
                break;
        }
        if (item != null && InventoryService.gI().getCountEmptyBag(player) > 0) {
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().sendItemBags(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
        }
    }

    public boolean isDTL(int id) {
        return id >= 555 && id <= 567;
    }

    public void OpenSKHVIPTS(Player player, int itemUseId, int select) throws Exception {
        if (select < 0 || select > 4) {
            return;
        }
        Item itemUse = InventoryService.gI().findItem(player.inventory.itemsBag, itemUseId);
        int[][] items = {{Manager.aotdVipp[Util.nextInt(0, 2)], Manager.quantdVipp[Util.nextInt(0, 2)], Manager.gangtdVipp[Util.nextInt(0, 2)], Manager.giaytdVipp[Util.nextInt(0, 2)], Manager.radaSKHtdVipp[Util.nextInt(0, 2)]},
        {Manager.aonmVipp[Util.nextInt(0, 2)], Manager.quannmVipp[Util.nextInt(0, 2)], Manager.gangnmVipp[Util.nextInt(0, 2)], Manager.giaynmVipp[Util.nextInt(0, 2)], Manager.radaSKHnmVipp[Util.nextInt(0, 2)]},
        {Manager.aoxdVipp[Util.nextInt(0, 2)], Manager.quanxdVipp[Util.nextInt(0, 2)], Manager.gangxdVipp[Util.nextInt(0, 2)], Manager.giayxdVipp[Util.nextInt(0, 2)], Manager.radaSKHxdVipp[Util.nextInt(0, 2)]}};
        int[][] options = {{128, 129, 246}, {130, 131, 132}, {133, 135, 134}};
        int skhv1 = 25;// ti le
        int skhv2 = 35;//ti le
        int skhc = 40;//ti le
        int skhId = -1;

        int rd = Util.nextInt(1, 100);
        if (rd <= skhv1) {
            skhId = 0;
        } else if (rd <= skhv1 + skhv2) {
            skhId = 1;
        } else if (rd <= skhv1 + skhv2 + skhc) {
            skhId = 2;
        }
        Item item = null;
        switch (itemUseId) {
            case 1472:
                item = randomCS(items[0][select], options[0][skhId]);
                break;
            case 1473:
                item = randomCS(items[1][select], options[1][skhId]);
                break;
            case 1474:
                item = randomCS(items[2][select], options[2][skhId]);
                break;
        }
        if (item != null && InventoryService.gI().getCountEmptyBag(player) > 0) {
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().sendItemBags(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
        }
    }

    public void openHopSKHThuong(Player player, int planet) {
        openHopSKHThuong(player, planet, Util::nextInt);
    }

    void openHopSKHThuong(Player player, int planet, IntBinaryOperator randomIndex) {
        if (planet < 0 || planet > 2) return;
        synchronized (player.inventory.itemsBag) {
            Item itemUse = InventoryService.gI().findItemBagByTemp(player, 1494);
            if (itemUse == null || itemUse.quantity <= 0) {
                Service.gI().sendThongBao(player, "Không tìm thấy Hộp SKH thường");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
                return;
            }

            // Five equipment slots, with all ordinary tiers through divine equipment.
            int[][] clothes = UseItem.LIST_ITEM_CLOTHES[planet];
            int[] slot = clothes[randomIndex.applyAsInt(0, clothes.length - 1)];
            int rewardItemId = slot[randomIndex.applyAsInt(0, slot.length - 1)];
            int[][] sets = {{127, 128, 129, 246}, {130, 131, 132}, {133, 134, 135}};
            int skhOptionId = sets[planet][randomIndex.applyAsInt(0, sets[planet].length - 1)];
            if (getTemplate(rewardItemId) == null) {
                Service.gI().sendThongBao(player, "Có lỗi xảy ra khi tạo vật phẩm!");
                return;
            }
            Item reward = isDTL(rewardItemId)
                    ? randomCS(rewardItemId, skhOptionId) : itemSKH(rewardItemId, skhOptionId);
            if (reward == null || !reward.isNotNullItem()
                    || !InventoryService.gI().addItemBag(player, reward)) {
                Service.gI().sendThongBao(player, "Không thể nhận vật phẩm, hãy kiểm tra hành trang.");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            player.idMark.setIndexMenu(consts.ConstNpc.IGNORE_MENU);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + reward.template.name);
        }
    }

    public void openHopSKHHuyDiet(Player player, int select) {
        if (select < 0 || select > 2) {
            return;
        }
        Item itemUse = InventoryService.gI().findItemBagByTemp(player, 1495);
        if (itemUse == null || itemUse.quantity <= 0) {
            Service.gI().sendThongBao(player, "Không tìm thấy Hộp SKH Hủy diệt");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }

        int[][] itemsDHD = {
            {650, 651, 657, 658, 656}, // Trái Đất: Áo, Quần, Găng, Giày, Nhẫn
            {652, 653, 659, 660, 656}, // Namếc: Áo, Quần, Găng, Giày, Nhẫn
            {654, 655, 661, 662, 656}  // Xayda: Áo, Quần, Găng, Giày, Nhẫn
        };

        int[][] skhOptions = {
            {127, 128, 129, 246}, // Trái Đất: Thên Xin Hăng, Kirin, Sôngôku, Kaiôken
            {130, 131, 132},      // Namếc: Picolo, Ốc tiêu, Pikkoro Daimao
            {133, 134, 135}       // Xayda: Kakarot, Ca Đíc, Nappa
        };

        int randomItemIndex = Util.nextInt(0, itemsDHD[select].length - 1);
        int rewardItemId = itemsDHD[select][randomItemIndex];

        int randomSkhIndex = Util.nextInt(0, skhOptions[select].length - 1);
        int skhOptionId = skhOptions[select][randomSkhIndex];

        Item reward = randomCS(rewardItemId, skhOptionId);
        if (reward != null) {
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, reward);
            InventoryService.gI().sendItemBags(player);

            String setTen = switch (skhOptionId) {
                case 127 -> "Set Thên Xin Hăng";
                case 128 -> "Set Kirin";
                case 129 -> "Set Sôngôku";
                case 246 -> "Set Kaiôken";
                case 130 -> "Set Picolo";
                case 131 -> "Set Ốc tiêu";
                case 132 -> "Set Pikkoro Daimao";
                case 133 -> "Set Kakarot";
                case 134 -> "Set Ca Đíc";
                case 135 -> "Set Nappa";
                default -> "Set Kích Hoạt";
            };
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + reward.template.name + " [" + setTen + "]");
        } else {
            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi tạo vật phẩm!");
        }
    }

    public Item randomCS(int itemId, int... skhId) {
        Item it = createNewItem((short) itemId, 1);
        List<Integer> ao = it.isDTL() ? Arrays.asList(555, 557, 559) : it.isDHD() ? Arrays.asList(650, 652, 654) : Arrays.asList(1048, 1049, 1050);
        List<Integer> quan = it.isDTL() ? Arrays.asList(556, 558, 560) : it.isDHD() ? Arrays.asList(651, 653, 655) : Arrays.asList(1051, 1052, 1053);
        List<Integer> gang = it.isDTL() ? Arrays.asList(562, 564, 566) : it.isDHD() ? Arrays.asList(657, 659, 661) : Arrays.asList(1054, 1055, 1056);
        List<Integer> giay = it.isDTL() ? Arrays.asList(563, 565, 567) : it.isDHD() ? Arrays.asList(658, 660, 662) : Arrays.asList(1057, 1058, 1059);
        List<Integer> nhan = it.isDTL() ? Arrays.asList(561, 561, 561) : it.isDHD() ? Arrays.asList(656, 656, 656) : Arrays.asList(1060, 1061, 1062);
        if (ao.contains(itemId)) {
            it.itemOptions.add(new ItemOption(47, it.isDTL() ? Util.nextInt(400) + 800 : it.isDHD() ? Util.nextInt(500) + 1300 : Util.nextInt(900) + 1900)); // def
            it.itemOptions.add(new ItemOption(94, it.isDTL() ? Util.nextInt(1, 3) : it.isDHD() ? Util.nextInt(3, 5) : Util.nextInt(5, 8))); // % def
        }
        if (quan.contains(itemId)) {
            it.itemOptions.add(new ItemOption(22, it.isDTL() ? Util.nextInt(10) + 45 : it.isDHD() ? Util.nextInt(29) + 46 : Util.nextInt(94) + 76)); // hp
            it.itemOptions.add(new ItemOption(80, it.isDTL() ? Util.nextInt(3) : it.isDHD() ? Util.nextInt(5) : Util.nextInt(8))); // hp hồi
        }
        if (gang.contains(itemId)) {
            it.itemOptions.add(new ItemOption(0, it.isDTL() ? Util.nextInt(1000) + 3500 : it.isDHD() ? Util.nextInt(2900) + 4600 : Util.nextInt(2000) + 18000)); // tc
            it.itemOptions.add(new ItemOption(50, it.isDTL() ? Util.nextInt(1, 3) : it.isDHD() ? Util.nextInt(3, 5) : Util.nextInt(5, 10))); // dame
        }
        if (giay.contains(itemId)) {
            it.itemOptions.add(new ItemOption(23, it.isDTL() ? Util.nextInt(10) + 35 : it.isDHD() ? Util.nextInt(29) + 46 : Util.nextInt(74) + 76)); // mp
            it.itemOptions.add(new ItemOption(81, it.isDTL() ? Util.nextInt(3) : it.isDHD() ? Util.nextInt(5) : Util.nextInt(8))); // mp hồi
        }
        if (nhan.contains(itemId)) {
            it.itemOptions.add(new ItemOption(14, it.isDTL() ? Util.nextInt(3) + 12 : it.isDHD() ? Util.nextInt(2) + 16 : Util.nextInt(7) + 18)); // crit
            it.itemOptions.add(new ItemOption(5, it.isDTL() ? Util.nextInt(1, 3) : it.isDHD() ? Util.nextInt(3, 5) : Util.nextInt(5, 8))); // sdcm
        }
        if (skhId.length == 1 && skhId[0] != -1) {
            it.itemOptions.add(new ItemOption(skhId[0], 1));
            it.itemOptions.add(new ItemOption(optionIdSKH(skhId[0]), 1));
        }
        it.itemOptions.add(new ItemOption(21, it.isDTL() ? 18 : it.isDHD() ? 45 : 80)); // ycsm 18tỉ
        it.itemOptions.add(new ItemOption(30, 1)); // khóa gd
        return it;
    }

    public int randomSKHId(byte gender) {
        // Handle the case where gender is 3 (invalid or special case)
        if (gender == 3) {
            gender = (byte) Util.nextInt(0, 2); // Randomly set gender to 0 or 1
        }

        // Validate gender to be within the expected range (0, 1, 2)
        if (gender < 0 || gender > 2) {
            // Log or handle the error: gender is out of range
            return -1; // Or some other error handling value
        }

        // Array with different SKH options based on gender
        int[][] options = {
            {128, 129, 246}, // For gender 0
            {130, 131, 132}, // For gender 1
            {133, 135, 134} // For gender 2
        };

        // Skew values for determining the random skhId
        int skhv1 = CombineRateConfig.skhVip1;
        int skhv2 = CombineRateConfig.skhVip2;
        int skhc = CombineRateConfig.skhThuong;
        int skhId = -1;

        // Random value between 1 and 100 to determine which skhId to choose
        int total = skhv1 + skhv2 + skhc;
        if (total <= 0) {
            total = 100;
        }
        int rd = Util.nextInt(1, total);

        if (rd <= skhv1) {
            skhId = 0; // First SKH option
        } else if (rd <= skhv1 + skhv2) {
            skhId = 1; // Second SKH option
        } else {
            skhId = 2; // Third SKH option
        }

        // Return the appropriate SKH ID based on gender and random choice
        return options[gender][skhId];
    }

    public void OpenItem1259(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 20;
            int ruby = 20;
            int dbv = 20;
            int vb = 20;
            int bh = 20;
            Item item = randomda();
            if (rd <= rac) {
                item = caitrangchuot(true);
            } else if (rd <= rac + ruby) {
                item = caitrangho(true);
            } else if (rd <= rac + ruby + dbv) {
                item = randomda();
            } else if (rd <= rac + ruby + dbv + vb) {
                item = petran(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1246(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 20;
            int ruby = 20;
            int dbv = 20;
            int vb = 20;
            int bh = 20;
            Item item = randomda();
            if (rd <= rac) {
                item = cobaolixi(true);
            } else if (rd <= rac + ruby) {
                item = caitrangthantai(true);
            } else if (rd <= rac + ruby + dbv) {
                item = thoivangbay(true);
            } else if (rd <= rac + ruby + dbv + vb) {
                item = bunmathanhlich(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1247(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 20;
            int ruby = 20;
            int dbv = 20;
            int vb = 20;
            int bh = 20;
            Item item = randommanhkhi();
            if (rd <= rac) {
                item = cachep(true);
            } else if (rd <= rac + ruby) {
                item = caitrangthantai(true);
            } else if (rd <= rac + ruby + dbv) {
                item = caitrangbroly(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            //  player.lixi += 1;
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1305(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 25;
            int ruby = 25;
            int dbv = 25;
            int vb = 25;
            Item item = null;
            if (rd <= rac) {
                item = BabySecret(true);
            } else if (rd <= rac + ruby) {
                item = BabyRabbit(true);
            } else if (rd <= rac + ruby + dbv) {
                item = BabyDino(true);
            } else if (rd <= rac + ruby + dbv + vb) {
                item = BabyPanda(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1258(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 20;
            int ruby = 20;
            int dbv = 20;
            int vb = 20;
            int bh = 20;
            Item item = daolong(true);
            if (rd <= rac) {
                item = daolong(true);
            } else if (rd <= rac + ruby) {
                item = bena(true);
            } else if (rd <= rac + ruby + dbv) {
                item = cacheprong(true);
            } else if (rd <= rac + ruby + dbv + vb) {
                item = caitrangtet(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            player.phaobong += 1;
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem1245(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 20;
            int ruby = 20;
            int dbv = 20;
            int vb = 20;
            int bh = 20;
            Item item = randomda();
            if (rd <= rac) {
                item = cobaolixi(true);
            } else if (rd <= rac + ruby) {
                item = caitrangthantai(true);
            } else if (rd <= rac + ruby + dbv) {
                item = thoivangbay(true);
            } else if (rd <= rac + ruby + dbv + vb) {
                item = bunmathanhlich(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Item randomda() {
        Item item = createItemSetKichHoat(Util.nextInt(1074, 1083), 1);
        return item;
    }

    public Item randommanhkhi() {
        Item item = createItemSetKichHoat(Util.nextInt(1276, 1278), 1);
        return item;
    }

    public Item petran(boolean rating) {
        Item item = createItemSetKichHoat(1240, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(10) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(10) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(10) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item cobaolixi(boolean rating) {
        Item item = createItemSetKichHoat(1274, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(15) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item cachep(boolean rating) {
        Item item = createItemSetKichHoat(1235, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(15) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item BabySecret(boolean rating) {
        Item item = createItemSetKichHoat(1301, 1);
        item.itemOptions.add(new Item.ItemOption(50, 22));
        item.itemOptions.add(new Item.ItemOption(77, 22));
        item.itemOptions.add(new Item.ItemOption(94, 8));
        item.itemOptions.add(new Item.ItemOption(5, 11));
        item.itemOptions.add(new Item.ItemOption(14, 8));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item BabyDino(boolean rating) {
        Item item = createItemSetKichHoat(1303, 1);
        item.itemOptions.add(new Item.ItemOption(77, 18));
        item.itemOptions.add(new Item.ItemOption(5, 7));
        item.itemOptions.add(new Item.ItemOption(14, 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 1));// hsd
        }
        return item;
    }

    public Item BabyRabbit(boolean rating) {
        Item item = createItemSetKichHoat(1302, 1);
        item.itemOptions.add(new Item.ItemOption(77, 18));
        item.itemOptions.add(new Item.ItemOption(94, 5));
        item.itemOptions.add(new Item.ItemOption(108, 7));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 1));// hsd
        }
        return item;
    }

    public Item BabyPanda(boolean rating) {
        Item item = createItemSetKichHoat(1304, 1);
        item.itemOptions.add(new Item.ItemOption(50, 16));
        item.itemOptions.add(new Item.ItemOption(77, 16));
        item.itemOptions.add(new Item.ItemOption(103, 16));
        item.itemOptions.add(new Item.ItemOption(229, 20));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 1));// hsd
        }
        return item;
    }

    public Item daolong(boolean rating) {
        Item item = createItemSetKichHoat(1273, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(12) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(12) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(12) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item bena(boolean rating) {
        Item item = createItemSetKichHoat(1240, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(15) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item cacheprong(boolean rating) {
        Item item = createItemSetKichHoat(1272, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(10) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(10) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(10) + 5));
        item.itemOptions.add(new Item.ItemOption(84, new Random().nextInt(10) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item caitrangtet(boolean rating) {
        Item item = createItemSetKichHoat(Util.nextInt(1269, 1271), 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(23) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(23) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(23) + 5));
        item.itemOptions.add(new Item.ItemOption(14, new Random().nextInt(1) + 10));
        item.itemOptions.add(new Item.ItemOption(101, new Random().nextInt(10) + 30));
        item.itemOptions.add(new Item.ItemOption(95, new Random().nextInt(10) + 20));
        if (Util.isTrue(999, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item caitrangthantai(boolean rating) {
        Item item = createItemSetKichHoat(Util.nextInt(1266, 1268), 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(14, new Random().nextInt(1) + 10));
        item.itemOptions.add(new Item.ItemOption(101, new Random().nextInt(10) + 40));
        item.itemOptions.add(new Item.ItemOption(95, new Random().nextInt(10) + 20));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item caitrangbroly(boolean rating) {
        Item item = createItemSetKichHoat(1241, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(5, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(14, new Random().nextInt(1) + 10));
        item.itemOptions.add(new Item.ItemOption(101, new Random().nextInt(10) + 40));
        item.itemOptions.add(new Item.ItemOption(95, new Random().nextInt(10) + 20));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item thoivangbay(boolean rating) {
        Item item = createItemSetKichHoat(1265, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(5) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(5) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(5) + 5));
        item.itemOptions.add(new Item.ItemOption(84, 0));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item bunmathanhlich(boolean rating) {
        Item item = createItemSetKichHoat(1264, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(14, new Random().nextInt(1) + 10));
        item.itemOptions.add(new Item.ItemOption(101, new Random().nextInt(10) + 40));
        item.itemOptions.add(new Item.ItemOption(95, new Random().nextInt(10) + 20));
        if (Util.isTrue(999, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item caitrangchuot(boolean rating) {
        Item item = createItemSetKichHoat(Util.nextInt(754, 756), 1);
        item.itemOptions.add(new Item.ItemOption(77, 30));
        item.itemOptions.add(new Item.ItemOption(27, 50));
        item.itemOptions.add(new Item.ItemOption(50, 20));
        item.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 7)));// hsds
        return item;
    }

    public Item caitrangho(boolean rating) {
        Item item = createItemSetKichHoat(Util.nextInt(952, 953), 1);
        item.itemOptions.add(new Item.ItemOption(77, 22));
        item.itemOptions.add(new Item.ItemOption(103, 22));
        item.itemOptions.add(new Item.ItemOption(50, 23));
        item.itemOptions.add(new Item.ItemOption(94, 10));
        item.itemOptions.add(new Item.ItemOption(27, 11));
        item.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 7)));// hsds
        return item;
    }

    public int[] randOptionItemKichHoatNew(int gender) {
        int op1;
        int op2;
        int op3;
        int op4;
        switch (gender) {
            case 0 -> {

                op1 = 242;
                op2 = 243;
                op3 = 244;
                op4 = 245;

            }
            case 1 -> {

                op1 = 234;
                op2 = 235;
                op3 = 236;
                op4 = 237;

            }
            case 2 -> {

                op1 = 238;
                op2 = 239;
                op3 = 240;
                op4 = 241;

            }
            default -> {

                op1 = -1;
                op2 = -1;
                op3 = -1;
                op4 = -1;

            }
        }
        int[] options = {op1, op2, op3, op4};
        return options;
    }

    public void settltd(Player player) throws Exception {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1324);
            Item ao = ItemService.gI().otpThanLinh((short) 555);
            Item quan = ItemService.gI().otpThanLinh((short) 556);
            Item gang = ItemService.gI().otpThanLinh((short) 562);
            Item giay = ItemService.gI().otpThanLinh((short) 563);
            Item nhan = ItemService.gI().otpThanLinh((short) 561);
            ao.itemOptions.add(new Item.ItemOption(87, 0));
            quan.itemOptions.add(new Item.ItemOption(87, 0));
            gang.itemOptions.add(new Item.ItemOption(87, 0));
            giay.itemOptions.add(new Item.ItemOption(87, 0));
            nhan.itemOptions.add(new Item.ItemOption(87, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 4) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().addItemBag(player, quan);
                InventoryService.gI().addItemBag(player, gang);
                InventoryService.gI().addItemBag(player, giay);
                InventoryService.gI().addItemBag(player, nhan);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được set thần linh kích hoạt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBags(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
            }
        }
    }

    public void settlnm(Player player) throws Exception {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1324);
            Item ao = ItemService.gI().otpThanLinh((short) 557);
            Item quan = ItemService.gI().otpThanLinh((short) 558);
            Item gang = ItemService.gI().otpThanLinh((short) 564);
            Item giay = ItemService.gI().otpThanLinh((short) 565);
            Item nhan = ItemService.gI().otpThanLinh((short) 561);
            ao.itemOptions.add(new Item.ItemOption(87, 0));
            quan.itemOptions.add(new Item.ItemOption(87, 0));
            gang.itemOptions.add(new Item.ItemOption(87, 0));
            giay.itemOptions.add(new Item.ItemOption(87, 0));
            nhan.itemOptions.add(new Item.ItemOption(87, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 4) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().addItemBag(player, quan);
                InventoryService.gI().addItemBag(player, gang);
                InventoryService.gI().addItemBag(player, giay);
                InventoryService.gI().addItemBag(player, nhan);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được set thần linh kích hoạt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBags(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
            }
        }
    }

    public void settlxd(Player player) throws Exception {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1324);
            Item ao = ItemService.gI().otpThanLinh((short) 559);
            Item quan = ItemService.gI().otpThanLinh((short) 560);
            Item gang = ItemService.gI().otpThanLinh((short) 566);
            Item giay = ItemService.gI().otpThanLinh((short) 567);
            Item nhan = ItemService.gI().otpThanLinh((short) 561);
            ao.itemOptions.add(new Item.ItemOption(87, 0));
            quan.itemOptions.add(new Item.ItemOption(87, 0));
            gang.itemOptions.add(new Item.ItemOption(87, 0));
            giay.itemOptions.add(new Item.ItemOption(87, 0));
            nhan.itemOptions.add(new Item.ItemOption(87, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 4) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().addItemBag(player, quan);
                InventoryService.gI().addItemBag(player, gang);
                InventoryService.gI().addItemBag(player, giay);
                InventoryService.gI().addItemBag(player, nhan);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được set thần linh kích hoạt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBags(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
            }
        }
    }

    public Item otpThanLinh(short tempId) {
        return otpTL(tempId, 1);
    }

    public Item otpTL(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        if (item.template.type == 0) {
            item.itemOptions.add(new ItemOption(21, 15));
            item.itemOptions.add(new ItemOption(47, Util.nextInt(1000, 1500)));
        }
        if (item.template.type == 1) {
            item.itemOptions.add(new ItemOption(21, 15));
            item.itemOptions.add(new ItemOption(22, Util.nextInt(45, 70)));
        }
        if (item.template.type == 2) {
            item.itemOptions.add(new ItemOption(21, 15));
            item.itemOptions.add(new ItemOption(0, Util.nextInt(3500, 4500)));
        }
        if (item.template.type == 3) {
            item.itemOptions.add(new ItemOption(21, 15));
            item.itemOptions.add(new ItemOption(23, Util.nextInt(45, 70)));
        }
        if (item.template.type == 4) {
            item.itemOptions.add(new ItemOption(21, 15));
            item.itemOptions.add(new ItemOption(14, Util.nextInt(13, 15)));
        }
        item.content = item.getContent();
        item.info = item.getInfo();
        if (isTrainArmor(item) && item.itemOptions.stream().noneMatch(io -> io.optionTemplate.id == 9)) {
            item.itemOptions.add(new ItemOption(9, 0));
        }
        return item;
    }

    public int[] randOptionItemKichHoat(int gender, Player player) {
        int op1, op2;
        // Chọn set theo hành tinh, không phụ thuộc số món đang có trong hành trang.
        switch (gender) {
            case 0 -> {
                if (Util.isTrue(10, 100)) { // Sôngôku
                    op1 = 129;
                    op2 = 141;
                } else if (Util.isTrue(40, 100)) {
                    op1 = 128;
                    op2 = 140;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 127;
                    op2 = 139;
                } else {
                    op1 = 246;
                    op2 = 215;
                }
            }
            case 1 -> {
                if (Util.isTrue(10, 100)) { // Picolo
                    op1 = 130;
                    op2 = 142;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 131;
                    op2 = 143;
                } else {
                    op1 = 132;
                    op2 = 144;
                }
            }
            default -> {
                if (Util.isTrue(10, 100)) { // Nappa
                    op1 = 135;
                    op2 = 138;
                } else if (Util.isTrue(60, 100)) {
                    op1 = 134;
                    op2 = 137;
                } else {
                    op1 = 133;
                    op2 = 136;
                }
            }
        }
        return new int[]{op1, op2};
    }

    public ItemMap mobDropWithCustomOption(Zone zone, int quantity, int x, int y, long id) {
        ItemMap itemMap;
        short itemDrop;

        // Random item rơi
        if (MapService.gI().isMapCold(zone.map.mapId) || MapService.gI().isMapNappa(zone.map.mapId)) {
            itemDrop = Manager.ITEMS_MOB_DROP[Util.nextInt(0, 2)][Util.nextInt(0, 4)][Util.nextInt(8, 11)];
        } else {
            itemDrop = Manager.ITEMS_MOB_DROP[Util.nextInt(0, 2)][Util.nextInt(0, 4)][Util.nextInt(0, 8)];
        }

        itemMap = new ItemMap(zone, itemDrop, 1, x, y, id);

        // Lấy option shop (KHÔNG sửa trực tiếp)
        List<Item.ItemOption> itemOptionsShop = new ArrayList<>(ItemService.gI().getListOptionItemShop(itemMap.itemTemplate.id));
        if (Util.isTrue(10, 100)) {
            itemOptionsShop.add(new ItemOption(107, Util.nextInt(0, 3)));
        }
        for (Item.ItemOption io : itemOptionsShop) {

            Item.ItemOption newIo = new Item.ItemOption(io);

            int percent = Util.nextInt(0, 15); // -5% -> +10%
            int newParam = io.param + io.param * percent / 100;

            // Giới hạn tránh quá ảo
            int max = (int) (io.param * 1.15);
            int min = (int) (io.param * 0.95);

            if (newParam > max) {
                newParam = max;
            }
            if (newParam < min) {
                newParam = min;
            }

            newIo.param = newParam;

            itemMap.options.add(newIo);
        }

        // Thêm option random ngoài
        int[] opts = {50, 77, 103, 94, 95, 96, 97, 98, 99, 100, 101};
        int optId = opts[Util.nextInt(opts.length)];

        itemMap.options.add(new Item.ItemOption(optId, Util.nextInt(1, 3)));
        itemMap.options.add(new Item.ItemOption(86, 0));

        return itemMap;
    }

    public ItemMap mobDropWithCustomOption1(Zone zone, int quantity, int x, int y, long id) {
        ItemMap itemMap;
        short itemDrop;

        // Random item rơi
        if (MapService.gI().isMapCold(zone.map.mapId) || MapService.gI().isMapNappa(zone.map.mapId)) {
            itemDrop = Manager.ITEMS_MOB_DROP[Util.nextInt(0, 2)][Util.nextInt(0, 4)][Util.nextInt(8, 11)];
        } else {
            itemDrop = Manager.ITEMS_MOB_DROP[Util.nextInt(0, 2)][Util.nextInt(0, 4)][Util.nextInt(0, 8)];
        }

        itemMap = new ItemMap(zone, itemDrop, 1, x, y, id);

        // Lấy option shop (KHÔNG sửa trực tiếp)
        List<Item.ItemOption> itemOptionsShop = new ArrayList<>(ItemService.gI().getListOptionItemShop(itemMap.itemTemplate.id));
        if (Util.isTrue(10, 100)) {
            itemOptionsShop.add(new ItemOption(107, Util.nextInt(0, 3)));
        }
        for (Item.ItemOption io : itemOptionsShop) {

            Item.ItemOption newIo = new Item.ItemOption(io);

            int percent = Util.nextInt(0, 15); // -5% -> +10%
            int newParam = io.param + io.param * percent / 100;

            // Giới hạn tránh quá ảo
            int max = (int) (io.param * 1.15);
            int min = (int) (io.param * 0.95);

            if (newParam > max) {
                newParam = max;
            }
            if (newParam < min) {
                newParam = min;
            }

            newIo.param = newParam;

            itemMap.options.add(newIo);
        }

        // Thêm option random ngoài
        int[] opts = {73};
        int optId = opts[Util.nextInt(opts.length)];

        itemMap.options.add(new Item.ItemOption(optId, Util.nextInt(1, 3)));
        itemMap.options.add(new Item.ItemOption(86, 0));

        return itemMap;
    }

    public ItemMap randDoTLBoss(Zone zone, int quantity, int x, int y, long id) {
        short idTempTL;
        short[] ao = {555, 557, 559};
        short[] quan = {556, 558, 560};
        short[] gang = {562, 564, 566};
        short[] giay = {563, 565, 567};
        if (Util.isTrue(5, 100)) {
            idTempTL = 561;
        } else if (Util.isTrue(10, 100)) {
            idTempTL = gang[Util.nextInt(3)];
        } else if (Util.isTrue(45, 100)) {
            idTempTL = quan[Util.nextInt(3)];
        } else if (Util.isTrue(75, 100)) {
            idTempTL = ao[Util.nextInt(3)];
        } else {
            idTempTL = giay[Util.nextInt(3)];
        }

        int tiLe = Util.nextInt(100, 115);
        List<ItemOption> itemoptions = new ArrayList<>();

        switch (idTempTL) {
            case 555 ->
                itemoptions.add(new ItemOption(47, 800 * tiLe / 100));
            case 557 ->
                itemoptions.add(new ItemOption(47, 850 * tiLe / 100));
            case 559 ->
                itemoptions.add(new ItemOption(47, 900 * tiLe / 100));
            case 556 -> {
                int chiso = 52000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 558 -> {
                int chiso = 50000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 560 -> {
                int chiso = 48000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 562 ->
                itemoptions.add(new ItemOption(0, 4400 * tiLe / 100));
            case 564 ->
                itemoptions.add(new ItemOption(0, 4300 * tiLe / 100));
            case 566 ->
                itemoptions.add(new ItemOption(0, 4500 * tiLe / 100));
            case 563 -> {
                int chiso = 48000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso / 20));
            }
            case 565 -> {
                int chiso = 50000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso / 20));
            }
            case 567 -> {
                int chiso = 46000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso * 150 / 1000));
            }
            case 561 ->
                itemoptions.add(new ItemOption(14, 14 * tiLe / 100));
        }
        itemoptions.add(new ItemOption(209, 0));
        itemoptions.add(new ItemOption(21, Util.nextInt(15, 17)));
        itemoptions.add(new ItemOption(86, 1));
        int star = Util.isTrue(5, 100) ? 3 : Util.nextInt(1, 2);
        itemoptions.add(new ItemOption(107, star));
        ItemMap it = new ItemMap(zone, idTempTL, quantity, x, y, id);
        it.options.clear();
        it.options.addAll(itemoptions);
        return it;
    }

    public ItemMap randDoTL3Sao(Zone zone, int quantity, int x, int y, long id) {
        short idTempTL;
        short[] ao = {555, 557, 559};
        short[] quan = {556, 558, 560};
        short[] gang = {562, 564, 566};
        short[] giay = {563, 565, 567};
        if (Util.isTrue(5, 100)) {
            idTempTL = 561;
        } else if (Util.isTrue(10, 100)) {
            idTempTL = gang[Util.nextInt(3)];
        } else if (Util.isTrue(45, 100)) {
            idTempTL = quan[Util.nextInt(3)];
        } else if (Util.isTrue(75, 100)) {
            idTempTL = ao[Util.nextInt(3)];
        } else {
            idTempTL = giay[Util.nextInt(3)];
        }

        int tiLe = Util.nextInt(100, 115);
        List<ItemOption> itemoptions = new ArrayList<>();

        switch (idTempTL) {
            case 555 ->
                itemoptions.add(new ItemOption(47, 800 * tiLe / 100));
            case 557 ->
                itemoptions.add(new ItemOption(47, 850 * tiLe / 100));
            case 559 ->
                itemoptions.add(new ItemOption(47, 900 * tiLe / 100));
            case 556 -> {
                int chiso = 52000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 558 -> {
                int chiso = 50000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 560 -> {
                int chiso = 48000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
            }
            case 562 ->
                itemoptions.add(new ItemOption(0, 4400 * tiLe / 100));
            case 564 ->
                itemoptions.add(new ItemOption(0, 4300 * tiLe / 100));
            case 566 ->
                itemoptions.add(new ItemOption(0, 4500 * tiLe / 100));
            case 563 -> {
                int chiso = 48000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso / 20));
            }
            case 565 -> {
                int chiso = 50000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso / 20));
            }
            case 567 -> {
                int chiso = 46000 * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso * 150 / 1000));
            }
            case 561 ->
                itemoptions.add(new ItemOption(14, 14 * tiLe / 100));
        }
        itemoptions.add(new ItemOption(209, 0));
        itemoptions.add(new ItemOption(21, Util.nextInt(15, 17)));
        itemoptions.add(new ItemOption(86, 1));
        itemoptions.add(new ItemOption(107, 3));
        ItemMap it = new ItemMap(zone, idTempTL, quantity, x, y, id);
        it.options.clear();
        it.options.addAll(itemoptions);
        return it;
    }

//    public ItemMap mobDropWithCustomOption2(Zone zone, int quantity, int x, int y, long id) {
//        ItemMap itemMap;
//        short itemDrop;
//
//        // Random item rơi
//        if (MapService.gI().isMapCold(zone.map.mapId) || MapService.gI().isMapNappa(zone.map.mapId)) {
//            itemDrop = Manager.ITEMS_MOB_DROP[Util.nextInt(0, 2)][Util.nextInt(0, 4)][Util.nextInt(8, 11)];
//        } else {
//            itemDrop = Manager.ITEMS_MOB_DROP[Util.nextInt(0, 2)][Util.nextInt(0, 4)][Util.nextInt(0, 8)];
//        }
//
//        itemMap = new ItemMap(zone, itemDrop, 1, x, y, id);
//
//        // Lấy option shop (KHÔNG sửa trực tiếp)
//        List<Item.ItemOption> itemOptionsShop = new ArrayList<>(ItemService.gI().getListOptionItemShop(itemMap.itemTemplate.id));
//        if (Util.isTrue(10, 100)) {
//            itemOptionsShop.add(new ItemOption(107, Util.nextInt(0, 3)));
//        }
//        for (Item.ItemOption io : itemOptionsShop) {
//
//            Item.ItemOption newIo = new Item.ItemOption(io);
//
//            int percent = Util.nextInt(0, 15); // -5% -> +10%
//            int newParam = io.param + io.param * percent / 100;
//
//            // Giới hạn tránh quá ảo
//            int max = (int) (io.param * 1.15);
//            int min = (int) (io.param * 0.95);
//
//            if (newParam > max) {
//                newParam = max;
//            }
//            if (newParam < min) {
//                newParam = min;
//            }
//
//            newIo.param = newParam;
//
//            itemMap.options.add(newIo);
//        }
//
//        // Thêm option random ngoài
//        int[] opts = {34, 35, 36};
//        int optId = opts[Util.nextInt(opts.length)];
//
//        itemMap.options.add(new Item.ItemOption(optId, Util.nextInt(1, 3)));
    ////        itemMap.options.add(new Item.ItemOption(206, Util.nextInt(1, 15)));
////        itemMap.options.add(new Item.ItemOption(86, 0));
//        itemMap.options.add(new Item.ItemOption(107, Util.nextInt(0, 3)));
//
//        return itemMap;
//    }
    public ItemMap randQuanTL(Zone zone, int quantity, int x, int y, long id) {
        short idTempTL;
        short[] quan = {556, 558, 560};
        short[] options = {86, 86};
        idTempTL = quan[Util.nextInt(3)];

        int tiLe = Util.nextInt(100, 115);
        List<ItemOption> itemoptions = new ArrayList<>();

        switch (idTempTL) {
            case 556:
                int chiso = 52000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
                if (tiLe > 100) {

                }
                break;
            case 558:
                chiso = 50000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
                if (tiLe > 100) {

                }
                break;
            case 560:
                chiso = 48000 * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso / 20));
                if (tiLe > 100) {

                }
                break;
        }
        if (Util.isTrue(30, 100)) {
            if (Util.isTrue(70, 100)) {
                itemoptions.add(new ItemOption(options[Util.nextInt(options.length)], 0));
            }
        }

        itemoptions.add(new ItemOption(21, Util.nextInt(15, 17)));

        ItemMap it = new ItemMap(zone, idTempTL, quantity, x, y, id);
        it.options.clear();
        it.options.addAll(itemoptions);
        return it;
    }
}
