/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.npc.npc_list;

import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import models.npc.NpcFactory;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.ItemService;
import services.Service;
import services.ShopService;
import services.func.Input;
import services.map.ChangeMapService;
import services.player.InventoryService;
import utils.Logger;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class CayThong extends Npc {

    public CayThong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 0:
                case 7:
                case 14:
                    createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Đang có 1 lượt trang trí\n"
                            + "Trang trí 5.000 lượt sẽ tặng: x2 exp toàn máy chủ 12 giờ\n"
                            + "Trang trí 15000 lượt sẽ tặng: x3 exp toàn máy chủ 12 giờ\n"
                            + "Trang trí 25000 lượt sẽ tăng: x4 exp toàn máy chủ 24 giờ",
                            "Trang trí", "Shop\nSự Kiện", "Top 100\nTrang trí\nCây Noel",
                            "Xem điểm", "Vùng đất\nbăng giá\nSự kiện Noel", "Đóng");
                case 184:
                    createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ta có thể giúp gì cho ngươi ?\n",
                            "Quay về", "Đóng");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 0 || this.mapId == 7 || this.mapId == 14) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            Item chuong = InventoryService.gI().findItemBagByTemp(player, 1215);
                            Item quachau = InventoryService.gI().findItemBagByTemp(player, 1216);
                            Item ngoisao = InventoryService.gI().findItemBagByTemp(player, 1217);
                            Item datkimtuyen = InventoryService.gI().findItemBagByTemp(player, 1218);
                            Item moctreonoel = InventoryService.gI().findItemBagByTemp(player, 1219);

                            if ((chuong != null && chuong.quantity >= 30)
                                    && (quachau != null && quachau.quantity >= 30)
                                    && (ngoisao != null && ngoisao.quantity >= 30)
                                    && (datkimtuyen != null && datkimtuyen.quantity >= 2)
                                    && (moctreonoel != null && moctreonoel.quantity >= 1)) {
                                createOtherMenu(player, ConstNpc.MENU_DOI_VPSK,
                                        "|2|Trang trí noel\n"
                                        + "|1|Chuông " + chuong.quantity + "/30\n"
                                        + "Quả châu " + quachau.quantity + "/30\n"
                                        + "Ngôi sao " + ngoisao.quantity + "/30\n"
                                        + "Dây kim tuyến " + datkimtuyen.quantity + "/2\n"
                                        + "Móc treo Noel " + moctreonoel.quantity + "/1\n"
                                        + "|4%", "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Trang trí noel\n";
                                if (chuong == null) {
                                    NpcSay += "|7|Chuông " + "0/30\n";
                                } else {
                                    NpcSay += "|1|Chuông " + chuong.quantity + "/30\n";
                                }
                                if (quachau == null) {
                                    NpcSay += "|7|Quả châu " + "0/30\n";
                                } else {
                                    NpcSay += "|1|Quả châu " + quachau.quantity + "/30\n";
                                }
                                if (ngoisao == null) {
                                    NpcSay += "|7|Ngôi sao " + "0/30\n";
                                } else {
                                    NpcSay += "|1|Ngôi sao " + ngoisao.quantity + "/30\n";
                                }
                                if (datkimtuyen == null) {
                                    NpcSay += "|7|Dây kim tuyến " + "0/2\n";
                                } else {
                                    NpcSay += "|1|Dây kim tuyến " + datkimtuyen.quantity + "/2\n";
                                }
                                if (moctreonoel == null) {
                                    NpcSay += "|7|Móc treo Noel " + "0/1\n";
                                } else {
                                    NpcSay += "|1|Móc treo Noel " + moctreonoel.quantity + "/1\n";
                                }
                                NpcSay += "|7|%\n";
                                createOtherMenu(player, ConstNpc.MENU_DOI_VPSK_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                        case 1:
                            ShopService.gI().opendShop(player, "SHOP_NOEL", false);
                            break;
                        case 2:
//                            Service.gI().showListTop(player, Manager.topsk);
                            break;
                        case 3:
                            createOtherMenu(player, ConstNpc.MENI_XEM_DIEM,
                                            "|7|SỰ KIỆN TRANG TRÍ NOEL\n"
                                            + "|7|Bạn đang có " + player.diemsukien + " điểm",
                                             "Đóng");
                            break;

                        case 4:
                            ChangeMapService.gI().changeMapNonSpaceship(player, 184, 1548, 432);
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_DOI_VPSK) {
                    switch (select) {
                        case 0:
                            Item chuong = InventoryService.gI().findItemBagByTemp(player, 1215);
                            Item quachau = InventoryService.gI().findItemBagByTemp(player, 1216);
                            Item ngoisao = InventoryService.gI().findItemBagByTemp(player, 1217);
                            Item datkimtuyen = InventoryService.gI().findItemBagByTemp(player, 1218);
                            Item moctreonoel = InventoryService.gI().findItemBagByTemp(player, 1219);

                            short daykimtuyen = 1220;
                            Item sachTuyetKy = ItemService.gI().createNewItem((short) daykimtuyen);

                            if (Util.isTrue(100, 100)) {
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
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, chuong));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, quachau));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, ngoisao));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, datkimtuyen));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, moctreonoel));
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                    msg = new Message(-81);
                                    msg.writer().writeByte(7);
                                    msg.writer().writeShort(sachTuyetKy.template.iconID);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                } catch (Exception e) {
                                    System.out.println("lỗi 4");
                                }
                                InventoryService.gI().addItemList(player.inventory.itemsBag, sachTuyetKy);
                                InventoryService.gI().subQuantityItemsBag(player, chuong, 30);
                                InventoryService.gI().subQuantityItemsBag(player, quachau, 30);
                                InventoryService.gI().subQuantityItemsBag(player, ngoisao, 30);
                                InventoryService.gI().subQuantityItemsBag(player, datkimtuyen, 2);
                                InventoryService.gI().subQuantityItemsBag(player, moctreonoel, 1);
                                player.diemsukien += 1;
                                Service.gI().sendThongBao(player, "Bạn nhận được được 1 điểm trang trí");
                                InventoryService.gI().sendItemBags(player);
                                return;
                            } else {
                                try { // send effect faile
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
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, chuong));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, quachau));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, ngoisao));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, datkimtuyen));
                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, moctreonoel));
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                    msg = new Message(-81);
                                    msg.writer().writeByte(8);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                } catch (Exception e) {
                                    Logger.logException(NpcFactory.class, e, "Lỗi VPSK");
                                }
                                InventoryService.gI().subQuantityItemsBag(player, chuong, 30);
                                InventoryService.gI().subQuantityItemsBag(player, quachau, 30);
                                InventoryService.gI().subQuantityItemsBag(player, ngoisao, 30);
                                InventoryService.gI().subQuantityItemsBag(player, datkimtuyen, 2);
                                InventoryService.gI().subQuantityItemsBag(player, moctreonoel, 1);
                                player.diemsukien += 1;
                                Service.gI().sendThongBao(player, "Bạn nhận được được 1 điểm trang trí");
                                InventoryService.gI().sendItemBags(player);
                            }
                            return;
                        case 1:
                            break;
                    }
                }
            }

            // Separate block for mapId == 184
            if (this.mapId == 184) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                            break;
                    }
                }
            }
        }
    }
}
