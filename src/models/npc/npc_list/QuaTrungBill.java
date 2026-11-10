package models.npc.npc_list;

 
import consts.ConstNpc;
import consts.ConstPlayer;
import models.npc.Npc;
import models.player.Player;
import models.item.Item;
import services.player.InventoryService;
import services.Service;
import utils.Util;

public class QuaTrungBill extends Npc {

    private final int COST_AP_TRUNG_NHANH = 1_000_000_000;

    public QuaTrungBill(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 154) {
                if (player.billEgg != null) {
                    player.billEgg.sendBillEgg();
                    if (player.billEgg.getSecondDone() != 0) {
                        this.createOtherMenu(player, ConstNpc.CAN_NOT_OPEN_EGG, "Burk Burk...",
                                "Hủy bỏ\ntrứng", "Ấp nhanh\n" + Util.numberToMoney(COST_AP_TRUNG_NHANH) + " vàng", "Đóng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.CAN_OPEN_EGG, "Burk Burk...", "Nở", "Hủy bỏ\ntrứng", "Đóng");
                    }
                } else if (player.blackEgg != null) {
                    player.blackEgg.sendBlackEgg();
                    if (player.blackEgg.getSecondDone() != 0) {
                        this.createOtherMenu(player, ConstNpc.CAN_NOT_OPEN_EGG, "Hắc ám đang trỗi dậy...",
                                "Hủy bỏ\ntrứng", "Ấp nhanh\n" + Util.numberToMoney(COST_AP_TRUNG_NHANH) + " vàng", "Đóng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.CAN_OPEN_EGG, "Hắc ám đang trỗi dậy...", "Nở", "Hủy bỏ\ntrứng", "Đóng");
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 154) {
                boolean isBlack = player.billEgg == null && player.blackEgg != null;
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.CAN_NOT_OPEN_EGG:
                        if (select == 0) {
                            this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                    isBlack ? "Bạn có chắc chắn muốn hủy bỏ trứng Black Goku?"
                                            : "Bạn có chắc chắn muốn hủy bỏ trứng Bill?", "Đồng ý", "Từ chối");
                        } else if (select == 1) {
                            if (player.inventory.gold >= COST_AP_TRUNG_NHANH) {
                                player.inventory.gold -= COST_AP_TRUNG_NHANH;
                                if (isBlack) {
                                    player.blackEgg.timeDone = 0;
                                    Service.gI().sendMoney(player);
                                    player.blackEgg.sendBlackEgg();
                                } else {
                                    player.billEgg.timeDone = 0;
                                    Service.gI().sendMoney(player);
                                    player.billEgg.sendBillEgg();
                                }
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
                                if (isBlack) {
                                    this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                            "Bạn có chắc chắn cho trứng nở?\n"
                                            + "Đệ tử của bạn sẽ được thay thế bằng đệ Black Goku",
                                            "Đệ Black\nTrái Đất", "Đệ Black\nNamếc", "Đệ Black\nXayda", "Từ chối");
                                } else {
                                    this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                            "Bạn có chắc chắn cho trứng nở?\n"
                                            + "Đệ tử của bạn sẽ được thay thế bằng đệ Bill",
                                            "Đệ Bill\nTrái Đất", "Đệ Bill\nNamếc", "Đệ Bill\nXayda", "Từ chối");
                                }
                                break;
                            case 1:
                                this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                        isBlack ? "Bạn có chắc chắn muốn hủy bỏ trứng Black Goku?"
                                                : "Bạn có chắc chắn muốn hủy bỏ trứng Bill?", "Đồng ý", "Từ chối");
                                break;
                        }
                        break;
                    case ConstNpc.CONFIRM_OPEN_EGG:
                        switch (select) {
                            case 0:
                                if (isBlack) {
                                    player.blackEgg.openEgg(ConstPlayer.TRAI_DAT);
                                } else {
                                    player.billEgg.openEgg(ConstPlayer.TRAI_DAT);
                                }
                                break;
                            case 1:
                                if (isBlack) {
                                    player.blackEgg.openEgg(ConstPlayer.NAMEC);
                                } else {
                                    player.billEgg.openEgg(ConstPlayer.NAMEC);
                                }
                                break;
                            case 2:
                                if (isBlack) {
                                    player.blackEgg.openEgg(ConstPlayer.XAYDA);
                                } else {
                                    player.billEgg.openEgg(ConstPlayer.XAYDA);
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstNpc.CONFIRM_DESTROY_EGG:
                        if (select == 0) {
                            if (isBlack) {
                                player.blackEgg.destroyEgg();
                            } else {
                                player.billEgg.destroyEgg();
                            }
                        }
                        break;
                }
            }
        }
    }
}
