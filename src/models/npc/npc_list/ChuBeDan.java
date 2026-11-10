package models.npc.npc_list;

import java.util.List;

import consts.ConstNpc;
import database.daos.DonatePackageDAO;
import database.daos.DonatePackageDAO.Pack;
import database.daos.DonatePackageDAO.PackItem;
import models.Template;
import models.item.Item;
import models.npc.Npc;
import static models.npc.NpcFactory.PLAYERID_OBJECT;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;


public class ChuBeDan extends Npc {

    private static final int MENU_CHON_GOI = ConstNpc.BASE_MENU;
    private static final int MENU_XAC_NHAN_MUA = 2392;

    public ChuBeDan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        List<Pack> packs = DonatePackageDAO.getActivePackages();
        if (packs.isEmpty()) {
            this.npcChat(player, "Hiện chưa có gói quà nào\nQuay lại sau nhé!");
            return;
        }
        java.util.Set<Integer> bought = DonatePackageDAO.getBoughtPackageIds(player.getSession().userId);
        String[] menus = new String[packs.size()];
        for (int i = 0; i < packs.size(); i++) {
            Pack p = packs.get(i);
            menus[i] = bought.contains(p.id)
                    ? p.name + "\n[Đã mua]"
                    : p.name + "\n[" + Util.format(p.costVnd) + " VNĐ]";
        }
        this.createOtherMenu(player, MENU_CHON_GOI,
                "Gói quà donate đây!\nMỗi gói chỉ mua được 1 lần duy nhất đó nha"
                + "\n|7|Số tiền của bạn còn: " + Util.format(player.getSession().sotien) + " VNĐ",
                menus, packs);
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        switch (player.idMark.getIndexMenu()) {
            case MENU_CHON_GOI -> {
                Object obj = PLAYERID_OBJECT.get(player.id);
                if (!(obj instanceof List<?> packs)) {
                    return;
                }
                if (select < 0 || select >= packs.size()) {
                    return;
                }
                Pack pack = (Pack) packs.get(select);
                StringBuilder say = new StringBuilder();
                say.append(pack.name).append("\nGiá: ").append(Util.format(pack.costVnd)).append(" VNĐ");
                say.append("\n|7|Số tiền của bạn còn: ").append(Util.format(player.getSession().sotien)).append(" VNĐ");
                if (pack.detail != null && !pack.detail.isEmpty()) {
                    say.append("\n").append(pack.detail);
                }
                say.append("\nGồm:");
                for (PackItem it : pack.items) {
                    Template.ItemTemplate temp = ItemService.gI().getTemplate(it.tempId);
                    say.append("\n- ").append(temp != null ? temp.name : ("Item " + it.tempId));
                    if (it.quantity > 1) {
                        say.append(" x").append(it.quantity);
                    }
                }
                if (DonatePackageDAO.hasBought(pack.id, player.getSession().userId)) {
                    say.append("\n|2|Bạn đã mua gói này rồi");
                    this.createOtherMenu(player, ConstNpc.IGNORE_MENU, say.toString(), "Đóng");
                } else {
                    this.createOtherMenu(player, MENU_XAC_NHAN_MUA, say.toString(),
                            new String[]{"Mua", "Từ chối"}, pack);
                }
            }
            case MENU_XAC_NHAN_MUA -> {
                if (select != 0) {
                    return;
                }
                Object obj = PLAYERID_OBJECT.get(player.id);
                if (!(obj instanceof Pack pack)) {
                    return;
                }
                buyPack(player, pack);
            }
        }
    }

    private void buyPack(Player player, Pack pack) {
        Pack current = DonatePackageDAO.getActivePackages().stream().filter(p -> p.id == pack.id).findFirst().orElse(null);
        if (!pack.sameContents(current)) {
            PLAYERID_OBJECT.remove(player.id);
            Service.gI().sendThongBao(player, "Gói donate đã thay đổi hoặc ngừng bán. Hãy mở lại NPC để xem giá và quà mới.");
            return;
        }
        int accountId = player.getSession().userId;
        if (pack.costVnd <= 0 || pack.costVnd > Integer.MAX_VALUE) {
            Service.gI().sendThongBao(player, "Gói này cấu hình giá chưa đúng, vui lòng báo admin");
            return;
        }
        if (player.getSession().sotien < pack.costVnd) {
            Service.gI().sendThongBao(player, "Bạn không đủ " + Util.format(pack.costVnd)
                    + " VNĐ, hãy nạp thêm nhé");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < pack.items.size()) {
            Service.gI().sendThongBao(player, "Cần " + pack.items.size() + " ô hành trang trống để nhận quà");
            return;
        }

        if (!DonatePackageDAO.markBought(pack.id, accountId)) {
            Service.gI().sendThongBao(player, "Mỗi gói chỉ mua được 1 lần, bạn mua gói này rồi");
            return;
        }
        if (!DonatePackageDAO.chargeVnd(player, pack.costVnd)) {
            DonatePackageDAO.unmarkBought(pack.id, accountId);
            Service.gI().sendThongBao(player, "Số dư không đủ hoặc có lỗi, vui lòng thử lại");
            return;
        }
        for (PackItem it : pack.items) {
            Item item = ItemService.gI().createNewItem((short) it.tempId, it.quantity);
            for (int[] op : it.options) {
                item.itemOptions.add(new Item.ItemOption(op[0], op[1]));
            }
            InventoryService.gI().addItemBag(player, item);
        }
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Mua thành công " + pack.name);
    }
}
