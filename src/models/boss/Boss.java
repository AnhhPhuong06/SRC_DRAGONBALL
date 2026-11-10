package models.boss;

import consts.AppearType;
import consts.BossStatus;
import static consts.BossStatus.ACTIVE;
import static consts.BossStatus.AFK;
import static consts.BossStatus.CHAT_E;
import static consts.BossStatus.CHAT_S;
import static consts.BossStatus.DIE;
import static consts.BossStatus.JOIN_MAP;
import static consts.BossStatus.LEAVE_MAP;
import static consts.BossStatus.RESPAWN;
import static consts.BossStatus.REST;
import consts.BossType;
import static consts.BossType.BROLY;
import static consts.BossType.CHRISTMAS_EVENT;
import static consts.BossType.FINAL;
import static consts.BossType.HIT_EVENT;
import static consts.BossType.HUNGVUONG_EVENT;
import static consts.BossType.PHOBAN;
import static consts.BossType.PHOBANBDKB;
import static consts.BossType.PHOBANCDRD;
import static consts.BossType.PHOBANDT;
import static consts.BossType.PHOBANKGHD;
import static consts.BossType.SKILLSUMMONED;
import static consts.BossType.TET_EVENT;
import static consts.BossType.TRUNGTHU_EVENT;
import static consts.BossType.YARDART;
import consts.BossID;
import consts.ConstPlayer;
import managers.boss.*;
import network.io.Message;
import java.util.List;
import models.map.ItemMap;
import models.map.Zone;
import models.mob.Mob;
import models.player.Pet;
import models.player.Player;
import models.skill.Skill;
import server.ServerNotify;
import services.EffectSkillService;
import services.map.MapService;
import services.ItemService;
import services.player.PlayerService;
import services.Service;
import services.SkillService;
import services.TaskService;
import services.map.ChangeMapService;
import utils.Logger;
import utils.SkillUtil;
import utils.Util;
import interfaces.IBoss;
import java.io.IOException;
import static models.player.Player.idOutfitMafuba;

public class Boss extends Player implements IBoss {

    public int currentLevel = -1;
    public final BossData[] data;

    public BossStatus bossStatus;

    protected Zone lastZone;

    public long lastTimeRest;
    private long lastTimeJoinAttempt;
    public int secondsRest;

    public int getSecondsRest() {
        return this.secondsRest;
    }

    public void setSecondsRest(int secondsRest) {
        this.secondsRest = secondsRest;
    }

    protected long lastTimeChatS;
    protected int timeChatS;
    protected byte indexChatS;
    protected long lastTimeStartChatS;

    protected long lastTimeChatE;
    protected int timeChatE;
    protected byte indexChatE;
    protected long lastTimeStartChatE;

    protected long lastTimeChatM;
    protected int timeChatM;

    protected long lastTimeTargetPlayer;
    protected int timeTargetPlayer;
    protected Player playerTarger;

    protected Boss parentBoss;
    public Boss[][] bossAppearTogether;

    public Zone zoneFinal = null;

    public Player playerReward;

    public int lv;

    public int error;

    public boolean prepareBom;

    public boolean isNotifyDisabled;
    public boolean isZone01SpawnDisabled;

