package services.player;


import models.item.Item;
import models.item.Item.ItemOption;
import models.npc.specialnpc.MabuEgg;
import models.player.Inventory;
import models.player.Pet;
import models.player.Player;
import network.io.Message;
import services.ItemService;
import services.Service;
import services.dungeon.NgocRongNamecService;
import services.map.ChangeMapService;

import java.util.ArrayList;
import java.util.List;
import models.boss.Boss;
import models.map.ItemMap;
import models.npc.specialnpc.BillEgg;
import services.dungeon.BlackBallWarService;
import services.map.ItemMapService;

public class InventoryService {

    private static final int[] MAIN_HIDDEN_STATS = {50, 77, 103};
    private static final int[] SECONDARY_HIDDEN_STATS = {5, 94, 108, 95, 96};

    private static InventoryService I;

    public static InventoryService gI() {
        if (InventoryService.I == null) {
            InventoryService.I = new InventoryService();
        }
        return InventoryService.I;
    }

    private void __________________Tìm_kiếm_item_____________________________() {
        //**********************************************************************
    }

    public Item findItemBagByTemp(Player player, int tempId) {
        return findItem(player.inventory.itemsBag, tempId);
    }

    public Item findItem(List<Item> list, int tempId) {
        try {
            for (Item item : list) {
                if (item.isNotNullItem() && item.template.id == tempId) {
                    return item;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Item findItemBody(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBody, tempId);
    }

    public Item findItemBag(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBag, tempId);
    }

    public Item findItemBox(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBox, tempId);
    }
    public boolean hasAllItemsBag(Player player, int fromId, int toId) {
        for (int id = fromId; id <= toId; id++) {
            Item it = findItemBag(player, id);
            if (it == null || !it.isNotNullItem() || it.quantity < 1) {
                return false;
            }
        }
        return true;
    }
    public Item findItemInAllInventories(Player player, int tempId) {
        Item bodyItem = this.findItem(player.inventory.itemsBody, tempId);
        if (bodyItem != null) {
            return bodyItem;
        }
        Item bagItem = this.findItem(player.inventory.itemsBag, tempId);
        if (bagItem != null) {
            return bagItem;
        }
        Item boxItem = this.findItem(player.inventory.itemsBox, tempId);
        if (boxItem != null) {
            return boxItem;
        }
        return null;
    }

    public boolean isExistItem(List<Item> list, int tempId) {
        try {
            return this.findItem(list, tempId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Item findItem(Player player, int id, int quantity) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && (item.template.id == id) && item.quantity >= quantity) {
                return item;
            }
        }
        return null;
    }

    public boolean isExistItemBody(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBody, tempId);
    }

