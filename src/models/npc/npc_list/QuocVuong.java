package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */


import consts.ConstNpc;
import models.npc.Npc;
import models.player.NPoint;
import models.player.Player;
import services.OpenPowerService;
import services.Service;
import models.item.Item;
import services.player.InventoryService;
import utils.Util;

public class QuocVuong extends Npc {

    public QuocVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                "Bản thân", "Đệ tử", "Từ chối");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
                            String msg = "Ta sẽ truền năng lượng giúp con mở giới hạn sức mạnh của bản thân lên "
                                    + Util.numberToMoney(player.nPoint.getPowerNextLimit());
                            if (player.nPoint.limitPower >= 5) {
                                msg += "\n(Yêu cầu: 1 món đồ Thần Linh trong hành trang)";
                            }
                            this.createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT,
                                    msg,
                                    "Nâng\ngiới hạn\nsức mạnh",
                                    "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " ngọc", "Đóng");
                        } else {
                            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                    "Sức mạnh của con đã đạt tới giới hạn",
                                    "Đóng");
                        }
                    }
                    case 1 -> {
                        if (player.pet != null) {
                            if (player.pet.nPoint.limitPower < NPoint.MAX_LIMIT) {
                                String msg = "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của đệ tử lên "
                                        + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit());
                                if (player.pet.nPoint.limitPower >= 5) {
                                    msg += "\n(Yêu cầu: 1 món đồ Thần Linh trong hành trang)";
                                }
                                this.createOtherMenu(player, ConstNpc.OPEN_POWER_PET,
                                        msg,
                                        "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " ngọc", "Đóng");
                            } else {
                                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                        "Sức mạnh của đệ con đã đạt tới giới hạn",
                                        "Đóng");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                        }
                        //giới hạn đệ tử
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.OPEN_POWER_MYSEFT) {
                switch (select) {
                    case 0 -> {
                        if (player.nPoint.limitPower >= 5) {
                            Item itemTL = player.inventory.itemsBag.stream().filter(item -> item != null && item.isNotNullItem() && item.isDTL()).findFirst().orElse(null);
                            if (itemTL == null) {
                                Service.gI().sendThongBao(player, "Yêu cầu cần có 1 món đồ Thần Linh trong hành trang để mở giới hạn sức mạnh trên 40 tỉ!");
                                return;
                            }
                            OpenPowerService.gI().openPowerBasic(player);
                        } else {
                            OpenPowerService.gI().openPowerBasic(player);
                        }
                    }
                    case 1 -> {
                        if (player.inventory.gem >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                            if (player.nPoint.limitPower >= 5) {
                                Item itemTL = player.inventory.itemsBag.stream().filter(item -> item != null && item.isNotNullItem() && item.isDTL()).findFirst().orElse(null);
                                if (itemTL == null) {
                                    Service.gI().sendThongBao(player, "Yêu cầu cần có 1 món đồ Thần Linh trong hành trang để mở giới hạn sức mạnh trên 40 tỉ!");
                                    return;
                                }
                                if (OpenPowerService.gI().openPowerSpeed(player)) {
                                    player.inventory.gem -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                                    Service.gI().sendMoney(player);
                                }
                            } else {
                                if (OpenPowerService.gI().openPowerSpeed(player)) {
                                    player.inventory.gem -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                                    Service.gI().sendMoney(player);
                                }
                            }
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Bạn không đủ ngọc để mở, còn thiếu "
                                    + Util.numberToMoney((OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gem)) + " ngọc");
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.OPEN_POWER_PET) {
                if (select == 0) {
                    if (player.inventory.gem >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                        if (player.pet.nPoint.limitPower >= 5) {
                            Item itemTL = player.inventory.itemsBag.stream().filter(item -> item != null && item.isNotNullItem() && item.isDTL()).findFirst().orElse(null);
                            if (itemTL == null) {
                                Service.gI().sendThongBao(player, "Yêu cầu cần có 1 món đồ Thần Linh trong hành trang để mở giới hạn sức mạnh trên 40 tỉ!");
                                return;
                            }
                            if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                                player.inventory.gem -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                                Service.gI().sendMoney(player);
                            }
                        } else {
                            if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                                player.inventory.gem -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                                Service.gI().sendMoney(player);
                            }
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không đủ ngọc để mở, còn thiếu "
                                + Util.numberToMoney((OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gem)) + " ngọc");
                    }
                }
            }
        }
    }
}
