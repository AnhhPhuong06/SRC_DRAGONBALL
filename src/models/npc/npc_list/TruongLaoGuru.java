package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */


import models.clan.Clan;
import consts.ConstNpc;
import consts.ConstPlayer;
import java.util.ArrayList;
import models.npc.Npc;
import models.player.Player;
import services.map.NpcService;
import services.RewardService;
import services.Service;
import services.TaskService;
import services.map.ChangeMapService;
import services.func.Input;
import utils.Util;

public class TruongLaoGuru extends Npc {

    public TruongLaoGuru(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (player.gender != ConstPlayer.NAMEC) {
                    NpcService.gI().createTutorial(player, tempId, avartar, "Con hãy về hành tinh của mình mà thể hiện");
                    return;
                }
                ArrayList<String> menu = new ArrayList<>();
                if (!player.canReward) {
                    menu.add("Nhiệm vụ");
                    menu.add("Học\nKỹ năng");
                    Clan clan = player.clan;
                    if (clan != null) {
                        menu.add("Về khu\nvực bang");
                        if (clan.isLeader(player)) {
                            menu.add("Giải tán\nBang hội");
                        }
                    }
                } else {
                    menu.add("Giao\nTuần Lộc");
                }
                String[] menus = menu.toArray(String[]::new);
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chào con, ta rất vui khi gặp được con\nCon muốn làm gì nào ?", menus);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.canReward) {
                RewardService.gI().rewardLancon(player);
                return;
            }
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 ->
                        NpcService.gI().createTutorial(player, tempId, avartar, player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                    case 1 ->
                        Service.gI().sendThongBao(player, "Tới Uron mua sách học kĩ năng đi bạn yêu , tiềm năng để mà nâng chỉ số");
                    case 2 -> {
                        Clan clan = player.clan;
                        if (clan != null) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 153, Util.nextInt(100, 200), 432);
                        }
                    }
                    case 3 -> {
                        Clan clan = player.clan;
                        if (clan != null) {
                            if (clan.isLeader(player)) {
                                createOtherMenu(player, 3, "Con có chắc muốn giải tán bang hội không?", "Đồng ý", "Từ chối");
                            }
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == 3) {
                Clan clan = player.clan;
                if (clan != null) {
                    if (clan.isLeader(player)) {
                        if (select == 0) {
                            Input.gI().createFormGiaiTanBangHoi(player);
                        }
                    }
                }
            }
        }
    }
}
