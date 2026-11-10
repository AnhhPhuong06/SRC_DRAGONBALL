package models.boss;

import consts.BossID;
import consts.ConstPlayer;
import java.util.ArrayList;
import java.util.List;
import models.item.Item;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import models.skill.Skill;
import server.Client;
import services.Service;
import services.SkillService;
import services.map.ChangeMapService;
import services.map.MapService;
import services.player.InventoryService;
import utils.Util;

public class AnTrom extends Boss {

    private long lastTimeAnTrom;
    private long lastTimeJoinMap;
    private long goldAnTrom;
    private static final long TIME_CHANGE_MAP = 30000000;
    private List<Item> ITEM;
    private static final int ITEM_TO_STEAL_ID = 457;
    private long lastTimeAttack;
    private long lastTimeTargetPlayer;
    private int timeTargetPlayer;
    private int stolenItemCount;
    private boolean movedAway;
    private long moveAwayTime;
    private static final long TIME_MOVE_AWAY = 1500;
    private static final long TIME_RETURN = 5000;

    private long moveAwayStartTime = 0;
    private boolean isMovingAway = false;

    public AnTrom() throws Exception {
        super(BossID.AN_TROM, new BossData(
                "Ăn trộm " + Util.nextInt(50, 100),
                ConstPlayer.TRAI_DAT,
                new short[]{201, 202, 203, -1, -1, -1},
                1,
                new long[]{100},
                new int[]{3, 4, 5, 6, 27, 28, 29, 30,
                    9, 11, 12, 13, 10, 34, 33, 32, 31,
                    16, 17, 18, 19, 20, 37, 38, 36, 35,
                    24, 25, 26},
                new int[][]{
                    {Skill.THAI_DUONG_HA_SAN, 3, 20000}},
                new String[]{"|-1|Tới giờ làm việc, lụm lụm", "|-1|Cảm giác mình vào phải khu người nghèo :))"},
                new String[]{"|-1|Ái chà vàng vàng", "|-1|Không làm vẫn có ăn :))", "|-2|Giám ăn trộm giữa ban ngày thế à", "|-2|Cút ngay không là ăn đòn"},
                new String[]{"|-1|Híc lần sau ta sẽ cho ngươi phá sản",
                    "|-2|Chừa thói ăn trộm nghe chưa"},
                300
        ));
        this.ITEM = new ArrayList<>();
        lastTimeJoinMap = System.currentTimeMillis() + TIME_CHANGE_MAP;
        this.moveAwayTime = 0;
        this.movedAway = false;
    }

    @Override
    public Zone getMapJoin() {
        int mapId = this.data[this.currentLevel].getMapJoin()[Util.nextInt(0, this.data[this.currentLevel].getMapJoin().length - 1)];
        return MapService.gI().getMapById(mapId).zones.get(0);
    }

