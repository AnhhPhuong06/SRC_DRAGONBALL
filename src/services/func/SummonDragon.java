package services.func;

import java.util.HashMap;
import java.util.Map;
import models.item.Item;
import consts.ConstItem;
import consts.ConstNpc;
import consts.ConstPlayer;
import models.item.Item.ItemOption;
import models.map.Zone;
import services.map.NpcService;
import models.player.Inventory;
import models.player.Player;
import services.Service;
import utils.Util;
import network.io.Message;
import services.ItemService;
import services.player.PlayerService;
import services.player.InventoryService;
import utils.Logger;
import java.util.List;
import server.Maintenance;

public class SummonDragon {

    public static final byte WISHED = 0;
    public static final byte TIME_UP = 1;

    public static final byte DRAGON_SHENRON = 0;
    public static final byte DRAGON_PORUNGA = 1;
    public static final byte DRAGON_BLACK_SHENRON = 2;

    public static final short NGOC_RONG_1_SAO = 14;
    public static final short NGOC_RONG_2_SAO = 15;
    public static final short NGOC_RONG_3_SAO = 16;
    public static final short NGOC_RONG_4_SAO = 17;
    public static final short NGOC_RONG_5_SAO = 18;
    public static final short NGOC_RONG_6_SAO = 19;
    public static final short NGOC_RONG_7_SAO = 20;

    public static final short[] NGOC_RONG_DEN = {702, 703, 704, 705, 706, 707, 708};

    public static final String SUMMON_SHENRON_TUTORIAL = "Có 3 cách gọi rồng thần. Gọi từ ngọc 1 sao, gọi từ ngọc 2 sao, hoặc gọi từ ngọc 3 sao\n"
            + "Các ngọc 4 sao đến 7 sao không thể gọi rồng thần được\n"
            + "Để gọi rồng 1 sao cần ngọc từ 1 sao đến 7 sao\n"
            + "Để gọi rồng 2 sao cần ngọc từ 2 sao đến 7 sao\n"
            + "Để gọi rồng 3 sao cần ngọc từ 3 sao đến 7sao\n"
            + "Điều ước rồng 3 sao: Capsule 3 sao, hoặc 2 triệu sức mạnh, hoặc 200k vàng\n"
            + "Điều ước rồng 2 sao: Capsule 2 sao, hoặc 20 triệu sức mạnh, hoặc 2 triệu vàng\n"
            + "Điều ước rồng 1 sao: Capsule 1 sao, hoặc 200 triệu sức mạnh, hoặc 20 triệu vàng, hoặc đẹp trai, hoặc....\n"
            + "Ngọc rồng sẽ mất ngay khi gọi rồng dù bạn có ước hay không\n"
            + "Quá 5 phút nếu không ước rồng thần sẽ bay mất";
    public static final String SHENRON_SAY = "Ta sẽ ban cho người 1 điều ước, ngươi có 5 phút, hãy suy nghĩ thật kỹ trước khi quyết định";

    public static final String[] SHENRON_1_STAR_WISHES_1 = new String[]{"Giàu có\n+2 Tỏi\nVàng",
        "Găng tay\nđang mang\nlên 1 cấp", "Chí mạng\nGốc +2%",
        "Thay\nChiêu 2-3\nĐệ tử", "Điều ước\nkhác"};
    public static final String[] SHENRON_1_STAR_WISHES_2 = new String[]{"Đẹp trai\nnhất\nVũ trụ",
        "Giàu có\n+10K\nNgọc",
        "Găng tay đệ\nđang mang\nlên 1 cấp",
        "Điều ước\nkhác"};
    public static final String[] SHENRON_2_STARS_WHISHES = new String[]{"Giàu có\n+2K\nNgọc",
        "Giàu có\n+200 Tr\nVàng"};
    public static final String[] SHENRON_3_STARS_WHISHES = new String[]{"Giàu có\n+200\nNgọc",
        "Giàu có\n+20 Tr\nVàng"};

