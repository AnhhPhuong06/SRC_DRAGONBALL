package services.func.minigame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.player.Player;
import server.Client;
import server.ServerNotify;
import services.ChatGlobalService;
import services.Service;
import utils.Util;

public class CSMM implements Runnable {

    public static CSMM i;

    public static synchronized CSMM gI() {
        if (i == null) {
            i = new CSMM();
        }
        return i;
    }

    public long lastTimeRollCSMM = System.currentTimeMillis();
    public int NumRANDOM = -1;
    public java.util.concurrent.ConcurrentHashMap<Player, Integer> listRegNumber = new java.util.concurrent.ConcurrentHashMap<>();
    public int totalRuby;
    public int SecondsTarget = 60;
    public int Size_Num = 999;
    public int costRuby = 100;

    @Override
    public void run() {
        while (true) {
            try {
                if (Util.canDoWithTime(lastTimeRollCSMM, (SecondsTarget * 1000L))) {
                    HandleCSMM();
                }
                Thread.sleep(1000);
            } catch (Exception e) {

            }
        }
    }

    synchronized void HandleCSMM() {
        int ranNum = Util.nextInt(1, Size_Num);
        NumRANDOM = ranNum;

        ServerNotify.gI()
                .notify("Làm khò không giấu - làm giàu không khó chỉ có tại Lý Tiểu Nương.");

        if (!listRegNumber.isEmpty()) {
            List<Player> players = new ArrayList<>();
            for (Map.Entry<Player, Integer> entry : listRegNumber.entrySet()) {
                Player pl = entry.getKey();
                Integer regNum = entry.getValue();
                if (regNum.equals(ranNum)) {
                    players.add(pl);
                }
            }

            List<Player> plWins = new ArrayList<>();
            if (!players.isEmpty()) {
                for (Player pl : players) {
                    if (pl != null && Client.gI().getPlayer(pl.id) != null) {
                        // int quanFist = pl.inventory.ruby;
                        // giveReward(pl);
                        // int quanAF = pl.inventory.ruby;
                        plWins.add(pl);
                    }
                }
            }

            if(!plWins.isEmpty()){
                int reward = totalRuby / plWins.size();
                for(Player pl : plWins){
                    giveReward(pl,reward);
                }
            }
        }

        lastTimeRollCSMM = System.currentTimeMillis();
        listRegNumber.clear();
        totalRuby = 0;
    }

    private void giveReward(Player pl,int numRuby) {
        pl.inventory.ruby = (int)Math.min(Integer.MAX_VALUE, (long)pl.inventory.ruby + numRuby);
        Service.gI().sendThongBaoFromAdmin(pl, "Bạn vừa nhận được " + numRuby + "  Hồng ngọc từ Con số may mắn !");
        Service.gI().sendMoney(pl);
    }

    public synchronized void Register(Player player, int numReg) {
        if (player == null || player.inventory == null || numReg < 1 || numReg > Size_Num || costRuby <= 0
                || totalRuby < 0 || totalRuby > Integer.MAX_VALUE - costRuby) return;
        if (!listRegNumber.containsKey(player)) {
            if (player.inventory.ruby >= costRuby) {
                totalRuby += costRuby;
                player.inventory.ruby -= costRuby;
                Service.gI().sendThongBao(player, "Bạn đã đăng ký thành công với số " + numReg + "!");
                listRegNumber.put(player, numReg);
            } else {
                Service.gI().sendThongBao(player, "Bạn còn thiếu " + (costRuby - player.inventory.ruby) + " hồng ngọc nữa !");
            }

        } else {
            Service.gI().sendThongBao(player,
                    "Bạn đã đăng ký với số " + listRegNumber.get(player) + " rồi mà ?!");
        }
    }

}
