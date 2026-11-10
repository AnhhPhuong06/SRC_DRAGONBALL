package server;

 
import Bot.BotManager;
import ui.AdminPanel;
import database.AlyraManager;
import database.AlyraResultSet;
import utils.FileRunner;
import managers.boss.BrolyManager;
import database.daos.HistoryTransactionDAO;
import managers.boss.BossManager;
import managers.boss.OtherBossManager;
import managers.boss.TreasureUnderSeaManager;
import managers.boss.SnakeWayManager;
import managers.boss.RedRibbonHQManager;
import managers.boss.GasDestroyManager;
import managers.boss.YardartManager;
import managers.boss.ChristmasEventManager;
import managers.boss.FinalBossManager;
import managers.boss.Boss12hManager;
import managers.boss.Boss14hManager;
import managers.boss.HitEventManager;
import managers.boss.HungVuongEventManager;
import managers.boss.LunarNewYearEventManager;
import managers.boss.SkillSummonedManager;
import managers.boss.TrungThuEventManager;
import java.io.IOException;
import interfaces.ISession;
import network.Network;
import network.io.MyKeyHandler;
import network.session.MySession;
import services.player.ClanService;
import services.dungeon.NgocRongNamecService;
import services.func.minigame.CSMM;
import utils.Logger;
import utils.TimeUtil;

import java.util.*;
import managers.tournament.The23rdMartialArtCongressManager;
import managers.tournament.DeathOrAliveArenaManager;
import event.EventManager;
import database.daos.EventDAO;
import database.daos.PlayerDAO;
import managers.tournament.WorldMartialArtsTournamentManager;
import network.io.MessageSendCollect;
import managers.ShenronEventManager;
import interfaces.ISessionAcceptHandler;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;
import managers.ConsignShopManager;
import managers.SuperRankManager;
import managers.boss.Bktne;
import managers.boss.TeaEventManager;
import models.npc.TaiXiu;
import models.npc.TaiXiuXocDiaProLuxuryVip999;
import models.player.Player;
import network.TcpKickServer;
import services.Service;
import services.top.TopAutoService;
import services.top.TopService;

public class ServerManager {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static String timeStart;

    public static long timeStartMillis;

    public static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> CLIENTS = new java.util.concurrent.ConcurrentHashMap<>();

    public static String NAME = "Vũ trụ 1";
    public static String IP = "221.132.33.173";
    public static int PORT = 14446;

    private static ServerManager instance;

    public static boolean isRunning;

