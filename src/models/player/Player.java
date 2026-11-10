package models.player;


import utils.Functions;
import models.npc.NonInteractiveNPC;
import models.radar.Card;
import models.radar.RadarCard;
import services.RadarService;
import services.dungeon.MajinBuuService;
import models.skill.PlayerSkill;
import java.util.List;

import models.clan.Clan;
import models.intrinsic.IntrinsicPlayer;
import models.item.Item;
import models.item.ItemTime;
import models.Template.ItemTemplate;
import services.ItemService;
import models.npc.specialnpc.MagicTree;
import consts.ConstPlayer;
import consts.ConstTask;
import models.npc.specialnpc.MabuEgg;
import models.npc.specialnpc.BillEgg;
import models.npc.specialnpc.BlackEgg;
import models.mob.MobMe;
import models.clan.ClanMember;
import consts.ConstAchievement;
import consts.ConstItem;
import consts.ConstMap;
import models.map.Zone;
import interfaces.IPVP;
import models.matches.TYPE_LOSE_PVP;
import models.skill.Skill;
import services.Service;
import network.session.MySession;
import models.task.TaskPlayer;
import network.io.Message;
import server.Client;
import services.EffectSkillService;
import services.player.FriendAndEnemyService;
import services.map.MapService;
import services.PetService;
import services.player.PlayerService;
import services.TaskService;
import services.map.ChangeMapService;
import models.combine.Combine;
import utils.Logger;
import utils.Util;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import services.dungeon.BlackBallWarService;
import managers.tournament.The23rdMartialArtCongressManager;
import models.map.ItemMap;
import models.map.MaBuHold;
import models.dungeon.MajinBuu14H;
import services.dungeon.SuperDivineWaterService;
import models.tournament.The23rdMartialArtCongress;
import services.tournament.GuruGuildWarService;
import services.player.InventoryService;
import services.dungeon.NgocRongNamecService;
import models.ShenronEvent;
import models.map.Boss10;
import server.Maintenance;
import services.func.minigame.ChonAiDay_Gem;
import services.func.minigame.ChonAiDay_Gold;
import services.func.minigame.ChonAiDay_Ruby;

public class Player implements Runnable {

    public int point_pokemon;
    public long Damage;
    public long timemaydam;
    public long lastAttackTime;
    public int hit;
    public long diemmaydam;
    public long lasttimeTD;
    public int sanBoss = 0;

    public boolean isCookingBanhDay = false;
    public boolean isCookingBanhChung = false;
    public long lastTimeEatPea;
    public List<Integer> idEffChar = new ArrayList<>();
    @Setter
    @Getter
    public MySession session;

    //Tài Xỉu - Chẵn Lẻ
    public int betChan, betLe, betTuTuTrang, betTuTuDo, bet3Trang1Do, bet3Do1Trang;

    public int luotNhanBuaMienPhi = 1;
    public int diemDanhSuKien = 1;
    public Date firstTimeLogin;

    public int diemsukien;
    public int phaobong;
    public int lixi;
    public int mocVongQuay;

    public boolean daNhanMocVongQuay(int index) {
        return (this.mocVongQuay & (1 << index)) != 0;
    }

    public void setNhanMocVongQuay(int index) {
        this.mocVongQuay |= (1 << index);
    }

    /**
     * Part của danh hiệu đang hiện trên đầu nhân vật, -1 là chưa hiện gì. Chỉ dùng để gỡ
     * đúng hiệu ứng cũ trước khi vẽ cái mới, không lưu DB.
     */
    public int partDanhHieuDangHien = -1;

    public PlayerEffect effect;

    public long goldTai, goldXiu;
    public long id;
    public String name;
    public byte gender;
    public boolean isNewMember;
    public short head;
    public int deltaTime;
    public byte typePk;
    public byte cFlag;
    public boolean haveTennisSpaceShip;
    public boolean resetdame = false;
    public long lastTimeDame;
    public long banhtrung;
    public long banhtet;
    public double dametong = 0;
    public long lastTimeUseOption;

    public boolean isBot;

    public boolean isCopy;

    public boolean beforeDispose;

    public int rubyNormar;
    public int rubyVIP;
    public int id_CSMM_Ruby;

    public int gemNormar;
    public int gemVIP;
    public int id_CSMM_Gem;

    public int goldNormar;
    public int goldVIP;
    public int id_CSMM_Gold;

    public List<String> textRuongGo = new ArrayList<>();

    public int mbv = 0;
    public boolean baovetaikhoan;
    public long mbvtime;

    public int timeGohome;

    public long lastUpdateGohomeTime;

    public boolean goHome;

    public long lastPkCommesonTime;

    public boolean callBossPocolo;
    public Zone zoneSieuThanhThuy;
    public boolean winSTT;
    public long lastTimeWinSTT;
    public long lastTimeUpdateSTT;

    public MajinBuu14H maBu2H;
    public boolean isMabuHold;
    public MaBuHold maBuHold;
    public int precentMabuHold;
    public boolean isPhuHoMapMabu;

    public boolean danhanthoivang;
    public long lastRewardGoldBarTime;

    public int timesPerDayBDKB = 0;
    public long lastTimeJoinBDKB;

    public boolean joinCDRD;
    public long lastTimeJoinCDRD;
    public boolean talkToThuongDe;
    public boolean talkToThanMeo;
    public long timeChangeMap144;

    public long lastTimeJoinDT;

    public int typeChibi;
    public long lastTimeChibi;
    public long lastTimeUpdateChibi;

    public String captcha = "";
    public boolean doesNotAttack;
    public long lastTimePlayerNotAttack;
    public int timeNotAttack = 1800000;

    /** Đếm tick (mỗi tick ≈ 1s) để kiểm tra hết hạn item option 93 mỗi 5 phút. */
    private transient int tickCountItemExpiry = 0;

    public boolean isPet;
    public boolean isNewPet;
    public boolean isNewPet1;
    public boolean isBoss;
    public boolean isPlayer;
    public IPVP pvp;
    public byte maxTime = 30;
    public byte type = 0;
    public boolean isOffline = false;

    public String notify = null;

    public int mapIdBeforeLogout;

    public long map182Deadline;
    public int restrictedMapTicket = -1;

    public long hitBossLv20Time;

    public int halloweenRewardClaimed;

    public int halloweenLevelDone;
    public List<Zone> mapBlackBall;
    public List<Zone> mapMaBu;

    public List<Player> temporaryEnemies = new ArrayList<>();

    public Zone zone;
    public Zone mapBeforeCapsule;
    public List<Zone> mapCapsule;
    public Pet pet;
    public NewPet newPet;
    public MobMe mobMe;
    public Location location;
    public SetClothes setClothes;
    public EffectSkill effectSkill;
    public BillEgg billEgg;
    public MabuEgg mabuEgg;
    public BlackEgg blackEgg;
    public TaskPlayer playerTask;
    public ItemTime itemTime;
    public Fusion fusion;
    public boolean isOffPorataOutfit;
    public MagicTree magicTree;
    public IntrinsicPlayer playerIntrinsic;
    public Inventory inventory;
    public PlayerSkill playerSkill;
    public Combine combineNew;
    public IDMark idMark;
    public Charms charms;
    public EffectSkin effectSkin;
    public NPoint nPoint;
    public RewardBlackBall rewardBlackBall;
    public FightMabu fightMabu;
    public NewSkill newSkill;
    public Satellite satellite;
    public Achievement achievement;
    public GiftCode giftCode;
    public Traning traning;

    public Clan clan;
    public ClanMember clanMember;

    public List<Friend> friends;
    public List<Enemy> enemies;

