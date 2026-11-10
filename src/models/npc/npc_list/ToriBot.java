/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.npc.npc_list;

import consts.ConstNpc;
import database.daos.PlayerDAO;
import java.util.ArrayList;
import java.util.HashMap;
import models.item.Item;
import models.item.Item.ItemOption;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.ShopService;
import services.func.UseItem;
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class ToriBot extends Npc {

    public static final String ITEM_SHOP_TAG = "TORI_BOT_ITEMS";

    public ToriBot(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 5) {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Vật phẩm của ta được bán bằng Đá ngũ sắc. Giá niêm yết là số viên đá cần dùng.",
                        "Shop\nvật phẩm", "Đóng");
                return;
            }
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ngươi muốn gì ở ta?",
                    "Mua VIP", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 5) {
                if (player.idMark.isBaseMenu() && select == 0) {
                    ShopService.gI().opendShop(player, ITEM_SHOP_TAG, false);
                }
                return;
            }
            if (this.mapId == 0 || this.mapId == 7 || this.mapId == 14) {
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU -> {
                        switch (select) {
                            case 0 ->
                                createOtherMenu(player, ConstNpc.BUY_VIP,
                                        "|7|Trong thời gian mùa 1 diễn ra\nNếu mua VIP sẽ được nhận\nnhiều ưu đãi hơn nữa.\nLưu ý: nâng cấp mỗi VIP chỉ được nâng 1 lần/1 VIP",
                                        "VIP 1", "VIP 2", "VIP 3", "Đóng");
                        }
                    }
                    case ConstNpc.BUY_VIP -> {
                        switch (select) {
                            case 0 ->
                                createOtherMenu(player, ConstNpc.BUY_VIP1,
                                        "|5|Nâng cấp VIP 1 bạn sẽ được"
                                        + "\nDanh hiệu VIP 03 5% chỉ số hp ki sd vv"
                                        + "\nx20 sao pha lê tnsm"
                                        + "\nCải trang Karin Kid Lân 15 ngày"
                                        + "\nx3 hộp mù bé ba"
                                        + "\nVán bay mây mưa 15 ngày.",
                                        "50K", "Đóng");
                            case 1 ->
                                createOtherMenu(player, ConstNpc.BUY_VIP2,
                                        "|5|Nâng cấp VIP 2 bạn sẽ được"
                                        + "\nDanh hiệu VIP 02 8% chỉ số hp ki sd vv"      
                                        + "\nx10 đá bảo vệ"
                                        + "\nbán bay mây mưa vv"
                                        + "\nCT Karin múa Lân vv"
                                        + "\nlính bảo vệ tròn vv"
                                        + "\nx10 mảnh đội trưởng vàng"
                                        + "\nx5 hộp mù bé ba",
                                        "200K", "Đóng");
                            case 2 ->
                                createOtherMenu(player, ConstNpc.BUY_VIP3,
                                        "|5|Nâng cấp VIP 3 bạn sẽ được"
                                        + "\nDanh hiệu VIP 01 10% chỉ số hp ki sd vv"
                                        + "\nx10 ngọc rồng 3 sao"        
                                        + "\nx30 đá bảo vệ"
                                        + "\nbán bay máy bay 41 vv"
                                        + "\nCT Bunma Rider vv"
                                        + "\nCánh thiên thần ác quỷ vv"
                                        + "\npet cá mập vv"
                                        + "\nx20 mảnh đội trưởng vàng"
                                        + "\nx10 hộp mù bé ba",
                                        "500K", "Đóng");
                        }
                    }
                    case ConstNpc.BUY_VIP1 -> {
                        switch (select) {
                            case 0, 1 -> {
                                UseItem.gI().ComfirmNhanVIP(player, select == 0);
                            }
                        }
                    }
                    case ConstNpc.BUY_VIP2 -> {
                        switch (select) {
                            case 0, 1 -> {
                                UseItem.gI().ComfirmNhanVIP2(player, select == 0);
                            }
                        }
                    }
                    case ConstNpc.BUY_VIP3 -> {
                        switch (select) {
                            case 0, 1 -> {
                                UseItem.gI().ComfirmNhanVIP3(player, select == 0);
                            }
                        }
                    }
                    case ConstNpc.RECEIVE_MOC_NAP -> {
                        switch (select) {
                            case 0 ->
                                createOtherMenu(player, ConstNpc.SEEN_MOC_NAP, "Xin chào"
                                        + "\n|2|Hiện tại con đã nạp được: " + Util.format(player.getSession().danap) + " VNĐ"
                                        + "\n|-1|Con hãy chọn mốc nạp muốn nhận?",
                                        "Xem Mốc\n100K VNĐ",
                                        "Xem Mốc\n300K VNĐ",
                                        "Xem Mốc\n500K VNĐ",
                                        "Xem Mốc\n1Tr VNĐ",
                                        "Xem Mốc\n2Tr VNĐ",
                                        "Xem Mốc\n3Tr VNĐ");
                        }
                    }
                    case ConstNpc.SEEN_MOC_NAP -> {
                        StringBuilder text = new StringBuilder();
                        for (Item item : ITEM_REWARDS.get(SWITCH_MOC_NAP[select])) {
                            text.append("x").append(item.quantity).append(" ").append(item.template.name).append("\n");
                        }
                        createOtherMenu(player, ConstNpc.CONFIRM_RECEIVE_MOC_NAP, "|7|Phần Thưởng Mốc Nạp " + Util.format(SWITCH_MOC_NAP[select]) + " VNĐ Gồm:"
                                + "\n|2|" + text
                                + "|7|Bạn có mún nhận khum?",
                                "Nhận Ngay", "Đóng");
                    }
                    case ConstNpc.CONFIRM_RECEIVE_MOC_NAP -> {
                        switch (select) {
                            case 0 ->
                                ConfirmMocNap(player);
                        }
                    }
                }
            }
        }
    }

    private static final int SWITCH_MOC_NAP[] = {100_000, 300_000, 500_000, 1_000_000, 2_000_000, 3_000_000};
    private static final HashMap<Integer, ArrayList<Item>> ITEM_REWARDS = new HashMap<>();

    public static final void addItemToListRewardMocNap() {
        ArrayList<Item> listItem = new ArrayList<>();
        //MỐC 1 [NRO 3S, SPL TNSM, DNS]
        listItem.add(ItemService.gI().createNewItem((short) 16, 10));
        Item itemReceive = ItemService.gI().createNewItem((short) 447,99);
        itemReceive.itemOptions.add(new ItemOption(101, 5));
        listItem.add(itemReceive);
        listItem.add(ItemService.gI().createNewItem((short) 674, 100));
        ITEM_REWARDS.put(SWITCH_MOC_NAP[0], new ArrayList<>(listItem));
        listItem.clear();
        //MỐC 2 [DBV, SKILL2 ĐỆ, DNS, MẢNH KHỈ VÀNG, TRỨNG MABƯ]
        listItem.add(ItemService.gI().createNewItem((short) 987, 50));
        listItem.add(ItemService.gI().createNewItem((short) 1381, 10));
        listItem.add(ItemService.gI().createNewItem((short) 674, 300));
        listItem.add(ItemService.gI().createNewItem((short) 1277, 40));
        listItem.add(ItemService.gI().createNewItem((short) 568, 1));
        ITEM_REWARDS.put(SWITCH_MOC_NAP[1], new ArrayList<>(listItem));
        listItem.clear();
        //MỐC 3 [NRO 3S, SKILL3 ĐỆ, DNS, MẢNH KHỈ VÀNG, TRỨNG MABƯ]
        listItem.add(ItemService.gI().createNewItem((short) 16, 20));
        listItem.add(ItemService.gI().createNewItem((short) 1382, 10));
        listItem.add(ItemService.gI().createNewItem((short) 674, 500));
        listItem.add(ItemService.gI().createNewItem((short) 1277, 100));
        listItem.add(ItemService.gI().createNewItem((short) 568, 1));
        ITEM_REWARDS.put(SWITCH_MOC_NAP[2], new ArrayList<>(listItem));
        listItem.clear();
        //MỐC 4 [DBV, HỘP CT HIT, SKILL4 ĐỆ, DNS, MẢNH KHỈ VÀNG, TRỨNG BERUS]
        listItem.add(ItemService.gI().createNewItem((short) 987, 199));
        listItem.add(ItemService.gI().createNewItem((short) 1451, 10));
        listItem.add(ItemService.gI().createNewItem((short) 1383, 10));
        listItem.add(ItemService.gI().createNewItem((short) 674, 1000));
        listItem.add(ItemService.gI().createNewItem((short) 1277, 100));
        listItem.add(ItemService.gI().createNewItem((short) 1475, 1));
        ITEM_REWARDS.put(SWITCH_MOC_NAP[3], new ArrayList<>(listItem));
        listItem.clear();
        //MỐC 5 [NRO 3S, HỘP CT HIT, HỘP BABY THREE, HỘP CT CADIC VIP, HỘP THẦN LINH, RADA PICOLO DAIMA, TRỨNG BERUS]
        listItem.add(ItemService.gI().createNewItem((short) 16, 50));
        listItem.add(ItemService.gI().createNewItem((short) 1451, 30));
        listItem.add(ItemService.gI().createNewItem((short) 1305, 30));
        listItem.add(ItemService.gI().createNewItem((short) 1199, 10));
        listItem.add(ItemService.gI().createNewItem((short) 1324, 1));
        listItem.add(ItemService.gI().createNewItem((short) 1476, 99));
        listItem.add(ItemService.gI().createNewItem((short) 1475, 1));
        ITEM_REWARDS.put(SWITCH_MOC_NAP[4], new ArrayList<>(listItem));
        listItem.clear();
        //MỐC 6 [CT BROLY SSJ4, NRO 3S, HỘP CT HIT, HỘP BABY THREE, HỘP THẦN LINH, DNS]
        itemReceive = ItemService.gI().createNewItem((short) 1241);
        itemReceive.itemOptions.add(new ItemOption(50, 50));
        itemReceive.itemOptions.add(new ItemOption(77, 50));
        itemReceive.itemOptions.add(new ItemOption(103, 50));
        itemReceive.itemOptions.add(new ItemOption(5, 30));
        itemReceive.itemOptions.add(new ItemOption(14, 30));
        itemReceive.itemOptions.add(new ItemOption(106, 0));
        listItem.add(itemReceive);
        listItem.add(ItemService.gI().createNewItem((short) 16, 99));
        listItem.add(ItemService.gI().createNewItem((short) 1451, 50));
        listItem.add(ItemService.gI().createNewItem((short) 1305, 50));
        listItem.add(ItemService.gI().createNewItem((short) 1324, 3));
        listItem.add(ItemService.gI().createNewItem((short) 674, 3000));
        ITEM_REWARDS.put(SWITCH_MOC_NAP[5], new ArrayList<>(listItem));
        listItem.clear();
    }

    public void ConfirmMocNap(Player player) {
        if (player.getSession().danap >= SWITCH_MOC_NAP[0] && player.getSession().mocNap == 0) {
            if (InventoryService.gI().getCountEmptyBag(player) < ITEM_REWARDS.get(SWITCH_MOC_NAP[0]).size() - 1) {
                Service.gI().sendThongBao(player, "Bạn cần trống " + (ITEM_REWARDS.get(SWITCH_MOC_NAP[0]).size() - 1) + " ô hành trang!");
                return;
            }
            if (PlayerDAO.updateMocNap(player.getSession().uu, SWITCH_MOC_NAP[0])) {
                player.getSession().mocNap = SWITCH_MOC_NAP[0];
                for (Item item : ITEM_REWARDS.get(SWITCH_MOC_NAP[0])) {
                    Service.gI().sendThongBao(player, "Bạn vừa nhận được " + item.quantity + " " + item.template.name);
                    InventoryService.gI().addItemBag(player, ItemService.gI().copyItem(item));
                }
                InventoryService.gI().sendItemBags(player);
            }
        } else if (player.getSession().danap >= SWITCH_MOC_NAP[1] && player.getSession().mocNap == SWITCH_MOC_NAP[0]) {
            if (InventoryService.gI().getCountEmptyBag(player) < ITEM_REWARDS.get(SWITCH_MOC_NAP[1]).size() - 1) {
                Service.gI().sendThongBao(player, "Bạn cần trống " + (ITEM_REWARDS.get(SWITCH_MOC_NAP[1]).size() - 1) + " ô hành trang!");
                return;
            }
            if (PlayerDAO.updateMocNap(player.getSession().uu, SWITCH_MOC_NAP[1])) {
                player.getSession().mocNap = SWITCH_MOC_NAP[1];
                for (Item item : ITEM_REWARDS.get(SWITCH_MOC_NAP[1])) {
                    Service.gI().sendThongBao(player, "Bạn vừa nhận được " + item.quantity + " " + item.template.name);
                    InventoryService.gI().addItemBag(player, ItemService.gI().copyItem(item));
                }
                InventoryService.gI().sendItemBags(player);
            }
        } else if (player.getSession().danap >= SWITCH_MOC_NAP[2] && player.getSession().mocNap == SWITCH_MOC_NAP[1]) {
            if (InventoryService.gI().getCountEmptyBag(player) < ITEM_REWARDS.get(SWITCH_MOC_NAP[2]).size() - 1) {
                Service.gI().sendThongBao(player, "Bạn cần trống " + (ITEM_REWARDS.get(SWITCH_MOC_NAP[2]).size() - 1) + " ô hành trang!");
                return;
            }
            if (PlayerDAO.updateMocNap(player.getSession().uu, SWITCH_MOC_NAP[2])) {
                player.getSession().mocNap = SWITCH_MOC_NAP[2];
                for (Item item : ITEM_REWARDS.get(SWITCH_MOC_NAP[2])) {
                    Service.gI().sendThongBao(player, "Bạn vừa nhận được " + item.quantity + " " + item.template.name);
                    InventoryService.gI().addItemBag(player, ItemService.gI().copyItem(item));
                }
                InventoryService.gI().sendItemBags(player);
            }
        } else if (player.getSession().danap >= SWITCH_MOC_NAP[3] && player.getSession().mocNap == SWITCH_MOC_NAP[2]) {
            if (InventoryService.gI().getCountEmptyBag(player) < ITEM_REWARDS.get(SWITCH_MOC_NAP[3]).size() - 1) {
                Service.gI().sendThongBao(player, "Bạn cần trống " + (ITEM_REWARDS.get(SWITCH_MOC_NAP[3]).size() - 1) + " ô hành trang!");
                return;
            }
            if (PlayerDAO.updateMocNap(player.getSession().uu, SWITCH_MOC_NAP[3])) {
                player.getSession().mocNap = SWITCH_MOC_NAP[3];
                for (Item item : ITEM_REWARDS.get(SWITCH_MOC_NAP[3])) {
                    Service.gI().sendThongBao(player, "Bạn vừa nhận được " + item.quantity + " " + item.template.name);
                    InventoryService.gI().addItemBag(player, ItemService.gI().copyItem(item));
                }
                InventoryService.gI().sendItemBags(player);
            }
        } else if (player.getSession().danap >= SWITCH_MOC_NAP[4] && player.getSession().mocNap == SWITCH_MOC_NAP[3]) {
            if (InventoryService.gI().getCountEmptyBag(player) < ITEM_REWARDS.get(SWITCH_MOC_NAP[4]).size() - 1) {
                Service.gI().sendThongBao(player, "Bạn cần trống " + (ITEM_REWARDS.get(SWITCH_MOC_NAP[4]).size() - 1) + " ô hành trang!");
                return;
            }
            if (PlayerDAO.updateMocNap(player.getSession().uu, SWITCH_MOC_NAP[4])) {
                player.getSession().mocNap = SWITCH_MOC_NAP[4];
                for (Item item : ITEM_REWARDS.get(SWITCH_MOC_NAP[4])) {
                    Service.gI().sendThongBao(player, "Bạn vừa nhận được " + item.quantity + " " + item.template.name);
                    InventoryService.gI().addItemBag(player, ItemService.gI().copyItem(item));
                }
                InventoryService.gI().sendItemBags(player);
            }
        } else if (player.getSession().danap >= SWITCH_MOC_NAP[5] && player.getSession().mocNap == SWITCH_MOC_NAP[4]) {
            if (InventoryService.gI().getCountEmptyBag(player) < ITEM_REWARDS.get(SWITCH_MOC_NAP[5]).size() - 1) {
                Service.gI().sendThongBao(player, "Bạn cần trống " + (ITEM_REWARDS.get(SWITCH_MOC_NAP[5]).size() - 1) + " ô hành trang!");
                return;
            }
            if (PlayerDAO.updateMocNap(player.getSession().uu, SWITCH_MOC_NAP[5])) {
                player.getSession().mocNap = SWITCH_MOC_NAP[5];
                for (Item item : ITEM_REWARDS.get(SWITCH_MOC_NAP[5])) {
                    Service.gI().sendThongBao(player, "Bạn vừa nhận được " + item.quantity + " " + item.template.name);
                    InventoryService.gI().addItemBag(player, ItemService.gI().copyItem(item));
                }
                InventoryService.gI().sendItemBags(player);
            }
        } else {
            Service.gI().sendThongBao(player, "Bạn Chưa Đủ Điều Kiện Nhận Mốc Nạp Này!");
        }
    }
}
