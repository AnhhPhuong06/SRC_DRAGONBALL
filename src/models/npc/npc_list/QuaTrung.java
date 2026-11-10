package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import consts.ConstPlayer;
import models.npc.Npc;
import models.player.Player;
import models.item.Item;
import services.player.InventoryService;
import services.Service;
import utils.Util;

public class QuaTrung extends Npc {

    private final int COST_AP_TRUNG_NHANH = 100000000;

    public QuaTrung(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == (21 + player.gender)) {
                if (player.mabuEgg == null) {
                    Service.gI().sendThongBao(player, "Con chưa có trứng Mabư.");
                    return;
                }
                player.mabuEgg.sendMabuEgg();
                if (player.mabuEgg.getSecondDone() != 0) {
                    this.createOtherMenu(player, ConstNpc.CAN_NOT_OPEN_EGG, "Bư bư bư...",
                            "Hủy bỏ\ntrứng", "Ấp nhanh\n" + Util.numberToMoney(COST_AP_TRUNG_NHANH) + " vàng",
                            "Đóng");
                } else {
                    this.createOtherMenu(player, ConstNpc.CAN_OPEN_EGG, "Bư bư bư...", "Nở", "Hủy bỏ\ntrứng",
                            "Đóng");
                }
            } else if (this.mapId == 154) {
                player.billEgg.sendBillEgg();
                if (player.billEgg.getSecondDone() != 0) {
                    this.createOtherMenu(player, ConstNpc.CAN_NOT_OPEN_EGG, "Burk Burk...",
                            "Hủy bỏ\ntrứng", "Ấp nhanh\n" + Util.numberToMoney(COST_AP_TRUNG_NHANH) + " vàng", "Đóng");
                } else {
                    this.createOtherMenu(player, ConstNpc.CAN_OPEN_EGG, "Burk Burk...", "Nở", "Hủy bỏ\ntrứng", "Đóng");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == (21 + player.gender)) {
                if (player.mabuEgg == null) {
                    return;
                }
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.CAN_NOT_OPEN_EGG -> {
                        if (select == 0) {
                            this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                    "Bạn có chắc chắn muốn hủy bỏ trứng Mabư?", "Đồng ý", "Từ chối");
                        } else if (select == 1) {
                            if (player.inventory.gold >= COST_AP_TRUNG_NHANH) {
                                player.inventory.gold -= COST_AP_TRUNG_NHANH;
                                player.mabuEgg.timeDone = 0;
                                Service.gI().sendMoney(player);
                                player.mabuEgg.sendMabuEgg();
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Bạn không đủ vàng để thực hiện, còn thiếu "
                                        + Util.numberToMoney(
                                                (COST_AP_TRUNG_NHANH - player.inventory.gold))
                                        + " vàng");
                            }
                        }
                    }
                    case ConstNpc.CAN_OPEN_EGG -> {
                        switch (select) {
                            case 0 -> {
                                this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                        "Bạn có chắc chắn cho trứng nở?\nĐệ tử của bạn sẽ được thay thế bằng đệ Mabư",
                                        "Đệ mabư\nTrái Đất", "Đệ mabư\nNamếc", "Đệ mabư\nXayda", "Từ chối");
                            }
                            case 1 ->
                                this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                        "Bạn có chắc chắn muốn hủy bỏ trứng Mabư?", "Đồng ý", "Từ chối");
                        }
                    }
                    case ConstNpc.CONFIRM_OPEN_EGG -> {
                        switch (select) {
                            case 0 ->
                                player.mabuEgg.openEgg(ConstPlayer.TRAI_DAT);
                            case 1 ->
                                player.mabuEgg.openEgg(ConstPlayer.NAMEC);
                            case 2 ->
                                player.mabuEgg.openEgg(ConstPlayer.XAYDA);
                            default -> {
                            }
                        }
                    }
                    case ConstNpc.CONFIRM_DESTROY_EGG -> {
                        if (select == 0) {
                            player.mabuEgg.destroyEgg();
                        }
                    }
                }
            } else if (this.mapId == 154) {
                if (player.billEgg == null) {
                    return;
                }
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.CAN_NOT_OPEN_BILL:
                        if (select == 0) {
                            this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_BILL,
                                    "Bạn có chắc chắn muốn hủy bỏ trứng Bill?", "Đồng ý", "Từ chối");
                        } else if (select == 1) {
                            if (player.inventory.gold >= COST_AP_TRUNG_NHANH) {
                                player.inventory.gold -= COST_AP_TRUNG_NHANH;
                                player.billEgg.timeDone = 0;
                                Service.gI().sendMoney(player);
                                player.billEgg.sendBillEgg();
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Bạn không đủ vàng để thực hiện, còn thiếu "
                                        + Util.numberToMoney((COST_AP_TRUNG_NHANH - player.inventory.gold)) + " vàng");
                            }
                        }
                        break;
                    case ConstNpc.CAN_OPEN_EGG:
                        switch (select) {
                            case 0:
                                this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_BILL,
                                        "Bạn có chắc chắn cho trứng nở?\n"
                                        + "Đệ tử của bạn sẽ được thay thế bằng đệ Bill",
                                        "Đệ Bill\nTrái Đất", "Đệ Bill\nNamếc", "Đệ Bill\nXayda", "Từ chối");
                                break;
                            case 1:
                                this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_BILL,
                                        "Bạn có chắc chắn muốn hủy bỏ trứng Bill?", "Đồng ý", "Từ chối");
                                break;
                        }
                        break;
                    case ConstNpc.CONFIRM_OPEN_BILL:
                        switch (select) {
                            case 0:
                                player.billEgg.openEgg(ConstPlayer.TRAI_DAT);
                                break;
                            case 1:
                                player.billEgg.openEgg(ConstPlayer.NAMEC);
                                break;
                            case 2:
                                player.billEgg.openEgg(ConstPlayer.XAYDA);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstNpc.CONFIRM_DESTROY_BILL:
                        if (select == 0) {
                            player.billEgg.destroyEgg();
                        }
                        break;
                }
            }
        }
    }
}
