package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ShopService;
import services.func.Input;
import services.player.InventoryService;

public class Santa extends Npc {

    public Santa(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            int soLuong = 0;
            if (pGG != null) {
                soLuong = pGG.quantity;
            }
            if (soLuong >= 1) {
                this.createOtherMenu(player, ConstNpc.SANTA_PGG, "Xin chào, ta có một số vật phẩm đặt biệt cậu có muốn xem không?",
                        "Cửa hàng", "Giảm giá\n80%", "Mở rộng\nHành trang\nRương đồ", "Nhập mã\nQùa tặng", "Tiệm\nHớt tóc");
            } else {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Xin chào, ta có một số vật phẩm đặt biệt cậu có muốn xem không?",
                        "Cửa hàng", "Mở rộng\nHành trang\nRương đồ", "Nhập mã\nQùa tặng", "Tiệm\nHớt tóc");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 5 || this.mapId == 13 || this.mapId == 20) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0 ->
                            ShopService.gI().opendShop(player, "SANTA", false);
                        case 1 ->
                            ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                        case 2 ->
                            Input.gI().createFormGiftCode(player);
                        case 3 ->
                            ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.SANTA_PGG) {
                    switch (select) {
                        case 0 ->
                            ShopService.gI().opendShop(player, "SANTA", false);
                        case 1 ->
                            ShopService.gI().opendShop(player, "SANTA_PGG", false);
                        case 2 ->
                            ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                        case 3 ->
                            Input.gI().createFormGiftCode(player);
                        case 4 ->
                            ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                    }
                }
            }
        }
    }
}