package models.npc.npc_list;


import consts.ConstNpc;
import models.npc.Npc;
import models.player.Player;
import services.Service;
import services.top.BossHuntRanking;

import services.top.TopManager;
import services.top.TopService;

public class DaiThienSu extends Npc {

    public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (TopManager.SELECT == null || TopManager.SELECT.length == 0) {
                TopManager.setSelectNpc();
            }
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|2|Người mún gì ở em?",
                    "Bảng\nXếp Hạng", "Top\nSăn Boss");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.idMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU -> {
                    switch (select) {
                        case 0 -> {
                            if (TopManager.SELECT == null || TopManager.SELECT.length == 0) {
                                TopManager.setSelectNpc();
                            }
                            if (TopManager.SELECT != null && TopManager.SELECT.length > 0) {
                                createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|BXH Đua Top Ngọc Rồng Online.", TopManager.SELECT);
                            } else {
                                Service.gI().sendThongBao(player, "Bảng xếp hạng đang được cập nhật!");
                            }
                        }
                        case 1 -> showBossHuntRanking(player);
                    }
                }
                case ConstNpc.IGNORE_MENU -> {
                    TopManager.TopTemplate temp = null;
                    if (select >= 0 && select < TopManager.TOP_TEMPLATE.size()) {
                        temp = TopManager.TOP_TEMPLATE.get(select);
                    } else {
                        temp = TopService.gI().getTop(select);
                    }
                    if (temp != null) {
                        if (player != null && !player.isBot) {
                            try {
                                database.daos.PlayerDAO.updatePlayer(player);
                            } catch (Exception ignored) {
                            }
                        }
                        TopService.gI().loadTop(temp);
                        Service.gI().showListTop(player, temp);
                    } else {
                        Service.gI().sendThongBao(player, "Không tìm thấy bảng xếp hạng!");
                    }
                }
            }
        }
    }

    protected void showBossHuntRanking(Player player) {
        BossHuntRanking.show(player);
    }
}
