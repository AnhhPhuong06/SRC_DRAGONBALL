package network.session;

 
import java.net.Socket;

import models.player.Player;
import server.Controller;
import data.DataGame;
import database.daos.NDVSqlFetcher;
import models.item.Item;
import java.io.IOException;

import network.io.Message;
import server.Client;
import server.Maintenance;
import server.Manager;
import models.AntiLogin;
import services.Service;
import services.ClientVersionPolicy;
import utils.Logger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import utils.TimeUtil;

public class MySession extends Session {

    private static final Map<String, AntiLogin> ANTILOGIN = new ConcurrentHashMap<>();
    public Player player;

    public byte timeWait = 100;
    public boolean sentKey;

    public static final byte[] KEYS = {0};
    public byte curR, curW;

    public String ipAddress;
    public boolean isAdmin;
    public int userId;
    public String uu;
    public String pp;

    public int typeClient;
    public byte zoomLevel;

    public long lastTimeLogout;
    public boolean joinedGame;
    public boolean enteredMap;
    public long lastTimeReadMessage;
    private final AtomicBoolean kicked = new AtomicBoolean(false);

    /** @return true đúng lần đầu tiên — các lần sau trả false để bỏ qua. */
    public boolean markKicked() {
        return kicked.compareAndSet(false, true);
    }
    public boolean actived;
    public boolean hasReceivedVIP;
    public boolean hasReceivedVIP1;
    public boolean hasReceivedVIP2;
    public long lastTimeReceivedVIP;
    public long lastTimeReceivedVIP1;
    public long lastTimeReceivedVIP2;

    public boolean check;

    public int goldBar;
    public long gold;
    public int eventPoint;
    public int phieu;
    public List<Item> itemsReward;
    public String dataReward;
    public boolean is_gift_box;
    public double bdPlayer;

    public int version;
    private volatile String reportedClientVersion;
    private volatile boolean loginVersionVerified;

    public boolean reportClientVersion(String releaseVersion) {
        this.reportedClientVersion = releaseVersion;
        this.loginVersionVerified = false;
        this.version = ClientVersionPolicy.isAllowed(releaseVersion) ? ClientVersionPolicy.PROTOCOL_VERSION : 0;
        return ClientVersionPolicy.isAllowed(releaseVersion);
    }

    public boolean hasAllowedClientVersion() {
        return ClientVersionPolicy.isAllowed(reportedClientVersion) && loginVersionVerified;
    }

    public void login(String username, String password, String releaseVersion) {
        this.loginVersionVerified = ClientVersionPolicy.isAllowed(reportedClientVersion)
                && ClientVersionPolicy.isAllowed(releaseVersion);
        login(username, password);
    }
    public int cash;
    public int danap;
    //Mốc Nạp
    public int mocNap;
    
    public int diemReceive;
    public int sotien;
    public int vip;
    public int vip1;
    public int vip2;
    public int luotquay;

    public boolean finishUpdate;
    public final java.util.concurrent.atomic.AtomicBoolean countedConnection = new java.util.concurrent.atomic.AtomicBoolean();

// Login/edit coordination uses bounded striped locks: collisions only serialize unrelated
    // accounts; a user-controlled username can never grow an unbounded lock map.
    private static final Object[] ACCOUNT_LOCKS = new Object[256];
    static {
        for (int i = 0; i < ACCOUNT_LOCKS.length; i++) ACCOUNT_LOCKS[i] = new Object();
    }

