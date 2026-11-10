package models.npc.specialnpc;

import services.map.ChangeMapService;
import services.PetService;
import models.player.Player;
import utils.Util;
import network.io.Message;
import models.item.Item;
import services.player.InventoryService;
import services.Service;
import utils.Logger;

public class BlackEgg {

    private static final long DEFAULT_TIME_DONE = 86400000L;

    private Player player;
    public long lastTimeCreate;
    public long timeDone;

    private final short id = 52;

    public BlackEgg(Player player, long lastTimeCreate, long timeDone) {
        this.player = player;
        this.lastTimeCreate = lastTimeCreate;
        this.timeDone = timeDone;
    }

    public static void createBlackEgg(Player player) {
        player.blackEgg = new BlackEgg(player, System.currentTimeMillis(), DEFAULT_TIME_DONE);
    }

    public void sendBlackEgg() {
        Message msg;
        if (player.blackEgg != null) {
            try {
                msg = new Message(-122);
                msg.writer().writeShort(this.id);
                msg.writer().writeByte(1);
                msg.writer().writeShort(4672);
                msg.writer().writeByte(0);
                msg.writer().writeInt(this.getSecondDone());
                this.player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Logger.logException(BlackEgg.class, e);
            }
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu phải có đệ tử");
        }
    }

    public int getSecondDone() {
        int seconds = (int) ((lastTimeCreate + timeDone - System.currentTimeMillis()) / 1000);
        return seconds > 0 ? seconds : 0;
    }

    public void openEgg(int gender) {
        if (this.player.pet != null) {
            new Thread(() -> {
                try {
                    destroyEgg();
                    Thread.sleep(4000);
                    if (this.player == null || !this.player.isPl() || this.player.zone == null) {
                        return;
                    }
                    if (this.player.pet == null) {
                        PetService.gI().createBlackPet(this.player, gender);
                    } else {
                        PetService.gI().changeBlackPet(this.player, gender);
                    }
                    ChangeMapService.gI().changeMapInYard(this.player, this.player.gender * 7, -1, Util.nextInt(300, 500));
                    this.player.blackEgg = null;
                    Thread.sleep(500);
                    InventoryService.gI().sendItemBags(this.player);
                    Service.gI().point(this.player);
                } catch (Exception e) {
                }
            }).start();
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu phải có đệ tử");
        }
    }

    public void destroyEgg() {
        try {
            Message msg = new Message(-117);
            msg.writer().writeByte(101);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
        this.player.blackEgg = null;
    }

    public void subTimeDone(int d, int h, int m, int s) {
        this.timeDone -= ((d * 24 * 60 * 60 * 1000) + (h * 60 * 60 * 1000) + (m * 60 * 1000) + (s * 1000));
        this.sendBlackEgg();
    }

    public void dispose() {
        this.player = null;
    }
}
