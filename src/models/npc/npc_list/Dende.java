package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */


import consts.ConstNpc;
import consts.ConstPlayer;
import models.npc.Npc;
import models.player.Player;
import services.dungeon.NgocRongNamecService;
import services.map.NpcService;
import services.TaskService;
import services.func.SummonDragonNamek;
import services.ShopService;
import utils.TimeUtil;
import utils.Util;

public class Dende extends Npc {

    public Dende(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (player.idNRNM != -1) {
                    if (player.zone.map.mapId == 7) {
                        this.createOtherMenu(player, 1, "Ồ, ngọc rồng namếc, bạn thật là may mắn\nnếu tìm đủ 7 viên sẽ được Rồng Thiêng Namếc ban cho điều ước", "Hướng\ndẫn\nGọi Rồng", "Gọi rồng", "Từ chối");
                    }
                } else {
                    if (player.gender != 1) {
                        NpcService.gI().createTutorial(player, tempId, this.avartar, "Xin lỗi anh, em chỉ bán đồ cho dân tộc Namếc");
                    } else if (!player.inventory.itemsDaBan.isEmpty()) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Anh cần trang bị gì cứ đến chỗ em nhé", "Cửa\nhàng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Anh cần trang bị gì cứ đến chỗ em nhé", "Cửa\nhàng");
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        //Shop
                        if (player.gender == ConstPlayer.NAMEC) {
                            ShopService.gI().opendShop(player, "DENDE", true);
                        } else {
                            this.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Xin lỗi anh, em chỉ bán đồ cho dân tộc Namếc", "Đóng");
                        }
                    }
//                    case 1 -> {
//                        if (!player.inventory.itemsDaBan.isEmpty()) {
//                            ShopService.gI().opendShop(player, "ITEMS_DABAN", true);
//                        }
//                    }
                }
            } else if (player.idMark.getIndexMenu() == 1) {
                switch (select) {
                    case 0 ->
                        NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_NRNM);
                    case 1 -> {
                        // Tắt tính năng ước ngọc rồng namek
                        NpcService.gI().createTutorial(player, tempId, this.avartar,
                                "Rồng Thiêng đang ngủ, tính năng gọi Rồng tạm thời đóng. Mong các anh thông cảm.");
                    }
                }
            }
        }
    }
}
