package models.npc.npc_list;

/**
 * @author NGOJC
 */
import consts.ConstItem;
import consts.ConstNpc;

import models.item.Item;
import models.npc.Npc;
import models.player.Player;

import services.ItemService;
import services.Service;
import services.player.InventoryService;

public class NoiBanh extends Npc {

    public NoiBanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào " + player.name + "\n"
                    + "Tôi là nồi nấu bánh\n"
                    + "Tôi có thể giúp gì cho bạn ?",
                    "Tự nấu\n bánh", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 5 || this.mapId == 0 || this.mapId == 7 || this.mapId == 14) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            createOtherMenu(player, 1,
                                    "Hãy tìm đủ nguyên liệu và chọn loại bánh muốn nấu",
                                    "Nấu\n Bánh dầy", "Nấu\n Bánh chưng", "Từ chối");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == 1) {
                    switch (select) {
                        case 0:
                            Item comNep = InventoryService.gI().findItemBag(player, 1434);
                            Item botGao = InventoryService.gI().findItemBag(player, 1440);
                            Item muoiTieu = InventoryService.gI().findItemBag(player, 1438);
                            Item chaLua = InventoryService.gI().findItemBag(player, 1437);
                            if ((comNep != null && comNep.quantity >= 99)
                                    && (botGao != null && botGao.quantity >= 5)
                                    && (muoiTieu != null && muoiTieu.quantity >= 2)
                                    && (chaLua != null && chaLua.quantity >= 1)
                                    && player.inventory.gold >= 1000000) {
                                createOtherMenu(player, ConstNpc.MENU_BANH_TET,
                                        "|2|Bạn muốn nấu bánh tét?\n"
                                        + "|1|Cơm nếp " + comNep.quantity + "/99\n"
                                        + "Bột gạo " + botGao.quantity + "/5\n"
                                        + "Muối tiêu " + muoiTieu.quantity + "/2\n"
                                        + "Chả lụa " + chaLua.quantity + "/1\n"
                                        + "Giá vàng: 1.000.000",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Bạn muốn nấu bánh dầy\n";
                                if (comNep == null) {
                                    NpcSay += "|7|Cơm nếp " + "0/99\n";
                                } else {
                                    NpcSay += "|1|Cơm nếp " + comNep.quantity + "/99\n";
                                }
                                if (botGao == null) {
                                    NpcSay += "|7|Bột gạo " + "0/5\n";
                                } else {
                                    NpcSay += "|1|Bột gạo " + botGao.quantity + "/5\n";
                                }
                                if (muoiTieu == null) {
                                    NpcSay += "|7|Muối tiêu " + "0/2\n";
                                } else {
                                    NpcSay += "|1|Muối tiêu" + muoiTieu.quantity + "/2\n";
                                }
                                if (chaLua == null) {
                                    NpcSay += "|7|Chả lụa " + "0/1\n";
                                } else {
                                    NpcSay += "|1|Chả lụa " + chaLua.quantity + "/1\n";
                                }
                                if (player.inventory.gold < 1000000) {
                                    NpcSay += "|7|Còn thiếu vàng";
                                } else {
                                    NpcSay += "|1|Giá vàng: 1.000.000\n";
                                }
                                createOtherMenu(player, ConstNpc.MENU_BANH_TET_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                        case 1:
                            Item comNepNe = InventoryService.gI().findItemBag(player, 1434);
                            Item dauXanh = InventoryService.gI().findItemBag(player, 1441);
                            Item thitTuoi = InventoryService.gI().findItemBag(player, 1442);
                            if ((comNepNe != null && comNepNe.quantity >= 99)
                                    && (dauXanh != null && dauXanh.quantity >= 2)
                                    && (thitTuoi != null && thitTuoi.quantity >= 2)
                                    && player.inventory.gold >= 5000000) {
                                createOtherMenu(player, ConstNpc.MENU_BANH_CHUNG,
                                        "|2|Bạn muốn nấu bánh tét?\n"
                                        + "|1|Cơm nếp " + comNepNe.quantity + "/99\n"
                                        + "Đậu xanh " + dauXanh.quantity + "/2\n"
                                        + "Thịt tươi " + thitTuoi.quantity + "/2\n"
                                        + "Giá vàng: 5.000.000",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Bạn muốn nấu bánh chưng\n";
                                if (comNepNe == null) {
                                    NpcSay += "|7|Cơm nếp " + "0/99\n";
                                } else {
                                    NpcSay += "|1|Cơm nếp " + comNepNe.quantity + "/99\n";
                                }
                                if (dauXanh == null) {
                                    NpcSay += "|7|Đậu xanh " + "0/2\n";
                                } else {
                                    NpcSay += "|1|Đậu xanh " + dauXanh.quantity + "/2\n";
                                }
                                if (thitTuoi == null) {
                                    NpcSay += "|7|Thịt tươi " + "0/2\n";
                                } else {
                                    NpcSay += "|1|Thịt tươi" + thitTuoi.quantity + "/2\n";
                                }
                                if (player.inventory.gold < 5000000) {
                                    NpcSay += "|7|Còn thiếu vàng";
                                } else {
                                    NpcSay += "|1|Giá vàng: 5.000.000\n";
                                }
                                createOtherMenu(player, ConstNpc.MENU_BANH_CHUNG_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_BANH_TET) {
                    switch (select) {
                        case 0: {
                            if (player.isCookingBanhDay) {
                                this.npcChat(player, "Bạn đang nấu bánh dầy rồi mà!");
                                return;
                            } else if (player.isCookingBanhChung) {
                                this.npcChat(player, "Bạn đang nấu bánh chưng rồi mà!");
                                return;
                            }
                            Item comNep = InventoryService.gI().findItemBag(player, 1434);
                            Item botGao = InventoryService.gI().findItemBag(player, 1440);
                            Item muoiTieu = InventoryService.gI().findItemBag(player, 1438);
                            Item chaLua = InventoryService.gI().findItemBag(player, 1437);
                            Item banhDay = ItemService.gI().createNewItem((short) 1435);
                            int vang = 1000000;
                            player.isCookingBanhDay = true;
                            this.npcChat(player, "Bắt đầu nấu bánh dầy...\n|7|Vui lòng chờ trong giây lát!");
                            new Thread(() -> {
                                int timeWait = 60;
                                while (timeWait > 0) {
                                    try {
                                        this.npcChat(player,
                                                "Đang nấu bánh dầy\n|7|Thời gian còn lại: " + timeWait + " giây.");
                                        Thread.sleep(1000);
                                        timeWait--;
                                    } catch (InterruptedException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                InventoryService.gI().subQuantityItemsBag(player, comNep, 99);
                                InventoryService.gI().subQuantityItemsBag(player, botGao, 5);
                                InventoryService.gI().subQuantityItemsBag(player, muoiTieu, 2);
                                InventoryService.gI().subQuantityItemsBag(player, chaLua, 1);
                                player.inventory.gold -= vang;
                                Service.gI().sendMoney(player);
                                InventoryService.gI().addItemBag(player, banhDay);
                                InventoryService.gI().sendItemBags(player);
                                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                        "Đã nấu xong bánh dầy!\n|7|Bạn đã nhận được " + banhDay.template.name,
                                        "Nhận Ngay");
                            }).start();
                            break;
                        }

                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_BANH_CHUNG) {
                    switch (select) {
                        case 0:
                            if (player.isCookingBanhChung) {
                                this.npcChat(player, "Bạn đang nấu bánh chưng rồi mà!");
                                return;
                            } else if (player.isCookingBanhDay) {
                                this.npcChat(player, "Bạn đang nấu bánh dầy rồi mà!");
                                return;
                            }
                            Item comNepNe = InventoryService.gI().findItemBag(player, 1434);
                            Item dauXanh = InventoryService.gI().findItemBag(player, 1441);
                            Item thitTuoi = InventoryService.gI().findItemBag(player, 1442);
                            Item banhChung = ItemService.gI().createNewItem((short) 1436);
                            int vang = 5000000;
                            player.isCookingBanhChung = true;
                            this.npcChat(player, "Bắt đầu nấu bánh chưng...\n|7|Vui lòng chờ trong giây lát!");
                            new Thread(() -> {
                                int timeWait = 60;
                                while (timeWait > 0) {
                                    try {
                                        this.npcChat(player,
                                                "Đang nấu bánh chưng\n|7|Thời gian còn lại: " + timeWait + " giây.");
                                        Thread.sleep(1000);
                                        timeWait--;
                                    } catch (InterruptedException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                InventoryService.gI().subQuantityItemsBag(player, comNepNe, 99);
                                InventoryService.gI().subQuantityItemsBag(player, dauXanh, 2);
                                InventoryService.gI().subQuantityItemsBag(player, thitTuoi, 2);
                                player.inventory.gold -= vang;
                                Service.gI().sendMoney(player);
                                InventoryService.gI().addItemBag(player, banhChung);
                                InventoryService.gI().sendItemBags(player);
                                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                        "Đã nấu xong bánh dầy!\n|7|Bạn đã nhận được " + banhChung.template.name,
                                        "Nhận Ngay");
                            }).start();
                            break;
                    }
                }
            }
        }
    }
}
