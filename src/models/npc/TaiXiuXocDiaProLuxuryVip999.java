/*
 */
package models.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import models.player.Player;
import server.ServerManager;
import services.Service;
import utils.Util;

/**
 *
 * @author by Ts
 */
public class TaiXiuXocDiaProLuxuryVip999 implements Runnable {

    public static boolean baoTri;
    public volatile boolean settling = false;

    public int totalChan, totalLe, total3Trang1Do, total3Do1Trang, totalTuTuTrang, totalTuTuDo;

    private boolean adminChangeMode, ketquaChan, ketquaLe, ketqua3Trang1Do, ketqua3Do1Trang, ketquaTuTuTrang, ketquaTuTuDo;

    public final List<Player> PlayersChan = new java.util.concurrent.CopyOnWriteArrayList<>(), PlayersLe = new java.util.concurrent.CopyOnWriteArrayList<>();
    public final List<Player> Players3Trang1Do = new ArrayList<>(), Players3Do1Trang = new ArrayList<>();
    public final List<Player> PlayersTuTuTrang = new ArrayList<>(), PlayersTuTuDo = new ArrayList<>();

    public long lastTimeEnd = System.currentTimeMillis();
    public static final int TIME_END = 60000;

    private int a, b, c, d, soDo, soTrang;

    public String ketQuaXocDia;
    public final List<String> listKetQua = new ArrayList<>();

    private static TaiXiuXocDiaProLuxuryVip999 instance;

    public static TaiXiuXocDiaProLuxuryVip999 gI() {
        if (instance == null) {
            instance = new TaiXiuXocDiaProLuxuryVip999();
        }
        return instance;
    }

    public StringBuffer getKetQua() {
        StringBuffer kq = new StringBuffer();
        if (!listKetQua.isEmpty()) {
            for (String i : listKetQua) {
                kq.append("\n|5|").append(i);
            }
        }
        return kq;
    }

    private void addKetQua() {
        if (listKetQua.size() > 5) {
            listKetQua.clear();
        }
        listKetQua.add(ketQuaXocDia);
    }

    public void addPlayerChan(Player pl) {
        if (!PlayersChan.contains(pl)) {
            PlayersChan.add(pl);
        }
    }

    public void addPlayerLe(Player pl) {
        if (!PlayersLe.contains(pl)) {
            PlayersLe.add(pl);
        }
    }

    public void addPlayer3Trang1Do(Player pl) {
        if (!Players3Trang1Do.contains(pl)) {
            Players3Trang1Do.add(pl);
        }
    }

    public void addPlayer3Do1Trang(Player pl) {
        if (!Players3Do1Trang.contains(pl)) {
            Players3Do1Trang.add(pl);
        }
    }

    public void addPlayerTuTuTrang(Player pl) {
        if (!PlayersTuTuTrang.contains(pl)) {
            PlayersTuTuTrang.add(pl);
        }
    }

    public void addPlayerTuTuDo(Player pl) {
        if (!PlayersTuTuDo.contains(pl)) {
            PlayersTuTuDo.add(pl);
        }
    }

    public static final byte TU_TU_DO = 0;
    public static final byte TU_TU_TRANG = 1;
    public static final byte BA_TRANG_MOT_DO = 2;
    public static final byte BA_DO_MOT_TRANG = 3;
    public static final byte CHAN = 4;
    public static final byte LE = 5;

