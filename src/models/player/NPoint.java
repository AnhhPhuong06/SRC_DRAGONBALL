package models.player;

 
import models.radar.Card;
import models.radar.OptionCard;
import models.radar.RadarCard;
import services.RadarService;
import consts.ConstPlayer;
import consts.ConstRatio;
import models.intrinsic.Intrinsic;
import models.item.Item;
import models.item.Item.ItemOption;
import models.skill.Skill;
import server.Manager;
import services.CombineService;
import services.EffectSkillService;
import services.ItemService;
import services.map.MapService;
import services.player.PlayerService;
import services.Service;
import services.TaskService;
import utils.Logger;
import utils.SkillUtil;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import models.clan.Clan;
import database.daos.EventDAO;
import lombok.Setter;
import models.mob.Mob;
import utils.TimeUtil;

public class NPoint {

    public static final byte MAX_LIMIT = 10;

    @Setter
    private Player player;

    public NPoint(Player player) {
        this.player = player;
        this.tlHp = new ArrayList<>();
        this.tlMp = new ArrayList<>();
        this.tlDef = new ArrayList<>();
        this.tlDame = new ArrayList<>();
        this.tlDameAttMob = new ArrayList<>();
        this.tlTNSM = new ArrayList<>();
        this.tlDameCrit = new ArrayList<>();
    }

    public boolean isCrit;
    public boolean isCrit100;
    public boolean isCritTele;
    public boolean buffDefenseSatellite, buffExpSatellite;

    private Intrinsic intrinsic;
    private int percentDameIntrinsic;
    public int dameAfter;

    /*-----------------------Chỉ số cơ bản------------------------------------*/
    public byte numAttack;
    public short stamina, maxStamina;

    public byte limitPower;
    public long power;
    public long tiemNang;

    public long hp, hpMax, hpg;
    public long mp, mpMax, mpg;
    public long dame, dameg;
    public int def, defg;
    public int crit, critg;
    public byte speed = 8;

    public boolean teleport;

    public boolean khangTDHS;

    /**
     * Chỉ số cộng thêm
     */
    public int hpAdd, mpAdd, dameAdd, defAdd, critAdd, hpHoiAdd, mpHoiAdd;

    /**
     * //+#% sức đánh chí mạng
     */
    public List<Integer> tlDameCrit;
    public int tlSDCM;

    /**
     * Tỉ lệ hp, mp cộng thêm
     */
    public List<Integer> tlHp, tlMp;

    /**
     * Tỉ lệ giáp cộng thêm
     */
    public List<Integer> tlDef;

    /**
     * Tỉ lệ sức đánh/ sức đánh khi đánh quái
     */
    public List<Integer> tlDame, tlDameAttMob;

    /**
     * Lượng hp, mp hồi mỗi 30s, mp hồi cho người khác
     */
    public long hpHoi, mpHoi, mpHoiCute;

    /**
     * Tỉ lệ hp, mp hồi cộng thêm
     */
    public short tlHpHoi, tlMpHoi;

    /**
     * Tỉ lệ hp, mp hồi bản thân và đồng đội cộng thêm
     */
    public short tlHpHoiBanThanVaDongDoi, tlMpHoiBanThanVaDongDoi;

    /**
     * Tỉ lệ hút hp, mp khi đánh, hp khi đánh quái
     */
    public short tlHutHp, tlHutMp, tlHutHpMob;

    /**
     * Tỉ lệ hút hp, mp xung quanh mỗi 5s
     */
    public short tlHutHpMpXQ;

    /**
     * Tỉ lệ phản sát thương
     */
    public short tlPST;

    /**
     * Tỉ lệ tiềm năng sức mạnh
     */
    public List<Integer> tlTNSM;

    /**
     * Tỉ lệ vàng cộng thêm
     */
    public short tlGold;

    /**
     * Tỉ lệ né đòn
     */
    public short tlNeDon;

    public short tlBom;

    public short tlGiap;

    public short tlxgcc;

    public short tlxgc;

    public short tlchinhxac;

    public short tlTNSMPet;
    public short xChuong;

    public short setltdb;
    public short setTinhAn;
    public short setNhatAn;
    public short setNguyetAn;

    public int tlDameChuong;
    public boolean xDameChuong;

    public int tlSexyDame;

    public short tlSubSD;

    public int voHieuChuong;

    /*------------------------Effect skin-------------------------------------*/
    public Item trainArmor;
    public boolean wearingTrainArmor;

    public boolean wearingVoHinh;
    public boolean isKhongLanh;
    public boolean islinhthuydanhbac;
    public boolean isTinhAn;
    public boolean isNhatAn;
    public boolean isNguyetAn;
    public boolean isTanHinh;
    public boolean isHoaDa;
    public boolean isLamCham;
    public boolean isDoSPL;
    public boolean isThoBulma;

    public short tlHpGiamODo;

    public boolean isGogeta;

    public int tlSpeed;

    public int levelBT;

    public void calPoint() {
        if (this.player.pet != null) {
            this.player.pet.nPoint.setPointWhenWearClothes();
        }
        this.setPointWhenWearClothes();
    }

    private synchronized void setPointWhenWearClothes() {
        resetPoint();
        if (this.player.setClothes != null) {
            this.player.setClothes.setup();
        }
        if (this.player.rewardBlackBall.timeOutOfDateReward[2] > System.currentTimeMillis()) {
            tlHutHp += RewardBlackBall.R3S_1;
        }
        if (this.player.rewardBlackBall.timeOutOfDateReward[3] > System.currentTimeMillis()) {
            tlPST += RewardBlackBall.R4S_2;
        }
        if (this.player.rewardBlackBall.timeOutOfDateReward[4] > System.currentTimeMillis()) {
            this.critAdd += RewardBlackBall.R5S_1;
        }
        if (this.player.rewardBlackBall.timeOutOfDateReward[6] > System.currentTimeMillis()) {
            tlNeDon += RewardBlackBall.R7S_1;
        }

        if (this.player.Cards != null && !this.player.Cards.isEmpty()) {
            for (Card card : this.player.Cards) {
                if (card == null || (card.Amount <= 0 && card.Level < 0)) {
                    continue;
                }

                RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream()
                        .filter(c -> c != null && c.Id == card.Id)
                        .findFirst()
                        .orElse(null);
                List<OptionCard> options = (radarTemplate != null && radarTemplate.Options != null && !radarTemplate.Options.isEmpty())
                        ? radarTemplate.Options
                        : card.Options;
                if (options != null) {
                    for (OptionCard io : options) {
                        if (io == null) {
                            continue;
                        }
                        switch (io.id) {
                        case 0: //Tấn công +#
                            this.dameAdd += io.param;
                            break;
                        case 2: //HP, KI+#000
                            this.hpAdd += io.param * 1000;
                            this.mpAdd += io.param * 1000;
                            break;
                        case 3:// vô hiệu chưởng
                            this.voHieuChuong += io.param;
                            break;
                        case 5: //+#% sức đánh chí mạng
                        case ItemService.MAGE_CRITICAL_DAMAGE_PERCENT:
                            this.tlDameCrit.add(io.param);
                            this.tlSDCM += io.param;
                            break;
                        case 6: //HP+#
                            this.hpAdd += io.param;
                            break;
                        case 7: //KI+#
                            this.mpAdd += io.param;
                            break;
                        case 8: //Hút #% HP, KI xung quanh mỗi 5 giây
                            this.tlHutHpMpXQ += io.param;
                            break;
                        case 14: //Chí mạng+#%
                            this.critAdd += io.param;
                            break;
                        case 16: // Speed
                        case 114:
                        case 148:
                            this.tlSpeed += io.param;
                            break;
                        case 18: //Chinh xac
                            this.tlchinhxac += io.param;
                            break;
                        case 19: //Tấn công+#% khi đánh quái
                            this.tlDameAttMob.add(io.param);
                            break;
                        case 22: //HP+#K
                            this.hpAdd += io.param * 1000;
                            break;
                        case 23: //MP+#K
                            this.mpAdd += io.param * 1000;
                            break;
                        case 27: //+# HP/30s
                            this.hpHoiAdd += io.param;
                            break;
                        case 28: //+# KI/30s
                            this.mpHoiAdd += io.param;
                            break;
                        case 33: //dịch chuyển tức thời
                            this.teleport = true;
                            break;
                        case 34:
                            this.setTinhAn += 1;
                            break;
                        case 35:
                            this.setNguyetAn += 1;
                            break;
                        case 36:
                            this.setNhatAn += 1;
                            break;
                        case 47: //Giáp+#
                            this.defAdd += io.param;
                            break;
                        case 48: //HP/KI+#
                            this.hpAdd += io.param;
                            this.mpAdd += io.param;
                            break;
                        case 49: //Tấn công+#%
                        case 50: //Sức đánh+#%
                        case ItemService.MAGE_DAMAGE_PERCENT:
                            this.tlDame.add(io.param);
                            break;
                        case ItemService.PERCENT_SD_HP_KI:
                            this.tlDame.add(io.param);
                            this.tlHp.add(io.param);
                            this.tlMp.add(io.param);
                            break;
                        case 77: //HP+#%
                        case ItemService.MAGE_HP_PERCENT:
                            this.tlHp.add(io.param);
                            break;
                        case 80: //HP+#%/30s
                            this.tlHpHoi += io.param;
                            break;
                        case 81: //MP+#%/30s
                            this.tlMpHoi += io.param;
                            break;
                        case 88: //Cộng #% exp khi đánh quái
                            this.tlTNSM.add(io.param);
                            break;
                        case 94: //Giáp #%
                            this.tlGiap += io.param;
                            break;
                        case 95: //Biến #% tấn công thành HP
                            this.tlHutHp += io.param;
                            break;
                        case 96: //Biến #% tấn công thành MP
                            this.tlHutMp += io.param;
                            break;
                        case 97: //Phản #% sát thương
                            this.tlPST += io.param;
                            break;
                        case 98: //Xuyen giap chuong
                            this.tlxgc += io.param;
                            break;
                        case 99: //Xuyen giap can chien
                            this.tlxgcc += io.param;
                            break;
                        case 100: //+#% vàng từ quái
                            this.tlGold += io.param;
                            break;
                        case 101: //+#% TN,SM
                            this.tlTNSM.add(io.param);
                            break;
                        case 103: //KI +#%
                        case ItemService.MAGE_KI_PERCENT:
                            this.tlMp.add(io.param);
                            break;
                        case 104: //Biến #% tấn công quái thành HP
                            this.tlHutHpMob += io.param;
                            break;
                        case 105: //Vô hình khi không đánh quái và boss
                            this.wearingVoHinh = true;
                            break;
                        case 106: //Không ảnh hưởng bởi cái lạnh
                            this.isKhongLanh = true;
                            break;
                        case 108: //#% Né đòn
                            this.tlNeDon += io.param;
                            break;
                        case 109: //Hôi, giảm #% HP
                            this.tlHpGiamODo += io.param;
                            break;
                        case 116: //Kháng thái dương hạ san
                            this.khangTDHS = true;
                            break;
                        case 117: //Đẹp +#% SĐ cho mình và người xung quanh
                            if (io.param > this.tlSexyDame) {
                                this.tlSexyDame = io.param;
                            }
                            break;
                        case 147: //+#% sức đánh
                            this.tlDame.add(io.param);
                            break;
                        case 156: //Giảm 50% sức đánh, HP, KI và +#% SM, TN, vàng từ quái
                            this.tlSubSD += 50;
                            this.tlTNSM.add(io.param);
                            this.tlGold += io.param;
                            break;
                        case 162: //Cute hồi #% KI/s bản thân và xung quanh
                            this.mpHoiCute += io.param;
                            break;
                        case 173: //Phục hồi #% HP và KI cho đồng đội
                            this.tlHpHoiBanThanVaDongDoi += io.param;
                            this.tlMpHoiBanThanVaDongDoi += io.param;
                            break;
                        case 211:
                            this.setltdb += 1;
                            break;
                        case 153: //% phát nổ sau khi chết
                            this.tlBom += io.param;
                            break;
                            }
                        }
                    }
                }
        }

        if (this.player.itemTime != null && this.player.itemTime.isBanhGatoNhen) {
            this.tlGiap += 10;
        }

        // Bông tai cấp 2
        if (this.player.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2 || this.player.fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA_BLUE) {
            this.player.inventory.itemsBag.stream()
                    .filter(it -> it.isNotNullItem() && it.template.id == 921)
                    .findFirst()
                    .ifPresent(btc2 -> {
                        for (ItemOption io : btc2.itemOptions) {
                            addOption(io);
                            if (io.optionTemplate.id == 72) {
                                this.levelBT = io.param;
                            }
                        }
                    });
        }
        if (this.player.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3 || this.player.fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA_BLUE) {
            this.player.inventory.itemsBag.stream()
                    .filter(it -> it.isNotNullItem() && it.template.id == 1492)
                    .findFirst()
                    .ifPresent(btc2 -> {
                        for (ItemOption io : btc2.itemOptions) {
                            addOption(io);
                            if (io.optionTemplate.id == 72) {
                                this.levelBT = io.param;
                            }
                        }
                    });
        }

        this.player.setClothes.worldcup = 0;
        for (Item item : this.player.inventory.itemsBody) {
            if (item.isNotNullItem()) {
                switch (item.template.id) {
                    case 966:
                    case 982:
                    case 983:
                    case 883:
                    case 904:
                        player.setClothes.worldcup++;
                }
                if (item.template.id >= 592 && item.template.id <= 594) {
                    teleport = true;
                }
                for (ItemOption io : item.itemOptions) {
                    addOption(io);
                }
            }
        }

        if (CombineService.duBoPhaLe(this.player)) {
            this.tlDame.add(CombineService.BUFF_PHA_LE);
            this.tlHp.add(CombineService.BUFF_PHA_LE);
            this.tlMp.add(CombineService.BUFF_PHA_LE);
        }
        setDameTrainArmor();
        setBasePoint();
        setOutfitFusion();
        setSpeed();
    }

