package models.mob.bigboss_list;

/*
 * @Author: 0789305260
 *
 */
import models.item.Item;
import java.util.ArrayList;
import java.util.List;
import models.map.ItemMap;
import models.mob.BigBoss;
import models.mob.Mob;
import network.io.Message;
import models.player.Player;
import models.player.Pet;
import services.Service;
import utils.Logger;
import utils.Util;

public class Hirudegarn extends BigBoss {

    private int errors;
    private Player lastAttacker;
    private long PlayerID;

    public Hirudegarn(Mob mob) {
        super(mob);
    }

    @Override
    public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
        damage = this.point.hp / 100 > 0 ? this.point.hp / 100 : 1;

        if (plAtt != null) {
            this.lastAttacker = plAtt;
            this.PlayerID = plAtt.id;
        }
        super.injured(plAtt, damage, false);
    }

    @Override
    public void update() {
        if (isDie() && (System.currentTimeMillis() - lastTimeDie) > 10000 && lvMob == 3) {
            lvMob = 0;
            action = 0;
            this.status = 5; // Set alive
            this.location.x = Util.nextInt(100, 900);
            this.location.y = 360;
            this.point.hp = this.point.getHpFull();
            Service.gI().sendBigBoss2(this.zone, action, this);
            Message msg = null;
            try {
                msg = new Message(-9);
                msg.writer().writeByte(this.id);
                msg.writer().writeLong(this.point.gethp());
                msg.writer().writeInt(1);
                Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            } catch (Exception e) {
                Logger.logException(Hirudegarn.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } else if (isDie() && (System.currentTimeMillis() - lastTimeDie) > 5000 && lvMob <= 2) {
            switch (lvMob) {
                case 0 -> {
                    lvMob = 1;
                    action = 6;
                    this.point.hp = this.point.getHpFull();
                    this.status = 5;
                }
                case 1 -> {
                    lvMob = 2;
                    action = 5;
                    this.point.hp = this.point.getHpFull();
                    this.status = 5;
                }
                case 2 -> {
                    lvMob = 3;
                    action = 9;
                    this.point.hp = this.point.getHpFull();
                    this.status = 5;
                }
                default -> {
                }
            }

            int trai = 0;
            int phai = 1;
            int next = 0;
            for (int i = 0; i < 30; i++) {
                int X = next == 0 ? -5 * trai : 5 * phai;
                if (next == 0) {
                    trai++;
                } else {
                    phai++;
                }
                next = next == 0 ? 1 : 0;
                if (trai > 10) {
                    trai = 0;
                }
                if (phai > 10) {
                    phai = 1;
                }
                Service.gI().dropItemMap(
                        this.zone,
                        new ItemMap(zone, 190, 32000, this.location.x + X, this.location.y, -1));
            }
            if (Util.isTrue(80, 100)) {
                ItemMap it = new ItemMap(this.zone, 568, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                        this.location.y - 24), this.lastAttacker != null ? this.lastAttacker.id : -1);
                Service.gI().dropItemMap(this.zone, it);
            }
            boolean CheckPet = false;
            try {
                if (this.zone != null && this.zone.getPlayers() != null) {
                    for (Player player : this.zone.getPlayers()) {
                        if (player != null && player.pet != null
                                && // de tu mabu tren 40 B
                                player.pet.typePet == 1 && player.pet.nPoint.power >= 40_000_000_000L) {
                            CheckPet = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                CheckPet = false;
            }           
            if (Util.isTrue(0, 100)) {
                short[] possibleIds = {555, 557, 559};
                short idItem = possibleIds[Util.nextInt(possibleIds.length)];
                int tiLe = Util.nextInt(100, 115);
                List<Item.ItemOption> itemOptions = new ArrayList<>();
                switch (idItem) {
                    case 555 -> {
                        itemOptions.add(new Item.ItemOption(47, 800 * tiLe / 100));
                        if (tiLe > 100) {
                            itemOptions.add(new Item.ItemOption(206, tiLe - 100));
                        }
                    }
                    case 557 -> {
                        itemOptions.add(new Item.ItemOption(47, 850 * tiLe / 100));
                        if (tiLe > 100) {
                            itemOptions.add(new Item.ItemOption(206, tiLe - 100));
                        }
                    }
                    case 559 -> {
                        itemOptions.add(new Item.ItemOption(47, 900 * tiLe / 100));
                        if (tiLe > 100) {
                            itemOptions.add(new Item.ItemOption(206, tiLe - 100));
                        }
                    }
                }

                // Thêm options bổ sung (theo logic cũ randDoTL)
                short[] options = {86, 73};
                if (Util.isTrue(30, 100)) {
                    if (Util.isTrue(70, 100)) {
                        itemOptions.add(new Item.ItemOption(options[Util.nextInt(options.length)], 0));
                    }
                }
                itemOptions.add(new Item.ItemOption(21, Util.nextInt(15, 17)));

                // Tạo item với options tương ứng
                ItemMap it = new ItemMap(this.zone, idItem, 1, this.location.x,
                        this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), -1);
                it.options.clear();
                it.options.addAll(itemOptions);
                Service.gI().dropItemMap(this.zone, it);
            }
            Service.gI().sendBigBoss2(this.zone, action, this);
            Message msg = null;
            try {
                msg = new Message(-9);
                msg.writer().writeByte(this.id);
                msg.writer().writeLong(this.point.gethp());
                msg.writer().writeInt(1);
                Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            } catch (Exception e) {
                Logger.logException(Hirudegarn.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
        if (lvMob == 3 && !isDie()) {
            this.location.x = -1000;
            this.location.y = -1000;
        }

        super.update();
    }

    @Override
    public void attack() {
        if (!isDie() && !effectSkill.isHaveEffectSkill() && Util.canDoWithTime(lastBigBossAttackTime, 3000)) {
            Message msg = null;
            try {
                int[] idAction = new int[]{1, 2, 3, 7};
                if (this.lvMob >= 2) {
                    idAction = new int[]{1, 2};
                }
                action = action == 7 ? 0 : idAction[Util.nextInt(0, idAction.length - 1)];

                List<Player> nearbyPlayers = new ArrayList<>();
                for (Player p : this.zone.getPlayers()) {
                    if (p != null && !p.isDie() && Util.getDistance(this.location.x, this.location.y, p.location.x, p.location.y) <= 500) {
                        nearbyPlayers.add(p);
                    }
                }

                if (nearbyPlayers.isEmpty()) {
                    return;
                }

                int index = Util.nextInt(0, nearbyPlayers.size() - 1);
                Player player = nearbyPlayers.get(index);
                if (player == null || player.isDie()) {
                    return;
                }
                if (action == 1) {
                    this.location.x = (short) player.location.x;
                    Service.gI().sendBigBoss2(this.zone, 8, this);
                }
                msg = new Message(101);
                msg.writer().writeByte(action);
                if (action >= 0 && action <= 4) {
                    switch (action) {
                        case 1:
                            msg.writer().writeByte(1);
                            long dame = player.injured(null, this.point.getDameAttack(), false, true);
                            msg.writer().writeInt((int) player.id);
                            msg.writer().writeLong(dame);
                            break;
                        case 3:
                            this.location.x = (short) player.location.x;
                            msg.writer().writeShort(this.location.x);
                            msg.writer().writeShort(this.location.y);
                            break;
                        default:
                            msg.writer().writeByte(nearbyPlayers.size());
                            for (int i = 0; i < nearbyPlayers.size(); i++) {
                                Player pl = nearbyPlayers.get(i);
                                if (pl != null && !pl.isDie()) {
                                    long d = pl.injured(null, this.point.getDameAttack(), false, true);
                                    msg.writer().writeInt((int) pl.id);
                                    msg.writer().writeLong(d);
                                }
                            }
                            break;
                    }
                } else {
                    if (action == 6 || action == 8) {
                        this.location.x = (short) player.location.x;
                        msg.writer().writeShort(this.location.x);
                        msg.writer().writeShort(this.location.y);
                    }
                }
                Service.gI().sendMessAllPlayerInMap(this.zone, msg);
                lastBigBossAttackTime = System.currentTimeMillis();
            } catch (Exception e) {
                if (errors < 5) {
                    errors++;
                    Logger.logException(Hirudegarn.class, e);
                }
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

}