    public void changeKetQua(int typeset) {
        switch (typeset) {
            case TU_TU_DO -> {
                a = 1;
                b = 1;
                c = 1;
                d = 1;
            }
            case TU_TU_TRANG -> {
                a = 0;
                b = 0;
                c = 0;
                d = 0;
            }
            case BA_TRANG_MOT_DO -> {
                if (Util.isTrue(1, 2)) {
                    a = 1;
                    b = 0;
                    c = 0;
                    d = 0;
                } else if (Util.isTrue(1, 2)) {
                    a = 0;
                    b = 1;
                    c = 0;
                    d = 0;
                } else if (Util.isTrue(1, 2)) {
                    a = 0;
                    b = 0;
                    c = 1;
                    d = 0;
                } else if (Util.isTrue(1, 2)) {
                    a = 0;
                    b = 0;
                    c = 0;
                    d = 1;
                }
            }
            case BA_DO_MOT_TRANG -> {
                if (Util.isTrue(1, 2)) {
                    a = 0;
                    b = 1;
                    c = 1;
                    d = 1;
                } else if (Util.isTrue(1, 2)) {
                    a = 1;
                    b = 0;
                    c = 1;
                    d = 1;
                } else if (Util.isTrue(1, 2)) {
                    a = 1;
                    b = 1;
                    c = 0;
                    d = 1;
                } else if (Util.isTrue(1, 2)) {
                    a = 1;
                    b = 1;
                    c = 1;
                    d = 0;
                }
            }
            case CHAN -> {
                a = 1;
                b = 1;
                c = 1;
                d = 1;
            }
            case LE -> {
                a = 0;
                b = 0;
                c = 0;
                d = 0;
            }
        }
        adminChangeMode = true;
        soDo = a + b + c + d;
        soTrang = 4 - soDo;
        ketQuaXocDia = (a == 0 ? "Trắng" : "Đỏ") + " : " + (b == 0 ? "Trắng" : "Đỏ") + " : " + (c == 0 ? "Trắng" : "Đỏ") + " : " + (d == 0 ? "Trắng" : "Đỏ") + " (" + (soTrang == 4 ? "Tứ Tử Trắng" : soDo == 4 ? "Tứ Tử Đỏ" : soTrang == 3 && soDo == 1 ? "3 Trắng 1 Đỏ" : soDo == 3 && soTrang == 1 ? "3 Đỏ 1 Trắng" : soDo % 2 == 0 ? "Chẵn" : "Lẻ") + ")";
        if (soTrang == 4) {
            ketquaTuTuTrang = true;
        } else if (soDo == 4) {
            ketquaTuTuDo = true;
        } else if (soTrang == 3 && soDo == 1) {
            ketqua3Trang1Do = true;
        } else if (soTrang == 1 && soDo == 3) {
            ketqua3Do1Trang = true;
        } else if (soDo % 2 == 0) {
            ketquaChan = true;
        } else {
            ketquaLe = true;
        }
    }

    public String getKetQuaAdminChange() {
        StringBuilder text = new StringBuilder();
        if (adminChangeMode) {
            text.append("\n|-1|ĐÃ CAN THIỆP KẾT QUẢ\n|7|").append(a == 0 ? "Trắng" : "Đỏ").append(" : ").append(b == 0 ? "Trắng" : "Đỏ").append(" : ").append(c == 0 ? "Trắng" : "Đỏ").append(" : ").append(d == 0 ? "Trắng" : "Đỏ").append(" (").append(soTrang == 4 ? "Tứ Tử Trắng" : soDo == 4 ? "Tứ Tử Đỏ" : soTrang == 3 && soDo == 1 ? "3 Trắng 1 Đỏ" : soDo == 3 && soTrang == 1 ? "3 Đỏ 1 Trắng" : soDo % 2 == 0 ? "Chẵn" : "Lẻ").append(")");
        }
        return text.toString();
    }

    private void setKetQua() {
        if (adminChangeMode) {
            adminChangeMode = false;
        } else {
            a = Util.nextInt(0, 1);
            b = Util.nextInt(0, 1);
            c = Util.nextInt(0, 1);
            d = Util.nextInt(0, 1);
            soDo = a + b + c + d;
            soTrang = 4 - soDo;
            ketQuaXocDia = (a == 0 ? "Trắng" : "Đỏ") + " : " + (b == 0 ? "Trắng" : "Đỏ") + " : " + (c == 0 ? "Trắng" : "Đỏ") + " : " + (d == 0 ? "Trắng" : "Đỏ") + " (" + (soTrang == 4 ? "Tứ Tử Trắng" : soDo == 4 ? "Tứ Tử Đỏ" : soTrang == 3 && soDo == 1 ? "3 Trắng 1 Đỏ" : soDo == 3 && soTrang == 1 ? "3 Đỏ 1 Trắng" : soDo % 2 == 0 ? "Chẵn" : "Lẻ") + ")";
            if (soTrang == 4) {
                if (Util.isTrue(1, 100)) {
                    ketquaTuTuTrang = true;
                } else {
                    changeKetQua(Util.nextInt(2, 5));
                }
            } else if (soDo == 4) {
                if (Util.isTrue(1, 100)) {
                    ketquaTuTuDo = true;
                } else {
                    changeKetQua(Util.nextInt(2, 5));
                }
            } else if (soTrang == 3 && soDo == 1) {
                ketqua3Trang1Do = true;
            } else if (soTrang == 1 && soDo == 3) {
                ketqua3Do1Trang = true;
            } else if (soDo % 2 == 0) {
                ketquaChan = true;
            } else {
                ketquaLe = true;
            }
        }
    }