    public boolean justRevived;
    public long lastTimeRevived;

    public long timeChangeZone;
    public long lastUseOptionTime;

    public short idNRNM = -1;
    public short idGo = -1;
    public long lastTimePickNRNM;

    public List<Card> Cards = new ArrayList<>();

    public int levelWoodChest;
    public long goldChallenge;
    public long rubyChallenge;
    public long lastTimeRewardWoodChest;
    public List<Item> itemsWoodChest = new ArrayList<>();
    public int indexWoodChest;
    public long lastTimePKDHVT23;

    public boolean lostByDeath;

    public boolean isPKDHVT;

    public int xSend;
    public int ySend;
    public boolean isFly;
    public int idAuraTest = -1;

    // Dai Chien Bang Hoi Dao Guru
    public boolean isBuffGuruWar;
    public long lastTimeWinGuruWar;

    // shenron event
    public long lastTimeShenronAppeared;
    public boolean isShenronAppear;
    public ShenronEvent shenronEvent;

    // vo dai sinh tu
    public long lastTimePKVoDaiSinhTu;
    public boolean haveRewardVDST;
    public int thoiVangVoDaiSinhTu;
    public long timePKVDST;

    public int binhChonHatMit;
    public int binhChonPlayer;
    public Zone zoneBinhChon;

    public ItemEvent itemEvent;

    public int levelLuyenTap;
    public boolean isThachDau;
    public int tnsmLuyenTap;
    public boolean dangKyTapTuDong;
    public long lastTimeOffline;
    public int mapIdDangTapTuDong;
    public int lastMapOffline;
    public int lastZoneOffline;
    public int lastXOffline;
    public String thongBaoTapTuDong;
    public boolean teleTapTuDong;

    public boolean isTrade;

    public int timesPerDayCuuSat;
    public long lastTimeCuuSat;
    public boolean nhanVangNangVIP;
    public boolean nhanDeTuNangVIP;
    public boolean nhanSKHVIP;

    public long totalDamageTaken;
    public boolean thongBaoChangeMap;
    public String textThongBaoChangeMap;
    public boolean thongBaoThua;
    public String textThongBaoThua;

    public SuperRank superRank;

    public boolean canReward;
    public boolean changeMapVIP;
    public boolean haveReward;
    public Set<Integer> npcsTrickOrTreat = new HashSet<>();
    public long lastTimeTrickOrTreat;

    public void checkResetTrickOrTreat() {
        if (Util.isAfterReset3AM(this.lastTimeTrickOrTreat)) {
            this.npcsTrickOrTreat.clear();
            this.lastTimeTrickOrTreat = System.currentTimeMillis();
        }
    }

    public int tayThong;
    public HocKyNang hocKyNang;
    public List<Integer> BoughtSkills = new ArrayList();
    public List<Item> itemsTradeWVP = new ArrayList<>();
    public long goldTradeWVP;
    public boolean tradeWVP;
    public long plIdWVP;

    private DropItem dropItem;
    public ItemMap veTinhTriLuc = null;
    public ItemMap veTinhSinhLuc = null;
    public ItemMap veTinhPhongThu = null;
    public ItemMap veTinhTriTue = null;

    public long lastTimeVeTinhTriLuc = 0;
    public long lastTimeVeTinhSinhLuc = 0;
    public int daubuoi;
    // Chống spam skill
    public boolean isCastingSkill = false;
    public long lastTimeCasting = 0;

    public Player() {
        effect = new PlayerEffect(this);
        lastTimeUseOption = System.currentTimeMillis();
        lastUseOptionTime = System.currentTimeMillis();
        location = new Location();
        hocKyNang = new HocKyNang();
        nPoint = new NPoint(this);
        inventory = new Inventory();
        playerSkill = new PlayerSkill(this);
        setClothes = new SetClothes(this);
        effectSkill = new EffectSkill(this);
        fusion = new Fusion(this);
        playerIntrinsic = new IntrinsicPlayer();
        rewardBlackBall = new RewardBlackBall(this);
        fightMabu = new FightMabu(this);
        idMark = new IDMark();
        combineNew = new Combine();
        playerTask = new TaskPlayer();
        friends = new ArrayList<>();
        enemies = new ArrayList<>();
        itemTime = new ItemTime(this);
        charms = new Charms();
        effectSkin = new EffectSkin(this);
        newSkill = new NewSkill(this);
//        satellite = new Satellite();
        achievement = new Achievement(this);
        giftCode = new GiftCode();
        traning = new Traning();
        itemEvent = new ItemEvent(this);
        superRank = new SuperRank(this);
        dropItem = new DropItem(this);
    }

    //--------------------------------------------------------------------------
    public boolean isDie() {
        if (this.nPoint != null) {
            return this.nPoint.hp <= 0;
        }
        return true;
    }

    public void sendMessage(Message msg) {
        if (this.session != null) {
            session.sendMessage(msg);
        }
    }

    public boolean isPl() {
        return isPlayer && !isPet && !isBoss && !isBot && !isNewPet && !isNewPet1 && !(this instanceof NonInteractiveNPC);
    }

    @Override
    public void run() {
        Functions.sleep(500);
        while (!Maintenance.isRunning && session != null && session.isConnected() && this.name != null) {
            long st = System.currentTimeMillis();
            update();
            long time = 1000 - (System.currentTimeMillis() - st);
            if (time > 0) {
                Functions.sleep(time);
            }
        }
    }

    public void start() {
        new Thread(this, "Update player " + this.name).start();
    }

