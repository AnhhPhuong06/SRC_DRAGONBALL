package models.npc.npc_list;

import consts.ConstNpc;
import models.combine.list.NangCapZeno;
import models.npc.Npc;
import models.player.Player;
import services.CombineService;

/**
 * NPC 111 "Zeno và cận vệ" - mở tab đập để nâng cấp và tẩy Pet Cận vệ Zeno (1976),
 * Cải trang Zeno (1967). Toàn bộ luật nằm ở {@link models.combine.list.NangCapZeno}.
 */
public class ZenoNpc extends Npc {

    public static final int NPC_ID = 111;

    private static final int MENU_CHON = 11110;

    public ZenoNpc(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        createOtherMenu(player, MENU_CHON,
                "Ta có thể rót sức mạnh của Zeno vào pet và cải trang của ngươi.\n"
                + "Bỏ Pet Cận vệ Zeno hoặc Cải trang Zeno cùng 200 thỏi vàng mỗi món vào ô.\n"
                + "Mỗi lần nâng được 1 cấp, tối đa " + NangCapZeno.CAP_TOI_DA + " cấp. "
                + "Tỉ lệ thành công mỗi cấp " + NangCapZeno.TI_LE_THANH_CONG
                + "%, thành công thì cộng thêm một dòng ngẫu nhiên trong "
                + "Sức đánh, HP, KI, SĐCM với mức +1% đến +10%.\n"
                + "Nếu chưa vừa ý, đưa ta 1 Ngôi sao và 200 thỏi vàng, "
                + "ta sẽ tẩy sạch để ngươi nâng cấp lại từ đầu.",
                "Nâng cấp", "Tẩy chỉ số\nđã nâng", "Đóng");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        int menu = player.idMark.getIndexMenu();
        if (menu == MENU_CHON) {
            switch (select) {
                case 0 ->
                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_ZENO);
                case 1 ->
                    CombineService.gI().openTabCombine(player, CombineService.TAY_ZENO);
            }
        } else if (menu == ConstNpc.MENU_START_COMBINE && select == 0 && player.combineNew != null
                && (player.combineNew.typeCombine == CombineService.NANG_CAP_ZENO
                || player.combineNew.typeCombine == CombineService.TAY_ZENO)) {
            CombineService.gI().startCombine(player);
        }
    }
}