    public void baoTriAndRefund() {
        if (!baoTri) {
            baoTri = true;
            try {
                refund();
            } finally {
                clear();
            }
        } else {
            baoTri = false;
        }
    }

    private void refund() {
        if (!PlayersTuTuTrang.isEmpty()) {
            for (Player pl : PlayersTuTuTrang) {
                try {
                    pl.inventory.ruby += pl.betTuTuTrang;
                    pl.betTuTuTrang = 0;
                    Service.gI().sendMoney(pl);
                } catch (Exception e) {
                }
            }
        }
        if (!PlayersTuTuDo.isEmpty()) {
            for (Player pl : PlayersTuTuDo) {
                try {
                    pl.inventory.ruby += pl.betTuTuDo;
                    pl.betTuTuDo = 0;
                    Service.gI().sendMoney(pl);
                } catch (Exception e) {
                }
            }
        }
        if (!Players3Trang1Do.isEmpty()) {
            for (Player pl : Players3Trang1Do) {
                try {
                    pl.inventory.ruby += pl.bet3Trang1Do;
                    pl.bet3Trang1Do = 0;
                    Service.gI().sendMoney(pl);
                } catch (Exception e) {
                }
            }
        }
        if (!Players3Do1Trang.isEmpty()) {
            for (Player pl : Players3Do1Trang) {
                try {
                    pl.inventory.ruby += pl.bet3Do1Trang;
                    pl.bet3Do1Trang = 0;
                    Service.gI().sendMoney(pl);
                } catch (Exception e) {
                }
            }
        }
        if (!PlayersChan.isEmpty()) {
            for (Player pl : PlayersChan) {
                try {
                    pl.inventory.ruby += pl.betChan;
                    pl.betChan = 0;
                    Service.gI().sendMoney(pl);
                } catch (Exception e) {
                }
            }
        }
        if (!PlayersLe.isEmpty()) {
            for (Player pl : PlayersLe) {
                try {
                    pl.inventory.ruby += pl.betLe;
                    pl.betLe = 0;
                    Service.gI().sendMoney(pl);
                } catch (Exception e) {
                }
            }
        }
    }

