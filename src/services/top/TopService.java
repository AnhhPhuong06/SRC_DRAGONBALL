package services.top;

import database.AlyraManager;
import database.daos.NDVSqlFetcher;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import models.clan.Clan;
import models.player.Player;
import network.io.Message;
import server.Client;
import server.ServerManager;
import static services.top.TopManager.TOP_TEMPLATE;
import services.top.TopManager.TopTemplate;
import models.task.TaskMain;
import org.json.simple.JSONArray;
import utils.Logger;
import utils.TimeUtil;
import utils.Util;

/**
 *
 * @author Ts
 */
public class TopService implements Runnable {

    private static TopService I;

    public static TopService gI() {
        if (I == null) {
            I = new TopService();
        }
        return I;
    }

    @Getter
    /** Khởi tạo rỗng thay vì null: NPC Ghi Danh đọc thẳng danh sách này, null là NPE. */
    public static List<TOP> topMayDam = new ArrayList<>();
    private static long timeRealTop = 0;

    public static final String TOP_MAYDAM
            = "SELECT player.id AS id, CAST(JSON_EXTRACT(loadtimetop, '$[0]') AS UNSIGNED) AS lasttime, "
            + "CAST(JSON_EXTRACT(loadtimetop, '$[1]') AS UNSIGNED) AS maydamtd FROM player "
            + "WHERE loadtimetop IS NOT NULL AND JSON_VALID(loadtimetop) "
            + "ORDER BY maydamtd DESC LIMIT 300;";

    public synchronized void reloadTopMayDam() {
        try (Connection con = AlyraManager.getConnection()) {
            topMayDam = realTop(TOP_MAYDAM, con);
            timeRealTop = System.currentTimeMillis();
        } catch (Exception e) {
            Logger.logException(TopService.class, e);
        }
    }

