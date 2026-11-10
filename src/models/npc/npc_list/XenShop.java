package models.npc.npc_list;


import consts.ConstNpc;
import models.player.Player;
import services.ShopService;
import utils.Util;

public class XenShop extends ShopNew {

    private static final String SHOP_TAG = "XEN_SHOP";

    public XenShop(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        createOtherMenu(player, ConstNpc.BASE_MENU,
                "Xin chào, ta có một số vật phẩm đặc biệt, cậu có muốn xem không?"
                + "\nĐiểm Xên hiện có:" + Util.numberToMoney(player.inventory.diemXen),
                "Cửa hàng\nHỗ Trợ", "Cửa Hàng\nThỏi Vàng", "Đổi điểm\nsự kiện", "Đóng");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (player.idMark.isBaseMenu() && select == 2) {
            ShopService.gI().opendShop(player, SHOP_TAG, false);
            return;
        }
        super.confirmMenu(player, select);
    }

    /**
     * Cộng/trừ điểm Xên — currency RIÊNG {@code inventory.diemXen}, persist qua PlayerDAO (index 6).
     * Không còn swap qua coupon: shop XEN_SHOP (id 48) trừ thẳng diemXen trong ShopService.
     */
    public static void themDiem(Player player, int delta) {
        if (player == null || player.inventory == null || delta == 0) {
            return;
        }
        player.inventory.diemXen = Math.max(0, player.inventory.diemXen + delta);
    }
}
