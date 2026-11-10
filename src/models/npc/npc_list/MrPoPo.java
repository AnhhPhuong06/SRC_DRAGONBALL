package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */


import models.clan.Clan;
import models.clan.ClanMember;
import consts.ConstNpc;
import models.dungeon.DestronGas;
import services.dungeon.DestronGasService;
import models.npc.Npc;
import static models.npc.NpcFactory.PLAYERID_OBJECT;
import models.player.Player;
import services.map.NpcService;
import services.func.Input;
import utils.TimeUtil;

public class MrPoPo extends Npc {

    public MrPoPo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 0) {
                if (player.clan != null) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Thượng Đế vừa phát hiện ra 1 loại khí đang âm thầm\nhủy diệt mọi mầm sống trên Trái Đất,\nnó được gọi là Destron Gas.\nTa sẽ đưa các cậu đến nơi ấy, các cậu đã sẵn sàng chưa?",
                            "Thông tin\nChi tiết", "Top 100\nBang hội", "Thành tích\nBang", "OK", "Từ chối");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Thượng Đế vừa phát hiện ra 1 loại khí đang âm thầm\nhủy diệt mọi mầm sống trên Trái Đất,\nnó được gọi là Destron Gas.\nTa sẽ đưa các cậu đến nơi ấy, các cậu đã sẵn sàng chưa?",
                            "Thông tin\nChi tiết", "Top 100\nBang hội", "OK", "Từ chối");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 0) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0 ->
                            NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_KHI_GAS_HUY_DIET);
                        case 1 -> {
                        }
                        case 2 -> {
                        }
                        case 3 -> {
                            Clan clan = player.clan;
                            if (clan != null) {
                                ClanMember cm = clan.getClanMember((int) player.id);
                                if (cm != null) {
                                    if (player.clanMember.getNumDateFromJoinTimeToToday() < 1) {
                                        NpcService.gI().createTutorial(player, tempId, this.avartar,
                                                "Gia nhập bang hội trên 1 ngày mới được tham gia");
                                        return;
                                    }
                                    if (player.clan.KhiGasHuyDiet != null) {
                                        createOtherMenu(player, 2,
                                                "Bang hội của cậu đang tham gia Destron Gas cấp độ " + player.clan.KhiGasHuyDiet.level + "\n"
                                                + "cậu có muốn đi cùng họ không ? ("
                                                + TimeUtil.convertTimeNow(player.clan.KhiGasHuyDiet.getLastTimeOpen())
                                                + " trước)", "Đồng ý", "Từ chối");
                                        return;
                                    }
                                    if (!clan.isLeader(player)) {
                                        NpcService.gI().createTutorial(player, tempId, this.avartar, "Chức năng chỉ dành cho bang chủ");
                                        return;
                                    }
                                    if (clan.members.size() < DestronGas.N_PLAYER_CLAN) {
                                        NpcService.gI().createTutorial(player, tempId, this.avartar,
                                                "Bang hội phải có ít nhất 5 thành viên mới có thể tham gia");
                                        return;
                                    }
                                    // Bỏ chọn level, mặc định 110
                                    DestronGasService.gI().openKhiGasHuyDiet(player, (byte) 110);
                                }
                            }
                        }
                    }
                } else if (player.idMark.getIndexMenu() == 2) {
                    if (select == 0) {
                        if (player.clan.KhiGasHuyDiet == null) {
                            DestronGasService.gI().openKhiGasHuyDiet(player, (byte) 110);
                        } else {
                            DestronGasService.gI().openKhiGasHuyDiet(player, (byte) 0);
                        }
                    }
                }
            }
        }
    }
}
