package Bot;

import consts.ConstPlayer;
import java.util.Random;
import models.Template.SkillTemplate;
import models.map.Map;
import models.map.Zone;
import models.player.Player;
import models.skill.NClass;
import models.skill.Skill;
import server.Manager;
import services.EffectSkillService;
import services.Service;
import services.SkillService;
import services.map.ChangeMapService;
import services.map.MapService;
import services.player.PlayerService;
import utils.Util;

public class Bot extends Player {

    private short head_;
    private short body_;
    private short leg_;
    private short flag_;
    public int type;
    private int index_ = 0;
    public Sanb boss;
    public Mobb mo1;
    public BotNappa botnappa;
    public BotTuonglai bottuonglai;
    public BotCold1 botcold;
    /** Bộ não "như người chơi thật" — dùng cho {@link #TYPE_NHU_PLAYER}. */
    public BotBrain brain;
    /** Cấu hình lúc tạo (hành vi: nuôi đệ tử / phá player / chat). Có thể null. */
    public BotConfig cfg;

    /** Loại bot hành xử như người chơi thật: train + đệ tử + boss + PK + chat. */
    public static final int TYPE_NHU_PLAYER = 5;


    private int[] TraiDat = new int[]{1, 2, 3, 4, 6, 29, 30, 28, 27};
    private int[] Namec = new int[]{8, 9, 10, 11, 12, 13, 33, 34, 32, 31};
    private int[] XayDa = new int[]{15, 16, 17, 18, 19, 20, 37, 36, 35, 52};

    private int[] Nappa = new int[]{63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83};
    private int[] tuonglai = new int[]{92, 93, 94, 96, 97, 98, 99, 100};
    private int[] cold = new int[]{105, 106, 107, 108, 109, 110};

    /**
     * Id bot lấy từ dải riêng, tăng dần. Bản cũ dùng số ngẫu nhiên trong 0..2 tỉ nên
     * có thể trùng id người chơi thật (hoặc trùng nhau) — client phân biệt nhân vật
     * theo id, trùng là vẽ nhầm/đánh nhầm. Dải này nằm trên mọi id trong DB và vẫn
     * lọt trong phạm vi int mà gói tin dùng.
     */
    private static final java.util.concurrent.atomic.AtomicInteger ID_BOT =
            new java.util.concurrent.atomic.AtomicInteger(1_500_000_000);

    public Bot(short head, short body, short leg, int type, String name, short flag) {
        this.head_ = head;
        this.body_ = body;
        this.leg_ = leg;
        this.name = name;
        this.id = ID_BOT.getAndIncrement();
        this.type = type;
        this.isBot = true;
        this.flag_ = flag;
    }

    public int MapToPow() {
        Random random = new Random();
        long power = this.nPoint.power;
        int mapId = 21;
        if (power < 20000000) {
            if (this.gender == 0) {
                mapId = TraiDat[random.nextInt(TraiDat.length)];
            } else if (this.gender == 1) {
                mapId = Namec[random.nextInt(Namec.length)];
            } else {
                mapId = XayDa[random.nextInt(XayDa.length)];
            }
        } else if (power < 100000000) {
            mapId = 62 + random.nextInt(15);
        } else if (power < 1000000000) {
            if (Util.isTrue(30, 100)) {
                mapId = 91 + random.nextInt(3);
            } else if (Util.isTrue(30, 100)) {
                mapId = 95 + random.nextInt(5);
            } else {
                mapId = 102 + random.nextInt(2);
            }
        } else {
            if (Util.isTrue(30, 100)) {
                mapId = 104 + random.nextInt(6);
            } else if (Util.isTrue(30, 100)) {
                mapId = 173 + random.nextInt(3);
            } else {
                mapId = 157 + random.nextInt(2);
            }
        }
        return mapId;
    }

    public void joinMap() {
        Zone zone = getRandomZone(MapToPow());
        if (zone != null) {
            ChangeMapService.gI().goToMap(this, zone);
            datViTriNgauNhien();
            this.zone.load_Me_To_Another(this);
            this.mo1.lastTimeChanM = System.currentTimeMillis();
        }
    }

