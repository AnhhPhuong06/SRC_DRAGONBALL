package models.item;


import models.player.NPoint;
import models.player.Player;
import services.Service;
import utils.Util;
import services.ItemTimeService;

public class ItemTime {

    public static final byte DOANH_TRAI = 0;
    public static final byte BAN_DO_KHO_BAU = 1;
    public static final byte CON_DUONG_RAN_DOC = 2;
    public static final byte KHI_GAS_HUY_DIET = 3;
    public static final byte TEXT_NHAN_BUA_MIEN_PHI = 4;
    public static final byte MAP_182 = 5;
    public static final byte HALLOWEEN = 6;

    public static final int TIME_ITEM = 600000;
    public static final int TIME_OPEN_POWER = 8640000;
    public static final int TIME_MAY_DO = 1800000;
    public static final int DEVNDUNG = 3600000;
    public static final int TIME_CO = 21600000;
    public static final int TIME_MAY_DO2 = 1800000;
    public static final int TIME_EAT_MEAL = 600000;
    public static final int TIME_CMS = 3600000;
    public static final int TIME_DK = 1800000;
    public static final int TIME_NCD = 1800000;

    private Player player;

    public boolean isUseBoHuyet;
    public boolean isUseBoKhi;
    public boolean isUseGiapXen;
    public boolean isUseCuongNo;
    public boolean isUseAnDanh;
    public boolean isUseBoHuyet2;
    public boolean isUseBoKhi2;
    public boolean isUseGiapXen2;
    public boolean isUseCuongNo2;
    public boolean isUseAnDanh2;
    public boolean isXuong;
    public long lastTimeXuong;
    public int iconXuong;
    public long lastTimeBoHuyet;
    public long lastTimeBoKhi;
    public long lastTimeGiapXen;
    public long lastTimeCuongNo;
    public long lastTimeAnDanh;

    public long lastTimeBoHuyet2;
    public long lastTimeBoKhi2;
    public long lastTimeGiapXen2;
    public long lastTimeCuongNo2;
    public long lastTimeAnDanh2;

    public boolean isUseMayDo;
    public long lastTimeUseMayDo;
    public boolean isUseMayDo2;
    public long lastTimeUseMayDo2;

    public boolean isUseCo4La;
    public long lastTimeUseCo4La;

    public boolean isOpenPower;
    public long lastTimeOpenPower;

    public boolean banhchung;
    public long banhchunglastTime;

    public boolean banhtet;
    public long banhtetlastTime;

    public boolean nguqua;
    public long nguqualastTime;

    public boolean isUseTDLT;
    public long lastTimeUseTDLT;
    public int timeTDLT;

    public boolean isUseRX;
    public long lastTimeUseRX;
    public int timeRX;

    public boolean isUseCMS;
    public long lastTimeUseCMS;

    public boolean isUseNCD;
    public long lastTimeUseNCD;

    public boolean isUseGTPT;
    public long lastTimeUseGTPT;

    public boolean isUseDK;
    public long lastTimeUseDK;

    public boolean isEatMeal;
    public long lastTimeEatMeal;
    public int iconMeal;

    public boolean isEatMeal2;
    public long lastTimeEatMeal2;
    public int iconMeal2;

    public boolean isKhauTrang;
    public long lastTimeKhauTrang;

    public boolean isTnDeTu;
    public long lastTimeTnDeTu;

    public boolean isXimuoihoadao;
    public boolean isXimuoihoamai;
    public long lastTimeXimuoihoadao;
    public long lastTimeXimuoihoamai;

    public boolean isBuaTNSM;
    public long lastTimeBuaTNSM;


    public int tnsmX;
    public long lastTimeTnsmX;

    public static int iconTnsmX(int x) {
        return x == 3 ? 20657 : x == 5 ? 20658 : 20659;
    }
    public boolean miakl;
    public boolean miathom;
    public boolean miasaurieng;
    public long timemiakl;
    public long timemiathom;
    public long timemiasaurieng;
    public static final int TIME_HALLOWEEN_BUFF = 1800000; // 30 phút