    public Boss(int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(id, data);
        this.isNotifyDisabled = false;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(BossType bossType, int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(bossType, id, data);
        this.isNotifyDisabled = false;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(int id, BossData... data) throws Exception {
        this.id = id;
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        services.BossSettings.get();
        this.secondsRest = this.data[0].getSecondsRest();
        this.bossStatus = BossStatus.REST;
        BossManager.gI().addBoss(this);

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        boss.lv = j;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    public Boss(BossType bossType, int id, BossData... data) throws Exception {
        this.id = id;
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        services.BossSettings.get();
        this.secondsRest = this.data[0].getSecondsRest();
        this.bossStatus = BossStatus.REST;
        switch (bossType) {
            case YARDART ->
                YardartManager.gI().addBoss(this);
            case FINAL ->
                FinalBossManager.gI().addBoss(this);
            case SKILLSUMMONED ->
                SkillSummonedManager.gI().addBoss(this);
            case BROLY ->
                BrolyManager.gI().addBoss(this);
            case PHOBAN ->
                OtherBossManager.gI().addBoss(this);
            case PHOBANDT ->
                RedRibbonHQManager.gI().addBoss(this);
            case PHOBANBDKB ->
                TreasureUnderSeaManager.gI().addBoss(this);
            case PHOBANCDRD ->
                SnakeWayManager.gI().addBoss(this);
            case PHOBANKGHD ->
                GasDestroyManager.gI().addBoss(this);
            case TRUNGTHU_EVENT ->
                TrungThuEventManager.gI().addBoss(this);
            case HIT_EVENT ->
                HitEventManager.gI().addBoss(this);
            case CHRISTMAS_EVENT ->
                ChristmasEventManager.gI().addBoss(this);
            case HUNGVUONG_EVENT ->
                HungVuongEventManager.gI().addBoss(this);
            case TET_EVENT ->
                LunarNewYearEventManager.gI().addBoss(this);
            case TEA_EVENT ->
                TeaEventManager.gI().addBoss(this);
            case BKTNE ->
                Bktne.gI().addBoss(this);
            case BOSS12H ->
                Boss12hManager.gI().addBoss(this);
            case BOSS14H ->
                Boss14hManager.gI().addBoss(this);
        }

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    @Override
    public void initBase() {
        configuredDropsAwarded = false;
        BossData data = this.data[this.currentLevel];
        this.name = String.format(data.getName(), Util.nextInt(0, 100));
        this.gender = data.getGender();
        this.nPoint.mpg = 31_07_2002;
        this.nPoint.dameg = data.getDame();
        this.nPoint.hpg = data.getHp()[Util.nextInt(0, data.getHp().length - 1)];
        this.nPoint.hp = nPoint.hpg;
        this.nPoint.calPoint();
        this.initSkill();
        this.resetBase();
    }

    public void initSkill() {
        for (Skill skill : this.playerSkill.skills) {
            skill.dispose();
        }
        this.playerSkill.skills.clear();
        this.playerSkill.skillSelect = null;
        int levelIdx = (this.currentLevel >= 0 && this.data != null && this.currentLevel < this.data.length) ? this.currentLevel : 0;
        int[][] skillTemps = (this.data != null && this.data.length > levelIdx && this.data[levelIdx] != null) ? this.data[levelIdx].getSkillTemp() : null;
        if (skillTemps != null) {
            for (int[] skillTemp : skillTemps) {
                Skill skill = SkillUtil.createSkill(skillTemp[0], skillTemp[1]);
                if (skillTemp.length == 3) {
                    skill.coolDown = skillTemp[2];
                }
                this.playerSkill.skills.add(skill);
            }
        }
    }

    protected void resetBase() {
        this.lastTimeChatS = 0;
        this.lastTimeChatE = 0;
        this.timeChatS = 0;
        this.timeChatE = 0;
        this.indexChatS = 0;
        this.indexChatE = 0;
        this.lastTimeStartChatS = 0;
        this.lastTimeStartChatE = 0;
    }

    //.outfit.
    @Override
    public short getHead() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        }
        return this.data[this.currentLevel].getOutfit()[0];
    }

    @Override
    public short getBody() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        }
        return this.data[this.currentLevel].getOutfit()[1];
    }

