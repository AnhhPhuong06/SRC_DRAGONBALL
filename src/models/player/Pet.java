package models.player;

 
import consts.ConstPlayer;
import lombok.Getter;
import services.map.MapService;
import models.mob.Mob;
import models.skill.Skill;
import utils.SkillUtil;
import services.Service;
import utils.Util;
import network.io.Message;
import services.ItemTimeService;
import services.player.PlayerService;
import services.SkillService;
import services.EffectSkillService;
import services.map.ChangeMapService;
import utils.TimeUtil;

import java.util.List;

import services.func.UseItem;

public class Pet extends Player {

    private static final short ARANGE_CAN_ATTACK = 300;
    private static final short ARANGE_ATT_SKILL1 = 50;

    private static final short[][] PET_ID = {{285, 286, 287}, {288, 289, 290}, {282, 283, 284}, {304, 305, 303}};

    public static final byte FOLLOW = 0;
    public static final byte PROTECT = 1;
    public static final byte ATTACK = 2;
    public static final byte GOHOME = 3;
    public static final byte FUSION = 4;
    public static final byte HTVV = 5;

    public Player master;
    @Getter
    public byte status = 0;

    public byte typePet;
    public boolean isTransform;

    public long lastTimeDie;

    private boolean goingHome;

    private Mob mobAttack;
    private Player playerAttack;

    private static final int TIME_WAIT_AFTER_UNFUSION = 5000;
    private long lastTimeUnfusion;

    private int indexChat = 0;
    private long lastTimeChat;

    public Pet(Player master) {
        this.master = master;
        this.isPet = true;
    }

