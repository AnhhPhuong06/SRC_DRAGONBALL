package models.npc.npc_list;


import models.clan.Clan;
import consts.ConstItem;
import consts.ConstNpc;
import event.EventManager;
import models.item.Item;
import java.util.ArrayList;
import models.clan.ClanMember;
import models.dungeon.TreasureUnderSea;
import services.dungeon.TreasureUnderSeaService;
import models.npc.Npc;
import static models.npc.NpcFactory.PLAYERID_OBJECT;
import models.player.Player;
import models.task.Task_Kol;
import services.ItemService;
import services.player.InventoryService;
import services.map.NpcService;
import services.RewardService;
import services.Service;
import services.ShopService;
import services.TaskService;
import services.map.ChangeMapService;
import services.func.Input;
import services.top.TopService;
import utils.Util;

public class QuyLaoKame extends Npc {

    public QuyLaoKame(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            ArrayList<String> menu = new ArrayList<>();
            if (!player.canReward) {
                menu.add("Nói\nchuyện");
                menu.add("Nhận quà\nKOL");
                if (EventManager.HALLOWEEN) menu.add("Đổi quà\nSự kiện");
            } else {
                menu.add("Giao\n Bé na");
            }
            String[] menus = menu.toArray(String[]::new);
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Con muốn hỏi gì nào?", menus);
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
            switch (player.idMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU -> {
                    switch (select) {
                        case 0 -> {
                            ArrayList<String> menu = new ArrayList<>();
                            menu.add("Nhiệm vụ");
                            menu.add("Học\nKỹ năng");
                            menu.add("Chức năng\nBang hội");
                            String[] menus = menu.toArray(String[]::new);

                            this.createOtherMenu(player, 0,
                                    "Chào con, ta rất vui khi gặp con\nCon muốn làm gì nào ?", menus);
                        }
                        case 1 -> {
                            Task_Kol.checkThresholds(player); // mốc đạt-ngưỡng tự hoàn thành nếu đã vượt từ trước
                            Task_Kol taskKol = player.playerTask.task_kol;
                            String kolProgress = taskKol.getLevelDescriptionBase(taskKol.level, false) + "\n Hoàn thành " + taskKol.count + "/" + taskKol.maxCount + " (" + taskKol.getPercentProcess() + "%)";
                            if (taskKol.isDone()) {
                                createOtherMenu(player, 2191, kolProgress, "Nhận quà", "Đóng");
                            } else {
                                NpcService.gI().createMenuConMeo(player, -1, -1, kolProgress, "Đóng");
                            }
                        }
                        case 2 -> {
                            if (!EventManager.HALLOWEEN) return;
                            this.createOtherMenu(player, ConstNpc.DOI_QUA_SU_KIEN_BI_NGO,
                                    "|2|ĐỔI QUÀ SỰ KIỆN HALLOWEEN\n"
                                    + "|7|Thu thập Bí Ngô và Kẹo Bàn Tay từ đánh quái hoặc săn Boss để đổi các phần quà giá trị:\n"
                                    + "|1|• 99 Bí Ngô: Cải trang Bill Bí Ngô (15% Vĩnh viễn / HSD)\n"
                                    + "|1|• 50 Bí Ngô: Giỏ Kẹo Halloween\n"
                                    + "|1|• 99 Kẹo Bàn Tay: Làm 1 Giỏ Kẹo Halloween\n"
                                    + "|1|• 30 Bí Ngô: 5 Thỏi Vàng\n"
                                    + "Con muốn đổi món quà nào?",
                                    "Bill Bí Ngô\n(99 Bí Ngô)",
                                    "Giỏ Kẹo\n(50 Bí Ngô)",
                                    "Làm Giỏ Kẹo\n(99 Kẹo)",
                                    "5 Thỏi Vàng\n(30 Bí Ngô)",
                                    "Từ chối");
                        }
                        
                    }
                }
                case 0 -> {
                    switch (select) {
                        case 0 ->
                            NpcService.gI().createTutorial(player, tempId, avartar, player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                        case 1 ->
                            ShopService.gI().opendShop(player, "HOC_KY_NANG", false);
                        case 2 ->
                            this.createOtherMenu(player, ConstNpc.CHUC_NANG_BANG_HOI, "Ta có hỗ trợ những chức năng Bang hội, nhà ngươi cần gì?",
                                    "Giải tán\nBang Hội",
                                    "Nâng cấp\nBang Hội",
                                    "Quyên góp\nCapsule",
                                    "Lãnh địa\nBang Hội",
                                    "Kho báu\nDưới biển",
                                    "Từ chối");
                    }
                }
                case ConstNpc.CHUC_NANG_BANG_HOI -> {
                    switch (select) {
                        case 0 -> {
                            Clan clan = player.clan;
                            if (clan != null) {
                                ClanMember cm = clan.getClanMember((int) player.id);
                                if (cm != null) {
                                    if (!clan.isLeader(player)) {
                                        Service.gI().sendThongBao(player, "Yêu cầu phải là bang chủ!");
                                        return;
                                    }
                                    if (clan.members.size() > 1) {
                                        Service.gI().sendThongBao(player, "Yêu cầu bang hội chỉ còn một thành viên!");
                                        return;
                                    }
                                    this.createOtherMenu(player, ConstNpc.CONFIRM_DISSOLUTION_CLAN, "Bạn có chắc chắn muốn giải tán bang hội?\n"
                                            + "( Yêu cầu sẽ không thể hoàn tác )",
                                            "Đồng ý", "Từ chối!");
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Yêu câu tham gia bang hội");
                            }
                        }
                        case 1 -> {
                            if (player.clan != null) {
                                if (player.clan.level >= 10) {
                                    this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                            "|7|Bang hội: " + player.clan.name.toUpperCase()
                                            + "\n|3|Level: " + player.clan.level + " (Đã đạt cấp tối đa)"
                                            + "\n|-1|Capsule: " + player.clan.capsuleClan
                                            + "\nThành viên: " + player.clan.members.size() + "/" + player.clan.maxMember
                                            + "\nChỉ số: Tất cả thành viên tăng (" + (player.clan.level * 5) + "% TNSM, " + (player.clan.level * 2) + "% DAME,HP,KI)"
                                            + "\n|2|Bang hội đã đạt cấp tối đa (Level 10)!",
                                            "Đóng");
                                    return;
                                }
                                int costCapsule = player.clan.level * 100;
                                int costThoiVang = 1000 + (player.clan.level - 1) * 500;
                                this.createOtherMenu(player, ConstNpc.NANG_CAP_BANG_HOI,
                                        "|7|Bang hội: " + player.clan.name.toUpperCase()
                                        + "\n|3|Level: " + player.clan.level
                                        + "\n|-1|Capsule: " + player.clan.capsuleClan
                                        + "\nThành viên: " + player.clan.members.size() + "/" + player.clan.maxMember
                                        + "\nChỉ số: Tất cả thành viên tăng (" + (player.clan.level * 5) + "% TNSM, " + (player.clan.level * 2) + "% DAME,HP,KI)"
                                        + "\n|2|Nâng cấp sẽ mở thêm sức chứa và tăng chỉ số cho toàn bộ thành viên"
                                        + "\n|5|Cần " + Util.format(costThoiVang) + " Thỏi Vàng và " + Util.format(costCapsule) + " Capsule bang để nâng cấp bang hội lên cấp " + (player.clan.level + 1),
                                        "Nâng cấp", "Từ chối");
                            } else {
                                Service.gI().sendThongBao(player, "Yêu cầu tham gia bang hội");
                            }
                        }
                        case 2 -> {
                            if (player.clan != null) {
                                Input.gI().createFormDonateCapsuleBang(player);
                            } else {
                                Service.gI().sendThongBao(player, "Yêu câu tham gia bang hội");
                            }
                        }
                        case 3 -> {
                            if (player.clan != null) {
                                if (player.getSession().player.nPoint.power >= 1_500_000L) {
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 153, -1, 432);
                                } else {
                                    this.npcChat(player, "Bạn chưa đủ 1tr5 sức mạnh để vào");
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Yêu câu tham gia bang hội");
                            }
                        }
                        case 4 -> {
                            if (player.clan != null && player.clan.BanDoKhoBau != null) {
                                this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                                        "Bang hội con đang ở hang kho báu cấp "
                                        + player.clan.BanDoKhoBau.level + "\ncon có muốn đi cùng họ không?",
                                        "Đồng ý", "Từ chối");
                            } else {
                                this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                                        "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\nỞ đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                                        "Chọn\ncấp độ", "Từ chối");
                            }
                        }
                    }
                }
                case ConstNpc.NANG_CAP_BANG_HOI -> {
                    switch (select) {
                        case 0 -> {
                            if (player.clan == null) {
                                Service.gI().sendThongBao(player, "Yêu cầu tham gia bang hội!");
                                return;
                            }
                            if (!player.clan.isLeader(player)) {
                                Service.gI().sendThongBao(player, "Chức năng chỉ dành cho bang chủ!");
                                return;
                            }
                            synchronized (player.clan) {
                                if (player.clan.level >= 10) {
                                    Service.gI().sendThongBaoOK(player, "Bang hội đã đạt cấp cao nhất (Level 10)!");
                                    return;
                                }
                                int costCapsule = player.clan.level * 100;
                                int costThoiVang = 1000 + (player.clan.level - 1) * 500;
                                if (player.clan.capsuleClan < costCapsule) {
                                    Service.gI().sendThongBaoOK(player, "Bang hội còn thiếu " + Util.format(costCapsule - player.clan.capsuleClan) + " Capsule bang để nâng cấp!");
                                    return;
                                }
                                Item thoiVang = InventoryService.gI().findItemBagByTemp(player, ConstItem.THOI_VANG);
                                if (thoiVang == null || thoiVang.quantity < costThoiVang) {
                                    int have = thoiVang != null ? thoiVang.quantity : 0;
                                    Service.gI().sendThongBaoOK(player, "Còn thiếu " + Util.format(costThoiVang - have) + " Thỏi Vàng");
                                    return;
                                }
                                player.clan.capsuleClan -= costCapsule;
                                InventoryService.gI().subQuantityItemsBag(player, thoiVang, costThoiVang);
                                InventoryService.gI().sendItemBags(player);
                                player.clan.level += 1;
                                player.clan.maxMember += 1;
                                player.clan.update();
                                player.clan.sendMyClanForAllMember();
                                Service.gI().sendMoney(player);
                                for (int i = player.clan.membersInGame.size() - 1; i >= 0; i--) {
                                    Service.gI().point(player.clan.membersInGame.get(i));
                                }
                                Service.gI().sendThongBaoFromAdmin(player, "|7|Nâng cấp bang hội lên level " + player.clan.level + " thành công!\nTất cả thành viên nhận +" + (player.clan.level * 5) + "% TNSM và +" + (player.clan.level * 2) + "% DAME, HP, KI.");
                            }
                        }
                    }
                }
                case ConstNpc.CONFIRM_DISSOLUTION_CLAN -> {
                    if (select == 0) {
                        Input.gI().createFormGiaiTanBangHoi(player);
                    }
                }
                case 3 -> {
                    Clan clan = player.clan;
                    if (clan != null) {
                        if (clan.isLeader(player)) {
                            if (select == 0) {
                                Input.gI().createFormGiaiTanBangHoi(player);
                            }
                        }
                    }
                }
                case ConstNpc.ĐỔI_PHIẾU_BÉ_NGOAN -> {
                    switch (select) {
                        case 0: {
                            if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                                Service.gI().sendThongBao(player, "Cần 2 ô hành trang mới có thể đổi!!!");
                                return;
                            }
                            int soluong = 30;
                            Item phieubengoan = InventoryService.gI().findItemBag(player, 1395);
                            if (phieubengoan == null || phieubengoan.quantity < soluong) {
                                Service.gI().sendThongBao(player, "Cần ít nhất " + soluong + " Phiếu Bé Ngoan để đổi!!!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, phieubengoan, soluong);
                            Item meomun = ItemService.gI().createNewItem((short) 1260);
                            meomun.itemOptions.add(new Item.ItemOption(50, 10));
                            meomun.itemOptions.add(new Item.ItemOption(77, 10));
                            meomun.itemOptions.add(new Item.ItemOption(103, 10));
                            meomun.itemOptions.add(new Item.ItemOption(93, 15));
                            InventoryService.gI().addItemBag(player, meomun);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đổi thành công! Bạn đã nhận được vật phẩm.");
                            break;
                        }
                        case 1: {
                            if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                                Service.gI().sendThongBao(player, "Cần 2 ô hành trang mới có thể đổi!!!");
                                return;
                            }
                            int soluong = 50;
                            Item phieubengoan = InventoryService.gI().findItemBag(player, 1395);
                            if (phieubengoan == null || phieubengoan.quantity < soluong) {
                                Service.gI().sendThongBao(player, "Cần ít nhất " + soluong + " Phiếu Bé Ngoan để đổi!!!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, phieubengoan, soluong);
                            Item ximuoidoadao = ItemService.gI().createNewItem((short) 1261);
                            Item ximuoidoamai = ItemService.gI().createNewItem((short) 1262);
                            ximuoidoadao.itemOptions.add(new Item.ItemOption(174, 2025));
                            ximuoidoamai.itemOptions.add(new Item.ItemOption(174, 2025));
                            InventoryService.gI().addItemBag(player, ximuoidoadao);
                            InventoryService.gI().addItemBag(player, ximuoidoamai);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đổi thành công! Bạn đã nhận được vật phẩm.");
                            break;
                        }
                        case 2: {
                            if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                                Service.gI().sendThongBao(player, "Cần 2 ô hành trang mới có thể đổi!!!");
                                return;
                            }
                            int soluong = 70;
                            Item phieubengoan = InventoryService.gI().findItemBag(player, 1395);
                            if (phieubengoan == null || phieubengoan.quantity < soluong) {
                                Service.gI().sendThongBao(player, "Cần ít nhất " + soluong + " Phiếu Bé Ngoan để đổi!!!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, phieubengoan, soluong);
                            Item meomun = ItemService.gI().createNewItem((short) 1263);
                            meomun.itemOptions.add(new Item.ItemOption(50, 15));
                            meomun.itemOptions.add(new Item.ItemOption(77, 15));
                            meomun.itemOptions.add(new Item.ItemOption(103, 15));
                            meomun.itemOptions.add(new Item.ItemOption(5, 11));
                            meomun.itemOptions.add(new Item.ItemOption(93, 30));
                            InventoryService.gI().addItemBag(player, meomun);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đổi thành công! Bạn đã nhận được vật phẩm.");
                            break;
                        }
                        case 3: {
                            if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                                Service.gI().sendThongBao(player, "Cần 2 ô hành trang mới có thể đổi!!!");
                                return;
                            }
                            int soluong = 99;
                            Item phieubengoan = InventoryService.gI().findItemBag(player, 1395);
                            if (phieubengoan == null || phieubengoan.quantity < soluong) {
                                Service.gI().sendThongBao(player, "Cần ít nhất " + soluong + " Phiếu Bé Ngoan để đổi!!!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, phieubengoan, soluong);
                            Item bunma = ItemService.gI().createNewItem((short) 1264);
                            bunma.itemOptions.add(new Item.ItemOption(50, 33));
                            bunma.itemOptions.add(new Item.ItemOption(77, 31));
                            bunma.itemOptions.add(new Item.ItemOption(103, 31));
                            bunma.itemOptions.add(new Item.ItemOption(5, 20));
                            bunma.itemOptions.add(new Item.ItemOption(14, 20));
                            bunma.itemOptions.add(new Item.ItemOption(106, 0));
                            bunma.itemOptions.add(new Item.ItemOption(93, Util.nextInt(5, 15)));
                            InventoryService.gI().addItemBag(player, bunma);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đổi thành công! Bạn đã nhận được vật phẩm.");
                            break;
                        }
                    }

                }
                case ConstNpc.MENU_OPENED_DBKB -> {
                    switch (select) {
                        case 0 -> {
                            if (player.clan == null) {
                                Service.gI().sendThongBao(player, "Hãy vào bang hội trước");
                                return;
                            }
                            if (player.isAdmin() || player.nPoint.power >= TreasureUnderSea.POWER_CAN_GO_TO_DBKB) {
                                ChangeMapService.gI().goToDBKB(player);
                            } else {
                                this.npcChat(player, "Yêu cầu sức mạnh lớn hơn "
                                        + Util.numberToMoney(TreasureUnderSea.POWER_CAN_GO_TO_DBKB));
                            }
                        }

                    }
                }
                case ConstNpc.MENU_OPEN_DBKB -> {
                    switch (select) {
                        case 0 -> {
                            if (player.clan == null) {
                                Service.gI().sendThongBao(player, "Hãy vào bang hội trước");
                                return;
                            }
                            if (player.isAdmin() || player.nPoint.power >= TreasureUnderSea.POWER_CAN_GO_TO_DBKB) {
                                // Bỏ chọn level — mặc định vào phó bản kho báu cấp 110
                                TreasureUnderSeaService.gI().openBanDoKhoBau(player, (byte) 110);
                            } else {
                                this.npcChat(player, "Yêu cầu sức mạnh lớn hơn "
                                        + Util.numberToMoney(TreasureUnderSea.POWER_CAN_GO_TO_DBKB));
                            }
                        }
                    }
                }
                case ConstNpc.MENU_ACCEPT_GO_TO_BDKB -> {
                    switch (select) {
                        case 0 ->
                            TreasureUnderSeaService.gI().openBanDoKhoBau(player, Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id))));
                    }
                }

                case 2191 -> {
                    if (select == 0) {
                        Task_Kol.gI().NhanKOL(player);
                    }
                }
                case ConstNpc.DOI_QUA_SU_KIEN_BI_NGO -> {
                    if (!EventManager.HALLOWEEN) {
                        Service.gI().sendThongBao(player, "Sự kiện Halloween đã tắt.");
                        return;
                    }
                    switch (select) {
                        case 0 -> { // 99 Bí Ngô lấy Cải trang Bill Bí Ngô
                            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                                Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống!");
                                return;
                            }
                            Item biNgo = InventoryService.gI().findItemBagByTemp(player, ConstItem.BI_NGO);
                            int cost = 99;
                            if (biNgo == null || biNgo.quantity < cost) {
                                Service.gI().sendThongBao(player, "Con cần ít nhất " + cost + " quả Bí Ngô để đổi Cải trang Bill Bí Ngô!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, biNgo, cost);
                            Item ct = ItemService.gI().createNewItem((short) ConstItem.CAI_TRANG_BILL_BI_NGO, 1);
                            ct.itemOptions.add(new Item.ItemOption(50, Util.nextInt(35, 52))); // % sức đánh
                            ct.itemOptions.add(new Item.ItemOption(77, Util.nextInt(35, 52))); // % HP
                            ct.itemOptions.add(new Item.ItemOption(103, Util.nextInt(35, 52)));; // % KI
                            if (!Util.isTrue(15, 100)) { // 85% có HSD 7-15 ngày, 15% vĩnh viễn
                                ct.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 3)));
                            }
                            InventoryService.gI().addItemBag(player, ct);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đổi thành công! Con nhận được " + ct.template.name);
                        }
                        case 1 -> { // 50 Bí Ngô lấy Giỏ kẹo Halloween (1497)
                            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                                Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống!");
                                return;
                            }
                            Item biNgo = InventoryService.gI().findItemBagByTemp(player, ConstItem.BI_NGO);
                            int cost = 50;
                            if (biNgo == null || biNgo.quantity < cost) {
                                Service.gI().sendThongBao(player, "Con cần ít nhất " + cost + " quả Bí Ngô để đổi Giỏ Kẹo Halloween!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, biNgo, cost);
                            Item gioKeo = ItemService.gI().createNewItem((short) ConstItem.GIO_KEO_BI_NGO, 1);
                            gioKeo.itemOptions.add(new Item.ItemOption(30, 0));
                            InventoryService.gI().addItemBag(player, gioKeo);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đổi thành công! Con nhận được " + gioKeo.template.name);
                        }
                        case 2 -> { // 99 Kẹo bàn tay lấy 1 Giỏ kẹo Halloween (1497)
                            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                                Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống!");
                                return;
                            }
                            Item keo = InventoryService.gI().findItemBagByTemp(player, ConstItem.KEO_BAN_TAY);
                            int cost = 99;
                            if (keo == null || keo.quantity < cost) {
                                Service.gI().sendThongBao(player, "Con cần ít nhất " + cost + " Kẹo Bàn Tay để làm 1 Giỏ Kẹo Halloween!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, keo, cost);
                            Item gioKeo = ItemService.gI().createNewItem((short) ConstItem.GIO_KEO_BI_NGO, 1);
                            gioKeo.itemOptions.add(new Item.ItemOption(30, 0));
                            InventoryService.gI().addItemBag(player, gioKeo);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Làm thành công! Con nhận được " + gioKeo.template.name);
                        }
                        case 3 -> { // 30 Bí Ngô lấy 5 Thỏi vàng (457)
                            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                                Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống!");
                                return;
                            }
                            Item biNgo = InventoryService.gI().findItemBagByTemp(player, ConstItem.BI_NGO);
                            int cost = 30;
                            if (biNgo == null || biNgo.quantity < cost) {
                                Service.gI().sendThongBao(player, "Con cần ít nhất " + cost + " quả Bí Ngô để đổi 5 Thỏi Vàng!");
                                return;
                            }
                            InventoryService.gI().subQuantityItemsBag(player, biNgo, cost);
                            Item thoiVang = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, 5);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đổi thành công! Con nhận được 5 Thỏi Vàng!");
                        }
                    }
                }
            }
        }
    }
}