    private void addOption(ItemOption io) {
        switch (io.optionTemplate.id) {
            case 0: //Tấn công +#
                this.dameAdd += io.param;
                break;
            case 2: //HP, KI+#000
                this.hpAdd += io.param * 1000;
                this.mpAdd += io.param * 1000;
                break;
            case 3:// vô hiệu chưởng
                this.voHieuChuong += io.param;
                break;
            case 5: //+#% sức đánh chí mạng
            case ItemService.MAGE_CRITICAL_DAMAGE_PERCENT:
                this.tlDameCrit.add(io.param);
                this.tlSDCM += io.param;
                break;
            case 6: //HP+#
                this.hpAdd += io.param;
                break;
            case 7: //KI+#
                this.mpAdd += io.param;
                break;
            case 8: //Hút #% HP, KI xung quanh mỗi 5 giây
                this.tlHutHpMpXQ += io.param;
                break;
            case 14: //Chí mạng+#%
                this.critAdd += io.param;
                break;
            case 16: // Speed
            case 114:
            case 148:
                this.tlSpeed += io.param;
                break;
            case 18: //Chinh xac
                this.tlchinhxac += io.param;
                break;
            case 19: //Tấn công+#% khi đánh quái
                this.tlDameAttMob.add(io.param);
                break;
            case 22: //HP+#K
                this.hpAdd += io.param * 1000;
                break;
            case 23: //MP+#K
                this.mpAdd += io.param * 1000;
                break;
            case 24: //Làm chậm
                this.isLamCham = true;
                break;
            case 25: //Tàn hình
                this.isTanHinh = true;
                break;
            case 26: //Hóa đá
                this.isHoaDa = true;
                break;
            case 27: //+# HP/30s
                this.hpHoiAdd += io.param;
                break;
            case 28: //+# KI/30s
                this.mpHoiAdd += io.param;
                break;
            case 33: //dịch chuyển tức thời
                this.teleport = true;
                break;
            case 34:
                this.setTinhAn += 1;
                break;
            case 35:
                this.setNguyetAn += 1;
                break;
            case 36:
                this.setNhatAn += 1;
                break;
            case 47: //Giáp+#
                this.defAdd += io.param;
                break;
            case 48: //HP/KI+#
                this.hpAdd += io.param;
                this.mpAdd += io.param;
                break;
            case 49: //Tấn công+#%
            case 50: //Sức đánh+#%
            case ItemService.MAGE_DAMAGE_PERCENT:
                this.tlDame.add(io.param);
                break;
            case ItemService.PERCENT_SD_HP_KI:
                this.tlDame.add(io.param);
                this.tlHp.add(io.param);
                this.tlMp.add(io.param);
                break;
            case 77: //HP+#%
            case ItemService.MAGE_HP_PERCENT:
                this.tlHp.add(io.param);
                break;
            case 80: //HP+#%/30s
                this.tlHpHoi += io.param;
                break;
            case 81: //MP+#%/30s
                this.tlMpHoi += io.param;
                break;
            case 88: //Cộng #% exp khi đánh quái
                this.tlTNSM.add(io.param);
                break;
            case 94: //Giáp #%
                this.tlGiap += io.param;
                break;
            case 95: //Biến #% tấn công thành HP
                this.tlHutHp += io.param;
                break;
            case 96: //Biến #% tấn công thành MP
                this.tlHutMp += io.param;
                break;
            case 97: //Phản #% sát thương
                this.tlPST += io.param;
                break;
            case 98: //Xuyen giap chuong
                this.tlxgc += io.param;
                break;
            case 99: //Xuyen giap can chien
                this.tlxgcc += io.param;
                break;
            case 100: //+#% vàng từ quái
                this.tlGold += io.param;
                break;
            case 101: //+#% TN,SM
                this.tlTNSM.add(io.param);
                break;
            case 103: //KI +#%
            case ItemService.MAGE_KI_PERCENT:
                this.tlMp.add(io.param);
                break;
            case 104: //Biến #% tấn công quái thành HP
                this.tlHutHpMob += io.param;
                break;
            case 105: //Vô hình khi không đánh quái và boss
                this.wearingVoHinh = true;
                break;
            case 106: //Không ảnh hưởng bởi cái lạnh
                this.isKhongLanh = true;
                break;
            case 108: //#% Né đòn
                this.tlNeDon += io.param;
                break;
            case 109: //Hôi, giảm #% HP
                this.tlHpGiamODo += io.param;
                break;
            case 110: //Do spl
                this.isDoSPL = true;
                break;
            case 116: //Kháng thái dương hạ san
                this.khangTDHS = true;
                break;
            case 117: //Đẹp +#% SĐ cho mình và người xung quanh
                if (io.param > this.tlSexyDame) {
                    this.tlSexyDame = io.param;
                }
                break;
            case 147: //+#% sức đánh
                this.tlDame.add(io.param);
                break;
            case 156: //Giảm 50% sức đánh, HP, KI và +#% SM, TN, vàng từ quái
                this.tlSubSD += 50;
                this.tlTNSM.add(io.param);
                this.tlGold += io.param;
                break;
            case 162: //Cute hồi #% KI/s bản thân và xung quanh
                this.mpHoiCute += io.param;
                break;
            case 159: // x chưởng
                //     this.tlDameChuong += io.param;
                break;
            case 222:
                //   this.tlDameChuong = 2;
                break;
            case 223:
                //    this.tlDameChuong = 4;
                break;
            case 160: // TNSM PET;
                this.tlTNSMPet += io.param;
                break;
            case 173: //Phục hồi #% HP và KI cho đồng đội
                this.tlHpHoiBanThanVaDongDoi += io.param;
                this.tlMpHoiBanThanVaDongDoi += io.param;
                break;
            case 211:
                this.setltdb += 1;
                break;
            case 153: //% phát nổ sau khi chết
                this.tlBom += io.param;
                break;
        }
    }

