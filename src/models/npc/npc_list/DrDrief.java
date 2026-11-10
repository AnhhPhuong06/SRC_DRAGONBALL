package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */


import utils.Functions;
import models.clan.Clan;
import models.clan.ClanMember;
import consts.ConstItem;
import consts.ConstNpc;
import consts.ConstPlayer;
import consts.ConstTask;
import java.util.ArrayList;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import server.Client;
import services.player.ClanService;
import services.player.InventoryService;
import services.map.NpcService;
import services.Service;
import services.TaskService;
import services.map.ChangeMapService;
import services.func.Input;
import utils.Util;

public class DrDrief extends Npc {

    public DrDrief(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            if (this.mapId == 84) {
                this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                        "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?",
                        pl.gender == ConstPlayer.TRAI_DAT ? "Đến\nTrái Đất" : pl.gender == ConstPlayer.NAMEC ? "Đến\nNamếc" : "Đến\nXayda");
            } else if (this.mapId == 153) {

                ArrayList<String> menu = new ArrayList<>();
                Clan clan = pl.clan;
                if (clan != null) {
                    if (clan.isLeader(pl)) {
                        menu.add("Chức năng\nbang hội");
                    }
                    menu.add("Nhiệm vụ Bang\n[" + pl.playerTask.clanTask.leftTask + "/" + ConstTask.MAX_CLAN_TASK + "]");
                }
                menu.add("Đảo Kame");
                menu.add("Từ chối");
                String[] menus = menu.toArray(String[]::new);

                this.createOtherMenu(pl, ConstNpc.BASE_MENU, "Tôi có thể giúp gì cho bang hội của bạn ?", menus);
            } else if (!TaskService.gI().checkDoneTaskTalkNpc(pl, this)) {
                if (pl.playerTask.taskMain.id == 7) {
                    NpcService.gI().createTutorial(pl, this.avartar, "Hãy lên đường cứu đứa bé nhà tôi\nChắc bây giờ nó đang sợ hãi lắm rồi");
                } else {
                    this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                            "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?",
                            "Đến\nNamếc", "Đến\nXayda", "Siêu thị");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 84) {
                ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 24, -1, -1);
            } else if (this.mapId == 153) {
                OUTER:
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU -> {
                        Clan clan = player.clan;
                        if (clan != null) {
                            if (clan.isLeader(player)) {
                                switch (select) {
                                    case 0 ->
                                        createOtherMenu(player, 1, "Tôi có thể giúp gì cho bang hội của bạn ?", "Đổi tên\ntên bang\nviết tắt", "Chọn ngẫu nhiên tên bang viết tắt", "Nâng cấp Bang hội", "Đóng");
                                    case 1 -> {
                                        if (player.playerTask.clanTask.template != null) {
                                            if (player.playerTask.clanTask.isDone()) {
                                                createOtherMenu(player, ConstNpc.MENU_CLAN_TASK, "Nhiệm vụ đã hoàn thành, hãy nhận " + ((player.playerTask.clanTask.level + 1) * 10) + " capsule bang", "Nhận\nthưởng", "Đóng");
                                                break;
                                            }
                                            createOtherMenu(player, ConstNpc.MENU_CLAN_TASK, "Nhiệm vụ hiện tại: " + player.playerTask.clanTask.getName() + ". Đã hạ được " + player.playerTask.clanTask.count, "OK", "Hủy bỏ\nNhiệm vụ\nnày");
                                        } else {
                                            TaskService.gI().changeClanTask(this, player, (byte) Util.nextInt(5));
                                        }
                                    }
                                    case 2 ->
                                        ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, -1);
                                    default -> {
                                    }
                                }
                            } else {
                                switch (select) {
                                    case 0 -> {
                                        if (player.playerTask.clanTask.template != null) {
                                            if (player.playerTask.clanTask.isDone()) {
                                                createOtherMenu(player, ConstNpc.MENU_CLAN_TASK, "Nhiệm vụ đã hoàn thành, hãy nhận " + ((player.playerTask.clanTask.level + 1) * 10) + " capsule bang", "Nhận\nthưởng", "Đóng");
                                                break;
                                            }
                                            createOtherMenu(player, ConstNpc.MENU_CLAN_TASK, "Nhiệm vụ hiện tại: " + player.playerTask.clanTask.getName() + ". Đã hạ được " + player.playerTask.clanTask.count, "OK", "Hủy bỏ\nNhiệm vụ\nnày");
                                        } else {
                                            TaskService.gI().changeClanTask(this, player, (byte) Util.nextInt(5));
                                        }
                                    }
                                    case 1 ->
                                        ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, -1);
                                    default -> {
                                    }
                                }
                            }
                        }
                    }
                    case 1 -> {
                        Clan clan;
                        switch (select) {
                            case 0 ->
                                Input.gI().createFormBangHoi(player);
                            case 1 -> {
                                clan = player.clan;
                                if (clan != null) {
                                    if (clan.isLeader(player)) {
                                        if (clan.canUpdateClan(player)) {
                                            String tenvt = Functions.generateRandomCharacters(Util.nextInt(2, 4));
                                            clan.name2 = tenvt;
                                            clan.update();
                                            Service.gI().sendThongBao(player, "[" + tenvt + "] OK");
                                        }
                                    }
                                }
                            }
                            case 2 -> {
                                clan = player.clan;
                                if (clan != null) {
                                    int level = clan.level;
                                    if (clan.isLeader(player)) {
                                        if (level >= 10) {
                                            String npcSay = "|7|Bang hội: " + clan.name.toUpperCase()
                                                    + "\n|3|Level: " + level + " (Đã đạt cấp tối đa)"
                                                    + "\n|-1|Capsule: " + clan.capsuleClan
                                                    + "\nThành viên: " + clan.members.size() + "/" + clan.maxMember
                                                    + "\nChỉ số: Tất cả thành viên tăng (" + (level * 5) + "% TNSM, " + (level * 2) + "% DAME,HP,KI)"
                                                    + "\n|2|Bang hội đã đạt cấp tối đa (Level 10)!";
                                            createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                                            return;
                                        }
                                        int costCapsule = level * 100;
                                        int costThoiVang = 1000 + (level - 1) * 500;
                                        String npcSay = "Cần " + Util.format(costThoiVang) + " Thỏi Vàng và " + Util.format(costCapsule) + " Capsule bang để nâng cấp bang hội lên cấp " + (level + 1);
                                        npcSay += "\n+1 tối đa số lượng thành viên";
                                        npcSay += "\n+Mỗi cấp tăng 5% TNSM (Hiện tại: +" + (level * 5) + "% TNSM)";
                                        npcSay += "\n+Mỗi cấp tăng 2% DAME, HP, KI (Hiện tại: +" + (level * 2) + "%)";
                                        if (level > 1) {
                                            npcSay += "\n+1 ô trống tối đa rương bang.";
                                        }
                                        npcSay += "\n+Mở bán bùa bang cấp " + (level + 1);
                                        createOtherMenu(player, ConstNpc.MENU_CLAN_UP, npcSay, "Đồng ý", "Từ chối");
                                    }
                                }
                            }
                            default -> {
                            }
                        }
                    }
                    case ConstNpc.MENU_CLAN_UP -> {
                        Clan clan = player.clan;
                        if (clan != null) {
                            if (clan.isLeader(player)) {
                                synchronized (clan) {
                                    if (clan.level >= 10) {
                                        Service.gI().sendThongBao(player, "Đang ở cấp độ cao nhất.");
                                        return;
                                    }
                                    int costCapsule = clan.level * 100;
                                    int costThoiVang = 1000 + (clan.level - 1) * 500;
                                    if (clan.capsuleClan < costCapsule) {
                                        Service.gI().sendThongBao(player, "Bang hội còn thiếu " + Util.format(costCapsule - clan.capsuleClan) + " Capsule bang để nâng cấp!");
                                        return;
                                    }
                                    Item thoiVang = InventoryService.gI().findItemBagByTemp(player, ConstItem.THOI_VANG);
                                    if (thoiVang == null || thoiVang.quantity < costThoiVang) {
                                        int have = thoiVang != null ? thoiVang.quantity : 0;
                                        Service.gI().sendThongBao(player, "Còn thiếu " + Util.format(costThoiVang - have) + " Thỏi Vàng");
                                        return;
                                    }
                                    clan.capsuleClan -= costCapsule;
                                    InventoryService.gI().subQuantityItemsBag(player, thoiVang, costThoiVang);
                                    InventoryService.gI().sendItemBags(player);
                                    clan.level++;
                                    clan.maxMember++;
                                    clan.update();
                                    clan.sendMyClanForAllMember();
                                    for (int i = clan.membersInGame.size() - 1; i >= 0; i--) {
                                        Service.gI().point(clan.membersInGame.get(i));
                                    }
                                    Service.gI().sendThongBao(player, "Chúc mừng bang hội của bạn đã lên cấp " + (clan.level) + "!\nTất cả thành viên nhận +" + (clan.level * 5) + "% TNSM và +" + (clan.level * 2) + "% DAME, HP, KI.");
                                }
                            }
                        }
                    }
                    case ConstNpc.MENU_CLAN_TASK -> {
                        if (player.playerTask.clanTask.template != null) {
                            switch (select) {
                                case 0 -> {
                                    if (player.playerTask.clanTask.isDone()) {
                                        TaskService.gI().payClanTask(player);
                                    }
                                }
                                case 1 -> {
                                    if (!player.playerTask.clanTask.isDone()) {
                                        createOtherMenu(player, ConstNpc.MENU_CLAN_TASK_REMOVE, "Bạn có chắc muốn hủy nhiệm vụ này?\nNếu hủy nhiệm vụ bạn sẽ mất 1 lượt nhiệm vụ trong ngày.", "Đồng ý", "Từ chối");
                                    }
                                }
                            }
                        }
                    }
                    case ConstNpc.MENU_CLAN_TASK_REMOVE -> {
                        if (player.playerTask.clanTask.template != null) {
                            if (select == 0 && !player.playerTask.clanTask.isDone()) {
                                TaskService.gI().removeClanTask(player);
                            }
                        }
                    }
                    default -> {
                    }
                }
            } else if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 ->
                        ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                    case 1 ->
                        ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                    case 2 ->
                        ChangeMapService.gI().changeMapBySpaceShip(player, 84, -1, -1);
                }
            }
        }
    }
}
