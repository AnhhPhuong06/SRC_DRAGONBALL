package services;

 
import consts.ConstPlayer;
import models.item.Item;
import static models.item.ItemTime.*;
import models.player.Fusion;
import models.player.Player;
import network.io.Message;
import java.io.IOException;
import models.dungeon.TreasureUnderSea;
import models.dungeon.SnakeWay;
import models.dungeon.DestronGas;
import models.dungeon.RedRibbonHQ;
import services.player.InventoryService;
import utils.Logger;
import utils.Util;

public class ItemTimeService {

    private static ItemTimeService i;

    public static ItemTimeService gI() {
        if (i == null) {
            i = new ItemTimeService();
        }
        return i;
    }

    public void sendAllItemTime(Player player) {
        ItemTimeService.gI().sendTextBanDoKhoBau(player);
        ItemTimeService.gI().sendTextMap182(player);
        ItemTimeService.gI().sendTextHalloween(player);
        ItemTimeService.gI().sendBuff1016(player);
        ItemTimeService.gI().sendBuffZeno(player);
        ItemTimeService.gI().sendTextDoanhTrai(player);
        ItemTimeService.gI().sendTextConDuongRanDoc(player);
        ItemTimeService.gI().sendTextKhiGasHuyDiet(player);
        ItemTimeService.gI().sendTextTimePickDoanhTrai(player);
        ItemTimeService.gI().send_text_time_nhan_bua_mien_phi(player);
        if (player.fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
            sendItemTime(player, player.gender == ConstPlayer.NAMEC ? 3901 : 3790,
                    (int) ((Fusion.TIME_FUSION - (System.currentTimeMillis() - player.fusion.lastTimeFusion)) / 1000));
        }
        if (player.itemTime.isBuaTNSM) {
            sendItemTime(player, 11474,
                    (int) ((TIME_CMS - (System.currentTimeMillis() - player.itemTime.lastTimeBuaTNSM)) / 1000));
        }
        if (player.itemTime.tnsmX > 1) {
            sendItemTime(player, models.item.ItemTime.iconTnsmX(player.itemTime.tnsmX),
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeTnsmX)) / 1000));
        }
        if (player.itemTime.isTnDeTu) {
            sendItemTime(player, 13540,
                    (int) ((TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeTnDeTu)) / 1000));
        }
        if (player.itemTime.isKhauTrang) {
            sendItemTime(player, 7149,
                    (int) ((TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeKhauTrang)) / 1000));
        }
        if (player.itemTime.isUseBoHuyet) {
            sendItemTime(player, 2755,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet)) / 1000));
        }
        if (player.itemTime.isUseBoKhi) {
            sendItemTime(player, 2756,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi)) / 1000));
        }
        if (player.itemTime.isUseGiapXen) {
            sendItemTime(player, 2757,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen)) / 1000));
        }
        if (player.itemTime.isUseCuongNo) {
            sendItemTime(player, 2754,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo)) / 1000));
        }
        if (player.effectSkill.isBienHinh) {
            int iconBienHinh = 21669;
            switch (player.effectSkill.levelBienHinh) {
                case 1:
                    iconBienHinh = 21669;
                    break;
                case 2:
                    iconBienHinh = 21669;
                    break;
                case 3:
                    iconBienHinh = 21669;
                    break;
                case 4:
                    iconBienHinh = 21669;
                    break;
                case 5:
                    iconBienHinh = 21669;
                    break;
                case 6:
                    iconBienHinh = 21669;
                    break;
            }
            sendItemTime(player, iconBienHinh, (int) ((player.effectSkill.timeBienHinh
                    - (System.currentTimeMillis() - player.effectSkill.lastTimeBienHinh)) / 1000));
        }

        if (player.itemTime.isUseAnDanh) {
            sendItemTime(player, 2760,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh)) / 1000));
        }
        if (player.itemTime.miakl) {
            sendItemTime(player, 13462,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.timemiakl)) / 1000));
        }

        if (player.itemTime.miathom) {
            sendItemTime(player, 13463,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.timemiathom)) / 1000));
        }
        if (player.itemTime.miasaurieng) {
            sendItemTime(player, 13464,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.timemiasaurieng)) / 1000));
        }
        if (player.itemTime.isUseBoHuyet2) {
            sendItemTime(player, 10714,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet2)) / 1000));
        }
        if (player.itemTime.isUseBoKhi2) {
            sendItemTime(player, 10715,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi2)) / 1000));
        }
        if (player.itemTime.isUseGiapXen2) {
            sendItemTime(player, 10712,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen2)) / 1000));
        }
        if (player.itemTime.isUseCuongNo2) {
            sendItemTime(player, 10716,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo2)) / 1000));
        }
        if (player.itemTime.isXimuoihoadao) {
            sendItemTime(player, 10905,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeXimuoihoadao)) / 1000));
        }
        if (player.itemTime.isXimuoihoamai) {
            sendItemTime(player, 10904,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeXimuoihoamai)) / 1000));
        }
        if (player.itemTime.isUseAnDanh2) {
            sendItemTime(player, 10717,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh2)) / 1000));
        }
        if (player.itemTime.isUseCMS) {
            sendItemTime(player, 5829,
                    (int) ((TIME_CMS - (System.currentTimeMillis() - player.itemTime.lastTimeUseCMS)) / 1000));
        }
        if (player.itemTime.isUseNCD) {
            sendItemTime(player, 11173,
                    (int) ((TIME_NCD - (System.currentTimeMillis() - player.itemTime.lastTimeUseNCD)) / 1000));
        }
        if (player.itemTime.isUseGTPT) {
            sendItemTime(player, 3778,
                    (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeUseGTPT)) / 1000));
        }
        if (player.itemTime.isUseDK) {
            sendItemTime(player, 5072,
                    (int) ((TIME_DK - (System.currentTimeMillis() - player.itemTime.lastTimeUseDK)) / 1000));
        }
        if (player.itemTime.isOpenPower) {
            sendItemTime(player, 3783,
                    (int) ((TIME_OPEN_POWER - (System.currentTimeMillis() - player.itemTime.lastTimeOpenPower))
                            / 1000));
        }
        if (player.itemTime.isUseMayDo) {
            sendItemTime(player, 2758,
                    (int) ((TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo)) / 1000));
        }
        if (player.itemTime.isUseMayDo2) {
            sendItemTime(player, 2758,
                    (int) ((TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo2)) / 1000));
        }
        if (player.itemTime.isUseCo4La) {
            sendItemTime(player, 13618,
                    (int) ((TIME_CO - (System.currentTimeMillis() - player.itemTime.lastTimeUseCo4La)) / 1000));
        }
        if (player.itemTime.banhchung) {
            sendItemTime(player, 7080,
                    (int) ((DEVNDUNG - (System.currentTimeMillis() - player.itemTime.banhchunglastTime)) / 1000));
        }
        if (player.itemTime.banhtet) {
            sendItemTime(player, 7079,
                    (int) ((DEVNDUNG - (System.currentTimeMillis() - player.itemTime.banhtetlastTime)) / 1000));
        }
        if (player.itemTime.nguqua) {
            sendItemTime(player, 10889,
                    (int) ((DEVNDUNG - (System.currentTimeMillis() - player.itemTime.nguqualastTime)) / 1000));
        }
        if (player.itemTime.isEatMeal) {
            sendItemTime(player, player.itemTime.iconMeal,
                    (int) ((TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal)) / 1000));
        }
        if (player.itemTime.isEatMeal2) {
            sendItemTime(player, player.itemTime.iconMeal2,
                    (int) ((TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal2)) / 1000));
        }
        if (player.itemTime.isUseTDLT) {
            sendItemTime(player, 4387, player.itemTime.timeTDLT / 1000);
        }
        if (player.itemTime.isUseRX) {
            sendItemTime(player, 6581, player.itemTime.timeRX / 1000);
        }
        if (player.itemTime.isXuong) {
            int timeLeft = (int) ((1_800_000 - (System.currentTimeMillis() - player.itemTime.lastTimeXuong)) / 1000);
            if (timeLeft > 0) {
                sendItemTime(player, player.itemTime.iconXuong, timeLeft);
            }
        }
        if (player.itemTime.isKeoMotMat) {
            int timeLeft = (int) ((models.item.ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeKeoMotMat)) / 1000);
            if (timeLeft > 0) {
                sendItemTime(player, 8243, timeLeft);
            }
        }
        if (player.itemTime.isSupBiHacAm) {
            int timeLeft = (int) ((models.item.ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeSupBiHacAm)) / 1000);
            if (timeLeft > 0) {
                sendItemTime(player, 8244, timeLeft);
            }
        }
        if (player.itemTime.isBanhGatoNhen) {
            int timeLeft = (int) ((models.item.ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeBanhGatoNhen)) / 1000);
            if (timeLeft > 0) {
                sendItemTime(player, 8246, timeLeft);
            }
        }
        if (player.itemTime.isHamburgerSau) {
            int timeLeft = (int) ((models.item.ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeHamburgerSau)) / 1000);
            if (timeLeft > 0) {
                sendItemTime(player, 8247, timeLeft);
            }
        }
    }

    public void turnOnTDLT(Player player, Item item) {
        int min = 0;
        int minleft = 0;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 1) {
                min = io.param;
                minleft = (min * 60 - 32767) / 60;
                io.param = minleft > 0 ? minleft : 0;
                break;
            }
        }

        player.itemTime.isUseTDLT = true;

        int timeTDLTInMinutes = min - minleft;
        int timeTDLTInSeconds = timeTDLTInMinutes * 60;
        int maxTimeInSeconds = 32767;
        int timeTDLTInSecondsLimited = Math.min(timeTDLTInSeconds, maxTimeInSeconds);

        player.itemTime.timeTDLT = timeTDLTInSecondsLimited * 1000;

        player.itemTime.lastTimeUseTDLT = System.currentTimeMillis();
        sendCanAutoPlay(player);
        sendItemTime(player, 4387, timeTDLTInSecondsLimited);
        InventoryService.gI().sendItemBags(player);
    }

    public void turnOffTDLT(Player player, Item item) {
        player.itemTime.isUseTDLT = false;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 1) {
                io.param += (short) ((player.itemTime.timeTDLT
                        - (System.currentTimeMillis() - player.itemTime.lastTimeUseTDLT)) / 60 / 1000);
                break;
            }
        }
        sendCanAutoPlay(player);
        removeItemTime(player, 4387);
        InventoryService.gI().sendItemBags(player);
    }

    public void sendCanAutoPlay(Player player) {
        Message msg;
        try {
            msg = new Message(-116);
            msg.writer().writeByte(player.itemTime.isUseTDLT ? 1 : 0);
            player.sendMessage(msg);
        } catch (IOException e) {
            Logger.logException(ItemTimeService.class, e);
        }
    }

    public void sendTextDoanhTrai(Player player) {
        if (player.clan != null && !player.clan.haveGoneDoanhTrai
                && player.clan.lastTimeOpenDoanhTrai != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenDoanhTrai) / 1000);
            int secondsLeft = (RedRibbonHQ.TIME_DOANH_TRAI / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, DOANH_TRAI, "Trại độc nhãn:", secondsLeft);
        }
    }

    public void sendTextTimePickDoanhTrai(Player player) {
        if (player.clan != null && player.clan.doanhTrai != null && player.clan.doanhTrai.isTimePicking) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.doanhTrai.lastTimePick) / 1000);
            int secondsLeft = (RedRibbonHQ.TIME_PICK_DOANH_TRAI / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, DOANH_TRAI, "Trại độc nhãn:", secondsLeft);
        }
    }

    private String str = "Nhận ngẫu nhiên bùa 1h mỗi ngày tại Bà Hạt Mít ở vách núi !!";

    public void send_text_time_nhan_bua_mien_phi(Player player) {
        if (Util.canDoWithTime(player.itemTime.lastTimeSendTextTime, 60000)) {
            if (player.luotNhanBuaMienPhi == 1) {
                ItemTimeService.gI().sendTextTime(player, TEXT_NHAN_BUA_MIEN_PHI, str, 30);
            }
            player.itemTime.lastTimeSendTextTime = System.currentTimeMillis();
        }
    }

    public void sendTextBanDoKhoBau(Player player) {
        if (player.clan != null
                && player.clan.lastTimeOpenBanDoKhoBau != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenBanDoKhoBau) / 1000);
            int secondsLeft = (TreasureUnderSea.TIME_BAN_DO_KHO_BAU / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, BAN_DO_KHO_BAU, "Hang kho báu:", secondsLeft);
        }
    }

    /** Đồng hồ đếm ngược ở map 182 (Núi Seo Phi) — cùng cơ chế đếm giờ như phó bản kho báu. Tự ẩn khi rời map. */
    public void sendTextMap182(Player player) {
        if (player.zone != null && player.zone.map != null && player.zone.map.mapId == 182
                && player.map182Deadline > 0) {
            int secondsLeft = (int) ((player.map182Deadline - System.currentTimeMillis()) / 1000);
            if (secondsLeft > 0 && secondsLeft <= 32767) { // 32767 = short-max, tránh tràn writeShort & không hardcode 30 phút
                sendTextTime(player, MAP_182, "Núi Seo Phi:", secondsLeft);
                return;
            }
        }
        removeTextTime(player, MAP_182);
    }

    /**
     * Đồng hồ đếm ngược phó bản Halloween (map 181). DÙNG LẠI SLOT KHI_GAS_HUY_DIET (id 3) vì client
     * không render slot mới (id 6). An toàn: clan không ở Khí Gas + Halloween cùng lúc; và trong
     * sendAllItemTime hàm này chạy TRƯỚC sendTextKhiGasHuyDiet nên nếu đang ở Khí Gas thì gas ghi đè đúng.
     */
    public void sendTextHalloween(Player player) {
        models.dungeon.HalloweenDungeon d = (player.clan != null) ? player.clan.halloweenDungeon : null;
        if (d != null && d.isOpened) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenHalloween) / 1000);
            int secondsLeft = (int) (models.dungeon.HalloweenDungeon.TIME_LIMIT / 1000) - secondPassed;
            if (secondsLeft > 0 && secondsLeft <= 32767) {
                sendTextTime(player, KHI_GAS_HUY_DIET, "Phó bản Halloween:", secondsLeft);
                return;
            }
        }
        removeTextTime(player, KHI_GAS_HUY_DIET);
    }

    public void sendTextXinbato(Player player) {
        sendTextTime(player, BAN_DO_KHO_BAU, "Tìm nước cho Xinbatô ở đảo Kame hoặc đảo Guru", 30);
    }

    public void sendTextConDuongRanDoc(Player player) {
        if (player.clan != null
                && player.clan.lastTimeOpenConDuongRanDoc != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenConDuongRanDoc) / 1000);
            int secondsLeft = (SnakeWay.TIME_CON_DUONG_RAN_DOC / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, CON_DUONG_RAN_DOC, "Con đường rắn độc:", secondsLeft);
        }
    }

    public void sendTextKhiGasHuyDiet(Player player) {
        if (player.clan != null
                && player.clan.lastTimeOpenKhiGasHuyDiet != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenKhiGasHuyDiet) / 1000);
            int secondsLeft = (DestronGas.TIME_KHI_GAS_HUY_DIET / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, KHI_GAS_HUY_DIET, "Khí gas hủy diệt:", secondsLeft);
        }
    }

    public void removeTextDoanhTrai(Player player) {
        removeTextTime(player, DOANH_TRAI);
    }

    public void removeTextBanDoKhoBau(Player player) {
        removeTextTime(player, BAN_DO_KHO_BAU);
    }

    public void removeTextConDuongRanDoc(Player player) {
        removeTextTime(player, CON_DUONG_RAN_DOC);
    }

    public void removeTextKhiGasHuyDiet(Player player) {
        removeTextTime(player, KHI_GAS_HUY_DIET);
    }

    public void removeTextTime(Player player, byte id) {
        sendTextTime(player, id, null, 0);
    }

    private void sendTextTime(Player player, byte id, String text, int seconds) {
        Message msg;
        try {
            msg = new Message(65);
            msg.writer().writeByte(id);
            msg.writer().writeUTF(text == null ? "" : text);
            msg.writer().writeShort(seconds);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendItemTime(Player player, int itemId, int time) {
        Message msg;
        try {
            msg = new Message(-106);
            msg.writer().writeShort(itemId);
            msg.writer().writeShort(time);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void removeItemTime(Player player, int itemTime) {
        sendItemTime(player, itemTime, 0);
    }

    public void sendBuff1016(Player player) {
        if (player.charms != null && player.charms.tdBuff1016 > System.currentTimeMillis()) {
            int secLeft = (int) ((player.charms.tdBuff1016 - System.currentTimeMillis()) / 1000);
            if (secLeft > 0 && secLeft <= 32767) {
                sendItemTime(player, 9068, secLeft);
            }
        }
    }

    private static final int ICON_BUFF_ZENO_HP = 20217;
    private static final int ICON_BUFF_ZENO_KI = 20218;
    private static final int ICON_BUFF_ZENO_SD = 20219;

    public void sendBuffZeno(Player player) {
        if (player.charms == null) {
            return;
        }
        long now = System.currentTimeMillis();
        sendBuffZenoIcon(player, ICON_BUFF_ZENO_HP, player.charms.tdBuffZenoHP, now);
        sendBuffZenoIcon(player, ICON_BUFF_ZENO_KI, player.charms.tdBuffZenoKI, now);
        sendBuffZenoIcon(player, ICON_BUFF_ZENO_SD, player.charms.tdBuffZenoSD, now);
    }

    private void sendBuffZenoIcon(Player player, int icon, long td, long now) {
        if (td <= now) {
            return;
        }
        int secLeft = (int) ((td - now) / 1000);
        if (secLeft > 0 && secLeft <= 32767) { // wire là 1 short
            sendItemTime(player, icon, secLeft);
        }
    }

}