    private void setSpeed() {
        if (player.isPl()) {
            speed = (byte) (8 + 8 * (tlSpeed / 100));
        }
    }

    private void setOutfitFusion() {
        if (this.player.inventory.itemsBody.size() < 6 || this.player.pet == null || this.player.pet.inventory.itemsBody.size() < 6) {
            return;
        }
        Item skin = this.player.inventory.itemsBody.get(5);
        Item pskin = this.player.pet.inventory.itemsBody.get(5);
        if (skin.isNotNullItem() && pskin.isNotNullItem()) {
            this.isGogeta = skin.template.id == 2133 && pskin.template.id == 2134 || skin.template.id == 2134 && pskin.template.id == 2133;
        } else {
            this.isGogeta = false;
        }
    }

    private void setDameTrainArmor() {
        if (!this.player.isBoss) {
            if (this.player.inventory.itemsBody.size() < 7) {
                return;
            }
            try {
                Item gtl = this.player.inventory.itemsBody.get(6);
                if (gtl.isNotNullItem()) {
                    this.wearingTrainArmor = true;
                    this.player.inventory.trainArmor = gtl;
                    if (!this.player.isPet) {
                        this.tlSubSD += ItemService.gI().getPercentTrainArmor(gtl);
                    }
                } else if (this.player.isPet) {
                    this.wearingTrainArmor = false;
                    this.player.inventory.trainArmor = null;
                } else if (!this.player.isPet && this.player.inventory.itemsBag != null) {
                    if (this.player.inventory.trainArmor != null
                            && !this.player.inventory.itemsBag.contains(this.player.inventory.trainArmor)) {
                        this.player.inventory.trainArmor = null;
                    }
                    if (this.player.inventory.trainArmor == null) {
                        gtl = this.player.inventory.itemsBag.stream().filter(item -> item.isNotNullItem() && item.template.type == 32 && item.itemOptions != null
                                && item.itemOptions.stream().filter(io -> io.optionTemplate.id == 9 && io.param > 0).findFirst().orElse(null) != null).findFirst().orElse(null);
                        if (gtl == null) {
                            return;
                        }
                        this.player.inventory.trainArmor = gtl;
                    }
                    this.wearingTrainArmor = false;
                    if (this.player.inventory.trainArmor.itemOptions == null) {
                        return;
                    }
                    for (Item.ItemOption io : this.player.inventory.trainArmor.itemOptions) {
                        if (io.optionTemplate.id == 9 && io.param > 0) {
                            this.tlDame.add(ItemService.gI().getPercentTrainArmor(this.player.inventory.trainArmor));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Logger.error("Lỗi get giáp tập luyện " + this.player.name + "\n" + e + "\n");
            }
        }
    }

    public void setBasePoint() {
        setHpMax();
        setHp();
        setMpMax();
        setMp();
        setDame();
        setDef();
        setCrit();
        setHpHoi();
        setMpHoi();
        setLtdb();
        setThoBulma();
        setTinhNhatNguyetAn();
        setPointWithClanLevel();
    }

    private void setPointWithClanLevel() {
        if (this.player.isBoss) return;
        Clan clan = this.player.clan;
        if (clan == null && this.player.isPet && this.player instanceof Pet pet && pet.master != null) {
            clan = pet.master.clan;
        }
        if (clan != null && clan.level > 0) {
            hpMax += hpMax * (clan.level * 2L) / 100L;
            mpMax += mpMax * (clan.level * 2L) / 100L;
            dame += dame * (clan.level * 2L) / 100L;
        }
    }

    private int getMasterFusionType() {
        if (!(this.player instanceof Pet pet) || pet.master == null || pet.master.fusion == null) {
            return ConstPlayer.NON_FUSION;
        }
        return pet.master.fusion.typeFusion;
    }

    private void setLtdb() {
        this.islinhthuydanhbac = this.setltdb >= 5;
    }

    private void setThoBulma() {
        this.isThoBulma = (this.player.inventory != null && this.player.inventory.itemsBody != null
                && this.player.inventory.itemsBody.size() >= 5 && this.player.inventory.itemsBody.get(5).isNotNullItem()
                && this.player.inventory.itemsBody.get(5).template.id == 584);
    }

    private void setTinhNhatNguyetAn() {
        this.isTinhAn = this.setTinhAn >= 5;
        this.isNhatAn = this.setNhatAn >= 5;
        this.isNguyetAn = this.setNguyetAn >= 5;
    }

    private void setHpHoi() {
        this.hpHoi = this.hpMax / 100;
        this.hpHoi += this.hpHoiAdd;

        if (this.tlHpHoi > 100) {
            this.tlHpHoi = 100;
        } else if (this.tlHpHoi < 0) {
            this.tlHpHoi = 0;
        }

        this.hpHoi += ((long) this.hpMax * this.tlHpHoi / 100);

        if (this.tlHpHoiBanThanVaDongDoi > 100) {
            this.tlHpHoiBanThanVaDongDoi = 100;
        } else if (this.tlHpHoiBanThanVaDongDoi < 0) {
            this.tlHpHoiBanThanVaDongDoi = 0;
        }

        this.hpHoi += ((long) this.hpMax * this.tlHpHoiBanThanVaDongDoi / 100);
    }

    private void setMpHoi() {
        this.mpHoi = this.mpMax / 100;
        this.mpHoi += this.mpHoiAdd;

        if (this.tlMpHoi > 100) {
            this.tlMpHoi = 100;
        } else if (this.tlMpHoi < 0) {
            this.tlMpHoi = 0;
        }

        this.mpHoi += ((long) this.mpMax * this.tlMpHoi / 100);

        if (this.tlMpHoiBanThanVaDongDoi > 100) {
            this.tlMpHoiBanThanVaDongDoi = 100;
        } else if (this.tlMpHoiBanThanVaDongDoi < 0) {
            this.tlMpHoiBanThanVaDongDoi = 0;
        }

        this.mpHoi += (((long) this.mpMax * this.tlMpHoiBanThanVaDongDoi) / 100);
    }

    private void setHpMax() {
        // Tính toán giới hạn hpMax
        long hpMax = this.hpg + this.hpAdd;

        // Áp dụng các yếu tố ảnh hưởng đến hpMax
        for (Integer tl : this.tlHp) {
            if (tl == null) continue;
            hpMax += (hpMax * tl / 100L);
        }
        if (this.player.setClothes.cadicM >= 2) {
            hpMax += (hpMax * 20L / 100L);
        }
        // Xử lý set nappa
        if (this.player.setClothes.nappa == 5) {
            hpMax += (hpMax * 80L / 100L);
        }
        if (this.player.setClothes.setNhatAn == 5) {
            hpMax += (hpMax * 10 / 100);
        }

        // Xử lý set worldcup
        if (this.player.setClothes.worldcup == 2) {
            hpMax += (hpMax * 10 / 100L);
        }

        // Xử lý rồng xương
        if (player.itemTime != null && player.itemTime.isUseRX) {
            hpMax += (hpMax * 10L / 100L);
        }

        // Xử lý set nhật ấn
        if (this.isNhatAn) {
            hpMax += (hpMax * 15L / 100L);
        }

        // Xử lý ngọc rồng đen 2 sao
        if (this.player.rewardBlackBall.timeOutOfDateReward[1] > System.currentTimeMillis()) {
            hpMax += (hpMax * RewardBlackBall.R2S_1 / 100L);
        }

        // Xử lý khỉ
        if (this.player.effectSkill.isMonkey) {
            if (!this.player.isPet || (this.player.isPet && ((Pet) this.player).status != Pet.FUSION)) {
                int percent = SkillUtil.getPercentHpMonkey(player.effectSkill.levelMonkey);
                hpMax += (hpMax * percent / 100L);
            }
        }

        // Xử lý biến hình
        if (this.player.effectSkill.isBienHinh) {
            int[] percentBienHinh = {0, 15, 20, 25, 30, 35, 40};
            if (this.player.effectSkill.levelBienHinh >= 1 && this.player.effectSkill.levelBienHinh <= 6) {
                hpMax += (hpMax * percentBienHinh[this.player.effectSkill.levelBienHinh] / 100L);
            }
        }

        // Xử lý pet pic
        if (this.player.isPet && ((Pet) this.player).typePet == 3 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            hpMax += (hpMax * 15 / 100L);
        }

        // Xử lý pet mabư
        if (this.player.isPet && ((Pet) this.player).typePet == 1 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            hpMax += (hpMax * 10 / 100L);
        }

        // Xử lý pet berus
        if (this.player.isPet && ((Pet) this.player).typePet == 2 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            hpMax += (hpMax * 25 / 100L);
        }

        // Xử lý pet black
        if (this.player.isPet && ((Pet) this.player).typePet == 4 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            hpMax += (hpMax * 20 / 100L);
        }

        // Xử lý pet jiren
        if (this.player.isPet && ((Pet) this.player).typePet == 5 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            hpMax += (hpMax * 30 / 100L);
        }

        // Xử lý pet cell max
        if (this.player.isPet && ((Pet) this.player).typePet == 6 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            hpMax += (hpMax * 35 / 100L);
        }
        if (this.player.isPet && ((Pet) this.player).typePet == 3 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            hpMax += (hpMax * 20 / 100L);
        }

        // Xử lý pet mabư
        if (this.player.isPet && ((Pet) this.player).typePet == 1 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            hpMax += (hpMax * 15 / 100L);
        }

        // Xử lý pet berus
        if (this.player.isPet && ((Pet) this.player).typePet == 2 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            hpMax += (hpMax * 30 / 100L);
        }

        // Xử lý pet black
        if (this.player.isPet && ((Pet) this.player).typePet == 4 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            hpMax += (hpMax * 25 / 100L);
        }

        // Xử lý pet jiren
        if (this.player.isPet && ((Pet) this.player).typePet == 5 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            hpMax += (hpMax * 35 / 100L);
        }

        // Xử lý pet cell max
        if (this.player.isPet && ((Pet) this.player).typePet == 6 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            hpMax += (hpMax * 40 / 100L);
        }

        // Xử lý phù
        if (this.player.zone != null && MapService.gI().isMapBlackBallWar(this.player.zone.map.mapId)) {
            hpMax *= this.player.effectSkin.xHPKI;
        }

        // Xử lý thức ăn 2
        if (this.player.itemTime != null && this.player.itemTime.isEatMeal2 && this.player.itemTime.iconMeal2 == 8062) {
            hpMax += (hpMax * 5 / 100L);
        }

        // Xử lý gogeta
        if (this.isGogeta) {
            hpMax += (hpMax * 10 / 100L);
        }

        // Phù map mabu
        if (this.player.isPhuHoMapMabu) {
            hpMax += 1_000_000;
        }

        // Xử lý +hp đệ
        if (this.player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            hpMax += this.player.pet.nPoint.hpMax;
        }

        // Xử lý bổ huyết
        if (this.player.itemTime != null && this.player.itemTime.isUseBoHuyet && !this.player.itemTime.isUseBoHuyet2) {
            hpMax *= 2;
        }

        // Xử lý item sieu cap
        if (this.player.itemTime != null && this.player.itemTime.isUseBoHuyet2) {
            hpMax *= 2.2;
        }
        if (this.player.itemTime != null && this.player.itemTime.miakl) {
            hpMax *= 1.1;
        }
        if (this.player.itemTime != null && this.player.itemTime.miathom) {
            hpMax *= 1.1;
        }
        if (this.player.itemTime != null && this.player.itemTime.miasaurieng) {
            hpMax *= 1.1;
        }
        if (this.player.itemTime != null && this.player.itemTime.isSupBiHacAm) {
            hpMax += (hpMax * 10 / 100L);
        }
        // Xử lý huýt sáo (chỉ cộng cho sư phụ, không tính cho đệ tử)
        if (!this.player.isPet) {
            if (this.player.effectSkill.tiLeHPHuytSao != 0) {
                hpMax += (hpMax * this.player.effectSkill.tiLeHPHuytSao / 100L);
            }
        }

        // Xử lý chibi
        if (this.player.effectSkill != null && this.player.effectSkill.isChibi && this.player.typeChibi == 3) {
            hpMax *= 2;
        }

        // Xử lý map lạnh
        if (!this.player.isBoss && this.player.zone != null && MapService.gI().isMapCold(this.player.zone.map) && !this.isKhongLanh) {
            hpMax /= 2;
        }

        if (!this.player.isBoss && !this.player.isNewPet && TimeUtil.checkTime(EventDAO.getRemainingTimeToIncreaseHP())) {
            hpMax += hpMax / 10;
        }

        // Buff item 1016: +10% HP trong thời gian hiệu lực
        if (this.player.charms != null && this.player.charms.tdBuff1016 > System.currentTimeMillis()) {
            hpMax += hpMax / 10;
        }

        if (this.player.charms != null && this.player.charms.tdBuffZenoHP > System.currentTimeMillis()) {
            hpMax += hpMax * 15 / 100;
        } 

        this.hpMax =   hpMax;
    }

    private void setHp() {
        this.hp = Math.min(this.hp, this.hpMax);
    }

    private void setMpMax() {
        // Tính toán giới hạn mpMax
        long mpMax = this.mpg + this.mpAdd;

        // Áp dụng các yếu tố ảnh hưởng đến mpMax
        for (Integer tl : this.tlMp) {
            if (tl == null) continue;
            mpMax += (mpMax * tl / 100L);
        }

        // Xử lý set picolo
        if (this.player.setClothes.picolo == 5) {
            mpMax *= 2;
        }
        if (this.player.setClothes.setNguyetAn == 5) {
            mpMax += (mpMax * 10 / 100);
        }

        // Xử lý set nguyệt ấn
        if (this.isNguyetAn) {
            mpMax += (mpMax * 15L / 100L);
        }

        // Xử lý ngọc rồng đen 6 sao
        if (this.player.rewardBlackBall.timeOutOfDateReward[5] > System.currentTimeMillis()) {
            mpMax += (mpMax * RewardBlackBall.R6S_1 / 100L);
        }

        // Xử lý set worldcup
        if (this.player.setClothes.worldcup == 2) {
            mpMax += (this.mpMax * 10 / 100L);
        }

        // Xử lý pet pic
        if (this.player.isPet && ((Pet) this.player).typePet == 3
                && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            mpMax += (this.mpMax * 15 / 100L);
        }

        // Xử lý pet mabư
        if (this.player.isPet && ((Pet) this.player).typePet == 1
                && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            mpMax += (this.mpMax * 10 / 100L);
        }

        // Xử lý pet br
        if (this.player.isPet && ((Pet) this.player).typePet == 2
                && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            mpMax += (this.mpMax * 25 / 100L);// MP berus
        }

        // Xử lý pet black
        if (this.player.isPet && ((Pet) this.player).typePet == 4
                && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            mpMax += (this.mpMax * 20 / 100L);// MP black
        }

        // Xử lý pet jiren
        if (this.player.isPet && ((Pet) this.player).typePet == 5
                && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            mpMax += (this.mpMax * 30 / 100L);// MP jiren
        }

        // Xử lý pet cell max
        if (this.player.isPet && ((Pet) this.player).typePet == 6
                && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            mpMax += (this.mpMax * 35 / 100L);// MP cell max
        }
        if (this.player.isPet && ((Pet) this.player).typePet == 3
                && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            mpMax += (this.mpMax * 20 / 100L);
        }

        // Xử lý pet mabư
        if (this.player.isPet && ((Pet) this.player).typePet == 1
                && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            mpMax += (this.mpMax * 15 / 100L);
        }

        // Xử lý pet br
        if (this.player.isPet && ((Pet) this.player).typePet == 2
                && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            mpMax += (this.mpMax * 30 / 100L);// MP berus
        }

        // Xử lý pet black
        if (this.player.isPet && ((Pet) this.player).typePet == 4
                && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            mpMax += (this.mpMax * 25 / 100L);// MP black
        }

        // Xử lý pet jiren
        if (this.player.isPet && ((Pet) this.player).typePet == 5
                && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            mpMax += (this.mpMax * 35 / 100L);// MP jiren
        }

        // Xử lý pet cell max
        if (this.player.isPet && ((Pet) this.player).typePet == 6
                && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            mpMax += (this.mpMax * 40 / 100L);// MP cell max
        }

        // Xử lý phù
        if (this.player.zone != null && MapService.gI().isMapBlackBallWar(this.player.zone.map.mapId)) {
            mpMax *= this.player.effectSkin.xHPKI;
        }

        // Xử lý gogeta
        if (this.isGogeta) {
            mpMax += (mpMax * 10 / 100L);
        }

        // Phù map mabu
        if (this.player.isPhuHoMapMabu) {
            mpMax += 1_000_000;
        }

        // Xử lý rồng xương
        if (player.itemTime != null && player.itemTime.isUseRX) {
            mpMax += (mpMax * 10L / 100L);
        }

        // Xử lý hợp thể
        if (this.player.fusion.typeFusion != 0) {
            mpMax += this.player.pet.nPoint.mpMax;
        }

        // Xử lý bổ khí
        if (this.player.itemTime != null && this.player.itemTime.isUseBoKhi && !this.player.itemTime.isUseBoKhi2) {
            mpMax *= 2;
        }

        // Xử lý item sieu cap
        if (this.player.itemTime != null && this.player.itemTime.isUseBoKhi2) {
            mpMax *= 2.2;
        }

        // Xử lý biến hình
        if (this.player.effectSkill.isBienHinh) {
            int[] percentBienHinh = {0, 15, 20, 25, 30, 35, 40};
            if (this.player.effectSkill.levelBienHinh >= 1 && this.player.effectSkill.levelBienHinh <= 6) {
                mpMax += (mpMax * percentBienHinh[this.player.effectSkill.levelBienHinh] / 100L);
            }
        }

        if (!this.player.isBoss && !this.player.isNewPet && TimeUtil.checkTime(EventDAO.getRemainingTimeToIncreaseMP())) {
            mpMax += mpMax / 10;
        }

        // Buff item 1016: +10% KI trong thời gian hiệu lực
        if (this.player.charms != null && this.player.charms.tdBuff1016 > System.currentTimeMillis()) {
            mpMax += mpMax / 10;
        }
        
        if (this.player.charms != null && this.player.charms.tdBuffZenoKI > System.currentTimeMillis()) {
            mpMax += mpMax * 15 / 100;
        }

        this.mpMax =  mpMax;
    }

    private void setMp() {
        this.mp = Math.min(this.mp, this.mpMax);
    }

    public long getHP() {
        return Math.min(this.hp, this.hpMax);
    }

    public void setHP(long hp) {
        if (hp > 0) {
            this.hp =   (hp <= this.hpMax ? hp : this.hpMax);
        } else {
            player.setDie();
        }
    }

    public long getMP() {
        return Math.min(this.mp, this.mpMax);
    }

    public void setMP(long mp) {
        if (mp > 0) {
            this.mp =   (mp <= this.mpMax ? mp : this.mpMax);
        } else {
            this.mp = 0;
        }
    }

    public static long calPercent(long param, int percent) {
        return param * percent / 150;
    }

    private void setDame() {
        long dame = this.dameg + this.dameAdd;

        for (Integer tl : this.tlDame) {
            if (tl == null) continue;
            dame += (dame * tl / 100L);
        }

//        for (Integer tl : this.tlSDDep) {
//            dame += (dame * tl / 100L);
//        }
        if (this.player.itemTime != null && this.player.itemTime.isXimuoihoadao) {
            dame += (dame * 20 / 100L);
        }
        if (this.player.setClothes.setTinhAn == 5) {
            dame += (dame * 10 / 100);
        }
        if (this.player.itemTime != null && this.player.itemTime.isXimuoihoamai) {
            dame += (dame * 20 / 100L);
        }
        // Xử lý pet pic
        if (this.player.isPet && ((Pet) this.player).typePet == 3 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            dame += (dame * 15 / 100L);
        }

        // Xử lý pet mabư
        if (this.player.isPet && ((Pet) this.player).typePet == 1 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            dame += (dame * 10 / 100L);
        }

        // Xử lý pet br
        if (this.player.isPet && ((Pet) this.player).typePet == 2 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            dame += (dame * 25 / 100L);
        }

        // Xử lý pet black
        if (this.player.isPet && ((Pet) this.player).typePet == 4 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            dame += (dame * 20 / 100L);
        }

        // Xử lý pet jiren
        if (this.player.isPet && ((Pet) this.player).typePet == 5 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            dame += (dame * 30 / 100L);
        }

        // Xử lý pet cell max
        if (this.player.isPet && ((Pet) this.player).typePet == 6 && (getMasterFusionType() == ConstPlayer.HOP_THE_PORATA || getMasterFusionType() == ConstPlayer.HOP_THE_PORATA2)) {
            dame += (dame * 35 / 100L);
        }
        if (this.player.isPet && ((Pet) this.player).typePet == 3 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            dame += (dame * 20 / 100L);
        }
        // Xử lý pet mabư
        if (this.player.isPet && ((Pet) this.player).typePet == 1 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            dame += (dame * 15 / 100L);
        }

        // Xử lý pet br
        if (this.player.isPet && ((Pet) this.player).typePet == 2 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            dame += (dame * 30 / 100L);
        }

        // Xử lý pet black
        if (this.player.isPet && ((Pet) this.player).typePet == 4 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            dame += (dame * 25 / 100L);
        }

        // Xử lý pet jiren
        if (this.player.isPet && ((Pet) this.player).typePet == 5 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            dame += (dame * 35 / 100L);
        }

        // Xử lý pet cell max
        if (this.player.isPet && ((Pet) this.player).typePet == 6 && getMasterFusionType() == ConstPlayer.HOP_THE_PORATA3) {
            dame += (dame * 40 / 100L);
        }

        // Xử lý set tinh ấn
        if (this.isTinhAn) {
            dame += (dame * 15L / 100L);
        }

        // Xử lý thức ăn
        if (!this.player.isPet && this.player.itemTime != null && this.player.itemTime.isEatMeal
                || this.player.isPet && ((Pet) this.player).master != null && ((Pet) this.player).master.itemTime != null && ((Pet) this.player).master.itemTime.isEatMeal) {
            dame += (dame * 10 / 100L);
        }

        // Xử lý thức ăn 2
        if (this.player.itemTime != null && this.player.itemTime.isEatMeal2 && this.player.itemTime.iconMeal2 == 8060) {
            dame += (dame * 5 / 100L);
        }
        if (this.player.setClothes.nail >= 2) {
            this.tlDameCrit.add(20);
        }

        if (this.player.itemTime != null && this.player.itemTime.banhtet) {
            dame += (dame * 15 / 100L);
        }
        if (this.player.itemTime != null && this.player.itemTime.miasaurieng) {
            dame += (dame * 10 / 100L);
        }
        if (this.player.itemTime != null && this.player.itemTime.banhchung) {
            dame += (dame * 25 / 100L);
        }
        if (this.player.itemTime != null && this.player.itemTime.isHamburgerSau) {
            dame += (dame * 10 / 100L);
        }
        // Xử lý thức ăn 2
        if (this.player.itemTime != null && this.player.itemTime.isEatMeal2 && this.player.itemTime.iconMeal2 == 8061) {
            this.tlDameCrit.add(5);
            this.tlSDCM += 5;
        }

        // Xử lý cuồng nộ
        if (this.player.itemTime != null && this.player.itemTime.isUseCuongNo && !this.player.itemTime.isUseCuongNo2) {
            dame *= 2;
        }
        if (this.player.itemTime != null && this.player.itemTime.isUseCuongNo2) {
            dame *= 2.2;
        }

        // Xử lý ngọc rồng đen 1 sao
        if (this.player.rewardBlackBall.timeOutOfDateReward[0] > System.currentTimeMillis()) {
            dame += (dame * RewardBlackBall.R1S_2 / 100L);
        }

        // Xử lý set worldcup
        if (this.player.setClothes.worldcup == 2) {
            dame += (dame * 10 / 100L);
        }

        // Xử lý gogeta
        if (this.isGogeta) {
            dame += (dame * 10 / 100L);
        }

        // Phù map mabu
        if (this.player.isPhuHoMapMabu) {
            dame += 10_000;
        }

        // Xử lý rồng xương
        if (player.itemTime != null && player.itemTime.isUseRX) {
            dame += (dame * 10L / 100L);
        }

        // Xử lý phù
        if (this.player.zone != null && MapService.gI().isMapBlackBallWar(this.player.zone.map.mapId)) {
            dame *= this.player.effectSkin.xDame;
        }

        // Xử lý hợp thể
        if (this.player.fusion.typeFusion != 0) {
            dame += this.player.pet.nPoint.dame;
        }

        // Xử lý khỉ
        if (this.player.effectSkill.isMonkey) {
            if (!this.player.isPet || (this.player.isPet && ((Pet) this.player).status != Pet.FUSION)) {
                int percent = SkillUtil.getPercentDameMonkey(player.effectSkill.levelMonkey);
                dame += (dame * percent / 100L);
            }
        }

        // Xử lý biến hình
        if (this.player.effectSkill.isBienHinh) {
            int[] percentBienHinh = {0, 15, 20, 25, 30, 35, 40};
            if (this.player.effectSkill.levelBienHinh >= 1 && this.player.effectSkill.levelBienHinh <= 6) {
                dame += (dame * percentBienHinh[this.player.effectSkill.levelBienHinh] / 100L);
            }
        }

        //Sức đánh đẹp
        dame += (dame * tlSexyDame / 100L);

        // Xử lý giảm dame
        dame -= (dame * tlSubSD / 100L);

        // Xử lý map cold
        if (!this.player.isBoss && this.player.zone != null && MapService.gI().isMapCold(this.player.zone.map) && !this.isKhongLanh) {
            dame /= 2;
        }

        if (!this.player.isBoss && !this.player.isNewPet && TimeUtil.checkTime(EventDAO.getRemainingTimeToIncreaseDame())) {
            dame += dame / 10;
        }

        // Buff item 1016: +10% sức đánh trong thời gian hiệu lực
        if (this.player.charms != null && this.player.charms.tdBuff1016 > System.currentTimeMillis()) {
            dame += dame / 10;
        }
        
        if (this.player.charms != null && this.player.charms.tdBuffZenoSD > System.currentTimeMillis()) {
            dame += dame * 15 / 100;
        }
        
        if (dame < 0) {
            dame = 0;
        }

        this.dame =   dame;
    }

    private void setDef() {
        this.def = this.defg * 4;
        this.def += this.defAdd;
        if (this.player.itemTime != null && this.player.itemTime.miasaurieng) {
            this.def *= 1.1;
        }
        if (this.player.itemTime != null && this.player.itemTime.isBanhGatoNhen) {
            this.def *= 1.1;
        }
    }

    private void setCrit() {
        this.crit = this.critg;
        this.crit += this.critAdd;
        if (this.player.effectSkill.isMonkey) {
            this.crit = 110;
        }
        if (player.setClothes.thanVuTruKaio >= 2) {
            this.crit += 20;
        }
        if (this.player.itemTime != null && this.player.itemTime.banhtet) {
            this.crit += 15;
        }
        if (this.player.itemTime != null && this.player.itemTime.banhchung) {
            this.crit += 25;
        }
        if (this.player.itemTime != null && this.player.itemTime.miathom) {
            this.crit += 10;
        }
        if (this.player.itemTime != null && this.player.itemTime.isKeoMotMat) {
            this.crit += 5;
        }
    }

    private void resetPoint() {
        this.voHieuChuong = 0;
        this.hpAdd = 0;
        this.mpAdd = 0;
        this.dameAdd = 0;
        this.defAdd = 0;
        this.critAdd = 0;
        this.tlHp.clear();
        this.tlMp.clear();
        this.tlDef.clear();
        this.tlDame.clear();
        this.tlDameCrit.clear();
        this.tlDameAttMob.clear();
        this.tlSDCM = 0;
        this.tlHpHoiBanThanVaDongDoi = 0;
        this.tlMpHoiBanThanVaDongDoi = 0;
        this.hpHoi = 0;
        this.mpHoi = 0;
        this.mpHoiCute = 0;
        this.tlHpHoi = 0;
        this.tlMpHoi = 0;
        this.tlHutHp = 0;
        this.tlHutMp = 0;
        this.tlHutHpMob = 0;
        this.tlHutHpMpXQ = 0;
        this.tlPST = 0;
        this.tlTNSM.clear();
        this.tlDameAttMob.clear();
        this.tlGold = 0;
        this.tlNeDon = 0;
        this.tlBom = 0;
        this.tlGiap = 0;
        this.tlxgcc = 0;
        this.tlxgc = 0;
        this.tlchinhxac = 0;
        this.tlTNSMPet = 0;
        this.xChuong = 0;
        this.setltdb = 0;
        this.setTinhAn = 0;
        this.setNhatAn = 0;
        this.setNguyetAn = 0;
        this.tlSexyDame = 0;
        this.tlSubSD = 0;
        this.tlHpGiamODo = 0;
        this.tlSpeed = 0;
        this.teleport = false;

        this.wearingVoHinh = false;
        this.isKhongLanh = false;
        this.khangTDHS = false;
        this.isTanHinh = false;
        this.isHoaDa = false;
        this.isLamCham = false;
        this.isDoSPL = false;
        this.isThoBulma = false;
        this.xDameChuong = false;
    }

    public void addHp(long hp) {
        if (hp > 0) {
            long potentialHp = (long) this.hp + hp;
            if (potentialHp > this.hpMax) {
                this.hp = this.hpMax;
            } else {
                this.hp = potentialHp;
            }
        }
    }

    public void addMp(long mp) {
        long potentialMp = this.mp + mp;

        if (potentialMp > this.mpMax) {
            this.mp = this.mpMax;
        } else if (potentialMp < 0) {
            this.mp = 0;
        } else {
            this.mp =   potentialMp;
        }
    }

    public void setHp(long hp) {
        if (hp < 0) {
            this.hp = 0;
        } else {
            this.hp =   Math.min(hp, Long.MAX_VALUE);
        }
    }

    public void setMp(long mp) {
        if (mp < 0) {
            this.mp = 0;
        } else {
            this.mp = Math.min(mp, Long.MAX_VALUE);
        }
    }

    private void setIsCrit() {
        if (intrinsic != null && intrinsic.id == 25
                && this.getCurrPercentHP() <= intrinsic.param1) {
            isCrit = true;
        } else if (isCrit100) {
            isCrit100 = false;
            isCrit = true;
        } else {
            isCrit = Util.isTrue(this.crit, ConstRatio.PER100);
        }
    }

    public int getDameAttack(boolean isAttackMob) {
        return getDameAttack(isAttackMob, false);
    }

    public int getDameAttack(boolean isAttackMob, boolean guaranteedCrit) {
        // Resolve the equipped intrinsic before rolling this hit's critical chance.
        intrinsic = this.player.playerIntrinsic.intrinsic;
        setIsCrit();
        // Target effects apply only to this hit, never to a later unbound target.
        if (guaranteedCrit) isCrit = true;
        long dameAttack = this.dame;
        percentDameIntrinsic = 0;
        int percentDameSkill = 0;
        byte percentXDame = 0;
        Skill skillSelect = player.playerSkill.skillSelect;
        if (skillSelect.template.id != Skill.DICH_CHUYEN_TUC_THOI && isCritTele) {
            isCrit = true;
            isCritTele = false;
        }
        switch (skillSelect.template.id) {
            case Skill.DRAGON:
                if (intrinsic.id == 1) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                percentDameSkill = skillSelect.damage;
                break;
            case Skill.KAMEJOKO:
                if (intrinsic.id == 2) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                percentDameSkill = skillSelect.damage;
                if (this.player.setClothes.songoku == 5) {
                    percentXDame = 100;
                }
                break;
            case Skill.GALICK:
                if (intrinsic.id == 16) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                percentDameSkill = skillSelect.damage;
                if (this.player.setClothes.kakarot == 5) {
                    percentXDame = 100;
                }
                break;
            case Skill.ANTOMIC:
                if (intrinsic.id == 17) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                percentDameSkill = skillSelect.damage;
                break;
            case Skill.DEMON:
                if (intrinsic.id == 8) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                percentDameSkill = skillSelect.damage;
                break;
            case Skill.MASENKO:
                if (intrinsic.id == 9) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                if (this.player.setClothes.nail == 5) {
                    percentXDame = 50;
                }
                percentDameSkill = skillSelect.damage;
                break;
            case Skill.KAIOKEN:
                if (intrinsic.id == 26) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                percentDameSkill = skillSelect.damage;
                if (this.player.setClothes.kaioken == 5) {
                    percentXDame = 100;
                } else if (player.setClothes.thanVuTruKaio == 5) {
                    percentXDame = 30;
                }
                break;
            case Skill.TU_SAT:
                percentDameSkill = skillSelect.damage;
                if (this.player.setClothes.cadicM == 4) {
                    percentXDame = 20;
                } else if (this.player.setClothes.cadicM == 5) {
                    percentXDame = 40;
                }
                break;
            case Skill.LIEN_HOAN:
                if (intrinsic.id == 13) {
                    percentDameIntrinsic = intrinsic.param1;
                }
                percentDameSkill = skillSelect.damage;
                if (this.player.setClothes.ocTieu == 5) {
                    percentXDame = 100;
                }
                break;
            case Skill.DICH_CHUYEN_TUC_THOI: {
                isCrit = true;
                isCritTele = true;
                long dmgBase = Math.max(0L, dameAttack);
                long dmgDelta = dmgBase / 100L * 5L;
                long dmgFromTele = Math.max(0L, Math.min(2_000_000_000L, dmgBase - dmgDelta));
                long dmgToTele = Math.max(dmgFromTele, Math.min(2_000_000_000L, dmgBase + dmgDelta));
                dameAttack = Util.nextInt((int) dmgFromTele, (int) dmgToTele);
                break;
                }
            case Skill.MAKANKOSAPPO:
                percentDameSkill = skillSelect.damage;
                int dameSkill = (int) Math.min(2_000_000_000L, (long) this.mpMax * percentDameSkill / 100);
                return dameSkill;
            case Skill.QUA_CAU_KENH_KHI:
                long totalHpMob = 0;
                long totalHpPlayer = 0;
                int mobCount = 0;
                int playerCount = 0;

                // Tổng HP mob trong phạm vi
                for (Mob mob : this.player.zone.mobs) {
                    if (!mob.isDie() && Util.getDistance(this.player, mob) <= SkillUtil.getRangeQCKK(this.player.playerSkill.skillSelect.point)) {
                        totalHpMob += mob.point.hp;
                        mobCount++;
                    }
                }

                // Tổng HP người chơi trong phạm vi
                for (Player pl : this.player.zone.getHumanoids()) {
                    if (!pl.isDie() && this.player.id != pl.id
                            && Util.getDistance(this.player, pl) <= SkillUtil.getRangeQCKK(this.player.playerSkill.skillSelect.point)) {
                        totalHpPlayer += pl.nPoint.hp;
                        playerCount++;
                    }
                }

                // Trung bình HP
                long avgHpMob = mobCount > 0 ? totalHpMob / mobCount : 0;
                long avgHpPlayer = playerCount > 0 ? totalHpPlayer / playerCount : 0;

                // Dame cơ bản
                long baseDame = this.dame;

                // Hút năng lượng xung quanh (10% HP trung bình)
                long energyAbsorb = (avgHpMob * 1 / 100) + (avgHpPlayer * 1 / 100);

                // Tổng dame = dame cơ bản * 10 + năng lượng hút được
                long dameQCKK = (baseDame * 5) + energyAbsorb;

                // Nếu có set Kirin 5 sao → nhân đôi
                if (this.player.setClothes.kirin == 5) {
                    dameQCKK *= 2;
                }

                // Dao động ngẫu nhiên ±5%
                dameQCKK += (Util.nextInt(-5, 5) * dameQCKK / 100);

                // Giới hạn dame (để tránh overflow khi ép int)
                if (dameQCKK > Integer.MAX_VALUE) {
                    dameQCKK = Integer.MAX_VALUE;
                } else if (dameQCKK < 0) {
                    dameQCKK = 0;
                }

                return (int) dameQCKK;

            case Skill.DE_TRUNG:
                if (player.setClothes.pikkoroDaimao == 5) {
                    dameAttack *= 4;
                }
                if (dameAttack > 2_000_000_000) {
                    dameAttack = 2_000_000_000;
                }
                return (int) dameAttack;
        }

        if (intrinsic.id == 18 && this.player.effectSkill.isMonkey) {
            percentDameIntrinsic = intrinsic.param1;
        }

        if (percentDameSkill != 0) {
            dameAttack = dameAttack * percentDameSkill / 100;
        }
        dameAttack += (dameAttack * percentDameIntrinsic / 100);
        dameAttack += (dameAttack * dameAfter / 100);
        if (this.player.effectSkill != null && this.player.effectSkill.isDameBuff && tlSexyDame == 0) {
            int tiLeDame = this.player.effectSkill.tileDameBuff;
            dameAttack += (dameAttack * tiLeDame / 100L);
        }
        if (isAttackMob) {
            for (Integer tl : this.tlDameAttMob) {
                dameAttack += (dameAttack * tl / 100);
            }
            if (this.player.itemTime != null && this.player.itemTime.isKhauTrang) {
                dameAttack += (dameAttack * 10 / 100);
            }
            if (this.player.isPet && ((Pet) this.player).master.charms.tdDeTu > System.currentTimeMillis()) {
                dameAttack *= 2;
            }
            if (this.player.isBuffGuruWar) {
                if (System.currentTimeMillis() - this.player.lastTimeWinGuruWar < 24 * 60 * 60 * 1000L) {
                    dameAttack += (dameAttack * 20 / 100); // Buff Thần Guru: +20% SĐ khi đánh quái
                } else {
                    this.player.isBuffGuruWar = false;
                }
            }
        }

        dameAfter = 0;

        if (isCrit) {
            dameAttack *= 2;
            dameAttack += (dameAttack * tlSDCM / 100);
        }

        dameAttack += ((long) dameAttack * percentXDame / 100);

        long tempDameAttack = (long) (dameAttack / 100L * 5L);
        if (tempDameAttack <= 0) {
            tempDameAttack = 1;
        }
        dameAttack += (long) (Util.getOne(-1, 1) * Util.nextInt((int) tempDameAttack) + 1);
        if (dameAttack > 2_000_000_000) {
            dameAttack = 2_000_000_000;
        }

        return (int) dameAttack;
    }

    public int getCurrPercentHP() {
        if (this.hpMax == 0) {
            return 100;
        }
        return (int) ((long) this.hp * 100 / this.hpMax);
    }

    public int getCurrPercentMP() {
        return (int) ((long) this.mp * 100 / this.mpMax);
    }

    public void setFullHpMp() {
        this.hp = this.hpMax;
        this.mp = this.mpMax;
    }

    public void subHP(long sub) {
        this.hp -= sub;
        if (this.hp <= 0) {
            this.hp = 0;
            this.setHp(0);
        }
    }

    public void subMP(long sub) {
        this.mp -= sub;
        if (this.mp <= 0) {
            this.mp = 0;
        }
 
    }

    public int getGoldFromMob(int baseGold) {
        Intrinsic activeIntrinsic = this.player.playerIntrinsic == null ? null : this.player.playerIntrinsic.intrinsic;
        long bonus = Math.max(0, this.tlGold);
        if (activeIntrinsic != null && activeIntrinsic.id == 23) bonus += Math.max(0, activeIntrinsic.param1);
        long gold = Math.max(0, baseGold);
        gold += gold * bonus / 100;
        return (int) Math.min(Integer.MAX_VALUE, gold);
    }

    public long calSucManhTiemNang(long tiemNang) {
        if (power < getPowerLimit()) {
            for (Integer tl : this.tlTNSM) {
                tiemNang += ((long) tiemNang * tl / 100);
            }
            if (this.player.itemTime != null && this.player.itemTime.isKhauTrang) {
                tiemNang += ((long) tiemNang * 5 / 100);
            }
            long tn1 = tiemNang;
            if (this.player.itemTime != null && this.player.itemTime.isTnDeTu) {
                tiemNang += tn1 * 2;
            }
            if (this.player.cFlag != 0) {
                if (this.player.cFlag == 8) {
                    tiemNang += ((long) tiemNang * 10 / 100);
                } else {
                    tiemNang += ((long) tiemNang * 5 / 100);
                }
            }
            long tn = tiemNang;
            if (this.player.charms.tdTriTue > System.currentTimeMillis()) {
                tiemNang += tn;
            }
            if (this.player.charms.tdTriTue3 > System.currentTimeMillis()) {
                tiemNang += tn * 2;
            }
            if (this.player.charms.tdTriTue4 > System.currentTimeMillis()) {
                tiemNang += tn * 3;
            }
            if (this.player.charms.tdTriTue4 > System.currentTimeMillis()) {
                tiemNang += tn * 3;
            }
            if (this.player.effectSkill.isChibi && this.player.typeChibi == 2) {
                tiemNang += tn * 2;
            }
//            if (this.player.getSession() != null && this.player.getSession().vip > 0 && System.currentTimeMillis() < this.player.getSession().lastTimeReceivedVIP + (1000 * 60 * 60 * 24 * 7) || this.player.isPet
//                    && ((Pet) this.player).master.getSession() != null && ((Pet) this.player).master.getSession().vip > 0 && System.currentTimeMillis() < ((Pet) this.player).master.getSession().lastTimeReceivedVIP + (1000 * 60 * 60 * 24 * 7)) {
//                tiemNang += tn * 3;
//            }
            if (this.player.itemTime != null && this.player.itemTime.isUseDK) {
                tiemNang += tn * 2;
            }
            if (this.player.itemTime != null && this.player.itemTime.isBuaTNSM) {
                tiemNang += tn * 2;
            }
            if (this.player.satellite != null && this.player.satellite.isIntelligent) {
                tiemNang += tn / 5;
            }
            if (this.player.itemTime != null && this.player.itemTime.tnsmX > 1) {
                // bình x3/x5/x7 (item 1357-1359): cộng thêm (x-1) lần để tổng thành x lần
                tiemNang += tn * (this.player.itemTime.tnsmX - 1);
            }
            Intrinsic activeIntrinsic = this.player.playerIntrinsic == null ? null : this.player.playerIntrinsic.intrinsic;
            if (activeIntrinsic != null && activeIntrinsic.id == 24) {
                tiemNang += ((long) tiemNang * activeIntrinsic.param1 / 100);
            }
            if (this.power >= 60_000_000_000L) {
                tiemNang -= ((long) tiemNang * 99 / 100);
            }
            if (this.player.isPet) {
                if (((Pet) this.player).master.charms.tdDeTu > System.currentTimeMillis()) {
                    tiemNang += tn * 2;
                }
                if (((Pet) this.player).master.nPoint != null && ((Pet) this.player).master.nPoint.tlTNSMPet > 0) {
                    tiemNang += tn / 100 * (((Pet) this.player).master.nPoint.tlTNSMPet + 100);
                }
            }
            if (this.player.isPet) {
                Pet pet = (Pet) this.player;
                Player master = pet.master;
                if (master != null && master.itemTime != null && master.itemTime.isBuaTNSM) {
                    tiemNang += tn * 2; // nhân đôi tiềm năng cho đệ tử
                }
                if (master != null && master.itemTime != null && master.itemTime.tnsmX > 1) {
                        tiemNang += tn * (master.itemTime.tnsmX - 1); // đệ tử hưởng bình x3/x5/x7 theo sư phụ
                    }
            }

//            if (TimeUtil.checkTime(EventDAO.getRemainingTimeToIncreasePotentialAndPower())) {
//                tiemNang *= 2;
//            }
//            if (this.player.zone != null && this.player.zone.map != null && MapService.gI().isMapNguHanhSon(this.player.zone.map.mapId)) {
//                tiemNang *= 4;
//            }
            if (MapService.gI().isMapBanDoKhoBau(this.player.zone.map.mapId)) {
                tiemNang *= 20;
                if (tiemNang > 50_000_000) {
                    tiemNang = 50_000_000;
                }
            }
            if (MapService.gI().isMapDoanhTrai(this.player.zone.map.mapId)) {
                tiemNang *= 20;
                if (tiemNang > 50_000_000) {
                    tiemNang = 50_000_000;
                }
            }
//            if (MapService.gI().isMapConDuongRanDoc(this.player.zone.map.mapId)) {
//                tiemNang *= 10;
//                if (tiemNang > 50_000_000) {
//                    tiemNang = 50_000_000;
//                }
//            }
    //            if (MapService.gI().isMapHanhTinhNgucTu(this.player.zone.map.mapId)) {
    //                tiemNang *= 0.5f;
    //
    //            }
            Clan clan = this.player.clan;
            if (clan == null && this.player.isPet && this.player instanceof Pet pet && pet.master != null) {
                clan = pet.master.clan;
            }
            if (clan != null && clan.level > 0) {
                tiemNang += tiemNang * (clan.level * 5L) / 100L;
            }
            if (player.zone.map.mapId == 79) {
                tiemNang *= 2;
            }
            if (player.zone.map.mapId >= 105 && player.zone.map.mapId <= 110) {
                tiemNang *= 3;
            }
            if (player.zone.map.mapId == 155) {
                tiemNang *= 3;
            } else if (player.zone.map.mapId >= 156 && player.zone.map.mapId <= 159) {
                tiemNang /= 2;
            }
            tiemNang *= Manager.RATE_EXP_SERVER;
            tiemNang = calSubTNSM(tiemNang);
            if (tiemNang <= 0) {
                tiemNang = 1;
            }
        } else {
            tiemNang = 10;
        }
        return tiemNang < 1 ? 1 : tiemNang;
    }

    public long calSubTNSM(long tiemNang) {
        tiemNang = (long) (tiemNang * 0.5f);
        if (player.nPoint.power >= 80_000_000_000L) {
            tiemNang *= 0.05f;
        } else if (player.nPoint.power >= 60_000_000_000L) {
            tiemNang *= 0.2f;
        } else if (player.nPoint.power >= 40_000_000_000L) {
            tiemNang *= 0.5f;
        }
        return tiemNang;
    }

    public short getTileHutHp(boolean isMob) {
        if (isMob) {
            return (short) (this.tlHutHp + this.tlHutHpMob);
        } else {
            return this.tlHutHp;
        }
    }

    public short getTiLeHutMp() {
        return this.tlHutMp;
    }

    public long subDameInjureWithDeff(long dame) {
        long def = this.def;
        if (this.player != null && this.player.isBoss) {
            def /= 2;
        }
        dame -= def;
        if (dame < 0) {
            dame = 1;
        }
        return  dame;
    }

    /*------------------------------------------------------------------------*/
    public boolean canOpenPower() {
        return this.power >= getPowerLimit();
    }

    public long getPowerLimit() {
        switch (limitPower) {
            case 0:
                return 17999999999L;
            case 1:
                return 18999999999L;
            case 2:
                return 20999999999L;
            case 3:
                return 24999999999L;
            case 4:
                return 30999999999L;
            case 5:
                return 40999999999L;
            case 6:
                return 60999999999L;
            case 7:
                return 80999999999L;
            case 8:
                return 120999999999L;
            case 9:
                return 160999999999L;
            case 10:
                return 500000000000L;
            default:
                return 0;
        }
    }

    public long getPowerNextLimit() {
        switch (limitPower + 1) {
            case 0:
                return 17999999999L;
            case 1:
                return 18999999999L;
            case 2:
                return 20999999999L;
            case 3:
                return 24999999999L;
            case 4:
                return 30999999999L;
            case 5:
                return 40999999999L;
            case 6:
                return 60999999999L;
            case 7:
                return 80999999999L;
            case 8:
                return 120999999999L;
            case 9:
                return 160999999999L;
//            case 10:
//                return 140999999999L;
            case 10:
                return 500000000000L;
//            case 12:
//                return 300999999999L;
//            case 13:
//                return 400999999999L;
//            case 14:
//                return 500999999999L;
//            case 15:
//                return 600999999999L;
//            case 16:
//                return 700999999999L;
//            case 17:
//                return 800999999999L;
//            case 18:
//                return 900999999999L;
//            case 19:
//                return 9009999999999L;
//            case 20:
//                return 90099999999999L;
            default:
                return 0;
        }
    }

    public int getHpMpLimit() {
        if (limitPower == 0) {
            return 220000;
        }
        if (limitPower == 1) {
            return 240000;
        }
        if (limitPower == 2) {
            return 300000;
        }
        if (limitPower == 3) {
            return 350000;
        }
        if (limitPower == 4) {
            return 400000;
        }
        if (limitPower == 5) {
            return 450000;
        }
        if (limitPower == 6) {
            return 500000;
        }
        if (limitPower == 7) {
            return 525000;
        }
        if (limitPower == 8) {
            return 600000;
        }
        if (limitPower == 9) {
            return 625000;
        }
        if (limitPower == 10) {
            return 650000;
        }
//        if (limitPower > 9) {
//            return 700000;
//        }
        return 0;
    }

    public int getDameLimit() {
        if (limitPower == 0) {
            return 11000;
        }
        if (limitPower == 1) {
            return 12000;
        }
        if (limitPower == 2) {
            return 15000;
        }
        if (limitPower == 3) {
            return 18000;
        }
        if (limitPower == 4) {
            return 20000;
        }
        if (limitPower == 5) {
            return 22000;
        }
        if (limitPower == 6) {
            return 24000;
        }
        if (limitPower == 7) {
            return 24000;
        }
        if (limitPower == 8) {
            return 28000;
        }
        if (limitPower == 9) {
            return 30000;
        }
        if (limitPower == 10) {
            return 32000;
        }
//        if (limitPower == 10) {
//            return 35000;
//        }
//        if (limitPower == 11) {
//            return 37000;
//        }
//        if (limitPower == 12) {
//            return 39000;
//        }
//        if (limitPower == 13) {
//            return 42000;
//        }
//        if (limitPower >= 14) {
//            return 45000;
//        }
        return 0;
    }

    public short getDefLimit() {
        if (limitPower == 0) {
            return 550;
        }
        if (limitPower == 1) {
            return 600;
        }
        if (limitPower == 2) {
            return 700;
        }
        if (limitPower == 3) {
            return 800;
        }
        if (limitPower == 4) {
            return 1000;
        }
        if (limitPower == 5) {
            return 1200;
        }
        if (limitPower == 6) {
            return 1400;
        }
        if (limitPower == 7) {
            return 1600;
        }
        if (limitPower == 8) {
            return 1800;
        }
        if (limitPower == 9) {
            return 1900;
        }
        if (limitPower == 10) {
            return 2000;
        }
//        if (limitPower > 9) {
//            return 1800;
//        }
        return 0;
    }

    public byte getCritLimit() {
        if (limitPower == 0) {
            return 5;
        }
        if (limitPower == 1) {
            return 6;
        }
        if (limitPower == 2) {
            return 7;
        }
        if (limitPower == 3) {
            return 8;
        }
        if (limitPower == 4) {
            return 9;
        }
        if (limitPower == 5) {
            return 10;
        }
        if (limitPower == 6) {
            return 11;
        }
        if (limitPower == 7) {
            return 12;
        }
        if (limitPower == 8) {
            return 14;
        }
        if (limitPower == 9) {
            return 14;
        }
        if (limitPower == 10) {
            return 15;
        }
        return 0;
    }

    public void powerUp(long power) {
        this.power += power;
        if (this.power >= 90_000_000_000L) { // KOL 29 (đạt 90 tỷ sức mạnh)
            models.task.Task_Kol.complete(player, 29);
        }
        TaskService.gI().checkDoneTaskPower(player, this.power);
    }

    public void tiemNangUp(long tiemNang) {
        this.tiemNang += tiemNang;
        if (tiemNang > 0) { // KOL 9 (kiếm 100 tỷ tiềm năng — cộng dồn)
            models.task.Task_Kol.tick(this.player, tiemNang, 9);
        }
    }

    public void increasePoint(byte type, short point, boolean manualForPet) {
        if (point <= 0 || point > 100) {
            return;
        }
        long tiemNangUse;
        if (type == 0) {
            int pointHp = point * 20;
            tiemNangUse = point * (2 * (this.hpg + 1000) + pointHp - 20) / 2;
            if ((this.hpg + pointHp) <= getHpMpLimit()) {
                if (doUseTiemNang(tiemNangUse)) {
                    hpg += pointHp;
                }
            } else {
                Service.gI().sendThongBaoOK(player, "Vui lòng mở giới hạn sức mạnh");
                return;
            }
        }
        if (type == 1) {
            int pointMp = point * 20;
            tiemNangUse = point * (2 * (this.mpg + 1000) + pointMp - 20) / 2;
            if ((this.mpg + pointMp) <= getHpMpLimit()) {
                if (doUseTiemNang(tiemNangUse)) {
                    mpg += pointMp;
                }
            } else {
                Service.gI().sendThongBaoOK(player, "Vui lòng mở giới hạn sức mạnh");
                return;
            }
        }
        if (type == 2) {
            //  TaskService.gI().checkDoneTaskNangCS(player);
            tiemNangUse = point * (2 * this.dameg + point - 1) / 2 * 100;
            if ((this.dameg + point) <= getDameLimit()) {
                if (doUseTiemNang(tiemNangUse)) {
                    dameg += point;
                }
                // TaskService.gI().checkDoneTaskNangCS(player);
            } else {
                Service.gI().sendThongBaoOK(player, "Vui lòng mở giới hạn sức mạnh");
                return;
            }
        }
        if (type == 3) {
            tiemNangUse = 2 * (this.defg + 5) / 2 * 100000;
            if ((this.defg + point) <= getDefLimit()) {
                if (doUseTiemNang(tiemNangUse)) {
                    defg += point;
                }
            } else {
                Service.gI().sendThongBaoOK(player, "Vui lòng mở giới hạn sức mạnh");
                return;
            }
        }
        if (type == 4) {
            tiemNangUse = 50000000L;
            for (int i = 0; i < this.critg; i++) {
                tiemNangUse *= 3L;
            }
            if ((this.critg + point) <= getCritLimit()) {
                if (doUseTiemNang(tiemNangUse)) {
                    critg += point;
                    // [KOL] nâng chí mạng: KOL 11 (lên 3), KOL 14 (lên 5)
                    if (this.critg >= 3) {
                        models.task.Task_Kol.complete(this.player, 11);
                    }
                    if (this.critg >= 5) {
                        models.task.Task_Kol.complete(this.player, 14);
                    }
                }
            } else {
                Service.gI().sendThongBaoOK(player, "Vui lòng mở giới hạn sức mạnh");
                return;
            }
        }
        Service.gI().point(player);
        if (manualForPet) {
            Service.gI().sendChiSoPetGoc(((Pet) player).master);
            Service.gI().showInfoPet(((Pet) player).master);
            Service.gI().point(((Pet) player).master);
        }
    }

    private boolean doUseTiemNang(long tiemNang) {
        if (this.tiemNang < tiemNang) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ tiềm năng");
            return false;
        }
        if (this.tiemNang >= tiemNang && this.tiemNang - tiemNang >= 0) {
            this.tiemNang -= tiemNang;
            TaskService.gI().checkDoneTaskUseTiemNang(player);
            return true;
        }
        return false;
    }

    public long getFullTN() {
        long tnhp = 0, tnki = 0, tnsd = 0, tng = 0, tncm = 0;

        if (hpg > 0) {
            tnhp = (((hpg / 20L) * (50L + (50L + (hpg / 20L) - 1L)) / 2L) * 20L);
        }
        if (mpg > 0) {
            tnki = (((mpg / 20L) * (50L + (50L + (mpg / 20L) - 1L)) / 2L) * 20L);
        }
        if (dameg > 0) {
            tnsd = ((dameg * (dameg - 1L) * 100L) / 2L);
        }
        if (defg > 0) {
            tng = ((defg * (500000L + (500000L + (defg - 1L) * 100000L))) / 2L);
        }
        if (critg > 0) {
            tncm = ((50L * (((long) Math.pow(5L, critg) - 1L)) / (5L - 1L) * 1000000L));
        }
        return tnhp + tnki + tnsd + tng + tncm;
    }

    //--------------------------------------------------------------------------
    private long lastTimeHoiPhuc;
    private long lastTimeHoiStamina;

    public void update() {
        if (player != null && player.effectSkill != null) {
            if (player.effectSkill.isCharging && player.effectSkill.countCharging < 10) {
                if (player.playerSkill == null || player.playerSkill.skillSelect == null) {
                    EffectSkillService.gI().stopCharge(player);
                    return;
                }
                int tiLeHoiPhuc = SkillUtil.getPercentCharge(player.playerSkill.skillSelect.point);
                if (player.effectSkill.isCharging && !player.isDie() && !player.effectSkill.isHaveEffectSkill()
                        && (hp < hpMax || mp < mpMax)) {
                    long hpRecovered = hpMax / 100 * tiLeHoiPhuc;
                    long mpRecovered = mpMax / 100 * tiLeHoiPhuc;

                    

                    PlayerService.gI().hoiPhuc(player, hpRecovered, mpRecovered);

                    if (player.effectSkill.countCharging % 3 == 0) {
                        Service.gI().chat(player, "Phục hồi năng lượng " + getCurrPercentHP() + "%");
                    }
                } else {
                    EffectSkillService.gI().stopCharge(player);
                }
                if (++player.effectSkill.countCharging >= 10) {
                    EffectSkillService.gI().stopCharge(player);
                }
            }

            if (Util.canDoWithTime(lastTimeHoiPhuc, 30000)) {
                PlayerService.gI().hoiPhuc(this.player, hpHoi, mpHoi);
                this.lastTimeHoiPhuc = System.currentTimeMillis();
            }

            if (Util.canDoWithTime(lastTimeHoiStamina, 60000) && this.stamina < this.maxStamina) {
                this.stamina++;
                this.lastTimeHoiStamina = System.currentTimeMillis();

                if (!this.player.isBoss && !this.player.isPet) {
                    PlayerService.gI().sendCurrentStamina(this.player);
                }
            }
        }
    }

    public void dispose() {
        this.intrinsic = null;
        this.player = null;
        this.tlHp = null;
        this.tlMp = null;
        this.tlDef = null;
        this.tlDame = null;
        this.tlDameAttMob = null;
        this.tlTNSM = null;
    }
}
