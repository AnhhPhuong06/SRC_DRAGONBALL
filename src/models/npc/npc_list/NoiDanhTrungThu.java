/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.npc.npc_list;

import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class NoiDanhTrungThu extends Npc {

    public NoiDanhTrungThu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Chọn Loại Bánh Con Muốn Nấu", "Thực hiện nấu bánh", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        int botMiQty = InventoryService.gI().getNumItemBag(player, 888);
        int dauXanhQty = InventoryService.gI().getNumItemBag(player, 889);
        int hatSenQty = InventoryService.gI().getNumItemBag(player, 1504);
        int gaConQty = InventoryService.gI().getNumItemBag(player, 887);
        int trungMuoiQty = InventoryService.gI().getNumItemBag(player, 886);

        if (player.idMark.isBaseMenu()) {
            switch (select) {
                case 0 -> {
                    this.createOtherMenu(player, 511,
                            "Chọn Loại Bánh Con Muốn Nấu",
                            "Bánh trung thu Hạt sen",
                            "Bánh trung thu Gà quay hảo hạng",
                            "Bánh trung thu Gà quay",
                            "Từ chối");
                }
                default -> {
                }
            }
        } else if (player.idMark.getIndexMenu() == 511) {
            switch (select) {
                case 0 -> {
                    // Bánh trung thu Hạt sen (20 ngọc)
                    if (botMiQty >= 10 && dauXanhQty >= 10 && hatSenQty >= 1 && trungMuoiQty >= 2 && player.inventory.gem >= 20) {
                        createOtherMenu(player, 0,
                                "|2|Bánh trung thu Hạt sen\n"
                                + "|1|Bột mì: " + botMiQty + "/10\n"
                                + "|1|Đậu xanh: " + dauXanhQty + "/10\n"
                                + "|1|Trứng muối: " + trungMuoiQty + "/2\n"
                                + "|1|Hạt sen: " + hatSenQty + "/1\n"
                                + "|1|Giá ngọc: 20\n"
                                + "|4%", "Đồng ý", "Từ chối");
                    } else {
                        String npcSay = "|2|Để làm ra 1 Bánh trung thu Hạt sen con cần:\n";
                        npcSay += (botMiQty >= 10 ? "|1|" : "|7|") + "Bột mì: " + botMiQty + "/10\n";
                        npcSay += (dauXanhQty >= 10 ? "|1|" : "|7|") + "Đậu xanh: " + dauXanhQty + "/10\n";
                        npcSay += (trungMuoiQty >= 2 ? "|1|" : "|7|") + "Trứng muối: " + trungMuoiQty + "/2\n";
                        npcSay += (hatSenQty >= 1 ? "|1|" : "|7|") + "Hạt sen: " + hatSenQty + "/1\n";
                        npcSay += (player.inventory.gem >= 20 ? "|1|" : "|7|") + "Giá ngọc: 20 (" + player.inventory.gem + "/20)\n";
                        npcSay += "|7|%";
                        createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                    }
                }
                case 1, 2 -> {
                    // select 1: Gà quay hảo hạng (20 ngọc, 30% thập cẩm)
                    // select 2: Gà quay thường (20.000.000 vàng)
                    boolean hasCurrency = (select == 1) ? (player.inventory.gem >= 20) : (player.inventory.gold >= 20_000_000);
                    if (botMiQty >= 10 && dauXanhQty >= 10 && trungMuoiQty >= 2 && gaConQty >= 1 && hasCurrency) {
                        createOtherMenu(player, select,
                                "|2|Bánh trung thu Gà quay\n"
                                + (select == 1 ? "|2|30% cơ hội nhận thêm Bánh trung thu thập cẩm\n" : "")
                                + "|1|Bột mì: " + botMiQty + "/10\n"
                                + "|1|Đậu xanh: " + dauXanhQty + "/10\n"
                                + "|1|Trứng muối: " + trungMuoiQty + "/2\n"
                                + "|1|Gà quay nguyên con: " + gaConQty + "/1\n"
                                + (select == 1 ? "|1|Giá ngọc: 20\n" : "|1|Giá vàng: 20.000.000\n")
                                + "|4%", "Đồng ý", "Từ chối");
                    } else {
                        String npcSay = "|2|Bánh trung thu Gà quay\n"
                                + (select == 1 ? "|2|30% cơ hội nhận thêm Bánh trung thu thập cẩm\n" : "");
                        npcSay += (botMiQty >= 10 ? "|1|" : "|7|") + "Bột mì: " + botMiQty + "/10\n";
                        npcSay += (dauXanhQty >= 10 ? "|1|" : "|7|") + "Đậu xanh: " + dauXanhQty + "/10\n";
                        npcSay += (trungMuoiQty >= 2 ? "|1|" : "|7|") + "Trứng muối: " + trungMuoiQty + "/2\n";
                        npcSay += (gaConQty >= 1 ? "|1|" : "|7|") + "Gà quay nguyên con: " + gaConQty + "/1\n";
                        if (select == 1) {
                            npcSay += (player.inventory.gem >= 20 ? "|1|" : "|7|") + "Giá ngọc: 20 (" + player.inventory.gem + "/20)\n";
                        } else {
                            npcSay += (player.inventory.gold >= 20_000_000 ? "|1|" : "|7|") + "Giá vàng: 20.000.000\n";
                        }
                        npcSay += "|7|%";
                        createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                    }
                }
                default -> {
                }
            }
        } else if (player.idMark.getIndexMenu() == 0) {
            // Nấu bánh Hạt sen
            if (select == 0) {
                if (botMiQty < 10 || dauXanhQty < 10 || hatSenQty < 1 || trungMuoiQty < 2) {
                    Service.gI().sendThongBao(player, "Ngươi không đủ nguyên liệu để nấu bánh!");
                    return;
                }
                if (player.inventory.gem < 20) {
                    Service.gI().sendThongBao(player, "Ngươi không đủ 20 ngọc để nấu bánh!");
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                    Service.gI().sendThongBao(player, "Hành trang cần ít nhất 2 ô trống!");
                    return;
                }
                player.inventory.gem -= 20;
                InventoryService.gI().subQuantityItemsBag(player, 888, 10);
                InventoryService.gI().subQuantityItemsBag(player, 1504, 1);
                InventoryService.gI().subQuantityItemsBag(player, 889, 10);
                InventoryService.gI().subQuantityItemsBag(player, 886, 2);

                Item banh = ItemService.gI().createNewItem((short) 1512, 1);
                banh.itemOptions.add(new Item.ItemOption(174, 2025));
                banh.itemOptions.add(new Item.ItemOption(30, 0));
                InventoryService.gI().addItemBag(player, banh);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                this.npcChat(player, "Bạn vừa nhận được Bánh trung thu Hạt sen!");
            }
        } else if (player.idMark.getIndexMenu() == 1) {
            // Nấu bánh Gà quay hảo hạng (ngọc)
            if (select == 0) {
                if (botMiQty < 10 || dauXanhQty < 10 || trungMuoiQty < 2 || gaConQty < 1) {
                    Service.gI().sendThongBao(player, "Ngươi không đủ nguyên liệu để nấu bánh!");
                    return;
                }
                if (player.inventory.gem < 20) {
                    Service.gI().sendThongBao(player, "Ngươi không đủ 20 ngọc để nấu bánh!");
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                    Service.gI().sendThongBao(player, "Hành trang cần ít nhất 2 ô trống!");
                    return;
                }
                player.inventory.gem -= 20;
                InventoryService.gI().subQuantityItemsBag(player, 888, 10);
                InventoryService.gI().subQuantityItemsBag(player, 887, 1);
                InventoryService.gI().subQuantityItemsBag(player, 889, 10);
                InventoryService.gI().subQuantityItemsBag(player, 886, 2);

                Item banhGa = ItemService.gI().createNewItem((short) 890, 1);
                banhGa.itemOptions.add(new Item.ItemOption(174, 2025));
                banhGa.itemOptions.add(new Item.ItemOption(30, 0));
                InventoryService.gI().addItemBag(player, banhGa);

                if (Util.isTrue(30, 100)) {
                    Item thapCam = ItemService.gI().createNewItem((short) 891, 1);
                    thapCam.itemOptions.add(new Item.ItemOption(174, 2025));
                    thapCam.itemOptions.add(new Item.ItemOption(30, 0));
                    InventoryService.gI().addItemBag(player, thapCam);
                    Service.gI().sendThongBao(player, "May mắn! Nhận thêm Bánh trung thu Thập cẩm!");
                }
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                this.npcChat(player, "Bạn vừa nhận được Bánh trung thu Gà quay!");
            }
        } else if (player.idMark.getIndexMenu() == 2) {
            // Nấu bánh Gà quay thường (vàng)
            if (select == 0) {
                if (botMiQty < 10 || dauXanhQty < 10 || trungMuoiQty < 2 || gaConQty < 1) {
                    Service.gI().sendThongBao(player, "Ngươi không đủ nguyên liệu để nấu bánh!");
                    return;
                }
                if (player.inventory.gold < 20_000_000) {
                    Service.gI().sendThongBao(player, "Ngươi không đủ 20.000.000 vàng để nấu bánh!");
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                    Service.gI().sendThongBao(player, "Hành trang cần ít nhất 2 ô trống!");
                    return;
                }
                player.inventory.gold -= 20_000_000;
                InventoryService.gI().subQuantityItemsBag(player, 888, 10);
                InventoryService.gI().subQuantityItemsBag(player, 887, 1);
                InventoryService.gI().subQuantityItemsBag(player, 889, 10);
                InventoryService.gI().subQuantityItemsBag(player, 886, 2);

                Item banhGa = ItemService.gI().createNewItem((short) 890, 1);
                banhGa.itemOptions.add(new Item.ItemOption(174, 2025));
                banhGa.itemOptions.add(new Item.ItemOption(30, 0));
                InventoryService.gI().addItemBag(player, banhGa);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                this.npcChat(player, "Bạn vừa nhận được Bánh trung thu Gà quay!");
            }
        }
    }
}
