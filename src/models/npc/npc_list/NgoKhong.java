package models.npc.npc_list;

/**
 *
 * @author NGOJC
 */
import consts.ConstItem;
import consts.ConstNpc;
import java.util.Random;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.player.InventoryService;

public class NgoKhong extends Npc {

    public NgoKhong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 122 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU, "|0|Chu mi nga\n",
                            "Tặng quả\nhồng đào", "Tặng quả\nhồng đào\nchín");
                }
                default ->
                    super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0: {
                        Item daohong = InventoryService.gI().findItemBag(player, ConstItem.QUA_HONG_DAO);

                        if (daohong != null && daohong.quantity >= 1) {
                            InventoryService.gI().subQuantityItemsBag(player, daohong, 1);
                            Item[] chuItems = new Item[]{
                                ItemService.gI().createNewItem((short) ConstItem.CHU_KHAI),
                                ItemService.gI().createNewItem((short) ConstItem.CHU_GIAI),
                                ItemService.gI().createNewItem((short) ConstItem.CHU_AN),
                                ItemService.gI().createNewItem((short) ConstItem.CHU_PHONG)
                            };
                            Random rand = new Random();
                            Item selectedItem = chuItems[rand.nextInt(chuItems.length)];
                            InventoryService.gI().addItemBag(player, selectedItem);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Bạn nhận được: " + selectedItem.template.name);
                        } else {
                            Service.gI().sendThongBao(player, "cần 1 quả hồng đào!");
                        }
                        break;
                    }
                    case 1: {
                        Item daohong = InventoryService.gI().findItemBag(player, ConstItem.QUA_HONG_DAO_CHIN);
                        if (daohong != null && daohong.quantity >= 1) {
                            InventoryService.gI().subQuantityItemsBag(player, daohong, 1);
                            Item[] chuItems = new Item[]{
                                ItemService.gI().createNewItem((short) ConstItem.CHU_KHAI),
                                ItemService.gI().createNewItem((short) ConstItem.CHU_GIAI),
                                ItemService.gI().createNewItem((short) ConstItem.CHU_AN),
                                ItemService.gI().createNewItem((short) ConstItem.CHU_PHONG)
                            };
                            Random rand = new Random();
                            Item selectedItem = chuItems[rand.nextInt(chuItems.length)];
                            InventoryService.gI().addItemBag(player, selectedItem);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Bạn nhận được: " + selectedItem.template.name);
                        } else {
                            Service.gI().sendThongBao(player, "cần 1 quả hồng đào!");
                        }
                        break;
                    }
                }
            }
        }
    }

}
