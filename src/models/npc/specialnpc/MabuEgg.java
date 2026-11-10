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

public class MabuEgg {

//    private static final long DEFAULT_TIME_DONE = 7776000000L;
    private static final long DEFAULT_TIME_DONE = 86400000L;

    private Player player;
    public long lastTimeCreate;
    public long timeDone;

    private final short id = 50;

    public MabuEgg(Player player, long lastTimeCreate, long timeDone) {
        this.player = player;
        this.lastTimeCreate = lastTimeCreate;
        this.timeDone = timeDone;
    }

    public static void createMabuEgg(Player player) {
        player.mabuEgg = new MabuEgg(player, System.currentTimeMillis(), DEFAULT_TIME_DONE);
    }

    public void sendMabuEgg() {
        Message msg;
        try {

            msg = new Message(-122);
            msg.writer().writeShort(this.id);
            msg.writer().writeByte(1);
            msg.writer().writeShort(4664);
            msg.writer().writeByte(0);
            msg.writer().writeInt(this.getSecondDone());
            this.player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(MabuEgg.class, e);
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
                        PetService.gI().createMabuPet(this.player, gender);
                    } else {
                        PetService.gI().changeMabuPet(this.player, gender);
                    }
                    int mapId;
                    switch (gender) {
                        case 0 ->
                            mapId = 0;
                        case 1 ->
                            mapId = 7;
                        case 2 ->
                            mapId = 14;
                        default -> {
                            return;
                        }
                    }
                    ChangeMapService.gI().changeMapInYard(this.player, mapId, -1, Util.nextInt(300, 500));
                    this.player.mabuEgg = null;
                    Thread.sleep(500);
                    InventoryService.gI().sendItemBags(this.player);
                    Service.gI().point(this.player);
                } catch (Exception e) {
                    e.printStackTrace();
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
        this.player.mabuEgg = null;
    }

    public void subTimeDone(int d, int h, int m, int s) {
        this.timeDone -= ((d * 24 * 60 * 60 * 1000) + (h * 60 * 60 * 1000) + (m * 60 * 1000) + (s * 1000));
        this.sendMabuEgg();
    }

    public void dispose() {
        this.player = null;
    }
}
