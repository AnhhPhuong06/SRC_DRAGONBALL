/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.npc.npc_list;

import consts.ConstNpc;
import models.item.Item;
import models.item.Item.ItemOption;
import models.npc.Npc;
import models.player.Player;
import network.io.Message;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class HungVuong extends Npc {

    public HungVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ngươi tìm ta có việc gì ?",
                    "Dâng\nsính lễ", "Dâng\nsính lễ\nxịn", "Dâng\nBánh dầy", "Dâng\nBánh chưng\nLang Liêu");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 0) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            Item ngaVoi = InventoryService.gI().findItemBag(player, 1443);
                            Item cuaGa = InventoryService.gI().findItemBag(player, 1444);
                            Item hongMao = InventoryService.gI().findItemBag(player, 1445);
                            if ((ngaVoi != null && ngaVoi.quantity >= 9)
                                    && (cuaGa != null && cuaGa.quantity >= 9)
                                    && (hongMao != null && hongMao.quantity >= 9)
                                    && player.inventory.gold >= 1000000) {
                                createOtherMenu(player, ConstNpc.MENU_SINHLE,
                                        "|2|Con muốn dâng sính lễ ?\n"
                                                + "|1|Ngà voi " + ngaVoi.quantity + "/9\n"
                                                + "Cựa gà " + cuaGa.quantity + "/9\n"
                                                + "Hồng mao " + hongMao.quantity + "/9\n"
                                                + "Giá vàng: 1.000.000",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Con muốn dâng sính lễ\n";
                                if (ngaVoi == null) {
                                    NpcSay += "|7|Ngà voi " + "0/9\n";
                                } else {
                                    NpcSay += "|1|Ngà voi " + ngaVoi.quantity + "/9\n";
                                }
                                if (cuaGa == null) {
                                    NpcSay += "|7|Cựa gà " + "0/9\n";
                                } else {
                                    NpcSay += "|1|Cựa gà " + cuaGa.quantity + "/9\n";
                                }
                                if (hongMao == null) {
                                    NpcSay += "|7|Hồng mao " + "0/9\n";
                                } else {
                                    NpcSay += "|1|Hồng mao" + hongMao.quantity + "/9\n";
                                }
                                if (player.inventory.gold < 1000000) {
                                    NpcSay += "|7|Còn thiếu vàng";
                                } else {
                                    NpcSay += "|1|Giá vàng: 1.000.000\n";
                                }
                                createOtherMenu(player, ConstNpc.MENU_SINHLE_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                        case 1:
                            Item ngaVoii = InventoryService.gI().findItemBag(player, 1443);
                            Item cuaGaa = InventoryService.gI().findItemBag(player, 1444);
                            Item hongMaoo = InventoryService.gI().findItemBag(player, 1445);
                            if ((ngaVoii != null && ngaVoii.quantity >= 9)
                                    && (cuaGaa != null && cuaGaa.quantity >= 9)
                                    && (hongMaoo != null && hongMaoo.quantity >= 9)
                                    && player.inventory.gem >= 10) {
                                createOtherMenu(player, ConstNpc.MENU_SINHLE_XIN,
                                        "|2|Con muốn dâng sính lễ ?\n"
                                                + "|1|Ngà voi " + ngaVoii.quantity + "/9\n"
                                                + "Cựa gà " + cuaGaa.quantity + "/9\n"
                                                + "Hồng mao " + hongMaoo.quantity + "/9\n"
                                                + "Giá ngọc: 10",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Con muốn dâng sính lễ\n";
                                if (ngaVoii == null) {
                                    NpcSay += "|7|Ngà voi " + "0/9\n";
                                } else {
                                    NpcSay += "|1|Ngà voi " + ngaVoii.quantity + "/9\n";
                                }
                                if (cuaGaa == null) {
                                    NpcSay += "|7|Cựa gà " + "0/9\n";
                                } else {
                                    NpcSay += "|1|Cựa gà " + cuaGaa.quantity + "/9\n";
                                }
                                if (hongMaoo == null) {
                                    NpcSay += "|7|Hồng mao " + "0/9\n";
                                } else {
                                    NpcSay += "|1|Hồng mao" + hongMaoo.quantity + "/9\n";
                                }
                                if (player.inventory.gem < 10) {
                                    NpcSay += "|7|Còn thiếu ngọc";
                                } else {
                                    NpcSay += "|1|Giá ngọc: 10\n";
                                }
                                createOtherMenu(player, ConstNpc.MENU_SINHLE_XIN_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                        case 2:
                            Item banhday = InventoryService.gI().findItemBag(player, 1435);
                            if ((banhday != null && banhday.quantity >= 1)) {
                                createOtherMenu(player, ConstNpc.MENU_SINHLE_BANH_DAY,
                                        "|2|Con muốn dâng sính lễ ?\n"
                                                + "|1|Bánh dầy " + banhday.quantity + "/1\n",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Con muốn dâng sính lễ\n";
                                if (banhday == null) {
                                    NpcSay += "|7|Bánh dầy " + "0/1\n";
                                } else {
                                    NpcSay += "|1|Bánh dầy " + banhday.quantity + "/1\n";
                                }
                                createOtherMenu(player, ConstNpc.MENU_SINHLE_BANH_DAY_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                        case 3:
                            Item banhChung = InventoryService.gI().findItemBag(player, 1436);
                            if ((banhChung != null && banhChung.quantity >= 1)) {
                                createOtherMenu(player, ConstNpc.MENU_SINHLE_BANH_CHUNG,
                                        "|2|Con muốn dâng sính lễ ?\n"
                                                + "|1|Bánh chưng Lang Liêu " + banhChung.quantity + "/1\n",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Con muốn dâng sính lễ\n";
                                if (banhChung == null) {
                                    NpcSay += "|7|Bánh chưng Lang Liêu " + "0/1\n";
                                } else {
                                    NpcSay += "|1|Bánh chưng Lang Leieu " + banhChung.quantity + "/1\n";
                                }
                                createOtherMenu(player, ConstNpc.MENU_SINHLE_BANH_CHUNG_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_SINHLE) {
                    switch (select) {
                        case 0:
                            Item ngaVoi = InventoryService.gI().findItemBag(player, 1443);
                            Item cuaGa = InventoryService.gI().findItemBag(player, 1444);
                            Item hongMao = InventoryService.gI().findItemBag(player, 1445);
                            Item hopQuaThuong = ItemService.gI().createNewItem((short) 1428);
                            int vang = 1000000;
                            try { // send effect susscess
                                Message msg = new Message(-81);
                                msg.writer().writeByte(0);
                                msg.writer().writeUTF("test");
                                msg.writer().writeUTF("test");
                                msg.writer().writeShort(tempId);
                                player.sendMessage(msg);
                                msg.cleanup();
                                msg = new Message(-81);
                                msg.writer().writeByte(1);
                                msg.writer().writeByte(2);
                                msg.writer().writeByte(InventoryService.gI().getIndexBag(player, ngaVoi));
                                msg.writer().writeByte(InventoryService.gI().getIndexBag(player, cuaGa));
                                msg.writer().writeByte(InventoryService.gI().getIndexBag(player, hongMao));
                                player.sendMessage(msg);
                                msg.cleanup();
                                msg = new Message(-81);
                                msg.writer().writeByte(7);
                                msg.writer().writeShort(hopQuaThuong.template.iconID);
                                msg.writer().writeShort(-1);
                                msg.writer().writeShort(-1);
                                msg.writer().writeShort(-1);
                                player.sendMessage(msg);
                                msg.cleanup();
                            } catch (Exception e) {
                                System.out.println("lỗi 4");
                            }
                            InventoryService.gI().addItemList(player.inventory.itemsBag, hopQuaThuong);
                            InventoryService.gI().subQuantityItemsBag(player, ngaVoi, 9);
                            InventoryService.gI().subQuantityItemsBag(player, cuaGa, 9);
                            InventoryService.gI().subQuantityItemsBag(player, hongMao, 9);
                            player.inventory.gold -= vang;
                            Service.gI().sendThongBao(player, "Dâng sính lễ thành công bạn nhận được " + hopQuaThuong.template.name);
                            InventoryService.gI().sendItemBags(player);
                            return;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_SINHLE_XIN) {
                    switch (select) {
                        case 0:
                            Item ngaVoii = InventoryService.gI().findItemBag(player, 1443);
                            Item cuaGaa = InventoryService.gI().findItemBag(player, 1444);
                            Item hongMaoo = InventoryService.gI().findItemBag(player, 1445);
                            Item hopQuaVip = ItemService.gI().createNewItem((short) 1429);
                            int gem = 10;
                            try { // send effect susscess
                                Message msg = new Message(-81);
                                msg.writer().writeByte(0);
                                msg.writer().writeUTF("test");
                                msg.writer().writeUTF("test");
                                msg.writer().writeShort(tempId);
                                player.sendMessage(msg);
                                msg.cleanup();
                                msg = new Message(-81);
                                msg.writer().writeByte(1);
                                msg.writer().writeByte(2);
                                msg.writer().writeByte(InventoryService.gI().getIndexBag(player, ngaVoii));
                                msg.writer().writeByte(InventoryService.gI().getIndexBag(player, cuaGaa));
                                msg.writer().writeByte(InventoryService.gI().getIndexBag(player, hongMaoo));
                                player.sendMessage(msg);
                                msg.cleanup();
                                msg = new Message(-81);
                                msg.writer().writeByte(7);
                                msg.writer().writeShort(hopQuaVip.template.iconID);
                                msg.writer().writeShort(-1);
                                msg.writer().writeShort(-1);
                                msg.writer().writeShort(-1);
                                player.sendMessage(msg);
                                msg.cleanup();
                            } catch (Exception e) {
                                System.out.println("lỗi 4");
                            }
                            InventoryService.gI().addItemList(player.inventory.itemsBag, hopQuaVip);
                            InventoryService.gI().subQuantityItemsBag(player, ngaVoii, 9);
                            InventoryService.gI().subQuantityItemsBag(player, cuaGaa, 9);
                            InventoryService.gI().subQuantityItemsBag(player, hongMaoo, 9);
                            player.inventory.gem -= gem;
                            Service.gI().sendThongBao(player, "Dâng sính lễ thành công bạn nhận được " + hopQuaVip.template.name);
                            InventoryService.gI().sendItemBags(player);
                            return;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_SINHLE_BANH_DAY) {
                    switch (select) {
                        case 0:
                            Item banhday = InventoryService.gI().findItemBag(player, 1435);
                            Item vanBay = ItemService.gI().createNewItem((short) 1235);
                            Item test = ItemService.gI().randomRac();
                            vanBay.itemOptions.add(new ItemOption(85, 0));
                            vanBay.itemOptions.add(new ItemOption(50, 7));
                            vanBay.itemOptions.add(new ItemOption(77, 5));
                            vanBay.itemOptions.add(new ItemOption(77, 5));
                            if (Util.isTrue(95, 100)) {
                                vanBay.itemOptions.add(new ItemOption(1, 7));
                            }
                            vanBay.itemOptions.add(new ItemOption(174, 2025));
                            InventoryService.gI().subQuantityItemsBag(player, banhday, 1);
                            if (Util.isTrue(1, 100)) {
                                InventoryService.gI().addItemBag(player, vanBay);
                                Service.gI().sendThongBao(player, "Dâng sính lễ thành công bạn nhận được " + vanBay.template.name);
                            } else {
                                InventoryService.gI().addItemBag(player, test);
                                Service.gI().sendThongBao(player, "Dâng sính lễ thành công bạn nhận được " + test.template.name);
                            }
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendMoney(player);
                            Service.gI().sendThongBao(player, "Dâng sính lễ thành công");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_SINHLE_BANH_CHUNG) {
                    switch (select) {
                        case 0:
                            Item banhChung = InventoryService.gI().findItemBag(player, 1436);
                            Item caiTrang = ItemService.gI().createNewItem((short) 1337);
                            Item test = ItemService.gI().randomRac();
                            caiTrang.itemOptions.add(new ItemOption(50, 17));
                            caiTrang.itemOptions.add(new ItemOption(77, 15));
                            caiTrang.itemOptions.add(new ItemOption(77, 15));
                            caiTrang.itemOptions.add(new ItemOption(14, 11));
                            if (Util.isTrue(95, 100)) {
                                caiTrang.itemOptions.add(new ItemOption(1, 7));
                            }
                            caiTrang.itemOptions.add(new ItemOption(174, 2025));
                            InventoryService.gI().subQuantityItemsBag(player, banhChung, 1);
                            if (Util.isTrue(1, 100)) {
                                InventoryService.gI().addItemBag(player, caiTrang);
                                Service.gI().sendThongBao(player, "Dâng sính lễ thành công bạn nhận được " + caiTrang.template.name);
                            } else {
                                InventoryService.gI().addItemBag(player, test);
                                Service.gI().sendThongBao(player, "Dâng sính lễ thành công bạn nhận được " + test.template.name);
                            }
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendMoney(player);
                            Service.gI().sendThongBao(player, "Dâng sính lễ thành công");
                            break;
                    }
                }
            }
        }
    }
}