    public static final String[] SHEN_RON_BI_NGO_WISHES = new String[]{"Đổi kỹ năng 2\n Đệ tử",
        "Đổi kỹ năng 3\n Đệ tử", "Đổi kỹ năng 4\n Đệ tử", "X10 \n Item Siêu cấp"};
    // --------------------------------------------------------------------------
    private static SummonDragon instance;
    private final Map pl_dragonStar;
    private long lastTimeShenronAppeared;
    private long lastTimeShenronWait;
    private final int timeResummonShenron = 600000;
    private boolean isShenronAppear;
    private final int timeShenronWait = 300000;

    private final Thread update;
    private boolean active;

    public boolean isPlayerDisconnect;
    public Player playerSummonShenron;
    private int playerSummonShenronId;
    private Zone mapShenronAppear;
    private byte shenronStar;
    private int menuShenron;
    private byte select;

    private SummonDragon() {
        this.pl_dragonStar = new HashMap<>();
        this.update = new Thread(() -> {
            while (active && !Maintenance.isRunning) {
                try {
                    if (isShenronAppear) {
                        if (isPlayerDisconnect) {
                            List<Player> players = mapShenronAppear.getPlayers();
                            for (Player plMap : players) {
                                if (plMap.isPl() && plMap.id == playerSummonShenronId) {
                                    playerSummonShenron = plMap;
                                    reSummonShenron();
                                    isPlayerDisconnect = false;
                                    break;
                                }
                            }

                        }
                        if (Util.canDoWithTime(lastTimeShenronWait, timeShenronWait)) {
                            shenronLeave(playerSummonShenron, TIME_UP);
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Logger.logException(SummonDragon.class, e);
                }
            }
        });
        this.active();
    }

    private void active() {
        if (!active) {
            active = true;
            this.update.start();
        }
    }

    public static SummonDragon gI() {
        if (instance == null) {
            instance = new SummonDragon();
        }
        return instance;
    }

    public void openMenuSummonShenron(Player pl, byte dragonBallStar, byte Dragon) {
        switch (Dragon) {
            case DRAGON_SHENRON:
                this.pl_dragonStar.put(pl, dragonBallStar);
                NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_SHENRON, -1, "Bạn muốn gọi rồng thần ?",
                        "Hướng\ndẫn thêm\n(mới)", "Gọi\nRồng Thần\n" + dragonBallStar + " Sao");
                break;
            case DRAGON_BLACK_SHENRON:
                this.pl_dragonStar.put(pl, dragonBallStar);
                NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_BLACK_SHENRON, -1, "Bạn muốn gọi Rồng Bí ngô ?",
                        "Đồng ý", "Từ chối");
                break;
        }
    }

