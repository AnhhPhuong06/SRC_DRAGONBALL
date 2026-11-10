package services.func;

import consts.ConstItem;
import services.CombineService;
import services.ShenronEventService;
import models.radar.Card;
import services.RadarService;
import models.radar.RadarCard;
import consts.ConstMap;
import models.item.Item;
import consts.ConstNpc;
import consts.ConstPlayer;
import consts.ConstTask;
import database.daos.PlayerDAO;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import models.boss.Boss;
import models.boss.boss_list.pocolo.Pocolo;
import models.combine.list.nangpetubu;
import models.item.Item.ItemOption;
import models.map.ItemMap;
import models.map.Map;
import models.map.Zone;
import models.map.vetinh.Satellite;
import models.map.vetinh.SatelliteDef;
import models.map.vetinh.SatelliteExp;
import models.map.vetinh.SatelliteHp;
import models.map.vetinh.SatelliteMp;
import models.npc.specialnpc.BillEgg;
import models.npc.specialnpc.BlackEgg;
import models.npc.specialnpc.MabuEgg;
import models.player.Inventory;
import services.map.NpcService;
import models.player.Player;
import models.skill.Skill;
import network.io.Message;
import services.map.ChangeMapService;
import utils.SkillUtil;
import services.Service;
import utils.Util;
import network.session.MySession;
import services.ItemService;
import services.ItemTimeService;
import services.PetService;
import services.RewardService;
import services.player.PlayerService;
import services.TaskService;
import services.player.InventoryService;
import services.map.MapService;
import services.dungeon.NgocRongNamecService;
import utils.Logger;
import utils.TimeUtil;

public class UseItem {

    /** Sức mạnh tối thiểu để dùng vé vào map 182 (item 1446). */
    private static final long SUC_MANH_DUNG_VE_182 = 30_000_000_000L;

    private static final int ITEM_BOX_TO_BODY_OR_BAG = 0;
    private static final int ITEM_BAG_TO_BOX = 1;
    private static final int ITEM_BODY_TO_BOX = 3;
    private static final int ITEM_BAG_TO_BODY = 4;
    private static final int ITEM_BODY_TO_BAG = 5;
    private static final int ITEM_BAG_TO_PET_BODY = 6;
    private static final int ITEM_BODY_PET_TO_BAG = 7;

    private static final byte DO_USE_ITEM = 0;
    private static final byte DO_THROW_ITEM = 1;
    private static final byte ACCEPT_THROW_ITEM = 2;
    private static final byte ACCEPT_USE_ITEM = 3;

    private static UseItem instance;

    private UseItem() {

    }

    public static UseItem gI() {
        if (instance == null) {
            instance = new UseItem();
        }
        return instance;
    }

