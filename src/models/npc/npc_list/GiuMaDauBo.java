package models.npc.npc_list;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import models.npc.Npc;
import models.player.Player;
import services.Service;
import services.map.ChangeMapService;

public class GiuMaDauBo extends Npc {

    public GiuMaDauBo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi đang muốn tìm mảnh vỡ và mảnh hồn bông tai Porata trong truyền thuyết, ta sẽ đưa ngươi đến đó ?",
                    "Vào map\nBang Hội", "Về map\nĐảo Kame", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        if (player.nPoint == null || player.nPoint.power < 40_000_000_000L) {
                            Service.gI().sendThongBao(player, "Cần 40 tỷ sức mạnh mới vào được map này");
                            return;
                        }
                        this.createOtherMenu(player, 1, "Ngươi muốn vào khu vực nào để tìm nguyên liệu nâng cấp bông tai Porata?",
                                "Up bông tai\ncấp 2", "Up bông tai\ncấp 3", "Đóng");
                    }
                    case 1 -> {
                        ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1055);
                    }
                }
            } else if (player.idMark.getIndexMenu() == 1) {
                switch (select) {
                    case 0 -> {
                        ChangeMapService.gI().changeMapBySpaceShip(player, 156, -1, 816);
                    }
                    case 1 -> {
                        ChangeMapService.gI().changeMapBySpaceShip(player, 158, -1, 528);
                    }
                }
            }
        }
    }
}