    public void summonBlackShenron(Player pl) {
        if (pl.zone.map.mapId == 5) {
            if (checkShenronBall(pl, DRAGON_BLACK_SHENRON)) {
                if (isShenronAppear) {
                    Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    return;
                }
                if (Util.canDoWithTime(lastTimeShenronAppeared, timeResummonShenron)) {
                    playerSummonShenron = pl;
                    playerSummonShenronId = (int) pl.id;
                    mapShenronAppear = pl.zone;
                    for (int i : NGOC_RONG_DEN) {
                        InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBagByTemp(pl, i),
                                1);
                    }
                    InventoryService.gI().sendItemBags(pl);
                    sendNotifyShenronAppear();
                    isShenronAppear = true;
                    activeShenron(pl, true, (byte) 1);
                    sendWhishesShenron(pl);
                } else {
                    int timeLeft = (int) ((timeResummonShenron - (System.currentTimeMillis() - lastTimeShenronAppeared))
                            / 1000);
                    Service.gI().sendThongBao(pl,
                            "Vui lòng đợi " + (timeLeft < 60 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
                }
            }
        } else {
            Service.gI().sendThongBao(pl, "Chỉ được gọi rồng bí ngô ở Đảo Kame");
        }
    }

    public void summonShenron(Player pl) {
        if (pl.zone.map.mapId == 0 || pl.zone.map.mapId == 7 || pl.zone.map.mapId == 14) {
            if (checkShenronBall(pl)) {
                if (isShenronAppear) {
                    Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    return;
                }

                if (Util.canDoWithTime(lastTimeShenronAppeared, timeResummonShenron)) {
                    // gọi rồng
                    playerSummonShenron = pl;
                    playerSummonShenronId = (int) pl.id;
                    mapShenronAppear = pl.zone;
                    byte dragonStar = (byte) pl_dragonStar.get(playerSummonShenron);
                    if (dragonStar == 1) { // KOL 5/15/25 (ước Ngọc Rồng 1 sao)
                        models.task.Task_Kol.tick(pl, 1, 5, 15, 25);
                    }
                    int begin = NGOC_RONG_1_SAO;
                    switch (dragonStar) {
                        case 2:
                            begin = NGOC_RONG_2_SAO;
                            break;
                        case 3:
                            begin = NGOC_RONG_3_SAO;
                            break;
                    }
                    for (int i = begin; i <= NGOC_RONG_7_SAO; i++) {
                        try {
                            InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, i), 1);
                            if (playerSummonShenron.playerTask.taskdh.Shenron < 10) {
                                int required = 10;
                                int percentDone = (int) ((double) playerSummonShenron.playerTask.taskdh.Shenron
                                        / required * 100);
                                playerSummonShenron.playerTask.taskdh.Shenron++;
                                playerSummonShenron.playerTask.taskdh.ResetTime = System.currentTimeMillis();
                                Service.gI().sendThongBao(playerSummonShenron,
                                        "Tiến độ hiện tại: " + percentDone + "%");

                            }
                        } catch (Exception ex) {
                        }
                    }
                    InventoryService.gI().sendItemBags(pl);
                    sendNotifyShenronAppear();
                    activeShenron(pl, true, SummonDragon.DRAGON_SHENRON);
                    sendWhishesShenron(pl);
                } else {
                    int timeLeft = (int) ((timeResummonShenron - (System.currentTimeMillis() - lastTimeShenronAppeared))
                            / 1000);
                    Service.gI().sendThongBao(pl, "Vui lòng đợi "
                            + (timeLeft < 7200 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
                }
            }
        } else {
            Service.gI().sendThongBao(pl, "Chỉ được gọi rồng thần ở ngôi làng trước nhà");
        }
    }

    private void reSummonShenron() {
        activeShenron(playerSummonShenron, true, SummonDragon.DRAGON_SHENRON);
        sendWhishesShenron(playerSummonShenron);
    }

    private void sendWhishesShenron(Player pl) {
        byte dragonStar;
        try {
            dragonStar = (byte) pl_dragonStar.get(pl);
            this.shenronStar = dragonStar;
        } catch (Exception e) {
            dragonStar = this.shenronStar;
        }
        switch (dragonStar) {
            case 1:
                pl.effect.addPointTrumUocRong();
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                break;
            case 2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_2, SHENRON_SAY, SHENRON_2_STARS_WHISHES);
                break;
            case 3:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_3, SHENRON_SAY, SHENRON_3_STARS_WHISHES);
                break;
            case (byte) ConstItem.BI_NGO_1_SAO:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHEN_RON_BI_NGO, SHENRON_SAY, SHEN_RON_BI_NGO_WISHES);
                break;
        }
    }

    private void activeShenron(Player pl, boolean appear, byte type) {
        Message msg;
        try {
            msg = new Message(-83);
            msg.writer().writeByte(appear ? 0 : (byte) 1);
            if (appear) {
                msg.writer().writeShort(pl.zone.map.mapId);
                msg.writer().writeShort(pl.zone.map.bgId);
                msg.writer().writeByte(pl.zone.zoneId);
                msg.writer().writeInt((int) pl.id);
                msg.writer().writeUTF("Ngọc Rồng Kami");
                msg.writer().writeShort(pl.location.x);
                msg.writer().writeShort(pl.location.y);
                msg.writer().writeByte(type);
                lastTimeShenronWait = System.currentTimeMillis();
                isShenronAppear = true;
                pl.idMark.setShenronType(-1);
            }
            Service.gI().sendMessAllPlayer(msg);
        } catch (Exception e) {
        }
    }

    private boolean checkShenronBall(Player pl, byte Dragon) {
        byte dragonStar = (byte) this.pl_dragonStar.get(pl);
        switch (Dragon) {
            case DRAGON_BLACK_SHENRON:
                for (int i : NGOC_RONG_DEN) {
                    if (!InventoryService.gI().isExistItemBag(pl, i)) {
                        Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng bí ngô " + (i - 701) + " sao");
                        return false;
                    }
                }
                return true;
        }
        return false;
    }

    private boolean checkShenronBall(Player pl) {
        byte dragonStar = (byte) this.pl_dragonStar.get(pl);
        if (dragonStar == 1) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_2_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 2 sao");
                return false;
            }
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 3 sao");
                return false;
            }
        } else if (dragonStar == 2) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 3 sao");
                return false;
            }
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_4_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 4 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_5_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 5 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_6_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 6 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_7_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 7 sao");
            return false;
        }
        return true;
    }

    private void sendNotifyShenronAppear() {
        Message msg = null;
        try {
            msg = new Message(-25);
            msg.writer().writeUTF(playerSummonShenron.name + " vừa gọi rồng thần tại "
                    + playerSummonShenron.zone.map.mapName + " khu vực " + playerSummonShenron.zone.zoneId);
            Service.gI().sendMessAllPlayerIgnoreMe(playerSummonShenron, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    boolean _ChangeSkillPetByIndex(int index) {
        if (playerSummonShenron.pet != null) {
            if (playerSummonShenron.pet.playerSkill.skills.get(index).skillId != -1) {
                switch (index) {
                    case 1:
                        playerSummonShenron.pet.openSkill2();
                        break;
                    case 2:
                        playerSummonShenron.pet.openSkill3();
                        break;
                    case 3:
                        playerSummonShenron.pet.openSkill4();
                        break;
                }
                Service.gI().sendThongBao(playerSummonShenron, "Đổi thành công kỹ năng " + (index + 1) + " đệ tử !");
                return true;
            } else {
                Service.gI().sendThongBao(playerSummonShenron,
                        "Đệ tử bạn ít nhất phải có Skill " + (index + 1) + " Chứ !?");
                return false;
            }
        } else {
            Service.gI().sendThongBao(playerSummonShenron, "Bạn phải có đệ tử để thực hiện chức năng này !");
            return false;
        }
    }

    public void confirmWish() {
        switch (this.menuShenron) {
            case ConstNpc.SHEN_RON_BI_NGO:
                switch (select) {
                    case 0:
                        if (!_ChangeSkillPetByIndex(1)) {
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 1:
                        if (!_ChangeSkillPetByIndex(2)) {
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 2:
                        if (!_ChangeSkillPetByIndex(3)) {
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 3:
                        if (InventoryService.gI().getCountEmptyBag(playerSummonShenron) > 4) {
                            short[] list = {1099, 1100, 1101, 1102, 1103};
                            int quan = 10;
                            for (short id : list) {
                                Item vp = ItemService.gI().createNewItem(id, quan);
                                InventoryService.gI().addItemBag(playerSummonShenron, vp);
                                InventoryService.gI().sendItemBags(playerSummonShenron);
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Hành trang không đủ chỗ trống (4) !");
                        }
                        break;
                }
                break;
            case ConstNpc.SHENRON_1_1:
                switch (this.select) {
                    case 0: // 20 tr vàng
                        if (this.playerSummonShenron.inventory.gold > (Inventory.LIMIT_GOLD - 2000000000)) {
                            this.playerSummonShenron.inventory.gold = Inventory.LIMIT_GOLD;
                        } else {
                            this.playerSummonShenron.inventory.gold += 2000000000;
                        }
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                    case 1: // găng tay đang đeo lên 1 cấp
                        Item item = this.playerSummonShenron.inventory.itemsBody.get(2);
                        if (item.isNotNullItem()) {
                            int level = 0;
                            for (ItemOption io : item.itemOptions) {
                                if (io.optionTemplate.id == 72) {
                                    level = io.param;
                                    if (level < 7) {
                                        io.param++;
                                    }
                                    break;
                                }
                            }
                            if (level < 7) {
                                if (level == 0) {
                                    item.itemOptions.add(new ItemOption(72, 1));
                                }
                                for (ItemOption io : item.itemOptions) {
                                    if (io.optionTemplate.id == 0) {
                                        io.param += (io.param * 10 / 100);
                                        break;
                                    }
                                }
                                InventoryService.gI().sendItemBody(playerSummonShenron);
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron, "Găng tay của ngươi đã đạt cấp tối đa");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi hiện tại có đeo găng đâu");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 2: // chí mạng +2%
                        if (this.playerSummonShenron.nPoint.critg < 9) {
                            this.playerSummonShenron.nPoint.critg += 2;
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron,
                                    "Điều ước này đã quá sức với ta, ta sẽ cho ngươi chọn lại");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 3: // thay chiêu 2-3 đệ tử
                        if (playerSummonShenron.pet != null) {
                            if (playerSummonShenron.pet.playerSkill.skills.get(1).skillId != -1) {
                                playerSummonShenron.pet.openSkill2();
                                if (playerSummonShenron.pet.playerSkill.skills.get(2).skillId != -1) {
                                    playerSummonShenron.pet.openSkill3();
                                }
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron,
                                        "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi làm gì có đệ tử?");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                }
                break;
            case ConstNpc.SHENRON_1_2:
                switch (this.select) {
                    case 0: // đẹp trai nhất vũ trụ
                        if (InventoryService.gI().getCountEmptyBag(playerSummonShenron) > 0) {
                            byte gender = this.playerSummonShenron.gender;
                            Item avtVip = ItemService.gI().createNewItem((short) (gender == ConstPlayer.TRAI_DAT ? 227
                                    : gender == ConstPlayer.NAMEC ? 228 : 229));
                            avtVip.itemOptions.add(new ItemOption(97, Util.nextInt(5, 10)));
                            avtVip.itemOptions.add(new ItemOption(77, Util.nextInt(10, 20)));
                            InventoryService.gI().addItemBag(playerSummonShenron, avtVip);
                            InventoryService.gI().sendItemBags(playerSummonShenron);
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Hành trang đã đầy");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 1: // +1,5 ngọc
                        this.playerSummonShenron.inventory.gem += 10000;
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                    case 2: // găng tay đệ lên 1 cấp
                        if (this.playerSummonShenron.pet != null) {
                            Item item = this.playerSummonShenron.pet.inventory.itemsBody.get(2);
                            if (item.isNotNullItem()) {
                                int level = 0;
                                for (ItemOption io : item.itemOptions) {
                                    if (io.optionTemplate.id == 72) {
                                        level = io.param;
                                        if (level < 7) {
                                            io.param++;
                                        }
                                        break;
                                    }
                                }
                                if (level < 7) {
                                    if (level == 0) {
                                        item.itemOptions.add(new ItemOption(72, 1));
                                    }
                                    for (ItemOption io : item.itemOptions) {
                                        if (io.optionTemplate.id == 0) {
                                            io.param += (io.param * 10 / 100);
                                            break;
                                        }
                                    }
                                    Service.gI().point(playerSummonShenron);
                                } else {
                                    Service.gI().sendThongBao(playerSummonShenron,
                                            "Găng tay của đệ ngươi đã đạt cấp tối đa");
                                    reOpenShenronWishes(playerSummonShenron);
                                    return;
                                }
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron, "Đệ ngươi hiện tại có đeo găng đâu");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi đâu có đệ tử");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                }
                break;
            case ConstNpc.SHENRON_2:
                switch (this.select) {
                    case 0: // +150 ngọc
                        this.playerSummonShenron.inventory.gem += 2000;
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                    case 1: // 2 tr vàng
                        if (this.playerSummonShenron.inventory.gold > (Inventory.LIMIT_GOLD - 200000000)) {
                            this.playerSummonShenron.inventory.gold = Inventory.LIMIT_GOLD;
                        } else {
                            this.playerSummonShenron.inventory.gold += 200000000;
                        }
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                }
                break;
            case ConstNpc.SHENRON_3:
                switch (this.select) {
                    case 0: // +15 ngọc
                        this.playerSummonShenron.inventory.gem += 200;
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                    case 1: // 200k vàng
                        if (this.playerSummonShenron.inventory.gold > (Inventory.LIMIT_GOLD - 20000000)) {
                            this.playerSummonShenron.inventory.gold = Inventory.LIMIT_GOLD;
                        } else {
                            this.playerSummonShenron.inventory.gold += 20000000;
                        }
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                }
                break;
        }
        shenronLeave(this.playerSummonShenron, WISHED);
    }

    public void showConfirmShenron(Player pl, int menu, byte select) {
        String[] wishes = null;
        switch (menu) {
            case ConstNpc.SHENRON_1_1:
                wishes = SHENRON_1_STAR_WISHES_1;
                break;
            case ConstNpc.SHENRON_1_2:
                wishes = SHENRON_1_STAR_WISHES_2;
                break;
            case ConstNpc.SHENRON_2:
                wishes = SHENRON_2_STARS_WHISHES;
                break;
            case ConstNpc.SHENRON_3:
                wishes = SHENRON_3_STARS_WHISHES;
                break;
            case ConstNpc.SHEN_RON_BI_NGO:
                wishes = SHEN_RON_BI_NGO_WISHES;
                break;
        }
        if (wishes == null || select < 0 || select >= wishes.length) {
            return;
        }
        this.menuShenron = menu;
        this.select = select;
        NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_CONFIRM, "Ngươi có chắc muốn ước?", wishes[select], "Từ chối");
    }

    public void reOpenShenronWishes(Player pl) {
        switch (menuShenron) {
            case ConstNpc.SHENRON_1_1:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                break;
            case ConstNpc.SHENRON_1_2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_2, SHENRON_SAY, SHENRON_1_STAR_WISHES_2);
                break;
            case ConstNpc.SHENRON_2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_2, SHENRON_SAY, SHENRON_2_STARS_WHISHES);
                break;
            case ConstNpc.SHENRON_3:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_3, SHENRON_SAY, SHENRON_3_STARS_WHISHES);
                break;
            case ConstNpc.SHEN_RON_BI_NGO:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHEN_RON_BI_NGO, SHENRON_SAY, SHEN_RON_BI_NGO_WISHES);
                break;
        }
    }

    public void shenronLeave(Player pl, byte type) {
        if (pl != null && pl.isPl() && pl.idMark != null) {
            if (type == WISHED) {
                NpcService.gI().createTutorial(pl, 0,
                        "Điều ước của ngươi đã trở thành sự thật\nHẹn gặp ngươi lần sau, ta đi ngủ đây, bái bai");
            } else {
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.IGNORE_MENU,
                        "Ta buồn ngủ quá rồi\nHẹn gặp ngươi lần sau, ta đi đây, bái bai");
            }
            activeShenron(pl, false, SummonDragon.DRAGON_SHENRON);
        }
        this.isShenronAppear = false;
        this.menuShenron = -1;
        this.select = -1;
        this.playerSummonShenron = null;
        this.playerSummonShenronId = -1;
        this.shenronStar = -1;
        this.mapShenronAppear = null;
        lastTimeShenronAppeared = System.currentTimeMillis();
    }

}