    /**
     * Đặt bot xuống một chỗ ngẫu nhiên trên mặt đất của khu.
     *
     * <p>goToMap KHÔNG gán toạ độ (người chơi thật do client tự báo vị trí lên), nên
     * bot vào map là nằm ở gốc 0,0 — trôi ra ngoài bản đồ, người chơi không thấy đâu.
     */
    public void datViTriNgauNhien() {
        try {
            if (this.zone == null || this.zone.map == null || this.location == null) {
                return;
            }
            int rong = this.zone.map.mapWidth;
            if (rong <= 100) {
                return;
            }
            // yPhysicInTop trả 0 khi cột đó không có nền đất -> thử vài chỗ khác,
            // đứng ở y=0 là lơ lửng trên đỉnh bản đồ.
            for (int lan = 0; lan < 10; lan++) {
                int x = Util.nextInt(50, rong - 50);
                int y = this.zone.map.yPhysicInTop(x, 0);
                if (y > 0) {
                    this.location.x = x;
                    this.location.y = y;
                    return;
                }
            }
            this.location.x = rong / 2;
            this.location.y = Math.max(1, this.zone.map.mapHeight / 2);
        } catch (Exception ignored) {
        }
    }

    public void joinMapNappa() {
        Zone zone = getRandomZone(MapToPowNappa());
        if (zone != null) {
            ChangeMapService.gI().goToMap(this, zone);
            datViTriNgauNhien();
            this.zone.load_Me_To_Another(this);
            this.botnappa.lastTimeChanM = System.currentTimeMillis();
        }
    }

    public void joinMapTuonglai() {
        Zone zone = getRandomZone(MapToPowTuonglai());
        if (zone != null) {
            ChangeMapService.gI().goToMap(this, zone);
            datViTriNgauNhien();
            this.zone.load_Me_To_Another(this);
            this.bottuonglai.lastTimeChanM = System.currentTimeMillis();
        }
    }

    public void joinMapCold() {
        Zone zone = getRandomZone(MapToPowCold());
        if (zone != null) {
            ChangeMapService.gI().goToMap(this, zone);
            datViTriNgauNhien();
            this.zone.load_Me_To_Another(this);
            this.botcold.lastTimeChanM = System.currentTimeMillis();
        }
    }

    public int MapToPowCold() {
        Random random = new Random();
        long power = this.nPoint.power;
        int mapId = 21;
        if (power < 20000000) {
            if (this.gender == 0) {
                mapId = cold[random.nextInt(cold.length)];
            } else if (this.gender == 1) {
                mapId = cold[random.nextInt(cold.length)];
            } else {
                mapId = cold[random.nextInt(cold.length)];
            }
        } else if (power < 100000000) {
            mapId = 62 + random.nextInt(15);
        } else if (power < 1000000000) {
            if (Util.isTrue(30, 100)) {
                mapId = 91 + random.nextInt(3);
            } else if (Util.isTrue(30, 100)) {
                mapId = 95 + random.nextInt(5);
            } else {
                mapId = 102 + random.nextInt(2);
            }
        } else {
            if (Util.isTrue(30, 100)) {
                mapId = 104 + random.nextInt(6);
            } else if (Util.isTrue(30, 100)) {
                mapId = 173 + random.nextInt(3);
            } else {
                mapId = 157 + random.nextInt(2);
            }
        }
        return mapId;
    }