    public void update() {
        if (!this.beforeDispose && !isBot) {
            try {
                // Thời gian online tích luỹ cho hệ thống quà (opcode 99) — update() chạy nhịp 1 giây.
                services.func.GiftProgressService.gI().onOnlineTick(this);
                // Sự kiện "Dắt Đường Tăng" — tự trả quà khi player vào đúng map đích.
                models.npc.npc_list.DuongTang.tickCheckArrive(this);
                // Map 182 (Núi Seo Phi): hết 30 phút thì đẩy về nhà
                if (this.isPl() && this.zone != null && this.zone.map != null && this.zone.map.mapId == 182
                        && this.map182Deadline > 0 && System.currentTimeMillis() >= this.map182Deadline) {
                    this.map182Deadline = 0;
                    services.Service.gI().sendThongBao(this, "Đã hết 30 phút trong map, đưa bạn về nhà!");
                    services.map.ChangeMapService.gI().changeMapNonSpaceship(this, this.gender + 21, 300, 336);
                }
                // Buff item 1016 (+10% HP/KI/sức đánh): hết hạn -> tính lại chỉ số bỏ buff
                if (this.isPl() && this.charms != null && this.charms.tdBuff1016 > 0
                        && System.currentTimeMillis() >= this.charms.tdBuff1016) {
                    this.charms.tdBuff1016 = 0;
                    this.nPoint.calPoint();
                    services.Service.gI().point(this);
                    services.ItemTimeService.gI().removeItemTime(this, 9068); // gỡ icon buff (icon_id item 1016)
                    services.Service.gI().sendThongBao(this, "Đã hết hiệu lực buff +10% chỉ số.");
                }
                if (this.zone != null || (!this.isPl() && this.zone == null)) {
                    if (itemTime != null) {
                        itemTime.update();
                    }
                    if (magicTree != null) {
                        magicTree.update();
                    }

                    if (this.isPl() && this.zone != null && this.zone.map.mapId == this.gender + 21
                            && (TaskService.gI().getIdTask(this) == ConstTask.TASK_0_0 || TaskService.gI().getIdTask(this) == ConstTask.TASK_0_1)) {
                        this.playerTask.taskMain.index = 2;
                        TaskService.gI().sendTaskMain(this);
                    }
                }

                // === Kiểm tra hết hạn đồ ngày (option 93) mỗi 60 giây ===
                if (this.isPl() && this.inventory != null && ++tickCountItemExpiry >= 60) {
                    tickCountItemExpiry = 0;
                    boolean bodyChanged = checkAndRemoveExpiredItems(this.inventory.itemsBody);
                    boolean bagChanged  = checkAndRemoveExpiredItems(this.inventory.itemsBag);
                    boolean boxChanged  = checkAndRemoveExpiredItems(this.inventory.itemsBox);
                    boolean crackBallChanged = checkAndRemoveExpiredItemsCrackBall(this.inventory.itemsBoxCrackBall);
                    boolean petBodyChanged = (this.pet != null && this.pet.inventory != null)
                            ? checkAndRemoveExpiredItems(this.pet.inventory.itemsBody)
                            : false;
                    if (bodyChanged) {
                        InventoryService.gI().sendItemBody(this);
                        this.nPoint.calPoint();
                        Service.gI().point(this);
                        Service.gI().Send_Caitrang(this);
                        Service.gI().sendFlagBag(this);
                        Service.gI().sendPetFollow(this, (short) 0);
                        Service.gI().sendEffPlayer(this);
                    }
                    if (bagChanged) {
                        InventoryService.gI().sendItemBags(this);
                    }
                    if (petBodyChanged && this.pet != null) {
                        if (this.pet.nPoint != null) {
                            this.pet.nPoint.calPoint();
                        }
                        Service.gI().Send_Caitrang(this.pet);
                        Service.gI().sendFlagBag(this.pet);
                        Service.gI().showInfoPet(this);
                    }
                    if (bodyChanged || bagChanged || boxChanged || crackBallChanged || petBodyChanged) {
                        final Player plSave = this;
                        new Thread(() -> {
                            try {
                                database.daos.PlayerDAO.updatePlayer(plSave);
                            } catch (Exception ignored) {}
                        }, "SaveExpiredItems-" + this.name).start();
                    }
                }

                if ((this.zone != null && !MapService.gI().isHome(this.zone.map.mapId)) || (!this.isPl() && this.zone == null)) {
                    if (isPl() && idMark != null && idMark.isBan() && Util.canDoWithTime(idMark.getLastTimeBan(), 5000)) {
                        Client.gI().kickSession(session);
                        return;
                    }

                    if (nPoint != null) {
                        nPoint.update();
                    }
                    if (fusion != null) {
                        fusion.update();
                    }
                    if (effectSkill != null) {
                        effectSkill.update();
                    }
                    if (mobMe != null) {
                        mobMe.update();
                    }
                    if (effectSkin != null) {
                        effectSkin.update();
                    }
                    if (pet != null) {
                        pet.update();
                    }
                    if (newPet != null) {
                        newPet.update();
                    }
                    if (satellite != null) {
                        satellite.update();
                    }

                    if (this.isPl() && !this.isDie() && this.effectSkill != null && !this.effectSkill.isChibi && Util.canDoWithTime(lastTimeChibi, 300000)) {
                        if (Util.isTrue(1, 10) && !MapService.gI().isMapBlackBallWar(this.zone.map.mapId)) {
                            EffectSkillService.gI().setChibi(this, 600000);
                        }
                        lastTimeChibi = System.currentTimeMillis();
                    }
                    if (this.isPl() && !this.isDie() && this.effectSkill != null && this.effectSkill.isChibi && Util.canDoWithTime(lastTimeUpdateChibi, 1000)) {
                        if (this.typeChibi == 1) {
                            if (this.nPoint.mp < this.nPoint.mpMax) {
                                if (this.nPoint.mpMax - this.nPoint.mp < this.nPoint.mpMax / 10) {
                                    this.nPoint.mp = this.nPoint.mpMax;
                                } else {
                                    this.nPoint.mp += this.nPoint.mpMax / 10;
                                }
                            }
                            PlayerService.gI().sendInfoMp(this);
                        } else if (this.typeChibi == 3) {
                            if (this.nPoint.hp < this.nPoint.hpMax) {
                                if (this.nPoint.hpMax - this.nPoint.hp < this.nPoint.hpMax / 10) {
                                    this.nPoint.hp = this.nPoint.hpMax;
                                } else {
                                    this.nPoint.hp += this.nPoint.hpMax / 10;
                                }
                            }
                            PlayerService.gI().sendInfoHp(this);
                        }
                        lastTimeUpdateChibi = System.currentTimeMillis();
                    }

                    if (this.isPl() && this.achievement != null) {
                        this.achievement.done(ConstAchievement.HOAT_DONG_CHAM_CHI, 1000);
                    }

                    if (this.zone != null && !this.zone.items.isEmpty()) {
                        List<ItemMap> itemList = new ArrayList<>(this.zone.items);
                        for (ItemMap item : itemList) {
                            if (item != null && Util.myGetDistcance(this.location.x, this.location.y, item.x, item.y) <= ConstMap.RANGE_VE_TINH && item.isBelongToMe(this)) {
                                switch (item.itemTemplate.id) {
                                    case 345 ->
                                        veTinhSinhLuc = item;
                                    case 344 ->
                                        veTinhPhongThu = item;
                                    case 343 ->
                                        veTinhTriTue = item;
                                    case 342 ->
                                        veTinhTriLuc = item;
                                    default -> {
                                    }
                                }
                            }
                        }
                    }

                    if (!this.isDie() && Util.canDoWithTime(lastTimeVeTinhTriLuc, ConstMap.TIME_HOI_VE_TINH)
                            && ItemMap.isInVeTinhRange(this, ConstItem.VE_TINH_TRI_LUC, this.location.x, this.location.y)) {
                        long mpTriLuc = NPoint.calPercent(this.nPoint.mpMax, ConstMap.PERCENT_VE_TINH_TRI_LUC);
                        this.nPoint.addMp(mpTriLuc);
                        PlayerService.gI().hoiPhuc(this, 0, mpTriLuc);
                        lastTimeVeTinhTriLuc = System.currentTimeMillis();
                    }

                    if (!this.isDie() && Util.canDoWithTime(lastTimeVeTinhSinhLuc, ConstMap.TIME_HOI_VE_TINH)
                            && ItemMap.isInVeTinhRange(this, ConstItem.VE_TINH_SINH_LUC, this.location.x, this.location.y)) {
                        long hpSinhLuc = NPoint.calPercent(this.nPoint.hpMax, ConstMap.PERCENT_VE_TINH_SINH_LUC);
                        this.nPoint.addHp(hpSinhLuc);
                        PlayerService.gI().hoiPhuc(this, hpSinhLuc, 0);
                        lastTimeVeTinhSinhLuc = System.currentTimeMillis();
                    }

                    if (this.isPl() && this.effectSkill != null && this.effectSkill.isMabuHold) {
                        this.nPoint.subHP(this.nPoint.hpMax / 100);
                        if (Util.isTrue(1, 10)) {
                            Service.gI().chat(this, "Cứu tôi với");
                        }
                        PlayerService.gI().sendInfoHp(this);
                        if (this.precentMabuHold > 15) {
                            EffectSkillService.gI().removeMabuHold(this);
                        }
                        if (this.nPoint.hp <= 0) {
                            EffectSkillService.gI().removeMabuHold(this);
                            setDie();
                        }
                    }

                    if (this.zone != null && this.effectSkin != null && this.effectSkin.xHPKI > 1 && !MapService.gI().isMapBlackBallWar(this.zone.map.mapId) && this.nPoint != null) {
                        this.effectSkin.xHPKI = 1;
                        this.nPoint.calPoint();
                        Service.gI().point(this);
                    }

                    if (this.zone != null && this.effectSkin != null && this.effectSkin.xDame > 1 && !MapService.gI().isMapBlackBallWar(this.zone.map.mapId) && this.nPoint != null) {
                        this.effectSkin.xDame = 1;
                        this.nPoint.calPoint();
                        Service.gI().point(this);
                    }

                    if (this.isPl() && this.zone != null) {
                        fixBlackBallWar();
                    }

                    if (this.zone != null && this.zone.map.mapId == (21 + this.gender)) {
                        if (this.mabuEgg != null) {
                            this.mabuEgg.sendMabuEgg();
                        }
                    }

                    if (this.isPhuHoMapMabu && this.zone != null && this.nPoint != null && !MapService.gI().isMapMabu2H(this.zone.map.mapId)) {
                        this.isPhuHoMapMabu = false;
                        this.nPoint.calPoint();
                        Service.gI().point(this);
                        Service.gI().Send_Info_NV(this);
                        Service.gI().Send_Caitrang(this);
                    }

                    if (this.isPl() && this.clan != null && this.clan.ConDuongRanDoc != null && this.joinCDRD
                            && this.clan.ConDuongRanDoc.allMobsDead && this.talkToThanMeo && this.zone != null && this.zone.map.mapId == 47
                            && Util.canDoWithTime(timeChangeMap144, 5000)) {
                        ChangeMapService.gI().changeMapYardrat(this, this.clan.ConDuongRanDoc.getMapById(144), 300 + Util.nextInt(-100, 100), 312);
                        this.timeChangeMap144 = System.currentTimeMillis();
                    }

                    if (this.isPl() && this.zone != null && !MapService.gI().isMapMaBu(this.zone.map.mapId) && (this.cFlag == 9 || this.cFlag == 10)) {
                        Service.gI().changeFlag(this, 0);
                    }

                    if (this.isPl() && this.superRank != null) {
                        if (Util.isAfterMidnight(this.superRank.lastRewardTime)) {
                            this.superRank.reward();
                        }
                        if (this.superRank.rank < 10 && TaskService.gI().getIdTask(this) == ConstTask.TASK_34_0) {
//                            TaskService.gI().checkDoneTaskSuperRank(this);
                        }
                    }

                    if (this.isPl() && this.zone != null && MapService.gI().isMapMaBu(this.zone.map.mapId) && this.cFlag != 9 && this.cFlag != 10) {
                        Service.gI().changeFlag(this, Util.nextInt(9, 10));
                    }

                    if (dropItem != null) {
                        dropItem.update();
                    }

                    Boss10.gI().update(this);
                    MajinBuuService.gI().update(this);
                    SuperDivineWaterService.gI().update(this);

                    if (!isBoss && this.idMark != null && this.idMark.isGotoFuture() && Util.canDoWithTime(this.idMark.getLastTimeGoToFuture(), 60000)) {
                        ChangeMapService.gI().changeMapBySpaceShip(this, 102, -1, Util.nextInt(60, 200));
                        this.idMark.setGotoFuture(false);
                    }
                }
            } catch (Exception e) {
                Logger.logException(Player.class, e, "Lỗi tại player: " + this.name);
            }
        }
    }

