package services.player;

 


import database.AlyraManager;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import models.player.Player;
import network.io.Message;
import server.Client;
import services.EffectSkillService;
import services.Service;
import services.TaskService;
import services.map.ChangeMapService;
import services.map.MapService;
import utils.Logger;
import utils.Util;

public class PlayerService {

    private static PlayerService i;

    public PlayerService() {
    }

    public static PlayerService gI() {
        if (i == null) {
            i = new PlayerService();
        }
        return i;
    }

    public void sendTNSM(Player player, byte type, long param) {
        if (param > 0) {
            Message msg;
            try {
                msg = new Message(-3);
                msg.writer().writeByte(type);// 0 là cộng sm, 1 cộng tn, 2 là cộng cả 2
                msg.writer().writeLong(param);// số tn cần cộng
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
            }
        }
    }
    public void dailyLogin(Player player) {
        if (Util.compareDay(Date.from(Instant.now()), player.firstTimeLogin)) {
            player.luotNhanBuaMienPhi = 1;
            player.diemDanhSuKien = 1;
            player.firstTimeLogin = Date.from(Instant.now());
        }
    }

    public void sendMessageAllPlayer(Message msg) {
        for (Player pl : Client.gI().getPlayers()) {
            if (pl != null) {
                pl.sendMessage(msg);
            }
        }
        msg.cleanup();
    }

    public void sendMessageIgnore(Player plIgnore, Message msg) {
        for (Player pl : Client.gI().getPlayers()) {
            if (pl != null && !pl.equals(plIgnore)) {
                pl.sendMessage(msg);
            }
        }
        msg.cleanup();
    }

    public void sendInfoHp(Player player) {
        Message msg;
        try {
            msg = Service.gI().messageSubCommand((byte) 5);
            msg.writer().writeLong(player.nPoint.hp);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(PlayerService.class, e);
        }
    }

    public void sendInfoMp(Player player) {
        Message msg;
        try {
            msg = Service.gI().messageSubCommand((byte) 6);
            msg.writer().writeLong(player.nPoint.mp);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(PlayerService.class, e);
        }
    }

    public void sendInfoHpMp(Player player) {
        if (player.nPoint == null) {
            return;
        }
        sendInfoHp(player);
        sendInfoMp(player);
    }

    public void hoiPhuc(Player player, long hp, long mp) {
        if (!player.isDie()) {
            player.nPoint.addHp(hp);
            player.nPoint.addMp(mp);
            // Send_Info_NV phát gói MÁU cho TOÀN KHU. Khi chỉ trừ KI (hp == 0) thì gói
            // đó mang đúng con số máu cũ -> vô nghĩa. Đường bay gọi hoiPhuc(0, -mp) mỗi
            // bước chân, nên khu đông là mỗi người bay bắn thừa một lượt broadcast cho
            // tất cả những người còn lại — chính là thứ làm máy yếu đơ.
            if (hp != 0) {
                Service.gI().Send_Info_NV(player);
            }
            if (!player.isPet && !player.isNewPet) {
                PlayerService.gI().sendInfoHpMp(player);
            }
        }
    }

    public void sendInfoHpMpMoney(Player player) {
        Message msg;
        try {
            long gold = player.inventory.getGoldDisplay();
            msg = Service.gI().messageSubCommand((byte) 4);
            if (player.isVersionAbove(214)) {
                msg.writer().writeLong(gold);//xu
            } else {
                msg.writer().writeInt((int) gold);//xu
            }
            msg.writer().writeInt(player.inventory.gem);//luong
            msg.writer().writeLong(player.nPoint.hp);//chp
            msg.writer().writeLong(player.nPoint.mp);//cmp
            msg.writer().writeInt(player.inventory.ruby);//ruby
            player.sendMessage(msg);
        } catch (IOException e) {
            Logger.logException(PlayerService.class, e);
        }
    }

    public void playerMove(Player player, int x, int y) {
        if (player.zone == null) {
            return;
        }
        if (!player.isDie()) {
            if (player.effectSkill.isCharging) {
                EffectSkillService.gI().stopCharge(player);
            }
            if (player.effectSkill.useTroi) {
                EffectSkillService.gI().removeUseTroi(player);
            }
            // Kẹp vào trong bản đồ. Boss đuổi theo người chơi bị kéo lên mép trên hoặc
            // ra ngoài map sẽ tự đi ra ngoài theo, rồi mọi phép tính dựa trên ô tile
            // (tileMap[y/24][x/24]) vượt mảng và ném lỗi liên tục -> server khựng.
            try {
                models.map.Map bd = player.zone.map;
                if (bd != null && bd.mapWidth > 48 && bd.mapHeight > 48) {
                    if (x < 24) {
                        x = 24;
                    } else if (x > bd.mapWidth - 24) {
                        x = bd.mapWidth - 24;
                    }
                    if (y < 24) {
                        y = 24;
                    } else if (y > bd.mapHeight - 24) {
                        y = bd.mapHeight - 24;
                    }
                }
            } catch (Exception ignored) {
            }
            // Đứng yên mà vẫn phát gói cho cả khu là gói thừa 100%: bot đánh quái gọi
            // playerMove tới đúng chỗ nó đang đứng, client cũng gửi lặp cùng toạ độ.
            boolean coDiChuyen = (player.location.x != x || player.location.y != y);
            player.location.x = x;
            player.location.y = y;
            player.location.lastTimeplayerMove = System.currentTimeMillis();
            switch (player.zone.map.mapId) {
                case 85:
                case 86:
                case 87:
                case 88:
                case 89:
                case 90:
                case 91:
                    if (!player.isBoss && !player.isPet && !player.isNewPet) {
                        if (x < 24 || x > player.zone.map.mapWidth - 24 || y < 0 || y > player.zone.map.mapHeight - 24) {
                            if (MapService.gI().getWaypointPlayerIn(player) == null) {
                                ChangeMapService.gI().changeMap(player, 21 + player.gender, 0, 200, 336);
                                return;
                            }
                        }
                        int yTop = player.zone.map.yPhysicInTop(player.location.x, player.location.y);
                        if (yTop >= player.zone.map.mapHeight - 24) {
                            ChangeMapService.gI().changeMap(player, 21 + player.gender, 0, 200, 336);
                            return;
                        }
                    }
                    break;
            }
            if (player.pet != null) {
                player.pet.followMaster();
            }
            if (player.newPet != null) {
                player.newPet.followMaster();
            }
            if (player.isPl()) {
                try {
                    int type = player.zone.map.tileMap[player.location.y / 24][player.location.x / 24];
                    player.isFly = type == 0;
                } catch (Exception e) {
                }
                if (player.isFly && player.getMount() == -1) {
                    long mp = player.nPoint.mpg / (100 * (player.effectSkill.isMonkey ? 2 : 1));
                    hoiPhuc(player, 0, -mp);
                }
            }
            if (coDiChuyen) {
                MapService.gI().sendPlayerMove(player);
            }
            TaskService.gI().checkDoneTaskGoToMap(player, player.zone);
        }
    }