    public void getItem(MySession session, Message msg) {
        Player player = session.player;
        if (player == null) {
            return;
        }
        TransactionService.gI().cancelTrade(player);
        try {
            int type = msg.reader().readByte();
            int index = msg.reader().readByte();
            if (index == -1) {
                return;
            }
            switch (type) {
                case ITEM_BOX_TO_BODY_OR_BAG:
                    InventoryService.gI().itemBoxToBodyOrBag(player, index);
                    TaskService.gI().checkDoneTaskGetItemBox(player);
                    break;
                case ITEM_BAG_TO_BOX:
                    InventoryService.gI().itemBagToBox(player, index);
                    break;
                case ITEM_BODY_TO_BOX:
                    InventoryService.gI().itemBodyToBox(player, index);
                    break;
                case ITEM_BAG_TO_BODY:
                    InventoryService.gI().itemBagToBody(player, index);
                    break;
                case ITEM_BODY_TO_BAG:
                    InventoryService.gI().itemBodyToBag(player, index);
                    break;
                case ITEM_BAG_TO_PET_BODY:
                    InventoryService.gI().itemBagToPetBody(player, index);
                    break;
                case ITEM_BODY_PET_TO_BAG:
                    InventoryService.gI().itemPetBodyToBag(player, index);
                    break;
            }
            if (player.setClothes != null) {
                player.setClothes.setup();
            }
            if (player.pet != null) {
                player.pet.setClothes.setup();
            }
            player.setClanMember();
            Service.gI().sendFlagBag(player);
            if (player.pet != null) {
                Service.gI().sendFlagBag(player.pet);
            }
            Service.gI().point(player);
            Service.gI().sendSpeedPlayer(player, -1);
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);

        }
    }

    public Item finditem(Player player, int iditem) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == iditem) {
                return item;
            }
        }
        return null;
    }

    public void dungPhieuVnd(Player pl, int tempId, int soLuong, int donGia) {
        if (soLuong < 1) {
            Service.gI().sendThongBao(pl, "Số lượng không hợp lệ");
            return;
        }
        long tongTien = (long) soLuong * donGia;
        if (tongTien > Integer.MAX_VALUE) {
            Service.gI().sendThongBao(pl, "Số lượng quá lớn");
            return;
        }
        int dangCo = InventoryService.gI().getNumItemBag(pl, tempId);
        if (dangCo < soLuong) {
            Service.gI().sendThongBao(pl, "Phiếu trong hành trang không đủ, đang có " + dangCo + " phiếu");
            return;
        }
        // Kê trước sẽ trừ bao nhiêu từ stack nào (giữ reference để nếu phải hoàn thì trả
        // đúng option của từng stack — các stack cùng id có thể khác option)
        List<Item> stackTru = new ArrayList<>();
        List<Integer> luongTru = new ArrayList<>();
        int canTru = soLuong;
        for (Item it : pl.inventory.itemsBag) {
            if (canTru <= 0) {
                break;
            }
            if (it != null && it.isNotNullItem() && it.template.id == tempId) {
                int tru = Math.min(canTru, it.quantity);
                stackTru.add(it);
                luongTru.add(tru);
                canTru -= tru;
            }
        }
        InventoryService.gI().subQuantityItemsBag(pl, tempId, soLuong);
        InventoryService.gI().sendItemBags(pl);
        boolean ok = false;
        try {
            ok = PlayerDAO.addVndNe(pl, (int) tongTien);
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        }
        if (ok) {
            Service.gI().sendThongBao(pl, "Bạn đã dùng " + soLuong + " phiếu, được cộng " + Util.numberToMoney(tongTien) + " VNĐ");
            return;
        }
        // Cộng tiền thất bại: hoàn theo từng stack đã trừ, chia phần dưới mức 99.999/stack.
        // addItemBag có thể gộp được MỘT PHẦN rồi trả false (hoan.quantity là phần chưa vào)
        // nên chỉ tính thiếu phần thật sự không vào được túi.
        int thieu = 0;
        for (int i = 0; i < stackTru.size(); i++) {
            int can = luongTru.get(i);
            while (can > 0) {
                int phan = Math.min(can, 99_999);
                Item hoan = ItemService.gI().createNewItem((short) tempId, phan);
                hoan.itemOptions.addAll(stackTru.get(i).itemOptions);
                if (InventoryService.gI().addItemBag(pl, hoan)) {
                    can -= phan;
                } else {
                    can -= (phan - hoan.quantity);
                    thieu += can;
                    break;
                }
            }
        }
        InventoryService.gI().sendItemBags(pl);
        if (thieu > 0) {
            Logger.fileLog("phieu_vnd", "Hoàn phiếu " + tempId + " cho " + pl.name + " (id=" + pl.id
                    + ") thiếu " + thieu + "/" + soLuong + " phiếu vì túi không nhận");
            Service.gI().sendThongBao(pl, "Có lỗi khi cộng tiền, hoàn thiếu " + thieu + " phiếu, hãy liên hệ admin!");
        } else {
            Service.gI().sendThongBao(pl, "Có lỗi xảy ra khi cộng tiền, phiếu đã được hoàn lại");
        }
    }

    public void doItem(Player player, Message _msg) {
        TransactionService.gI().cancelTrade(player);
        Message msg = null;
        byte type;
        try {
            type = _msg.reader().readByte();
            int where = _msg.reader().readByte();
            int index = _msg.reader().readByte();
            switch (type) {
                case DO_USE_ITEM:
                    if (player != null && player.inventory != null) {
                        if (index != -1) {
                            if (index < 0) {
                                return;
                            }
                            Item item = player.inventory.itemsBag.get(index);
                            if (item.isNotNullItem()) {
                                if (ItemService.gI().isOutOfDateTime(item)) {
                                    String name = item.template != null ? item.template.name : "Vật phẩm";
                                    player.inventory.itemsBag.set(index, ItemService.gI().createItemNull());
                                    InventoryService.gI().sendItemBags(player);
                                    Service.gI().sendThongBao(player, name + " đã hết hạn sử dụng.");
                                    return;
                                }
                                if (item.template.type == 7) {
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc chắn học "
                                            + player.inventory.itemsBag.get(index).template.name + "?");
                                    player.sendMessage(msg);
                                } else if (item.template.id == 570) {
                                    if (player.getSession().isAdmin) {
                                        return;
                                    }
                                    if (!Util.isAfterMidnight(player.lastTimeRewardWoodChest)) {
                                        Service.gI().sendThongBao(player, "Hãy chờ đến ngày mai");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn mở\n"
                                            + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else if (item.template.type == 22) {
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn dùng\n"
                                            + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else {
                                    UseItem.gI().useItem(player, item, index);
                                }
                            }
                        } else {
                            int iditem = _msg.reader().readShort();
                            Item item = finditem(player, iditem);
                            UseItem.gI().useItem(player, item, index);
                        }
                    }
                    break;
                case DO_THROW_ITEM:
                    if (!(player.zone.map.mapId == 21 || player.zone.map.mapId == 22 || player.zone.map.mapId == 23)) {
                        Item item = null;
                        if (index < 0) {
                            return;
                        }
                        if (where == 0) {
                            item = player.inventory.itemsBody.get(index);
                        } else {
                            item = player.inventory.itemsBag.get(index);
                        }

                        if (item.isNotNullItem() && item.template.id == 673) {
                            Service.gI().sendThongBao(player, "Không thể bỏ ra vật phẩm này . Cay chưa ?");
                            return;
                        }
                        if (item.isNotNullItem() && item.template.id == 570) {
                            Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                            return;
                        }
                        if (!item.isNotNullItem()) {
                            return;
                        }
                        msg = new Message(-43);
                        msg.writer().writeByte(type);
                        msg.writer().writeByte(where);
                        msg.writer().writeByte(index);
                        msg.writer().writeUTF("Bạn chắc chắn muốn vứt " + item.template.name + "?");
                        player.sendMessage(msg);
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }
                    break;
                case ACCEPT_THROW_ITEM:
                    InventoryService.gI().throwItem(player, where, index);
                    Service.gI().point(player);
                    InventoryService.gI().sendItemBags(player);
                    break;
                case ACCEPT_USE_ITEM:
                    UseItem.gI().useItem(player, player.inventory.itemsBag.get(index), index);
                    break;
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void useSatellite(Player player, Item item) {
        Satellite satellite = null;
        if (player.zone != null) {
            if (player.zone.getSatellites().size() < 5) {
                switch (item.template.id) {
                    case 342 ->
                        satellite = new SatelliteMp(player.zone, 342, player.location.x,
                                player.zone.map.yPhysicInTop(player.location.x, player.location.y), player);
                    case 343 ->
                        satellite = new SatelliteExp(player.zone, 343, player.location.x,
                                player.zone.map.yPhysicInTop(player.location.x, player.location.y), player);
                    case 344 ->
                        satellite = new SatelliteDef(player.zone, 344, player.location.x,
                                player.zone.map.yPhysicInTop(player.location.x, player.location.y), player);
                    case 345 ->
                        satellite = new SatelliteHp(player.zone, 345, player.location.x,
                                player.zone.map.yPhysicInTop(player.location.x, player.location.y), player);
                }
                if (satellite != null) {
                    InventoryService.gI().subQuantityItemsBag(player, item, 1);
                    satellite.sendVeTinh();
                }
            } else {
                Service.gI().sendThongBaoOK(player, "Đã đạt tối đa số lượng vệ tinh có thể đặt trong khu!");
            }
        }
    }

    private void useItem(Player pl, Item item, int indexBag) {
        if (item != null && item.isNotNullItem()) {
            if (InventoryService.gI().revealHiddenStats(pl, item)) {
                return;
            }
            if (item.template.id == 570) {
                if (!Util.isAfterMidnight(pl.lastTimeRewardWoodChest)) {
                    Service.gI().sendThongBao(pl, "Hãy chờ đến ngày mai");
                } else {
                    openWoodChest(pl, item);
                }
                return;
            }
            if (item.template.strRequire <= pl.nPoint.power) {
                switch (item.template.type) {
                    case 33: // card
                        UseCard(pl, item);
                        break;
                    case 7: // sách học, nâng skill
                        learnSkill(pl, item);
                        break;
                    case 6: // đậu thần
                        this.eatPea(pl);
                        break;
                    case 12: // ngọc rồng các loại
                        controllerCallRongThan(pl, item);
                        break;
                    case 22:
                        useSatellite(pl, item);
                        break;
                    case 23: // thú cưỡi mới
                    case 24: // thú cưỡi cũ
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    case 11: // item bag
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendFlagBag(pl);
                        break;
                    case 21:
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        PetService.Pet2(pl, pl.getHeadThuCung(), pl.getBodyThuCung(), pl.getLegThuCung());
                        Service.gI().point(pl);
                        break;
                    case 72: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendPetFollow(pl, (short) (item.template.iconID - 1));
                        break;
                    }
                    case 74: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendFoot(pl, item.template.id);
                        break;
                    }
                    case 36: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    }
                    case 35: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    }
                    default:
                        switch (item.template.id) {
                            case 457:
                                Input.gI().createFormUseGoldBar(pl);
                                break;

                            case 1500:
                                if (pl.zone.map.mapId != 18 && pl.zone.map.mapId != 19 && pl.zone.map.mapId != 20) {
                                    Service.gI().sendThongBao(pl, "Vui lòng tới hành tình xayda như : Vách núi đen,Thung lũng đen,..");
                                    return;
                                }
                                NpcService.gI().createMenuConMeo(pl, 3213, -1, "Ngươi có chắc muốn dùng Ánh trăng tròn để dụ Khỉ Đột không lồ không ?", "Đồng ý", "Từ chối");
                                break;
                            case 1471:
                                if (TransactionService.gI().check(pl)) {
                                    Service.gI().sendThongBao(pl, "Không thể thực hiện");
                                    return;
                                }
                                if (pl.inventory.gold + 100_000_000 > Inventory.LIMIT_GOLD) {
                                    // int slban = (int) ((Inventory.LIMIT_GOLD - pl.inventory.gold) / 100000000);
                                    // if (slban < 1) {
                                    Service.gI().sendThongBao(pl, "Vàng sau khi bán vượt quá giới hạn");
                                    // } else if (slban < 2) {
                                    // Service.gI().sendThongBao(pl, "Bạn chỉ có thể bán 1 Thỏi vàng");
                                    // } else {
                                    // Service.gI().sendThongBao(pl, "Số lượng trong khoảng 1 tới " + slban);
                                    // }
                                } else {
                                    Item ThoiVang = InventoryService.gI().findItemBag(pl, 1471);

                                    InventoryService.gI().subQuantityItemsBag(pl, ThoiVang, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                    pl.inventory.gold += 100_000_000;
                                    Service.gI().sendMoney(pl);
                                    Service.gI().sendThongBao(pl, "Đã sử dụng Thỏi vàng thu được "
                                            + Util.numberToMoney(100_000_000) + " vàng");
                                }
                                break;
                            case 456:
                                Boss boss = (Boss) pl.zone.getBosses().stream().filter(b -> b.name.equals("Xinbatô"))
                                        .findAny().orElse(null);
                                if (item.quantity >= 99 && boss != null) {
                                    int[] itemne = {441, 442, 443, 444, 445, 446, 447};
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 99);
                                    InventoryService.gI().addItemBag(pl,
                                            Util.saoPhaLe(Util.isTrue(70, 100) ? itemne[Util.nextInt(itemne.length - 1)]
                                                    : itemne[itemne.length - 1], 1));
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().chat(boss, "Ngước ngon quá, bắn tùm lum");
                                    if (pl.playerTask.taskdh.ChoNuoc < 5) {
                                        int percentDone = (int) ((double) pl.playerTask.taskdh.ChoNuoc / 5 * 100);
                                        pl.playerTask.taskdh.ChoNuoc++;
                                        pl.playerTask.taskdh.ResetTime = System.currentTimeMillis();
                                        Service.gI().sendThongBao(pl, "Tiến độ hiện tại: " + percentDone + "%");
                                    }
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                    }
                                    Zone newZone = Util.randomAllMap();
                                    ChangeMapService.gI().changeMapYardrat(boss, newZone,
                                            newZone.map.mapWidth + Util.nextInt(-50, 50),
                                            newZone.map.yPhysicInTop(newZone.map.mapWidth, newZone.map.mapHeight));
                                } else {
                                    Service.gI().sendThongBao(pl, "Hãy sử dụng khi gặp Xinbato.");
                                }
                                break;
                            case 460:
                                if (pl.zone.getBosses().stream().filter(b -> b.name.equals("Sói hẹc quyn")).findAny()
                                        .orElse(null) != null) {
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().dropItemMap(pl.zone,
                                            new ItemMap(pl.zone, 460, 1, pl.location.x, pl.location.y, pl.id));
                                    if (pl.playerTask.taskdh.ChoSuong < 20) {
                                        int percentDone = (int) ((double) pl.playerTask.taskdh.ChoSuong / 20 * 100);
                                        pl.playerTask.taskdh.ChoSuong++;
                                        pl.playerTask.taskdh.ResetTime = System.currentTimeMillis();
                                        Service.gI().sendThongBao(pl, "Tiến độ hiện tại: " + percentDone + "%");
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl,
                                            "Không thể vứt cục xương, hãy đến nơi có Sói hẹc quyn.");
                                }
                                break;
                            case 1363: { // Rương Đá: mở ngẫu nhiên các loại đá 1364, 1366, 1368 (đã bỏ 1365, 1367)
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                short[] dsDa = {1364, 1366, 1368};
                                short idDa = dsDa[Util.nextInt(0, dsDa.length - 1)];
                                Item da = ItemService.gI().createNewItem(idDa);
                                da.itemOptions.add(new ItemOption(30, 0));
                                InventoryService.gI().addItemBag(pl, da);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + da.template.name);
                                break;
                            }
                            case 1959: {

                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {

                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");

                                    return;

                                }

                                short[] ngocBang = {925, 926, 927, 928, 929, 930, 931};

                                short idNgoc = ngocBang[Util.nextInt(0, ngocBang.length - 1)];

                                Item vien = ItemService.gI().createNewItem(idNgoc);

                                InventoryService.gI().addItemBag(pl, vien);

                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);

                                InventoryService.gI().sendItemBags(pl);

                                Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + vien.template.name);

                                break;

                            }
                            case ConstItem.GIO_KEO_BI_NGO: {
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                int roll = Util.nextInt(1, 100);
                                if (roll <= 10) { // 10% Cải trang Bí ngô nhí nhảnh (910)
                                    int hsd = Util.nextInt(3, 7);
                                    Item ct = ItemService.gI().createNewItem((short) ConstItem.CAI_TRANG_BILL_BI_NGO, 1);
                                    ct.itemOptions.add(new Item.ItemOption(50, Util.nextInt(15, 25))); // % Sức đánh
                                    ct.itemOptions.add(new Item.ItemOption(77, Util.nextInt(15, 25))); // % HP
                                    ct.itemOptions.add(new Item.ItemOption(103, Util.nextInt(15, 25))); // % KI
                                    ct.itemOptions.add(new Item.ItemOption(93, hsd)); // HSD 3-7 ngày
                                    InventoryService.gI().addItemBag(pl, ct);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn mở được " + ct.template.name + " (" + hsd + " ngày)!");
                                } else if (roll <= 45) { // 35% Thỏi vàng (457) x1 - x3
                                    int countGold = Util.nextInt(1, 3);
                                    Item thoiVang = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, countGold);
                                    InventoryService.gI().addItemBag(pl, thoiVang);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn mở được " + countGold + " Thỏi Vàng!");
                                } else if (roll <= 80) { // 35% Ngọc rồng 2 - 4 sao
                                    short[] dsNr = {ConstItem.NGOC_RONG_2_SAO, ConstItem.NGOC_RONG_3_SAO, ConstItem.NGOC_RONG_4_SAO};
                                    short idNr = dsNr[Util.nextInt(0, dsNr.length - 1)];
                                    Item nr = ItemService.gI().createNewItem(idNr);
                                    InventoryService.gI().addItemBag(pl, nr);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn mở được " + nr.template.name + "!");
                                } else { // 20% Hồng ngọc (50 - 100)
                                    int ruby = Util.nextInt(50, 100);
                                    pl.inventory.ruby += ruby;
                                    Service.gI().sendMoney(pl);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn mở được " + ruby + " Hồng Ngọc!");
                                }
                                InventoryService.gI().sendItemBags(pl);
                                break;
                            }
                            case 572: { // Rương Vàng: mở ngẫu nhiên 1 trong các đá vô cực 1577-1581
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                // Đá Không Gian (1580) hiếm hơn: 5%; 4 loại còn lại chia đều 95% (mỗi loại 23.75%)
                                short idDa;
                                if (Util.nextInt(1, 100) <= 5) {
                                    idDa = 1580;
                                } else {
                                    short[] daThuong = {1577, 1578, 1579, 1581};
                                    idDa = daThuong[Util.nextInt(0, 3)];
                                }
                                Item da = ItemService.gI().createNewItem(idDa);
                                InventoryService.gI().addItemBag(pl, da);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + da.template.name);
                                break;
                            }
                            case 1005: // xô cá
                                short[] itemIds = {997, 998, 999, 1000, 995, 996, 1347, 1023};
                                Random rd = new Random();
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                if (item.quantity >= 1) {
                                    short id = itemIds[rd.nextInt(itemIds.length)];
                                    Item it = ItemService.gI().createNewItem(id);
                                    it.itemOptions.add(new ItemOption(50, Util.nextInt(8, 15)));
                                    it.itemOptions.add(new ItemOption(77, Util.nextInt(8, 15)));
                                    it.itemOptions.add(new ItemOption(103, Util.nextInt(8, 15)));
                                    it.itemOptions.add(new ItemOption(14, Util.nextInt(8, 12)));
                                    it.itemOptions.add(new ItemOption(5, Util.nextInt(5, 10)));
                                    it.itemOptions.add(new ItemOption(93, Util.nextInt(3, 10)));
                                    InventoryService.gI().addItemBag(pl, it);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + it.template.name);

                                }
                                break;
                            case 1006:// xô cá
                                short[] itemId = {999, 994, 995, 997, 998, 996, 1347, 1023, 1000, 1001};
                                Random Rd = new Random();
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                if (item.quantity >= 1) {
                                    short id = itemId[Rd.nextInt(itemId.length)];
                                    Item it = ItemService.gI().createNewItem(id);
                                    it.itemOptions.add(new ItemOption(50, Util.nextInt(8, 15)));
                                    it.itemOptions.add(new ItemOption(77, Util.nextInt(8, 14)));
                                    it.itemOptions.add(new ItemOption(103, Util.nextInt(8, 14)));
                                    it.itemOptions.add(new ItemOption(14, Util.nextInt(8, 11)));
                                    it.itemOptions.add(new ItemOption(5, Util.nextInt(5, 10)));
                                    it.itemOptions.add(new ItemOption(93, Util.nextInt(5, 10)));
                                    InventoryService.gI().addItemBag(pl, it);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + it.template.name);
                                }
                                break;
                            case 1003:// xô cá
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                if (item.quantity >= 10) {
                                    Item it = ItemService.gI().createNewItem((short) 1004);
                                    InventoryService.gI().addItemBag(pl, it);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 10);
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + it.template.name);
                                }
                                break;
                            case 1346: {  // kemque
                                short[] ID = {1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083};
                                Random RD = new Random();
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                if (item.quantity >= 1) {
                                    if (Util.isTrue(30, 100)) {
                                        int gold = Util.nextInt(30000, 100000);
                                        pl.inventory.gold += gold;
                                        Service.gI().sendMoney(pl);
                                        Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + gold + " vàng");
                                    } else {
                                        short id = ID[RD.nextInt(ID.length)];
                                        Item it = ItemService.gI().createNewItem(id);
                                        it.itemOptions.add(new ItemOption(30, 0));
                                        it.itemOptions.add(new ItemOption(93, 30));
                                        InventoryService.gI().addItemBag(pl, it);
                                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                        InventoryService.gI().sendItemBags(pl);
                                        Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + it.template.name);
                                    }
                                }
                                break;
                            }

                            case 1347: // kem trái cây
                                if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
                                    Service.gI().sendThongBao(pl, "Ngươi cần ít nhất 1 ô trống hành trang");
                                    return;
                                }
                                if (item.quantity >= 1) {
                                    Item it = ItemService.gI().createNewItem((short) 1346);
                                    InventoryService.gI().addItemBag(pl, it);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    Service.gI().sendThongBao(pl, "Chúc mừng bạn vừa nhận được " + it.template.name);
                                    InventoryService.gI().sendItemBags(pl);
                                }
                                break;
                            case 568:
                                if (pl.mabuEgg == null) {
                                    MabuEgg.createMabuEgg(pl);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                } else {
                                    Service.gI().sendThongBao(pl, "Con đang có trứng Mabu chưa mở ở nhà rồi.");
                                }
                                break;
                            case 1475:
                                if (pl.billEgg == null) {
                                    BillEgg.createBillEgg(pl);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                } else {
                                    Service.gI().sendThongBao(pl, "Con đang có trứng Bill chưa mở ở nhà rồi.");
                                }
                                break;
                            case 1519:
                                if (pl.blackEgg == null) {
                                    BlackEgg.createBlackEgg(pl);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                } else {
                                    Service.gI().sendThongBao(pl, "Con đang có trứng Black Goku chưa mở ở nhà rồi.");
                                }
                                break;
                            case 1518:
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.MENU_CHON_DE_JIREN, 0, "Mở Trứng Jiren nhận Đệ tử Jiren cực mạnh!\nChọn hành tinh cho Đệ tử Jiren của bạn:\n(Lưu ý: Nếu đã có đệ tử sẽ đổi thành đệ tử Jiren)", "Trái đất", "Namek", "Xayda", "Từ chối");
                                break;
                            case 1396:
                                PetService.gI().createNormalPet(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                break;
                            case 1138:
                            case 1139:
                                Input.gI().createFormThongBao(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                if (pl.playerTask.taskdh.DungLoa < 10) {
                                    pl.playerTask.taskdh.DungLoa++;
                                    pl.playerTask.taskdh.ResetTime = System.currentTimeMillis();
                                }
                                break;
                            case 992: // Nhan thoi khong
                                pl.type = 2;
                                pl.maxTime = 5;
                                Service.gI().Transport(pl);
                                break;
                            case 361:
                                pl.idGo = (short) Util.nextInt(0, 6);
                                NgocRongNamecService.gI().menuCheckTeleNamekBall(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                break;
                            case 1128:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1404, 1405, 1406);
                                Service.gI().point(pl);
                                break;
                            case 1129:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1407, 1408, 1409);
                                Service.gI().point(pl);
                                break;
                            case 1232:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1500, 1501, 1502);
                                Service.gI().point(pl);
                                break;
                            case 1130:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1410, 1411, 1412);
                                Service.gI().point(pl);
                                break;
                            case 1131:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1413, 1414, 1415);
                                Service.gI().point(pl);
                                break;
                            case 1132:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1416, 1417, 1418);
                                Service.gI().point(pl);
                                break;
                            case 1133:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1419, 1420, 1421);
                                Service.gI().point(pl);
                                break;
                            case 1146:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1435, 1436, 1437);
                                Service.gI().point(pl);
                                break;
                            case 1145:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1438, 1439, 1440);
                                Service.gI().point(pl);
                                break;
                            case 1144:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1441, 1442, 1443);
                                Service.gI().point(pl);
                                break;
                            case 1148:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1444, 1445, 1446);
                                Service.gI().point(pl);
                                break;
                            case 1222:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1485, 1486, 1487);
                                Service.gI().point(pl);
                                break;
                            case 211: // nho tím
                            case 212: // nho xanh
                                eatGrapes(pl, item);
                                break;
                            case 380: // cskb
                                openCSKB(pl, item);
                                break;
                            case 381: // cuồng nộ
                                models.task.Task_Kol.tick(pl, 1, 36); // KOL 36 (dùng 99 Cuồng Nộ)
                            case 382: // bổ huyết
                            case 383: // bổ khí
                            case 384: // giáp xên
                            case 385: // ẩn danh
                            case 379: // máy dò capsule
                            case 638: // commeson
                            case 2075: // rocket
                            case 2160: // Nồi cơm điện
                            case 579:
                            case 1045: // đuôi khỉ
                            case 663: // bánh pudding
                            case 664: // xúc xíc
                            case 665: // kem dâu
                            case 666: // mì ly
                            case 667: // sushi
                            case 1099:
                            case 1100:
                            case 1101:
                            case 1102:
                            case 1103:
                            case 764:
                            case 1136:
                            case 1137:
                            case 753:
                            case 752:
                            case 899:
                            case 900:
                            case 902:
                            case 903:
                            case 1261:
                            case 1262:
                            case 1397:
                            case 1343:
                            case 1344:
                            case 1345:
                            case 1357: // bình x3 tiềm năng sức mạnh
                            case 1358: // bình x5 tiềm năng sức mạnh
                            case 1359: // bình x7 tiềm năng sức mạnh
                                useItemTime(pl, item);
                                break;
                            case 1150:
                                openHopRandom1149(pl, item);
                                break;
                            case 1451:
                                hopCaiTrangHit(pl, item);
                                break;
                            case 1617:
                                hopCaiTrangZeno(pl, item);
                                break;
                            case 1618:
                                hopVanBay(pl, item);
                                break;
                            case 1619:
                                hopCaiTrangGoku(pl, item);
                                break;
                            case 1452:
                                skipToTask20(pl);
                                break;
                            case 1454:
                                hopCaiTrangHitVIP(pl, item);
                                break;
                            case 1467:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1468:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1469:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1472:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1473:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1474:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1426:
                                open1426(pl, item);
                                break;
                            case 1429:
                                open1429(pl, item);
                                break;
                            case 1428:
                                open1428(pl, item);
                                break;
                            case 1430:
                                pl.restrictedMapTicket = 168; 
                                ChangeMapService.gI().changeMapNonSpaceship(pl, 168, 155, 312);
                                if (pl.zone == null || pl.zone.map == null || pl.zone.map.mapId != 168) {
                                    pl.restrictedMapTicket = -1;
                                }
                                break;
                            case 1340:
                                Input.gI().createFormDungPhieuVnd(pl, 1340, 10000);
                                break;
                            case 1341:
                                Input.gI().createFormDungPhieuVnd(pl, 1341, 8000);
                                break;
                            case 1427:
                                Item trungRong = InventoryService.gI().findItemBag(pl, 1427);
                                if (trungRong != null && trungRong.quantity >= 99) {
                                    open1427(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Đã đủ địt đâu và sử dụng gom x99 giùm bố cái");
                                }
                                break;
                            case 880:
                            case 881:
                            case 882:
                                if (pl.itemTime.isEatMeal2) {
                                    Service.gI().sendThongBao(pl, "Chỉ được sử dụng 1 cái");
                                    break;
                                }
                                useItemTime(pl, item);
                                break;
                            case 570:
                                openWoodChest(pl, item);
                                models.task.Task_Kol.tick(pl, 1, 39); // KOL 39 (mở 2 Rương Gỗ)
                                break;
                            case 521: // tdlt
                                useTDLT(pl, item);
                                break;
                            case 1391:
                                OpenDevNung(pl, item);
                                break;
                            case 454: // bông tai
                                UseItem.gI().usePorata(pl);
                                break;
                            case 1227:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1228:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1229:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 921: // bông tai
                                UseItem.gI().usePorata2(pl);
                                break;
                            case 1492:
                                UseItem.gI().usePorata3(pl);
                                break;
                            case 1477:
                            case 1478:
                            case 1501:
                            case 1502:
                                services.ThienSuSKHBox.open(pl, item);
                                return;
                            case 1494:
                                UseItem.gI().openHopSKHThuong(pl, item);
                                break;
                            case 1495:
                                UseItem.gI().openHopSKHHuyDiet(pl, item);
                                break;
                            case 1496:
                                UseItem.gI().openCapsuleCellMax(pl, item);
                                break;
                            case 193: // gói 10 viên capsule
                                if (models.npc.npc_list.DuongTang.isDangDatDuong(pl.id)) {
                                    Service.gI().sendThongBao(pl, "Đang hộ tống Đường Tăng không thể dùng capsule!");
                                    break;
                                }
                                openCapsuleUI(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            case 194: // capsule đặc biệt
                                if (models.npc.npc_list.DuongTang.isDangDatDuong(pl.id)) {
                                    Service.gI().sendThongBao(pl, "Đang hộ tống Đường Tăng không thể dùng capsule!");
                                    break;
                                }
                                openCapsuleUI(pl);
                                break;
                            case 401: // đổi đệ tử
                                changePet(pl, item);
                                break;
                            case 402: // sách nâng chiêu 1 đệ tử
                            case 403: // sách nâng chiêu 2 đệ tử
                            case 404: // sách nâng chiêu 3 đệ tử
                            case 759: // sách nâng chiêu 4 đệ tử
                                upSkillPet(pl, item);
                                break;
                            case 726:
                                UseItem.gI().ItemManhGiay(pl, item);
                                break;
                            case 1476:
                                Map map = MapService.gI().getMapById(146);
                                if (map.zones.stream().anyMatch(z -> z.getBosses().stream().anyMatch(b -> b instanceof Pocolo))) {
                                    ChangeMapService.gI().changeMapNonSpaceship(pl, 146, 190, 336);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                } else {
                                    Service.gI().sendThongBao(pl, "Hiện boss đã chết, vui lòng quay lại sau!");
                                }
                                break;
                            case 1446:
                                // Chặn TRƯỚC khi trừ vé, không thì người chơi mất vé mà
                                // vẫn không vào được map.
                                if (pl.nPoint == null || pl.nPoint.power < SUC_MANH_DUNG_VE_182) {
                                    Service.gI().sendThongBao(pl, "Cần "
                                            + Util.numberToMoney(SUC_MANH_DUNG_VE_182)
                                            + " sức mạnh mới dùng được vật phẩm này");
                                    break;
                                }
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                pl.map182Deadline = System.currentTimeMillis() + 30 * 60 * 1000L;
                                pl.restrictedMapTicket = 182;
                                ChangeMapService.gI().changeMapNonSpaceship(pl, 182, 200, 336);
                                if (pl.zone == null || pl.zone.map == null || pl.zone.map.mapId != 182) {
                                    pl.restrictedMapTicket = -1; // vào hụt (map đầy...) -> thu hồi vé
                                    pl.map182Deadline = 0;
                                    break;
                                }
                                Service.gI().sendThongBao(pl, "Bạn có 30 phút trong map, hết giờ sẽ bị đưa về nhà!");
                                ItemTimeService.gI().sendTextMap182(pl);
                                break;
                            case 1009: // Rương Tranh Huyền Bí: mở random item + option SĐ/HP/KI
                                ItemService.gI().OpenRuongTranhHuyenBi(pl, item);
                                models.task.Task_Kol.tick(pl, 1, 18); // KOL 18 (mở 50 Rương Huyền Bí)
                                break;
                            case 1016: // Thuốc mỡ Ipana: +10% HP/KI/sức đánh trong 10 phút + hồi đầy HP/KI
                                pl.charms.addTimeCharms(1016, 10);
                                pl.nPoint.calPoint();
                                pl.nPoint.hp = pl.nPoint.hpMax;
                                pl.nPoint.mp = pl.nPoint.mpMax;
                                Service.gI().point(pl);
                                PlayerService.gI().sendInfoHp(pl);
                                PlayerService.gI().sendInfoMp(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                ItemTimeService.gI().sendBuff1016(pl); // icon + đếm giờ như cuồng nộ
                                Service.gI().sendThongBao(pl, "Đã dùng " + item.template.name + ": +10% HP/KI/Sức đánh trong 10 phút!");
                                break;
                            case 727:
                            case 728:
                                UseItem.gI().ItemSieuThanThuy(pl, item);
                                break;
                            case 648:
                                ItemService.gI().OpenItem648(pl, item);
                                break;
                            case 1143:
                                ItemService.gI().OpenItem1143(pl, item);
                                break;
                            case 1198:
                                ItemService.gI().OpenItem1198(pl, item);
                                break;
                            case 1199:
                                ItemService.gI().OpenItem1199(pl, item);
                                break;
                            case 1200:
                                ItemService.gI().OpenItem1200(pl, item);
                                break;
                            case 736:
                                ItemService.gI().OpenItem736(pl, item);
                                break;
                            case 1530:
                                open1530(pl, item);
                                break;
                            case 1531:
                                open1531(pl, item);
                                break;
                            case 1532:
                                open1532(pl, item);
                                break;
                            case 1521:
                                if (pl.pet == null) {
                                    Service.gI().sendThongBao(pl, "Ngươi làm gì có đệ tử?");
                                    break;
                                }
                                NpcService.gI().createMenuConMeo(pl, 5566, 0, "Chọn hành tinh đệ tử Ubu đi mày", "Trái đất", "Namek", "Xayda");
                                break;
                            case 987:
                                Service.gI().sendThongBao(pl, "Bảo vệ trang bị không bị rớt cấp"); // đá bảo vệ
                                break;
                            case 2006:
                            case 1935:
                                Input.gI().createFormChangeNameByItem(pl);
                                break;
                            case 1156, 1157:
                                ItemService.gI().OpenCapsuleCaiTrang(pl, item);
                                break;
                            case 1190:
                                Input.gI().createFormTangRuby(pl);
                                break;
                            case 1189:
                                Input.gI().createFormTangGem(pl);
                                break;
                            case 1259:
                                ItemService.gI().OpenItem1259(pl, item);
                                break;
                            case 1245:
                                ItemService.gI().OpenItem1245(pl, item);
                                break;
                            case 1258:
                                ItemService.gI().OpenItem1258(pl, item);
                                break;
                            case 1246:
                                ItemService.gI().OpenItem1246(pl, item);
                                break;
                            case 1247:
                                ItemService.gI().OpenItem1247(pl, item);
                                break;
                            case 1305:
                                ItemService.gI().OpenItem1305(pl, item);
                                break;
                            case 1381:
                                if (pl.pet == null) { // Đổi skill 2
                                    Service.gI().sendThongBao(pl, "Ngươi làm gì có đệ tử?");
                                    break;
                                }
                                if (pl.pet.playerSkill.skills.get(1).skillId != -1) {
                                    pl.pet.openSkill2();
                                    InventoryService.gI().subQuantityItem(pl.inventory.itemsBag, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().sendThongBao(pl, "Đã đổi thành công chiêu 2 đệ tử");
                                } else {
                                    Service.gI().sendThongBao(pl, "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");
                                }
                                break;
                            case 1382:
                                if (pl.pet == null) { // Đổi skill 3
                                    Service.gI().sendThongBao(pl, "Ngươi làm gì có đệ tử?");
                                    break;
                                }
                                if (pl.pet.playerSkill.skills.get(2).skillId != -1) {
                                    pl.pet.openSkill3();
                                    InventoryService.gI().subQuantityItem(pl.inventory.itemsBag, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().sendThongBao(pl, "Đã đổi thành công chiêu 3");
                                } else {
                                    Service.gI().sendThongBao(pl, "Ít nhất đệ tử ngươi phải có chiêu 3 chứ!");
                                }
                                break;
                            case 1383:
                                if (pl.pet == null) { // Đổi skill 4
                                    Service.gI().sendThongBao(pl, "Ngươi làm gì có đệ tử?");
                                    break;
                                }
                                if (pl.pet.playerSkill.skills.get(3).skillId != -1) {
                                    pl.pet.openSkill4();
                                    InventoryService.gI().subQuantityItem(pl.inventory.itemsBag, item, 1);
                                    InventoryService.gI().sendItemBags(pl);
                                    Service.gI().sendThongBao(pl, "Đã đổi thành công chiêu 4");
                                } else {
                                    Service.gI().sendThongBao(pl, "Ít nhất đệ tử ngươi phải có chiêu 4 chứ!");
                                }
                                break;
                            case 1032: {
                                Item giaymau = InventoryService.gI().findItemBagByTemp(pl, 1032);
                                boolean hasgiaymau = giaymau != null && giaymau.quantity >= 99;
                                StringBuilder npcSay = new StringBuilder();
                                npcSay.append("|2|Chế tạo Hộp đựng quà\n");
                                npcSay.append(hasgiaymau ? "|1|" : "|7|").append("Giấy màu ")
                                        .append(hasgiaymau ? giaymau.quantity : "0").append("/99\n");
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.CHE_TAO_HOP_QUA, -1, npcSay.toString(),
                                        "Đồng ý", "Từ chối");
                            }
                            break;
                            case 1544: {
                                Item hoahonggiay = InventoryService.gI().findItemBagByTemp(pl, 1308);
                                Item socola = InventoryService.gI().findItemBagByTemp(pl, 1307);
                                Item hopdungqua = InventoryService.gI().findItemBagByTemp(pl, 1313);
                                boolean hashoahonggiay = hoahonggiay != null && hoahonggiay.quantity >= 30;
                                boolean hassocola = socola != null && socola.quantity >= 5;
                                boolean hashopdungqua = hopdungqua != null && hopdungqua.quantity >= 1;

                                StringBuilder npcSay = new StringBuilder();
                                npcSay.append("|2|Chế tạo Hộp quà Nhẹ Nhàng\n");
                                npcSay.append(hashoahonggiay ? "|1|" : "|7|").append("Hoa hồng giấy: ")
                                        .append(hashoahonggiay ? hoahonggiay.quantity : "0").append("/30\n");
                                npcSay.append(hassocola ? "|1|" : "|7|").append("Socola: ")
                                        .append(hassocola ? socola.quantity : "0").append("/5\n");
                                npcSay.append(hashopdungqua ? "|1|" : "|7|").append("Hộp đựng quà: ")
                                        .append(hashopdungqua ? hopdungqua.quantity : "0").append("/1\n");

                                NpcService.gI().createMenuConMeo(pl, ConstNpc.CHE_TAO_HOP_QUA_NHE_NHANG, -1,
                                        npcSay.toString(), "Đồng ý", "Từ chối");
                            }
                            break;
                            case 1545: {
                                Item hoahonggiay = InventoryService.gI().findItemBagByTemp(pl, 1308);
                                Item socola = InventoryService.gI().findItemBagByTemp(pl, 1307);
                                Item hopdungqua = InventoryService.gI().findItemBagByTemp(pl, 1313);
                                boolean hashoahonggiay = hoahonggiay != null && hoahonggiay.quantity >= 30;
                                boolean hassocola = socola != null && socola.quantity >= 5;
                                boolean hashopdungqua = hopdungqua != null && hopdungqua.quantity >= 1;

                                StringBuilder npcSay = new StringBuilder();
                                npcSay.append("|2|Chế tạo Hộp quà Nhẹ Nhàng\n");
                                npcSay.append(hashoahonggiay ? "|1|" : "|7|").append("Hoa hồng giấy: ")
                                        .append(hashoahonggiay ? hoahonggiay.quantity : "0").append("/30\n");
                                npcSay.append(hassocola ? "|1|" : "|7|").append("Socola: ")
                                        .append(hassocola ? socola.quantity : "0").append("/5\n");
                                npcSay.append(hashopdungqua ? "|1|" : "|7|").append("Hộp đựng quà: ")
                                        .append(hashopdungqua ? hopdungqua.quantity : "0").append("/1\n");

                                NpcService.gI().createMenuConMeo(pl, ConstNpc.CHE_TAO_HOP_QUA_NHE_NHANG, -1,
                                        npcSay.toString(), "Đồng ý", "Từ chối");
                            }
                            break;
                            case 1550: {
                                Item hoahonggiay = InventoryService.gI().findItemBagByTemp(pl, 1308);
                                Item socola = InventoryService.gI().findItemBagByTemp(pl, 1307);
                                Item hopdungqua = InventoryService.gI().findItemBagByTemp(pl, 1313);
                                boolean hashoahonggiay = hoahonggiay != null && hoahonggiay.quantity >= 30;
                                boolean hassocola = socola != null && socola.quantity >= 5;
                                boolean hashopdungqua = hopdungqua != null && hopdungqua.quantity >= 1;

                                StringBuilder npcSay = new StringBuilder();
                                npcSay.append("|2|Chế tạo Hộp quà Nhẹ Nhàng\n");
                                npcSay.append(hashoahonggiay ? "|1|" : "|7|").append("Hoa hồng giấy: ")
                                        .append(hashoahonggiay ? hoahonggiay.quantity : "0").append("/30\n");
                                npcSay.append(hassocola ? "|1|" : "|7|").append("Socola: ")
                                        .append(hassocola ? socola.quantity : "0").append("/5\n");
                                npcSay.append(hashopdungqua ? "|1|" : "|7|").append("Hộp đựng quà: ")
                                        .append(hashopdungqua ? hopdungqua.quantity : "0").append("/1\n");

                                NpcService.gI().createMenuConMeo(pl, ConstNpc.CHE_TAO_HOP_QUA_NHE_NHANG, -1,
                                        npcSay.toString(), "Đồng ý", "Từ chối");
                            }
                            break;
                            case 1548: {
                                Item hoahonggiay = InventoryService.gI().findItemBagByTemp(pl, 1308);
                                Item socola = InventoryService.gI().findItemBagByTemp(pl, 1307);
                                Item hopdungqua = InventoryService.gI().findItemBagByTemp(pl, 1313);
                                Item no = InventoryService.gI().findItemBagByTemp(pl, 1311);

                                boolean hashoahonggiay = hoahonggiay != null && hoahonggiay.quantity >= 30;
                                boolean hassocola = socola != null && socola.quantity >= 5;
                                boolean hashopdungqua = hopdungqua != null && hopdungqua.quantity >= 1;
                                boolean hasno = no != null && no.quantity >= 1;

                                StringBuilder npcSay = new StringBuilder();
                                npcSay.append("|2|Chế tạo Hộp quà Chỉn chu\n");
                                npcSay.append(hashoahonggiay ? "|1|" : "|7|").append("Hoa hồng giấy: ")
                                        .append(hashoahonggiay ? hoahonggiay.quantity : "0").append("/30\n");
                                npcSay.append(hassocola ? "|1|" : "|7|").append("Socola: ")
                                        .append(hassocola ? socola.quantity : "0").append("/5\n");
                                npcSay.append(hashopdungqua ? "|1|" : "|7|").append("Hộp đựng quà: ")
                                        .append(hashopdungqua ? hopdungqua.quantity : "0").append("/1\n");
                                npcSay.append(hasno ? "|1|" : "|7|").append("Nơ: ").append(hasno ? no.quantity : "0")
                                        .append("/1\n");

                                NpcService.gI().createMenuConMeo(pl, ConstNpc.CHE_TAO_HOP_QUA_CHIN_CHU, -1,
                                        npcSay.toString(), "Đồng ý", "Từ chối");
                            }
                            break;
                            case 1097: {
                                Item dattrong = InventoryService.gI().findItemBagByTemp(pl, 1306);
                                Item ongtrenuoc = InventoryService.gI().findItemBagByTemp(pl, 1312);
                                Item hatgiong = InventoryService.gI().findItemBagByTemp(pl, 1309);
                                Item chaudat = InventoryService.gI().findItemBagByTemp(pl, 1322);

                                boolean hasdattrong = dattrong != null && dattrong.quantity >= 99;
                                boolean hasongtrenuoc = ongtrenuoc != null && ongtrenuoc.quantity >= 5;
                                boolean hashatgiong = hatgiong != null && hatgiong.quantity >= 1;
                                boolean haschaudat = chaudat != null && chaudat.quantity >= 1;

                                StringBuilder npcSay = new StringBuilder();
                                npcSay.append("|2|Trồng hoa hồng\n");
                                npcSay.append(hasdattrong ? "|1|" : "|7|").append("Đất trồng: ")
                                        .append(hasdattrong ? dattrong.quantity : "0").append("/99\n");
                                npcSay.append(hasongtrenuoc ? "|1|" : "|7|").append("Ống tre nước: ")
                                        .append(hasongtrenuoc ? ongtrenuoc.quantity : "0").append("/5\n");
                                npcSay.append(hashatgiong ? "|1|" : "|7|").append("Hạt giống: ")
                                        .append(hashatgiong ? hatgiong.quantity : "0").append("/1\n");
                                npcSay.append(haschaudat ? "|1|" : "|7|").append("Chậu đất: ")
                                        .append(haschaudat ? chaudat.quantity : "0").append("/1\n");

                                NpcService.gI().createMenuConMeo(pl, ConstNpc.TRONG_HOA_HONG, -1, npcSay.toString(),
                                        "Đồng ý", "Từ chối");
                            }
                            break;
                            case 1096: {
                                Item dattrong = InventoryService.gI().findItemBagByTemp(pl, 1306);
                                Item ongtrenuoc = InventoryService.gI().findItemBagByTemp(pl, 1312);
                                Item hatgiong = InventoryService.gI().findItemBagByTemp(pl, 1309);
                                Item chaudat = InventoryService.gI().findItemBagByTemp(pl, 1322);
                                Item thuoctangtruong = InventoryService.gI().findItemBagByTemp(pl, 1323);

                                boolean hasdattrong = dattrong != null && dattrong.quantity >= 99;
                                boolean hasongtrenuoc = ongtrenuoc != null && ongtrenuoc.quantity >= 5;
                                boolean hashatgiong = hatgiong != null && hatgiong.quantity >= 1;
                                boolean haschaudat = chaudat != null && chaudat.quantity >= 1;
                                boolean hasthuoctangtruong = thuoctangtruong != null && thuoctangtruong.quantity >= 1;

                                StringBuilder npcSay = new StringBuilder();
                                npcSay.append("|2|Trồng hoa hồng\n");
                                npcSay.append(hasdattrong ? "|1|" : "|7|").append("Đất trồng: ")
                                        .append(hasdattrong ? dattrong.quantity : "0").append("/99\n");
                                npcSay.append(hasongtrenuoc ? "|1|" : "|7|").append("Ống tưới nước: ")
                                        .append(hasongtrenuoc ? ongtrenuoc.quantity : "0").append("/5\n");
                                npcSay.append(hashatgiong ? "|1|" : "|7|").append("Hạt giống: ")
                                        .append(hashatgiong ? hatgiong.quantity : "0").append("/1\n");
                                npcSay.append(haschaudat ? "|1|" : "|7|").append("Chậu đất: ")
                                        .append(haschaudat ? chaudat.quantity : "0").append("/1\n");
                                npcSay.append(hasthuoctangtruong ? "|1|" : "|7|").append("Thuốc tăng trưởng: ")
                                        .append(hasthuoctangtruong ? thuoctangtruong.quantity : "0").append("/1\n");

                                NpcService.gI().createMenuConMeo(pl, ConstNpc.TRONG_HOA_HONG_1, -1, npcSay.toString(),
                                        "Đồng ý", "Từ chối");
                            }
                            break;
                            case 1321:
                                useHopbabytry(pl, item);
                                break;
                            case 1338:
                                useradangocrong(pl, item);
                                break;
                            case 1324:
                                HopThanLinh(pl, item);
                                break;
                            case 1482, 1483, 1484, 1485, 1486, 1487: {
                                int tempID = item.template.id;
                                int Phieu = switch (tempID) {
                                    case 1482 ->
                                        1;
                                    case 1483 ->
                                        2;
                                    case 1484 ->
                                        3;
                                    case 1485 ->
                                        5;
                                    case 1486 ->
                                        10;
                                    case 1487 ->
                                        20;
                                    default ->
                                        0;
                                };
                                int phieuan = pl.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == tempID).mapToInt(i -> i.quantity).sum() * Phieu;
                                pl.getSession().phieu += phieuan;
                                break;
                            }
                        }
                        break;
                }
                TaskService.gI().checkDoneTaskUseItem(pl, item);
                InventoryService.gI().sendItemBags(pl);
            } else {
                Service.gI().sendThongBaoOK(pl, "Sức mạnh không đủ yêu cầu");
            }
        }
    }

    private void HopThanLinh(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của bạn!", "Set\nTrái đất",
                "Set\nNamec", "Set\nXayda", "Từ chối");
    }

    private int randClothes(int level) {
        int[] pool = ConstItem.LIST_ITEM_CLOTHES[Util.nextInt(0, 2)][Util.nextInt(0, 4)];
        int idx = Math.max(0, Math.min(level - 1, pool.length - 1));
        return pool[idx];
    }

    private void useHopbabytry(Player pl, Item item) { // phụ kiện
        if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
            return;
        }

        int[] items = new int[]{1318, 1319};
        int itemID = items[Util.nextInt(0, items.length - 1)];
        Item reward;
        switch (itemID) {
            case 1319:
                reward = ItemService.gI().createNewItem((short) 1319);
                reward.itemOptions.add(new ItemOption(50, 18));
                reward.itemOptions.add(new ItemOption(77, 7));
                reward.itemOptions.add(new ItemOption(103, 7));
                reward.itemOptions.add(new ItemOption(5, 11));
                if (Util.isTrue(70, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
                }
                break;
            case 1318:
                reward = ItemService.gI().createNewItem((short) 1318);
                reward.itemOptions.add(new ItemOption(50, 18));
                reward.itemOptions.add(new ItemOption(5, 8));
                reward.itemOptions.add(new ItemOption(14, 5));
                if (Util.isTrue(70, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
                }
                break;
            default:
                reward = ItemService.gI().createNewItem((short) itemID, 1);
                break;
        }
        InventoryService.gI().addItemBag(pl, reward);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        Service.gI().sendThongBao(pl, "Bạn đã nhận được " + reward.template.name);
    }

    private void useradangocrong(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
            return;
        }

        int[] items = new int[]{1329, 1330, 1331, 1332, 1333, 1334, 1335, 1336};
        int itemID = items[Util.nextInt(0, items.length - 1)];
        Item reward;
        switch (itemID) {
            case 1336:
                reward = ItemService.gI().createNewItem((short) 1336);
                reward.itemOptions.add(new ItemOption(50, 22));
                reward.itemOptions.add(new ItemOption(77, 22));
                reward.itemOptions.add(new ItemOption(103, 22));
                reward.itemOptions.add(new ItemOption(108, 10));
                reward.itemOptions.add(new ItemOption(94, 12));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(70, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
                }
                break;
            case 1335:
                reward = ItemService.gI().createNewItem((short) 1335);
                reward.itemOptions.add(new ItemOption(50, 12));
                reward.itemOptions.add(new ItemOption(77, 12));
                reward.itemOptions.add(new ItemOption(103, 12));
                reward.itemOptions.add(new ItemOption(14, 5));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(50, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
                }
                break;
            case 1333:
                reward = ItemService.gI().createNewItem((short) 1333);
                reward.itemOptions.add(new ItemOption(50, 15));
                reward.itemOptions.add(new ItemOption(77, 15));
                reward.itemOptions.add(new ItemOption(103, 15));
                reward.itemOptions.add(new ItemOption(14, 5));
                reward.itemOptions.add(new ItemOption(108, 5));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(50, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
                }
                break;
            case 1330:
                reward = ItemService.gI().createNewItem((short) 1330);
                reward.itemOptions.add(new ItemOption(50, 18));
                reward.itemOptions.add(new ItemOption(77, 18));
                reward.itemOptions.add(new ItemOption(103, 18));
                reward.itemOptions.add(new ItemOption(108, 10));
                reward.itemOptions.add(new ItemOption(94, 10));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(50, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
                }
                break;
            case 1329:
                reward = ItemService.gI().createNewItem((short) 1329);
                reward.itemOptions.add(new ItemOption(50, 20));
                reward.itemOptions.add(new ItemOption(77, 20));
                reward.itemOptions.add(new ItemOption(103, 20));
                reward.itemOptions.add(new ItemOption(108, 12));
                reward.itemOptions.add(new ItemOption(94, 12));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(50, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 13)));
                }
                break;
            case 1334:
                reward = ItemService.gI().createNewItem((short) 1334);
                reward.itemOptions.add(new ItemOption(50, 13));
                reward.itemOptions.add(new ItemOption(77, 13));
                reward.itemOptions.add(new ItemOption(103, 13));
                reward.itemOptions.add(new ItemOption(108, 5));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(50, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
                }
                break;
            case 1332:
                reward = ItemService.gI().createNewItem((short) 1332);
                reward.itemOptions.add(new ItemOption(50, 16));
                reward.itemOptions.add(new ItemOption(77, 16));
                reward.itemOptions.add(new ItemOption(103, 16));
                reward.itemOptions.add(new ItemOption(14, 5));
                reward.itemOptions.add(new ItemOption(5, 5));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(50, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 13)));
                }
                break;
            case 1331:
                reward = ItemService.gI().createNewItem((short) 1331);
                reward.itemOptions.add(new ItemOption(50, 17));
                reward.itemOptions.add(new ItemOption(77, 17));
                reward.itemOptions.add(new ItemOption(103, 17));
                reward.itemOptions.add(new ItemOption(108, 5));
                reward.itemOptions.add(new ItemOption(94, 5));
                reward.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(50, 100)) {
                    reward.itemOptions.add(new ItemOption(93, Util.nextInt(3, 13)));
                }
                break;
            default:
                reward = ItemService.gI().createNewItem((short) itemID, 1);
                break;
        }
        InventoryService.gI().addItemBag(pl, reward);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        Service.gI().sendThongBao(pl, "Bạn đã nhận được " + reward.template.name);
    }

    private void openWoodChest(Player pl, Item item) {
        int time = (int) TimeUtil.diffDate(new Date(), new Date(item.createTime), TimeUtil.DAY);
        if (time != 0) {
            Item itemReward;
            int param = item.itemOptions.get(0).param;
            if (param < 1) {
                param = 1; 
            }
            int gold;
            int[] listItem = {441, 442, 443, 444, 445, 446, 447, 223, 224, 225, 20, 19};
            int[] listClothesReward;
            int[] listItemReward;
            String text = "Bạn nhận được\n";
            if (param < 8) {
                gold = 1000000 * param;
                listClothesReward = new int[]{randClothes(param)};
                listItemReward = Util.pickNRandInArr(listItem, 3);
            } else if (param < 10) {
                gold = 1000000 * param;
                listClothesReward = new int[]{randClothes(param), randClothes(param)};
                listItemReward = Util.pickNRandInArr(listItem, 4);
            } else {
                gold = 1000000 * param;
                listClothesReward = new int[]{randClothes(param), randClothes(param), randClothes(param)};
                listItemReward = Util.pickNRandInArr(listItem, 5);
                int ruby = Util.nextInt(1, 5);
                pl.inventory.ruby += ruby;
                pl.textRuongGo.add(text + "|1| " + ruby + " Hồng Ngọc");
            }
            for (var i : listClothesReward) {
                itemReward = ItemService.gI().createNewItem((short) i);
                RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type,
                        itemReward.itemOptions);
                RewardService.gI().initStarOption(itemReward, new RewardService.RatioStar[]{
                    new RewardService.RatioStar((byte) 1, 1, 2), new RewardService.RatioStar((byte) 2, 1, 3),
                    new RewardService.RatioStar((byte) 3, 1, 4), new RewardService.RatioStar((byte) 4, 1, 5),});
                InventoryService.gI().addItemBag(pl, itemReward);
                pl.textRuongGo.add(text + itemReward.getInfoItem());
            }
            for (var i : listItemReward) {
                itemReward = ItemService.gI().createNewItem((short) i);
                RewardService.gI().initBaseOptionSaoPhaLe(itemReward);
                itemReward.quantity = Util.nextInt(1, 5);
                InventoryService.gI().addItemBag(pl, itemReward);
                pl.textRuongGo.add(text + itemReward.getInfoItem());
            }
            if (param == 11) {
                itemReward = ItemService.gI().createNewItem((short) ConstItem.MANH_NHAN);
                itemReward.quantity = Util.nextInt(1, 3);
                InventoryService.gI().addItemBag(pl, itemReward);
                pl.textRuongGo.add(text + itemReward.getInfoItem());
            }
            NpcService.gI().createMenuConMeo(pl, ConstNpc.RUONG_GO, -1,
                    "Bạn nhận được\n|1|+" + Util.numberToMoney(gold) + " vàng", "OK [" + pl.textRuongGo.size() + "]");
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            pl.inventory.addGold(gold);
            InventoryService.gI().sendItemBags(pl);
            PlayerService.gI().sendInfoHpMpMoney(pl);
        } else {
            Service.gI().sendThongBao(pl, "Vì bạn quên không lấy chìa nên cần đợi 24h để bẻ khóa");
        }
    }

    private void OpenDevNung(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] temp = {17, 18, 19, 20, 188, 189, 441, 442, 443, 444, 445, 446, 447, 190, 381, 382, 383, 384, 385,
                1392, 1393};
            int[][] gold = {{5000, 20000}};
            byte index = (byte) Util.nextInt(0, temp.length - 1);
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            if (index <= 3) {
                pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                }
                PlayerService.gI().sendInfoHpMpMoney(pl);
                icon[1] = 930;
            } else {
                Item it = ItemService.gI().createNewItem(temp[index]);
                if (it.template.id == 1392 || it.template.id == 1393) {
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 21)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 21)));
                    it.itemOptions.add(new ItemOption(14, Util.nextInt(1, 21)));
                } else {
                    it.itemOptions.add(new ItemOption(73, 0));
                }
                it.itemOptions.add(new ItemOption(73, 0));
                RewardService.gI().initBaseOptionSaoPhaLe(it);
                InventoryService.gI().addItemBag(pl, it);
                icon[1] = it.template.iconID;
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);

            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void changePet(Player player, Item item) {
        if (player.pet != null) {
            int gender = player.pet.gender + 1;
            if (gender > 2) {
                gender = 0;
            }
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
            PetService.gI().changeNormalPet(player, gender);
            InventoryService.gI().sendItemBags(player);
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    private void eatGrapes(Player pl, Item item) {
        int percentCurrentStatima = pl.nPoint.stamina * 100 / pl.nPoint.maxStamina;
        if (percentCurrentStatima > 50) {
            Service.gI().sendThongBao(pl, "Thể lực vẫn còn trên 50%");
            return;
        } else if (item.template.id == 211) {
            pl.nPoint.stamina = pl.nPoint.maxStamina;
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 100%");
        } else if (item.template.id == 212) {
            pl.nPoint.stamina += (pl.nPoint.maxStamina * 20 / 100);
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 20%");
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        PlayerService.gI().sendCurrentStamina(pl);
    }

    private void openCSKB(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] temp = {76, 188, 189, 190, 381, 382, 383, 384, 385};
            int[][] gold = {{5000, 20000}};
            byte index = (byte) Util.nextInt(0, temp.length - 1);
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            if (index <= 3) {
                pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                }
                PlayerService.gI().sendInfoHpMpMoney(pl);
                icon[1] = 930;
            } else {
                Item it = ItemService.gI().createNewItem(temp[index]);
                it.itemOptions.add(new ItemOption(73, 0));
                InventoryService.gI().addItemBag(pl, it);
                icon[1] = it.template.iconID;
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);

            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1427(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] temp = {1426};
            byte index = (byte) Util.nextInt(0, temp.length - 1);
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            Item it = ItemService.gI().createNewItem(temp[index]);
            InventoryService.gI().addItemBag(pl, it);
            Service.gI().sendThongBao(pl, "Bạn Nhận Được Quả Trứng Rồng Nhí");
            icon[1] = 15127;
            InventoryService.gI().subQuantityItemsBag(pl, item, 99);
            InventoryService.gI().sendItemBags(pl);
            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1428(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            int rand = Util.nextInt(100); // sử dụng hệ số 1000 để tăng độ chính xác

            if (rand < 50) { // 0.5% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1413);
                item2.itemOptions.add(new ItemOption(77, 15));
                item2.itemOptions.add(new ItemOption(108, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 50) { // 1% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1414);
                item2.itemOptions.add(new ItemOption(50, 15));
                item2.itemOptions.add(new ItemOption(14, 6));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                Service.gI().sendThongBao(pl, "Bạn không nhận được vật phẩm nào.");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                return;
            }
            // Thêm option ngẫu nhiên
            if (Util.nextInt(100) < 99) {
                item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
            }

            InventoryService.gI().addItemBag(pl, item2);
            Service.gI().sendThongBao(pl, "Bạn nhận được " + item2.template.name);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1429(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            int rand = Util.nextInt(1000); // sử dụng hệ số 1000 để tăng độ chính xác

            if (rand < 5) { // 0.5% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1416);
                item2.itemOptions.add(new ItemOption(50, 26));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                item2.itemOptions.add(new ItemOption(95, 10));
                item2.itemOptions.add(new ItemOption(97, 10));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 15) { // 1% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1113);
                item2.itemOptions.add(new Item.ItemOption(50, 23));
                item2.itemOptions.add(new Item.ItemOption(77, 22));
                item2.itemOptions.add(new Item.ItemOption(27, 22));
                item2.itemOptions.add(new Item.ItemOption(108, 5));
                item2.itemOptions.add(new Item.ItemOption(30, 0));
            } else if (rand < 65) { // 5% - hiếm hơn
                item2 = ItemService.gI().createNewItem((short) 1413);
                item2.itemOptions.add(new ItemOption(77, 17));
                item2.itemOptions.add(new ItemOption(108, 10));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 135) { // 7% - hiếm hơn
                item2 = ItemService.gI().createNewItem((short) 1414);
                item2.itemOptions.add(new ItemOption(50, 17));
                item2.itemOptions.add(new ItemOption(14, 11));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 385) { // 25% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1447);
                item2.itemOptions.add(new ItemOption(50, 14));
                item2.itemOptions.add(new ItemOption(77, 14));
                item2.itemOptions.add(new ItemOption(103, 14));
                item2.itemOptions.add(new ItemOption(97, 7));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 685) { // 30% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1448);
                item2.itemOptions.add(new ItemOption(50, 14));
                item2.itemOptions.add(new ItemOption(77, 14));
                item2.itemOptions.add(new ItemOption(103, 14));
                item2.itemOptions.add(new ItemOption(97, 7));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 985) { // 30% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1449);
                item2.itemOptions.add(new ItemOption(50, 14));
                item2.itemOptions.add(new ItemOption(77, 14));
                item2.itemOptions.add(new ItemOption(103, 14));
                item2.itemOptions.add(new ItemOption(97, 7));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                Service.gI().sendThongBao(pl, "Bạn không nhận được vật phẩm nào.");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                return;
            }
            // Thêm option ngẫu nhiên
            if (Util.nextInt(100) < 99) {
                item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
            }

            InventoryService.gI().addItemBag(pl, item2);
            Service.gI().sendThongBao(pl, "Bạn nhận được " + item2.template.name);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1426(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            int rand = Util.nextInt(1000); // sử dụng hệ số 1000 để tăng độ chính xác

            if (rand < 5) { // 0.5% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1418);
                item2.itemOptions.add(new ItemOption(50, 20));
                item2.itemOptions.add(new ItemOption(77, 20));
                item2.itemOptions.add(new ItemOption(103, 20));
                item2.itemOptions.add(new ItemOption(108, 12));
                item2.itemOptions.add(new ItemOption(94, 12));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 15) { // 1% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1419);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(103, 18));
                item2.itemOptions.add(new ItemOption(108, 10));
                item2.itemOptions.add(new ItemOption(94, 10));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 65) { // 5% - hiếm hơn
                item2 = ItemService.gI().createNewItem((short) 1420);
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(108, 7));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 135) { // 7% - hiếm hơn
                item2 = ItemService.gI().createNewItem((short) 1421);
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(5, 7));
                item2.itemOptions.add(new ItemOption(14, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 385) { // 25% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1422);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(94, 15));
                item2.itemOptions.add(new ItemOption(108, 7));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 685) { // 30% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1423);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(5, 7));
                item2.itemOptions.add(new ItemOption(14, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 985) { // 30% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1424);
                item2.itemOptions.add(new ItemOption(77, 16));
                item2.itemOptions.add(new ItemOption(50, 16));
                item2.itemOptions.add(new ItemOption(103, 16));
                item2.itemOptions.add(new ItemOption(236, 20));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                Service.gI().sendThongBao(pl, "Bạn không nhận được vật phẩm nào.");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                return;
            }
            // Thêm option ngẫu nhiên
            if (Util.nextInt(100) < 99) {
                item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
            }

            InventoryService.gI().addItemBag(pl, item2);
            Service.gI().sendThongBao(pl, "Bạn nhận được " + item2.template.name);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void skipToTask20(Player pl) {
        pl.playerTask.taskMain.id = 20;
        pl.playerTask.taskMain.index = 1;
        TaskService.gI().sendTaskMain(pl);
        Service.gI().sendThongBao(pl, "Đã chuyển bạn tới nhiệm vụ 20!");
    }

    private void openHopRandom1149(Player pl, Item item) {
        // Bắt buộc có chìa khoá id 1149 trong bag
        Item chia = InventoryService.gI().findItemBag(pl, 1149);
        if (chia == null || chia.quantity <= 0) {
            Service.gI().sendThongBao(pl, "Cần có chìa khóa để mở hộp!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Hành trang đã đầy");
            return;
        }
        int roll = Util.nextInt(0, 6);
        Item reward;
        switch (roll) {
            case 0 -> reward = ItemService.gI().createNewItem((short) 17);
            case 1 -> reward = ItemService.gI().createNewItem((short) 18);
            case 2 -> reward = ItemService.gI().createNewItem((short) 19);
            case 3 -> reward = ItemService.gI().createNewItem((short) 20);
            case 4 -> {
                reward = ItemService.gI().createNewItem((short) 1574);
                reward.itemOptions.add(new Item.ItemOption(50, 26));
                reward.itemOptions.add(new Item.ItemOption(77, 26));
                reward.itemOptions.add(new Item.ItemOption(103, 26));
                reward.itemOptions.add(new Item.ItemOption(5, 15));
                reward.itemOptions.add(new Item.ItemOption(93, 1));
            }
            case 5 -> {
                reward = ItemService.gI().createNewItem((short) 1585);
                reward.itemOptions.add(new Item.ItemOption(50, 13));
                reward.itemOptions.add(new Item.ItemOption(77, 13));
                reward.itemOptions.add(new Item.ItemOption(103, 13));
                reward.itemOptions.add(new Item.ItemOption(5, 5));
                reward.itemOptions.add(new Item.ItemOption(93, 1));
            }
            default -> {
                reward = ItemService.gI().createNewItem((short) 1547);
                reward.itemOptions.add(new Item.ItemOption(50, 13));
                reward.itemOptions.add(new Item.ItemOption(77, 13));
                reward.itemOptions.add(new Item.ItemOption(103, 13));
                reward.itemOptions.add(new Item.ItemOption(5, 5));
                reward.itemOptions.add(new Item.ItemOption(93, 1));
            }
        }
        if (reward == null || reward.template == null) {
            Service.gI().sendThongBao(pl, "Không thể mở hộp — item lỗi");
            return;
        }
        InventoryService.gI().addItemBag(pl, reward);
        InventoryService.gI().sendItemBags(pl);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1); // trừ 1 hộp (1150)
        InventoryService.gI().subQuantityItemsBag(pl, chia, 1); // trừ 1 chìa (1149)
        models.task.Task_Kol.tick(pl, 1, 37); // KOL 37 (mở 10 Rương id 1150)
        Service.gI().sendThongBao(pl, "Bạn nhận được 1 " + reward.template.name);
    }

    private void hopCaiTrangHit(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item caiTrang = ItemService.gI().createNewItem((short) 884);
            caiTrang.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
            caiTrang.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 15)));
            caiTrang.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
            caiTrang.itemOptions.add(new Item.ItemOption(14, Util.nextInt(5, 15)));
            caiTrang.itemOptions.add(new Item.ItemOption(5, Util.nextInt(20, 60)));
            if (Util.isTrue(900, 1000)) {
                caiTrang.itemOptions.add(new Item.ItemOption(93, Util.nextInt(3, 10)));
            }
            InventoryService.gI().addItemBag(pl, caiTrang);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            Service.gI().sendThongBao(pl, "Bạn nhận được 1 " + caiTrang.template.name);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }
    private void hopCaiTrangZeno(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item caiTrang = ItemService.gI().createNewItem((short) 1984);
            caiTrang.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 20)));
            caiTrang.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 50)));
            caiTrang.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 50)));
            // 1/1000 ra Vĩnh Viễn, 999/1000 ra có Hạn sử dụng 1-3 ngày (option 93)
            if (!Util.isTrue(1, 1000)) {
                caiTrang.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 3)));
            }
            InventoryService.gI().addItemBag(pl, caiTrang);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            Service.gI().sendThongBao(pl, "Bạn nhận được 1 " + caiTrang.template.name);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }
    private void hopCaiTrangGoku(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item caiTrang = ItemService.gI().createNewItem((short) 1717);
            caiTrang.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 25)));
            caiTrang.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 30)));
            caiTrang.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 30)));
            caiTrang.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 10)));
            caiTrang.itemOptions.add(new Item.ItemOption(5, Util.nextInt(20, 80)));
            // 1/1000 ra Vĩnh Viễn, 999/1000 ra có Hạn sử dụng 1-3 ngày (option 93)
            if (!Util.isTrue(1, 1000)) {
                caiTrang.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 3)));
            }
            InventoryService.gI().addItemBag(pl, caiTrang);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            Service.gI().sendThongBao(pl, "Bạn nhận được 1 " + caiTrang.template.name);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }
    private void hopVanBay(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] idVanBays = {1934, 1957, 1958, 1977};
            Item vanBay = ItemService.gI().createNewItem(idVanBays[Util.nextInt(0, idVanBays.length - 1)]);
            vanBay.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 20)));
            vanBay.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 20)));
            vanBay.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 20)));
            // 1/1000 ra Vĩnh Viễn, 999/1000 ra có Hạn sử dụng 1-3 ngày (option 93)
            if (!Util.isTrue(1, 1000)) {
                vanBay.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 3)));
            }
            InventoryService.gI().addItemBag(pl, vanBay);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            Service.gI().sendThongBao(pl, "Bạn nhận được 1 " + vanBay.template.name);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void hopCaiTrangHitVIP(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item caiTrang = ItemService.gI().createNewItem((short) 884);
            caiTrang.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 15)));
            caiTrang.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 15)));
            caiTrang.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 15)));
            caiTrang.itemOptions.add(new Item.ItemOption(14, Util.nextInt(5, 15)));
            int hs5 = Util.isTrue(1, 1000) ? Util.nextInt(65, 80) : Util.nextInt(20, 64);
            caiTrang.itemOptions.add(new Item.ItemOption(5, hs5));
            if (Util.isTrue(70, 100)) {
                caiTrang.itemOptions.add(new Item.ItemOption(93, Util.nextInt(7, 10)));
            }
            InventoryService.gI().addItemBag(pl, caiTrang);
            InventoryService.gI().sendItemBags(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            Service.gI().sendThongBao(pl, "Bạn nhận được 1 " + caiTrang.template.name);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void open1532(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1522);
                item2.itemOptions.add(new ItemOption(0, 7500));
                item2.itemOptions.add(new ItemOption(6, 10000));
                item2.itemOptions.add(new ItemOption(7, 10000));
                if (Util.isTrue(50, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1523);
                item2.itemOptions.add(new ItemOption(50, 25));
                item2.itemOptions.add(new ItemOption(14, 25));
                item2.itemOptions.add(new ItemOption(160, 20));
                if (Util.isTrue(50, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1524);
                item2.itemOptions.add(new ItemOption(5, 25));
                item2.itemOptions.add(new ItemOption(14, 15));
                item2.itemOptions.add(new ItemOption(94, 20));
                if (Util.isTrue(50, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                item2 = ItemService.gI().createNewItem((short) 1525);
                item2.itemOptions.add(new ItemOption(103, 25));
                item2.itemOptions.add(new ItemOption(77, 25));
                item2.itemOptions.add(new ItemOption(83, 0));
                if (Util.isTrue(50, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            }
            InventoryService.gI().addItemBag(pl, item2);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void open1530(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1524);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1523);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                item2.itemOptions.add(new ItemOption(162, 2));
                if (Util.isTrue(95, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                item2 = ItemService.gI().createNewItem((short) 1525);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            }
            InventoryService.gI().addItemBag(pl, item2);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void open1531(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1524);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1523);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                item2.itemOptions.add(new ItemOption(162, 2));
                if (Util.isTrue(95, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                item2 = ItemService.gI().createNewItem((short) 1525);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                }
                item2.itemOptions.add(new ItemOption(30, 0));
            }
            InventoryService.gI().addItemBag(pl, item2);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void useItemTime(Player pl, Item item) {
        // Chặn dùng đồng thời buff thường (381-385) và buff VIP (1099-1103) cùng loại
        if (isBuffLoaiKhac(pl, item.template.id)) {
            Service.gI().sendThongBao(pl, "Không thể dùng đang dùng buff cùng loại rồi!");
            return;
        }
        switch (item.template.id) {
            case 1397:
                pl.itemTime.lastTimeBuaTNSM = System.currentTimeMillis();
                pl.itemTime.isBuaTNSM = true;
                break;
            case 1357: // bình x3 tn,sm — 10 phút, hiện icon buff như cuồng nộ
            case 1358: // bình x5 tn,sm
            case 1359: // bình x7 tn,sm
                if (pl.itemTime.tnsmX > 1) {
                    // đang buff bình khác -> gỡ icon cũ rồi ghi đè buff mới
                    ItemTimeService.gI().removeItemTime(pl, models.item.ItemTime.iconTnsmX(pl.itemTime.tnsmX));
                }
                pl.itemTime.lastTimeTnsmX = System.currentTimeMillis();
                pl.itemTime.tnsmX = item.template.id == 1357 ? 3 : item.template.id == 1358 ? 5 : 7;
                Service.gI().sendThongBao(pl, "Tiềm năng và sức mạnh nhận được x" + pl.itemTime.tnsmX + " trong 10 phút");
                break;
            case 382: // bổ huyết
                pl.itemTime.lastTimeBoHuyet = System.currentTimeMillis();
                pl.itemTime.isUseBoHuyet = true;
                break;
            case 383: // bổ khí
                pl.itemTime.lastTimeBoKhi = System.currentTimeMillis();
                pl.itemTime.isUseBoKhi = true;
                break;
            case 1343: // bổ khí
                pl.itemTime.timemiakl = System.currentTimeMillis();
                pl.itemTime.miakl = true;
                break;
            case 1344: // bổ khí
                pl.itemTime.timemiathom = System.currentTimeMillis();
                pl.itemTime.miathom = true;
                break;
            case 1345: // bổ khí
                pl.itemTime.timemiasaurieng = System.currentTimeMillis();
                pl.itemTime.miasaurieng = true;
                break;
            case 384: // giáp xên
                pl.itemTime.lastTimeGiapXen = System.currentTimeMillis();
                pl.itemTime.isUseGiapXen = true;
                break;
            case 381: // cuồng nộ
                pl.itemTime.lastTimeCuongNo = System.currentTimeMillis();
                pl.itemTime.isUseCuongNo = true;
                Service.gI().point(pl);
                break;
            case 385: // ẩn danh
                pl.itemTime.lastTimeAnDanh = System.currentTimeMillis();
                pl.itemTime.isUseAnDanh = true;
                break;
            case 379: // máy dò capsule
                pl.itemTime.lastTimeUseMayDo = System.currentTimeMillis();
                pl.itemTime.isUseMayDo = true;
                break;
            case 1099:// cn
                pl.itemTime.lastTimeCuongNo2 = System.currentTimeMillis();
                pl.itemTime.isUseCuongNo2 = true;
                Service.gI().point(pl);

                break;
            case 1100:// bo huyet
                pl.itemTime.lastTimeBoHuyet2 = System.currentTimeMillis();
                pl.itemTime.isUseBoHuyet2 = true;
                break;
            case 764:
                pl.itemTime.lastTimeKhauTrang = System.currentTimeMillis();
                pl.itemTime.isKhauTrang = true;
                break;
            case 1136:
                pl.itemTime.lastTimeTnDeTu = System.currentTimeMillis();
                pl.itemTime.isTnDeTu = true;
                break;
            case 1101:// bo khi
                pl.itemTime.lastTimeBoKhi2 = System.currentTimeMillis();
                pl.itemTime.isUseBoKhi2 = true;
                break;
            case 1102:// gx
                pl.itemTime.lastTimeGiapXen2 = System.currentTimeMillis();
                pl.itemTime.isUseGiapXen2 = true;
                break;
            case 1103:// an danh
                pl.itemTime.lastTimeAnDanh2 = System.currentTimeMillis();
                pl.itemTime.isUseAnDanh2 = true;
                break;
            case 638: // Commeson
                pl.itemTime.lastTimeUseCMS = System.currentTimeMillis();
                pl.itemTime.isUseCMS = true;
                break;
            case 2160: // Nồi cơm điện
                pl.itemTime.lastTimeUseNCD = System.currentTimeMillis();
                pl.itemTime.isUseNCD = true;
                break;
            case 579:
            case 1045: // Đuôi khỉ
                pl.itemTime.lastTimeUseDK = System.currentTimeMillis();
                pl.itemTime.isUseDK = true;
                break;
            case 663: // bánh pudding
            case 664: // xúc xíc
            case 665: // kem dâu
            case 666: // mì ly
            case 667: // sushi
                pl.itemTime.lastTimeEatMeal = System.currentTimeMillis();
                pl.itemTime.isEatMeal = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal);
                pl.itemTime.iconMeal = item.template.iconID;
                break;
            case 880:
            case 881:
            case 882:
                pl.itemTime.lastTimeEatMeal2 = System.currentTimeMillis();
                pl.itemTime.isEatMeal2 = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal2);
                pl.itemTime.iconMeal2 = item.template.iconID;
                break;
            case 1109: // máy dò đồ
                pl.itemTime.lastTimeUseMayDo2 = System.currentTimeMillis();
                pl.itemTime.isUseMayDo2 = true;
                break;
            case 1137:
                pl.itemTime.lastTimeUseCo4La = System.currentTimeMillis();
                pl.itemTime.isUseCo4La = true;
                break;
            case 753:
                pl.itemTime.banhchunglastTime = System.currentTimeMillis();
                pl.itemTime.banhchung = true;
                break;
            case 752:
                pl.itemTime.banhtetlastTime = System.currentTimeMillis();
                pl.itemTime.banhtet = true;
                break;
            case 1261:
                pl.itemTime.lastTimeXimuoihoadao = System.currentTimeMillis();
                pl.itemTime.isXimuoihoadao = true;
                break;
            case 1262:
                pl.itemTime.lastTimeXimuoihoamai = System.currentTimeMillis();
                pl.itemTime.isXimuoihoamai = true;
                break;
            case 899:
                pl.itemTime.lastTimeKeoMotMat = System.currentTimeMillis();
                pl.itemTime.isKeoMotMat = true;
                Service.gI().sendThongBao(pl, "Bạn vừa sử dụng Kẹo một mắt: +5% chí mạng trong 30 phút!");
                break;
            case 900:
                pl.itemTime.lastTimeSupBiHacAm = System.currentTimeMillis();
                pl.itemTime.isSupBiHacAm = true;
                Service.gI().sendThongBao(pl, "Bạn vừa sử dụng Súp bí hắc ám: +10% HP trong 30 phút!");
                break;
            case 902:
                pl.itemTime.lastTimeBanhGatoNhen = System.currentTimeMillis();
                pl.itemTime.isBanhGatoNhen = true;
                Service.gI().sendThongBao(pl, "Bạn vừa sử dụng Bánh gato nhện: +10% giáp trong 30 phút!");
                break;
            case 903:
                pl.itemTime.lastTimeHamburgerSau = System.currentTimeMillis();
                pl.itemTime.isHamburgerSau = true;
                Service.gI().sendThongBao(pl, "Bạn vừa sử dụng Hamburger sâu: +10% sức đánh trong 30 phút!");
                break;
        }
        Service.gI().point(pl);
        ItemTimeService.gI().sendAllItemTime(pl);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
    }

    /**
     * Trả true nếu player đang dùng buff loại ĐỐI CHỌI với id item này.
     * Cặp: 381↔1099 (cuồng nộ), 382↔1100 (bổ huyết), 383↔1101 (bổ khí),
     *       384↔1102 (giáp xên), 385↔1103 (ẩn danh).
     */
    private boolean isBuffLoaiKhac(Player pl, int id) {
        switch (id) {
            case 381:  return pl.itemTime.isUseCuongNo2;
            case 382:  return pl.itemTime.isUseBoHuyet2;
            case 383:  return pl.itemTime.isUseBoKhi2;
            case 384:  return pl.itemTime.isUseGiapXen2;
            case 385:  return pl.itemTime.isUseAnDanh2;
            case 1099: return pl.itemTime.isUseCuongNo;
            case 1100: return pl.itemTime.isUseBoHuyet;
            case 1101: return pl.itemTime.isUseBoKhi;
            case 1102: return pl.itemTime.isUseGiapXen;
            case 1103: return pl.itemTime.isUseAnDanh;
        }
        return false;
    }

    private void openMenuUocRongZeno(Player pl) {
        NpcService.gI().createMenuConMeo(pl, ConstNpc.MENU_UOC_RONG_ZENO, -1,
                "Hãy chọn điều ước bạn mong muốn",
                "Tăng\n15% HP", "Tăng\n15% KI", "Tăng\n15% SĐ",
                "x10\nItem cấp 2", "Cải Trang\nZeno Lên\nThần Zeno\n(+20%)", "Đóng");
    }

    private void controllerCallRongThan(Player pl, Item item) {
        int tempId = item.template.id;
        if (tempId >= ConstItem.NGOC_ZENO_1_SAO && tempId <= ConstItem.NGOC_ZENO_7_SAO) {
            if (tempId != ConstItem.NGOC_ZENO_1_SAO) {
                Service.gI().sendThongBao(pl, "Hãy dùng Ngọc Rồng Zeno 1 sao để gọi rồng!");
                } else if (!InventoryService.gI().hasAllItemsBag(pl,
                    ConstItem.NGOC_ZENO_1_SAO, ConstItem.NGOC_ZENO_7_SAO)) {
                Service.gI().sendThongBao(pl, "Cần đủ 7 viên Ngọc Rồng Zeno mới gọi được rồng!");
            } else {
                openMenuUocRongZeno(pl);
            }
            return;
        }

        if (tempId >= SummonDragon.NGOC_RONG_1_SAO && tempId <= SummonDragon.NGOC_RONG_7_SAO || tempId == ConstItem.BI_NGO_1_SAO) {
            switch (tempId) {
                case SummonDragon.NGOC_RONG_1_SAO:
                case SummonDragon.NGOC_RONG_2_SAO:
                case SummonDragon.NGOC_RONG_3_SAO:
                    SummonDragon.gI().openMenuSummonShenron(pl, (byte) (tempId - 13), SummonDragon.DRAGON_SHENRON);
                    break;
                case ConstItem.BI_NGO_1_SAO:
                    SummonDragon.gI().openMenuSummonShenron(pl, (byte) 702, SummonDragon.DRAGON_BLACK_SHENRON);
                    break;
                default:
                    NpcService.gI().createMenuConMeo(pl, ConstNpc.TUTORIAL_SUMMON_DRAGON,
                            -1, "Bạn chỉ có thể gọi rồng từ ngọc 3 sao, 2 sao, 1 sao", "Hướng\ndẫn thêm\n(mới)", "OK");
                    break;
            }
        } else if (tempId >= ShenronEventService.NGOC_RONG_1_SAO && tempId <= ShenronEventService.NGOC_RONG_7_SAO) {
            ShenronEventService.gI().openMenuSummonShenron(pl, 0);
        }
    }

    private void learnSkill(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.id >= 1599 && item.template.id <= 1616 && !Skill.ENABLE_SKILL_BIEN_HINH) {
                Service.gI().sendThongBao(pl, "Kỹ năng này hiện đang tạm đóng trên máy chủ!");
                return;
            }
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                String[] subName = item.template.name.split("");
                byte level = Byte.parseByte(subName[subName.length - 1]);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                int maxPoint = (curSkill != null && curSkill.template != null && curSkill.template.maxPoint > 0)
                        ? curSkill.template.maxPoint : 7;
                if (curSkill != null && curSkill.point > 0 && curSkill.point >= maxPoint) {
                    Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                } else {
                    if (curSkill == null || curSkill.point == 0) {
                        if (level == 1) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level, pl.gender);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 23);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                            pl.BoughtSkills.add((int) item.template.id);
                        } else {
                            Skill skillNeed = SkillUtil
                                    .createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), (byte) 1, pl.gender);
                            Service.gI().sendThongBao(pl,
                                    "Vui lòng học " + (skillNeed != null && skillNeed.template != null ? skillNeed.template.name : "kỹ năng") + " cấp 1 trước!");
                        }
                    } else {
                        if (curSkill.point + 1 == level) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level, pl.gender);
                            // System.out.println(curSkill.template.name + " - " + curSkill.point);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 62);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                            pl.BoughtSkills.add((int) item.template.id);

                        } else {
                            Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " cấp "
                                    + (curSkill.point + 1) + " trước!");
                        }
                    }
                    InventoryService.gI().sendItemBags(pl);
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        }
    }

    private void useTDLT(Player pl, Item item) {
        if (pl.itemTime.isUseTDLT) {
            ItemTimeService.gI().turnOffTDLT(pl, item);
        } else {
            ItemTimeService.gI().turnOnTDLT(pl, item);
        }
    }

    private void usePorata3(Player pl) {
        // Check cơ bản
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return; // ⚡ thêm return để ngăn NullPointer
        }

        Item get1 = InventoryService.gI().findItem(pl.inventory.itemsBody, 1197);
        Item get2 = InventoryService.gI().findItem(pl.inventory.itemsBody, 1205);

        Item pet1 = null;
        Item pet2 = null;
        if (pl.pet.inventory != null) {
            pet1 = InventoryService.gI().findItem(pl.pet.inventory.itemsBody, 1197);
            pet2 = InventoryService.gI().findItem(pl.pet.inventory.itemsBody, 1205);
        }

        boolean cond1 = (pet2 != null && get1 != null && pl.fusion.typeFusion == ConstPlayer.NON_FUSION);
        boolean cond2 = (pet1 != null && get2 != null && pl.fusion.typeFusion == ConstPlayer.NON_FUSION);

        if (cond1 || cond2) {
            pl.pet.ggtblue(true);
        } else if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION
                && (get1 == null || get2 == null)) {
            pl.pet.fusion3(true);
        } else {
            pl.pet.unFusion();
        }
    }

    private void usePorata2(Player pl) {
        // Check cơ bản
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return; // ⚡ thêm return để tránh NullPointer
        }

        Item get1 = InventoryService.gI().findItem(pl.inventory.itemsBody, 1197);
        Item get2 = InventoryService.gI().findItem(pl.inventory.itemsBody, 1205);

        Item pet1 = null;
        Item pet2 = null;
        if (pl.pet.inventory != null) {
            pet1 = InventoryService.gI().findItem(pl.pet.inventory.itemsBody, 1197);
            pet2 = InventoryService.gI().findItem(pl.pet.inventory.itemsBody, 1205);
        }

        boolean cond1 = (pet2 != null && get1 != null && pl.fusion.typeFusion == ConstPlayer.NON_FUSION);
        boolean cond2 = (pet1 != null && get2 != null && pl.fusion.typeFusion == ConstPlayer.NON_FUSION);

        if (cond1 || cond2) {
            pl.pet.ggtblue(true);
        } else if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION && (get1 == null || get2 == null)) {
            pl.pet.fusion2(true);
        } else {
            pl.pet.unFusion();
        }
    }

    private void usePorata(Player pl) {
        // Check cơ bản
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return; // ⚡ thêm return để tránh NullPointer
        }

        Item get1 = InventoryService.gI().findItem(pl.inventory.itemsBody, 1197);
        Item get2 = InventoryService.gI().findItem(pl.inventory.itemsBody, 1205);

        Item pet1 = null;
        Item pet2 = null;
        if (pl.pet.inventory != null) {
            pet1 = InventoryService.gI().findItem(pl.pet.inventory.itemsBody, 1197);
            pet2 = InventoryService.gI().findItem(pl.pet.inventory.itemsBody, 1205);
        }

        boolean cond1 = (pet2 != null && get1 != null && pl.fusion.typeFusion == ConstPlayer.NON_FUSION);
        boolean cond2 = (pet1 != null && get2 != null && pl.fusion.typeFusion == ConstPlayer.NON_FUSION);

        if (cond1 || cond2) {
            pl.pet.ggtblue(true);
        } else if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION && (get1 == null || get2 == null)) {
            pl.pet.fusion(true);
        } else {
            pl.pet.unFusion();
        }
    }

    private void openCapsuleUI(Player pl) {
        if (services.tournament.GuruGuildWarService.gI().isInWar(pl)) {
            Service.gI().sendThongBao(pl, "Không thể sử dụng capsule khi đang trong Đấu Trường Bang Hội!");
            return;
        }
        pl.idMark.setTypeChangeMap(ConstMap.CHANGE_CAPSULE);
        ChangeMapService.gI().openChangeMapTab(pl);
    }

    public void choseMapCapsule(Player pl, int index) {

        if (pl.idNRNM != -1) {
            Service.gI().sendThongBao(pl, "Không thể mang ngọc rồng này lên Phi thuyền");
            Service.gI().hideWaitDialog(pl);
            return;
        }

        int zoneId = -1;
        if (index > pl.mapCapsule.size() - 1 || index < 0) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            Service.gI().hideWaitDialog(pl);
            return;
        }
        Zone zoneChose = pl.mapCapsule.get(index);
        // Kiểm tra số lượng người trong khu
        zoneChose = ChangeMapService.gI().checkMapCanJoin(pl, zoneChose);
        if (zoneChose == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            Service.gI().hideWaitDialog(pl);
            return;
        }
        if (zoneChose.getNumOfPlayers() > 25
                || MapService.gI().isMapDoanhTrai(zoneChose.map.mapId)
                || MapService.gI().isMapMaBu(zoneChose.map.mapId)
                || MapService.gI().isMapHuyDiet(zoneChose.map.mapId)) {
            Service.gI().sendThongBao(pl, "Hiện tại không thể vào được khu!");
            return;
        }
        if (index != 0 || zoneChose.map.mapId == 21
                || zoneChose.map.mapId == 22
                || zoneChose.map.mapId == 23) {
            pl.mapBeforeCapsule = pl.zone;
        } else {
            zoneId = pl.mapBeforeCapsule != null ? pl.mapBeforeCapsule.zoneId : -1;
            pl.mapBeforeCapsule = null;
        }
        pl.changeMapVIP = true;
        ChangeMapService.gI().changeMapBySpaceShip(pl, pl.mapCapsule.get(index).map.mapId, zoneId, -1);
    }

    public void eatPea(Player player) {
        if (!Util.canDoWithTime(player.lastTimeEatPea, 1000)) {
            return;
        }
        player.lastTimeEatPea = System.currentTimeMillis();
        Item pea = null;
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 6) {
                pea = item;
                break;
            }
        }
        if (pea != null) {
            int hpKiHoiPhuc = 0;
            int lvPea = Integer.parseInt(pea.template.name.substring(13));
            for (Item.ItemOption io : pea.itemOptions) {
                if (io.optionTemplate.id == 2) {
                    hpKiHoiPhuc = io.param * 1000;
                    break;
                }
                if (io.optionTemplate.id == 48) {
                    hpKiHoiPhuc = io.param;
                    break;
                }
            }
            player.nPoint.setHp(player.nPoint.hp + hpKiHoiPhuc);
            player.nPoint.setMp(player.nPoint.mp + hpKiHoiPhuc);
            PlayerService.gI().sendInfoHpMp(player);
            Service.gI().sendInfoPlayerEatPea(player);
            if (player.pet != null && player.zone != null && player.zone.equals(player.pet.zone) && !player.pet.isDie()) {
                int statima = 100 * lvPea;
                player.pet.nPoint.stamina += statima;
                if (player.pet.nPoint.stamina > player.pet.nPoint.maxStamina) {
                    player.pet.nPoint.stamina = player.pet.nPoint.maxStamina;
                }
                player.pet.nPoint.setHp(player.pet.nPoint.hp + hpKiHoiPhuc);
                player.pet.nPoint.setMp(player.pet.nPoint.mp + hpKiHoiPhuc);
                Service.gI().sendInfoPlayerEatPea(player.pet);
                Service.gI().chatJustForMe(player, player.pet, "Cám ơn sư phụ");
            }

            InventoryService.gI().subQuantityItemsBag(player, pea, 1);
            InventoryService.gI().sendItemBags(player);
        }
    }

    private void upSkillPet(Player pl, Item item) {
        if (pl.pet == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        try {
            switch (item.template.id) {
                case 402: // skill 1
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 0)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 403: // skill 2
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 1)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 404: // skill 3
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 2)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 759: // skill 4
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 3)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;

            }

        } catch (Exception e) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void ItemManhGiay(Player pl, Item item) {
        if (pl.winSTT && !Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            Service.gI().sendThongBao(pl, "Hãy gặp thần mèo Karin để sử dụng");
            return;
        } else if (pl.winSTT && Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            pl.winSTT = false;
            pl.callBossPocolo = false;
            pl.zoneSieuThanhThuy = null;
        }
        NpcService.gI().createMenuConMeo(pl, item.template.id, 564,
                "Đây chính là dấu hiệu riêng của...\nĐại Ma Vương Pôcôlô\nĐó là một tên quỷ dữ đội lốt người, một kẻ đại gian ác\ncó sức mạnh vô địch và lòng tham không đáy...\nĐối phó với hắn không phải dễ\nCon có chắc chắn muốn tìm hắn không?",
                "Đồng ý", "Từ chối");
    }

    private void ItemSieuThanThuy(Player pl, Item item) {
        long tnsm = 500_000;
        int n = 0;
        switch (item.template.id) {
            case 727:
                n = 1;
                break;
            case 728:
                n = 2;
                break;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        if (Util.isTrue(50, 100)) {
            Service.gI().sendThongBao(pl, "Bạn đã bị chết vì độc của thuốc tăng lực siêu thần thủy.");
            pl.setDie();
        } else {
            for (int i = 0; i < n; i++) {
                Service.gI().addSMTN(pl, (byte) 2, tnsm, true);
            }
        }
    }

    public void UseCard(Player pl, Item item) {
        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(c -> c.Id == item.template.id)
                .findFirst().orElse(null);
        if (radarTemplate == null) {
            return;
        }
        if (radarTemplate.Require != -1) {
            RadarCard radarRequireTemplate = RadarService.gI().RADAR_TEMPLATE.stream()
                    .filter(r -> r.Id == radarTemplate.Require).findFirst().orElse(null);
            if (radarRequireTemplate == null) {
                return;
            }
            Card cardRequire = pl.Cards.stream().filter(r -> r.Id == radarRequireTemplate.Id).findFirst().orElse(null);
            if (cardRequire == null || cardRequire.Level < radarTemplate.RequireLevel) {
                Service.gI().sendThongBao(pl, "Bạn cần sưu tầm " + radarRequireTemplate.Name + " ở cấp độ "
                        + radarTemplate.RequireLevel + " mới có thể sử dụng thẻ này");
                return;
            }
        }
        Card card = pl.Cards.stream().filter(r -> r.Id == item.template.id).findFirst().orElse(null);
        boolean isSpecial = (item.template.id == 1591 || item.template.id == 1592 || item.template.id == 1593);
        int maxAmount = isSpecial ? 100 : ((card != null && card.MaxAmount > 0) ? card.MaxAmount : radarTemplate.Max);
        if (maxAmount <= 0) {
            maxAmount = 1;
        }

        // Kiểm tra giới hạn: Đã đạt tối đa mảnh của thẻ
        if (card != null && card.Amount >= maxAmount) {
            Service.gI().sendThongBao(pl, "Bạn đã sưu tầm tối đa số mảnh của thẻ này (" + maxAmount + "/" + maxAmount + ")");
            return;
        }

        if (card == null) {
            byte initialAmount = 1;
            byte initialLevel;
            if (isSpecial) {
                // Cấp 1 là 10 thẻ, Cấp 2 là 50 thẻ, Cấp 3 là 100 thẻ
                initialLevel = (byte) (initialAmount >= 100 ? 3 : (initialAmount >= 50 ? 2 : (initialAmount >= 10 ? 1 : 0)));
            } else {
                initialLevel = (byte) (initialAmount >= maxAmount ? 2 : 1);
            }
            Card newCard = new Card(item.template.id, initialAmount, maxAmount, initialLevel, radarTemplate.Options);
            pl.Cards.add(newCard);
            RadarService.gI().RadarSetAmount(pl, newCard.Id, newCard.Amount, newCard.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, newCard.Id, newCard.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);
            if (newCard.Level > 0) {
                Service.gI().sendThongBao(pl, "Chúc mừng bạn đã đạt Cấp " + newCard.Level + " thẻ " + radarTemplate.Name + "!");
            }
            Service.gI().point(pl);
        } else {
            card.MaxAmount = maxAmount;
            byte oldLevel = card.Level;
            card.Amount++;
            if (card.Amount > maxAmount) {
                card.Amount = (byte) maxAmount;
            }
            byte newLevel;
            if (isSpecial) {
                if (card.Amount >= 100) {
                    newLevel = 3;
                } else if (card.Amount >= 50) {
                    newLevel = 2;
                } else if (card.Amount >= 10) {
                    newLevel = 1;
                } else {
                    newLevel = 0;
                }
            } else {
                newLevel = (byte) (card.Amount >= maxAmount ? 2 : 1);
            }
            card.Level = newLevel;

            RadarService.gI().RadarSetAmount(pl, card.Id, card.Amount, card.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, card.Id, card.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);

            if (newLevel > oldLevel) {
                if (isSpecial && newLevel == 3) {
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn đã nâng cấp thẻ " + radarTemplate.Name + " lên Cấp 3 (Tối đa)!");
                } else {
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn đã nâng cấp thẻ " + radarTemplate.Name + " lên Cấp " + newLevel + "!");
                }
            }
            Service.gI().point(pl);
        }
    }

    public static final int[][][] LIST_ITEM_CLOTHES = {
        // áo , quần , găng ,giày,rada
        // td -> nm -> xd
        {{0, 33, 3, 34, 136, 137, 138, 139, 230, 231, 232, 233, 555},
        {6, 35, 9, 36, 140, 141, 142, 143, 242, 243, 244, 245, 556},
        {21, 24, 37, 38, 144, 145, 146, 147, 254, 255, 256, 257, 562},
        {27, 30, 39, 40, 148, 149, 150, 151, 266, 267, 268, 269, 563},
        {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561}},
        {{1, 41, 4, 42, 152, 153, 154, 155, 234, 235, 236, 237, 557},
        {7, 43, 10, 44, 156, 157, 158, 159, 246, 247, 248, 249, 558},
        {22, 46, 25, 45, 160, 161, 162, 163, 258, 259, 260, 261, 564},
        {28, 47, 31, 48, 164, 165, 166, 167, 270, 271, 272, 273, 565},
        {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561}},
        {{2, 49, 5, 50, 168, 169, 170, 171, 238, 239, 240, 241, 559},
        {8, 51, 11, 52, 172, 173, 174, 175, 250, 251, 252, 253, 560},
        {23, 53, 26, 54, 176, 177, 178, 179, 262, 263, 264, 265, 566},
        {29, 55, 32, 56, 180, 181, 182, 183, 274, 275, 276, 277, 567},
        {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561}}
    };

    private void ItemSKH(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Hãy chọn một món quà", "Áo", "Quần", "Găng", "Giày",
                "Rada", "Từ Chối");
    }

    public void openHopSKHThuong(Player pl, Item item) {
        if (item == null || !item.isNotNullItem() || item.template.id != 1494 || item.quantity <= 0
                || !pl.inventory.itemsBag.contains(item)) return;
        if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
            Service.gI().sendThongBao(pl, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }
        NpcService.gI().createMenuConMeo(pl, ConstNpc.MENU_OPEN_HOP_SKH_THUONG_PLANET, -1,
                "|0|Hộp SKH thường\n|7|Hãy chọn hành tinh:\n"
                        + "Nhận ngẫu nhiên 1 món đồ kích hoạt từ vải thô đến thần linh.",
                "Trái Đất", "Namếc", "Xayda", "Từ chối");
    }

    public void openHopSKHHuyDiet(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
            Service.gI().sendThongBao(pl, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }
        NpcService.gI().createMenuConMeo(pl, ConstNpc.MENU_OPEN_HOP_SKH_HUY_DIET_PLANET, -1,
                "|0|Hộp SKH Hủy diệt\n|7|Hãy chọn hành tinh:",
                "Trái Đất", "Namếc", "Xayda", "Từ chối");
    }

    public void openCapsuleCellMax(Player pl, Item item) {
        if (pl.pet == null) {
            Service.gI().sendThongBao(pl, "Bạn chưa có đệ tử! Cần có đệ tử Jiren để nâng cấp lên Cell Max.");
            return;
        }
        if (pl.pet.typePet != 5) {
            Service.gI().sendThongBao(pl, "Đệ tử của bạn không phải là Jiren! Phải có đệ tử Jiren mới có thể mở Capsule Cell Max.");
            return;
        }
        NpcService.gI().createMenuConMeo(pl, ConstNpc.MENU_CHON_DE_CELL_MAX, 0,
                "Mở Capsule Cell Max nâng cấp Đệ tử Jiren thành Đệ tử Cell Max cực mạnh!\nChọn hành tinh cho Đệ tử Cell Max của bạn:\n(Trang bị và giới hạn sức mạnh của đệ tử sẽ được giữ nguyên)",
                "Trái đất", "Namek", "Xayda", "Từ chối");
    }

    public void ComfirmNhanVIP(Player player, boolean receivedPet) {
        if (InventoryService.gI().getCountEmptyBag(player) > 5) {
            Item itemqua;
            Item itemqua1;
            Item itemqua2;
            Item itemqua3;
            Item itemqua4;
            Item itemqua5;
            Item itemqua6;
            try {
                int time = 5;
                if (player.getSession().hasReceivedVIP) {
                    Service.gI().sendThongBao(player, "Bạn Đã Sở Hữu VIP Rồi!");
                    return;
                }
                if (player.getSession().sotien >= 50000) {
                    Service.gI().sendThongBao(player, "Tiến Hành Nhận\nVIP 1 \nSau " + time + " Giây!");
                    while (time > 0) {
                        time--;
                        Thread.sleep(1000);
                        Service.gI().sendThongBao(player, "|7|" + time);
                    }
                    if (!PlayerDAO.purchaseVip(player, 1)) {
                        Service.gI().sendThongBao(player, "Không thể thanh toán hoặc VIP đã được nhận. Vui lòng thử lại.");
                        return;
                    }
                    player.getSession().vip = 1;
                    Service.gI().sendMoney(player);
                    itemqua2 = ItemService.gI().createNewItem((short) 1327, 1);
                    itemqua2.itemOptions.add(new ItemOption(50, 5));
                    itemqua2.itemOptions.add(new ItemOption(77, 5));
                    itemqua2.itemOptions.add(new ItemOption(103, 5));
                    itemqua2.itemOptions.add(new ItemOption(30, 0));
                    itemqua3 = ItemService.gI().createNewItem((short) 447, 20);
                    itemqua3.itemOptions.add(new ItemOption(101, 5));
                    itemqua4 = ItemService.gI().createNewItem((short) 1104, 1);
                    itemqua4.itemOptions.add(new ItemOption(50, 25));
                    itemqua4.itemOptions.add(new ItemOption(101, 70));
                    itemqua4.itemOptions.add(new ItemOption(95, 15));
                    itemqua4.itemOptions.add(new ItemOption(30, 0));
                    itemqua4.itemOptions.add(new ItemOption(93, 15));
                    itemqua5 = ItemService.gI().createNewItem((short) 1305, 3);
                    itemqua5.itemOptions.add(new ItemOption(30, 0));
                    itemqua6 = ItemService.gI().createNewItem((short) 1282, 1);
                    itemqua6.itemOptions.add(new ItemOption(77, 8));
                    itemqua6.itemOptions.add(new ItemOption(103, 8));
                    itemqua6.itemOptions.add(new ItemOption(84, 20));
                    itemqua6.itemOptions.add(new ItemOption(93, 15));

                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua2.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua3.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua4.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua5.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua6.template.name);

                    InventoryService.gI().addItemBag(player, itemqua2);
                    InventoryService.gI().addItemBag(player, itemqua3);
                    InventoryService.gI().addItemBag(player, itemqua4);
                    InventoryService.gI().addItemBag(player, itemqua5);
                    InventoryService.gI().addItemBag(player, itemqua6);
                    InventoryService.gI().sendItemBags(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn Chưa Tiền Để Nhận Vip Nạp Này!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 6 ô trống hành trang");
        }
    }

    private void AddBuaTriTue5N(Player player) {
        long currentTime = System.currentTimeMillis();
        if (player.charms.tdTriTue3 < currentTime) {
            player.charms.tdTriTue3 = currentTime;
        }
        long Day5 = 5 * 24 * 60 * 60 * 1000L;
        player.charms.tdTriTue3 += Day5;
        Service.gI().sendThongBao(player, "Bạn đã nhận Bùa X3 trong 5 ngày!");
    }

    public void ComfirmNhanVIP2(Player player, boolean receivedPet) {
        if (InventoryService.gI().getCountEmptyBag(player) > 5) {
            Item itemqua;
            Item itemqua1;
            Item itemqua2;
            Item itemqua3;
            Item itemqua4;
            Item itemqua5;
            Item itemqua6;

            try {
                int time = 5;
                if (player.getSession().hasReceivedVIP1) {
                    Service.gI().sendThongBao(player, "Bạn Đã Sở Hữu VIP Rồi!");
                    return;
                }
                if (player.getSession().sotien >= 200000) {
                    Service.gI().sendThongBao(player, "Tiến Hành Nhận\nVIP 2 \nSau " + time + " Giây!");
                    while (time > 0) {
                        time--;
                        Thread.sleep(1000);
                        Service.gI().sendThongBao(player, "|7|" + time);
                    }
                    if (!PlayerDAO.purchaseVip(player, 2)) {
                        Service.gI().sendThongBao(player, "Không thể thanh toán hoặc VIP đã được nhận. Vui lòng thử lại.");
                        return;
                    }
                    player.getSession().vip1 = 1;
                    Service.gI().sendMoney(player);

                    itemqua = ItemService.gI().createNewItem((short) 1326, 1);
                    itemqua.itemOptions.add(new ItemOption(50, 8));
                    itemqua.itemOptions.add(new ItemOption(77, 8));
                    itemqua.itemOptions.add(new ItemOption(103, 8));
                    itemqua.itemOptions.add(new ItemOption(30, 0));

                    itemqua1 = ItemService.gI().createNewItem((short) 987, 10);

                    itemqua2 = ItemService.gI().createNewItem((short) 1282, 1);
                    itemqua2.itemOptions.add(new ItemOption(77, 8));
                    itemqua2.itemOptions.add(new ItemOption(103, 8));
                    itemqua2.itemOptions.add(new ItemOption(84, 20));

                    itemqua3 = ItemService.gI().createNewItem((short) 1104, 1);
                    itemqua3.itemOptions.add(new ItemOption(50, 25));
                    itemqua3.itemOptions.add(new ItemOption(101, 70));
                    itemqua3.itemOptions.add(new ItemOption(95, 15));
                    itemqua3.itemOptions.add(new ItemOption(30, 0));

                    itemqua4 = ItemService.gI().createNewItem((short) 918, 1);
                    itemqua4.itemOptions.add(new ItemOption(50, 11));
                    itemqua4.itemOptions.add(new ItemOption(77, 12));
                    itemqua4.itemOptions.add(new ItemOption(103, 12));
                    itemqua4.itemOptions.add(new ItemOption(5, 15));
                    itemqua4.itemOptions.add(new ItemOption(30, 1));

                    itemqua5 = ItemService.gI().createNewItem((short) 956, 10);

                    itemqua6 = ItemService.gI().createNewItem((short) 1305, 5);

                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua1.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua2.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua3.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua4.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua5.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua6.template.name);


                    InventoryService.gI().addItemBag(player, itemqua);
                    InventoryService.gI().addItemBag(player, itemqua1);
                    InventoryService.gI().addItemBag(player, itemqua2);
                    InventoryService.gI().addItemBag(player, itemqua3);
                    InventoryService.gI().addItemBag(player, itemqua4);
                    InventoryService.gI().addItemBag(player, itemqua5);
                    InventoryService.gI().addItemBag(player, itemqua6);
                    InventoryService.gI().sendItemBags(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn Chưa Đủ Tiền Để Nhận Vip Này!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 8 ô trống hành trang");
        }
    }

    private void AddBuaTriTue10N(Player player) {
        long currentTime = System.currentTimeMillis();
        if (player.charms.tdTriTue3 < currentTime) {
            player.charms.tdTriTue3 = currentTime;
        }
        long Day10 = 10 * 24 * 60 * 60 * 1000L;
        player.charms.tdTriTue3 += Day10;
        Service.gI().sendThongBao(player, "Bạn đã nhận Bùa X3 trong 10 ngày!");
    }

    public void ComfirmNhanVIP3(Player player, boolean receivedPet) {
        if (InventoryService.gI().getCountEmptyBag(player) > 10) {
            Item itemqua;
            Item itemqua1;
            Item itemqua2;
            Item itemqua3;
            Item itemqua4;
            Item itemqua5;
            Item itemqua6;
            Item itemqua7;
            Item itemqua8;

            try {
                int time = 5;
                if (player.getSession().hasReceivedVIP2) {
                    Service.gI().sendThongBao(player, "Bạn Đã Sở Hữu VIP Rồi!");
                    return;
                }
                if (player.getSession().sotien >= 500000) {
                    Service.gI().sendThongBao(player, "Tiến Hành Nhận\nVIP 3 \nSau " + time + " Giây!");
                    while (time > 0) {
                        time--;
                        Thread.sleep(1000);
                        Service.gI().sendThongBao(player, "|7|" + time);
                    }
                    if (!PlayerDAO.purchaseVip(player, 3)) {
                        Service.gI().sendThongBao(player, "Không thể thanh toán hoặc VIP đã được nhận. Vui lòng thử lại.");
                        return;
                    }
                    player.getSession().vip2 = 1;
                    Service.gI().sendMoney(player);

                    itemqua = ItemService.gI().createNewItem((short) 1325, 1);
                    itemqua.itemOptions.add(new ItemOption(50, 10));
                    itemqua.itemOptions.add(new ItemOption(77, 10));
                    itemqua.itemOptions.add(new ItemOption(103, 10));
                    itemqua.itemOptions.add(new ItemOption(30, 0));

                    itemqua1 = ItemService.gI().createNewItem((short) 987, 30);

                    itemqua2 = ItemService.gI().createNewItem((short) 1317, 1);
                    itemqua2.itemOptions.add(new ItemOption(50, 15));
                    itemqua2.itemOptions.add(new ItemOption(77, 10));
                    itemqua2.itemOptions.add(new ItemOption(103, 10));

                    itemqua3 = ItemService.gI().createNewItem((short) 1316, 1);
                    itemqua3.itemOptions.add(new ItemOption(50, 30));
                    itemqua3.itemOptions.add(new ItemOption(77, 28));
                    itemqua3.itemOptions.add(new ItemOption(103, 28));
                    itemqua3.itemOptions.add(new ItemOption(101, 50));
                    itemqua3.itemOptions.add(new ItemOption(106, 0));

                    itemqua4 = ItemService.gI().createNewItem((short) 1127, 1);
                    itemqua4.itemOptions.add(new ItemOption(50, 15));
                    itemqua4.itemOptions.add(new ItemOption(77, 13));
                    itemqua4.itemOptions.add(new ItemOption(103, 13));
                    itemqua4.itemOptions.add(new ItemOption(14, 5));
                    itemqua4.itemOptions.add(new ItemOption(5, 10));
                    itemqua4.itemOptions.add(new ItemOption(30, 1));

                    itemqua5 = ItemService.gI().createNewItem((short) 1328, 1);
                    itemqua5.itemOptions.add(new ItemOption(50, 12));
                    itemqua5.itemOptions.add(new ItemOption(77, 16));
                    itemqua5.itemOptions.add(new ItemOption(103, 16));

                    itemqua6 = ItemService.gI().createNewItem((short) 956, 20);

                    itemqua7 = ItemService.gI().createNewItem((short) 1305, 10);

                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua1.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua2.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua3.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua4.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua5.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua6.template.name);
                    Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua7.template.name);

                    itemqua8 = ItemService.gI().createNewItem((short) 16, 10);

                    InventoryService.gI().addItemBag(player, itemqua);
                    InventoryService.gI().addItemBag(player, itemqua1);
                    InventoryService.gI().addItemBag(player, itemqua2);
                    InventoryService.gI().addItemBag(player, itemqua3);
                    InventoryService.gI().addItemBag(player, itemqua4);
                    InventoryService.gI().addItemBag(player, itemqua5);
                    InventoryService.gI().addItemBag(player, itemqua6);
                    InventoryService.gI().addItemBag(player, itemqua7);
                    InventoryService.gI().addItemBag(player, itemqua8);

                    InventoryService.gI().sendItemBags(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn Chưa Đủ Tiền Để Nhận Vip Này!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 10 ô trống hành trang");
        }
    }
}

// private void AddBuaTriTue30N(Player player) {
// long currentTime = System.currentTimeMillis();
// if (player.charms.tdTriTue3 < currentTime) {
// player.charms.tdTriTue3 = currentTime;
// }
// long Day10 = 30 * 24 * 60 * 60 * 1000L;
// player.charms.tdTriTue3 += Day10;
// Service.gI().sendThongBao(player, "Bạn đã nhận Bùa X3 trong 30 ngày!");
// }
//
// private void AddBuaTriTue50N(Player player) {
// long currentTime = System.currentTimeMillis();
// if (player.charms.tdTriTue3 < currentTime) {
// player.charms.tdTriTue3 = currentTime;
// }
// long Day10 = 50 * 24 * 60 * 60 * 1000L;
// player.charms.tdTriTue3 += Day10;
// Service.gI().sendThongBao(player, "Bạn đã nhận Bùa X3 trong 50 ngày!");
// }
//
// public void ComfirmNhanVIP4(Player player, boolean receivedPet) {
// if (InventoryService.gI().getCountEmptyBag(player) > 5) {
// Item itemqua;
// Item itemqua1;
// Item itemqua2;
// Item itemqua3;
// Item itemqua4;
// Item itemqua5;
// Item itemqua6 = null;
// Item itemqua7;
// Item itemqua8;
// Item itemqua9;
// Item itemqua10;
// Item itemqua11;
// try {
// int time = 5;
// if (player.getSession().hasReceivedVIP) {
// Service.gI().sendThongBao(player, "Bạn Đã Sở Hữu VIP Rồi!");
// return;
// }
// if (player.getSession().sotien >= 400000) {
// Service.gI().sendThongBao(player, "Tiến Hành Nhận\nVIP 4 \nSau " + time + "
// Giây!");
// while (time > 0) {
// time--;
// Thread.sleep(1000);
// Service.gI().sendThongBao(player, "|7|" + time);
// }
// PlayerDAO.subVIP(player);
// PlayerDAO.subvnd(player, 400000);
// player.getSession().vip = 4;
// Service.gI().sendMoney(player);
//
// itemqua1 = ItemService.gI().createNewItem((short) 457, 30);
// itemqua1.itemOptions.add(new ItemOption(100, 1));
// itemqua1.itemOptions.add(new ItemOption(86, 0));
//
// itemqua2 = ItemService.gI().createNewItem((short) 956, 15);
//
// int randomda = new int[]{1079, 1080, 1081, 1082, 1083, 220, 221, 222, 223,
// 224}[Util.nextInt(10)];
// itemqua3 = ItemService.gI().createNewItem((short) randomda, 15);
// itemqua4 = ItemService.gI().createNewItem((short) 987, 50);
// itemqua5 = ItemService.gI().createNewItem((short) 1187, 50);
// switch (player.gender) {
// case 0:
// itemqua6 = ItemService.gI().createNewItem((short) 1227, 2);
// break;
// case 1:
// itemqua6 = ItemService.gI().createNewItem((short) 1228, 2);
// break;
// case 2:
// itemqua6 = ItemService.gI().createNewItem((short) 1229, 2);
// break;
// default:
// break;
// }
// if (itemqua6 != null) {
// InventoryService.gI().addItemBag(player, itemqua6);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua6.template.name);
// } else {
// Service.gI().sendThongBao(player, "Không thể tạo vật phẩm theo hành tinh!");
// }
// itemqua7 = ItemService.gI().createNewItem((short) 1283, 1);
// itemqua7.itemOptions.add(new ItemOption(50, 27));
// itemqua7.itemOptions.add(new ItemOption(77, 25));
// itemqua7.itemOptions.add(new ItemOption(103, 25));
// itemqua7.itemOptions.add(new ItemOption(5, 15));
// itemqua7.itemOptions.add(new ItemOption(101, 100));
// itemqua7.itemOptions.add(new ItemOption(95, 10));
// itemqua7.itemOptions.add(new ItemOption(30, 1));
//
// itemqua8 = ItemService.gI().createNewItem((short) 1284, 1);
// itemqua8.itemOptions.add(new ItemOption(77, 15));
// itemqua8.itemOptions.add(new ItemOption(103, 15));
// itemqua8.itemOptions.add(new ItemOption(14, 9));
// itemqua8.itemOptions.add(new ItemOption(84, 0));
// itemqua8.itemOptions.add(new ItemOption(30, 1));
//
// itemqua9 = ItemService.gI().createNewItem((short) 1199, 20);
//
// itemqua10 = ItemService.gI().createNewItem((short) 1115, 1);
// itemqua10.itemOptions.add(new ItemOption(50, 28));
// itemqua10.itemOptions.add(new ItemOption(77, 27));
// itemqua10.itemOptions.add(new ItemOption(103, 27));
// itemqua10.itemOptions.add(new ItemOption(14, 15));
// itemqua10.itemOptions.add(new ItemOption(95, 10));
// itemqua10.itemOptions.add(new ItemOption(93, 365));
// itemqua10.itemOptions.add(new ItemOption(30, 1));
// itemqua11 = ItemService.gI().createNewItem((short) 1127, 1);
// itemqua11.itemOptions.add(new ItemOption(50, 15));
// itemqua11.itemOptions.add(new ItemOption(77, 13));
// itemqua11.itemOptions.add(new ItemOption(103, 13));
// itemqua11.itemOptions.add(new ItemOption(14, 5));
// itemqua11.itemOptions.add(new ItemOption(5, 10));
// itemqua11.itemOptions.add(new ItemOption(30, 1));
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua1.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua2.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua3.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua4.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua5.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua7.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua8.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua9.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua10.template.name);
// Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " +
// itemqua11.template.name);
// InventoryService.gI().addItemBag(player, itemqua1);
// InventoryService.gI().addItemBag(player, itemqua2);
// InventoryService.gI().addItemBag(player, itemqua3);
// InventoryService.gI().addItemBag(player, itemqua4);
// InventoryService.gI().addItemBag(player, itemqua5);
// InventoryService.gI().addItemBag(player, itemqua7);
// InventoryService.gI().addItemBag(player, itemqua8);
// InventoryService.gI().addItemBag(player, itemqua9);
// InventoryService.gI().addItemBag(player, itemqua10);
// InventoryService.gI().addItemBag(player, itemqua11);
// InventoryService.gI().sendItemBags(player);
// } else {
// Service.gI().sendThongBao(player, "Bạn Chưa Đủ Tiền Để Nhận Vip Này!");
// }
// } catch (Exception e) {
// e.printStackTrace();
// }
// } else {
// Service.gI().sendThongBao(player, "Bạn phải có ít nhất 6 ô trống hành
// trang");
// }
// }
//
// }