    private static final short[][] idOutfitFusion = {
        {380, 381, 382}, {383, 384, 385}, {391, 392, 393},
        {870, 871, 872}, {873, 874, 875}, {867, 868, 869},
        {1773, 1776, 1777}, {1679, 1682, 1683}, {1674, 1677, 1678},
        {1669, 1672, 1673}
    };

    public static final short[][] idOutfitGod = {
        {-1, 472, 473}, {-1, 476, 477}, {-1, 474, 475}
    };

    public static final short[][][] idOutfitHalloween = {
        {
            {545, 548, 549}, {547, 548, 549}, {546, 548, 549}
        },
        {
            {2082, 2085, 2086}, {2084, 2085, 2086}, {2083, 2085, 2086}
        },
        {
            {760, 761, 762}, {760, 761, 762}, {760, 761, 762}
        },
        {
            {654, 655, 656}, {654, 655, 656}, {654, 655, 656}
        },
        {
            {651, 652, 653}, {651, 652, 653}, {651, 652, 653}
        }};

    public static final short[][] idOutfitMafuba = {
        {2030, 2031, 2032}, {-1, -1, -1}, {1218, 1219, 1220}
    };

    public static final short[][] idItemBienHinh = {
        {1712, 1713, 1714, 1715, 1716, 1717}, // TRAI_DAT
        {1700, 1701, 1702, 1703, 1704, 1705}, // NAMEC
        {1706, 1707, 1708, 1709, 1710, 1711}  // XAYDA
    };

    private static final short[][][] idOutfitBienHinhFallback = {
        // TRAI_DAT (Goku)
        {
            {2289, 2290, 2291},
            {2292, 2290, 2291},
            {2293, 2290, 2291},
            {2294, 2290, 2291},
            {2295, 2290, 2291},
            {2296, 2290, 2291}
        },
        // NAMEC (Piccolo)
        {
            {2273, 2274, 2275},
            {2276, 2274, 2275},
            {2277, 2274, 2275},
            {2278, 2274, 2275},
            {2279, 2274, 2275},
            {2280, 2274, 2275}
        },
        // XAYDA (Vegeta)
        {
            {2281, 2282, 2283},
            {2284, 2282, 2283},
            {2285, 2282, 2283},
            {2286, 2282, 2283},
            {2287, 2282, 2283},
            {2288, 2282, 2283}
        }
    };

    // Aura riêng cho từng bộ cải trang 
    private static final short[][] auraBienHinhTheoItem = {
        {1711, 57}, {1717, 45}, {1705, 44},
        {1704, 48}, {1710, 56}, {1716, 56},
        {1709, 52}, {1715, 47}, {1703, 54},
        {1702, 49}, {1708, 50}, {1714, 50},
        {1701, 53}, {1707, 50}, {1713, 50},
        {1700, 54}, {1706, 51}, {1712, 51},
    };

    private short itemBienHinhHienTai() {
        short[] byLevel = idItemBienHinh[this.gender >= 0 && this.gender < idItemBienHinh.length
                ? this.gender : ConstPlayer.TRAI_DAT];
        int lv = effectSkill.levelBienHinh - 1;
        if (lv < 0) {
            lv = 0;
        }
        if (lv >= byLevel.length) {
            lv = byLevel.length - 1;
        }
        return byLevel[lv];
    }

    // idx: 0 = head, 1 = body, 2 = leg
    private short outfitBienHinh(int idx) {
        ItemTemplate temp = ItemService.gI().getTemplate(itemBienHinhHienTai());
        if (temp != null) {
            int part = idx == 0 ? temp.head : (idx == 1 ? temp.body : temp.leg);
            if (part != -1) {
                return (short) part;
            }
        }
        int g = (this.gender >= 0 && this.gender < idOutfitBienHinhFallback.length)
                ? this.gender : ConstPlayer.TRAI_DAT;
        int fb = effectSkill.levelBienHinh - 1;
        if (fb < 0) {
            fb = 0;
        }
        if (fb >= idOutfitBienHinhFallback[g].length) {
            fb = idOutfitBienHinhFallback[g].length - 1;
        }
        return idOutfitBienHinhFallback[g][fb][idx];
    }

    public int getHat() {
        return -1;
    }