    public int MapToPowTuonglai() {
        Random random = new Random();
        long power = this.nPoint.power;
        int mapId = 21;
        if (power < 20000000) {
            if (this.gender == 0) {
                mapId = tuonglai[random.nextInt(tuonglai.length)];
            } else if (this.gender == 1) {
                mapId = tuonglai[random.nextInt(tuonglai.length)];
            } else {
                mapId = tuonglai[random.nextInt(tuonglai.length)];
            }
        } else if (power < 100000000) {
            mapId = 62 + random.nextInt(15);
        } else if (power < 1000000000) {
            if (Util.isTrue(30, 100)) {
                mapId = 91 + random.nextInt(3);
            } else if (Util.isTrue(30, 100)) {
                mapId = 95 + random.nextInt(5);
            } else {
                mapId = 102 + random.nextInt(2);
            }
        } else {
            if (Util.isTrue(30, 100)) {
                mapId = 104 + random.nextInt(6);
            } else if (Util.isTrue(30, 100)) {
                mapId = 173 + random.nextInt(3);
            } else {
                mapId = 157 + random.nextInt(2);
            }
        }
        return mapId;
    }

    public int MapToPowNappa() {
        Random random = new Random();
        long power = this.nPoint.power;
        int mapId = 21;
        if (power < 20000000) {
            if (this.gender == 0) {
                mapId = Nappa[random.nextInt(Nappa.length)];
            } else if (this.gender == 1) {
                mapId = Nappa[random.nextInt(Nappa.length)];
            } else {
                mapId = Nappa[random.nextInt(Nappa.length)];
            }
        } else if (power < 100000000) {
            mapId = 62 + random.nextInt(15);
        } else if (power < 1000000000) {
            if (Util.isTrue(30, 100)) {
                mapId = 91 + random.nextInt(3);
            } else if (Util.isTrue(30, 100)) {
                mapId = 95 + random.nextInt(5);
            } else {
                mapId = 102 + random.nextInt(2);
            }
        } else {
            if (Util.isTrue(30, 100)) {
                mapId = 104 + random.nextInt(6);
            } else if (Util.isTrue(30, 100)) {
                mapId = 173 + random.nextInt(3);
            } else {
                mapId = 157 + random.nextInt(2);
            }
        }
        return mapId;
    }

    public Zone getRandomZone(int mapId) {
        Map map = MapService.gI().getMapById(mapId);
        Zone zone = null;
        try {
            if (map != null) {
                zone = map.zones.stream()
                        .filter(z -> z.getNumOfPlayers() == 0)
                        .findFirst()
                        .orElseGet(() -> {
                            Zone randomZone = map.zones.get(Util.nextInt(0, map.zones.size() - 1));
                            return randomZone.isFullPlayer() ? null : randomZone;
                        });
            }
        } catch (Exception e) {
        }
        if (zone != null) {
            this.index_ = 0;
            return zone;
        } else {
            this.index_ += 1;
            if (this.index_ >= 20) {
                BotManager.gI().xoaBot(this); // xoaBot da tu exitMap + go de tu
                return null;
            } else {
                return getRandomZone(MapToPow());
            }
        }
    }
    @Override
    public short getHead() {
        if (effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        } else {
            return this.head_;
        }
    }

    @Override
    public short getBody() {
        if (effectSkill.isMonkey) {
            return 193;
        } else {
            return this.body_;
        }
    }

    @Override
    public short getLeg() {
        if (effectSkill.isMonkey) {
            return 194;
        } else {
            return this.leg_;
        }
    }

    @Override
    public short getFlagBag() {
        return this.flag_;
    }

