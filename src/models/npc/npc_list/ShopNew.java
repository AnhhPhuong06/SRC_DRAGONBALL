package models.npc.npc_list;

import consts.ConstNpc;
import java.util.Random;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.ShopService;
import services.player.InventoryService;
import utils.Util;

public class ShopNew extends Npc {

    public ShopNew(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, ta có một số vật phẩm đặc biệt, cậu có muốn xem không?",
                    "Cửa hàng\nHỗ Trợ", "Cửa Hàng\nThỏi Vàng", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        Item dieuhong = InventoryService.gI().findItem(player.inventory.itemsBag, 1004);
        Item vooc = InventoryService.gI().findItem(player.inventory.itemsBag, 695);
        Item voso = InventoryService.gI().findItem(player.inventory.itemsBag, 696);
        Item cua = InventoryService.gI().findItem(player.inventory.itemsBag, 697);
        Item sao = InventoryService.gI().findItem(player.inventory.itemsBag, 698);

        if (player.idMark.isBaseMenu()) {
            switch (select) {
                case 0 ->              
                    ShopService.gI().opendShop(player, "SU_KIEN", false);
                case 1 ->
                    ShopService.gI().opendShop(player, "THOI_VANG", false);
                default -> {
                }
            }
            return;
        } else if (player.idMark.getIndexMenu() == 33) {
            switch (select) {
                case 0:
                    if (dieuhong != null && dieuhong.quantity >= 1) {
                        if (player.inventory.ruby >= 1000) {
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                InventoryService.gI().subQuantityItemsBag(player, dieuhong, 1);
                                player.inventory.ruby -= 1000;
                                Service.gI().sendMoney(player);
                                Item rewardXoCaXanh = ItemService.gI().createNewItem((short) 1005, 1);
                                InventoryService.gI().addItemBag(player, rewardXoCaXanh);
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendThongBao(player,
                                        "Bạn đã đổi 1 Cá diêu hồng + 1000 hồng ngọc để nhận 1 "
                                        + rewardXoCaXanh.template.name + "!");
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Hành trang không đủ chỗ trống!");
                            }
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Bạn không đủ 10 ngọc để đổi!");
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không có đủ Cá diêu hồng để đổi!");
                    }
                    break;

                case 1:
                    if (dieuhong != null && dieuhong.quantity >= 1) {
                        if (player.inventory.gold >= 500_000_000) {
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                InventoryService.gI().subQuantityItemsBag(player, dieuhong, 1);
                                player.inventory.gold -= 500_000_000;
                                Service.gI().sendMoney(player);

                                Item rewardXoCaVang = ItemService.gI().createNewItem((short) 1006, 1);
                                InventoryService.gI().addItemBag(player, rewardXoCaVang);
                                InventoryService.gI().sendItemBags(player);

                                Service.gI().sendThongBao(player,
                                        "Bạn đã đổi 1 Cá diêu hồng + 500 triệu vàng để nhận 1 "
                                        + rewardXoCaVang.template.name + "!");
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Hành trang không đủ chỗ trống!");
                            }
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Bạn không đủ 500 triệu vàng để đổi!");
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không có đủ Cá diêu hồng để đổi!");
                    }
                    break;

                default:
                    break;
            }
            return;
        } else if (player.idMark.getIndexMenu() == 44) {
            switch (select) {
                case 0:
                    if (vooc != null && vooc.quantity >= 99) {
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            InventoryService.gI().subQuantityItemsBag(player, vooc, 99);
                            Item rewardEvent = ItemService.gI().createNewItem((short) Util.nextInt(954, 955), 1);
                            rewardEvent.itemOptions.add(new Item.ItemOption(50, Util.nextInt(8, 16)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(77, Util.nextInt(8, 16)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(103, Util.nextInt(8, 16)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(93, Util.nextInt(3, 15)));
                            InventoryService.gI().addItemBag(player, rewardEvent);
                            InventoryService.gI().sendItemBags(player);

                            Service.gI().sendThongBao(player,
                                    "Bạn đã đổi 99 Vỏ ốc để nhận "
                                    + rewardEvent.template.name + "!");
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Cần ít nhất 1 ô trống trong hành trang!");
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không đủ 99 Vỏ ốc để đổi!");
                    }
                    break;

                case 1:
                    if (voso != null && voso.quantity >= 99) {
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            InventoryService.gI().subQuantityItemsBag(player, voso, 99);
                            int[] itesm = {1155, 1123};
                            Random rd = new Random();
                            int randomIndex = rd.nextInt(itesm.length);
                            int randomItemId = itesm[randomIndex];
                            Item rewardEvent = ItemService.gI().createNewItem((short) randomItemId, 1);
                            rewardEvent.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(93, Util.nextInt(5, 15)));
                            InventoryService.gI().addItemBag(player, rewardEvent);
                            InventoryService.gI().sendItemBags(player);

                            Service.gI().sendThongBao(player,
                                    "Bạn đã đổi 99 Vỏ sò để nhận "
                                    + rewardEvent.template.name + "!");
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Cần ít nhất 1 ô trống trong hành trang!");
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không đủ 99 Vỏ sò để đổi!");
                    }
                    break;

                case 2:
                    if (cua != null && cua.quantity >= 99) {
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            InventoryService.gI().subQuantityItemsBag(player, cua, 99);
                            int[] itesm = {897, 1265};
                            Random rd = new Random();
                            int randomIndex = rd.nextInt(itesm.length);
                            int randomItemId = itesm[randomIndex];
                            Item rewardEvent = ItemService.gI().createNewItem((short) randomItemId, 1);
                            rewardEvent.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(93, Util.nextInt(5, 15)));
                            InventoryService.gI().addItemBag(player, rewardEvent);
                            InventoryService.gI().sendItemBags(player);

                            Service.gI().sendThongBao(player,
                                    "Bạn đã đổi 99 Con cua để nhận "
                                    + rewardEvent.template.name + "!");
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Cần ít nhất 1 ô trống trong hành trang!");
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không đủ 99 Con cua để đổi!");
                    }
                    break;

                case 3:
                    if (sao != null && sao.quantity >= 99) {
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            InventoryService.gI().subQuantityItemsBag(player, sao, 99);
                            int[] itesm = {1360, 1361, 1362};
                            Random rd = new Random();
                            int randomIndex = rd.nextInt(itesm.length);
                            int randomItemId = itesm[randomIndex];
                            Item rewardEvent = ItemService.gI().createNewItem((short) randomItemId, 1);
                            rewardEvent.itemOptions.add(new Item.ItemOption(50, Util.nextInt(15, 25)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(77, Util.nextInt(15, 25)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(103, Util.nextInt(15, 25)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(14, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(5, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(108, Util.nextInt(5, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(93, Util.nextInt(5, 15)));
                            InventoryService.gI().addItemBag(player, rewardEvent);
                            InventoryService.gI().sendItemBags(player);

                            Service.gI().sendThongBao(player,
                                    "Bạn đã đổi 99 Sao biển để nhận "
                                    + rewardEvent.template.name + "!");
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Cần ít nhất 1 ô trống trong hành trang!");
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không đủ 99 Sao biển để đổi!");
                    }
                    break;

                case 4:
                    boolean okVoOc = (vooc != null && vooc.quantity >= 99);
                    boolean okVoSo = (voso != null && voso.quantity >= 99);
                    boolean okConCua = (cua != null && cua.quantity >= 99);
                    boolean okSaoBien = (sao != null && sao.quantity >= 99);

                    if (okVoOc && okVoSo && okConCua && okSaoBien) {
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            InventoryService.gI().subQuantityItemsBag(player, vooc, 99);
                            InventoryService.gI().subQuantityItemsBag(player, voso, 99);
                            InventoryService.gI().subQuantityItemsBag(player, cua, 99);
                            InventoryService.gI().subQuantityItemsBag(player, sao, 99);

                            Item rewardEvent = ItemService.gI().createNewItem((short) 1353, 1);
                            rewardEvent.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 16)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 15)));
                            rewardEvent.itemOptions.add(new Item.ItemOption(93, Util.nextInt(5, 15)));
                            InventoryService.gI().addItemBag(player, rewardEvent);
                            InventoryService.gI().sendItemBags(player);

                            Service.gI().sendThongBao(player,
                                    "Bạn đã đổi 99 Vỏ ốc, 99 Vỏ sò, 99 Con cua, 99 Sao biển để nhận "
                                    + rewardEvent.template.name + "!");
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Cần ít nhất 1 ô trống trong hành trang!");
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không đủ 99 mỗi loại (Vỏ ốc, Vỏ sò, Con cua, Sao biển) để đổi!");
                    }
                    break;

                default:
                    break;
            }
        }
    }
}
