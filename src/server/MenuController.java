package server;

import java.io.IOException;
import java.util.Objects;

import consts.ConstItem;
import consts.ConstNpc;
import models.item.Item;
import models.npc.Npc;
import services.map.NpcManager;
import network.session.MySession;
import models.player.Player;
import services.ItemService;
import services.ItemTimeService;
import services.Service;
import services.func.TransactionService;
import services.player.InventoryService;
import utils.Util;

public class MenuController {

    private static MenuController instance;

    public static MenuController gI() {
        if (instance == null) {
            instance = new MenuController();
        }
        return instance;
    }

    public static boolean isEligibleForTrickOrTreat(Npc npc, Player player) {
        if (npc == null || player == null || player.npcsTrickOrTreat == null) {
            return false;
        }
        player.checkResetTrickOrTreat();
        int id = npc.tempId;
        // Loại trừ các NPC hòm đồ, quả trứng, đậu thần, rồng thiêng...
        if (id == ConstNpc.RUONG_DO || id == ConstNpc.DAU_THAN || id == ConstNpc.CON_MEO
                || id == ConstNpc.KHU_VUC || id == 50 || id == 51
                || (id >= 24 && id <= 36) || id < 0) {
            return false;
        }
        return !player.npcsTrickOrTreat.contains(id);
    }

    public static void handleTrickOrTreat(Npc npc, Player player) {
        boolean choKeo = Util.isTrue(50, 100);
        if (choKeo) {
            npc.createOtherMenu(player, ConstNpc.MENU_NHAN_KEO,
                    "|7|[ HALLOWEEN: CHO KẸO HAY BỊ GHẸO? ]\n"
                    + "|7|Hôm nay là lễ Halloween!\n"
                    + "|7|Haha, ta cho con 1 Kẹo Bàn Tay nè!\n"
                    + "|7|(Mỗi ngày nhận 1 lần, làm mới lúc 3h sáng)",
                    "Nhận kẹo");
        } else {
            npc.createOtherMenu(player, ConstNpc.MENU_BI_GHEO,
                    "|7|[ HALLOWEEN: CHO KẸO HAY BỊ GHẸO? ]\n"
                    + "|7|Hôm nay là lễ Halloween!\n"
                    + "|7|Ú òa! Không có kẹo đâu, ta ghẹo con biến thành Xương Khô nhé haha!\n"
                    + "|7|(Biến thành xương khô 30 phút - làm mới lúc 3h sáng)",
                    "Bị ghẹo");
        }
    }

    public void openMenuNPC(MySession session, int idnpc, Player player) {
        TransactionService.gI().cancelTrade(player);

        if (player.zone == null) {
            Service.gI().hideWaitDialog(player);
            return;
        }
        Npc npc;
        if (idnpc == ConstNpc.CALICK && player.zone.map.mapId != 102) {
            npc = NpcManager.getNpc(ConstNpc.CALICK);
        }
//        else if (idnpc == ConstNpc.LY_TIEU_NUONG) {
//            npc = NpcManager.getNpc(ConstNpc.LY_TIEU_NUONG);
//        } 
        else {
            npc = player.zone.map.getNpc(player, idnpc);
        }
        if (npc != null) {
            if (isEligibleForTrickOrTreat(npc, player)) {
                handleTrickOrTreat(npc, player);
                return;
            }
            npc.openBaseMenu(player);
        } else {
            Service.gI().hideWaitDialog(player);
        }
    }

    public void doSelectMenu(Player player, int npcId, int select) throws IOException {
        TransactionService.gI().cancelTrade(player);

        if (player.idMark.getIndexMenu() == ConstNpc.MENU_NHAN_KEO) {
            player.lastTimeTrickOrTreat = System.currentTimeMillis();
            player.npcsTrickOrTreat.add(npcId);
            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                Item keo = ItemService.gI().createNewItem((short) ConstItem.KEO_BAN_TAY, 1);
                InventoryService.gI().addItemBag(player, keo);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bạn nhận được 1 Kẹo Bàn Tay!");
            } else {
                Service.gI().sendThongBao(player, "Hành trang đầy, không thể nhận kẹo!");
            }
            Npc npc = null;
            if (npcId == ConstNpc.CALICK && player.zone != null && player.zone.map.mapId != 102) {
                npc = NpcManager.getNpc(ConstNpc.CALICK);
            } else if (npcId == ConstNpc.LY_TIEU_NUONG) {
                npc = NpcManager.getNpc(ConstNpc.LY_TIEU_NUONG);
            } else if (player.zone != null) {
                npc = player.zone.map.getNpc(player, npcId);
            }
            if (npc != null) {
                npc.openBaseMenu(player);
            }
            return;
        }

        if (player.idMark.getIndexMenu() == ConstNpc.MENU_BI_GHEO) {
            player.lastTimeTrickOrTreat = System.currentTimeMillis();
            player.npcsTrickOrTreat.add(npcId);
            int iconXuong = player.gender == 0 ? 5101 : player.gender == 1 ? 5105 : 5103;
            player.itemTime.isXuong = true;
            player.itemTime.iconXuong = iconXuong;
            player.itemTime.lastTimeXuong = System.currentTimeMillis();
            ItemTimeService.gI().sendItemTime(player, iconXuong, 1800);
            Service.gI().Send_Caitrang(player);
            Service.gI().sendThongBao(player, "Bạn bị ghẹo và biến thành Xương Khô trong 30 phút!");

            Npc npc = null;
            if (npcId == ConstNpc.CALICK && player.zone != null && player.zone.map.mapId != 102) {
                npc = NpcManager.getNpc(ConstNpc.CALICK);
            } else if (npcId == ConstNpc.LY_TIEU_NUONG) {
                npc = NpcManager.getNpc(ConstNpc.LY_TIEU_NUONG);
            } else if (player.zone != null) {
                npc = player.zone.map.getNpc(player, npcId);
            }
            if (npc != null) {
                npc.openBaseMenu(player);
            }
            return;
        }

        switch (npcId) {
            case ConstNpc.RONG_THIENG, ConstNpc.CON_MEO ->
                Objects.requireNonNull(NpcManager.getNpc((byte) npcId)).confirmMenu(player, select);
            default -> {
                Npc npc = null;
                if (npcId == ConstNpc.CALICK && player.zone != null && player.zone.map.mapId != 102) {
                    npc = NpcManager.getNpc(ConstNpc.CALICK);
                } else if (npcId == ConstNpc.LY_TIEU_NUONG) {
                    npc = NpcManager.getNpc(ConstNpc.LY_TIEU_NUONG);
                } else if (player.zone != null) {
                    npc = player.zone.map.getNpc(player, npcId);
                }
                if (npc != null) {
                    npc.confirmMenu(player, select);
                } else {
                    Service.gI().hideWaitDialog(player);
                }
            }
        }

    }
}

