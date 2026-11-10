package models.npc.npc_list;


import consts.ConstNpc;
import lombok.extern.java.Log;
import models.item.Item;
import models.tournament.DeathOrAliveArena;
import managers.tournament.DeathOrAliveArenaManager;
import models.item.Item.ItemOption;
import services.tournament.DeathOrAliveArenaService;
import models.npc.Npc;
import models.npc.NpcFactory;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.player.InventoryService;
import services.ItemService;
import services.Service;
import services.map.ChangeMapService;
import services.CombineService;
import services.RewardService;
import services.ShopService;
import utils.Logger;
import utils.Util;

public class BaHatMit extends Npc {

    public BaHatMit(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    private void openVachNuiMenu(Player player) {
        createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi cần ta giúp gì?",
                "Bùa\nHỗ trợ", "Nâng cấp", "Sách\nTuyệt kỹ", "Sự kiện");
    }

    private void openBuaMenu(Player player) {
        createOtherMenu(player, ConstNpc.MENU_BHM_BUA, "Chọn chức năng bùa hỗ trợ.",
                "Mua bùa", player.luotNhanBuaMienPhi == 1 ? "Nhận bùa\nMiễn phí" : "Đã nhận\nBùa miễn phí",
                "Quay lại");
    }

    private void openNangCapMenu(Player player) {
        createOtherMenu(player, ConstNpc.MENU_BHM_NANG_CAP, "Chọn vật phẩm cần nâng cấp hoặc ghép.",
                "Nâng cấp\nTrang bị",
                InventoryService.gI().findItemBongTaiCap2(player) ? "Mở chỉ số\nPorata 2" : "Nâng cấp\nPorata 2",
                InventoryService.gI().findItemBongTaiCap3(player) ? "Mở chỉ số\nPorata 3" : "Nâng cấp\nPorata 3",
                "Làm phép\nNhập đá", "Nhập\nNgọc Rồng", "Quay lại");
    }

    private void receiveFreeCharm(Player player) {
        if (player.luotNhanBuaMienPhi != 1) {
            Service.gI().sendThongBao(player, "Hôm nay bạn đã nhận bùa miễn phí rồi!");
            return;
        }
        int itemId = Util.nextInt(213, 219);
        player.charms.addTimeCharms(itemId, 60);
        player.luotNhanBuaMienPhi = 0;
        Item charm = ItemService.gI().createNewItem((short) itemId);
        Service.gI().sendThongBao(player, "Bạn vừa nhận thưởng " + charm.getName());
        openBuaMenu(player);
    }

