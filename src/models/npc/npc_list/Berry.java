package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */


import models.npc.Npc;
import models.player.Player;
import services.TaskService;

public class Berry extends Npc {

    public Berry(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                switch (mapId) {
                    default ->
                        super.openBaseMenu(player);
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.getIndexMenu() == 0) {
                switch (mapId) {
                }
            }
        }
    }
}