    public boolean isKeoMotMat;
    public long lastTimeKeoMotMat;
    public boolean isSupBiHacAm;
    public long lastTimeSupBiHacAm;
    public boolean isBanhGatoNhen;
    public long lastTimeBanhGatoNhen;
    public boolean isHamburgerSau;
    public long lastTimeHamburgerSau;

    // Nhận bùa
    public long lastTimeSendTextTime;

    public ItemTime(Player player) {
        this.player = player;
    }

    public void update() {
        if (isBuaTNSM) {
            if (Util.canDoWithTime(lastTimeBuaTNSM, TIME_CMS)) {
                isBuaTNSM = false;
                Service.gI().point(player);
            }
        }
        if (tnsmX > 1) {
            if (Util.canDoWithTime(lastTimeTnsmX, TIME_ITEM)) {
                Service.gI().sendThongBao(player, "Bình x" + tnsmX + " tiềm năng sức mạnh đã hết hiệu lực");
                tnsmX = 0;
            }
        }
        if (isXimuoihoamai) {
            if (Util.canDoWithTime(lastTimeXimuoihoamai, TIME_ITEM)) {
                isXimuoihoamai = false;
                Service.gI().point(player);
            }
        }
        if (miakl) {
            if (Util.canDoWithTime(timemiakl, TIME_ITEM)) {
                miakl = false;
                Service.gI().point(player);
            }
        }
        if (miathom) {
            if (Util.canDoWithTime(timemiathom, TIME_ITEM)) {
                miathom = false;
                Service.gI().point(player);
            }
        }
        if (miasaurieng) {
            if (Util.canDoWithTime(timemiasaurieng, TIME_ITEM)) {
                miasaurieng = false;
                Service.gI().point(player);
            }
        }
        if (isXimuoihoadao) {
            if (Util.canDoWithTime(lastTimeXimuoihoadao, TIME_ITEM)) {
                isXimuoihoadao = false;
                Service.gI().point(player);
            }
        }
        if (isTnDeTu) {
            if (Util.canDoWithTime(lastTimeTnDeTu, TIME_MAY_DO)) {
                isTnDeTu = false;
                Service.gI().point(player);
            }
        }
        if (isKhauTrang) {
            if (Util.canDoWithTime(lastTimeKhauTrang, TIME_MAY_DO)) {
                isKhauTrang = false;
                Service.gI().point(player);
            }
        }
        if (isEatMeal) {
            if (Util.canDoWithTime(lastTimeEatMeal, TIME_EAT_MEAL)) {
                isEatMeal = false;
                Service.gI().point(player);
            }
        }
        if (isEatMeal2) {
            if (Util.canDoWithTime(lastTimeEatMeal2, TIME_EAT_MEAL)) {
                isEatMeal2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseBoHuyet) {
            if (Util.canDoWithTime(lastTimeBoHuyet, TIME_ITEM)) {
                isUseBoHuyet = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi) {
            if (Util.canDoWithTime(lastTimeBoKhi, TIME_ITEM)) {
                isUseBoKhi = false;
                Service.gI().point(player);
            }
        }

        if (isUseGiapXen) {
            if (Util.canDoWithTime(lastTimeGiapXen, TIME_ITEM)) {
                isUseGiapXen = false;
            }
        }
        if (isUseCuongNo) {
            if (Util.canDoWithTime(lastTimeCuongNo, TIME_ITEM)) {
                isUseCuongNo = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh) {
            if (Util.canDoWithTime(lastTimeAnDanh, TIME_ITEM)) {
                isUseAnDanh = false;
            }
        }

        if (isUseBoHuyet2) {
            if (Util.canDoWithTime(lastTimeBoHuyet2, TIME_ITEM)) {
                isUseBoHuyet2 = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi2) {
            if (Util.canDoWithTime(lastTimeBoKhi2, TIME_ITEM)) {
                isUseBoKhi2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseGiapXen2) {
            if (Util.canDoWithTime(lastTimeGiapXen2, TIME_ITEM)) {
                isUseGiapXen2 = false;
            }
        }
        if (isUseCuongNo2) {
            if (Util.canDoWithTime(lastTimeCuongNo2, TIME_ITEM)) {
                isUseCuongNo2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh2) {
            if (Util.canDoWithTime(lastTimeAnDanh2, TIME_ITEM)) {
                isUseAnDanh2 = false;
            }
        }
        if (isUseCMS) {
            if (Util.canDoWithTime(lastTimeUseCMS, TIME_CMS)) {
                isUseCMS = false;
            }
        }
        if (isUseGTPT) {
            if (Util.canDoWithTime(lastTimeUseGTPT, TIME_ITEM)) {
                isUseGTPT = false;
            }
        }
        if (isUseDK) {
            if (Util.canDoWithTime(lastTimeUseDK, TIME_DK)) {
                isUseDK = false;
            }
        }
        if (isOpenPower) {
            if (Util.canDoWithTime(lastTimeOpenPower, TIME_OPEN_POWER)) {
                player.nPoint.limitPower++;
                if (player.nPoint.limitPower > NPoint.MAX_LIMIT) {
                    player.nPoint.limitPower = NPoint.MAX_LIMIT;
                }
                Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã được tăng lên 1 bậc");
                isOpenPower = false;
            }
        }
        if (isUseMayDo) {
            if (Util.canDoWithTime(lastTimeUseMayDo, TIME_MAY_DO)) {
                isUseMayDo = false;
            }
        }
        if (isUseMayDo2) {
            if (Util.canDoWithTime(lastTimeUseMayDo2, TIME_MAY_DO2)) {
                isUseMayDo2 = false;
            }
        }
        if (isUseCo4La) {
            if (Util.canDoWithTime(lastTimeUseCo4La, TIME_CO)) {
                isUseCo4La = false;
            }
        }
        if (nguqua) {
            if (Util.canDoWithTime(nguqualastTime, DEVNDUNG)) {
                nguqua = false;
            }
        }
        if (banhchung) {
            if (Util.canDoWithTime(banhchunglastTime, DEVNDUNG)) {
                banhchung = false;
            }
        }
        if (banhtet) {
            if (Util.canDoWithTime(banhtetlastTime, DEVNDUNG)) {
                banhtet = false;
            }
        }
        if (isUseTDLT) {
            if (Util.canDoWithTime(lastTimeUseTDLT, timeTDLT)) {
                this.isUseTDLT = false;
                ItemTimeService.gI().sendCanAutoPlay(this.player);
            }
        }
        if (isUseRX) {
            if (Util.canDoWithTime(lastTimeUseRX, timeRX)) {
                isUseRX = false;
            }
        }
        if (isXuong) {
            if (Util.canDoWithTime(lastTimeXuong, 1_800_000)) {
                isXuong = false;
                ItemTimeService.gI().removeItemTime(player, iconXuong);
                Service.gI().Send_Caitrang(player);
            }
        }
        if (isKeoMotMat) {
            if (Util.canDoWithTime(lastTimeKeoMotMat, TIME_HALLOWEEN_BUFF)) {
                isKeoMotMat = false;
                ItemTimeService.gI().removeItemTime(player, 8243);
                Service.gI().point(player);
            }
        }
        if (isSupBiHacAm) {
            if (Util.canDoWithTime(lastTimeSupBiHacAm, TIME_HALLOWEEN_BUFF)) {
                isSupBiHacAm = false;
                ItemTimeService.gI().removeItemTime(player, 8244);
                Service.gI().point(player);
            }
        }
        if (isBanhGatoNhen) {
            if (Util.canDoWithTime(lastTimeBanhGatoNhen, TIME_HALLOWEEN_BUFF)) {
                isBanhGatoNhen = false;
                ItemTimeService.gI().removeItemTime(player, 8246);
                Service.gI().point(player);
            }
        }
        if (isHamburgerSau) {
            if (Util.canDoWithTime(lastTimeHamburgerSau, TIME_HALLOWEEN_BUFF)) {
                isHamburgerSau = false;
                ItemTimeService.gI().removeItemTime(player, 8247);
                Service.gI().point(player);
            }
        }
    }

    public void dispose() {
        this.player = null;
    }
}