    private void exchangeSnowman(Player player, int itemId) {
        synchronized (player.inventory.itemsBag) {
            Item item = InventoryService.gI().findItemBag(player, itemId);
            if (item == null || item.quantity < 1) {
                Service.gI().sendThongBao(player, itemId == 1210
                        ? "Bạn chưa có Người tuyết để đổi quà." : "Bạn chưa có Người tuyết băng giá để đổi quà.");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
                Service.gI().sendThongBao(player, "Cần 1 ô hành trang trống để nhận quà.");
                return;
            }
            if (itemId == 1210) RewardService.gI().rewardNguoiTuyet(player);
            else RewardService.gI().rewardNguoiTuyetBangGia(player);
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
            InventoryService.gI().sendItemBags(player);
        }
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 5 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?",
                            "Chức năng\nPha lê", "Nâng cấp\nSKH", "Ép Ấn\nTrang Bị", "Đến\nĐịa Ngục", "Siêu hóa\nCải trang", "Phân rã\nĐồ thần linh", "Vượt\ngiới hạn", "Pháp sư\nhóa", "Tẩy\npháp sư");
                case 112 -> {
                    if (Util.isAfterMidnight(player.lastTimePKVoDaiSinhTu)) {
                        player.haveRewardVDST = false;
                        player.thoiVangVoDaiSinhTu = 0;
                    }
                    if (player.haveRewardVDST) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Đây là phần thưởng cho con.",
                                "1 ngọc bí\nbất kì", "1 bí ngô");
                        return;
                    }
                    if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().equals(player)) {
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Ngươi muốn hủy đăng ký thi đấu võ đài?",
                                    "Top 100", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                            return;
                        }
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Ngươi muốn đăng ký thi đấu võ đài?\nnhiều phần thưởng giá trị đang đợi ngươi đó",
                                "Top 100", "Bình chọn", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                        return;
                    }
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi muốn đăng ký thi đấu võ đài?\nnhiều phần thưởng giá trị đang đợi ngươi đó",
                            "Top 100", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                }
                case 155 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?",
                            "Nâng cấp\nChân Mệnh", "Cửa hàng\nĐá Hoàng Kim", "Từ chối");
                case 174 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?",
                            "Quay về", "Từ chối");
                case 181 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?",
                            "Quay về", "Từ chối");
                default -> openVachNuiMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE
                    && (player.combineNew.typeCombine == CombineService.PHAP_SU_HOA
                    || player.combineNew.typeCombine == CombineService.TAY_PHAP_SU)) {
                if (!models.combine.list.PhapSuHoa.checkLocation(player)) return;
                if (select == 0 || select == 1) CombineService.gI().startCombine(player, select == 1 ? 10 : 1);
                return;
            }
            if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE
                    && player.combineNew.typeCombine == CombineService.VUOT_GIOI_HAN_LINH_THU) {
                if (this.mapId != 5) {
                    Service.gI().sendThongBao(player, "Chỉ có thể vượt giới hạn pet tại Bà Hạt Mít đảo Kame.");
                    return;
                }
                if (select == 0 || select == 1) CombineService.gI().startCombine(player, select == 1 ? 10 : 1);
                return;
            }
            if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE
                    && player.combineNew.typeCombine == CombineService.SIEU_HOA_CAI_TRANG) {
                if (this.mapId != 5) {
                    Service.gI().sendThongBao(player, "Chỉ có thể siêu hóa cải trang tại Bà Hạt Mít đảo Kame.");
                    return;
                }
                if (select == 0 || select == 1) CombineService.gI().startCombine(player, select == 1 ? 10 : 1);
                return;
            }
            switch (this.mapId) {
                case 5 -> {
                    if (player.idMark.isBaseMenu()) {
                        switch (select) {
                            case 0 ->
                                createOtherMenu(player, ConstNpc.CHUC_NANG_SAO_PHA_LE,
                                        "Chọn Chức Năng Ngươi Muốn Thực Hiện",
                                        "Ép Sao\n Trang Bị",
                                        "Pha lê\nhóa\ntrang bị"
                                        // "Cường Hóa\nLỗ Sao\nPha Lê"
                                        );
                            case 1 ->
                                createOtherMenu(player, ConstNpc.CHUC_NANG_SKH,
                                        "Chọn Chức Năng Ngươi Muốn Thực Hiện",
                                        "Nâng SKH\nThường");
                            case 2 -> {
                                CombineService.gI().openTabCombine(player, CombineService.AN_TRANG_BI);
                            }
                            case 3 -> {
                                if (player.nPoint != null && player.nPoint.power < 80_000_000_000L) {
                                    Service.gI().sendThongBao(player, "Cần 80 tỷ sức mạnh trở lên mới vào được");
                                    return;
                                }
                                ChangeMapService.gI().changeMapBySpaceShip(player, 174, -1, 145);
                            }
                            case 4 -> CombineService.gI().openTabCombine(player, CombineService.SIEU_HOA_CAI_TRANG);
                            case 5 -> CombineService.gI().openTabCombine(player, CombineService.PHAN_RA_DO_THAN_LINH);
                            case 6 -> CombineService.gI().openTabCombine(player, CombineService.VUOT_GIOI_HAN_LINH_THU);
                            case 7 -> CombineService.gI().openTabCombine(player, CombineService.PHAP_SU_HOA);
                            case 8 -> CombineService.gI().openTabCombine(player, CombineService.TAY_PHAP_SU);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.CHUC_NANG_SKH) {
                        switch (select) {
                            case 0 ->
                                CombineService.gI().openTabCombine(player, CombineService.NANG_HUY_DIET_LEN_SKH);
//                            case 2 ->
//                                CombineService.gI().openTabCombine(player, CombineService.DOI_DO_THAN);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.CHUC_NANG_SAO_PHA_LE) {
                        switch (select) {
                            case 0 ->
                                CombineService.gI().openTabCombine(player, CombineService.EP_SAO_TRANG_BI);
                            case 1 ->
                                CombineService.gI().openTabCombine(player, CombineService.PHA_LE_HOA_TRANG_BI);
                            // case 2 ->
                            //     CombineService.gI().openTabCombine(player, CombineService.CUONG_HOA_LO_SAO);
//                            case 3 ->
//                                CombineService.gI().openTabCombine(player, CombineService.TAO_DA_HEMATITE);

                        }
//                    } else if (player.idMark.getIndexMenu() == ConstNpc.CHUC_NANG_CHUYEN_HOA) {
//                        switch (select) {
//                            case 0:
//                                CombineService.gI().openTabCombine(player, CombineService.CHUYEN_HOA_BANG_VANG);
//                            case 1:
//                                CombineService.gI().openTabCombine(player, CombineService.CHUYEN_HOA_BANG_NGOC);
//                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        switch (player.combineNew.typeCombine) {
                            case CombineService.AN_TRANG_BI, CombineService.NANGPET, CombineService.NANG_TRUNG_DE_TU, CombineService.EP_SAO_TRANG_BI, CombineService.EP_DA_TANG_CHI_SO, CombineService.PHA_LE_HOA_TRANG_BI, CombineService.NANG_CAP_SAO_PHA_LE, CombineService.DANH_BONG_SAO_PHA_LE, CombineService.CUONG_HOA_LO_SAO, CombineService.TAO_DA_HEMATITE, CombineService.DOI_DO_THAN -> {
                                switch (select) {
                                    case 0 ->
                                        CombineService.gI().startCombine(player, 100);
                                    case 1 ->
                                        CombineService.gI().startCombine(player, 10);
                                    case 2 ->
                                        CombineService.gI().startCombine(player, 1);
                                    default -> {
                                    }
                                }
                            }
                            case CombineService.NANG_HUY_DIET_LEN_SKH, CombineService.PHAN_RA_DO_THAN_LINH -> {
                                if (select == 0) {
                                    CombineService.gI().startCombine(player);
                                }
                            }

                        }
                    }
                }
                case 112 -> {
                    if (player.idMark.isBaseMenu()) {
                        if (player.haveRewardVDST) {
                            switch (select) {
                                case 0 -> {
                                    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                        Item item = ItemService.gI().createNewItem((short) (Util.nextInt(19, 20)));
                                        item.itemOptions.add(new Item.ItemOption(93, 1));
                                        InventoryService.gI().addItemBag(player, item);
                                        InventoryService.gI().sendItemBags(player);
                                        Service.gI().sendThongBao(player, "Bạn nhận được " + item.template.name);
                                        player.haveRewardVDST = false;
                                    } else {
                                        Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống, không thể nhặt thêm");
                                    }
                                }
                                case 1 -> {
                                    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                        Item item = ItemService.gI().createNewItem((short) 226);
                                        item.itemOptions.add(new Item.ItemOption(93, 1));
                                        InventoryService.gI().addItemBag(player, item);
                                        InventoryService.gI().sendItemBags(player);
                                        Service.gI().sendThongBao(player, "Bạn nhận được " + item.template.name);
                                        player.haveRewardVDST = false;
                                    } else {
                                        Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống, không thể nhặt thêm");
                                    }
                                }
                            }
                            return;
                        }
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                            if (DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().equals(player)) {
                                switch (select) {
                                    case 0 -> {
                                    }
                                    case 1 ->
                                        this.npcChat(player, "Không thể thực hiện");
                                    case 2 -> {
                                    }
                                    case 3 ->
                                        ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                                }
                                return;
                            }
                            switch (select) {
                                case 0 -> {
                                }
                                case 1 ->
                                    this.createOtherMenu(player, ConstNpc.DAT_CUOC_HAT_MIT,
                                            "Phí bình chọn là 1 triệu vàng\nkhi trận đấu kết thúc\n90% tổng tiền bình chọn sẽ chia đều cho phe bình chọn chính xác",
                                            "Bình chọn cho " + DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().name + " (" + DeathOrAliveArenaManager.gI().getVDST(player.zone).getCuocPlayer() + ")",
                                            "Bình chọn cho hạt mít (" + DeathOrAliveArenaManager.gI().getVDST(player.zone).getCuocBaHatMit() + ")");
                                case 2 ->
                                    DeathOrAliveArenaService.gI().startChallenge(player);
                                case 3 -> {
                                }
                                case 4 ->
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                            }
                            return;
                        }
                        switch (select) {
                            case 0 -> {
                            }
                            case 1 ->
                                DeathOrAliveArenaService.gI().startChallenge(player);
                            case 2 -> {
                            }
                            case 3 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DAT_CUOC_HAT_MIT) {
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                            switch (select) {
                                case 0 -> {
                                    if (player.inventory.gold >= 1_000_000) {
                                        DeathOrAliveArena vdst = DeathOrAliveArenaManager.gI().getVDST(player.zone);
                                        vdst.setCuocPlayer(vdst.getCuocPlayer() + 1);
                                        vdst.addBinhChon(player);
                                        player.binhChonPlayer++;
                                        player.zoneBinhChon = player.zone;
                                        player.inventory.gold -= 1_000_000;
                                        Service.gI().sendMoney(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(1_000_000 - player.inventory.gold) + " vàng nữa");
                                    }
                                }
                                case 1 -> {
                                    if (player.inventory.gold >= 1_000_000) {
                                        DeathOrAliveArena vdst = DeathOrAliveArenaManager.gI().getVDST(player.zone);
                                        vdst.setCuocBaHatMit(vdst.getCuocBaHatMit() + 1);
                                        vdst.addBinhChon(player);
                                        player.binhChonHatMit++;
                                        player.zoneBinhChon = player.zone;
                                        player.inventory.gold -= 1_000_000;
                                        Service.gI().sendMoney(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(1_000_000 - player.inventory.gold) + " vàng nữa");
                                    }
                                }
                            }
                        }
                    }
                }
                case 155 -> {
                    if (player.idMark.isBaseMenu()) {
                        switch (select) {
                            case 0 ->
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_CHAN_MENH);
                            case 1 ->
                                ShopService.gI().opendShop(player, "SHOP_DA_HOANG_KIM", true);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        if (player.combineNew.typeCombine == CombineService.NANG_CAP_CHAN_MENH) {
                            if (select == 0) {
                                CombineService.gI().startCombine(player);
                            }
                        }
                    }
                }
                case 174 -> {
                    if (player.idMark.isBaseMenu()) {
                        switch (select) {
                            case 0 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    }
                }
                case 181 -> {
                    if (player.idMark.isBaseMenu()) {
                        switch (select) {
                            case 0 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    }
                }
                case 42, 43, 44, 84 -> {
                    if (player.idMark.isBaseMenu()) {
                        switch (select) {
                            case 0 -> openBuaMenu(player);
                            case 1 -> openNangCapMenu(player);
                            case 2 -> createOtherMenu(player, ConstNpc.SACH_TUYET_KY,
                                    "Chọn chức năng sách tuyệt kỹ.",
                                    "Đóng\nSách cũ", "Đổi\nSách tuyệt kỹ", "Giám định\nSách", "Tẩy\nSách",
                                    "Nâng cấp\nSách", "Hồi phục\nSách", "Phân rã\nSách", "Quay lại");
                            case 3 -> createOtherMenu(player, ConstNpc.MENU_BHM_SU_KIEN,
                                    "Mang Người tuyết hoặc Người tuyết băng giá đến để đổi quà.",
                                    "Đổi quà\nNgười tuyết", "Người tuyết\nBăng giá", "Quay lại");
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_BHM_BUA) {
                        switch (select) {
                            case 0 -> createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                    "Chọn thời hạn bùa muốn mua.",
                                    "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng", "Quay lại");
                            case 1 -> receiveFreeCharm(player);
                            case 2 -> openVachNuiMenu(player);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_BHM_NANG_CAP) {
                        switch (select) {
                            case 0 -> CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_VAT_PHAM);
                            case 1 -> CombineService.gI().openTabCombine(player,
                                    InventoryService.gI().findItemBongTaiCap2(player)
                                            ? CombineService.NANG_CHI_SO_BONG_TAI : CombineService.NANG_CAP_BONG_TAI);
                            case 2 -> CombineService.gI().openTabCombine(player,
                                    InventoryService.gI().findItemBongTaiCap3(player)
                                            ? CombineService.NANG_CHI_SO_BONG_TAI_CAP3 : CombineService.NANG_CAP_BONG_TAI_CAP3);
                            case 3 -> CombineService.gI().openTabCombine(player, CombineService.LAM_PHEP_NHAP_DA);
                            case 4 -> CombineService.gI().openTabCombine(player, CombineService.NHAP_NGOC_RONG);
                            case 5 -> openVachNuiMenu(player);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_BHM_SU_KIEN) {
                        switch (select) {
                            case 0 -> exchangeSnowman(player, 1210);
                            case 1 -> exchangeSnowman(player, 1211);
                            case 2 -> openVachNuiMenu(player);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.SACH_TUYET_KY) {
                        switch (select) {
                            case 7:
                                openVachNuiMenu(player);
                                break;
                            case 0:
                                Item trangSachCu = InventoryService.gI().findItemBagByTemp(player, 1188);

                                Item biaSach = InventoryService.gI().findItemBagByTemp(player, 1154);
                                if ((trangSachCu != null && trangSachCu.quantity >= 9999) && (biaSach != null && biaSach.quantity >= 1)) {
                                    createOtherMenu(player, ConstNpc.DONG_THANH_SACH_CU,
                                            "|2|Chế tạo Cuốn sách cũ\n"
                                            + "|1|Trang sách cũ " + trangSachCu.quantity + "/9999\n"
                                            + "Bìa sách " + biaSach.quantity + "/1\n"
                                            + "|4|Tỉ lệ thành công: 20%\n"
                                            + "Thất bại mất 99 trang sách và 1 bìa sách", "Đồng ý", "Từ chối");
                                    break;
                                } else {
                                    String NpcSay = "|2|Chế tạo Cuốn sách cũ\n";
                                    if (trangSachCu == null) {
                                        NpcSay += "|7|Trang sách cũ " + "0/9999\n";
                                    } else {
                                        NpcSay += "|1|Trang sách cũ " + trangSachCu.quantity + "/9999\n";
                                    }
                                    if (biaSach == null) {
                                        NpcSay += "|7|Bìa sách " + "0/1\n";
                                    } else {
                                        NpcSay += "|1|Bìa sách " + biaSach.quantity + "/1\n";
                                    }

                                    NpcSay += "|7|Tỉ lệ thành công: 20%\n";
                                    NpcSay += "|7|Thất bại mất 99 trang sách và 1 bìa sách";
                                    createOtherMenu(player, ConstNpc.DONG_THANH_SACH_CU_2,
                                            NpcSay, "Từ chối");
                                    break;
                                }
                            case 1:
                                Item cuonSachCu = InventoryService.gI().findItemBagByTemp(player, 1187);
                                Item kimBam = InventoryService.gI().findItemBagByTemp(player, 1153);

                                if ((cuonSachCu != null && cuonSachCu.quantity >= 10) && (kimBam != null && kimBam.quantity >= 1)) {
                                    createOtherMenu(player, ConstNpc.DOI_SACH_TUYET_KY,
                                            "|2|Đổi sách tuyệt kỹ 1\n"
                                            + "|1|Cuốn sách cũ " + cuonSachCu.quantity + "/10\n"
                                            + "Kìm bấm giấy " + kimBam.quantity + "/1\n"
                                            + "|4|Tỉ lệ thành công: 20%\n", "Đồng ý", "Từ chối");
                                    break;
                                } else {
                                    String NpcSay = "|2|Đổi sách Tuyệt kỹ 1\n";
                                    if (cuonSachCu == null) {
                                        NpcSay += "|7|Cuốn sách cũ " + "0/10\n";
                                    } else {
                                        NpcSay += "|1|Cuốn sách cũ " + cuonSachCu.quantity + "/10\n";
                                    }
                                    if (kimBam == null) {
                                        NpcSay += "|7|Kìm bấm giấy " + "0/1\n";
                                    } else {
                                        NpcSay += "|1|Kìm bấm giấy " + kimBam.quantity + "/1\n";
                                    }
                                    NpcSay += "|7|Tỉ lệ thành công: 20%\n";
                                    createOtherMenu(player, ConstNpc.DOI_SACH_TUYET_KY_2,
                                            NpcSay, "Từ chối");
                                }
                                break;
                            case 2:// giám định sách
                                CombineService.gI().openTabCombine(player,
                                        CombineService.GIAM_DINH_SACH);
                                break;
                            case 3:// tẩy sách
                                CombineService.gI().openTabCombine(player,
                                        CombineService.TAY_SACH);
                                break;
                            case 4:// nâng cấp sách
                                CombineService.gI().openTabCombine(player,
                                        CombineService.NANG_CAP_SACH_TUYET_KY);
                                break;
                            case 5:// phục hồi sách
                                CombineService.gI().openTabCombine(player,
                                        CombineService.PHUC_HOI_SACH);
                                break;
                            case 6:// phân rã sách
                                CombineService.gI().openTabCombine(player,
                                        CombineService.PHAN_RA_SACH);
                                break;
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DONG_THANH_SACH_CU) {
                        switch (select) {
                            case 0:

                                Item trangSachCu = InventoryService.gI().findItemBagByTemp(player, 1188);
                                Item biaSach = InventoryService.gI().findItemBagByTemp(player, 1154);
                                Item cuonSachCu = ItemService.gI().createNewItem((short) 1187);
                                if (Util.isTrue(20, 100)) {
                                    cuonSachCu.itemOptions.add(new ItemOption(30, 0));
                                    try { // send effect susscess
                                        Message msg = new Message(-81);
                                        msg.writer().writeByte(0);
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeShort(tempId);
                                        player.sendMessage(msg);
                                        msg.cleanup();

                                        msg = new Message(-81);
                                        msg.writer().writeByte(1);
                                        msg.writer().writeByte(2);
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, trangSachCu));
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, biaSach));
                                        player.sendMessage(msg);
                                        msg.cleanup();

                                        msg = new Message(-81);
                                        msg.writer().writeByte(7);
                                        msg.writer().writeShort(cuonSachCu.template.iconID);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        player.sendMessage(msg);
                                        msg.cleanup();

                                    } catch (Exception e) {
                                        System.out.println("lỗi 1");
                                    }

                                    InventoryService.gI().addItemList(player.inventory.itemsBag, cuonSachCu);
                                    InventoryService.gI().subQuantityItemsBag(player, trangSachCu, 9999);
                                    InventoryService.gI().subQuantityItemsBag(player, biaSach, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    return;
                                } else {
                                    try { // send effect faile
                                        Message msg = new Message(-81);
                                        msg.writer().writeByte(0);
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeShort(tempId);
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                        msg = new Message(-81);
                                        msg.writer().writeByte(1);
                                        msg.writer().writeByte(2);
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, biaSach));
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, trangSachCu));
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                        msg = new Message(-81);
                                        msg.writer().writeByte(8);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                    } catch (Exception e) {
                                        System.out.println("lỗi 2");
                                    }
                                    InventoryService.gI().subQuantityItemsBag(player, trangSachCu, 99);
                                    InventoryService.gI().subQuantityItemsBag(player, biaSach, 1);
                                    InventoryService.gI().sendItemBags(player);
                                }
                                return;
                            case 1:
                                break;
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DOI_SACH_TUYET_KY) {
                        switch (select) {
                            case 0:
                                Item cuonSachCu = InventoryService.gI().findItemBagByTemp(player, 1187);
                                Item kimBam = InventoryService.gI().findItemBagByTemp(player, 1153);

                                short baseValue = 1183;
                                short genderModifier = (player.gender == 0) ? -2 : ((player.gender == 2) ? 2 : (short) 0);

                                Item sachTuyetKy = ItemService.gI().createNewItem((short) (baseValue + genderModifier));

                                if (Util.isTrue(20, 100)) {

                                    sachTuyetKy.itemOptions.add(new ItemOption(224, 0));
                                    sachTuyetKy.itemOptions.add(new ItemOption(21, 40));
                                    sachTuyetKy.itemOptions.add(new ItemOption(30, 0));
                                    sachTuyetKy.itemOptions.add(new ItemOption(87, 1));
                                    sachTuyetKy.itemOptions.add(new ItemOption(225, 5));
                                    sachTuyetKy.itemOptions.add(new ItemOption(226, 1000));
                                    try { // send effect susscess
                                        Message msg = new Message(-81);
                                        msg.writer().writeByte(0);
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeShort(tempId);
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                        msg = new Message(-81);
                                        msg.writer().writeByte(1);
                                        msg.writer().writeByte(2);
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, kimBam));
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, cuonSachCu));
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                        msg = new Message(-81);
                                        msg.writer().writeByte(7);
                                        msg.writer().writeShort(sachTuyetKy.template.iconID);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                    } catch (Exception e) {
                                        System.out.println("lỗi 4");
                                    }
                                    InventoryService.gI().addItemList(player.inventory.itemsBag, sachTuyetKy);
                                    InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, 10);
                                    InventoryService.gI().subQuantityItemsBag(player, kimBam, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    return;
                                } else {
                                    try { // send effect faile
                                        Message msg = new Message(-81);
                                        msg.writer().writeByte(0);
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeUTF("test");
                                        msg.writer().writeShort(tempId);
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                        msg = new Message(-81);
                                        msg.writer().writeByte(1);
                                        msg.writer().writeByte(2);
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, kimBam));
                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, cuonSachCu));
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                        msg = new Message(-81);
                                        msg.writer().writeByte(8);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        msg.writer().writeShort(-1);
                                        player.sendMessage(msg);
                                        msg.cleanup();
                                    } catch (Exception e) {
                                        Logger.logException(NpcFactory.class, e, "Lỗi sách");
                                    }
                                    InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, 5);
                                    InventoryService.gI().subQuantityItemsBag(player, kimBam, 1);
                                    InventoryService.gI().sendItemBags(player);
                                }
                                return;
                            case 1:
                                break;
                        }
                        //làm phép nhập đá
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_SHOP_BUA) {
                        switch (select) {
                            case 0 ->
                                ShopService.gI().opendShop(player, "BUA_1H", true);
                            case 1 ->
                                ShopService.gI().opendShop(player, "BUA_8H", true);
                            case 2 ->
                                ShopService.gI().opendShop(player, "BUA_1M", true);
                            case 3 -> openBuaMenu(player);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        switch (player.combineNew.typeCombine) {
                            case CombineService.NANG_CAP_BONG_TAI_CAP3, CombineService.NANG_CHI_SO_BONG_TAI_CAP3, CombineService.NANG_CAP_VAT_PHAM, CombineService.NANG_CAP_BONG_TAI, CombineService.NANG_CHI_SO_BONG_TAI, CombineService.LAM_PHEP_NHAP_DA, CombineService.NHAP_NGOC_RONG, CombineService.GIAM_DINH_SACH, CombineService.TAY_SACH, CombineService.NANG_CAP_SACH_TUYET_KY, CombineService.PHUC_HOI_SACH, CombineService.PHAN_RA_SACH -> {
                                switch (select) {
                                    case 0 ->
                                        CombineService.gI().startCombine(player);
                                    case 1 ->
                                        CombineService.gI().startCombineVip(player, 10);
                                    case 2 ->
                                        CombineService.gI().startCombineVip(player, 100);
                                    default -> {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