    @Override
    public short getLeg() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        }
        return this.data[this.currentLevel].getOutfit()[2];

    }

    @Override
    public short getFlagBag() {
        return this.data[this.currentLevel].getOutfit()[3];
    }

    @Override
    public short getAura() {
        return (short) this.data[this.currentLevel].getOutfit()[4];
    }

    @Override
    public byte getEffFront() {
        return (byte) this.data[this.currentLevel].getOutfit()[5];
    }

    public Zone getMapJoin() {
        int[] mapJoinArr = this.data[this.currentLevel].getMapJoin();
        if (mapJoinArr == null || mapJoinArr.length == 0) {
            return null;
        }
        java.util.List<Integer> validMaps = new java.util.ArrayList<>();
        for (int m : mapJoinArr) {
            if (m != 5 && m != -1) {
                validMaps.add(m);
            }
        }
        if (validMaps.isEmpty()) {
            return null;
        }
        int mapId = validMaps.get(Util.nextInt(0, validMaps.size() - 1));
        Zone map = MapService.gI().getMapWithRandZone(mapId);
        return map;
    }

    @Override
    public void changeStatus(BossStatus status) {
        this.bossStatus = status;
        if (status == BossStatus.ACTIVE) {
            this.changeToTypePK();
        } else if (status == BossStatus.CHAT_E) {
            this.lastTimeStartChatE = System.currentTimeMillis();
        } else if (status == BossStatus.CHAT_S) {
            this.lastTimeStartChatS = System.currentTimeMillis();
        }
    }

    @Override
    public Player getPlayerAttack() {
        if (this.zone == null) {
            return null;
        }
        if (this.playerTarger != null && (this.playerTarger.isDie() || !this.zone.equals(this.playerTarger.zone))) {
            this.playerTarger = null;
        }
        if (this.playerTarger == null || Util.canDoWithTime(this.lastTimeTargetPlayer, this.timeTargetPlayer)) {
            this.playerTarger = this.zone.getRandomPlayerInMap();
            this.lastTimeTargetPlayer = System.currentTimeMillis();
            this.timeTargetPlayer = Util.nextInt(5000, 7000);
        }
        if (this.playerTarger != null && this.playerTarger.isPet && ((Pet) this.playerTarger).master != null && ((Pet) this.playerTarger).master.equals(this)) {
            this.playerTarger = null;
        }
        return this.playerTarger;
    }

    @Override
    public void changeToTypePK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.PK_ALL);
    }

    @Override
    public void changeToTypeNonPK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.NON_PK);
    }

    @Override
    public void updateInfo() {
        super.update();
    }

    @Override
    public void update() {
        if (prepareBom) {
            return;
        }
        giuTrongMap();
        super.update();
        this.nPoint.mp = this.nPoint.mpg;

        // Failsafe: Nếu boss đã hết máu mà chưa chuyển sang trạng thái chết/rời map
        if (this.isDie()) {
            if (this.bossStatus != BossStatus.DIE && this.bossStatus != BossStatus.CHAT_E
                    && this.bossStatus != BossStatus.LEAVE_MAP && this.bossStatus != BossStatus.REST
                    && this.bossStatus != BossStatus.RESPAWN) {
                this.changeStatus(BossStatus.DIE);
            }
        }

        switch (this.bossStatus) {
            case CHAT_S, AFK, ACTIVE ->
                this.autoLeaveMap();
        }
        switch (this.bossStatus) {
            case REST ->
                this.rest();
            case RESPAWN -> {
                this.respawn();
                this.changeStatus(BossStatus.JOIN_MAP);
            }
            case JOIN_MAP ->
                this.joinMap();
            case CHAT_S -> {
                if (chatS()) {
                    this.doneChatS();
                    this.lastTimeChatM = System.currentTimeMillis();
                    this.timeChatM = 5000;
                    if (this.bossStatus != BossStatus.AFK) {
                        this.changeStatus(BossStatus.ACTIVE);
                    }
                }
            }
            case AFK ->
                this.afk();
            case ACTIVE -> {
                if (this.effectSkill == null || this.effectSkill.isHaveEffectSkill() || (this.newSkill != null && this.newSkill.isStartSkillSpecial)) {
                    return;
                }
                this.chatM();
                if (this.effectSkill.isCharging && !Util.isTrue(1, 20) || this.effectSkill.useTroi) {
                    return;
                }
                this.active();
            }
            case DIE ->
                this.changeStatus(BossStatus.CHAT_E);
            case CHAT_E -> {
                if (chatE()) {
                    this.finishDeathChat();
                }
            }
            case LEAVE_MAP ->
                this.leaveMap();
        }
    }

    @Override
    public void rest() {
        int nextLevel = this.currentLevel + 1;
        if (nextLevel >= this.data.length) {
            nextLevel = 0;
        }
        int waitSec = (this.data != null && this.data.length > 0 && this.data[0] != null)
                ? this.data[0].getSecondsRest() : this.secondsRest;
        if (this.data[nextLevel].getTypeAppear() == AppearType.DEFAULT_APPEAR
                && Util.canDoWithTime(lastTimeRest, waitSec * 1000L)) {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void afk() {

    }

    @Override
    public void respawn() {
        this.currentLevel++;
        if (this.currentLevel >= this.data.length) {
            this.currentLevel = 0;
        }
        this.changeToTypeNonPK();
        this.initBase();
    }

    @Override
    public void joinMap() {
        if (!Util.canDoWithTime(this.lastTimeJoinAttempt, 1000)) {
            return;
        }
        this.lastTimeJoinAttempt = System.currentTimeMillis();
        try {
            this.tryJoinMap();
        } finally {
            // Retry this form after a failed spawn; RESPAWN advances the form,
            // while REST can wait forever for a parent/ANOTHER_LEVEL trigger.
            if (this.bossStatus == BossStatus.REST || this.bossStatus == BossStatus.RESPAWN) {
                this.changeStatus(BossStatus.JOIN_MAP);
            }
        }
    }

    private void tryJoinMap() {
        if (zoneFinal != null) {
            if (zoneFinal.map.mapId == 5) {
                zoneFinal = Util.randomAllMap();
            }
            if (zoneFinal != null && zoneFinal.map.mapId != 5) {
                joinMapByZone(zoneFinal);
                this.notifyJoinMap();
                this.changeStatus(BossStatus.CHAT_S);
                this.wakeupAnotherBossWhenAppear();
                return;
            }
        }
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone == null || this.zone.map.mapId == 5) {
            this.zone = getMapJoin();
        }
        if (this.zone != null && this.zone.map.mapId == 5) {
            this.changeStatus(BossStatus.REST);
            this.zone = null;
            this.lastZone = null;
            return;
        }
        if (this.zone != null) {
            try {
                if (this.currentLevel == 0) {
                    if (this.parentBoss == null) {
                        int zoneid = 0;
                        //this.zone.map.mapId == 80 || this.zone.map.mapId == 103 || this.zone.map.mapId == 97 || this.zone.map.mapId == 102
                        // Chỉ cho boss xuất hiện từ khu 2 trở lên ở map thường
                        boolean alreadyInZone = this.zone.getBosses().contains(this);
                        if (this.zone.map.zones.size() > 2 && !alreadyInZone) {
                            int totalZones = this.zone.map.zones.size();
                            int first = Util.nextInt(1, totalZones - 1);
                            zoneid = totalZones;
                            for (int i = 0; i < totalZones - 1; i++) {
                                int candidate = 1 + (first - 1 + i) % (totalZones - 1);
                                if (this.zone.map.zones.get(candidate).getBosses().isEmpty()) {
                                    zoneid = candidate;
                                    break;
                                }
                            }

                            if (zoneid < this.zone.map.zones.size()) {
                                this.zone = this.zone.map.zones.get(zoneid);
                            } else {
                                this.changeStatus(BossStatus.REST);
                                this.zone = null;
                                this.lastZone = null;
                                return;
                            }
                        } else if (!alreadyInZone) {
                            // Check trong khu lớn hơn 10 người chuyển sang khu n + 1
                            while (zoneid < this.zone.map.zones.size() && this.zone.map.zones.get(zoneid).getNumOfPlayers() > 10) {
                                zoneid++;
                            }
                            // Check trong khu có boss sẽ chuyển sang khu n + 1
                            while (zoneid < this.zone.map.zones.size() && !this.zone.map.zones.get(zoneid).getBosses().isEmpty()) {
                                zoneid++;
                            }
                            if (zoneid < this.zone.map.zones.size()) {
                                this.zone = this.zone.map.zones.get(zoneid);
                            } else {
                                this.zone = this.zone.map.zones.get(0);
                            }
                        }
                        int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    } else {
                        int x = this.parentBoss.location.x - (this.lv + 1) * 30;
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    }
                    this.wakeupAnotherBossWhenAppear();
                } else {
                    ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
                }
                Service.gI().sendFlagBag(this);
                this.notifyJoinMap();
                PlayerService.gI().hoiPhuc(this, this.nPoint.hpMax, this.nPoint.mpMax);
                Service.gI().hsChar(this, this.nPoint.hpMax, this.nPoint.mpMax);
                Service.gI().Send_Info_NV(this);
                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
                if (error < 5) {
                    Logger.error("Lỗi : " + e + "\n");
                    error++;
                }
            }
        //     if(this.name == "Zeno")
        // System.out.println("zeno: " + this.nPoint.hp);
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    public void joinMapByZone(Zone zone) {
        if (zone != null) {
            if (zone.map.mapId == 5) {
                Zone alt = Util.randomAllMap();
                if (alt != null && alt.map.mapId != 5) {
                    zone = alt;
                }
            }
            if (zone != null && zone.map.mapId != 5) {
                this.zone = zone;
                int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
                int y = this.zone.map.yPhysicInTop(x, 100);
                ChangeMapService.gI().changeMap(this, this.zone, x, y);
            }
        }
    }

    protected void notifyJoinMap() {
        if (canSendNotify()) {
            ServerNotify.gI().notify("BOSS " + this.name + " vừa xuất hiện tại " + this.zone.map.mapName);
        }
    }

    private boolean canSendNotify() {
        if (this.zone == null || this.zone.map == null) {
            return false;
        }
        return !(this.zone.map.mapId == 140
                || this instanceof models.boss.boss_list.Yardart.Yardart
                || this instanceof models.boss.boss_list.Nappa.Kuku
                || this instanceof models.boss.boss_list.Nappa.MapDauDinh
                || this instanceof models.boss.boss_list.Nappa.Rambo
                || MapService.gI().isMapYardart(this.zone.map.mapId)
                || MapService.gI().isMapPhoBan(this.zone.map.mapId)
                || MapService.gI().isMapMaBu(this.zone.map.mapId)
                || MapService.gI().isMapBlackBallWar(this.zone.map.mapId));
    }

    @Override
    public boolean chatS() {
        if (this.lastTimeStartChatS != 0 && Util.canDoWithTime(this.lastTimeStartChatS, 5000)) {
            return true;
        }
        if (Util.canDoWithTime(lastTimeChatS, timeChatS)) {
            if (this.data[this.currentLevel].getTextS() == null || this.indexChatS >= this.data[this.currentLevel].getTextS().length) {
                return true;
            }
            try {
                String textChat = this.data[this.currentLevel].getTextS()[this.indexChatS];
                int prefix = -1;
                if (textChat.startsWith("|") && textChat.indexOf("|", 1) > 0) {
                    prefix = Integer.parseInt(textChat.substring(1, textChat.indexOf("|", 1)));
                    textChat = textChat.substring(textChat.indexOf("|", 1) + 1);
                }
                this.chat(prefix, textChat);
                this.lastTimeChatS = System.currentTimeMillis();
                this.timeChatS = Math.min(2000, Math.max(500, textChat.length() * 100));
                this.indexChatS++;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doneChatS() {

    }

    @Override
    public void chatM() {
        if (this.typePk == ConstPlayer.NON_PK) {
            return;
        }
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    protected long lastTimeAttack;

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                if (this.playerSkill.skills == null || this.playerSkill.skills.isEmpty()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                // Vòng này chạy 10 LẦN/GIÂY cho MỖI boss. printStackTrace() vừa dựng
                // lại stack vừa khoá System.err, nên nếu lỗi lặp lại (boss bị kéo lên
                // mép map / ra ngoài map là dính ngay) thì cả server khựng theo. Chỉ
                // ghi log thưa để vẫn biết có lỗi mà không tự bóp cổ mình.
                if (Util.canDoWithTime(lastTimeLogLoiDanh, 10_000)) {
                    lastTimeLogLoiDanh = System.currentTimeMillis();
                    Logger.error("[BOSS] Lỗi khi đánh (" + this.name + "): " + ex + "\n");
                }
            }
        }
    }

    private long lastTimeLogLoiDanh;

    @Override
    public void checkPlayerDie(Player player) {
        if (player.isDie()) {

        }
    }

    protected int getRangeCanAttackWithSkillSelect() {
        int skillId = this.playerSkill.skillSelect.template.id;
        if (skillId == Skill.KAMEJOKO || skillId == Skill.MASENKO || skillId == Skill.ANTOMIC) {
            return Skill.RANGE_ATTACK_CHIEU_CHUONG;
        } else if (skillId == Skill.DRAGON || skillId == Skill.DEMON || skillId == Skill.GALICK || skillId == Skill.LIEN_HOAN || skillId == Skill.KAIOKEN) {
            return Skill.RANGE_ATTACK_CHIEU_DAM;
        }
        return 500;
    }

    @Override
    public void die(Player plKill) {
        if (plKill != null) {
            try {
                rewardWithConfiguredDrops(plKill);
            } catch (Exception e) {
                utils.Logger.logException(Boss.class, e);
            }
            try {
                if (!(this instanceof models.boss.boss_list.Yardart.Yardart)
                        && (this.zone == null || this.zone.map == null || !MapService.gI().isMapYardart(this.zone.map.mapId))) {
                    ServerNotify.gI().notify(plKill.name + ": Đã tiêu diệt được " + this.name + " mọi người đều ngưỡng mộ.");
                }
            } catch (Exception e) {
            }
        }
        this.changeStatus(BossStatus.DIE);
    }

    private volatile boolean configuredDropsAwarded;

    /** All death reward paths use this entry; subclass reward/task logic remains intact. */
    public final void rewardWithConfiguredDrops(Player killer) {
        synchronized (this) {
            if (configuredDropsAwarded) return;
            configuredDropsAwarded = true;
        }
        var dropZone = this.zone;
        int x = this.location.x, y = this.location.y;
        var keys = this.data != null && currentLevel >= 0 && currentLevel < data.length
                ? services.BossSettings.get().keysFor(new BossData[]{data[currentLevel]}) : java.util.List.<String>of();
        Player owner = killer instanceof models.player.Pet pet ? pet.master : killer;
        try { reward(killer); }
        finally {
            try { services.BossDropRules.get().drop(dropZone, keys, owner, x, y); }
            catch (Exception error) { utils.Logger.logException(Boss.class, error); }
        }
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public boolean chatE() {
        if (this.lastTimeStartChatE != 0 && Util.canDoWithTime(this.lastTimeStartChatE, 4000)) {
            return true;
        }
        if (Util.canDoWithTime(lastTimeChatE, timeChatE)) {
            if (this.data[this.currentLevel].getTextE() == null || this.indexChatE >= this.data[this.currentLevel].getTextE().length) {
                return true;
            }
            try {
                String textChat = this.data[this.currentLevel].getTextE()[this.indexChatE];
                int prefix = -1;
                if (textChat.startsWith("|") && textChat.indexOf("|", 1) > 0) {
                    prefix = Integer.parseInt(textChat.substring(1, textChat.indexOf("|", 1)));
                    textChat = textChat.substring(textChat.indexOf("|", 1) + 1);
                }
                this.chat(prefix, textChat);
                this.lastTimeChatE = System.currentTimeMillis();
                this.timeChatE = Math.min(2000, Math.max(500, textChat.length() * 100));
                this.indexChatE++;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doneChatE() {

    }

    protected final void finishDeathChat() {
        try {
            this.doneChatE();
        } catch (Exception error) {
            Logger.logException(Boss.class, error);
        } finally {
            // A departed parent or a failed companion callback must not strand a corpse.
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }

    @Override
    public void leaveMap() {
        if (this.currentLevel < this.data.length - 1) {
            this.lastZone = this.zone;
            this.changeStatus(BossStatus.RESPAWN);
        } else {
            ChangeMapService.gI().exitMap(this);
            this.lastZone = null;
            this.lastTimeRest = System.currentTimeMillis();
            if (this.data != null && this.data.length > 0 && this.data[0] != null) {
                this.secondsRest = this.data[0].getSecondsRest();
            }
            this.changeStatus(BossStatus.REST);
        }
        this.wakeupAnotherBossWhenDisappear();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
            }

            damage = this.nPoint.subDameInjureWithDeff(damage);

            if (effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }

            this.nPoint.subHP(damage);

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return damage;
        } else {
            if (this.bossStatus != BossStatus.DIE && this.bossStatus != BossStatus.CHAT_E
                    && this.bossStatus != BossStatus.LEAVE_MAP && this.bossStatus != BossStatus.REST
                    && this.bossStatus != BossStatus.RESPAWN) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return 0;
        }
    }

    @Override
    public void moveToPlayer(Player pl) {
        if (pl.location != null) {
            moveTo(pl.location.x, pl.location.y);
        }
    }

    @Override
    public void moveTo(int x, int y) {
        byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
        byte move = (byte) Util.nextInt(40, 60);
        int dichX = this.location.x + (dir == 1 ? move : -move);
        int dichY = y + (Util.isTrue(3, 10) ? -50 : 0);
        // Boss đuổi theo người chơi bị kéo lên mép trên / ra ngoài map sẽ tự đi theo
        // ra đó; ngoài biên là mọi phép tính theo ô tile vượt mảng và ném lỗi liên tục.
        if (this.zone != null && this.zone.map != null) {
            dichX = this.zone.map.kepX(dichX);
            dichY = this.zone.map.kepY(dichY);
        }
        PlayerService.gI().playerMove(this, dichX, dichY);
    }

    /**
     * Kéo boss về trong bản đồ nếu vì đường nào đó nó lọt ra ngoài. Gọi mỗi nhịp nên
     * dù vị trí bị gán thẳng ở chỗ khác thì cũng chỉ lệch đúng một nhịp.
     */
    protected void giuTrongMap() {
        try {
            if (this.zone == null || this.zone.map == null || this.location == null) {
                return;
            }
            int x = this.zone.map.kepX(this.location.x);
            int y = this.zone.map.kepY(this.location.y);
            if (x != this.location.x || y != this.location.y) {
                this.location.x = x;
                this.location.y = y;
            }
        } catch (Exception ignored) {
        }
    }

    public void chat(String text) {
        Service.gI().chat(this, text);
    }

    protected boolean chat(int prefix, String textChat) {
        try {
            if (prefix == -1) {
                this.chat(textChat);
            } else if (prefix == -2) {
                if (this.zone != null) {
                    Player plMap = this.zone.getRandomPlayerInMap();
                    if (plMap != null && !plMap.isDie() && Util.getDistance(this, plMap) <= 600) {
                        Service.gI().chat(plMap, textChat);
                    }
                }
            } else if (prefix == -3) {
                if (this.parentBoss != null && !this.parentBoss.isDie()) {
                    this.parentBoss.chat(textChat);
                }
            } else if (prefix >= 0) {
                if (this.bossAppearTogether != null && this.bossAppearTogether[this.currentLevel] != null
                        && prefix < this.bossAppearTogether[this.currentLevel].length) {
                    Boss boss = this.bossAppearTogether[this.currentLevel][prefix];
                    if (boss != null && !boss.isDie()) {
                        boss.chat(textChat);
                    }
                } else if (this.parentBoss != null && this.parentBoss.bossAppearTogether != null
                        && this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] != null
                        && prefix < this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel].length) {
                    Boss boss = this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel][prefix];
                    if (boss != null && !boss.isDie()) {
                        boss.chat(textChat);
                    }
                }
            }
        } catch (Exception e) {
        }
        return true;
    }

    @Override
    public void wakeupAnotherBossWhenAppear() {
        if (this.bossAppearTogether == null || this.bossAppearTogether[this.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
            int nextLevelBoss = boss.currentLevel + 1;
            if (nextLevelBoss >= boss.data.length) {
                nextLevelBoss = 0;
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.CALL_BY_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.APPEAR_WITH_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
                boss.changeStatus(BossStatus.RESPAWN);
            }
        }
    }

    @Override
    public void wakeupAnotherBossWhenDisappear() {
    }

    @Override
    public void autoLeaveMap() {

    }

    public void leaveMapNew() {
        if (this.data != null) {
            this.currentLevel = this.data.length;
        }
        this.changeStatus(BossStatus.LEAVE_MAP);
    }

    @Override
    public void setBom(Player plAtt) {
        try {
            if (!prepareBom) {
                prepareBom = true;
                this.nPoint.hp = 1;
                //gồng tự sát
                Service.gI().chat(Boss.this, "Rồi, rồi, mày xong rồi!");
                try {
                    Message msg = new Message(-45);
                    msg.writer().writeByte(7);
                    msg.writer().writeInt((int) Boss.this.id);
                    msg.writer().writeShort(104);
                    msg.writer().writeShort(2000);
                    Service.gI().sendMessAllPlayerInMap(Boss.this, msg);
                    msg.cleanup();
                } catch (Exception e) {
                }
                new Thread(() -> {
                    try {
                        Thread.sleep(2500);
                        setDie(this);
                        die(plAtt);
                        if (Boss.this.zone != null) {
                            long dame = Boss.this.nPoint.hpMax;
                            for (Mob mob : Boss.this.zone.mobs) {
                                mob.injured(Boss.this, dame, true);
                            }
                            List<Player> playersMap = Boss.this.zone.getNotBosses();
                            if (!MapService.gI().isMapOffline(Boss.this.zone.map.mapId)) {
                                for (int i = playersMap.size() - 1; i >= 0; i--) {
                                    Player pl = playersMap.get(i);
                                    if (pl != null && !Boss.this.equals(pl)) {
                                        pl.injured(Boss.this, dame, false, false);
                                        PlayerService.gI().sendInfoHpMpMoney(pl);
                                        Service.gI().Send_Info_NV(pl);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        setDie(this);
                        die(plAtt);
                    } finally {
                        prepareBom = false;
                    }
                }, "Boss SetBom " + this.name).start();
            }
        } catch (Exception e) {
            prepareBom = false;
            setDie(this);
            die(plAtt);
        }
    }

}