    private void reward() {
        if (ketquaTuTuTrang) {
            if (!PlayersTuTuTrang.isEmpty()) {
                for (Player pl : PlayersTuTuTrang) {
                    try {
                        long rubyReward = (long) pl.betTuTuTrang * 10;
                        pl.betTuTuTrang = 0;
                        pl.inventory.ruby = (int) Math.min((long) Integer.MAX_VALUE, pl.inventory.ruby + rubyReward);
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu " + ServerManager.NAME + " ]\n\n|2|" + ketQuaXocDia + "\n|5|Tổng là: Tứ tử trắng!\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.format(rubyReward) + " Hồng ngọc");
                    } catch (Exception e) {
                    }
                }
            }
        } else if (ketquaTuTuDo) {
            if (!PlayersTuTuDo.isEmpty()) {
                for (Player pl : PlayersTuTuDo) {
                    try {
                        int goldC = pl.betTuTuDo * 10;
                        pl.betTuTuDo = 0;
                        pl.inventory.ruby += goldC;
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu " + ServerManager.NAME + " ]\n\n|2|" + ketQuaXocDia + "\n|5|Tổng là: Tứ tử trắng!\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.format(goldC) + " Hồng ngọc");
                    } catch (Exception e) {
                    }
                }
            }
        } else if (ketqua3Trang1Do) {
            if (!Players3Trang1Do.isEmpty()) {
                for (Player pl : Players3Trang1Do) {
                    try {
                        long rubyReward = (long) pl.bet3Trang1Do * 3;
                        pl.bet3Trang1Do = 0;
                        pl.inventory.ruby = (int) Math.min((long) Integer.MAX_VALUE, pl.inventory.ruby + rubyReward);
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu " + ServerManager.NAME + " ]\n\n|2|" + ketQuaXocDia + "\n|5|Tổng là: Tứ tử trắng!\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.format(rubyReward) + " Hồng ngọc");
                    } catch (Exception e) {
                    }
                }
            }
        } else if (ketqua3Do1Trang) {
            if (!Players3Do1Trang.isEmpty()) {
                for (Player pl : Players3Do1Trang) {
                    try {
                        long rubyReward = (long) pl.bet3Do1Trang * 3;
                        pl.bet3Do1Trang = 0;
                        pl.inventory.ruby = (int) Math.min((long) Integer.MAX_VALUE, pl.inventory.ruby + rubyReward);
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu " + ServerManager.NAME + " ]\n\n|2|" + ketQuaXocDia + "\n|5|Tổng là: Tứ tử trắng!\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.format(rubyReward) + " Hồng ngọc");
                    } catch (Exception e) {
                    }
                }
            }
        } else if (ketquaChan) {
            if (!PlayersChan.isEmpty()) {
                for (Player pl : PlayersChan) {
                    try {
                        long rubyReward = (long) (pl.betChan * 1.8);
                        pl.betChan = 0;
                        pl.inventory.ruby = (int) Math.min((long) Integer.MAX_VALUE, pl.inventory.ruby + rubyReward);
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu " + ServerManager.NAME + " ]\n\n|2|" + ketQuaXocDia + "\n|5|Tổng là: Tứ tử trắng!\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.format(rubyReward) + " Hồng ngọc");
                    } catch (Exception e) {
                    }
                }
            }
        } else if (ketquaLe) {
            if (!PlayersLe.isEmpty()) {
                for (Player pl : PlayersLe) {
                    try {
                        long rubyReward = (long) (pl.betLe * 1.8);
                        pl.betLe = 0;
                        pl.inventory.ruby = (int) Math.min((long) Integer.MAX_VALUE, pl.inventory.ruby + rubyReward);
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu " + ServerManager.NAME + " ]\n\n|2|" + ketQuaXocDia + "\n|5|Tổng là: Tứ tử trắng!\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.format(rubyReward) + " Hồng ngọc");
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void clear() {
        totalChan = 0;
        totalLe = 0;
        total3Trang1Do = 0;
        total3Do1Trang = 0;
        totalTuTuTrang = 0;
        totalTuTuDo = 0;
        ketquaChan = false;
        ketquaLe = false;
        ketqua3Trang1Do = false;
        ketqua3Do1Trang = false;
        ketquaTuTuTrang = false;
        ketquaTuTuDo = false;

        if (!PlayersChan.isEmpty()) {
            for (Player pl : PlayersChan) {
                pl.betChan = 0;
            }
        }
        if (!PlayersLe.isEmpty()) {
            for (Player pl : PlayersLe) {
                pl.betLe = 0;
            }
        }
        if (!PlayersTuTuDo.isEmpty()) {
            for (Player pl : PlayersTuTuDo) {
                pl.betTuTuDo = 0;
            }
        }
        if (!PlayersTuTuTrang.isEmpty()) {
            for (Player pl : PlayersTuTuTrang) {
                pl.betTuTuTrang = 0;
            }
        }
        if (!Players3Trang1Do.isEmpty()) {
            for (Player pl : Players3Trang1Do) {
                pl.bet3Trang1Do = 0;
            }
        }
        if (!Players3Do1Trang.isEmpty()) {
            for (Player pl : Players3Do1Trang) {
                pl.bet3Do1Trang = 0;
            }
        }

        PlayersChan.clear();
        PlayersLe.clear();
        Players3Trang1Do.clear();
        Players3Do1Trang.clear();
        PlayersTuTuTrang.clear();
        PlayersTuTuDo.clear();
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
            }
            if (Util.canDoWithTime(lastTimeEnd, TIME_END)) {
                if (!baoTri) {
                    settling = true;
                    try {
                        setKetQua();
                    } finally {
                        addKetQua();
                        try {
                            reward();
                        } finally {
                            clear();
                            settling = false;
                        }
                    }
                }
                lastTimeEnd = System.currentTimeMillis();
            }
        }
    }
}
