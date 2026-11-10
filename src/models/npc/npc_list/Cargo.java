package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */


import consts.ConstNpc;
import models.npc.Npc;
import models.player.Player;
import services.map.NpcService;
import services.TaskService;
import services.map.ChangeMapService;

public class Cargo extends Npc {

    public Cargo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(pl, this)) {
                if (pl.playerTask.taskMain.id == 7) {
                    NpcService.gI().createTutorial(pl, this.avartar, "Hãy lên đường cứu đứa bé nhà tôi\nChắc bây giờ nó đang sợ hãi lắm rồi");
                } else {
                    this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                            "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?",
                            "Đến\nTrái Đất", "Đến\nXayda", "Siêu thị");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 ->
                        ChangeMapService.gI().changeMapBySpaceShip(player, 24, -1, -1);
                    case 1 ->
                        ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                    case 2 ->
                        ChangeMapService.gI().changeMapBySpaceShip(player, 84, -1, -1);
                }
            }
        }
    }
}