    public short getAura() {
        if (idAuraTest != -1) {
            return (short) idAuraTest;
        }
        if (effectSkill != null && effectSkill.isBienHinh) {
            short itemBienHinh = itemBienHinhHienTai();
            for (short[] r : auraBienHinhTheoItem) {
                if (r[0] == itemBienHinh) {
                    return r[1];
                }
            }
            if (effectSkill.levelBienHinh >= 4) {
                return 33;
            } else {
                return 34;
            }
        }
        if (inventory != null && inventory.itemsBody != null) {
            for (Item item : inventory.itemsBody) {
                if (item.isNotNullItem()) {
                    if (item.template.id == 1927) {
                        return 58;
                    }
                    if (item.template.id == 1586 || item.template.id == 1340) {
                        return 29;
                    }
                }
            }
        }
        if (!isPl() || this.Cards == null || this.Cards.isEmpty()) {
            return -1;
        }
        for (Card card : this.Cards) {
            if (card != null && card.Used == 1 && (card.Id == 956 || card.Id == 1276 || card.Id == 1277 || card.Id == 1278 || card.Id == 1550 || card.Id == 1591 || card.Id == 1592 || card.Id == 1593) && card.Level > 1) {
                RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream()
                        .filter(r -> r.Id == card.Id)
                        .findFirst()
                        .orElse(null);
                if (radarTemplate != null) {
                    return radarTemplate.AuraId;
                }
            }
        }
        return -1;
    }

    public byte getEffFront() {
        if (this.inventory == null) {
            return -1;
        }
        if (this.inventory.itemsBody.isEmpty() || this.inventory.itemsBody.size() < 10) {
            return -1;
        }
        int levelAo = 0;
        Item.ItemOption optionLevelAo = null;
        int levelQuan = 0;
        Item.ItemOption optionLevelQuan = null;
        int levelGang = 0;
        Item.ItemOption optionLevelGang = null;
        int levelGiay = 0;
        Item.ItemOption optionLevelGiay = null;
        int levelNhan = 0;
        Item.ItemOption optionLevelNhan = null;
        Item itemAo = this.inventory.itemsBody.get(0);
        Item itemQuan = this.inventory.itemsBody.get(1);
        Item itemGang = this.inventory.itemsBody.get(2);
        Item itemGiay = this.inventory.itemsBody.get(3);
        Item itemNhan = this.inventory.itemsBody.get(4);
        for (Item.ItemOption io : itemAo.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelAo = io.param;
                optionLevelAo = io;
                break;
            }
        }
        for (Item.ItemOption io : itemQuan.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelQuan = io.param;
                optionLevelQuan = io;
                break;
            }
        }
        for (Item.ItemOption io : itemGang.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelGang = io.param;
                optionLevelGang = io;
                break;
            }
        }
        for (Item.ItemOption io : itemGiay.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelGiay = io.param;
                optionLevelGiay = io;
                break;
            }
        }
        for (Item.ItemOption io : itemNhan.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelNhan = io.param;
                optionLevelNhan = io;
                break;
            }
        }
        if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 8 && levelQuan >= 8 && levelGang >= 8 && levelGiay >= 8 && levelNhan >= 8) {
            return 8;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 7 && levelQuan >= 7 && levelGang >= 7 && levelGiay >= 7 && levelNhan >= 7) {
            return 7;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 6 && levelQuan >= 6 && levelGang >= 6 && levelGiay >= 6 && levelNhan >= 6) {
            return 6;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 5 && levelQuan >= 5 && levelGang >= 5 && levelGiay >= 5 && levelNhan >= 5) {
            return 5;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 4 && levelQuan >= 4 && levelGang >= 4 && levelGiay >= 4 && levelNhan >= 4) {
            return 4;
        } else {
            return -1;
        }
    }

    public short getHead() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 454;
        }
        if (itemTime != null && itemTime.isXuong) {
            if (itemTime.iconXuong == 4082) {
                return 406;
            }
            return (short) (this.gender == 0 ? 545 : this.gender == 1 ? 547 : 546);
        }
//        if (effectSkill != null && effectSkill.isHalloween) {
//            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][0];
//        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        } else if (effectSkill != null && effectSkill.isBienHinh) {
            return outfitBienHinh(0);
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 412;
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION
                && (!this.isOffPorataOutfit || !isPorataFusion())) {
            if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                return idOutfitFusion[3 + this.gender][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                return idOutfitFusion[6 + this.gender][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA_BLUE) {
                return idOutfitFusion[9][0];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int headId = inventory.itemsBody.get(5).template.head;
            if (headId != -1) {
                return (short) headId;
            }
        }
        return this.head;
    }

    public boolean isPorataFusion() {
        return this.fusion != null && this.fusion.typeFusion != ConstPlayer.NON_FUSION
                && this.fusion.typeFusion != ConstPlayer.LUONG_LONG_NHAT_THE;
    }

    public int version() {
        if (session == null) {
            return 1;
        }

        return session.version;
    }

    public boolean isVersionAbove(int version) {
        if (session == null) {
            return false;
        }
        return version() >= version;
    }

    public short getBody() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 455;
        }
        if (itemTime != null && itemTime.isXuong) {
            if (itemTime.iconXuong == 4082) {
                return 407;
            }
            return 548;
        }
//        if (effectSkill != null && effectSkill.isHalloween) {
//            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][1];
//        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        } else if (effectSkill != null && effectSkill.isBienHinh) {
            return outfitBienHinh(1);
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 413;
        } else if (isPhuHoMapMabu && fusion != null && fusion.typeFusion == ConstPlayer.NON_FUSION) {
            return idOutfitGod[this.gender][1];
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION
                && (!this.isOffPorataOutfit || !isPorataFusion())) {
            if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                return idOutfitFusion[3 + this.gender][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                return idOutfitFusion[6 + this.gender][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA_BLUE) {
                return idOutfitFusion[9][1];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int body = inventory.itemsBody.get(5).template.body;
            if (body != -1) {
                return (short) body;
            }
        }
        if (inventory != null && inventory.itemsBody.get(0).isNotNullItem()) {
            return inventory.itemsBody.get(0).template.part;
        }
        return (short) (gender == ConstPlayer.NAMEC ? 59 : 57);
    }

    public short getLeg() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 456;
        }
        if (itemTime != null && itemTime.isXuong) {
            if (itemTime.iconXuong == 4082) {
                return 408;
            }
            return 549;
        }