    public void sendCurrentStamina(Player player) {
        Message msg;
        try {
            msg = new Message(-68);
            msg.writer().writeShort(player.nPoint.stamina);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(PlayerService.class, e);
        }
    }

    public void sendMaxStamina(Player player) {
        Message msg;
        try {
            msg = new Message(-69);
            msg.writer().writeShort(player.nPoint.maxStamina);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(PlayerService.class, e);
        }
    }

    public void changeAndSendTypePK(Player player, int type) {
        changeTypePK(player, type);
        sendTypePk(player);
    }

    public void changeTypePK(Player player, int type) {
        player.typePk = (byte) type;
    }

    public void sendTypePk(Player player) {
        Message msg;
        try {
            msg = Service.gI().messageSubCommand((byte) 35);
            msg.writer().writeInt((int) player.id);
            msg.writer().writeByte(player.typePk);
            Service.gI().sendMessAllPlayerInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void banPlayer(Player playerBaned) {
        try {
            AlyraManager.executeUpdate("update account set ban = 1 where id = ? and username = ?",
                    playerBaned.getSession().userId, playerBaned.getSession().uu);
        } catch (Exception e) {
        }
        Service.gI().sendThongBao(playerBaned,
                "Tài khoản của bạn đã bị khóa\nGame sẽ mất kết nối sau 5 giây...");
        playerBaned.idMark.setLastTimeBan(System.currentTimeMillis());
        playerBaned.idMark.setBan(true);
    }

    private static final int COST_GOLD_HOI_SINH = 100000;
    private static final int COST_GEM_HOI_SINH = 1;
    private static final int COST_GOLD_HOI_SINH_NRSD = 200000;

    public void hoiSinh(Player player) {
        if (player.isDie() && player.zone != null && player.zone.map.mapId != 51) {
            if (Util.canDoWithTime(player.lastTimeRevived, 1500)) {
                boolean canHs;
                if (MapService.gI().isMapBlackBallWar(player.zone.map.mapId)) {
                    if (player.inventory.gold >= COST_GOLD_HOI_SINH_NRSD) {
                        player.inventory.gold -= COST_GOLD_HOI_SINH_NRSD;
                        canHs = true;
                    } else {
                        Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện, còn thiếu " + Util.numberToMoney(COST_GOLD_HOI_SINH_NRSD
                                - player.inventory.gold) + " vàng");
                        return;
                    }
                } else {
                    if (player.inventory.gem >= COST_GEM_HOI_SINH) {
                        player.inventory.gem -= COST_GEM_HOI_SINH;
                        canHs = true;
                    } else {
                        Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện, còn thiếu " + Util.numberToMoney(COST_GEM_HOI_SINH
                                - player.inventory.gem) + " ngọc");
                        return;
                    }
                }
                if (canHs) {
                    Service.gI().sendMoney(player);
                    Service.gI().hsChar(player, player.nPoint.hpMax, player.nPoint.mpMax);
                }
            }
        }
    }

    public void hoiSinhMaBu(Player player) {
        if (player.isDie()) {
            boolean canHs = false;
            if (MapService.gI().isMapMaBu(player.zone.map.mapId)) {
                if (player.inventory.gold >= COST_GOLD_HOI_SINH_NRSD) {
                    player.inventory.gold -= COST_GOLD_HOI_SINH_NRSD;
                    canHs = true;
                } else {
                    Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện, còn thiếu " + Util.numberToMoney(COST_GOLD_HOI_SINH_NRSD
                            - player.inventory.gold) + " vàng");
                    return;
                }
            } else {
                if (player.inventory.gold >= COST_GOLD_HOI_SINH) {
                    player.inventory.gold -= COST_GOLD_HOI_SINH;
                    canHs = true;
                } else {
                    Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện, còn thiếu " + Util.numberToMoney(COST_GOLD_HOI_SINH
                            - player.inventory.gold) + " vàng");
                    return;
                }
            }
            if (canHs) {
                Service.gI().sendMoney(player);
                Service.gI().hsChar(player, player.nPoint.hpMax, player.nPoint.mpMax);
            }
        }
    }

}
