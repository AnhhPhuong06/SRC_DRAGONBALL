/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.npc.npc_list;

import consts.ConstNpc;
import java.time.LocalDate;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.player.InventoryService;

/**
 *
 * @author Administrator
 */
public class NoiBanh1 extends Npc {

    public NoiBanh1(int mapId, int status, int cx, int cy, int tempId, int avartar) {
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
        Item Thit = InventoryService.gI().findItem(player.inventory.itemsBag, 748);
        Item Nep = InventoryService.gI().findItem(player.inventory.itemsBag, 749);
        Item Dauxanh = InventoryService.gI().findItem(player.inventory.itemsBag, 750);
        Item Ladong = InventoryService.gI().findItem(player.inventory.itemsBag, 751);
        Item TrungMuoi = InventoryService.gI().findItem(player.inventory.itemsBag, 886);
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        this.createOtherMenu(player, 511,
                                "Chọn Loại Bánh Con Muốn Nấu", "Bánh Chưng", "Bánh Tét", "Từ chối");

                    }
                    default -> {
                    }
                }
            } else if (player.idMark.getIndexMenu() == 511) {
                switch (select) {
                    case 0 -> {
                        if ((Thit != null && Thit.quantity >= 20) && (Nep != null && Nep.quantity >= 20) && (Dauxanh != null && Dauxanh.quantity >= 20) && (Ladong != null && Ladong.quantity >= 20)
                                && (TrungMuoi != null && TrungMuoi.quantity >= 3)) {
                            createOtherMenu(player, 0,
                                    "|2|Để làm ra 1 Bánh Chưng\n"
                                    + "|1|Thịt Heo " + Thit.quantity + "/20\n"
                                    + "|1|Thúng Nếp " + Nep.quantity + "/20\n"
                                    + "|1|Thúng đậu xanh " + Dauxanh.quantity + "/20\n"
                                    + "|1|Lá Dong " + Ladong.quantity + "/20\n"
                                    + "|1|Trứng muối " + TrungMuoi.quantity + "/1\n"
                                    + "|4%", "Đồng ý", "Từ chối");
                            break;
                        } else {
                            String NpcSay = "|2|Để làm ra 1 Bánh chưng\n";
                            if (Thit == null) {
                                NpcSay += "|7|Thịt Heo " + "0/20\n";
                            } else {
                                NpcSay += "|1|Thịt Heo " + Thit.quantity + "/20\n";
                            }
                            if (Nep == null) {
                                NpcSay += "|7|Thúng Nếp " + "0/20\n";
                            } else {
                                NpcSay += "|1|Thúng Nếp " + Nep.quantity + "/20\n";
                            }
                            if (Dauxanh == null) {
                                NpcSay += "|7|Thúng đậu xanh " + "0/20\n";
                            } else {
                                NpcSay += "|1|Thúng đậu xanh " + Dauxanh.quantity + "/20\n";
                            }
                            if (Ladong == null) {
                                NpcSay += "|7|Lá Dong " + "0/20\n";
                            } else {
                                NpcSay += "|1|Lá Dong " + Ladong.quantity + "/20\n";
                            }
                            if (TrungMuoi == null) {
                                NpcSay += "|7|Trứng muối " + "0/1\n";
                            } else {
                                NpcSay += "|1|Trứng muối " + TrungMuoi.quantity + "/1\n";
                            }
                            NpcSay += "|7|%\n";
                            createOtherMenu(player, 0,
                                    NpcSay, "Từ chối");
                        }
                    }
                    case 1 -> {
                        if ((Thit != null && Thit.quantity >= 10) && (Nep != null && Nep.quantity >= 10) && (Dauxanh != null && Dauxanh.quantity >= 10) && (Ladong != null && Ladong.quantity >= 10)) {
                            createOtherMenu(player, 1,
                                    "|2|Để làm ra 1 Bánh Chưng\n"
                                    + "|1|Thịt Heo " + Thit.quantity + "/10\n"
                                    + "|1|Thúng Nếp " + Nep.quantity + "/10\n"
                                    + "|1|Thúng đậu xanh " + Dauxanh.quantity + "/10\n"
                                    + "|1|Lá Dong " + Ladong.quantity + "/10\n"
                                    + "|4%", "Đồng ý", "Từ chối");
                            break;
                        } else {
                            String NpcSay = "|2|Để làm ra 1 Bánh chưng\n";
                            if (Thit == null) {
                                NpcSay += "|7|Thịt Heo " + "0/10\n";
                            } else {
                                NpcSay += "|1|Thịt Heo " + Thit.quantity + "/10\n";
                            }
                            if (Nep == null) {
                                NpcSay += "|7|Thúng Nếp " + "0/10\n";
                            } else {
                                NpcSay += "|1|Thúng Nếp " + Nep.quantity + "/10\n";
                            }
                            if (Dauxanh == null) {
                                NpcSay += "|7|Thúng đậu xanh " + "0/10\n";
                            } else {
                                NpcSay += "|1|Thúng đậu xanh " + Dauxanh.quantity + "/10\n";
                            }
                            if (Ladong == null) {
                                NpcSay += "|7|Lá Dong " + "0/10\n";
                            } else {
                                NpcSay += "|1|Lá Dong " + Ladong.quantity + "/10\n";
                            }
                            NpcSay += "|7|%\n";
                            createOtherMenu(player, 1,
                                    NpcSay, "Từ chối");
                        }
                    }
                    default -> {
                    }
                }
            } else if (player.idMark.getIndexMenu() == 0) {
                switch (select) {
                    case 0 -> {
                        if (Thit != null && Nep != null && Dauxanh != null && Ladong != null && TrungMuoi != null
                                && Thit.quantity >= 20 && Nep.quantity >= 20 && Dauxanh.quantity >= 20 && Ladong.quantity >= 20 && TrungMuoi.quantity >= 3) {
                            if (InventoryService.gI().getCountEmptyBag(player) > 2) {
                                Item devndungtv = ItemService.gI().createNewItem((short) 753, 1);
                                devndungtv.itemOptions.add(new Item.ItemOption(50, 25));
                                devndungtv.itemOptions.add(new Item.ItemOption(14, 25));
                                devndungtv.itemOptions.add(new Item.ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, devndungtv);
                                InventoryService.gI().subQuantityItemsBag(player, Thit, 10);
                                InventoryService.gI().subQuantityItemsBag(player, Nep, 10);
                                InventoryService.gI().subQuantityItemsBag(player, Dauxanh, 10);
                                InventoryService.gI().subQuantityItemsBag(player, Ladong, 10);
                                InventoryService.gI().subQuantityItemsBag(player, TrungMuoi, 3);
                                InventoryService.gI().sendItemBags(player);
                                this.npcChat(player, "Bạn vừa nhận được bánh chưng");
                                player.inventory.gold -= 5000000;
                                Service.gI().sendMoney(player);
                            } else {
                                Service.gI().sendThongBao(player, "Ngươi cần 2 ô trống hành trang");
                            }
                        }
                    }
                    default -> {
                    }
                }
            } else if (player.idMark.getIndexMenu() == 1) {
                switch (select) {
                    case 0 -> {
                        if (Thit != null && Nep != null && Dauxanh != null && Ladong != null
                                && Thit.quantity >= 10 && Nep.quantity >= 10 && Dauxanh.quantity >= 10 && Ladong.quantity >= 10) {
                            if (InventoryService.gI().getCountEmptyBag(player) > 2) {
                                Item devndungtv = ItemService.gI().createNewItem((short) 752, 1);
                                devndungtv.itemOptions.add(new Item.ItemOption(50, 15));
                                devndungtv.itemOptions.add(new Item.ItemOption(14, 15));
                                devndungtv.itemOptions.add(new Item.ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, devndungtv);
                                InventoryService.gI().subQuantityItemsBag(player, Thit, 10);
                                InventoryService.gI().subQuantityItemsBag(player, Nep, 10);
                                InventoryService.gI().subQuantityItemsBag(player, Dauxanh, 10);
                                InventoryService.gI().subQuantityItemsBag(player, Ladong, 10);
                                InventoryService.gI().sendItemBags(player);
                                this.npcChat(player, "Bạn vừa nhận được bánh tét");
                                player.inventory.gold -= 5000000;
                                Service.gI().sendMoney(player);
                            } else {
                                Service.gI().sendThongBao(player, "Ngươi cần 2 ô trống hành trang");
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        }
    }

};
