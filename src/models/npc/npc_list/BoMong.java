package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import consts.ConstTask;
import services.AchievementService;
import models.npc.Npc;
import models.player.Player;
import services.TaskService;
import services.map.NpcService;

public class BoMong extends Npc {

    public BoMong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (this.mapId == 47 || this.mapId == 84) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi muốn vip, có nhiều cách, nạp thẻ là nhanh nhất, còn không thì chịu khó cày hãy nghe lời thầy dạy cần cù bù siêng năng.", "Nhiệm vụ\nhàng ngày\n(Tạm đóng)", "Nhiệm vụ\nthành tích", "Từ chối");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 47 || this.mapId == 84) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {
                            NpcService.gI().createTutorial(player, tempId, this.avartar,
                                    "Tính năng Nhiệm vụ hàng ngày tại Bò Mộng tạm thời đang đóng, vui lòng quay lại sau!");
                        }
                        case 1 -> {
                            AchievementService.gI().openAchievementUI(player);
                        }

                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK) {
                    NpcService.gI().createTutorial(player, tempId, this.avartar,
                            "Tính năng Nhiệm vụ hàng ngày tại Bò Mộng tạm thời đang đóng, vui lòng quay lại sau!");
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_PAY_SIDE_TASK) {
                    NpcService.gI().createTutorial(player, tempId, this.avartar,
                            "Tính năng Nhiệm vụ hàng ngày tại Bò Mộng tạm thời đang đóng, vui lòng quay lại sau!");
                }
            }
        }
    }
}