    public void changeStatus(byte status) {
        if (goingHome || master.fusion.typeFusion != 0 || (this.isDie() && status == FUSION)) {
            Service.gI().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        Service.gI().chatJustForMe(master, this, getTextStatus(status));
        if (status == GOHOME) {
            goHome();
        } else if (status == FUSION) {
            fusion(false);
        }
        this.status = status;
    }

    public void joinMapMaster() {
        if (status != GOHOME && status != FUSION && !isDie()) {
            if (this.master == null || this.master.zone == null) {
                return;
            }
            this.location.x = master.location.x + Util.nextInt(-10, 10);
            this.location.y = master.location.y;
            if (MapService.gI().isMapOffline(this.master.zone.map.mapId) || this.master.zone.map.mapId == 113) {
                ChangeMapService.gI().goToMap(this, MapService.gI().getMapCanJoin(this, master.gender + 21, -1));
                return;
            }
            ChangeMapService.gI().goToMap(this, master.zone);
            this.zone.load_Me_To_Another(this);
        }
    }

    public void goHome() {
        if (this.status == GOHOME) {
            return;
        }
        goingHome = true;
        new Thread(() -> {
            try {
                Pet.this.status = Pet.ATTACK;
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            if (master != null) {
                try {
                    ChangeMapService.gI().goToMap(this, MapService.gI().getMapCanJoin(this, master.gender + 21, -1));
                } catch (Exception e) {
                }
                this.zone.load_Me_To_Another(this);
                Pet.this.status = Pet.GOHOME;
                goingHome = false;
            }
        }).start();
    }

    private String getTextStatus(byte status) {
        if (this.typePet == 6) {
            switch (status) {
                case FOLLOW:
                    return "ROOOAAARRRR! Ta sẽ theo sau!";
                case PROTECT:
                    return "ROOOAAARRRR! Kẻ nào dám đụng vào sư phụ!";
                case ATTACK:
                    return "ROOOAAARRRR! HỦY DIỆT TẤT CẢ!";
                case GOHOME:
                    return "ROOOAAARRRR! Ta về đây!";
                case HTVV:
                    return "ROOOAAARRRR! Sức mạnh tối thượng!";
                default:
                    return "ROOOAAARRRR!";
            }
        }
        if (this.typePet == 5) {
            switch (status) {
                case FOLLOW:
                    return "Sức mạnh là công lý duy nhất!";
                case PROTECT:
                    return "Không ai có thể vượt qua ta!";
                case ATTACK:
                    return "Hãy cảm nhận sức mạnh của vũ trụ 11!";
                case GOHOME:
                    return "Ta sẽ trở về để rèn luyện thêm...";
                case HTVV:
                    return "Sức mạnh tuyệt đối sẽ chiến thắng!";
                default:
                    return "Niềm tin của ta là bất diệt!";
            }
        }
        if (this.typePet == 4) {
            switch (status) {
                case FOLLOW:
                    return "Lũ con người không đủ tư cách để nói chuyện với ta";
                case PROTECT:
                    return "Ta sẽ cho người biết sức mạnh của một vị thần là như thế nào !";
                case ATTACK:
                    return "Ta sẽ thống trị vũ trụ";
                case GOHOME:
                    return "Không lí nào ta lại run sợ bọn con người sao";
                case HTVV:
                    return "Lũ các ngươi làm ta thấy đau rồi ấy haha";
                default:
                    return "Sức mạnh của ta là không có giới hạn";
            }
        }
        switch (status) {
            case FOLLOW:
                return "Ok con theo sư phụ";
            case PROTECT:
                return "Ok con sẽ bảo vệ sư phụ";
            case ATTACK:
                return "Ok sư phụ để con lo cho";
            case GOHOME:
                return "OK con về, bibi sư phụ";
            case HTVV:
                return "Đm sư phụ thích hợp thể bố mày vĩnh viễn k ?";
            default:
                return "Sư phụ ơi con lên cấp rồi";
        }
    }

    public void ggtblue(boolean porata) {
        if (this.isDie()) {
            Service.gI().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (this.effectSkill != null && this.effectSkill.isMonkey) {
                EffectSkillService.gI().monkeyDown(this);
            }
            if (porata) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_GOGETA_BLUE;
            } else {
                master.fusion.lastTimeFusion = System.currentTimeMillis();
                master.fusion.typeFusion = ConstPlayer.LUONG_LONG_NHAT_THE;
                ItemTimeService.gI().sendItemTime(master, master.gender == ConstPlayer.NAMEC ? 3901 : 3790, Fusion.TIME_FUSION / 1000);
            }
            this.status = FUSION;
            ChangeMapService.gI().exitMap(this);
            fusionEffect(4);
            Service.gI().Send_Caitrang(master);
            this.nPoint.calPoint();
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.gI().point(master);
        } else {
            Service.gI().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void fusion3(boolean porata) {
        if (this.isDie()) {
            Service.gI().sendThongBao(master, "Yêu cầu phải có đệ tử và đệ tử còn sống");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (this.effectSkill != null && this.effectSkill.isMonkey) {
                EffectSkillService.gI().monkeyDown(this);
            }
            if (porata) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_PORATA3;
            }
            this.status = FUSION;
            ChangeMapService.gI().exitMap(this);
            fusionEffect(master.fusion.typeFusion);
            this.nPoint.calPoint();
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.gI().point(master);
            Service.gI().Send_Caitrang(master);
        } else {
            Service.gI().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void fusion2(boolean porata) {
        if (this.isDie()) {
            Service.gI().sendThongBao(master, "Yêu cầu phải có đệ tử và đệ tử còn sống");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (this.effectSkill != null && this.effectSkill.isMonkey) {
                EffectSkillService.gI().monkeyDown(this);
            }
            if (porata) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_PORATA2;
            }
            this.status = FUSION;
            ChangeMapService.gI().exitMap(this);
            fusionEffect(master.fusion.typeFusion);
            this.nPoint.calPoint();
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.gI().point(master);
            Service.gI().Send_Caitrang(master);
        } else {
            Service.gI().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void fusion(boolean porata) {
        if (this.isDie()) {
            Service.gI().sendThongBao(master, "Yêu cầu phải có đệ tử và đệ tử còn sống");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (this.effectSkill != null && this.effectSkill.isMonkey) {
                EffectSkillService.gI().monkeyDown(this);
            }
            if (porata) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_PORATA;
            } else {
                master.fusion.lastTimeFusion = System.currentTimeMillis();
                master.fusion.typeFusion = ConstPlayer.LUONG_LONG_NHAT_THE;
                ItemTimeService.gI().sendItemTime(master, master.gender == ConstPlayer.NAMEC ? 3901 : 3790, Fusion.TIME_FUSION / 1000);
            }
            this.status = FUSION;
            ChangeMapService.gI().exitMap(this);
            fusionEffect(master.fusion.typeFusion);
            Service.gI().Send_Caitrang(master);
            this.nPoint.calPoint();
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.gI().point(master);
        } else {
            Service.gI().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void unFusion() {
        master.fusion.typeFusion = 0;
        this.status = PROTECT;
        Service.gI().point(master);
        joinMapMaster();
        fusionEffect(master.fusion.typeFusion);
        Service.gI().Send_Caitrang(master);
        Service.gI().point(master);
        this.lastTimeUnfusion = System.currentTimeMillis();
    }

    private void fusionEffect(int type) {
        Message msg;
        try {
            msg = new Message(125);
            msg.writer().writeByte(type);
            msg.writer().writeInt((int) master.id);
            Service.gI().sendMessAllPlayerInMap(master, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    public long lastTimeMoveIdle;
    private int timeMoveIdle;
    public boolean idle;

    private void moveIdle() {
        if (status == GOHOME || status == FUSION || status == HTVV) {
            return;
        }
        if (idle && Util.canDoWithTime(lastTimeMoveIdle, timeMoveIdle)) {
            int dir = this.location.x - master.location.x <= 0 ? -1 : 1;
            PlayerService.gI().playerMove(this, master.location.x
                    + (dir == -1 ? 50 : -50), master.location.y);
            lastTimeMoveIdle = System.currentTimeMillis();
            timeMoveIdle = Util.nextInt(5000, 8000);
            idle = false;
        }
        //Util.nextInt(dir == -1 ? 50 : -50, dir == -1 ? 50 : 50)
    }

    private void masterDoesNotAttack() {
        if (Util.canDoWithTime(master.lastTimePlayerNotAttack, master.timeNotAttack)) {
            if (!MapService.gI().isMapOffline(master.zone.map.mapId)) {
                master.doesNotAttack = true;
            }
            master.lastTimePlayerNotAttack = System.currentTimeMillis();
            master.timeNotAttack = Util.nextInt(1800000, 3600000); // random 30p - 1h
        }
    }

    private long lastTimeMoveAtHome;
    private byte directAtHome = -1;

    /**
     * TODO: Code nhìn như cc rảnh sẽ fix lại
     */
    @Override
    public void update() {
        try {
            if (this.master != null && this.master.zone != null) {
                super.update();
                increasePoint(); //cộng chỉ số
                updatePower(); //check mở skill...
                if (isDie()) {
                    if (System.currentTimeMillis() - lastTimeDie > 50000) {
                        Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
                    } else {
                        return;
                    }
                }

                if (this.newSkill != null && this.newSkill.isStartSkillSpecial) {
                    return;
                }
                if (this.master != null && this.master.zone != null) {
                    if (justRevived && this.zone == master.zone) {
                        Service.gI().chatJustForMe(master, this, "Sư phụ ơi con đây nè");
                        justRevived = false;
                    }
                }
                if (this.zone == null || this.zone != master.zone) {
                    joinMapMaster();
                }
                if (master.isDie() || this.isDie() || effectSkill.isHaveEffectSkill()) {
                    return;
                }
                masterDoesNotAttack();
                moveIdle();
                switch (status) {
                    case FOLLOW:
                        followMaster(60);
                        break;
                    case PROTECT:
                        if (useSkill3() || useSkill4() || useSkill5() || useSkill6() || useSkill7()) {
                            break;
                        }
                        mobAttack = findMobAttack();
                        if (mobAttack != null) {
                            int disToMob = Util.getDistance(this, mobAttack);
                            if (disToMob <= ARANGE_ATT_SKILL1) {
                                //đấm
                                this.playerSkill.skillSelect = getSkill(1);
                                if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-60, 60), mobAttack.location.y);
                                        SkillService.gI().useSkill(this, null, mobAttack, -1, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            } else {
                                //chưởng
                                this.playerSkill.skillSelect = getSkill(2);
                                if (this.playerSkill.skillSelect.skillId != -1) {
                                    if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                        if (SkillService.gI().canUseSkillWithMana(this)) {
                                            SkillService.gI().useSkill(this, null, mobAttack, -1, null);
                                        } else {
                                            askPea();
                                        }
                                    }
                                } else {
                                    this.playerSkill.skillSelect = getSkill(1);
                                    if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                        if (SkillService.gI().canUseSkillWithMana(this)) {
                                            PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-60, 60), mobAttack.location.y);
                                            SkillService.gI().useSkill(this, null, mobAttack, -1, null);
                                        } else {
                                            askPea();
                                        }
                                    }
                                }
                            }

                        } else {
                            idle = true;
                            followMaster(60);
                        }

                        break;
                    case ATTACK:
                        if (useSkill3() || useSkill4() || useSkill5() || useSkill6() || useSkill7()) {
                            break;
                        }
                        playerAttack = findPlayerAttack();
                        if (playerAttack != null) {
                            if ((this.typePet == 2 || this.typePet == 4) && Util.isTrue(1, 5) && playerAttack.nPoint.hp < 1_000_000_000 && !playerAttack.nPoint.islinhthuydanhbac && !playerAttack.isBoss) {
                                playerAttack.setDie(this);
                                Service.gI().chat(this, "HAKAI " + playerAttack.name + "!");
                                Service.gI().sendThongBao(playerAttack, "Bạn đã bị Hakai!");
                            } else if (this.typePet == 6 && Util.isTrue(1, 5) && playerAttack.nPoint.hp < 1_000_000_000 && !playerAttack.nPoint.islinhthuydanhbac && !playerAttack.isBoss) {
                                playerAttack.setDie(this);
                                Service.gI().chat(this, "ROOOAAARRRR! HỦY DIỆT " + playerAttack.name + "!");
                                Service.gI().sendThongBao(playerAttack, "Bạn đã bị Cell Max nghiền nát!");
                            } else if (this.typePet == 5 && Util.isTrue(1, 5) && playerAttack.nPoint.hp < 1_000_000_000 && !playerAttack.nPoint.islinhthuydanhbac && !playerAttack.isBoss) {
                                playerAttack.setDie(this);
                                Service.gI().chat(this, "Đòn quyết định: Công lý tuyệt đối!");
                                Service.gI().sendThongBao(playerAttack, "Bạn đã bị Jiren hạ gục!");
                            } else {
                                petSay(playerAttack);
                            }
                            int disToPlayer = Util.getDistance(this, playerAttack);
                            if (disToPlayer <= ARANGE_ATT_SKILL1) {
                                //đấm
                                this.playerSkill.skillSelect = getSkill(1);
                                if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        PlayerService.gI().playerMove(this, playerAttack.location.x + Util.nextInt(-60, 60), playerAttack.location.y);
                                        SkillService.gI().useSkill(this, playerAttack, null, -1, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            } else {
                                //chưởng
                                this.playerSkill.skillSelect = getSkill(2);
                                if (this.playerSkill.skillSelect.skillId != -1) {
                                    if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                        if (SkillService.gI().canUseSkillWithMana(this)) {
                                            SkillService.gI().useSkill(this, playerAttack, null, -1, null);
                                        } else {
                                            askPea();
                                        }
                                    }
                                } else {
                                    this.playerSkill.skillSelect = getSkill(1);
                                    if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                        if (SkillService.gI().canUseSkillWithMana(this)) {
                                            PlayerService.gI().playerMove(this, playerAttack.location.x + Util.nextInt(-60, 60), playerAttack.location.y);
                                            SkillService.gI().useSkill(this, playerAttack, null, -1, null);
                                        } else {
                                            askPea();
                                        }
                                    }
                                }
                            }
                            return;
                        }
                        mobAttack = findMobAttack();
                        if (mobAttack != null) {
                            int disToMob = Util.getDistance(this, mobAttack);
                            if (disToMob <= ARANGE_ATT_SKILL1) {
                                this.playerSkill.skillSelect = getSkill(1);
                                if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                        SkillService.gI().useSkill(this, playerAttack, mobAttack, -1, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            } else {
                                this.playerSkill.skillSelect = getSkill(2);
                                if (this.playerSkill.skillSelect.skillId != -1) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                        SkillService.gI().useSkill(this, playerAttack, mobAttack, -1, null);
                                    }
                                } else {
                                    this.playerSkill.skillSelect = getSkill(1);
                                    if (SkillService.gI().canUseSkillWithCooldown(this) && canAttack()) {
                                        if (SkillService.gI().canUseSkillWithMana(this)) {
                                            PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                            SkillService.gI().useSkill(this, playerAttack, mobAttack, -1, null);
                                        } else {
                                            askPea();
                                        }
                                    }
                                }
                            }

                        } else {
                            idle = true;
                            followMaster(60);
                        }
                        break;

                    case GOHOME:
                        if (this.zone != null && (this.zone.map.mapId == 21 || this.zone.map.mapId == 22 || this.zone.map.mapId == 23)) {
                            if (System.currentTimeMillis() - lastTimeMoveAtHome <= 5000) {
                                return;
                            } else {
                                if (this.zone.map.mapId == 21) {
                                    if (directAtHome == -1) {

                                        PlayerService.gI().playerMove(this, 250, 336);
                                        directAtHome = 1;
                                    } else {
                                        PlayerService.gI().playerMove(this, 200, 336);
                                        directAtHome = -1;
                                    }
                                } else if (this.zone.map.mapId == 22) {
                                    if (directAtHome == -1) {
                                        PlayerService.gI().playerMove(this, 500, 336);
                                        directAtHome = 1;
                                    } else {
                                        PlayerService.gI().playerMove(this, 452, 336);
                                        directAtHome = -1;
                                    }
                                } else if (this.zone.map.mapId == 22) {
                                    if (directAtHome == -1) {
                                        PlayerService.gI().playerMove(this, 250, 336);
                                        directAtHome = 1;
                                    } else {
                                        PlayerService.gI().playerMove(this, 200, 336);
                                        directAtHome = -1;
                                    }
                                }
                                Service.gI().chatJustForMe(master, this, "Là do bạn không chơi đồ đấy bạn ạ!");
                                lastTimeMoveAtHome = System.currentTimeMillis();
                            }
                        }
                        break;
                    case HTVV:
                        if (master.gender == 1) {
                            Service.gI().sendThongBao(master, "Không thể thực hiện 1");
//                            fusionEffect(ConstPlayer.LUONG_LONG_NHAT_THE);
//                            ChangeMapService.gI().exitMap(this);
//                            Service.gI().addSMTN(master, (byte) 1, this.nPoint.power, true);
//                            master.pet = null;
//                            Service.gI().sendHavePet(master);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long lastTimeAskPea;

    public void askPea() {
        if (Util.canDoWithTime(lastTimeAskPea, 10000)) {
            if (this.master.isPet) {
                if (!this.isDie()) {
                    int statima = 100 * 10;
                    int hpKiHoiPhuc = 100000;
                    this.nPoint.stamina += statima;
                    if (this.nPoint.stamina > this.nPoint.maxStamina) {
                        this.nPoint.stamina = this.nPoint.maxStamina;
                    }
                    this.nPoint.setHp(this.nPoint.hp + hpKiHoiPhuc);
                    this.nPoint.setMp(this.nPoint.mp + hpKiHoiPhuc);
                    Service.gI().sendInfoPlayerEatPea(this);
                }
                lastTimeAskPea = System.currentTimeMillis();
                return;
            }
            Service.gI().chatJustForMe(master, this, this.typePet == 6 ? "ROOOAAARRRR! Đưa đậu cho ta!" : this.typePet == 5 ? "Đưa ta đậu để tiếp tục chiến đấu!" : this.typePet == 4 ? "Đưa ta đậu, nếu không ta sẽ hủy diệt thế giới này!" : "Sư phụ ơi cho con đậu thần");
            UseItem.gI().eatPea(master);
            lastTimeAskPea = System.currentTimeMillis();
        }
    }

    private int countTTNL;

    private boolean useSkill3() {
        try {
            playerSkill.skillSelect = getSkill(3);
            if (playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            switch (this.playerSkill.skillSelect.template.id) {
                case Skill.THAI_DUONG_HA_SAN:
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, -1, null);
                        Service.gI().chatJustForMe(master, this, "Bất ngờ chưa ông già");
                        return true;
                    }
                    return false;
                case Skill.TAI_TAO_NANG_LUONG:
                    if (this.effectSkill.isCharging && this.countTTNL < Util.nextInt(3, 5)) {
                        this.countTTNL++;
                        return true;
                    }
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)
                            && (this.nPoint.getCurrPercentHP() <= 20 || this.nPoint.getCurrPercentMP() <= 20)) {
                        SkillService.gI().useSkill(this, null, null, -1, null);
                        this.countTTNL = 0;
                        return true;
                    }
                    return false;
                case Skill.KAIOKEN:
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {

                        mobAttack = this.findMobAttack();
                        playerAttack = this.findPlayerAttack();
                        if (playerAttack != null) {
                            mobAttack = null;
                            int dis = Util.getDistance(this, playerAttack);
                            if (dis > ARANGE_ATT_SKILL1) {
                                PlayerService.gI().playerMove(this, playerAttack.location.x, playerAttack.location.y);
                            } else {
                                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                                    PlayerService.gI().playerMove(this, playerAttack.location.x + Util.nextInt(-20, 20), playerAttack.location.y);
                                }
                            }
                        } else if (mobAttack == null) {
                            return false;
                        }
                        if (mobAttack != null) {
                            int dis = Util.getDistance(this, mobAttack);
                            if (dis > ARANGE_ATT_SKILL1) {
                                PlayerService.gI().playerMove(this, mobAttack.location.x, mobAttack.location.y);
                            } else {
                                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                                    PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                }
                            }
                        }

                        SkillService.gI().useSkill(this, playerAttack, mobAttack, -1, null);
                        getSkill(1).lastTimeUseThisSkill = System.currentTimeMillis();
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean useSkill4() {
        try {
            this.playerSkill.skillSelect = getSkill(4);
            if (this.playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            switch (this.playerSkill.skillSelect.template.id) {
                case Skill.BIEN_KHI:
                    if (!this.effectSkill.isMonkey && SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, -1, null);
                        return true;
                    }
                    return false;
                case Skill.KHIEN_NANG_LUONG:
                    if (!this.effectSkill.isShielding && SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, -1, null);
                        return true;
                    }
                    return false;
                case Skill.DE_TRUNG:
                    if (this.mobMe == null && SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, -1, null);
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean useSkill5() {
        try {
            this.playerSkill.skillSelect = getSkill(5);
            if (this.playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                Player plAtt = findPlayerAttack();
                Mob mobAtt = findMobAttack();
                if (plAtt != null) {
                    mobAtt = null;
                } else if (mobAtt == null) {
                    return false;
                }
                SkillService.gI().useSkill(this, plAtt, mobAtt, -1, null);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean useSkill6() {
        try {
            this.playerSkill.skillSelect = getSkill(6);
            if (this.playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                Player plAtt = findPlayerAttack();
                Mob mobAtt = findMobAttack();
                if (plAtt != null) {
                    mobAtt = null;
                } else if (mobAtt == null) {
                    return false;
                }
                SkillService.gI().useSkill(this, plAtt, mobAtt, -1, null);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean useSkill7() {
        try {
            this.playerSkill.skillSelect = getSkill(7);
            if (this.playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                Player plAtt = findPlayerAttack();
                if (plAtt != null) {
                    SkillService.gI().useSkill(this, plAtt, null, -1, null);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    private long lastTimeIncreasePoint;

//    private void increasePoint() {
//        if (this.nPoint != null && Util.canDoWithTime(lastTimeIncreasePoint, 1)) {
//            if (Util.isTrue(20, 100)) {
//                this.nPoint.increasePoint((byte) 2, (short) 100);
//            } else {
//                byte type = (byte) Util.nextInt(0, 2);
//                short point = (short) Util.nextInt(50);
//                this.nPoint.increasePoint(type, point);
//            }
//            lastTimeIncreasePoint = System.currentTimeMillis();
//        }
//
//    }
    private void increasePoint() {
        this.nPoint.increasePoint((byte) Util.nextInt(0, 2), (short) 100, false);
        this.nPoint.increasePoint((byte) Util.nextInt(0, 2), (short) 10, false);
        this.nPoint.increasePoint((byte) Util.nextInt(0, 2), (short) 1, false);
    }

    public void followMaster() {
        if (this.isDie() || effectSkill.isHaveEffectSkill()) {
            return;
        }
        switch (this.status) {
            case ATTACK:
                if (mobAttack != null && Util.getDistance(this, master) <= 1000) {
                    break;
                }
            case FOLLOW:
            case PROTECT:
                followMaster(60);
                break;
        }
    }

    private long lastTimeBamChu;

    private void followMaster(int dis) {
        if (master == null || master.location == null || this.zone == null || master.zone == null) {
            return;
        }
        if (this.zone != master.zone) {
            joinMapMaster();
            return;
        }
        int mX = master.location.x;
        int mY = master.location.y;
        int dx = this.location.x - mX;
        int dy = this.location.y - mY;
        boolean quaXa = (long) dx * dx + (long) dy * dy >= (long) dis * dis;
        if (!quaXa) {
            return;
        }
        if (!Util.canDoWithTime(lastTimeBamChu, 150)) {
            return;
        }
        lastTimeBamChu = System.currentTimeMillis();
        int newX = (dx < 0) ? mX - 50 : mX + 50;
        int newY = mY;
        PlayerService.gI().playerMove(this, newX, newY);
    }

    public short getAvatar() {
        switch (this.typePet) {
            case 1:
                return 297;
            case 2:
                return 508;
            case 3:
                return 946;
            case 4:
                return 550;
            case 5:
                return 2270;
            case 6:
                return 231;
            default:
                return PET_ID[3][this.gender];
        }
    }

    @Override
    public short getHead() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 454;
        }
        if (effectSkill != null && effectSkill.isHalloween) {
            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][0];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 412;
        }
        if (inventory != null && inventory.itemsBody.size() > 5 && inventory.itemsBody.get(5).isNotNullItem()) {
            int part = inventory.itemsBody.get(5).template.head;
            if (part != -1) {
                return (short) part;
            }
        }
        if (this.typePet == 1) {
            return 297;
        } else if (this.typePet == 2) {
            return 508;
        } else if (this.typePet == 3) {
            return 946;
        } else if (this.typePet == 4) {
            if (!this.isTransform) {
                return 550;
            }
            return 553;
        } else if (this.typePet == 5) {
            return 2270;
        } else if (this.typePet == 6) {
            return 231;
        }
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][0];
        } else {
            return PET_ID[3][this.gender];
        }
    }

    @Override
    public short getBody() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 455;
        }
        if (effectSkill != null && effectSkill.isHalloween) {
            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][1];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 413;
        }
        if (inventory != null && inventory.itemsBody.size() > 5 && inventory.itemsBody.get(5).isNotNullItem()) {
            int body = inventory.itemsBody.get(5).template.body;
            if (body != -1) {
                return (short) body;
            }
        }
        if (this.typePet == 1 && !this.isTransform) {
            return 298;
        } else if (this.typePet == 2 && !this.isTransform) {
            return 509;
        } else if (this.typePet == 3 && !this.isTransform) {
            return 947;
        } else if (this.typePet == 4) {
            return 551;
        } else if (this.typePet == 5) {
            return 2271;
        } else if (this.typePet == 6) {
            return 232;
        }
        if (inventory != null && inventory.itemsBody.size() > 0 && inventory.itemsBody.get(0).isNotNullItem()) {
            return inventory.itemsBody.get(0).template.part;
        }
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][1];
        } else {
            return (short) (gender == ConstPlayer.NAMEC ? 59 : 57);
        }
    }

    @Override
    public short getLeg() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 456;
        }
        if (effectSkill != null && effectSkill.isHalloween) {
            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][2];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 414;
        }
        if (inventory != null && inventory.itemsBody.size() > 5 && inventory.itemsBody.get(5).isNotNullItem()) {
            int leg = inventory.itemsBody.get(5).template.leg;
            if (leg != -1) {
                return (short) leg;
            }
        }
        if (this.typePet == 1 && !this.isTransform) {
            return 299;
        } else if (this.typePet == 2 && !this.isTransform) {
            return 510;
        } else if (this.typePet == 3 && !this.isTransform) {
            return 948;
        } else if (this.typePet == 4) {
            return 552;
        } else if (this.typePet == 5) {
            return 2272;
        } else if (this.typePet == 6) {
            return 233;
        }
        if (inventory != null && inventory.itemsBody.size() > 1 && inventory.itemsBody.get(1).isNotNullItem()) {
            return inventory.itemsBody.get(1).template.part;
        }

        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][2];
        } else {
            return (short) (gender == ConstPlayer.NAMEC ? 60 : 58);
        }
    }

    private Player findPlayerAttack() {
        List<Player> playersMap = zone.getHumanoids();
        int dis = ARANGE_CAN_ATTACK;
        Player plAtt = null;

        for (int i = playersMap.size() - 1; i >= 0; i--) {
            Player pl = playersMap.get(i);
            if (!cantAttack(pl)) {
                int d = Util.getDistance(this, pl);
                if (d <= dis) {
                    dis = d;
                    plAtt = pl;
                }
            }
        }

        return plAtt;
    }

    private boolean cantAttack(Player player) {
        return player == null || player.location == null || player.isDie() || Util.getDistance(this, player) > 500
                || this.equals(player) || (player.equals(master) && this.typePet != 2 && this.typePet != 4 && this.typePet != 5 && this.typePet != 6)
                || (!temporaryEnemies.contains(player) && !master.temporaryEnemies.contains(player))
                || (!SkillService.gI().canAttackPlayer(this, player));
    }

    private Mob findMobAttack() {
        int dis = ARANGE_CAN_ATTACK;
        Mob mobAtt = null;
        for (Mob mob : zone.mobs) {
            if (mob.isDie()) {
                continue;
            }
            int d = Util.getDistance(this, mob);
            if (d <= dis) {
                dis = d;
                mobAtt = mob;
            }
        }
        return mobAtt;
    }

    //Sức mạnh mở skill đệ
    private void updatePower() {
        if (this.playerSkill != null) {
            switch (this.playerSkill.getSizeSkill()) {
                case 1:
                    if (this.nPoint.power >= 150000000) {
                        openSkill2();
                    }
                    break;
                case 2:
                    if (this.nPoint.power >= 1500000000) {
                        openSkill3();
                    }
                    break;
                case 3:
                    if (this.nPoint.power >= 20000000000L) {
                        openSkill4();
                    }
                    break;
            }
        }
    }

    public void openSkill2() {
        Skill skill = null;
        int tiLeKame = 40;
        int tiLeMasenko = 30;
        int tiLeAntomic = 30;

        int rd = Util.nextInt(1, 100);
        if (rd <= tiLeKame) {
            skill = SkillUtil.createSkill(Skill.KAMEJOKO, 1);
        } else if (rd <= tiLeKame + tiLeMasenko) {
            skill = SkillUtil.createSkill(Skill.MASENKO, 1);
        } else if (rd <= tiLeKame + tiLeMasenko + tiLeAntomic) {
            skill = SkillUtil.createSkill(Skill.ANTOMIC, 1);
        }
        skill.coolDown = 1000;
        this.playerSkill.skills.set(1, skill);
    }

    public void openSkill3() {
        Skill skill = null;
        int tiLeTDHS = 30;
        int tiLeTTNL = 30;
        int tiLeKOK = 40;

        int rd = Util.nextInt(1, 100);
        if (rd <= tiLeTDHS) {
            skill = SkillUtil.createSkill(Skill.THAI_DUONG_HA_SAN, 1);
        } else if (rd <= tiLeTDHS + tiLeTTNL) {
            skill = SkillUtil.createSkill(Skill.TAI_TAO_NANG_LUONG, 1);
        } else if (rd <= tiLeTDHS + tiLeTTNL + tiLeKOK) {
            skill = SkillUtil.createSkill(Skill.KAIOKEN, 1);
        }
        this.playerSkill.skills.set(2, skill);
    }

    public void openSkill4() {
        Skill skill = null;
        int tiLeBienKhi = 30;
        int tiLeDeTrung = 30;
        int tiLeKNL = 40;

        int rd = Util.nextInt(1, 100);
        if (rd <= tiLeBienKhi) {
            skill = SkillUtil.createSkill(Skill.BIEN_KHI, 1);
        } else if (rd <= tiLeBienKhi + tiLeDeTrung) {
            skill = SkillUtil.createSkill(Skill.DE_TRUNG, 1);
        } else if (rd <= tiLeBienKhi + tiLeDeTrung + tiLeKNL) {
            skill = SkillUtil.createSkill(Skill.KHIEN_NANG_LUONG, 1);
        }
        this.playerSkill.skills.set(3, skill);
    }

    public void openSkill5() {
        Skill skill = null;
        int tiLeSocola = 10;
        int tiLeGenki = 70;
        int tiLeDCTT = 20;

        int rd = Util.nextInt(1, 100);
        if (rd <= tiLeSocola) {
            skill = SkillUtil.createSkill(Skill.SOCOLA, 1);
        } else if (rd <= tiLeSocola + tiLeGenki) {
            switch (this.gender) {
                case 0:
                    skill = SkillUtil.createSkill(Skill.QUA_CAU_KENH_KHI, 1);
                    break;
                case 1:
                    skill = SkillUtil.createSkill(Skill.MAKANKOSAPPO, 1);
                    break;
                default:
                    skill = SkillUtil.createSkill(Skill.HUYT_SAO, 1);
                    break;
            }
        } else if (rd <= tiLeSocola + tiLeGenki + tiLeDCTT) {
            switch (this.gender) {
                case 0:
                    skill = SkillUtil.createSkill(Skill.DICH_CHUYEN_TUC_THOI, 1);
                    break;
                case 1:
                    skill = SkillUtil.createSkill(Skill.THOI_MIEN, 1);
                    break;
                default:
                    skill = SkillUtil.createSkill(Skill.TU_SAT, 1);
                    break;
            }
        }
        this.playerSkill.skills.set(4, skill);
    }

    private void openSkill6() {
        int idSkill[] = {Skill.SUPER_KAME, Skill.MA_PHONG_BA, Skill.LIEN_HOAN_CHUONG};
        Skill skill = SkillUtil.createSkill(idSkill[Util.nextInt(idSkill.length)], 1);
        this.playerSkill.skills.set(5, skill);
    }

    private Skill getSkill(int indexSkill) {
        return this.playerSkill.skills.get(indexSkill - 1);
    }

    public void transform() {
        if (this.typePet == 1) {
            this.isTransform = !this.isTransform;
            Service.gI().Send_Caitrang(this);
            Service.gI().chat(this, "Bố Mày Là Bư Nè !! Bư..Bư..Bư..Ma..Nhân..Bư....");
        }
        if (this.typePet == 2) {
            this.isTransform = !this.isTransform;
            Service.gI().Send_Caitrang(this);
            Service.gI().chat(this, "Tao là thần");
        }
        if (this.typePet == 2) {
            this.isTransform = !this.isTransform;
            Service.gI().Send_Caitrang(this);
            Service.gI().chat(this, "Chúng mày quỳ xuống");
        }
        if (this.typePet == 4) {
            this.isTransform = !this.isTransform;
            Service.gI().Send_Caitrang(this);
            Service.gI().chat(this, "Hỡi nhân loại thấp kém! Hãy chiêm ngưỡng vẻ đẹp của ta!");
        }
        if (this.typePet == 5) {
            this.isTransform = !this.isTransform;
            Service.gI().Send_Caitrang(this);
            Service.gI().chat(this, "Sức mạnh là tuyệt đối!");
        }
        if (this.typePet == 6) {
            this.isTransform = !this.isTransform;
            Service.gI().Send_Caitrang(this);
            Service.gI().chat(this, "ROOOAAARRRR! HỦY DIỆT!");
        }
    }

    public long lastTimeAskAttack;

    public boolean canAttack() {
        if (this.master.isPl() && this.master.doesNotAttack && this.master.charms.tdDeTu < System.currentTimeMillis()) {
            if (Util.canDoWithTime(lastTimeAskAttack, 10000)) {
                Service.gI().chatJustForMe(master, this, this.typePet == 6 ? "ROOOAAARRRR! Sao không đánh đi?" : this.typePet == 5 ? "Sao ngươi không ra tay?" : this.typePet == 4 ? "Sao ngươi không đánh đi?" : "Sao sư phụ không đánh đi?");
                lastTimeAskAttack = System.currentTimeMillis();
            }
            return false;
        }
        return true;
    }

    public void petSay(Player player) {
        if (this.typePet == 6) {
            if (Util.canDoWithTime(lastTimeChat, indexChat == 0 ? 15000 : 1500)) {
                String[] chat = {
                    "ROOOAAARRRR!",
                    "Ta sẽ nghiền nát tất cả!",
                    "Chết đi lũ sinh vật yếu ớt!",
                    "Sức mạnh hủy diệt tuyệt đối!",
                    "Không ai có thể cản được ta!"
                };
                Service.gI().chat(this, chat[indexChat]);
                indexChat = (indexChat + 1) % chat.length;
                lastTimeChat = System.currentTimeMillis();
            }
        } else if (this.typePet == 5) {
            if (Util.canDoWithTime(lastTimeChat, indexChat == 0 ? 15000 : 1500)) {
                String[] chat = {
                    "Ngươi quá yếu để đối đầu với ta",
                    "Sức mạnh là công lý duy nhất",
                    "Hãy cảm nhận sức mạnh của vũ trụ 11!",
                    "Không gì có thể làm lung lay ý chí của ta",
                    "Chấm dứt tại đây đi!"
                };
                Service.gI().chat(this, chat[indexChat]);
                indexChat = (indexChat + 1) % chat.length;
                lastTimeChat = System.currentTimeMillis();
            }
        } else if (this.typePet == 4) {
            if (Util.canDoWithTime(lastTimeChat, indexChat == 0 ? 15000 : 1500)) {
                String[] chat = {
                    "Ta chính là thế giới",
                    "Ta chính là công lí",
                    "Hãy chiêm ngưỡng vẻ đẹp của ta! Hỡi con người",
                    "Sức mạnh to lớn nằm trong cơ thể bất tử",
                    "Ta sẽ đem công lí tới toàn bộ vũ trụ này"
                };
                Service.gI().chat(this, chat[indexChat]);
                indexChat = (indexChat + 1) % chat.length;
                lastTimeChat = System.currentTimeMillis();
            }
        } else {
            if (Util.canDoWithTime(lastTimeChat, indexChat == 0 ? 15000 : 1500)) {
                String[] chat = {
                    "Mày chán sống rồi à " + player.name + "?",
                    "Mày muốn chết đúng không?",
                    "Ngày này năm sau",
                    "Tao sẽ nhớ uống thật nhiều nước",
                    "Để đái vào mộ mày"
                };
                Service.gI().chat(this, chat[indexChat]);
                indexChat = (indexChat + 1) % chat.length;
                lastTimeChat = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void dispose() {
        this.mobAttack = null;
        this.playerAttack = null;
        this.master = null;
        ChangeMapService.gI().exitMap(this);
        super.dispose();
    }
}
