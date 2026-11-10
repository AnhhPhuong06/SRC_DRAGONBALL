package models.item;

 
import models.Template;
import models.Template.ItemTemplate;
import services.ItemService;
import utils.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Item {

    public ItemTemplate template;

    public String info, message;

    public String content;

    public int quantity;

    public int quantityGD = 0;

    public List<ItemOption> itemOptions;

    public long createTime;

    public boolean isNotNullItem() {
        return this.template != null;
    }
    
    public boolean isNotLockItem() {
        if (itemOptions == null) {
            return true;
        }
        return itemOptions.stream().noneMatch(io -> io != null && io.optionTemplate != null && io.optionTemplate.id == 30);
    }

    public Item() {
        this.itemOptions = new ArrayList<>();
        this.createTime = System.currentTimeMillis();
    }

    public Item(short itemId) {
        this.template = ItemService.gI().getTemplate(itemId);
        this.itemOptions = new ArrayList<>();
        this.createTime = System.currentTimeMillis();
    }
    public boolean canConsign() {
        if (itemOptions == null) {
            return false;
        }
        for (ItemOption o : itemOptions) {
            if (o != null && o.optionTemplate != null) {
                int optionId = o.optionTemplate.id;
                if (optionId == 86 || optionId == 87) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Group mage lines for display without changing stored options or their values. */
    public List<ItemOption> getDisplayOptions() {
        List<ItemOption> display = new ArrayList<>();
        List<ItemOption> mage = new ArrayList<>();
        int mageIndex = -1;
        if (itemOptions == null) return display;
        for (ItemOption option : itemOptions) {
            if (option == null || option.optionTemplate == null) continue;
            int id = option.optionTemplate.id;
            if (id >= ItemService.MAGE_EQUIPMENT_COUNT && id <= ItemService.MAGE_CRITICAL_DAMAGE_PERCENT) {
                if (mageIndex < 0) mageIndex = display.size();
                mage.add(option);
            } else {
                display.add(option);
            }
        }
        if (mageIndex >= 0) {
            mage.sort(java.util.Comparator.comparingInt(option -> option.optionTemplate.id));
            display.addAll(mageIndex, mage);
        }
        return display;
    }

    public String getInfo() {
        String strInfo = "";
        for (ItemOption itemOption : getDisplayOptions()) {
            strInfo += itemOption.getOptionString();
        }
        return strInfo;
    }

    public String getName() {
        return template.name;
    }
    public void subOptionParam(int id, int param) {
        for (int i = 0; i < this.itemOptions.size(); i++) {
            ItemOption itemOption = this.itemOptions.get(i);
            if (itemOption != null && itemOption.optionTemplate.id == id) {
                itemOption.param -= param;
                return;
            }
        }
    }

    public String getInfoItem() {
        String strInfo = "|1|" + template.name + "\n|0|";
        for (ItemOption itemOption : getDisplayOptions()) {
            strInfo += itemOption.getOptionString() + "\n";
        }
        strInfo += "|2|" + template.description;
        return strInfo;
    }

    public String getContent() {
        if (this.template == null) {
            return "";
        }
        return "Yêu cầu sức mạnh " + this.template.strRequire + " trở lên";
    }

    public void dispose() {
        this.template = null;
        this.info = null;
        this.message = null;
        this.content = null;
        if (this.itemOptions != null) {
            for (ItemOption io : this.itemOptions) {
                io.dispose();
            }
            this.itemOptions.clear();
        }
        this.itemOptions = null;
    }

    public static class ItemOption {

        public int param;

        public Template.ItemOptionTemplate optionTemplate;

        public ItemOption() {
        }

        public ItemOption(ItemOption io) {
            this.param = io.param;
            this.optionTemplate = io.optionTemplate;
        }

        public ItemOption(int tempId, int param) {
            this.optionTemplate = ItemService.gI().getItemOptionTemplate(tempId);
            this.param = param;
        }

        public ItemOption(Template.ItemOptionTemplate temp, int param) {
            this.optionTemplate = temp;
            this.param = param;
        }

        public String getOptionString() {
            if (this.optionTemplate == null) {
                return "";
            }
            return Util.replace(this.optionTemplate.name, "#", String.valueOf(this.param));
        }

        private static final java.util.concurrent.ConcurrentHashMap<String, String> OPTION_STRING = new java.util.concurrent.ConcurrentHashMap<>();

        public String getOptionString(int param) {
            if (this.optionTemplate == null) {
                return "";
            }
            String key = this.optionTemplate.name + "#" + param;
            if (OPTION_STRING.size() > 5000) {
                OPTION_STRING.clear();
            }
            return OPTION_STRING.computeIfAbsent(key, k -> Util.replace(this.optionTemplate.name, "#", String.valueOf(param)));
        }

        public void dispose() {
            this.optionTemplate = null;
        }

        @Override
        public String toString() {
            final String n = "\"";
            return "{"
                    + n + "id" + n + ":" + n + optionTemplate.id + n + ","
                    + n + "param" + n + ":" + n + param + n
                    + "}";
        }
    }

    public boolean isSKH() {
        for (ItemOption itemOption : itemOptions) {
            if (itemOption.optionTemplate.id >= 127 && itemOption.optionTemplate.id <= 135) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isTrangBiTinhAn() {
        return this.isDTL() ||this.isDHD() || this.isDTS() || this.isSKH();
    }

    public boolean isDTS() {
        if (this.template.id >= 1048 && this.template.id <= 1062) {
            return true;
        }
        return false;
    }

    public boolean isDTL() {
        if (this.template.id >= 555 && this.template.id <= 567) {
            return true;
        }
        return false;
    }

    public boolean isDHD() {
        if (this.template.id >= 650 && this.template.id <= 662) {
            return true;
        }
        return false;
    }

    public boolean isManhTS() {
        if (this.template.id >= 1066 && this.template.id <= 1070) {
            return true;
        }
        return false;
    }

    public boolean isCongThucVip() {
        return (this.template.id >= 1071 && this.template.id <= 1073) || (this.template.id >= 1084 && this.template.id <= 1086);
    }

    public boolean isDaNangCap1() {
        if (this.template.id >= 1074 && this.template.id <= 1078) {
            return true;
        } else if (this.template.id == -1) {
        }
        return false;
    }

    public boolean isDaMayMan() {
        return this.template.id >= 1079 && this.template.id <= 1083;
    }

    public byte typeIdManh() {
        if (!isManhTS()) {
            return -1;
        }
        return switch (this.template.id) {
            case 1066 ->
                0;
            case 1067 ->
                1;
            case 1070 ->
                2;
            case 1068 ->
                3;
            case 1069 ->
                4;
            default ->
                -1;
        };
    }

    public boolean isDaNangCap() {
        if (this.template.id >= 1074 && this.template.id <= 1078) {
            return true;
        } else if (this.template.id == -1) {
        }
        return false;
    }

    public String typeName() {
        switch (this.template.type) {
            case 0:
                return "Áo";
            case 1:
                return "Quần";
            case 2:
                return "Găng";
            case 3:
                return "Giày";
            case 4:
                return "Rada";
            default:
                return "";
        }
    }
    public void addOptionParam(int id, int param) {
        for (int i = 0; i < this.itemOptions.size(); i++) {
            ItemOption itemOption = this.itemOptions.get(i);
            if (itemOption != null && itemOption.optionTemplate.id == id) {
                itemOption.param += param;
                return;
            }
        }
        this.itemOptions.add(new ItemOption(id, param));
    }
    public boolean isManhBongTai() {
        return this.template != null && (this.template.id == 933 || this.template.id == 934 || this.template.id == 935 || this.template.id == 1493);
    }

    public boolean isManhVoBongTai() {
        return this.template != null && (this.template.id == 933 || this.template.id == 1493);
    }

    public void setOptionParam(int id, int param) {
        if (this.itemOptions == null) {
            this.itemOptions = new ArrayList<>();
        }
        for (ItemOption io : this.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == id) {
                io.param = param;
                return;
            }
        }
        this.itemOptions.add(new ItemOption(id, param));
    }

    public boolean hasOption(int optionId) {
        if (this.itemOptions == null) {
            return false;
        }
        for (ItemOption io : this.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                return true;
            }
        }
        return false;
    }

    public void syncManhVoBongTai() {
        if (isManhVoBongTai()) {
            int optParam = getOptionParam(31);
            if (!hasOption(31) || optParam <= 0) {
                setOptionParam(31, this.quantity > 0 ? this.quantity : 1);
            }
            this.quantity = 1;
        }
    }

    public int getOptionParam(int id) {
        if (this.itemOptions == null) {
            return 0;
        }
        for (int i = 0; i < this.itemOptions.size(); i++) {
            ItemOption itemOption = this.itemOptions.get(i);
            if (itemOption != null && itemOption.optionTemplate != null && itemOption.optionTemplate.id == id) {
                return itemOption.param;
            }
        }
        return 0;
    }
    public boolean isHaveOption(int id) {
        if (this.itemOptions == null) {
            return false;
        }
        for (int i = 0; i < this.itemOptions.size(); i++) {
            ItemOption itemOption = this.itemOptions.get(i);
            if (itemOption != null && itemOption.optionTemplate != null && itemOption.optionTemplate.id == id) {
                return true;
            }
        }
        return false;
    }
    public boolean canPhaLeHoa() {
        if (this.template == null) {
            return false;
        }
        int type = this.template.type;
        // Đồ thường (0-4) và Bông tai (32). Phụ kiện (5, 11, 21, 23, 24) không cho đập
        return (type >= 0 && type <= 4) || type == 32;
    }
    public String getOptionInfo() {
        StringJoiner optionInfo = new StringJoiner("\n");
        for (ItemOption io : this.itemOptions) {
            if (io.optionTemplate.id != 72 && io.optionTemplate.id != 73 && io.optionTemplate.id != 102 && io.optionTemplate.id != 107 && io.optionTemplate.id != 218) {
                optionInfo.add(io.getOptionString());
            }
        }
        return optionInfo.toString();
    }
}