    @Override
    public Player getPlayerAttack() {
        return super.getPlayerAttack();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage = 1;
            moveAwayFromPlayer(plAtt);
            this.movedAway = true;
            moveAwayTime = System.currentTimeMillis();
            if (this.isDie()) {
                reward(plAtt);
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
            SkillService.gI().useSkill(this, plAtt, mobMe, -1, null);
            return damage;
        } else {
            return 0;
        }
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                if (Util.getDistance(this, pl) <= 40) {
                    if (!Util.canDoWithTime(this.lastTimeAnTrom, 500) || goldAnTrom > 10_000_000_000L) {
                        return;
                    }
                    int gold = 0;
                    if (pl.inventory.gold >= 10000000) {
                        gold = Util.nextInt(50000, 100000);
                    } else if (pl.inventory.gold >= 100000000) {
                        gold = Util.nextInt(400000, 500000);
                    } else if (pl.inventory.gold < 5000000) {
                        this.chat("Nghèo thế");
                        gold = 0;
                    }
                    this.chat("Trộm được " + Util.numberToMoney(gold) + " Vàng rồi!");
                    if (gold > 0) {
                        pl.inventory.gold -= gold;
                        goldAnTrom += gold;
                        Service.gI().stealMoney(pl, -gold);
                        this.stolenItemCount += gold;
                        this.lastTimeAnTrom = System.currentTimeMillis();
                        ItemMap itemMap = new ItemMap(this.zone, 190, gold, (this.location.x + pl.location.x) / 2, this.location.y, this.id);
                        Service.gI().dropItemMap(this.zone, itemMap);
                        Service.gI().sendToAntherMePickItem(this, itemMap.itemMapId);
                        this.zone.removeItemMap(itemMap);
                        this.lastTimeAnTrom = System.currentTimeMillis();
                    }
                } else {
                    if (this.movedAway && System.currentTimeMillis() - moveAwayTime > TIME_RETURN) {
                        this.moveToPlayer(pl);
                        this.movedAway = false;
                    } else if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    private void moveAwayFromPlayer(Player player) {
        int safeDistance = 600;
        int currentDistance = Util.getDistance(this, player);
        if (currentDistance > safeDistance && isMovingAway) {
            return;
        }
        int dx = this.location.x - player.location.x;
        int dy = this.location.y - player.location.y;
        double angle = Math.atan2(dy, dx);
        int moveSpeed = 180;
        int newX = this.location.x + (int) (moveSpeed * Math.cos(angle));
        int newY = this.location.y + (int) (moveSpeed * Math.sin(angle));
        this.moveTo(newX, newY);
        if (!isMovingAway) {
            moveAwayStartTime = System.currentTimeMillis();
            isMovingAway = true;
        }
    }

    private void returnToOriginalPosition() {
        long moveAwayDuration = System.currentTimeMillis() - moveAwayStartTime;
        if (moveAwayDuration >= 60000) {
            this.moveTo(this.location.x, this.location.y);
            isMovingAway = false;
        }
    }

    @Override
    public void reward(Player plKill) {
        models.task.Task_Kol.tick(plKill, 1, 12); // KOL 12 (tiêu diệt 10 Ăn Trộm)
        if (this.stolenItemCount > 0) {
            for (int i = -10; i <= 100; i += 10) {
                int goldForThisItem = this.stolenItemCount / 15;
                ItemMap item = new ItemMap(this.zone, 190, goldForThisItem, this.location.x + i, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
                if (item.itemTemplate != null) {
                    Service.gI().dropItemMap(this.zone, item);
                }
            }
            this.ITEM.clear();
            this.stolenItemCount = 0;
        }
        if (plKill.playerTask.taskdh.AnTrom < 30) {
            int percentDone = (int) ((double) plKill.playerTask.taskdh.AnTrom / 30 * 100);
            plKill.playerTask.taskdh.AnTrom++;
            plKill.playerTask.taskdh.ResetTime = System.currentTimeMillis();
            Service.gI().sendThongBao(plKill, "Tiến độ hiện tại: " + percentDone + "%");
        }
    }

    @Override
    public void joinMap() {
        super.joinMap();
    }

    @Override
    public void update() {
        super.update();
        if (isMovingAway) {
            returnToOriginalPosition();
        }
        if (this.zone != null) {
            List<Player> players = Client.gI().getPlayers();
            int playerCount = players.size();
            int[] mapCoTheVao = this.data[this.currentLevel].getMapJoin();
            int randomMapId = mapCoTheVao[Util.nextInt(0, mapCoTheVao.length - 1)];
            if (playerCount > 0) {
                Player ramdonPlayer = players.get(Util.nextInt(playerCount));
                if (ramdonPlayer != null && ramdonPlayer.zone != null && ramdonPlayer.zone.isKhongCoTrongTaiTrongKhu()) {
                    if (ramdonPlayer.zone.map.mapId != 51 && ramdonPlayer.zone.map.mapId != 113 && ramdonPlayer.zone.map.mapId != 129
                            && this.zone.getPlayers().size() <= 0 && System.currentTimeMillis() > this.lastTimeJoinMap) {

                        if (ramdonPlayer.id != -1000000) {
                            lastTimeJoinMap = System.currentTimeMillis() + TIME_CHANGE_MAP;
                            ChangeMapService.gI().spaceShipArrive(this, (byte) 2, ChangeMapService.DEFAULT_SPACE_SHIP);
                            ChangeMapService.gI().exitMap(this);
                            this.zone = MapService.gI().getMapById(randomMapId).zones.get(0);
                            this.location.x = Util.nextInt(Math.max(100, zone.map.mapWidth - 100));
                            this.location.y = zone.map.yPhysicInTop(this.location.x, 100);
                            this.joinMap();
                        }

                        if (this.zone != null) {
                            if (this.playerTarger == null || Util.canDoWithTime(this.lastTimeTargetPlayer, this.timeTargetPlayer)) {
                                this.playerTarger = this.zone.getRandomPlayerInMap();
                                if (this.playerTarger == null) {
                                }
                                this.lastTimeTargetPlayer = System.currentTimeMillis();
                                this.timeTargetPlayer = Util.nextInt(5000, 10000);
                            }
                        }
                    }
                }
            }
        }
    }
}
