package server;

 


import java.util.ArrayList;
import java.util.List;
import models.player.Player;
import network.io.Message;
import services.Service;
import utils.Util;

public class ServerNotify extends Thread {

    private long lastNotifyTime;

    private final java.util.Queue<String> notifies;

    private int indexNotify;

    private final String notify[] = {"Ngọc Rồng Online là phiên bản ngọc rồng online miễn phí cực kỳ đáng chơi , Tham gia nhóm chat tại : nroha.fun"};

    private static ServerNotify instance;

    private ServerNotify() {
        this.notifies = new java.util.concurrent.ConcurrentLinkedQueue<>();
        this.start();
    }

    public static ServerNotify gI() {
        if (instance == null) {
            instance = new ServerNotify();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                if (Util.canDoWithTime(this.lastNotifyTime, 100000)) {
                    sendChatVip(notify[indexNotify]);
                    this.lastNotifyTime = System.currentTimeMillis();
                    indexNotify++;
                    if (indexNotify >= notify.length) {
                        indexNotify = 0;
                    }
                }
                String noti = notifies.poll();
                if (noti != null) {
                    sendChatVip(noti);
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void sendChatVip(String text) {
        Message msg;
        try {
            msg = new Message(93);
            msg.writer().writeUTF(text);
            Service.gI().sendMessAllPlayer(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void notify(String text) {
        this.notifies.offer(text);
    }

    public void sendNotifyTab(Player player) {
        Message msg;
        try {
            msg = new Message(50);
            final int count = Math.min(Manager.NOTIFY.size(), 127);
            msg.writer().writeByte(count);
            for (int i = 0; i < count; i++) {
                String[] arr = Manager.NOTIFY.get(i).split("<>");
                msg.writer().writeShort(i);
                msg.writer().writeUTF(arr[0]);
                msg.writer().writeUTF(arr[1]);
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception ignored) {
        }
    }
}
