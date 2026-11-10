package models.npc.npc_list;

import consts.ConstNpc;
import models.npc.Npc;
import models.player.Player;

public class TrongTai extends Npc {

    public TrongTai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Chức năng NPC Trọng Tài hiện đang tắt.", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            // Reject confirmations from menus opened before this feature was disabled.
            player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        }
    }
}