    public void init() {
        Manager.gI();
        HistoryTransactionDAO.deleteHistory();
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    private static String[] startupArguments = new String[0];

    public static void main(String[] args) {
        startupArguments = args.clone();
        timeStartMillis = System.currentTimeMillis();
        timeStart = TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
        java.util.List<String> argList = Arrays.asList(args);
        if (argList.contains("-web")) {
            if (argList.contains("-oldpanel")) {
                throw new IllegalArgumentException("Không dùng đồng thời -web và -oldpanel");
            }
            System.setProperty("java.awt.headless", "true");
            ServerManager.gI().run();
        } else if (argList.contains("-nogui")) {
            // Chạy engine headless, không giao diện
            ServerManager.gI().run();
        } else if (argList.contains("-oldpanel")) {
            // Dùng lại bảng điều khiển cũ (AdminPanel)
            AdminPanel.launch();
            ServerManager.gI().run();
        } else {
            // Mặc định: bảng điều khiển mới — ServerManagerUI tự khởi động engine (run) ở luồng nền
            javax.swing.SwingUtilities.invokeLater(() -> new server.ui.ServerManagerUI());
        }
    }

    /**
     * Khởi động một luồng nền, hỏng thì bỏ qua chứ không kéo chết cả chuỗi.
     *
     * <p>Trước đây các luồng được start liền một mạch: chỉ cần một class thiếu trong
     * bản build (vd {@code NoClassDefFoundError: LunarNewYearEventManager}) là toàn bộ
     * luồng phía SAU không bao giờ chạy — kể cả "Thread Bot Game", nên bot tạo ra rồi
     * đứng im vì không ai gọi update.
     */
    private void khoiDongLuong(Runnable r, String ten) {
        try {
            new Thread(r, ten).start();
        } catch (Throwable t) {
            Logger.error("[KHOI DONG] Bỏ qua luồng \"" + ten + "\": " + t + "\n");
        }
    }

    public void run() {
        isRunning = true;
        activeServerSocket();
        activeCommandLine();
        services.CombineRateConfig.init();

        services.VongQuayThoiVangConfig.init();
        khoiDongLuong(() -> server.web.WebAdminServer.gI().start(), "Web Admin Panel");
        new Thread(NgocRongNamecService.gI(), "Update NRNM").start();
        new Thread(SuperRankManager.gI(), "Update Super Rank").start();
        new Thread(The23rdMartialArtCongressManager.gI(), "Update DHVT23").start();
        new Thread(DeathOrAliveArenaManager.gI(), "Update Võ Đài Sinh Tử").start();
        new Thread(WorldMartialArtsTournamentManager.gI(), "Update WMAT").start();
        khoiDongLuong(managers.tournament.GuruGuildWarManager.gI(), "Update Guru Guild War");
        khoiDongLuong(AutoMaintenance.gI(), "Update Bảo Trì Tự Động");
        khoiDongLuong(ShenronEventManager.gI(), "Update Shenron");
        BossManager.gI().loadBoss();
        Manager.MAPS.forEach(models.map.Map::initBoss);
        EventManager.gI().init();
        khoiDongLuong(BossManager.gI(), "Update boss");
        khoiDongLuong(YardartManager.gI(), "Update yardart boss");
        khoiDongLuong(FinalBossManager.gI(), "Update final boss");
        khoiDongLuong(Boss12hManager.gI(), "Update boss 12h");
        khoiDongLuong(Boss14hManager.gI(), "Update boss 14h");
        khoiDongLuong(SkillSummonedManager.gI(), "Update Skill-summoned boss");
        khoiDongLuong(BrolyManager.gI(), "Update broly boss");
        khoiDongLuong(OtherBossManager.gI(), "Update other boss");
        khoiDongLuong(RedRibbonHQManager.gI(), "Update reb ribbon hq boss");
        khoiDongLuong(TreasureUnderSeaManager.gI(), "Update treasure under sea boss");
        khoiDongLuong(SnakeWayManager.gI(), "Update snake way boss");
        khoiDongLuong(GasDestroyManager.gI(), "Update gas destroy boss");
        khoiDongLuong(TrungThuEventManager.gI(), "Update trung thu event boss");
        khoiDongLuong(HitEventManager.gI(), "Update hit event boss");
        khoiDongLuong(ChristmasEventManager.gI(), "Update christmas event boss");
        khoiDongLuong(HungVuongEventManager.gI(), "Update Hung Vuong event boss");
        khoiDongLuong(LunarNewYearEventManager.gI(), "Update lunar new year event boss");
        khoiDongLuong(TeaEventManager.gI(), "Update teamobi event boss");
        khoiDongLuong(BotManager.gI(), "Thread Bot Game");
        khoiDongLuong(TopService.gI(), "Thread TOP");
        khoiDongLuong(TaiXiu.gI(), "Thread TaiXiu");
        khoiDongLuong(TaiXiuXocDiaProLuxuryVip999.gI(), "Thread XocDia");
        khoiDongLuong(Bktne.gI(), "Update Bkt Ne event boss");
        khoiDongLuong(CSMM.gI(), "Thread CSMM"); 
        new Thread(() -> {
            while (!Maintenance.isRunning) {
                TopService.gI().loadListTop();
                try {
                    TimeUnit.SECONDS.sleep(60);
                } catch (InterruptedException e) {
                }
            }
        }, "Update Top").start();
        TcpKickServer tcpServer = new TcpKickServer(4425);
        tcpServer.start();
        TopAutoService.gI().activeAuto();
        AutoSaveManager.getInstance().startAutoSave();
    }
 
    private void activeServerSocket() {
        try {
            Network.gI().init().setAcceptHandler(new ISessionAcceptHandler() {
                @Override
                public void sessionInit(ISession is) {
                    if (!canConnectWithIp(is.getIP())) {
                        is.disconnect();
                        return;
                    }
                    ((MySession) is).countedConnection.set(true);
                    is.setMessageHandler(Controller.gI())
                            .setSendCollect(new MessageSendCollect())
                            .setKeyHandler(new MyKeyHandler())
                            .startCollect().startQueueHandler();
                }

                @Override
                public void sessionDisconnect(ISession session) {
                    Client.gI().kickSession((MySession) session);
                }
            }).setTypeSessionClone(MySession.class)
                    .setDoSomeThingWhenClose(() -> {
                        Logger.error("SERVER CLOSE\n");
                        System.exit(0);
                    })
                    .start(PORT);
        } catch (Exception e) {
        }
    }

    private boolean canConnectWithIp(String ipAddress) {
        if (ipAddress == null) {
            return true;
        }
        boolean[] admitted = {false};
        CLIENTS.compute(ipAddress, (key, count) -> {
            if (count == null) count = new java.util.concurrent.atomic.AtomicInteger();
            if (!Manager.ENABLE_IP_LIMIT || count.get() < Manager.MAX_PER_IP) {
                count.incrementAndGet();
                admitted[0] = true;
            }
            return count;
        });
        return admitted[0];
    }

    public void disconnect(MySession session) {
        if (session != null && session.ipAddress != null && session.countedConnection.compareAndSet(true, false)) {
            CLIENTS.computeIfPresent(session.ipAddress, (k, count) -> {
                int c = count.decrementAndGet();
                return (c <= 0) ? null : count;
            });
        }
    }

    public void close() {
        isRunning = false;
        try {
            ClanService.gI().close();
        } catch (Exception e) {
            Logger.error("Lỗi save clan!\n");
        }
        Client.gI().close();
        ConsignShopManager.gI().save();
        database.daos.PlayerGiftDAO.flushDirty(); // quà/nhiệm vụ: cứu mọi state chưa kịp ghi
        EventDAO.save();
        Logger.success("SUCCESSFULLY MAINTENANCE!\n");

        if (AutoMaintenance.isRunning) {
            AutoMaintenance.isRunning = false;
            try {
                FileRunner.restartServer(startupArguments);
            } catch (IOException e) {
            }
        }
        System.exit(0);
    }

    private void activeCommandLine() {

        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (true) {
                String line = sc.nextLine();
                if (line.equals("baotri")) {
                    Maintenance.gI().start(5);
                } else if (line.equals("athread")) {
                   Logger.error("TIEN DUNG DEP TRAI DEBUG SERVER: " + Thread.activeCount());
                } else if (line.equals("nplayer")) {
                    Logger.error("Player in game: " + Client.gI().getPlayers().size() + "\n");
                } else if (line.equals("admin")) {
                    new Thread(() -> {
                        Client.gI().close();
                    }).start();
                } else if (line.startsWith("kick")) {
                    new Thread(() -> {
                        try {

                            Client.gI().cloneMySessionNotConnect();
                            try {
                                Logger.error("Đang tiến hành lưu data người dùng");
                                for (Player pl : Client.gI().getPlayers()) {
                                    try {
                                        if (pl != null && pl.isPl()) {
                                            PlayerDAO.updatePlayer(pl);
                                            Thread.sleep(50);
                                        }
                                    } catch (Exception exx) {
                                        exx.printStackTrace();
                                    }
                                }
                                Thread.sleep(1000);
                                Logger.success("Lưu dữ liệu người dùng thành công");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Logger.error("Lỗi lưu dữ liệu người dùng");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.error("Thông báo: Lỗi khi kick ss.\n");
                        }
                    }).start();
                } else if (line.startsWith("bang")) {
                    new Thread(() -> {
                        try {
                            ClanService.gI().close();
                            Logger.error("Save " + Manager.CLANS.size() + " bang");
                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.error("Thông báo: lỗi lưu dữ liệu bang hội.\n");
                        }
                    }).start();
                } else if (line.startsWith("a")) {
                    String a = line.replace("a ", "");
                    Service.gI().sendThongBaoAllPlayer(a);
                }
            }
        }, "Active line").start();
    }
}
