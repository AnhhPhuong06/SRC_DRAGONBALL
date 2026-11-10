package models.npc.npc_list;

import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

public class Nuocmia extends Npc {

    public Nuocmia(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Cậu muốn uống gì nào mà thôi tự làm lấy mà hốc?", "Ép 1 ly\nnước mía", "Ép 10 ly\nnước mía", "Từ chối");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        Item khucmia = InventoryService.gI().findItem(player.inventory.itemsBag, 1354);
        Item nuocda = InventoryService.gI().findItem(player.inventory.itemsBag, 1355);
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        if ((khucmia != null && khucmia.quantity >= 10) && (nuocda != null && nuocda.quantity >= 3)) {
                            createOtherMenu(player, 401,
                                    "|2|Bạn muốn mua nước mía\n"
                                    + "|1|Khúc mía " + khucmia.quantity + "/10\n"
                                    + "|1|Nước đá " + nuocda.quantity + "/3\n"
                                    + "|4%", "Đồng ý", "Từ chối");
                        } else {
                            String npcSay = "|2|Để làm ra 1 ly nước mía\n";
                            npcSay += "|1|Khúc mía " + (khucmia == null ? "0" : khucmia.quantity) + "/5\n";
                            npcSay += "|1|Nước đá " + (nuocda == null ? "0" : nuocda.quantity) + "/2\n";
                            npcSay += "|7|%\n";
                            createOtherMenu(player, 444, npcSay, "Từ chối");
                        }
                    }
                    case 1 -> {
                        if ((khucmia != null && khucmia.quantity >= 100) && (nuocda != null && nuocda.quantity >= 30)) {
                            createOtherMenu(player, 402,
                                    "|2|Bạn muốn mua 10 ly nước mía\n"
                                    + "|1|Khúc mía " + khucmia.quantity + "/100\n"
                                    + "|1|Nước đá " + nuocda.quantity + "/30\n"
                                    + "|4%", "Đồng ý", "Từ chối");
                        } else {
                            String npcSay = "|2|Để làm ra 10 ly nước mía\n";
                            npcSay += "|1|Khúc mía " + (khucmia == null ? "0" : khucmia.quantity) + "/50\n";
                            npcSay += "|1|Nước đá " + (nuocda == null ? "0" : nuocda.quantity) + "/20\n";
                            npcSay += "|7|%\n";
                            createOtherMenu(player, 445, npcSay, "Từ chối");
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == 401) {
                switch (select) {
                    case 0 -> {
                        if (khucmia != null && nuocda != null && khucmia.quantity >= 10 && nuocda.quantity >= 3) {
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                InventoryService.gI().subQuantityItemsBag(player, khucmia, 10);
                                InventoryService.gI().subQuantityItemsBag(player, nuocda, 3);
                                Item nuocmia = ItemService.gI().createNewItem((short) Util.nextInt(1343, 1345), 1);
                                nuocmia.itemOptions.add(new Item.ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, nuocmia);
                                InventoryService.gI().sendItemBags(player);
                                player.inventory.gold -= 5000000;
                                Service.gI().sendMoney(player);
                                this.npcChat(player, "Bạn nhận được " + nuocmia.template.name);
                            } else {
                                Service.gI().sendThongBao(player, "Ngươi cần ít nhất 1 ô trống hành trang");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Không đủ nguyên liệu hoặc thiếu 5 tr vàng");
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == 402) {
                switch (select) {
                    case 0 -> {
                        if (khucmia != null && nuocda != null && khucmia.quantity >= 100 && nuocda.quantity >= 30) {
                            if (InventoryService.gI().getCountEmptyBag(player) >= 10) {
                                InventoryService.gI().subQuantityItemsBag(player, khucmia, 100);
                                InventoryService.gI().subQuantityItemsBag(player, nuocda, 30);
                                for (int i = 0; i < 10; i++) {
                                    Item nuocmia = ItemService.gI().createNewItem((short) Util.nextInt(1343, 1345), 1);
                                    nuocmia.itemOptions.add(new Item.ItemOption(30, 0));
                                    InventoryService.gI().addItemBag(player, nuocmia);
                                }
                                InventoryService.gI().sendItemBags(player);
                                player.inventory.gold -= 50000000;
                                Service.gI().sendMoney(player);
                                this.npcChat(player, "Bạn nhận được 10 ly nước mía!");
                            } else {
                                Service.gI().sendThongBao(player, "Ngươi cần ít nhất 10 ô trống hành trang");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Không đủ nguyên liệu hoặc thiếu 50 tr vàng");
                        }
                    }
                }
            }
        }
    }
}
