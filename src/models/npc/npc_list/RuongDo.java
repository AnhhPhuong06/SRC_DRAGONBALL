package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import models.npc.Npc;
import models.player.Player;
import services.ShopService;
import services.func.Input;
import services.player.InventoryService;

public class RuongDo extends Npc {

    public RuongDo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU, "|7|Bạn đang có " + player.inventory.itemsBoxCrackBall.stream().filter(it -> it.isNotNullItem() && it.message != null).count() + " thư chưa nhận.",
                    "Rương đồ", "Hòm thư", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 ->
                        InventoryService.gI().sendItemBox(player);
                    case 1 ->
                        ShopService.gI().opendShop(player, "Gift_Box", true);
                }
            }
        }
    }
}