    public int getPlayerRankMayDamDB(Player p) {
        if (p == null || p.diemmaydam <= 10) {
            return -1;
        }
        String sql = "SELECT COUNT(*) + 1 AS rank FROM player WHERE loadtimetop IS NOT NULL "
                + "AND JSON_VALID(loadtimetop) "
                + "AND CAST(JSON_EXTRACT(loadtimetop, '$[1]') AS UNSIGNED) > ?";
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, p.diemmaydam);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rank");
                }
            }
        } catch (Exception e) {
            Logger.logException(TopService.class, e);
        }
        return -1;
    }

    public TopTemplate getTop(Object value) {
        for (TopTemplate template : TOP_TEMPLATE) {
            try {
                int id = Integer.parseInt(value.toString());
                if (template.id == id) {
                    return template;
                }
            } catch (NumberFormatException e) {
                if (template.name.equals(value)) {
                    return template;
                }
            }
        }
        return null;
    }

    public static List<TOP> realTop(String query, Connection con) {
        List<TOP> tops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int playerId = rs.getInt("id");
                TOP t = TOP.builder().id_player(playerId).build();
                t.setLasttime(rs.getLong("lasttime"));
                switch (query) {
                    default -> {
                        String valKey = switch (query) {
                            case TOP_MAYDAM ->
                                "maydamtd";
                            default ->
                                null;
                        };
                        if (valKey != null) {
                            int d = rs.getInt(valKey);
                            t.setInfo1(String.valueOf(d));
                            t.setInfo2(String.valueOf(d));
                        }
                    }
                }
                try {
                    t.findPlayer(con, playerId);
                } catch (Exception ignored) {
                }
                if (TOP_MAYDAM.equals(query)) {
                    Player onlinePl = Client.gI().getPlayer(playerId);
                    if (onlinePl != null) {
                        t.setPlayer(onlinePl);
                        long liveScore = onlinePl.diemmaydam;
                        t.setInfo1(String.valueOf(liveScore));
                        t.setInfo2(String.valueOf(liveScore));
                        if (onlinePl.lasttimeTD > 0) {
                            t.setLasttime(onlinePl.lasttimeTD);
                        }
                    }
                }
                tops.add(t);
            }
            if (TOP_MAYDAM.equals(query)) {
                tops.sort((a, b) -> {
                    long sA = 0;
                    long sB = 0;
                    try {
                        sA = Long.parseLong(a.getInfo1());
                    } catch (Exception ignored) {}
                    try {
                        sB = Long.parseLong(b.getInfo1());
                    } catch (Exception ignored) {}
                    return Long.compare(sB, sA);
                });
            }
        } catch (Exception e) {
            Logger.logException(TopService.class, e);
        }
        return tops;
    }

    public static void showListTopTrung(Player player) {
        if (player != null && !player.isBot) {
            try {
                database.daos.PlayerDAO.updatePlayer(player);
            } catch (Exception ignored) {
            }
        }
        TopTrung.getInstance().load(true);
        List<Player> list = TopTrung.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                msg.writer().writeUTF("Top Bóng Master: " + Util.numberToMoney(top.point_pokemon));
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public static void showListTopQuay(Player player) {
        if (player != null && !player.isBot) {
            try {
                database.daos.PlayerDAO.updatePlayer(player);
            } catch (Exception ignored) {
            }
        }
        TopQuay.getInstance().load(true);
        List<Player> list = TopQuay.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top Lượt Quay");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                msg.writer().writeUTF("Số lần quay: " + Util.numberToMoney(top.lixi));
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public static void showListBuoi(Player player) {
        if (player != null && !player.isBot) {
            try {
                database.daos.PlayerDAO.updatePlayer(player);
            } catch (Exception ignored) {
            }
        }
        TopDauBuoi.getInstance().load(true);
        List<Player> list = TopDauBuoi.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top Sự Kiện");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                msg.writer().writeUTF("Điểm săn boss sự kiện : " + Util.numberToMoney(top.daubuoi));
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void showListTop(Player p, int type) {
        if (p == null) {
            return;
        }
        Message msg = new Message(-96);
        try {
            DataOutputStream w = msg.writer();
            w.writeByte(0);
            w.writeUTF("Top 100");
            List<TOP> tops;
            switch (type) {
                case 0, 1, 2 ->
                    tops = topMayDam;
                default -> {
                    return;
                }
            }
            if (tops == null || tops.isEmpty()) {
                w.writeByte(0);
                p.sendMessage(msg);
                return;
            }
            List<TopMayDamEntry> filt = new ArrayList<>();
            for (TOP t : tops) {
                if (t == null) {
                    continue;
                }
                Player pl = Client.gI().getPlayer(t.getId_player());
                if (pl == null) {
                    pl = t.getPlayer();
                }
                if (pl != null && (type > 2 || pl.gender == type)) {
                    filt.add(new TopMayDamEntry(t, pl));
                }
            }
            w.writeByte(filt.size());
            int rank = 1;
            for (TopMayDamEntry entry : filt) {
                Player pl = entry.player;
                TOP t = entry.top;
                w.writeInt(rank++);
                w.writeInt((int) pl.id);
                w.writeShort(pl.getHead());
                if (p.getSession().version >= 214) {
                    w.writeShort(-1);
                }
                w.writeShort(pl.getBody());
                w.writeShort(pl.getLeg());
                w.writeUTF(pl.name != null ? pl.name : "");
                String line1 = (type == 3)
                        ? "LV:" + t.getLevel() + " với " + Util.roundToTwoDecimals(t.getTime() / 1000d) + " giây"
                        : TimeUtil.getTimeLefttop(t.getLasttime(), 0);
                w.writeUTF(line1 != null ? line1 : "");
                String line2 = "...";
                if (type == 3) {
                    line2 = TimeUtil.getTimeLefttop(t.getLasttime(), 0);
                } else if (type <= 2) {
                    String info = t.getInfo2();
                    if (info != null && !info.isEmpty() && !info.equals("...")) {
                        try {
                            long pts = Long.parseLong(info);
                            line2 = "Điểm: " + Util.format(pts);
                        } catch (Exception ignored) {
                            line2 = info;
                        }
                    }
                }
                w.writeUTF(line2 != null ? line2 : "");
            }
            p.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(TopService.class, e);
        } finally {
            msg.cleanup();
        }
    }

    private static class TopMayDamEntry {
        final TOP top;
        final Player player;
        TopMayDamEntry(TOP top, Player player) {
            this.top = top;
            this.player = player;
        }
    }

    public List<TOP> getListTop(Object value) {
        for (TopTemplate template : TOP_TEMPLATE) {
            try {
                int id = Integer.parseInt(value.toString());
                if (template.id == id) {
                    return template.tops;
                }
            } catch (NumberFormatException e) {
                if (template.name.equals(value)) {
                    return template.tops;
                }
            }
        }
        return null;
    }

    public synchronized void loadListTop(Connection con) {
        try {
            for (TopTemplate temp : TOP_TEMPLATE) {
                loadTop(con, temp);
            }
        } catch (Exception e) {
            Logger.logException(getClass(), e);
        }
    }

    public synchronized void loadListTop() {
        try (Connection con = AlyraManager.getConnection()) {
            if (TOP_TEMPLATE.isEmpty()) {
                TopManager.loadTop(con);
            } else {
                for (TopTemplate temp : TOP_TEMPLATE) {
                    loadTop(con, temp);
                }
            }
        } catch (Exception e) {
            Logger.logException(getClass(), e);
        }
    }

    public void loadTop(TopTemplate temp) {
        if (temp == null) {
            return;
        }
        try (Connection con = AlyraManager.getConnection()) {
            loadTop(con, temp);
        } catch (Exception e) {
            Logger.logException(getClass(), e);
        }
    }

    public static void syncOnlinePlayersForTop(Connection con, int topId) {
        try {
            List<Player> players = new ArrayList<>(Client.gI().getPlayers());
            if (players.isEmpty()) {
                return;
            }
            switch (topId) {
                case 0 -> {
                    String sql = "UPDATE account SET danap = GREATEST(danap, ?) WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        for (Player pl : players) {
                            if (pl == null || pl.isBot || pl.getSession() == null || pl.getSession().userId <= 0) {
                                continue;
                            }
                            ps.setLong(1, pl.getSession().danap);
                            ps.setInt(2, pl.getSession().userId);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                case 1 -> {
                    String sql = "UPDATE player SET data_point = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        for (Player pl : players) {
                            if (pl == null || pl.isBot || pl.getSession() == null || pl.nPoint == null) {
                                continue;
                            }
                            JSONArray dataArray = new JSONArray();
                            dataArray.add(pl.nPoint.limitPower);
                            dataArray.add(pl.nPoint.power);
                            dataArray.add(pl.nPoint.tiemNang);
                            dataArray.add(pl.nPoint.stamina);
                            dataArray.add(pl.nPoint.maxStamina);
                            dataArray.add(pl.nPoint.hpg);
                            dataArray.add(pl.nPoint.mpg);
                            dataArray.add(pl.nPoint.dameg);
                            dataArray.add(pl.nPoint.defg);
                            dataArray.add(pl.nPoint.critg);
                            dataArray.add(0);
                            dataArray.add(pl.nPoint.hp);
                            dataArray.add(pl.nPoint.mp);
                            ps.setString(1, dataArray.toJSONString());
                            ps.setLong(2, pl.id);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                case 2 -> {
                    String sql = "UPDATE player SET data_task = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        for (Player pl : players) {
                            if (pl == null || pl.isBot || pl.getSession() == null || pl.playerTask == null || pl.playerTask.taskMain == null) {
                                continue;
                            }
                            JSONArray dataArray = new JSONArray();
                            dataArray.add(pl.playerTask.taskMain.id);
                            dataArray.add(pl.playerTask.taskMain.index);
                            short count = 0;
                            if (pl.playerTask.taskMain.subTasks != null
                                    && pl.playerTask.taskMain.index >= 0
                                    && pl.playerTask.taskMain.index < pl.playerTask.taskMain.subTasks.size()) {
                                count = pl.playerTask.taskMain.subTasks.get(pl.playerTask.taskMain.index).count;
                            }
                            dataArray.add(count);
                            dataArray.add(pl.playerTask.taskMain.lastTime);
                            ps.setString(1, dataArray.toJSONString());
                            ps.setLong(2, pl.id);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(TopService.class, e, "Lỗi syncOnlinePlayersForTop id: " + topId);
        }
    }

    public synchronized void loadTop(Connection con, TopTemplate temp) {
        if (temp == null) {
            return;
        }
        syncOnlinePlayersForTop(con, temp.id);
        temp.tops.forEach(TOP::dispose);
        temp.tops.clear();
        try (PreparedStatement ps = con.prepareStatement(temp.query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    int pId = rs.getInt("id");
                    TOP top = TOP.builder().id_player(pId).build().findPlayer(con, pId);
                    if (top.getPlayer() == null) {
                        continue; // người chơi không tồn tại (đã xóa) -> bỏ qua hàng này, KHÔNG làm hỏng cả bảng
                    }
                    switch (temp.id) {
                        case 0 -> {
                            long tongnap = rs.getLong("tongnap");
                            if (top.getPlayer() != null && top.getPlayer().getSession() != null) {
                                tongnap = Math.max(tongnap, (long) top.getPlayer().getSession().danap);
                            }
                            top.setValue(tongnap);
                            top.setInfo1(Util.powerToString(tongnap) + "$").setInfo2(Util.format(tongnap) + "\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                        }
                        case 1 -> {
                            top.getPoint(rs);
                            long power = (top.getPlayer() != null && top.getPlayer().nPoint != null)
                                    ? top.getPlayer().nPoint.power : rs.getLong("sm");
                            top.setValue(power);
                            top.setInfo1(Util.powerToString(power)).setInfo2(Util.format(power) + "\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                        }
                        case 2 -> {
                            top.getTask(rs);
                            String nvInfo1 = "Nhiệm vụ";
                            String nvInfo2 = "|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm");
                            try {
                                if (top.getPlayer().playerTask != null && top.getPlayer().playerTask.taskMain != null) {
                                    models.task.TaskMain tm = top.getPlayer().playerTask.taskMain;
                                    nvInfo1 = tm.id + "\n" + (tm.name != null ? tm.name : "");
                                    String subName = (tm.subTasks != null && tm.index >= 0 && tm.index < tm.subTasks.size())
                                            ? tm.subTasks.get(tm.index).name : "";
                                    nvInfo2 = "(" + subName + ")\n|5|Hoàn thành " + Util.msToTime(tm.lastTime) + " trước\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm");
                                }
                            } catch (Exception ignored) {
                            }
                            top.setInfo1(nvInfo1).setInfo2(nvInfo2);
                        }
                        default -> {
                            if (top.getInfo1() == null) {
                                top.setInfo1("Top " + temp.name);
                            }
                            if (top.getInfo2() == null) {
                                top.setInfo2("|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                            }
                        }
                    }
                    temp.tops.add(top);
                } catch (Exception exRow) {
                    Logger.logException(this.getClass(), exRow, "Lỗi Load Top (bỏ qua 1 hàng): " + temp.name);
                }
            }
        } catch (Exception e) {
            Logger.logException(this.getClass(), e, "Lỗi Load Top: " + temp.name + " ID: " + temp.id);
        }

        try {
            ensureOnlinePlayersInTop(temp);
        } catch (Exception e) {
            Logger.logException(this.getClass(), e, "Lỗi ensureOnlinePlayersInTop: " + temp.name);
        }

        try {
            sortTopList(temp);
        } catch (Exception e) {
            Logger.logException(this.getClass(), e, "Lỗi sortTopList: " + temp.name);
        }
    }

    private void ensureOnlinePlayersInTop(TopTemplate temp) {
        if (temp == null || temp.tops == null) {
            return;
        }
        for (Player pl : Client.gI().getPlayers()) {
            if (pl == null || pl.isBot) {
                continue;
            }
            if (pl.getSession() != null && pl.getSession().isAdmin) {
                continue;
            }
            boolean exists = false;
            for (TOP t : temp.tops) {
                if (t != null && (t.getId_player() == pl.id || (t.getPlayer() != null && t.getPlayer().id == pl.id))) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                continue;
            }
            switch (temp.id) {
                case 0 -> {
                    long danap = (pl.getSession() != null) ? pl.getSession().danap : 0;
                    if (danap > 0) {
                        TOP top = TOP.builder().id_player((int) pl.id).player(pl).build();
                        top.setValue(danap);
                        top.setInfo1(Util.powerToString(danap) + "$")
                           .setInfo2(Util.format(danap) + "\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                        temp.tops.add(top);
                    }
                }
                case 1 -> {
                    if (pl.nPoint != null && pl.nPoint.power > 2000) {
                        TOP top = TOP.builder().id_player((int) pl.id).player(pl).build();
                        long power = pl.nPoint.power;
                        top.setValue(power);
                        top.setInfo1(Util.powerToString(power))
                           .setInfo2(Util.format(power) + "\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                        temp.tops.add(top);
                    }
                }
                case 2 -> {
                    if (pl.playerTask != null && pl.playerTask.taskMain != null && pl.playerTask.taskMain.id > 1) {
                        TOP top = TOP.builder().id_player((int) pl.id).player(pl).build();
                        TaskMain tm = pl.playerTask.taskMain;
                        String nvInfo1 = tm.id + "\n" + (tm.name != null ? tm.name : "");
                        String subName = (tm.subTasks != null && tm.index >= 0 && tm.index < tm.subTasks.size())
                                ? tm.subTasks.get(tm.index).name : "";
                        String nvInfo2 = "(" + subName + ")\n|5|Hoàn thành " + Util.msToTime(tm.lastTime) + " trước\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm");
                        top.setInfo1(nvInfo1).setInfo2(nvInfo2);
                        temp.tops.add(top);
                    }
                }
            }
        }
    }

    private void sortTopList(TopTemplate temp) {
        if (temp == null || temp.tops == null || temp.tops.size() <= 1) {
            return;
        }
        switch (temp.id) {
            case 0, 1 -> {
                temp.tops.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            }
            case 2 -> {
                temp.tops.sort((a, b) -> {
                    int nvA = 0, nvB = 0;
                    int nvpA = 0, nvpB = 0;
                    int cntA = 0, cntB = 0;
                    long tA = Long.MAX_VALUE, tB = Long.MAX_VALUE;
                    if (a.getPlayer() != null && a.getPlayer().playerTask != null && a.getPlayer().playerTask.taskMain != null) {
                        TaskMain tmA = a.getPlayer().playerTask.taskMain;
                        nvA = tmA.id;
                        nvpA = tmA.index;
                        if (tmA.subTasks != null && nvpA >= 0 && nvpA < tmA.subTasks.size()) {
                            cntA = tmA.subTasks.get(nvpA).count;
                        }
                        tA = tmA.lastTime;
                    }
                    if (b.getPlayer() != null && b.getPlayer().playerTask != null && b.getPlayer().playerTask.taskMain != null) {
                        TaskMain tmB = b.getPlayer().playerTask.taskMain;
                        nvB = tmB.id;
                        nvpB = tmB.index;
                        if (tmB.subTasks != null && nvpB >= 0 && nvpB < tmB.subTasks.size()) {
                            cntB = tmB.subTasks.get(nvpB).count;
                        }
                        tB = tmB.lastTime;
                    }
                    if (nvA != nvB) return Integer.compare(nvB, nvA);
                    if (nvpA != nvpB) return Integer.compare(nvpB, nvpA);
                    if (cntA != cntB) return Integer.compare(cntB, cntA);
                    return Long.compare(tA, tB);
                });
            }
        }
        while (temp.tops.size() > 100) {
            temp.tops.remove(temp.tops.size() - 1);
        }
    }

    public int getPlayerRank(Player p, List<TOP> tops) {
        if (p == null || tops == null) {
            return -1;
        }
        for (int i = 0; i < tops.size(); i++) {
            TOP top = tops.get(i);
            if (top != null && (top.getId_player() == p.id || (top.getPlayer() != null && top.getPlayer().id == p.id))) {
                return i + 1;
            }
        }
        return -1;
    }

    private static final long CHU_KY_NAP_TOP_MS = 30_000;

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            long lastTime = System.currentTimeMillis();
            if (timeRealTop + CHU_KY_NAP_TOP_MS < lastTime) {
                boolean napDuoc = false;
                try (Connection con = AlyraManager.getConnection()) {
                    topMayDam = realTop(TOP_MAYDAM, con);
                    napDuoc = true;
                } catch (Exception ignored) {
                }
                timeRealTop = napDuoc ? lastTime : lastTime - CHU_KY_NAP_TOP_MS + 5000;
                loadListTop();
                try {
                    TopTrung.getInstance().load(true);
                    TopQuay.getInstance().load(true);
                    TopDauBuoi.getInstance().load(true);
                } catch (Exception ignored) {
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}