    /** Same bounded lock used by the full login activation and the web panel. */
    public static Object accountLock(String username) {
        String key = username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT);
        return ACCOUNT_LOCKS[(key.hashCode() & 0x7fffffff) % ACCOUNT_LOCKS.length];
    }

    public MySession(Socket socket) {
        super(socket);
        ipAddress = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void sendKey() throws Exception {
        super.sendKey();
        this.startSend();
    }

    public void sendSessionKey() {
        Message msg = new Message(-27);
        try {
            msg.writer().writeByte(KEYS.length);
            msg.writer().writeByte(KEYS[0]);
            for (int i = 1; i < KEYS.length; i++) {
                msg.writer().writeByte(KEYS[i] ^ KEYS[i - 1]);
            }
            this.sendMessage(msg);
            msg.cleanup();
            sentKey = true;
        } catch (IOException e) {
        }
    }

    public void login(String username, String password) {
        if (!hasAllowedClientVersion()) {
            Service.gI().sendThongBaoOK(this, ClientVersionPolicy.DENIED_MESSAGE);
            return;
        }
        try (var ignored = database.DatabaseResetGuard.GLOBAL.read()) {
            loginWithDatabaseLease(username, password);
        } catch (java.sql.SQLException e) {
            Service.gI().sendThongBaoOK(this, e.getMessage());
        }
    }

    private void loginWithDatabaseLease(String username, String password) {
        String ipKey = this.ipAddress != null ? this.ipAddress : "unknown";
        AntiLogin al = ANTILOGIN.computeIfAbsent(ipKey, k -> new AntiLogin());
        if (!al.canLogin()) {
            Service.gI().sendThongBaoOK(this, al.getNotifyCannotLogin());
            return;
        }

        if (Manager.LOCAL) {
            Service.gI().sendThongBaoOK(this, "Server này chỉ để lưu dữ liệu\nVui lòng qua server khác");
            return;
        }
        if (Maintenance.isRunning) {
            Service.gI().sendThongBaoOK(this, "Server đang trong thời gian bảo trì, vui lòng quay lại sau");
            return;
        }

        // Đếm người THẬT: bot cũng nằm trong danh sách online, tính cả bot vào đây thì
        // đàn bot sẽ chiếm hết chỗ và chặn người chơi thật đăng nhập.
        if (!this.isAdmin && Client.gI().getRealPlayerCount() >= Manager.MAX_PLAYER) {
            Service.gI().sendThongBaoOK(this, "Máy chủ hiện đang quá tải, "
                    + "cư dân vui lòng di chuyển sang máy chủ khác.");
            return;
        }

        Object lock = accountLock(username);

        // Đồng bộ hóa theo tài khoản (khóa theo username)
        synchronized (lock) {
            if (this.player == null) {
                Player pl = null;
                try {
                    long st = System.currentTimeMillis();
                    this.uu = username;
                    this.pp = password;
                    pl = NDVSqlFetcher.login(this, al);
                    if (pl != null) {
                        // -77 max small
                        DataGame.sendSmallVersion(this);
                        // -93 bgitem version
                        DataGame.sendBgItemVersion(this);

                        this.timeWait = 0;
                        this.joinedGame = true;
                        pl.nPoint.calPoint();
                        pl.nPoint.setHp(pl.nPoint.hp);
                        pl.nPoint.setMp(pl.nPoint.mp);
                        pl.zone.addPlayer(pl);
                        if (pl.pet != null) {
                            pl.pet.nPoint.calPoint();
                            pl.pet.nPoint.setHp(pl.pet.nPoint.hp);
                            pl.pet.nPoint.setMp(pl.pet.nPoint.mp);
                        }

                        pl.setSession(this);
                        Client.gI().put(pl);
                        this.player = pl;
                        services.func.GiftProgressService.gI().onLogin(pl);
                        //-28 -4 version data game
                        DataGame.sendVersionGame(this);
                        //-31 data item background
                        DataGame.sendDataItemBG(this);
                        Controller.gI().sendInfo(this);
                        Logger.log("[" + TimeUtil.getCurrHour() + ":" + TimeUtil.getCurrMin() + "] - Player Login: " + this.player.name + "\n");
                        if (this.player.notify != null && !this.player.notify.equals("null") && !this.player.notify.isEmpty() && this.player.notify.length() > 0) {
                            Service.gI().sendThongBao(this.player, this.player.notify);
                            this.player.notify = null;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (pl != null) {
                        pl.dispose();
                    }
                }
            }
        }
    }
}