    /**
     * Player.getMount() chặn ở {@code isPl()} nên bot không bao giờ hiện ván bay.
     * Đọc thẳng ô 9 giống hệt logic gốc để người chơi thấy bot cưỡi ván như mình.
     */
    @Override
    public short getMount() {
        try {
            if (this.inventory == null || this.inventory.itemsBody.size() <= 9) {
                return -1;
            }
            models.item.Item item = this.inventory.itemsBody.get(9);
            if (item == null || !item.isNotNullItem()) {
                return -1;
            }
            if (item.template.type == 24) {
                return (item.template.gender == 3 || item.template.gender == this.gender)
                        ? item.template.id : -1;
            }
            return item.template.id < 500 ? item.template.id : (short) (item.template.part + 30_000);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void update() {
        super.update();
        this.increasePoint();
        switch (this.type) {
            case 0:
                this.mo1.update();
                break;
            case 1:
                this.botnappa.update();
                break;
            case 2:
                this.bottuonglai.update();
                break;
             case 3:
                this.botcold.update();
                break;   
            case 4:
                this.boss.update();
                break;
            case TYPE_NHU_PLAYER:
                if (this.brain != null) {
                    this.brain.update();
                }
                break;
        }
        if (this.isDie()) {
            Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }
    }

    public void leakSkill() {
        for (NClass n : Manager.gI().NCLASS) {
            if (n.classId == this.gender) {
                for (SkillTemplate Template : n.skillTemplatess) {
                    for (Skill skills : Template.skillss) {
                        Skill cloneSkill = new Skill(skills);
                        this.playerSkill.skills.add(cloneSkill);
                        break;
                    }
                }
                break;
            }
        }
    }

    public boolean UseLastTimeSkill() {
        if (this.playerSkill.skillSelect.lastTimeUseThisSkillbot < (System.currentTimeMillis() - this.playerSkill.skillSelect.coolDown)) {
            this.playerSkill.skillSelect.lastTimeUseThisSkillbot = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }

    private long lastCongChiSo;

    private void increasePoint() {
        // Nhịp update là 150ms; cộng chỉ số 6-7 lần/giây cho cả nghìn bot là phí, mỗi
        // lần còn dựng một gói tin. Mỗi giây một lần là đủ giống người chơi.
        if (System.currentTimeMillis() - lastCongChiSo < 1000) {
            return;
        }
        lastCongChiSo = System.currentTimeMillis();
        long tiemNangUse = 0;
        int point = 0;
        if (this.nPoint != null) {
            if (Util.isTrue(50, 100)) {
                point = 100;
                int pointHp = point * 20;
                tiemNangUse = point * (2 * (this.nPoint.hpg + 1000) + pointHp - 20) / 2;
                if (doUseTiemNang(tiemNangUse)) {
                    this.nPoint.hpMax += point;
                    this.nPoint.hpg += point;
                    Service.gI().point(this);
                }
            } else {
                point = 10;
                tiemNangUse = point * (2 * this.nPoint.dameg + point - 1) / 2 * 100;
                if (doUseTiemNang(tiemNangUse)) {
                    this.nPoint.dameg += point;
                    Service.gI().point(this);
                }
            }
        }
    }

    private boolean doUseTiemNang(long tiemNang) {
        if (this.nPoint.tiemNang < tiemNang) {
            return false;
        } else {
            this.nPoint.tiemNang -= tiemNang;
            return true;
        }
    }

    public void useSkill(int skillId) {
        new Thread(() -> {
            switch (skillId) {
                case Skill.BIEN_KHI:
                    EffectSkillService.gI().sendEffectMonkey(this);
                    EffectSkillService.gI().setIsMonkey(this);
                    EffectSkillService.gI().sendEffectMonkey(this);

                    Service.gI().sendSpeedPlayer(this, 0);
                    Service.gI().Send_Caitrang(this);
                    Service.gI().sendSpeedPlayer(this, -1);
                    PlayerService.gI().sendInfoHpMp(this);
                    Service.gI().point(this);
                    Service.gI().Send_Info_NV(this);
                    Service.gI().sendInfoPlayerEatPea(this);
                    break;
                case Skill.QUA_CAU_KENH_KHI:
                    this.playerSkill.prepareQCKK = !this.playerSkill.prepareQCKK;
                    this.playerSkill.lastTimePrepareQCKK = System.currentTimeMillis();
                    SkillService.gI().sendPlayerPrepareSkill(this, 1000);
                    break;
                case Skill.MAKANKOSAPPO:
                    this.playerSkill.prepareLaze = !this.playerSkill.prepareLaze;
                    this.playerSkill.lastTimePrepareLaze = System.currentTimeMillis();
                    SkillService.gI().sendPlayerPrepareSkill(this, 3000);
                    break;
            }
        }).start();
    }
}
