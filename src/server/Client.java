package server;

 


import database.AlyraManager;
import database.daos.PlayerDAO;
import lombok.Getter;
import models.map.ItemMap;
import models.player.Player;
import network.session.SessionManager;
import interfaces.ISession;
import java.sql.SQLException;
import network.session.MySession;
import services.Service;
import services.map.ChangeMapService;
import services.func.SummonDragon;
import services.func.TransactionService;
import services.dungeon.NgocRongNamecService;
import utils.Logger;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import services.func.SummonDragonNamek;

public class Client implements Runnable {

    private static Client instance;

    /** Cờ bật/tắt tự dọn session/socket chết — panel Dashboard điều khiển. */
    public static boolean AUTO_CLEAR_DEAD_SESSION = false;

    // ConcurrentHashMap: bot được thêm/bớt từ luồng "Bot-Spawn" trong khi người chơi
    // thật đăng nhập/thoát trên luồng mạng — HashMap thường bị ghi song song sẽ hỏng.
    private final Map<Long, Player> players_id = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Player> players_userId = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Player> players_name = new java.util.concurrent.ConcurrentHashMap<>();
    @Getter
    private final List<Player> players = new CopyOnWriteArrayList<>();

    private Client() {
        new Thread(this, "Update Client").start();
    }

    public static Client gI() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }
    public Player getPlayerByUserName(String username) {
        if (username == null) return null;
        String u = username.trim();
        for (Player player : players) {
            if (player != null && player.getSession() != null && player.getSession().uu != null
                    && player.getSession().uu.equalsIgnoreCase(u)) {
                return player;
            }
        }
        return null;
    }

    public void put(Player player) {
        if (player == null) {
            return;
        }
        this.players_id.putIfAbsent(player.id, player);
        if (player.name != null) {
            this.players_name.putIfAbsent(player.name, player);
        }
        if (player.getSession() != null) {
            this.players_userId.putIfAbsent(player.getSession().userId, player);
        }
        if (!this.players.contains(player)) {
            this.players.add(player);
        }
    }


    private void remove(MySession session) {
        if (session.player != null) {
            this.remove(session.player);
            session.player.dispose();
        }

        if (session.joinedGame) {
            session.joinedGame = false;
            try {
                CompletableFuture.runAsync(() -> {
                    try {
                        AlyraManager.executeUpdate("update account set last_time_logout = ? where id = ?", new Timestamp(System.currentTimeMillis()), session.userId);
                     } catch (SQLException e) {
                        e.printStackTrace();
                        Logger.logException(Client.class, e, "Lỗi khi cập nhật cơ sở dữ liệu bất đồng bộ");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Logger.logException(Client.class, e, "Lỗi khi cập nhật cơ sở dữ liệu bất đồng bộ");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Logger.logException(Client.class, e, "Lỗi khi lên lịch cơ sở dữ liệu bất đồng bộ");
            }
        }

        ServerManager.gI().disconnect(session);
    }

    public void remove(Player player) {
        // Phiên cũ/clone không được xoá phiên chính hoặc lưu dữ liệu cũ đè lên DB.
        if (player == null || player.getSession() == null) return;
        if (this.players_id.get(player.id) != player
                || this.players_userId.get(player.getSession().userId) != player) {
            this.players.remove(player);
            return;
        }
        this.players_id.remove(player.id, player);
        if (player.name != null) {
            this.players_name.remove(player.name, player);
        }
        this.players_userId.remove(player.getSession().userId, player);
        this.players.remove(player);
        if (!player.beforeDispose) {
            player.beforeDispose = true;
            // Xoá phiên xác thực admin: đăng nhập lại phải nhập mật khẩu lệnh lại từ đầu.
            Command.gI().clearAuth(player);
            if (player.zone != null) {
                player.mapIdBeforeLogout = player.zone.map.mapId;
            }
            if (player.idNRNM != -1) {
                ItemMap itemMap = new ItemMap(player.zone, player.idNRNM, 1, player.location.x, player.location.y, -1);
                Service.gI().dropItemMap(player.zone, itemMap);
                NgocRongNamecService.gI().pNrNamec[player.idNRNM - 353] = "";
                NgocRongNamecService.gI().idpNrNamec[player.idNRNM - 353] = -1;
                player.idNRNM = -1;
            }
            ChangeMapService.gI().exitMap(player);
            TransactionService.gI().cancelTrade(player);
            if (player.clan != null) {
                services.tournament.GuruGuildWarService.gI().onPlayerDisconnect(player);
                player.clan.removeMemberOnline(null, player);
            }
            if (SummonDragon.gI().playerSummonShenron != null
                    && SummonDragon.gI().playerSummonShenron.id == player.id) {
                SummonDragon.gI().isPlayerDisconnect = true;
            }
            if (SummonDragonNamek.gI().playerSummonShenron != null
                    && SummonDragonNamek.gI().playerSummonShenron.id == player.id) {
                SummonDragonNamek.gI().isPlayerDisconnect = true;
            }
            if (player.shenronEvent != null) {
                player.shenronEvent.isPlayerDisconnect = true;
            }
            try {
                models.npc.npc_list.DuongTang.donKhiThoatGame(player.id);
            } catch (Exception ignored) {
            }
            if (player.mobMe != null) {
                player.mobMe.mobMeDie();
            }
            if (player.pet != null) {
                if (player.pet.mobMe != null) {
                    player.pet.mobMe.mobMeDie();
                }
                ChangeMapService.gI().exitMap(player.pet);
            }
        }
        PlayerDAO.updatePlayer(player);
        services.func.GiftProgressService.gI().onLogout(player);
    }

    public void kickSession(MySession session) {
        if (session != null && session.markKicked()) {
            try {
                this.remove(session);
            } finally {
                session.disconnect();
            }
        }
    }

    /**
     * Đưa bot vào đúng danh sách người chơi online để nó được đếm và tra cứu như
     * người thật. KHÔNG đụng {@code players_userId} vì bot không có session.
     *
     * <p>An toàn được nhờ hai chốt chặn: {@code PlayerDAO.updatePlayer} bỏ qua bot
     * (không ghi DB), và {@link #getRealPlayerCount()} dùng cho chỗ kiểm tra sức chứa.
     */
    public void putBot(Player bot) {
        if (bot == null || !bot.isBot) {
            return;
        }
        this.players_id.putIfAbsent(bot.id, bot);
        if (bot.name != null) {
            // putIfAbsent: người chơi thật trùng tên vẫn giữ nguyên ô của mình
            this.players_name.putIfAbsent(bot.name, bot);
        }
        if (!this.players.contains(bot)) {
            this.players.add(bot);
        }
    }


    public void removeBot(Player bot) {
        if (bot == null) {
            return;
        }
        // Chỉ xoá nếu ô đó đang trỏ ĐÚNG con bot này: tên bot sinh ngẫu nhiên có thể
        // trùng tên người chơi thật, xoá mù sẽ hất người thật ra khỏi danh sách.
        this.players_id.remove(bot.id, bot);
        if (bot.name != null) {
            this.players_name.remove(bot.name, bot);
        }
        this.players.remove(bot);
    }

    /** Số người online (đã gồm bot vì bot nằm trong {@link #players}). */
    public int getOnlineCount() {
        return this.players.size();
    }

    /** CHỈ người chơi thật — dùng cho giới hạn sức chứa, đừng để bot chặn người thật vào game. */
    public int getRealPlayerCount() {
        int n = 0;
        for (Player p : this.players) {
            if (p != null && !p.isBot) {
                n++;
            }
        }
        return n;
    }

    public Player getPlayer(long playerId) {
        return this.players_id.get(playerId);
    }

    public Player getPlayerByUser(int userId) {
        return this.players_userId.get(userId);
    }

    public Player getPlayer(String name) {
        if (name == null) return null;
        Player p = this.players_name.get(name);
        if (p != null) return p;
        String n = name.trim();
        for (Player pl : players) {
            if (pl != null && pl.name != null && pl.name.equalsIgnoreCase(n)) {
                return pl;
            }
        }
        return null;
    }

    public void close() {
        Logger.log(Logger.YELLOW, "BEGIN KICK OUT SESSION " + players.size() + "\n");
        List<Player> snap = new java.util.ArrayList<>(this.players);
        this.players.clear();
        for (Player pl : snap) {
            if (pl != null && pl.getSession() != null) {
                this.kickSession(pl.getSession());
            }
        }
        Logger.success("SUCCESSFUL\n");
    }
   public void cloneMySessionNotConnect() {
        Logger.error("---------START KICK SESSION DDOS---------");
        Logger.error("COUNT: " + SessionManager.gI().getSessions().size());

        for (ISession s : SessionManager.gI().getSessions()) {
            if (s instanceof MySession m && m.player == null) {
                SessionManager.gI().removeSession(m);
                this.kickSession(m);
            }
        }

        Logger.error("---------SUCCESSFUL---------\n");
    } 
    private static final long DEAD_SESSION_TIMEOUT_MS = 120_000;
    private void update() {
        long now = System.currentTimeMillis();
        for (ISession s : SessionManager.gI().getSessions()) {
            if (s == null) {
                SessionManager.gI().removeSession(s);
                continue;
            }
            if (!(s instanceof MySession session)) {
                continue;
            }
            if (session.timeWait > 0) {
                session.timeWait--;
                if (session.timeWait == 0) {
                    kickSession(session);
                }
            } else if (AUTO_CLEAR_DEAD_SESSION && session.player != null
                    && session.lastTimeReadMessage > 0
                    && now - session.lastTimeReadMessage > DEAD_SESSION_TIMEOUT_MS) {
                kickSession(session);
            }
        }
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            long st = System.currentTimeMillis();
            try {
                update();
            } catch (Exception e) {
                e.printStackTrace();
            }
            long timeDo = System.currentTimeMillis() - st;
            if (1000 - timeDo > 0) {
                try {
                    Thread.sleep(1000 - timeDo);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