//        if (effectSkill != null && effectSkill.isHalloween) {
//            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][2];
//        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        } else if (effectSkill != null && effectSkill.isBienHinh) {
            return outfitBienHinh(2);
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 414;
        } else if (isPhuHoMapMabu && fusion != null && fusion.typeFusion == ConstPlayer.NON_FUSION) {
            return idOutfitGod[this.gender][2];
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION
                && (!this.isOffPorataOutfit || !isPorataFusion())) {
            if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                return idOutfitFusion[3 + this.gender][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                return idOutfitFusion[6 + this.gender][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA_BLUE) {
                return idOutfitFusion[9][2];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int leg = inventory.itemsBody.get(5).template.leg;
            if (leg != -1) {
                return (short) leg;
            }
        }
        if (inventory != null && inventory.itemsBody.get(1).isNotNullItem()) {
            return inventory.itemsBody.get(1).template.part;
        }
        return (short) (gender == 1 ? 60 : 58);
    }

    public short getFlagBag() {
        if (this.idMark != null && this.idMark.isHoldBlackBall()) {
            return 31;
        } else if (this.idNRNM >= 353 && this.idNRNM <= 359) {
            return 30;
        }
        if (TaskService.gI().getIdTask(this) == ConstTask.TASK_3_2) {
            return 28;
        }
        if (this.inventory != null && this.inventory.itemsBody.size() > 8) {
            if (this.inventory.itemsBody.get(8).isNotNullItem()) {
                return this.inventory.itemsBody.get(8).template.part;
            }
        }
        if (this.clan != null) {
            return (short) this.clan.imgId;
        }
        return -1;
    }

    public short getMount() {
        if (isPl() && this.inventory != null) {
            Item item = this.inventory.itemsBody.get(9);
            if (item == null || !item.isNotNullItem()) {
                return -1;
            }
            if (item.template.type == 24) {
                if (item.template.gender == 3 || item.template.gender == this.gender) {
                    return item.template.id;
                } else {
                    return -1;
                }
            } else {
                if (item.template.id < 500) {
                    return item.template.id;
                } else {
                    return (short) (item.template.part + 30_000);
                }
            }
        } else {
            return -1;
        }
    }

    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (plAtt != null && !plAtt.equals(this)) {
                setTemporaryEnemies(plAtt);
            }
            if (plAtt != null && this.isPet && ((Pet) this).master.id == plAtt.id) {
                if (this.effectSkill != null && !this.effectSkill.isHalloween) {
                    // EffectSkillService.gI().setIsHalloween(this, -1, 1800000);
                }
            }
//            if (plAtt != null && !this.isBoss && plAtt.nPoint.xDameChuong && SkillUtil.isUseSkillChuong(plAtt)
//                    && SkillUtil.isUseSkillChuong1(plAtt)) {
//                damage = plAtt.nPoint.tlDameChuong * damage;
//                plAtt.nPoint.xDameChuong = false;
//            }
            if (plAtt != null && plAtt.playerSkill.skillSelect != null && !plAtt.isBoss && MapService.gI().isMapMaBu(this.zone.map.mapId)) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN ->
                        damage = damage > this.nPoint.hpMax / 20 ? this.nPoint.hpMax / 20 : damage;
                }
            }
            if (plAtt != null && plAtt.isBoss) {
                this.effectSkin.isVoHinh = false;
                this.effectSkin.lastTimeVoHinh = System.currentTimeMillis();
                if (!this.isBoss && damage > 0) {
                    damage = damage * 70 / 100;
                    if (damage < 1) {
                        damage = 1;
                    }
                }
            }
            if (plAtt != null && plAtt.effectSkill != null && plAtt.effectSkill.isBinh
                    && !Util.canDoWithTime(plAtt.effectSkill.lastTimeUpBinh, 3000)) {
                return 0;
            }
            if (plAtt != null && plAtt.isPl() && this.maBuHold != null && this.zone != null && this.zone.map.mapId == 128) {
                this.precentMabuHold++;
                damage = 1;
            }
            if (plAtt != null && this.nPoint.islinhthuydanhbac) {
                Service.gI().sendThongBao(plAtt, "Không thể tấn công! Vì người chơi này đã nạp lần đầu!");
                return 0;
            }
            if (plAtt != null && plAtt.idNRNM != -1 && (this.isBoss || this.isNewPet)) {
                return 1;
            }
            if (plAtt != null && (plAtt.idNRNM != -1 || this.idNRNM != -1) && plAtt.clan != null && this.clan != null && plAtt.clan == this.clan) {
                Service.gI().chatJustForMe(plAtt, this, "Ê cùng bang mà");
                return 0;
            }
            if (!Util.canDoWithTime(this.lastTimeRevived, 1500)) {
                return 0;
            }

            // Ghi nhận và xử lý sát thương trong Guru Guild War
            if (GuruGuildWarService.gI().isInWar(this)) {
                if (plAtt != null) {
                    GuruGuildWarService.gI().onPlayerDamage(plAtt, this, damage);
                }
                // Bang Chủ trong Guru War nhận giảm 70% sát thương từ Tự Sát và QCKK để cân bằng chiến thuật
                if (this.clan != null && this.clan.isLeader(this) && plAtt != null && plAtt.playerSkill != null && plAtt.playerSkill.skillSelect != null) {
                    int selSkill = plAtt.playerSkill.skillSelect.template.id;
                    if (selSkill == Skill.TU_SAT || selSkill == Skill.QUA_CAU_KENH_KHI) {
                        damage = damage * 30 / 100;
                    }
                }
            }

            if (plAtt != null && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                        if (this.nPoint.voHieuChuong > 0) {
                            PlayerService.gI().hoiPhuc(this, 0, (int) (damage * this.nPoint.voHieuChuong / 100));
                            return 0;
                        }
                    }
                }
            }

            int tlGiap = this.nPoint.tlGiap;
            int tlNeDon = this.nPoint.tlNeDon;

            if (plAtt != null && !isMobAttack && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.MAKANKOSAPPO, Skill.QUA_CAU_KENH_KHI ->
                        tlNeDon = 0;
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN, Skill.DICH_CHUYEN_TUC_THOI ->
                        tlNeDon -= plAtt.nPoint.tlchinhxac;
                    default ->
                        tlNeDon = 0;
                }

                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                        if (tlGiap - plAtt.nPoint.tlxgc >= 0) {
                            tlGiap -= plAtt.nPoint.tlxgc;
                        } else {
                            tlGiap = 0;
                        }
                    }
                    case Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN -> {
                        if (tlGiap - plAtt.nPoint.tlxgcc >= 0) {
                            tlGiap -= plAtt.nPoint.tlxgcc;
                        } else {
                            tlGiap = 0;
                        }
                    }
                }
            }

            if (piercing) {
                tlGiap = 0;
                tlNeDon = 0;
            }

            if (tlNeDon > 90) {
                tlNeDon = 90;
            }
            if (tlGiap > 86) {
                tlGiap = 86;
            }

            if (Util.isTrue(tlNeDon, 100)) {
                return 0;
            }

            damage -= ((damage / 100) * tlGiap);

            if (!piercing) {
                damage = this.nPoint.subDameInjureWithDeff(damage);
            }

            boolean isUseGX = false;
            if (!piercing && plAtt != null && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN, Skill.QUA_CAU_KENH_KHI, Skill.MAKANKOSAPPO, Skill.DICH_CHUYEN_TUC_THOI ->
                        isUseGX = true;
                }
            }
            if ((isUseGX || isMobAttack) && this.itemTime != null) {
                if (this.itemTime.isUseGiapXen && !this.itemTime.isUseGiapXen2) {
                    damage /= 2;
                }
                if (this.itemTime.isUseGiapXen2) {
                    damage = damage / 100 * 40;
                }
            }
            if (!piercing && effectSkill.isShielding && !isMobAttack) {
                if (this.idMark != null) {
                    this.idMark.setDamePST((int) Math.min(damage, 2_000_000_000L));
                }
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            damage = Math.min(damage, 2_000_000_000L); // Giới hạn damage không vượt quá 2 tỷ
            if (isMobAttack && charms.tdBatTu > System.currentTimeMillis() && damage >= nPoint.hp) {
                damage = nPoint.hp - 1;
            }
            if (!piercing && plAtt == null && isMobAttack && (this.charms.tdBatTu > System.currentTimeMillis() || this.effectSkill != null && this.effectSkill.isHalloween) && damage >= this.nPoint.hp) {
                damage = this.nPoint.hp - 1;
            }

            if (this.zone.map.mapId == 129) {
                if (damage >= this.nPoint.hp) {
                    this.lostByDeath = true;
                    The23rdMartialArtCongress mc = The23rdMartialArtCongressManager.gI().getMC(zone);
                    if (mc != null) {
                        mc.die();
                    }
                    return 0;
                }
            }
            if (this.zone.map.mapId == 51) {
                this.totalDamageTaken += damage;
            }
            this.nPoint.subHP(damage);
            if ((plAtt != null || isMobAttack) && isDie() && !isBoss && !isNewPet && !isNewPet1) {
                if (plAtt != null && this.isPl()) {
//                    TaskService.gI().checkDoneTaskPK(plAtt);
                    if (this.idMark != null && this.idMark.isHoldBlackBall()) {
                        //     TaskService.gI().checkDoneTaskNRSD(plAtt);
                    }
                }
                if (Util.isTrue(this.nPoint.tlBom, 100)) {
                    setBom(plAtt);
                } else {
                    setDie(plAtt);
                }
            }

            return damage;
        } else {
            return 0;
        }
    }

    public void setTemporaryEnemies(Player pl) {
        if (!temporaryEnemies.contains(pl)) {
            temporaryEnemies.add(pl);
        }
    }

    protected void setBom(Player plAtt) {
        setDie(plAtt);
    }

    public void setDie() {
        this.setDie(null);
    }

    protected void setDie(Player plAtt) {
        TaskService.gI().checkDoneTaskKillPlayer(plAtt);
        if (this.isPl()) {
            long vangtru = this.nPoint.power / 1000000;
            if (vangtru > 32000) {
                vangtru = 32000;
            }
            int vang = (int) vangtru - Util.nextInt(10, 100);

            if (this.inventory.gold >= vang && vang >= 1) {
                this.inventory.gold -= vang;
                Service.gI().sendMoney(this);
                vang = vang * 95 / 100;
                if (vang < 10000) {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 189, vang, this.location.x, this.location.y, this.id));
                } else if (vang < 20000) {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 188, vang, this.location.x, this.location.y, this.id));
                } else {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 190, vang, this.location.x, this.location.y, this.id));
                }
            }
        }

        if (this.effectSkin.xHPKI > 1) {
            this.effectSkin.xHPKI = 1;
            Service.gI().point(this);
        }
        if (this.effectSkin.xDame > 1) {
            this.effectSkin.xDame = 1;
            Service.gI().point(this);
        }
        this.playerSkill.prepareQCKK = false;
        this.playerSkill.prepareLaze = false;
        this.playerSkill.prepareTuSat = false;
        this.effectSkill.removeSkillEffectWhenDie();
        nPoint.setHp(0);
        nPoint.setMp(0);
        if (this.mobMe != null) {
            this.mobMe.mobMeDie();
            this.mobMe.dispose();
            this.mobMe = null;
        }
        Service.gI().charDie(this);
        if (!this.isPet && !this.isNewPet && !isBot && !this.isNewPet1 && !this.isBoss && plAtt != null && !plAtt.isPet && !plAtt.isNewPet && !plAtt.isNewPet1 && !plAtt.isBoss) {
            if (!plAtt.itemTime.isUseAnDanh) {
                FriendAndEnemyService.gI().addEnemy(this, plAtt);
            }
        }
        this.typePk = 0;
        if (this.pvp != null && this.zone.map.mapId != 140) {
            this.pvp.lose(this, TYPE_LOSE_PVP.DEAD);
        }
        BlackBallWarService.gI().dropBlackBall(this);
        NgocRongNamecService.gI().dropNamekBall(this);
        GuruGuildWarService.gI().onPlayerDie(this, plAtt);
    }

    public void setClanMember() {
        if (this.clanMember != null) {
            this.clanMember.powerPoint = this.nPoint.power;
            this.clanMember.head = this.getHead();
            this.clanMember.body = this.getBody();
            this.clanMember.leg = this.getLeg();
        }
    }

    public boolean isAdmin() {
        return this.session != null && this.session.isAdmin;
    }

    public void setJustRevivaled() {
        this.justRevived = true;
        this.lastTimeRevived = System.currentTimeMillis();
    }

    public boolean isActive() {
        return (this.isPl() && this.session != null && this.session.actived) || (this.isPet && ((Pet) this).master.session != null && ((Pet) this).master.session.actived);
    }

    public void sendNewPet() {
        if (isPl() && inventory != null && inventory.itemsBody.get(7) != null) {
            Item it = inventory.itemsBody.get(7);
            if (it != null && it.isNotNullItem() && newPet == null) {
                switch (it.template.id) {
                    case 942 -> {
                        PetService.Pet2(this, 966, 967, 968);
                        Service.gI().point(this);
                    }
                    case 943 -> {
                        PetService.Pet2(this, 969, 970, 971);
                        Service.gI().point(this);
                    }
                    case 944 -> {
                        PetService.Pet2(this, 972, 973, 974);
                        Service.gI().point(this);
                    }
                    case 967 -> {
                        PetService.Pet2(this, 1050, 1051, 1052);
                        Service.gI().point(this);
                    }
                    case 968 -> {
                        PetService.Pet2(this, 1183, 1184, 1185);
                        Service.gI().point(this);
                    }
                }
            }
        }
    }

    private void fixBlackBallWar() {
        int x = this.location.x;
        int y = this.location.y;
        switch (this.zone.map.mapId) {
            case 85, 86, 87, 88, 89, 90, 91 -> {
                if (this.isPl()) {
                    if (x < 24 || x > this.zone.map.mapWidth - 24 || y < 0 || y > this.zone.map.mapHeight - 24) {
                        if (MapService.gI().getWaypointPlayerIn(this) == null) {
                            Service.gI().resetPoint(this, x, this.zone.map.yPhysicInTop(this.location.x, 100));
                            this.nPoint.hp -= this.nPoint.hpMax / 10;
                            PlayerService.gI().sendInfoHp(this);
                            return;
                        }
                    }
                    int yTop = this.zone.map.yPhysicInTop(this.location.x, this.location.y);
                    if (yTop >= this.zone.map.mapHeight - 24) {
                        Service.gI().resetPoint(this, x, this.zone.map.yPhysicInTop(this.location.x, 100));
                        this.nPoint.hp -= this.nPoint.hpMax / 10;
                        PlayerService.gI().sendInfoHp(this);
                    }
                }
            }
        }
    }

    public void move(int _toX, int _toY) {
        if (_toX != this.location.x) {
            this.location.x = _toX;
        }
        if (_toY != this.location.y) {
            this.location.y = _toY;
        }
        MapService.gI().sendPlayerMove(this);
    }
    public volatile boolean disposed = false;

    public void dispose() {
        disposed = true;
        if (itemsTradeWVP != null) {
            if (!itemsTradeWVP.isEmpty()) {
                for (Item item : itemsTradeWVP) {
                    InventoryService.gI().addItemBag(this, item);
                }
            }
            itemsTradeWVP.clear();
            itemsTradeWVP = null;
        }
        if (pet != null) {
            pet.dispose();
            pet = null;
        }
        if (newPet != null) {
            newPet.dispose();
            newPet = null;
        }
        if (mapBlackBall != null) {
            mapBlackBall.clear();
            mapBlackBall = null;
        }
        zone = null;
        mapBeforeCapsule = null;
        if (mapMaBu != null) {
            mapMaBu.clear();
            mapMaBu = null;
        }
        if (billEgg != null) {
            billEgg.dispose();
            billEgg = null;
        }
        mapBeforeCapsule = null;
        if (mapCapsule != null) {
            mapCapsule.clear();
            mapCapsule = null;
        }
        if (mobMe != null) {
            mobMe.dispose();
            mobMe = null;
        }
        location = null;
        if (setClothes != null) {
            setClothes.dispose();
            setClothes = null;
        }
        if (effectSkill != null) {
            effectSkill.dispose();
            effectSkill = null;
        }
        if (mabuEgg != null) {
            mabuEgg.dispose();
            mabuEgg = null;
        }
        if (blackEgg != null) {
            blackEgg.dispose();
            blackEgg = null;
        }
        if (playerTask != null) {
            playerTask.dispose();
            playerTask = null;
        }
        if (itemTime != null) {
            itemTime.dispose();
            itemTime = null;
        }
        if (fusion != null) {
            fusion.dispose();
            fusion = null;
        }
        if (magicTree != null) {
            magicTree.dispose();
            magicTree = null;
        }
        if (playerIntrinsic != null) {
            playerIntrinsic.dispose();
            playerIntrinsic = null;
        }
        if (inventory != null) {
            inventory.dispose();
            inventory = null;
        }
        if (playerSkill != null) {
            playerSkill.dispose();
            playerSkill = null;
        }
        if (combineNew != null) {
            combineNew.dispose();
            combineNew = null;
        }
        if (idMark != null) {
            idMark.dispose();
            idMark = null;
        }
        if (charms != null) {
            charms.dispose();
            charms = null;
        }
        if (effectSkin != null) {
            effectSkin.dispose();
            effectSkin = null;
        }
        if (nPoint != null) {
            nPoint.dispose();
            nPoint = null;
        }
        if (rewardBlackBall != null) {
            rewardBlackBall.dispose();
            rewardBlackBall = null;
        }
        if (pvp != null) {
            pvp.dispose();
            pvp = null;
        }
        if (superRank != null) {
            superRank.dispose();
            superRank = null;
        }
        if (dropItem != null) {
            dropItem.dispose();
            dropItem = null;
        }
        if (satellite != null) {
            satellite = null;
        }
        if (achievement != null) {
            achievement.dispose();
            achievement = null;
        }
        if (giftCode != null) {
            giftCode.dispose();
            giftCode = null;
        }
        if (traning != null) {
            traning = null;
        }
        if (mapCapsule != null) {
            mapCapsule.clear();
            mapCapsule = null;
        }
        if (Cards != null) {
            Cards.clear();
            Cards = null;
        }
        if (itemsWoodChest != null) {
            itemsWoodChest.clear();
            itemsWoodChest = null;
        }
        if (friends != null) {
            friends.clear();
            friends = null;
        }
        if (enemies != null) {
            enemies.clear();
            enemies = null;
        }
        if (temporaryEnemies != null) {
            temporaryEnemies.clear();
            temporaryEnemies = null;
        }
        itemsWoodChest = null;
        Cards = null;
        itemEvent = null;
        maBu2H = null;
        maBuHold = null;
        zoneSieuThanhThuy = null;
        thongBaoTapTuDong = null;
        notify = null;
        clan = null;
        clanMember = null;
        friends = null;
        enemies = null;
        session = null;
        newSkill = null;
        name = null;
        textThongBaoChangeMap = null;
        textThongBaoThua = null;
    }

    public String percentRuby(int type) {
        try {
            if (type == 0) {
                double denominator2 = ChonAiDay_Ruby.gI().rubyNormar;
                if (denominator2 != 0) {
                    double percent = ((double) this.rubyNormar / denominator2) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            } else if (type == 1) {
                double denominator2 = ChonAiDay_Ruby.gI().rubyVip;
                if (denominator2 != 0) {
                    double percent = ((double) this.rubyVIP / denominator2) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            }
        } catch (ArithmeticException e) {
            return "0";
        }
        return "0";
    }

    public String percentGold(int type) {
        try {
            if (type == 0) {
                double denominator = ChonAiDay_Gold.gI().goldNormar;
                if (denominator != 0) {
                    double percent = ((double) this.goldNormar / denominator) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            } else if (type == 1) {
                double denominator = ChonAiDay_Gold.gI().goldVip;
                if (denominator != 0) {
                    double percent = ((double) this.goldVIP / denominator) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            }
        } catch (ArithmeticException e) {
            return "0";
        }
        return "0";
    }

    public String percentGem(int type) {
        try {
            ChonAiDay_Gem gemInstance = ChonAiDay_Gem.gI();
            if (gemInstance == null) {
                return "0";
            }
            switch (type) {
                case 0 -> {
                    double denominator3 = gemInstance.gemNormar;
                    if (denominator3 != 0) {
                        double percent = ((double) this.gemNormar / denominator3) * 100;
                        return String.valueOf(Math.ceil(percent));
                    } else {
                        return "0";
                    }
                }
                case 1 -> {
                    double denominator3 = gemInstance.gemVip;
                    if (denominator3 != 0) {
                        double percent = ((double) this.gemVIP / denominator3) * 100;
                        return String.valueOf(Math.ceil(percent));
                    } else {
                        return "0";
                    }
                }
                default -> {
                    return "0";
                }
            }
        } catch (ArithmeticException | NullPointerException e) {
            return "0";
        }
    }

    public short getHeadThuCung() {
        if (this.isPl() && this.inventory != null && this.inventory.itemsBody.size() > 7
                && this.inventory.itemsBody.get(7).isNotNullItem()) {
            return (short) (this.inventory.itemsBody.get(7).template.head);
        }
        return -1;
    }

    public short getBodyThuCung() {
        if (this.isPl() && this.inventory != null && this.inventory.itemsBody.size() > 7
                && this.inventory.itemsBody.get(7).isNotNullItem()) {
            return (short) (this.inventory.itemsBody.get(7).template.body);
        }
        return -1;
    }

    public short getLegThuCung() {
        if (this.isPl() && this.inventory != null && this.inventory.itemsBody.size() > 7
                && this.inventory.itemsBody.get(7).isNotNullItem()) {
            return (short) (this.inventory.itemsBody.get(7).template.leg);
        }
        return -1;
    }

    public void goToXY(int safeX, int safeY, boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean canCastSkill(long cooldown) {
        if (isCastingSkill && !Util.canDoWithTime(lastTimeCasting, cooldown)) {
            return false;
        }
        isCastingSkill = true;
        lastTimeCasting = System.currentTimeMillis();
        return true;
    }

    public void resetCasting() {
        isCastingSkill = false;
    }

    /**
     * Kiểm tra và trừ hạn sử dụng / xóa item hết hạn (option 93) trong danh sách items cố định (body, bag, box).
     */
    private boolean checkAndRemoveExpiredItems(java.util.List<Item> items) {
        if (items == null) return false;
        boolean changed = false;
        for (int i = 0, sz = items.size(); i < sz; i++) {
            Item item = items.get(i);
            if (item == null || item.template == null || !item.isHaveOption(93)) {
                continue;
            }
            int oldDays = item.getOptionParam(93);
            if (ItemService.gI().isOutOfDateTime(item)) {
                String name = item.template.name;
                items.set(i, ItemService.gI().createItemNull());
                Service.gI().sendThongBao(this, name + " đã hết hạn sử dụng.");
                changed = true;
            } else if (item.getOptionParam(93) != oldDays) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Kiểm tra và xóa item hết hạn trong rương phụ (itemsBoxCrackBall - danh sách động).
     */
    private boolean checkAndRemoveExpiredItemsCrackBall(java.util.List<Item> items) {
        if (items == null) return false;
        boolean changed = false;
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (item == null || item.template == null) {
                items.remove(i);
                changed = true;
                continue;
            }
            if (!item.isHaveOption(93)) {
                continue;
            }
            int oldDays = item.getOptionParam(93);
            if (ItemService.gI().isOutOfDateTime(item)) {
                String name = item.template.name;
                items.remove(i);
                Service.gI().sendThongBao(this, name + " (rương phụ) đã hết hạn sử dụng.");
                changed = true;
            } else if (item.getOptionParam(93) != oldDays) {
                changed = true;
            }
        }
        return changed;
    }

}