    public boolean isExistItemBag(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBag, tempId);
    }

    public boolean isExistItemBox(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBox, tempId);
    }

    private void __________________Sao_chép_danh_sách_item__________________() {
        //**********************************************************************
    }

    public List<Item> copyList(List<Item> items) {
        List<Item> list = new ArrayList<>();
        for (Item item : items) {
            list.add(ItemService.gI().copyItem(item));
        }
        return list;
    }

    public List<Item> copyItemsBody(Player player) {
        return copyList(player.inventory.itemsBody);
    }

    public List<Item> copyItemsBag(Player player) {
        return copyList(player.inventory.itemsBag);
    }

    public List<Item> copyItemsBox(Player player) {
        return copyList(player.inventory.itemsBox);
    }

    private void __________________Vứt_bỏ_item______________________________() {
        //**********************************************************************
    }

    public void throwItem(Player player, int where, int index) {
        if (where == 0) {
            Service.gI().sendThongBao(player, "Không thể vứt bỏ trang bị sử dụng");
            return;
        }
        Item itemThrow = null;
        if (where == 0) {
            itemThrow = player.inventory.itemsBody.get(index);
            removeItemBody(player, index);
            sendItemBody(player);
            Service.gI().Send_Caitrang(player);
        } else if (where == 1) {
            itemThrow = player.inventory.itemsBag.get(index);
            if (itemThrow.template != null && itemThrow.template.id == 673) {
                Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                return;
            }
            if (itemThrow.template != null && itemThrow.template.id != 457) {
                removeItemBag(player, index);
                sortItems(player.inventory.itemsBag);
                sendItemBags(player);
            } else {
                Service.gI().sendThongBao(player, "Không thể bỏ ra thỏi vàng.");
            }
        }
        if (itemThrow == null) {
            return;
        }
    }

    private void __________________Xoá_bỏ_item______________________________() {
        //**********************************************************************
    }

    public void removeItem(List<Item> items, int index) {
        if (items == null || index < 0 || index >= items.size()) {
            return;
        }
        synchronized (items) {
            Item item = ItemService.gI().createItemNull();
            items.set(index, item);
        }
    }

    public void removeItem(List<Item> items, Item item) {
        if (items == null || item == null) {
            return;
        }
        synchronized (items) {
            Item it = ItemService.gI().createItemNull();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).equals(item)) {
                    items.set(i, it);
                    break;
                }
            }
        }
    }

    public void removeItemBag(Player player, int index) {
        this.removeItem(player.inventory.itemsBag, index);
    }

    public void removeItemBag(Player player, Item item) {
        this.removeItem(player.inventory.itemsBag, item);
    }

    public void removeItemBody(Player player, int index) {
        this.removeItem(player.inventory.itemsBody, index);
    }

    public void removeItemPetBody(Player player, int index) {
        this.removeItemBody(player.pet, index);
    }

    public void removeItemBox(Player player, int index) {
        this.removeItem(player.inventory.itemsBox, index);
    }

    private void __________________Giảm_số_lượng_item_______________________() {
        //**********************************************************************
    }

    public void subQuantityItemsBag(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBag, item, quantity);
    }

    public int getNumItemBag(Player player, int tempId) {
        int total = 0;
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == tempId) {
                total += item.quantity;
            }
        }
        return total;
    }

    public void subQuantityItemsBag(Player player, int tempId, int quantity) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null || quantity <= 0) {
            return;
        }
        synchronized (player.inventory.itemsBag) {
            for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                Item item = player.inventory.itemsBag.get(i);
                if (item.isNotNullItem() && item.template.id == tempId) {
                    if (item.quantity > quantity) {
                        item.quantity -= quantity;
                        if (item.isManhVoBongTai() && item.hasOption(31)) {
                            item.quantity = 1;
                        }
                        break;
                    } else {
                        quantity -= item.quantity;
                        removeItemBag(player, i);
                    }
                }
            }
        }
    }

    public int getNumItemBagWithoutOption(Player player, int tempId, int optionId) {
        int total = 0;
        if (player != null && player.inventory != null && player.inventory.itemsBag != null) {
            for (Item item : player.inventory.itemsBag) {
                if (item.isNotNullItem() && item.template.id == tempId && !item.hasOption(optionId)) {
                    total += item.quantity;
                }
            }
        }
        return total;
    }

    public void subQuantityItemsBagWithoutOption(Player player, int tempId, int quantity, int optionId) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null || quantity <= 0) {
            return;
        }
        synchronized (player.inventory.itemsBag) {
            for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                Item item = player.inventory.itemsBag.get(i);
                if (item.isNotNullItem() && item.template.id == tempId && !item.hasOption(optionId)) {
                    if (item.quantity > quantity) {
                        item.quantity -= quantity;
                        break;
                    } else {
                        quantity -= item.quantity;
                        removeItemBag(player, i);
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public void subQuantityItemsBody(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBody, item, quantity);
    }

    public void subQuantityItemsBox(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBox, item, quantity);
    }

    public void subQuantityItem(List<Item> items, Item item, int quantity) {
        if (items == null || item == null || quantity <= 0) {
            return;
        }
        synchronized (items) {
            for (Item it : items) {
                if (item.equals(it)) {
                    it.quantity -= quantity;
                    if (it.quantity <= 0) {
                        removeItem(items, item);
                    } else if (it.isManhVoBongTai() && it.hasOption(31)) {
                        it.quantity = 1;
                    }
                    break;
                }
            }
        }
    }

    private void __________________Sắp_xếp_danh_sách_item___________________() {
        //**********************************************************************
    }

    public void sortItems(List<Item> list) {
        if (list == null) {
            return;
        }
        synchronized (list) {
            mergeThoiVang(list);
            mergeManhBongTai(list);
            sortItemsInternal(list);
        }
    }

    public void mergeManhBongTai(List<Item> list) {
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Item itemA = list.get(i);
            if (itemA == null || !itemA.isNotNullItem() || !itemA.isManhBongTai() || itemA.quantity >= 500_000_000) {
                continue;
            }
            if (itemA.isManhVoBongTai()) {
                itemA.syncManhVoBongTai();
            }
            for (int j = i + 1; j < list.size(); j++) {
                Item itemB = list.get(j);
                if (itemB == null || !itemB.isNotNullItem() || itemB.template.id != itemA.template.id) {
                    continue;
                }
                // Chỉ gộp khi các option khác (như KGD, HSD) giống nhau, và cùng có hoặc cùng không có option 31
                if (!checkOptionsEqualManhBongTai(itemA.itemOptions, itemB.itemOptions)) {
                    continue;
                }
                if (itemB.isManhVoBongTai()) {
                    itemB.syncManhVoBongTai();
                }
                if (itemA.isManhVoBongTai() && itemB.isManhVoBongTai()) {
                    int optA = itemA.getOptionParam(31);
                    int optB = itemB.getOptionParam(31);
                    long total = (long) optA + optB;
                    if (total <= 500_000_000L) {
                        itemA.setOptionParam(31, (int) total);
                        itemA.quantity = 1;
                        list.set(j, ItemService.gI().createItemNull());
                    } else {
                        itemA.setOptionParam(31, 500_000_000);
                        itemA.quantity = 1;
                        itemB.setOptionParam(31, (int) (total - 500_000_000L));
                        itemB.quantity = 1;
                    }
                } else if (!itemA.isManhVoBongTai()) {
                    long space = 500_000_000L - itemA.quantity;
                    if (space > 0) {
                        int add = (int) Math.min(space, (long) itemB.quantity);
                        itemA.quantity += add;
                        itemB.quantity -= add;
                        if (itemA.itemOptions.isEmpty()) {
                            itemA.itemOptions.add(new Item.ItemOption(73, 0));
                        }
                        if (itemB.quantity <= 0) {
                            list.set(j, ItemService.gI().createItemNull());
                        }
                    }
                }
                if (itemA.quantity >= 500_000_000) {
                    break;
                }
            }
        }
    }

    private void sortItemsInternal(List<Item> list) {
        int target = 0;
        for (int i = 0; i < list.size(); i++) {
            Item it = list.get(i);
            if (it != null && it.isNotNullItem()) {
                if (target != i) {
                    list.set(target, it);
                    list.set(i, ItemService.gI().createItemNull());
                }
                target++;
            }
        }
        for (int i = target; i < list.size(); i++) {
            if (list.get(i) == null) {
                list.set(i, ItemService.gI().createItemNull());
            }
        }
    }

    /**
     * Gộp các stack thỏi vàng (ID 457) CÙNG options vào nhau (tối đa 500.000.000).
     * Các stack có options khác nhau giữ nguyên ô riêng biệt, không gộp.
     */
    public void mergeThoiVang(List<Item> list) {
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Item itemA = list.get(i);
            if (itemA == null || !itemA.isNotNullItem() || itemA.template.id != 457 || itemA.quantity >= 500_000_000) {
                continue;
            }
            for (int j = i + 1; j < list.size(); j++) {
                Item itemB = list.get(j);
                if (itemB == null || !itemB.isNotNullItem() || itemB.template.id != 457) {
                    continue;
                }
                // Cùng options thì gộp, khác options thì không gộp
                if (checkListsEqual(itemA.itemOptions, itemB.itemOptions)) {
                    long space = 500_000_000L - itemA.quantity;
                    if (space > 0) {
                        int add = (int) Math.min(space, (long) itemB.quantity);
                        itemA.quantity += add;
                        itemB.quantity -= add;
                        if (itemA.itemOptions.isEmpty()) {
                            itemA.itemOptions.add(new Item.ItemOption(73, 0));
                        }
                        if (itemB.quantity <= 0) {
                            list.set(j, ItemService.gI().createItemNull());
                        }
                    }
                    if (itemA.quantity >= 500_000_000) {
                        break;
                    }
                }
            }
        }
    }

    public void sortItemv2(List<Item> items) {
        int index = 0;
        for (Item item : items) {
            if (item != null && item.quantity > 0) {
                items.set(index, item);
                index++;
            }
        }
        for (int i = index; i < items.size(); i++) {
            items.set(i, null);
        }
    }

    private void __________________Thao_tác_tháo_mặc_item___________________() {
        //**********************************************************************
    }

    private Item putItemBag(Player player, Item item) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null) {
            return item;
        }
        synchronized (player.inventory.itemsBag) {
            for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                if (!player.inventory.itemsBag.get(i).isNotNullItem()) {
                    player.inventory.itemsBag.set(i, item);
                    Item sItem = ItemService.gI().createItemNull();
                    return sItem;
                }
            }
            return item;
        }
    }

    private Item putItemBox(Player player, Item item) {
        if (player == null || player.inventory == null || player.inventory.itemsBox == null) {
            return item;
        }
        synchronized (player.inventory.itemsBox) {
            for (int i = 0; i < player.inventory.itemsBox.size(); i++) {
                if (!player.inventory.itemsBox.get(i).isNotNullItem()) {
                    player.inventory.itemsBox.set(i, item);
                    Item sItem = ItemService.gI().createItemNull();
                    return sItem;
                }
            }
            return item;
        }
    }

    private Item putItemBody(Player player, Item item) {
        if (!item.isNotNullItem()) {
            return item;
        }

        // Kiểm tra type item
        switch (item.template.type) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 32:
            case 23:
            case 24:
            case 11:
            case 36:
            case 35:
            case 27:
            case 21:
            case 74:
                break;
            default:
                Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Trang bị không phù hợp!");
                return item;
        }

        // Kiểm tra gender
        if (item.template.gender < 3 && item.template.gender != player.gender) {
            Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Trang bị không phù hợp!");
            return item;
        }

        // Kiểm tra sức mạnh
        long powerRequire = item.template.strRequire;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 21) {
                powerRequire = io.param * 1_000_000_000L;
                break;
            }
        }
        if (player.nPoint.power < powerRequire) {
            Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Sức mạnh không đủ yêu cầu!");
            return item;
        }

        // Xác định index trong itemsBody
        int index = -1;
        switch (item.template.type) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                index = item.template.type;
                break;
            case 32:
                index = 6;
                break;
            case 23:
            case 24:
                index = 9;
                break;
            case 11:
                index = 8;
                break;
            case 36:
                index = 10;
                break;
            case 35:
                index = 11;
                break;
            case 21:
            case 27:
                index = 7;
                break;
           case 74:
               index = 12;
               break;
        }

        if (index < 0) {
            Service.gI().sendThongBao(player, "Không thể trang bị vật phẩm, index không hợp lệ!");
            return item;
        }

        if (player.isPet) {
            int typePet = ((Pet) player).typePet;
            if (typePet == 0) {
                // Đệ tử thường chỉ mặc được 7 món cơ bản (quần áo, găng, giày, radar, cải trang, giáp luyện tập: index 0 - 6)
                if (index >= 7) {
                    Service.gI().sendThongBaoOK(((Pet) player).master, "Đệ tử thường không thể trang bị ô phụ kiện này!");
                    return item;
                }
            } else {
                // Các loại đệ tử khác (Mabư, Beerus, Black, Ubu, Jiren...) được mặc thêm Pet (index 7) và Vật phẩm đeo lưng (index 8)
                if (index > 8) {
                    Service.gI().sendThongBaoOK(((Pet) player).master, "Đệ tử không thể trang bị vật phẩm này!");
                    return item;
                }
            }
        }

        while (player.inventory.itemsBody.size() <= index) {
            player.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        Item oldItem = player.inventory.itemsBody.get(index);

        if ((index == 8 || index == 10) && oldItem.isNotNullItem()) {
            Service.gI().removeEffPlayer(player, oldItem.template.part);
        }

        player.inventory.itemsBody.set(index, item);
        return oldItem;
    }

    private int realStatId(int hiddenId) {
        switch (hiddenId) {
            case ItemService.RANDOM_SDCM_50_200: return 5; // +50-200% sức đánh chí mạng
            case ItemService.RANDOM_SD_HP_KI_1_30:
            case ItemService.RANDOM_SD_HP_KI_20_50: return ItemService.PERCENT_SD_HP_KI;
            case 248: return 50;  // +#% sức đánh
            case 249: return 77;  // +#% HP
            case 250: return 103; // +#% KI
            case 251: return 5;   // +#% sức đánh chí mạng
            default:  return -1;
        }
    }

    public boolean revealHiddenStats(Player pl, Item item) {
        if (item == null || item.itemOptions == null) {
            return false;
        }
        java.util.List<Item.ItemOption> toAdd = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();

        boolean hasOpt247 = item.itemOptions.stream().anyMatch(io -> io.optionTemplate != null && io.optionTemplate.id == 247);
        if (hasOpt247) {
            List<Item.ItemOption> added = ItemService.gI().removeAndAddOptionTemplate(item.itemOptions, 247);
            for (Item.ItemOption io : added) {
                sb.append("\n").append(io.getOptionString());
            }
        }

        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate == null) continue;
            int hiddenId = io.optionTemplate.id;
            int realId = realStatId(hiddenId);
            int param;
            if (hiddenId == ItemService.HIDDEN_MAIN_STAT || hiddenId == ItemService.HIDDEN_SECONDARY_STAT) {
                int[] pool = hiddenId == ItemService.HIDDEN_MAIN_STAT ? MAIN_HIDDEN_STATS : SECONDARY_HIDDEN_STATS;
                realId = pool[utils.Util.nextInt(pool.length)];
                param = io.param;
            } else if (realId != -1) {
                int min = switch (hiddenId) {
                    case ItemService.RANDOM_SDCM_50_200 -> 50;
                    case ItemService.RANDOM_SD_HP_KI_20_50 -> 20;
                    default -> 1;
                };
                int max = switch (hiddenId) {
                    case ItemService.RANDOM_SDCM_50_200 -> 200;
                    case ItemService.RANDOM_SD_HP_KI_1_30 -> 30;
                    default -> 50;
                };
                param = utils.Util.nextInt(min, max);
            } else {
                continue;
            }
            Item.ItemOption real = new Item.ItemOption(realId, param);
            toAdd.add(real);
            sb.append("\n").append(real.getOptionString());
        }
        if (toAdd.isEmpty() && !hasOpt247) {
            return false;
        }
        if (!toAdd.isEmpty()) {
            item.itemOptions.removeIf(io -> io.optionTemplate != null
                    && (realStatId(io.optionTemplate.id) != -1
                    || io.optionTemplate.id == ItemService.HIDDEN_MAIN_STAT
                    || io.optionTemplate.id == ItemService.HIDDEN_SECONDARY_STAT));
            item.itemOptions.addAll(toAdd);
        }
        sendItemBags(pl);
        Service.gI().sendThongBao(pl, "Đã mở chỉ số ẩn trên " + item.template.name + ":" + sb);
        return true;
    }

    public void itemBagToBody(Player player, int index) {
        if (index < 0 || index >= player.inventory.itemsBag.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện, index không hợp lệ");
            return;
        }

        Item item = player.inventory.itemsBag.get(index);
        if (!item.isNotNullItem()) {
            return;
        }

        revealHiddenStats(player, item); // mở chỉ số ẩn (194-198, 248-251) một lần khi mặc
        player.inventory.itemsBag.set(index, putItemBody(player, item));

        if (item.template.id > 1550 && item.template.id < 1560) {
            Service.gI().removeTitle(player);
            Service.gI().sendFoot(player, item.template.id);
        }

        sendItemBags(player);
        sendItemBody(player);
        Service.gI().point(player);
        Service.gI().Send_Caitrang(player);
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Service.gI().sendEffPlayer(player);
            } catch (Exception e) {
            }
        }).start();
    }

    public void itemBodyToBag(Player player, int index) {
        if (index < 0 || index > 12) {
            return;
        }
        Item item = player.inventory.itemsBody.get(index);
        if (item.isNotNullItem()) {
            if (index == 10) {
                Service.gI().sendPetFollow(player, (short) 0);
            }
            if (index == 7 && !player.isPet) {
                if (player.newPet != null) {
                    ChangeMapService.gI().exitMap(player.newPet);
                    player.newPet.dispose();
                    player.newPet = null;
                }
            }
            player.inventory.itemsBody.set(index, putItemBag(player, item));
            if (item.isNotNullItem()) {
                Service.gI().removeTitle(player);
                // Service.gI().sendFoot(player, item.template.id);
            }
            sendItemBags(player);
            sendItemBody(player);
            if (index == 8 || index == 10) {
                if (item.isNotNullItem()) {
                    Service.gI().removeEffPlayer(player, item.template.part);
                }
            }
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player);
            // Tháo đồ ra có thể làm mất hoặc lộ danh hiệu ẩn của bộ pha lê nên vẽ lại.
            Service.gI().sendEffPlayer(player);
        }
    }

    public void itemBagToPetBody(Player player, int index) {
        try {
            if (player.pet != null) {
                Item item = player.inventory.itemsBag.get(index);
                if (item.isNotNullItem()) {
                    boolean isTrainArmor = item.template.type == 32 || ItemService.gI().isTrainArmor(item);
                    if (player.pet.nPoint.power < 1500000 && !isTrainArmor) {
                        Service.gI().sendThongBao(player, "Đệ tử phải đạt 1tr5 sức mạnh mới có thể mặc");
                        return;
                    }
                    if (item.template.id == 454 || item.template.id == 921 || item.template.id == 1492) {
                        Service.gI().sendThongBao(player, "Bông tai hợp thể không thể trang bị cho đệ tử");
                        return;
                    }
                    revealHiddenStats(player, item);
                    Item itemSwap = putItemBody(player.pet, item);
                    player.inventory.itemsBag.set(index, itemSwap);
                    sendItemBags(player);
                    sendItemBody(player);
                    if (!itemSwap.equals(item)) {
                        Service.gI().point(player);
                        Service.gI().showInfoPet(player);
                    }
                    Service.gI().Send_Caitrang(player.pet);
                    Service.gI().Send_Caitrang(player);
                    Service.gI().sendFlagBag(player.pet);
                }
            } else {
                Service.gI().sendThongBao(player, "Không có đệ tử");
            }
        } catch (Exception E) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    public void itemPetBodyToBag(Player player, int index) {
        if (player == null || player.pet == null || player.pet.inventory == null || index < 0 || index >= player.pet.inventory.itemsBody.size()) {
            return;
        }
        Item item = player.pet.inventory.itemsBody.get(index);
        if (item.isNotNullItem()) {
            if (index == 8 || index == 10) {
                Service.gI().removeEffPlayer(player.pet, item.template.part);
            }
            player.pet.inventory.itemsBody.set(index, putItemBag(player, item));
            sendItemBags(player);
            sendItemBody(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player.pet);
            Service.gI().Send_Caitrang(player);
            Service.gI().sendFlagBag(player.pet);
            Service.gI().showInfoPet(player);
        }
    }

    public void itemBoxToBodyOrBag(Player player, int index) {
        if (player == null || player.inventory == null || player.inventory.itemsBox == null || player.inventory.itemsBag == null) {
            return;
        }
        if (index < 0 || index >= player.inventory.itemsBox.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = player.inventory.itemsBox.get(index);
        if (item != null && item.isNotNullItem()) {
            int originalQty = item.quantity;
            if (addItemList(player.inventory.itemsBag, item)) {
                if (item.quantity == 0) {
                    Item sItem = ItemService.gI().createItemNull();
                    player.inventory.itemsBox.set(index, sItem);
                }
                sortItems(player.inventory.itemsBag);
                sortItems(player.inventory.itemsBox);
                sendItemBags(player);
                sendItemBox(player);
            } else {
                if (item.quantity < originalQty) {
                    sortItems(player.inventory.itemsBag);
                    sortItems(player.inventory.itemsBox);
                    sendItemBags(player);
                    sendItemBox(player);
                }
                Service.gI().sendThongBao(player, "Hành trang đã đầy");
            }
        }
    }

    /**
     * Chuyển tất cả vật phẩm từ rương đồ (itemsBox) sang hành trang (itemsBag).
     */
    public void itemBoxToBagAll(Player player) {
        if (player == null || player.inventory == null || player.inventory.itemsBox == null || player.inventory.itemsBag == null) {
            return;
        }
        int countSuccess = 0;
        boolean bagFull = false;
        for (int i = 0; i < player.inventory.itemsBox.size(); i++) {
            Item item = player.inventory.itemsBox.get(i);
            if (item != null && item.isNotNullItem()) {
                if (addItemList(player.inventory.itemsBag, item)) {
                    countSuccess++;
                    if (item.quantity == 0) {
                        player.inventory.itemsBox.set(i, ItemService.gI().createItemNull());
                    }
                } else {
                    bagFull = true;
                    break;
                }
            }
        }
        sendItemBags(player);
        sendItemBox(player);
        if (countSuccess > 0) {
            if (bagFull) {
                Service.gI().sendThongBao(player, "Hành trang đã đầy! Đã chuyển " + countSuccess + " món từ rương sang hành trang.");
            } else {
                Service.gI().sendThongBao(player, "Đã chuyển toàn bộ vật phẩm từ rương sang hành trang.");
            }
        } else {
            if (bagFull) {
                Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống!");
            } else {
                Service.gI().sendThongBao(player, "Rương đồ hiện không có vật phẩm nào.");
            }
        }
    }

    public void itemBagToBox(Player player, int index) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null || player.inventory.itemsBox == null) {
            return;
        }
        if (index < 0 || index >= player.inventory.itemsBag.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = player.inventory.itemsBag.get(index);
        if (item != null && item.isNotNullItem()) {
            if (addItemBox(player, item)) {
                if (item.quantity == 0) {
                    Item sItem = ItemService.gI().createItemNull();
                    player.inventory.itemsBag.set(index, sItem);
                }
                sortItems(player.inventory.itemsBag);
                sortItems(player.inventory.itemsBox);
                sendItemBags(player);
                sendItemBox(player);
                Service.gI().point(player);
            } else {
                Service.gI().sendThongBao(player, "Rương đồ đã đầy");
            }
        }
    }

    public void itemBodyToBox(Player player, int index) {
        if (player == null || player.inventory == null || player.inventory.itemsBody == null) {
            return;
        }
        if (index < 0 || index >= player.inventory.itemsBody.size()) {
            return;
        }
        Item item = player.inventory.itemsBody.get(index);
        if (item != null && item.isNotNullItem()) {
            player.inventory.itemsBody.set(index, putItemBox(player, item));
            sortItems(player.inventory.itemsBag);
            sortItems(player.inventory.itemsBox);
            sendItemBody(player);
            sendItemBox(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player);
            // Cất đồ vào rương cũng là tháo đồ: bộ pha lê gãy thì danh hiệu ẩn phải tắt.
            Service.gI().sendEffPlayer(player);
        }
    }

    private void __________________Gửi_danh_sách_item_cho_người_chơi________() {
        //**********************************************************************
    }

    public void sendItemBags(Player player) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null) {
            return; 
        }
        java.util.List<Item> itemsBag = player.inventory.itemsBag;
        sortItems(itemsBag);
        Message msg;
        try {
            msg = new Message(-36);
            msg.writer().writeByte(0);
            msg.writer().writeByte(itemsBag.size());
            for (int i = 0; i < itemsBag.size(); i++) {
                Item item = itemsBag.get(i);
                if (!item.isNotNullItem()) {
                    msg.writer().writeShort(-1);
                    continue;
                }
                if (item.isManhVoBongTai()) {
                    item.syncManhVoBongTai();
                }
                int q = item.quantity;
                if (item.isManhVoBongTai() && item.hasOption(31)) {
                    q = item.getOptionParam(31);
                }
                msg.writer().writeShort(item.template.id);
                msg.writer().writeInt(q);
                msg.writer().writeUTF(item.getInfo());
                msg.writer().writeUTF(item.getContent());
                java.util.List<Item.ItemOption> opts = item.getDisplayOptions();
                msg.writer().writeByte(opts.size()); //options
                for (Item.ItemOption io : opts) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Service.gI().sendFlagBag(player);
        Service.gI().Send_Caitrang(player);
    }

    public void sendItemBody(Player player) {
        Message msg;
        try {
            msg = new Message(-37);
            msg.writer().writeByte(0);
            msg.writer().writeShort(player.getHead());
            msg.writer().writeByte(player.inventory.itemsBody.size());
            for (Item item : player.inventory.itemsBody) {
                if (!item.isNotNullItem()) {
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeShort(item.template.id);
                    msg.writer().writeInt(item.quantity);
                    msg.writer().writeUTF(item.getInfo());
                    msg.writer().writeUTF(item.getContent());
                    List<Item.ItemOption> itemOptions = item.getDisplayOptions();
                    msg.writer().writeByte(itemOptions.size());
                    for (Item.ItemOption itemOption : itemOptions) {
                        msg.writer().writeByte(itemOption.optionTemplate.id);
                        msg.writer().writeShort(itemOption.param);
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Service.gI().sendFlagBag(player);
        Service.gI().Send_Caitrang(player);
    }

    public void sendItemBox(Player player) {
        if (player == null || player.inventory == null || player.inventory.itemsBox == null) {
            return;
        }
        sortItems(player.inventory.itemsBox);
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(0);
            msg.writer().writeByte(player.inventory.itemsBox.size());
            for (Item it : player.inventory.itemsBox) {
                msg.writer().writeShort(it.isNotNullItem() ? it.template.id : -1);
                if (it.isNotNullItem()) {
                    if (it.isManhVoBongTai()) {
                        it.syncManhVoBongTai();
                    }
                    int q = it.quantity;
                    if (it.isManhVoBongTai() && it.hasOption(31)) {
                        q = it.getOptionParam(31);
                    }
                    msg.writer().writeInt(q);
                    msg.writer().writeUTF(it.getInfo());
                    msg.writer().writeUTF(it.getContent());
                    List<Item.ItemOption> optsBox = it.getDisplayOptions();
                    msg.writer().writeByte(optsBox.size());
                    for (Item.ItemOption io : optsBox) {
                        msg.writer().writeByte(io.optionTemplate.id);
                        msg.writer().writeShort(io.param);
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.openBox(player);
    }

    public void openBox(Player player) {
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(1);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    private void __________________Thêm_vật_phẩm_vào_danh_sách______________() {
        //**********************************************************************
    }

    private boolean addItemSpecial(Player player, Item item) {
        //bùa
        if (item.template.type == 13) {
            int min = 0;
            try {
                String tagShopBua = player.idMark.getShopOpen().tagName;
                if (tagShopBua.equals("BUA_1H")) {
                    min = 60;
                } else if (tagShopBua.equals("BUA_8H")) {
                    min = 60 * 8;
                } else if (tagShopBua.equals("BUA_1M")) {
                    min = 60 * 24 * 30;
                }
            } catch (Exception e) {
            }
            player.charms.addTimeCharms(item.template.id, min);
            return true;
        }

        switch (item.template.id) {
            case 453: //tàu tennis
                player.haveTennisSpaceShip = true;
                return true;
            case 74: //đùi gà nướng
                player.nPoint.setFullHpMp();
                PlayerService.gI().sendInfoHpMp(player);
                return true;
        }
        return false;
    }

    public boolean addItemBag(Player player, Item item) {
        if (player == null || player.inventory == null || item == null || item.template == null || item.quantity <= 0) {
            return false;
        }
        //ngọc rồng đen
        if (ItemMapService.gI().isBlackBall(item.template.id)) {
            return BlackBallWarService.gI().pickBlackBall(player, item);
        }

        //ngọc rồng namek
        if (ItemMapService.gI().isNamecBall(item.template.id) || ItemMapService.gI().isNamecBallStone(item.template.id)) {
            return NgocRongNamecService.gI().pickNamekBall(player, item);
        }
        if (addItemSpecial(player, item)) {
            return true;
        }

        //gold, gem, ruby
        switch (item.template.type) {
            case 9:
                long goldGain = (long) item.quantity * (player.effectSkill.isChibi && player.typeChibi == 0 ? 2L : 1L);
                if (goldGain > 0 && player.inventory.gold <= Inventory.LIMIT_GOLD - goldGain) {
                    player.inventory.gold += goldGain;
                    Service.gI().sendMoney(player);
                    return true;
                } else {
                    Service.gI().sendThongBao(player, "Vàng sau khi nhặt quá giới hạn cho phép");
                    return false;
                }
            case 10:
                long gem = (long) player.inventory.gem + (long) item.quantity;
                if (gem > Integer.MAX_VALUE) {
                    gem = Integer.MAX_VALUE;
                }
                player.inventory.gem = (int) gem;
                Service.gI().sendMoney(player);
                return true;
            case 34:
                long ruby = (long) player.inventory.ruby + (long) item.quantity;
                if (ruby > Integer.MAX_VALUE) {
                    ruby = Integer.MAX_VALUE;
                }
                player.inventory.ruby = (int) ruby;
                Service.gI().sendMoney(player);
                return true;
        }

        //mở rộng hành trang - rương đồ
        if (item.template.id == 517) {
            if (player.inventory.itemsBag.size() < Inventory.MAX_ITEMS_BAG) {
                player.inventory.itemsBag.add(ItemService.gI().createItemNull());
                Service.gI().sendThongBaoOK(player, "Hành trang của bạn đã được mở rộng thêm 1 ô");
                return true;
            } else {
                Service.gI().sendThongBaoOK(player, "Hành trang của bạn đã đạt tối đa");
                return false;
            }
        } else if (item.template.id == 518) {
            if (player.inventory.itemsBox.size() < Inventory.MAX_ITEMS_BOX) {
                player.inventory.itemsBox.add(ItemService.gI().createItemNull());
                Service.gI().sendThongBaoOK(player, "Rương đồ của bạn đã được mở rộng thêm 1 ô");
                return true;
            } else {
                Service.gI().sendThongBaoOK(player, "Rương đồ của bạn đã đạt tối đa");
                return false;
            }
        }
        return addItemList(player.inventory.itemsBag, item);
    }

    public boolean addItemBox(Player player, Item item) {
        return addItemList(player.inventory.itemsBox, item);
    }

    public boolean addItemList(List<Item> items, Item itemAdd) {
        if (items == null || itemAdd == null || itemAdd.template == null || itemAdd.quantity <= 0) {
            return false;
        }
        synchronized (items) {
        //nếu item ko có option, add option rỗng vào
        if (itemAdd.itemOptions.isEmpty()) {
            itemAdd.itemOptions.add(new Item.ItemOption(73, 0));
        }
        if (itemAdd.template.id >= 925 && itemAdd.template.id <= 931) {
            itemAdd.itemOptions.add(new Item.ItemOption(93, 2));
        }

        // Mảnh vỡ bông tai (ID 933, 1493): rơi / nhặt được cộng thẳng vào option 31
        if (itemAdd.isManhVoBongTai()) {
            itemAdd.syncManhVoBongTai();
            int addOpt = itemAdd.getOptionParam(31);
            for (Item it : items) {
                if (it != null && it.isNotNullItem() && it.template.id == itemAdd.template.id) {
                    it.syncManhVoBongTai();
                    int itOpt = it.getOptionParam(31);
                    long total = (long) itOpt + addOpt;
                    if (total <= 500_000_000L) {
                        it.setOptionParam(31, (int) total);
                        it.quantity = 1;
                        itemAdd.quantity = 0;
                        itemAdd.setOptionParam(31, 0);
                        return true;
                    } else {
                        int canAdd = 500_000_000 - itOpt;
                        it.setOptionParam(31, 500_000_000);
                        it.quantity = 1;
                        addOpt -= canAdd;
                        itemAdd.setOptionParam(31, addOpt);
                        itemAdd.quantity = 1;
                    }
                }
            }
        }

        //item cộng thêm chỉ số param: tự động luyện tập - bí kiếp
        int[] idParam = isItemIncrementalOption(itemAdd);
        if (idParam[0] != -1 && !itemAdd.isManhBongTai()) {
            for (Item it : items) {
                if (it.isNotNullItem() && it.template.id == itemAdd.template.id) {
                    boolean found = false;
                    for (Item.ItemOption io : it.itemOptions) {
                        if (io.optionTemplate.id == idParam[0]) {
                            io.param += idParam[1];
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        it.itemOptions.add(new Item.ItemOption(idParam[0], idParam[1]));
                    }
                    itemAdd.quantity = 0;
                    return true;
                }
            }
        }

        //item tăng số lượng
        if (itemAdd.template.isUpToUp || itemAdd.template.id == 457) {
            for (Item it : items) {
                if (!it.isNotNullItem() || it.template.id != itemAdd.template.id || it.quantity >= 500_000_000) {
                    continue;
                }
                boolean optionsMatch;
                if (itemAdd.isManhBongTai()) {
                    // Mảnh bông tai: so sánh options (bỏ qua khác biệt param số lượng của option 31)
                    optionsMatch = checkOptionsEqualManhBongTai(it.itemOptions, itemAdd.itemOptions);
                } else if (itemAdd.template.id == 2074 || itemAdd.isDaNangCap() || itemAdd.isManhTS()) {
                    optionsMatch = true;
                } else {
                    optionsMatch = checkListsEqual(it.itemOptions, itemAdd.itemOptions);
                }
                if (!optionsMatch) {
                    continue;
                }

                //========================ITEM TĂNG SỐ LƯỢNG========================
                if (itemAdd.template.id == 457) {
                    long total = (long) it.quantity + itemAdd.quantity;
                    if (total <= 500_000_000) {
                        it.quantity = (int) total;
                        itemAdd.quantity = 0;
                        if (it.itemOptions.isEmpty()) {
                            it.itemOptions.add(new Item.ItemOption(73, 0));
                        }
                        return true;
                    } else {
                        int add = 500_000_000 - it.quantity;
                        it.quantity = 500_000_000;
                        itemAdd.quantity -= add;
                        if (it.itemOptions.isEmpty()) {
                            it.itemOptions.add(new Item.ItemOption(73, 0));
                        }
                        continue;
                    }
                }
                if (itemAdd.isManhBongTai()) {
                    if (itemAdd.isManhVoBongTai()) {
                        it.syncManhVoBongTai();
                        itemAdd.syncManhVoBongTai();
                        int itOpt = it.getOptionParam(31);
                        int addOpt = itemAdd.getOptionParam(31);
                        long total = (long) itOpt + addOpt;
                        if (total <= 500_000_000L) {
                            it.setOptionParam(31, (int) total);
                            it.quantity = 1;
                            itemAdd.setOptionParam(31, 0);
                            itemAdd.quantity = 0;
                            return true;
                        } else {
                            int add = 500_000_000 - itOpt;
                            it.setOptionParam(31, 500_000_000);
                            it.quantity = 1;
                            int left = addOpt - add;
                            itemAdd.setOptionParam(31, left);
                            itemAdd.quantity = 1;
                            continue;
                        }
                    } else {
                        long total = (long) it.quantity + itemAdd.quantity;
                        if (total <= 500_000_000L) {
                            it.quantity = (int) total;
                            itemAdd.quantity = 0;
                            if (it.itemOptions.isEmpty()) {
                                it.itemOptions.add(new Item.ItemOption(73, 0));
                            }
                            return true;
                        } else {
                            int add = 500_000_000 - it.quantity;
                            it.quantity = 500_000_000;
                            itemAdd.quantity -= add;
                            if (it.itemOptions.isEmpty()) {
                                it.itemOptions.add(new Item.ItemOption(73, 0));
                            }
                            continue;
                        }
                    }
                }
                if ((itemAdd.template.id >= 1066 && itemAdd.template.id <= 1070)
                        || itemAdd.template.id == 610 || itemAdd.template.type == 14 || itemAdd.template.id == 821) {
                    it.quantity += itemAdd.quantity;
                    itemAdd.quantity = 0;
                    if (it.itemOptions.isEmpty()) {
                        it.itemOptions.add(new Item.ItemOption(73, 0));
                    }
                    return true;
                }

                if (it.quantity < 99999) {
                    int add = 99999 - it.quantity;
                    if (itemAdd.quantity <= add) {
                        it.quantity += itemAdd.quantity;
                        itemAdd.quantity = 0;
                        return true;
                    } else {
                        it.quantity = 99999;
                        itemAdd.quantity -= add;
                    }
                }
            }
        }

        //add item vào ô mới
        if (itemAdd.quantity > 0) {
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isNotNullItem()) {
                    Item newItem = ItemService.gI().copyItem(itemAdd);
                    if (newItem.isManhVoBongTai()) {
                        newItem.syncManhVoBongTai();
                    }
                    items.set(i, newItem);
                    itemAdd.quantity = 0;
                    return true;
                }
            }
        }
        return false;
        }
    }

    public boolean addItemGift(Player player, Item item) {
        return addItemGift(player.inventory.itemsBoxCrackBall, item);
    }

    public boolean addItemGift(List<Item> items, Item itemAdd) {
        //nếu item ko có option, add option rỗng vào
        if (itemAdd.itemOptions.isEmpty()) {
            itemAdd.itemOptions.add(new ItemOption(73, 0));
        }
        //add item vào ô mới
        items.add(ItemService.gI().copyItemGift(itemAdd));
        itemAdd.quantity = 0;
        return true;
    }

    public static boolean checkListsEqual(List<ItemOption> list1, List<ItemOption> list2) {
        List<ItemOption> c1 = getCleanOptions(list1);
        List<ItemOption> c2 = getCleanOptions(list2);

        if (c1.size() != c2.size()) {
            return false;
        }

        for (int i = 0; i < c1.size(); i++) {
            ItemOption o1 = c1.get(i);
            ItemOption o2 = c2.get(i);
            if (o1.optionTemplate.id != o2.optionTemplate.id || o1.param != o2.param) {
                return false;
            }
        }

        return true;
    }

    /**
     * Lọc danh sách option để so sánh:
     * - Bỏ qua option (73, 0) vì chỉ là placeholder rỗng (vật phẩm không có chỉ số).
     * - Sắp xếp theo optionTemplate.id rồi đến param để không bị ảnh hưởng bởi thứ tự chèn.
     */
    public static List<ItemOption> getCleanOptions(List<ItemOption> list) {
        List<ItemOption> clean = new ArrayList<>();
        if (list != null) {
            for (ItemOption io : list) {
                if (io != null && io.optionTemplate != null) {
                    if (io.optionTemplate.id == 73 && io.param == 0) {
                        continue;
                    }
                    clean.add(io);
                }
            }
        }
        clean.sort((a, b) -> {
            if (a.optionTemplate.id != b.optionTemplate.id) {
                return Integer.compare(a.optionTemplate.id, b.optionTemplate.id);
            }
            return Integer.compare(a.param, b.param);
        });
        return clean;
    }

    /**
     * So sánh danh sách option của Mảnh bông tai:
     * - Bỏ qua option (73, 0) vì là placeholder rỗng.
     * - Bỏ qua option 31 (vì là param số lượng của stack; các stack có số lượng khác nhau nên param 31 khác nhau).
     * - So sánh tất cả các options còn lại (như Option 30 [KGD], Option 93 [HSD]...):
     *   Nếu giống nhau -> gộp stack và cập nhật lại option 31 thành tổng số lượng.
     *   Nếu khác nhau (ví dụ 1 cái khóa, 1 cái không khóa) -> không gộp, giữ riêng ô.
     */
    public static boolean hasOption(List<ItemOption> list, int optionId) {
        if (list == null) {
            return false;
        }
        for (ItemOption io : list) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkOptionsEqualManhBongTai(List<ItemOption> list1, List<ItemOption> list2) {
        boolean has31_1 = hasOption(list1, 31);
        boolean has31_2 = hasOption(list2, 31);
        if (has31_1 != has31_2) {
            return false;
        }
        List<ItemOption> c1 = getCleanOptionsManhBongTai(list1);
        List<ItemOption> c2 = getCleanOptionsManhBongTai(list2);

        if (c1.size() != c2.size()) {
            return false;
        }

        for (int i = 0; i < c1.size(); i++) {
            ItemOption o1 = c1.get(i);
            ItemOption o2 = c2.get(i);
            if (o1.optionTemplate.id != o2.optionTemplate.id || o1.param != o2.param) {
                return false;
            }
        }

        return true;
    }

    public static List<ItemOption> getCleanOptionsManhBongTai(List<ItemOption> list) {
        List<ItemOption> clean = new ArrayList<>();
        if (list != null) {
            for (ItemOption io : list) {
                if (io != null && io.optionTemplate != null) {
                    if (io.optionTemplate.id == 73 && io.param == 0) {
                        continue;
                    }
                    if (io.optionTemplate.id == 31) {
                        continue;
                    }
                    if (io.optionTemplate.id == 86 && io.param == 0) {
                        continue;
                    }
                    clean.add(io);
                }
            }
        }
        clean.sort((a, b) -> {
            if (a.optionTemplate.id != b.optionTemplate.id) {
                return Integer.compare(a.optionTemplate.id, b.optionTemplate.id);
            }
            return Integer.compare(a.param, b.param);
        });
        return clean;
    }

    private void __________________Kiểm_tra_điều_kiện_vật_phẩm______________() {
        //**********************************************************************
    }

    /**
     * Kiểm tra vật phẩm có phải là vật phẩm tăng chỉ số option hay không
     *
     * @param item
     * @return id option tăng chỉ số - param
     */
    private int[] isItemIncrementalOption(Item item) {
        for (Item.ItemOption io : item.itemOptions) {
            switch (io.optionTemplate.id) {
                case 1:
                    return new int[]{io.optionTemplate.id, io.param};
                case 31:
                    return new int[]{io.optionTemplate.id, io.param};
            }
        }
        return new int[]{-1, -1};
    }

    private void __________________Kiểm_tra_danh_sách_còn_chỗ_trống_________() {
        //**********************************************************************
    }

    public byte getCountEmptyBag(Player player) {
        return getCountEmptyListItem(player.inventory.itemsBag);
    }

    public byte getCountEmptyListItem(List<Item> list) {
        byte count = 0;
        for (Item item : list) {
            if (!item.isNotNullItem()) {
                count++;
            }
        }
        return count;
    }

    public byte getIndexBag(Player pl, Item it) {
        return (byte) pl.inventory.itemsBag.indexOf(it);
    }

    public boolean finditemWoodChest(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 570) {
                return false;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 570) {
                return false;
            }
        }
        return true;
    }

    public int getParam(Player player, int idoption, int itemID) {
        long tong = 0;
        for (Item it : player.inventory.itemsBag) {
            if (it != null && it.itemOptions != null && it.isNotNullItem() && it.template.id == itemID) {
                if (idoption == 31 && it.isManhVoBongTai() && it.hasOption(31)) {
                    it.syncManhVoBongTai();
                }
                for (ItemOption iop : it.itemOptions) {
                    if (iop != null && iop.optionTemplate != null && iop.optionTemplate.id == idoption) {
                        tong += iop.param;
                        break;
                    }
                }
            }
        }
        return (int) Math.min(tong, Integer.MAX_VALUE);
    }

    public void subParamItemsBag(Player player, int itemID, int idoption, int param) {
        if (player == null || player.inventory == null || param <= 0) {
            return;
        }
        int con = param;
        List<Item> canXoa = new ArrayList<>();
        for (Item it : player.inventory.itemsBag) {
            if (con <= 0) {
                break;
            }
            if (it == null || !it.isNotNullItem() || it.template.id != itemID || it.itemOptions == null) {
                continue;
            }
            for (ItemOption op : it.itemOptions) {
                if (op == null || op.optionTemplate == null || op.optionTemplate.id != idoption) {
                    continue;
                }
                int tru = Math.min(con, op.param);
                op.param -= tru;
                con -= tru;
                if (op.param <= 0) {
                    canXoa.add(it);
                } else if (idoption == 31 && it.isManhVoBongTai() && it.hasOption(31)) {
                    it.quantity = 1;
                }
                break;
            }
        }
        for (Item it : canXoa) {
            removeItem(player.inventory.itemsBag, it);
        }
    }

    public boolean findItemBongTaiCap2(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 921) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 921) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemBongTaiCap3(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 1492) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 1492) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemNguoituyet(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 1210) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemNguoituyetbanggia(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 1211) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemSkinQuyLaoKame(Player player) {
        for (Item item : player.inventory.itemsBody) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemNTK(Player player) {
        if (player.isPl()) {
            for (Item item : player.inventory.itemsBag) {
                if (item.isNotNullItem() && item.template.id == 992) {
                    return true;
                }
            }
            for (Item item : player.inventory.itemsBox) {
                if (item.isNotNullItem() && item.template.id == 992) {
                    return true;
                }
            }
        }
        return false;
    }

//    public boolean findItemTVC(Player player) {
//        if (player.isPl()) {
//            for (Item item : player.inventory.itemsBag) {
//                if (item.isNotNullItem() && item.template.id == 2077) {
//                    return true;
//                }
//            }
//            for (Item item : player.inventory.itemsBox) {
//                if (item.isNotNullItem() && item.template.id == 2077) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
    public boolean findItemTatVoGiangSinh(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 649) {
                return true;
            }
        }
        return false;
    }

    public boolean fullSetThan(Player player) {
        for (int i = 0; i < 5; i++) {
            Item item = player.inventory.itemsBody.get(i);
            if (item == null || item.template == null || item.template.level != 13) {
                return false;
            }
        }
        return true;
    }

    public boolean x99ThucAn(Player player) {
        Item doAn = player.inventory.itemsBag.stream().filter(it -> it != null && it.template != null && (it.template.id == 663 || it.template.id == 664 || it.template.id == 665 || it.template.id == 666 || it.template.id == 667) && it.quantity >= 99).findFirst().orElse(null);
        return doAn != null;
    }

    public boolean canOpenBillShop(Player player) {
        return fullSetThan(player) && x99ThucAn(player);
    }

    public boolean optionCanUpgrade(int id) {
        return id == 0 || id == 6 || id == 7 || id == 22 || id == 23 || id == 14 || id == 27 || id == 28 || id == 47;
    }

    public int getIndexItem(Player player, List<Item> items, Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    public int getIndexItemBag(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBag, item);
    }

    public int getIndexItemBody(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBody, item);
    }

    public int getIndexItemBox(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBox, item);
    }

    /// item sự kiện
    public Item findVeTangNgochong(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && (item.template.id == 1190)) {
                return item;
            }
        }
        return null;
    }

    public Item findVeTangNgocxanh(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && (item.template.id == 1189)) {
                return item;
            }
        }
        return null;
    }

}
