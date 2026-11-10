package database.daos;


import database.AlyraManager;
import database.AlyraResultSet;
import models.item.Item;
import models.item.ItemTime;
import models.player.Friend;
import models.player.Fusion;
import models.player.Inventory;
import models.player.Player;
import models.skill.Skill;
import services.map.MapService;
import utils.Logger;
import network.session.MySession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import lombok.extern.java.Log;
import models.Template;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import services.func.GiftProgressService;
import services.map.ChangeMapService;
import utils.TimeUtil;
import utils.Util;
import server.Client;
import services.Service;

public class PlayerDAO {

    private static final Object CREATE_LOCK = new Object();

    public enum CreateResult { CREATED, ALREADY_EXISTS, ERROR }

    public static CreateResult createNewPlayer(int userId, String name, byte gender, int hair) {
        synchronized (CREATE_LOCK) {
            AlyraResultSet exist = null;
            try {
                exist = AlyraManager.executeQuery("select id from player where account_id = ? limit 1", userId);
                if (exist.first()) {
                    Logger.warning("Account " + userId + " đã có nhân vật, bỏ qua tạo mới.\n");
                    return CreateResult.ALREADY_EXISTS;
                }
            } catch (Exception ex) {
                Logger.logException(PlayerDAO.class, ex, "Lỗi kiểm tra account trước khi tạo player");
                return CreateResult.ERROR;
            } finally {
                if (exist != null) {
                    exist.dispose();
                }
            }
        try {
            JSONArray dataArray = new JSONArray();

            dataArray.add(200000); //vàng
            dataArray.add(200000); //ngọc xanh
            dataArray.add(10); //hồng ngọc
            dataArray.add(0); //point
            dataArray.add(0); //event

            String inventory = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(39 + gender); //map
            dataArray.add(100); //x
            dataArray.add(384); //y
            String location = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0); //giới hạn sức mạnh
            dataArray.add(2000); //sức mạnh
            dataArray.add(2000); //tiềm năng
            dataArray.add(1000); //thể lực
            dataArray.add(1000); //thể lực đầy
            dataArray.add(gender == 0 ? 200 : 100); //hp gốc
            dataArray.add(gender == 1 ? 200 : 100); //ki gốc
            dataArray.add(gender == 2 ? 15 : 10); //sức đánh gốc
            dataArray.add(0); //giáp gốc
            dataArray.add(0); //chí mạng gốc
            dataArray.add(0); //năng động
            dataArray.add(gender == 0 ? 200 : 100); //hp hiện tại
            dataArray.add(gender == 1 ? 200 : 100); //ki hiện tại
            String point = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(1); //level
            dataArray.add(5); //curent pea
            dataArray.add(0); //is upgrade
            dataArray.add(new Date().getTime()); //last time harvest
            dataArray.add(new Date().getTime()); //last time upgrade
            String magicTree = dataArray.toJSONString();
            dataArray.clear();
            /**
             *
             * [
             * {"temp_id":"1","option":[[5,7],[7,3]],"create_time":"49238749283748957""},
             * {"temp_id":"1","option":[[5,7],[7,3]],"create_time":"49238749283748957""},
             * {"temp_id":"-1","option":[],"create_time":"0""}, ... ]
             */

            int idAo = gender == 0 ? 0 : gender == 1 ? 1 : 2;
            int idQuan = gender == 0 ? 6 : gender == 1 ? 7 : 8;
            int def = gender == 2 ? 3 : 2;
            int hp = gender == 0 ? 30 : 20;

            JSONArray item = new JSONArray();
            JSONArray options = new JSONArray();
            JSONArray opt = new JSONArray();
            for (int i = 0; i < 12; i++) {
                switch (i) {
                    case 0:
                        //áo
                        opt.add(47); //id option
                        opt.add(def); //param option
                        item.add(idAo); //id item
                        item.add(1); //số lượng
                        options.add(opt.toJSONString());
                        opt.clear();
                        break;
                    case 1:
                        //quần
                        opt.add(6); //id option
                        opt.add(hp); //param option
                        item.add(idQuan); //id item
                        item.add(1); //số lượng
                        options.add(opt.toJSONString());
                        opt.clear();
                        break;
                    default:
                        item.add(-1); //id item
                        item.add(0); //số lượng
                        break;
                }
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBody = dataArray.toJSONString();
            dataArray.clear();

            for (int i = 0; i < 50; i++) {
                if (i == 0) { //thỏi vàng
                    opt.add(1); //id option
                    opt.add(500); //param option
                    item.add(521); //id item
                    item.add(1); //số lượng
                    options.add(opt.toJSONString());
                    opt.clear();
                } else {
                    item.add(-1); //id item
                    item.add(0); //số lượng
                }
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBag = dataArray.toJSONString();
            dataArray.clear();
            int idAo5s = gender == 0 ? 0 : gender == 1 ? 1 : 2;
            int idQuan5s = gender == 0 ? 6 : gender == 1 ? 7 : 8;
            int idGang5s = gender == 0 ? 21 : gender == 1 ? 22 : 23;
            int idGiay5s = gender == 0 ? 27 : gender == 1 ? 28 : 29;
            int idRadar5s = 12;

            for (int i = 0; i < 30; i++) {
                if (i >= 0 && i <= 4) {
                    int itemId = (i == 0) ? idAo5s : (i == 1) ? idQuan5s : (i == 2) ? idGang5s : (i == 3) ? idGiay5s : idRadar5s;
                    opt.add(107); opt.add(5); options.add(opt.toJSONString()); opt.clear();  // 5 Sao trang bị (Option 102 = 5)
                    opt.add(102); opt.add(0); options.add(opt.toJSONString()); opt.clear();  // 5 Sao trang bị (Option 102 = 5)           
                    opt.add(30); opt.add(0); options.add(opt.toJSONString()); opt.clear();   // Khóa giao dịch
                    item.add(itemId);
                    item.add(1);
                } else {
                    item.add(-1);
                    item.add(0);
                }
                item.add(options.toJSONString());
                item.add(System.currentTimeMillis());
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }

            for (int i = 0; i < 20; i++) {
                if (i == 0) { //rada
                    opt.add(14); //id option
                    opt.add(1); //param option
                    item.add(12); //id item
                    item.add(1); //số lượng
                    options.add(opt.toJSONString());
                    opt.clear();
                } else {
                    item.add(-1); //id item
                    item.add(0); //số lượng
                }
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBox = dataArray.toJSONString();
            dataArray.clear();

            for (int i = 0; i < 110; i++) {
                item.add(-1); //id item
                item.add(0); //số lượng
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBoxLuckyRound = dataArray.toJSONString();
            dataArray.clear();

            for (int i = 0; i < 110; i++) {
                item.add(-1); //id item
                item.add(0); //số lượng
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsDaBan = dataArray.toJSONString();
            dataArray.clear();

            String friends = dataArray.toJSONString();
            String enemies = dataArray.toJSONString();

            dataArray.add(0); //id nội tại
            dataArray.add(0); //chỉ số 1
            dataArray.add(0); //chỉ số 2
            dataArray.add(0); //số lần mở
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            String intrinsic = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0); //bổ huyết
            dataArray.add(0); //bổ khí
            dataArray.add(0); //giáp xên
            dataArray.add(0); //cuồng nộ
            dataArray.add(0); //ẩn danh
            dataArray.add(0); //bổ huyết
            dataArray.add(0); //bổ khí
            dataArray.add(0); //giáp xên
            dataArray.add(0); //cuồng nộ
            dataArray.add(0); //ẩn danh
            dataArray.add(0); //mở giới hạn sức mạnh
            dataArray.add(0); //máy dò
            dataArray.add(0); //thức ăn cold
            dataArray.add(0); //icon thức ăn cold
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            String itemTime = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            String itemTime_ndung = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0);  // thời gian nhận
            dataArray.add(0);  // Sử Dụng Loa Liên Vũ Trụ 10 Lần Trong Ngày
            dataArray.add(0);  // Nạp Tích Lũy 3250 Ngọc Trong Ngày
            dataArray.add(0);  // Ước Rồng Thần 1 Sao x10 Lần
            dataArray.add(0);  // Hạ Gục Cumber, Black Goku, Cooler, Xên (30 Lần)
            dataArray.add(0);  // Đập 3 Trang Bị +7 Trong Ngày
            dataArray.add(0);  // Top 1 Đại Hội Võ Đài Siêu Hạng
            dataArray.add(0);  // Hoàn Thành 10 Nhiệm Vụ Siêu Khó Tại Bò Mộng
            dataArray.add(0);  // Đánh Bại, Hoặc Cho Xương Sói 20 Lần
            dataArray.add(0);  // Hoàn Thành 5 Lần Nhiệm Vụ Cho Nước Xinbato
            dataArray.add(0);  // Nhặt Đồ 500 Lần Trong Ngày
            dataArray.add(0);  // Tiêu Diệt 30 Lần Boss Ăn Trộm
            dataArray.add(0);  // Tiêu Diệt 30 Lần Boss Ở Dơ
            dataArray.add(0);  // Sử Dụng Loa Liên Vũ Trụ 10 Lần Trong Ngày
            String dataDanhhieu = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0); //id nhiệm vụ
            dataArray.add(0); //index nhiệm vụ con
            dataArray.add(0); //số lượng đã làm
            String task = dataArray.toJSONString();
            dataArray.clear();

            String mabuEgg = dataArray.toJSONString();

            String billEgg = dataArray.toJSONString();

            dataArray.add(System.currentTimeMillis()); //bùa trí tuệ
            dataArray.add(System.currentTimeMillis()); //bùa mạnh mẽ
            dataArray.add(System.currentTimeMillis()); //bùa da trâu
            dataArray.add(System.currentTimeMillis()); //bùa oai hùng
            dataArray.add(System.currentTimeMillis()); //bùa bất tử
            dataArray.add(System.currentTimeMillis()); //bùa dẻo dai
            dataArray.add(System.currentTimeMillis()); //bùa thu hút
            dataArray.add(System.currentTimeMillis()); //bùa đệ tử
            dataArray.add(System.currentTimeMillis()); //bùa trí tuệ x3
            dataArray.add(System.currentTimeMillis()); //bùa trí tuệ x4
            String charms = dataArray.toJSONString();
            dataArray.clear();

            int[] skillsArr = gender == 0
                    ? (Skill.ENABLE_SKILL_BIEN_HINH ? new int[]{0, 1, 6, 9, 10, 20, 22, 19, 24, 27} : new int[]{0, 1, 6, 9, 10, 20, 22, 19, 24})
                    : gender == 1
                    ? (Skill.ENABLE_SKILL_BIEN_HINH ? new int[]{2, 3, 7, 11, 12, 17, 18, 19, 26, 27} : new int[]{2, 3, 7, 11, 12, 17, 18, 19, 26})
                    : (Skill.ENABLE_SKILL_BIEN_HINH ? new int[]{4, 5, 8, 13, 14, 21, 23, 19, 25, 27} : new int[]{4, 5, 8, 13, 14, 21, 23, 19, 25});
            //[{"temp_id":"4","point":0,"last_time_use":0},]

            JSONArray skill = new JSONArray();
            for (int i = 0; i < skillsArr.length; i++) {
                skill.add(skillsArr[i]); //id skill
                if (i == 0) {
                    skill.add(1); //level skill
                } else {
                    skill.add(0); //level skill
                }
                skill.add(0); //thời gian sử dụng trước đó
                dataArray.add(skill.toString());
                skill.clear();
            }
            String skills = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(gender == 0 ? 0 : gender == 1 ? 2 : 4);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            String skillsShortcut = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(gender == 0 ? 0 : gender == 1 ? 2 : 4);
            String boughtSkills = dataArray.toJSONString();
            dataArray.clear();

            String petData = dataArray.toJSONString();

            JSONArray blackBall = new JSONArray();
            for (int i = 1; i <= 7; i++) {
                blackBall.add(0);
                blackBall.add(0);
                blackBall.add(0);
                dataArray.add(blackBall.toJSONString());
                blackBall.clear();
            }
            String dataBlackBall = dataArray.toString();
            dataArray.clear();

            dataArray.add(-1); //id side task
            dataArray.add(0); //thời gian nhận
            dataArray.add(0); //số lượng đã làm
            dataArray.add(0); //số lượng cần làm
            dataArray.add(20); //số nhiệm vụ còn lại có thể nhận
            dataArray.add(0); //mức độ nhiệm vụ
            String dataSideTask = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0);
            dataArray.add(100);
            dataArray.add(1);
            for (int i = 0; i < 7; i++) {
                dataArray.add(0);
            }
            for (int i = 0; i < 7; i++) {
                dataArray.add(0);
            }
            String dataKOL = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0);
            dataArray.add(0);
            String datatime = dataArray.toJSONString();
            dataArray.clear();

            AlyraManager.executeUpdate("insert into player"
                    + "(account_id, name, head, gender, have_tennis_space_ship, clan_id, "
                    + "data_inventory, data_location, data_point, data_magic_tree, items_body, "
                    + "items_bag, items_box, items_box_lucky_round, items_daban, friends, enemies, data_intrinsic, data_item_time, devndung_time ,"
                    + "data_task, data_mabu_egg,bill_data, data_charm, skills, skills_shortcut, pet,"
                    + "data_black_ball, data_side_task, data_danh_hieu,masterDoesNotAttack,data_achievement,giftcode,info, boughtSkills,data_KOL,loadtimetop) "
                    + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", userId, name, hair, gender, 0, -1, inventory, location, point, magicTree,
                    itemsBody, itemsBag, itemsBox, itemsBoxLuckyRound, itemsDaBan, friends, enemies, intrinsic,
                    itemTime, itemTime_ndung, task, mabuEgg, billEgg, charms, skills, skillsShortcut, petData, dataBlackBall, dataSideTask, dataDanhhieu, 0, 0, 0, "[]", boughtSkills, dataKOL, datatime);
            Logger.success("Tạo player mới thành công!\n");
            return CreateResult.CREATED;
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e, "Lỗi tạo player mới");
            return CreateResult.ERROR;
        }
        } // end synchronized (CREATE_LOCK)
    }

    private static <T> java.util.ArrayList<T> copyInv(java.util.List<T> src) {
        for (int i = 0; i < 8; i++) {
            try {
                return new java.util.ArrayList<>(src);
            } catch (RuntimeException e) {
                // concurrent structural modification during the copy — retry the tiny race window
            }
        }
        return new java.util.ArrayList<>(src); // final attempt; if it throws, save aborts cleanly (no data loss, retried next cycle)
    }

    public static void updateLoadTimeTop(Player player) {
        if (player == null || player.isBot || player.getSession() == null) {
            return;
        }
        try {
            JSONArray arr = new JSONArray();
            arr.add(player.lasttimeTD);
            arr.add(player.diemmaydam);
            AlyraManager.executeUpdate("UPDATE player SET loadtimetop = ? WHERE id = ?", arr.toJSONString(), player.id);
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e);
        }
    }

    public static void updateDataPoint(Player player) {
        if (player == null || player.isBot || player.getSession() == null || player.nPoint == null) {
            return;
        }
        try {
            JSONArray dataArray = new JSONArray();
            dataArray.add(player.nPoint.limitPower);
            dataArray.add(player.nPoint.power);
            dataArray.add(player.nPoint.tiemNang);
            dataArray.add(player.nPoint.stamina);
            dataArray.add(player.nPoint.maxStamina);
            dataArray.add(player.nPoint.hpg);
            dataArray.add(player.nPoint.mpg);
            dataArray.add(player.nPoint.dameg);
            dataArray.add(player.nPoint.defg);
            dataArray.add(player.nPoint.critg);
            dataArray.add(0);
            dataArray.add(player.nPoint.hp);
            dataArray.add(player.nPoint.mp);
            AlyraManager.executeUpdate("UPDATE player SET data_point = ? WHERE id = ?", dataArray.toJSONString(), player.id);
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e);
        }
    }

    public static void updateDataTask(Player player) {
        if (player == null || player.isBot || player.getSession() == null || player.playerTask == null || player.playerTask.taskMain == null) {
            return;
        }
        try {
            JSONArray dataArray = new JSONArray();
            dataArray.add(player.playerTask.taskMain.id);
            dataArray.add(player.playerTask.taskMain.index);
            short count = 0;
            if (player.playerTask.taskMain.subTasks != null
                    && player.playerTask.taskMain.index >= 0
                    && player.playerTask.taskMain.index < player.playerTask.taskMain.subTasks.size()) {
                count = player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count;
            }
            dataArray.add(count);
            dataArray.add(player.playerTask.taskMain.lastTime);
            AlyraManager.executeUpdate("UPDATE player SET data_task = ? WHERE id = ?", dataArray.toJSONString(), player.id);
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e);
        }
    }

    public static void updateDanap(Player player) {
        if (player == null || player.isBot || player.getSession() == null) {
            return;
        }
        try {
            long danap = player.getSession().danap;
            int userId = player.getSession().userId;
            if (userId > 0) {
                if (danap > 0) {
                    AlyraManager.executeUpdate("UPDATE account SET danap = GREATEST(danap, ?) WHERE id = ?", danap, userId);
                }
                long dbDanap = getTotalDanap(userId);
                if (dbDanap > player.getSession().danap) {
                    player.getSession().danap = (int) Math.min(dbDanap, Integer.MAX_VALUE);
                }
            }
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e);
        }
    }

    public static void updatePlayer(Player player) {
        // Bot nằm chung danh sách người chơi online nên mọi đường lưu đều quét trúng nó.
        // Bot không có bản ghi trong DB, chốt chặn ngay đây cho mọi nơi gọi.
        if (player != null && (player.isBot || player.getSession() == null)) {
            return;
        }
        if (player != null && player.idMark != null && player.idMark.isLoadedAllDataPlayer()
                && player.inventory != null && player.nPoint != null && player.location != null
                && player.playerTask != null) {
            long st = System.currentTimeMillis();
            try {
                JSONArray dataArray = new JSONArray();

                //data kim lượng
                dataArray.add(player.inventory.gold > Inventory.LIMIT_GOLD
                        ? Inventory.LIMIT_GOLD : player.inventory.gold);
                dataArray.add(player.inventory.gem);
                dataArray.add(player.inventory.ruby);
                dataArray.add(player.inventory.coupon);
                dataArray.add(player.inventory.event);
                dataArray.add(player.inventory.diemDT); // index 5
                dataArray.add(player.inventory.diemXen); // index 6
                String inventory = dataArray.toJSONString();
                dataArray.clear();

                int mapId = player.mapIdBeforeLogout;
                int x = player.location.x;
                int y = player.location.y;
                long hp = player.nPoint.hp;
                long mp = player.nPoint.mp;
                if (player.isDie()) {
                    mapId = player.gender + 21;
                    x = 300;
                    y = 336;
                    hp = 1;
                    mp = 1;
                } else {
                    if (mapId == services.dungeon.HalloweenDungeonService.MAP_ID || MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId) || MapService.gI().isMapVaoBangVatPham(mapId) || ChangeMapService.gI().checkMapCanJoin(player, MapService.gI().getMapCanJoin(player, mapId, 0)) == null) {
                        mapId = player.gender + 21;
                        x = 300;
                        y = 336;
                    }
                }

                //data vị trí
                dataArray.add(mapId);
                dataArray.add(x);
                dataArray.add(y);
                dataArray.add(player.map182Deadline); // index 3 — mốc hết hạn map 182
                dataArray.add(player.hitBossLv20Time); // index 4 — mốc hạ boss Hit lv20 (khoá gọi tới 00h)
                dataArray.add(player.lastTimeWinGuruWar); // index 5 — mốc chiến thắng Guru War (buff 24h)
                String location = dataArray.toJSONString();
                dataArray.clear();

                //data chỉ số
                dataArray.add(player.nPoint.limitPower);
                dataArray.add(player.nPoint.power);
                dataArray.add(player.nPoint.tiemNang);
                dataArray.add(player.nPoint.stamina);
                dataArray.add(player.nPoint.maxStamina);
                dataArray.add(player.nPoint.hpg);
                dataArray.add(player.nPoint.mpg);
                dataArray.add(player.nPoint.dameg);
                dataArray.add(player.nPoint.defg);
                dataArray.add(player.nPoint.critg);
                dataArray.add(0);
                dataArray.add(hp);
                dataArray.add(mp);
                String point = dataArray.toJSONString();
                dataArray.clear();

                dataArray.add(player.playerTask.taskdh.Nap);
                dataArray.add(player.playerTask.taskdh.ResetTime);
                dataArray.add(player.playerTask.taskdh.Shenron);
                dataArray.add(player.playerTask.taskdh.Hagucboss);
                dataArray.add(player.playerTask.taskdh.DapDo);
                dataArray.add(player.playerTask.taskdh.SieuHang);
                dataArray.add(player.playerTask.taskdh.TaskBoMong);
                dataArray.add(player.playerTask.taskdh.ChoSuong);
                dataArray.add(player.playerTask.taskdh.ChoNuoc);
                dataArray.add(player.playerTask.taskdh.NhatDo);
                dataArray.add(player.playerTask.taskdh.AnTrom);
                dataArray.add(player.playerTask.taskdh.ODo);
                dataArray.add(player.playerTask.taskdh.DungLoa);
                String Danhieu = dataArray.toJSONString();
                dataArray.clear();

                //data đậu thần
                dataArray.add(player.magicTree.level);
                dataArray.add(player.magicTree.currPeas);
                dataArray.add(player.magicTree.isUpgrade ? 1 : 0);
                dataArray.add(player.magicTree.lastTimeHarvest);
                dataArray.add(player.magicTree.lastTimeUpgrade);
                String magicTree = dataArray.toJSONString();
                dataArray.clear();

                //data body
                JSONArray dataItem = new JSONArray();
                for (Item item : copyInv(player.inventory.itemsBody)) {
                    JSONArray opt = new JSONArray();
                    if (item.isNotNullItem()) {
                        dataItem.add(item.template.id);
                        dataItem.add(item.quantity);
                        JSONArray options = new JSONArray();
                        for (Item.ItemOption io : item.itemOptions) {
                            opt.add(io.optionTemplate.id);
                            opt.add(io.param);
                            options.add(opt.toJSONString());
                            opt.clear();
                        }
                        dataItem.add(options.toJSONString());
                    } else {
                        dataItem.add(-1);
                        dataItem.add(0);
                        dataItem.add(opt.toJSONString());
                    }
                    dataItem.add(item.createTime);
                    dataArray.add(dataItem.toJSONString());
                    dataItem.clear();
                }
                String itemsBody = dataArray.toJSONString();
                dataArray.clear();

                long thoiVangBag = 0;
                long thoiVangBox = 0;
                int ngocRong3sBag = 0;
                int ngocRong3sBox = 0;
                int ngocRong4sBag = 0;
                int ngocRong4sBox = 0;

                //data bag
                for (Item item : copyInv(player.inventory.itemsBag)) {
                    JSONArray opt = new JSONArray();
                    if (item.isNotNullItem()) {
                        dataItem.add(item.template.id);
                        dataItem.add(item.quantity);
                        if (item.template.id == 457) {
                            thoiVangBag += item.quantity;
                        } else if (item.template.id == 16) {
                            ngocRong3sBag += item.quantity;
                        } else if (item.template.id == 17) {
                            ngocRong4sBag += item.quantity;
                        }
                        JSONArray options = new JSONArray();
                        for (Item.ItemOption io : item.itemOptions) {
                            opt.add(io.optionTemplate.id);
                            opt.add(io.param);
                            options.add(opt.toJSONString());
                            opt.clear();
                        }
                        dataItem.add(options.toJSONString());
                    } else {
                        dataItem.add(-1);
                        dataItem.add(0);
                        dataItem.add(opt.toJSONString());
                    }
                    dataItem.add(item.createTime);
                    dataArray.add(dataItem.toJSONString());
                    dataItem.clear();
                }
                String itemsBag = dataArray.toJSONString();
                dataArray.clear();

                //data box
                for (Item item : copyInv(player.inventory.itemsBox)) {
                    JSONArray opt = new JSONArray();
                    if (item.isNotNullItem()) {
                        dataItem.add(item.template.id);
                        dataItem.add(item.quantity);
                        if (item.template.id == 457) {
                            thoiVangBox += item.quantity;
                        } else if (item.template.id == 16) {
                            ngocRong3sBox += item.quantity;
                        } else if (item.template.id == 17) {
                            ngocRong4sBox += item.quantity;
                        }
                        JSONArray options = new JSONArray();
                        for (Item.ItemOption io : item.itemOptions) {
                            opt.add(io.optionTemplate.id);
                            opt.add(io.param);
                            options.add(opt.toJSONString());
                            opt.clear();
                        }
                        dataItem.add(options.toJSONString());
                    } else {
                        dataItem.add(-1);
                        dataItem.add(0);
                        dataItem.add(opt.toJSONString());
                    }
                    dataItem.add(item.createTime);
                    dataArray.add(dataItem.toJSONString());
                    dataItem.clear();
                }
                String itemsBox = dataArray.toJSONString();
                dataArray.clear();

                //data box crack ball
                for (Item item : copyInv(player.inventory.itemsBoxCrackBall)) {
                    JSONArray opt = new JSONArray();
                    if (item.isNotNullItem()) {
                        dataItem.add(item.template.id);
                        dataItem.add(item.quantity);
                        JSONArray options = new JSONArray();
                        for (Item.ItemOption io : item.itemOptions) {
                            opt.add(io.optionTemplate.id);
                            opt.add(io.param);
                            options.add(opt.toJSONString());
                            opt.clear();
                        }
                        dataItem.add(options.toJSONString());
                    } else {
                        dataItem.add(-1);
                        dataItem.add(0);
                        dataItem.add(opt.toJSONString());
                    }
                    dataItem.add(item.createTime);
                    if (item.message != null) {
                        dataItem.add(item.message);
                    }
                    dataArray.add(dataItem.toJSONString());
                    dataItem.clear();
                }
                String itemsBoxLuckyRound = dataArray.toJSONString();
                dataArray.clear();

                //data item da ban
                for (Item item : copyInv(player.inventory.itemsDaBan)) {
                    JSONArray opt = new JSONArray();
                    if (item.isNotNullItem()) {
                        dataItem.add(item.template.id);
                        dataItem.add(item.quantity);
                        JSONArray options = new JSONArray();
                        for (Item.ItemOption io : item.itemOptions) {
                            opt.add(io.optionTemplate.id);
                            opt.add(io.param);
                            options.add(opt.toJSONString());
                            opt.clear();
                        }
                        dataItem.add(options.toJSONString());
                    } else {
                        dataItem.add(-1);
                        dataItem.add(0);
                        dataItem.add(opt.toJSONString());
                    }
                    dataItem.add(item.createTime);
                    dataArray.add(dataItem.toJSONString());
                    dataItem.clear();
                }
                String itemsDaBan = dataArray.toJSONString();
                dataArray.clear();

                //data bạn bè
                JSONArray dataFE = new JSONArray();
                for (Friend f : player.friends) {
                    dataFE.add(f.id);
                    dataFE.add(f.name);
                    dataFE.add(f.head);
                    dataFE.add(f.body);
                    dataFE.add(f.leg);
                    dataFE.add(f.bag);
                    dataFE.add(f.power);
                    dataArray.add(dataFE.toJSONString());
                    dataFE.clear();
                }
                String friend = dataArray.toJSONString();
                dataArray.clear();

                //data kẻ thù
                for (Friend e : player.enemies) {
                    dataFE.add(e.id);
                    dataFE.add(e.name);
                    dataFE.add(e.head);
                    dataFE.add(e.body);
                    dataFE.add(e.leg);
                    dataFE.add(e.bag);
                    dataFE.add(e.power);
                    dataArray.add(dataFE.toJSONString());
                    dataFE.clear();
                }
                String enemy = dataArray.toJSONString();
                dataArray.clear();

                //data nội tại
                JSONArray dataIntrinsic = new JSONArray();
                dataIntrinsic.add(player.playerIntrinsic.intrinsic.id);
                dataIntrinsic.add(player.playerIntrinsic.intrinsic.param1);
                dataIntrinsic.add(player.playerIntrinsic.countOpen);
                dataIntrinsic.add(player.playerIntrinsic.intrinsic.param2);
                String intrinsic = dataIntrinsic.toJSONString();

                //data item time
                dataArray.add((player.itemTime.isUseBoHuyet ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet)) : 0));
                dataArray.add((player.itemTime.isUseBoHuyet2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet2)) : 0));
                dataArray.add((player.itemTime.isUseBoKhi ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi)) : 0));
                dataArray.add((player.itemTime.isUseBoKhi2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi2)) : 0));
                dataArray.add((player.itemTime.isUseGiapXen ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen)) : 0));
                dataArray.add((player.itemTime.isUseGiapXen2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen2)) : 0));
                dataArray.add((player.itemTime.isUseCuongNo ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo)) : 0));
                dataArray.add((player.itemTime.isUseCuongNo2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo2)) : 0));
                dataArray.add((player.itemTime.isUseAnDanh ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh)) : 0));
                dataArray.add((player.itemTime.isUseAnDanh2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh2)) : 0));
                dataArray.add((player.itemTime.isOpenPower ? (ItemTime.TIME_OPEN_POWER - (System.currentTimeMillis() - player.itemTime.lastTimeOpenPower)) : 0));
                dataArray.add((player.itemTime.isUseMayDo ? (ItemTime.TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo)) : 0));
                dataArray.add((player.itemTime.isUseMayDo2 ? (ItemTime.TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo2)) : 0));
                dataArray.add(0);
                dataArray.add((player.itemTime.isEatMeal ? (ItemTime.TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal)) : 0));
                dataArray.add(player.itemTime.iconMeal);
                dataArray.add((player.itemTime.isUseTDLT ? ((player.itemTime.timeTDLT - (System.currentTimeMillis() - player.itemTime.lastTimeUseTDLT)) / 60 / 1000) : 0));
                dataArray.add((player.itemTime.isUseCMS ? (ItemTime.TIME_CMS - (System.currentTimeMillis() - player.itemTime.lastTimeUseCMS)) : 0));
                dataArray.add((player.itemTime.isUseGTPT ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeUseGTPT)) : 0));
                dataArray.add((player.itemTime.isUseDK ? (ItemTime.TIME_DK - (System.currentTimeMillis() - player.itemTime.lastTimeUseDK)) : 0));
                dataArray.add((player.itemTime.isUseRX ? ((player.itemTime.timeRX - (System.currentTimeMillis() - player.itemTime.lastTimeUseRX)) / 60 / 1000) : 0));
                dataArray.add((player.itemTime.isEatMeal2 ? (ItemTime.TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal2)) : 0));
                dataArray.add(player.itemTime.iconMeal2);
                dataArray.add(0);
                dataArray.add((player.itemTime.isUseNCD ? (ItemTime.TIME_NCD - (System.currentTimeMillis() - player.itemTime.lastTimeUseNCD)) : 0));
                dataArray.add(0);
                dataArray.add(0);
                dataArray.add(player.itemTime.isKhauTrang ? (ItemTime.TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeKhauTrang)) : 0);
                dataArray.add(player.itemTime.isTnDeTu ? (ItemTime.TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeTnDeTu)) : 0);
                dataArray.add((player.itemTime.isXimuoihoadao
                        ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeXimuoihoadao))
                        : 0));
                dataArray.add((player.itemTime.isXimuoihoamai
                        ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeXimuoihoamai))
                        : 0));
                dataArray.add((player.itemTime.isBuaTNSM
                        ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBuaTNSM))
                        : 0));
                // [32] thời gian còn lại + [33] hệ số của bình x3/x5/x7 TNSM (item 1357-1359)
                dataArray.add((player.itemTime.tnsmX > 1
                        ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeTnsmX))
                        : 0));
                dataArray.add(player.itemTime.tnsmX);
                String itemTime = dataArray.toJSONString();
                dataArray.clear();

                dataArray.add((player.itemTime.isUseCo4La ? (ItemTime.TIME_CO - (System.currentTimeMillis() - player.itemTime.lastTimeUseCo4La)) : 0));
                dataArray.add((player.itemTime.banhchung ? (ItemTime.DEVNDUNG - (System.currentTimeMillis() - player.itemTime.banhchunglastTime)) : 0));
                dataArray.add((player.itemTime.banhtet ? (ItemTime.DEVNDUNG - (System.currentTimeMillis() - player.itemTime.banhtetlastTime)) : 0));
                dataArray.add((player.itemTime.nguqua ? (ItemTime.DEVNDUNG - (System.currentTimeMillis() - player.itemTime.nguqualastTime)) : 0));
                dataArray.add((player.itemTime.miakl ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.timemiakl)) : 0));
                dataArray.add((player.itemTime.miathom ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.timemiathom)) : 0));
                dataArray.add((player.itemTime.miasaurieng ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.timemiasaurieng)) : 0));
                dataArray.add((player.itemTime.isKeoMotMat ? (ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeKeoMotMat)) : 0));
                dataArray.add((player.itemTime.isSupBiHacAm ? (ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeSupBiHacAm)) : 0));
                dataArray.add((player.itemTime.isBanhGatoNhen ? (ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeBanhGatoNhen)) : 0));
                dataArray.add((player.itemTime.isHamburgerSau ? (ItemTime.TIME_HALLOWEEN_BUFF - (System.currentTimeMillis() - player.itemTime.lastTimeHamburgerSau)) : 0));

                String DevNdung = dataArray.toJSONString();
                dataArray.clear();

                //data nhiệm vụ
                dataArray.add(player.playerTask.taskMain.id);
                dataArray.add(player.playerTask.taskMain.index);
                dataArray.add(player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count);
                dataArray.add(player.playerTask.taskMain.lastTime);
                String task = dataArray.toJSONString();
                dataArray.clear();

                //data nhiệm vụ hàng ngày
                dataArray.add(player.playerTask.sideTask.template != null ? player.playerTask.sideTask.template.id : -1);
                dataArray.add(player.playerTask.sideTask.receivedTime);
                dataArray.add(player.playerTask.sideTask.count);
                dataArray.add(player.playerTask.sideTask.maxCount);
                dataArray.add(player.playerTask.sideTask.leftTask);
                dataArray.add(player.playerTask.sideTask.level);
                String sideTask = dataArray.toJSONString();
                dataArray.clear();

                //data trứng bư
                if (player.mabuEgg != null) {
                    dataArray.add(player.mabuEgg.lastTimeCreate);
                    dataArray.add(player.mabuEgg.timeDone);
                }
                String mabuEgg = dataArray.toJSONString();
                dataArray.clear();

                //data trứng bill
                if (player.billEgg != null) {
                    dataArray.add(player.billEgg.lastTimeCreate);
                    dataArray.add(player.billEgg.timeDone);
                }
                String billEgg = dataArray.toJSONString();
                dataArray.clear();
                //data trứng black
                if (player.blackEgg != null) {
                    dataArray.add(player.blackEgg.lastTimeCreate);
                    dataArray.add(player.blackEgg.timeDone);
                }
                String blackEgg = dataArray.toJSONString();
                dataArray.clear();
                //data bùa
                dataArray.add(player.charms.tdTriTue);
                dataArray.add(player.charms.tdManhMe);
                dataArray.add(player.charms.tdDaTrau);
                dataArray.add(player.charms.tdOaiHung);
                dataArray.add(player.charms.tdBatTu);
                dataArray.add(player.charms.tdDeoDai);
                dataArray.add(player.charms.tdThuHut);
                dataArray.add(player.charms.tdDeTu);
                dataArray.add(player.charms.tdTriTue3);
                dataArray.add(player.charms.tdTriTue4);
                String charm = dataArray.toJSONString();
                dataArray.clear();

                dataArray.add(player.playerTask.task_kol.count);
                dataArray.add(player.playerTask.task_kol.maxCount);
                dataArray.add(player.playerTask.task_kol.level);
                for (boolean done : player.playerTask.task_kol.isRewarded) {
                    dataArray.add(done ? 1 : 0);
                }
                for (boolean vipDone : player.playerTask.task_kol.isVipRewarded) {
                    dataArray.add(vipDone ? 1 : 0);
                }
                String dataKOL = dataArray.toJSONString();
                dataArray.clear();

                dataArray.add(player.lasttimeTD);
                dataArray.add(player.diemmaydam);
                String dataTime = dataArray.toJSONString();
                dataArray.clear();

                dataArray.add(player.point_pokemon);
                String data_poke = dataArray.toJSONString();
                dataArray.clear();

                //data skill
                JSONArray dataSkill = new JSONArray();
                for (Skill skill : player.playerSkill.skills) {
                    dataSkill.add(skill.template.id);
                    dataSkill.add(skill.point);
                    dataSkill.add(skill.lastTimeUseThisSkill);
                    dataSkill.add(skill.currLevel);
                    dataArray.add(dataSkill.toJSONString());
                    dataSkill.clear();
                }
                String skills = dataArray.toJSONString();
                dataArray.clear();
                dataArray.clear();

                //data skill shortcut
                for (int skillId : player.playerSkill.skillShortCut) {
                    dataArray.add(skillId);
                }
                String skillShortcut = dataArray.toJSONString();
                dataArray.clear();

                String pet = dataArray.toJSONString();
                String petInfo;
                String petPoint;
                String petBody;
                String petSkill;

                //data pet
                if (player.pet != null) {
                    dataArray.add(player.pet.typePet);
                    dataArray.add(player.pet.gender);
                    dataArray.add(player.pet.name);
                    dataArray.add(player.fusion.typeFusion);
                    int timeLeftFusion = (int) (Fusion.TIME_FUSION - (System.currentTimeMillis() - player.fusion.lastTimeFusion));
                    dataArray.add(timeLeftFusion < 0 ? 0 : timeLeftFusion);
                    dataArray.add(player.pet.status);
                    petInfo = dataArray.toJSONString();
                    dataArray.clear();

                    dataArray.add(player.pet.nPoint.limitPower);
                    dataArray.add(player.pet.nPoint.power);
                    dataArray.add(player.pet.nPoint.tiemNang);
                    dataArray.add(player.pet.nPoint.stamina);
                    dataArray.add(player.pet.nPoint.maxStamina);
                    dataArray.add(player.pet.nPoint.hpg);
                    dataArray.add(player.pet.nPoint.mpg);
                    dataArray.add(player.pet.nPoint.dameg);
                    dataArray.add(player.pet.nPoint.defg);
                    dataArray.add(player.pet.nPoint.critg);
                    dataArray.add(player.pet.nPoint.hp);
                    dataArray.add(player.pet.nPoint.mp);
                    petPoint = dataArray.toJSONString();
                    dataArray.clear();

                    JSONArray items = new JSONArray();
                    JSONArray options = new JSONArray();
                    JSONArray opt = new JSONArray();
                    for (Item item : player.pet.inventory.itemsBody) {
                        if (item.isNotNullItem()) {
                            dataItem.add(item.template.id);
                            dataItem.add(item.quantity);
                            for (Item.ItemOption io : item.itemOptions) {
                                opt.add(io.optionTemplate.id);
                                opt.add(io.param);
                                options.add(opt.toJSONString());
                                opt.clear();
                            }
                            dataItem.add(options.toJSONString());
                        } else {
                            dataItem.add(-1);
                            dataItem.add(0);
                            dataItem.add(options.toJSONString());
                        }

                        dataItem.add(item.createTime);

                        items.add(dataItem.toJSONString());
                        dataItem.clear();
                        options.clear();
                    }
                    petBody = items.toJSONString();

                    JSONArray petSkills = new JSONArray();
                    for (Skill s : player.pet.playerSkill.skills) {
                        JSONArray pskill = new JSONArray();
                        if (s.skillId != -1) {
                            pskill.add(s.template.id);
                            pskill.add(s.point);
                            pskill.add(s.lastTimeUseThisSkill);
                            pskill.add(s.currLevel);
                        } else {
                            pskill.add(-1);
                            pskill.add(0);
                            pskill.add(0);
                            pskill.add(0);
                        }
                        petSkills.add(pskill.toJSONString());
                    }
                    petSkill = petSkills.toJSONString();

                    dataArray.add(petInfo);
                    dataArray.add(petPoint);
                    dataArray.add(petBody);
                    dataArray.add(petSkill);

                    pet = dataArray.toJSONString();
                }
                dataArray.clear();

                //data thưởng ngọc rồng đen
                for (int i = 0; i < player.rewardBlackBall.timeOutOfDateReward.length; i++) {
                    JSONArray dataBlackBall = new JSONArray();
                    dataBlackBall.add(player.rewardBlackBall.timeOutOfDateReward[i]);
                    dataBlackBall.add(player.rewardBlackBall.lastTimeGetReward[i]);
                    dataBlackBall.add(player.rewardBlackBall.quantilyBlackBall[i]);
                    dataArray.add(dataBlackBall.toJSONString());
                    dataBlackBall.clear();
                }
                String dataBlackBall = dataArray.toJSONString();
                dataArray.clear();

                //Ma Bao Ve
                dataArray.add(player.mbv);
                dataArray.add(player.baovetaikhoan);
                dataArray.add(player.mbvtime);
                dataArray.add(player.isOffPorataOutfit);
                String dataBVTK = dataArray.toJSONString();
                dataArray.clear();

                //Card
                String dataCard = JSONValue.toJSONString(player.Cards);

                //BDKB
                dataArray.add(player.timesPerDayBDKB);
                dataArray.add(player.lastTimeJoinBDKB);
                String dataBDKB = dataArray.toJSONString();
                dataArray.clear();

                //CDRD
                dataArray.add(player.joinCDRD);
                dataArray.add(player.lastTimeJoinCDRD);
                dataArray.add(player.talkToThuongDe);
                dataArray.add(player.talkToThanMeo);
                String dataCDRD = dataArray.toJSONString();
                dataArray.clear();

                //Nhận Thỏi Vàng
                dataArray.add(player.danhanthoivang);
                dataArray.add(player.lastRewardGoldBarTime);
                String dataNhanThoiVang = dataArray.toJSONString();
                dataArray.clear();

                //Rương Gỗ
                dataArray.add(player.levelWoodChest);
                dataArray.add(player.goldChallenge);
                dataArray.add(player.rubyChallenge);
                dataArray.add(player.lastTimeRewardWoodChest);
                dataArray.add(player.lastTimePKDHVT23);
                String dataRuongGo = dataArray.toJSONString();
                dataArray.clear();

                //Siêu thần thủy
                dataArray.add(player.winSTT);
                dataArray.add(player.lastTimeWinSTT);
                dataArray.add(player.callBossPocolo);
                String dataSieuThanThuy = dataArray.toJSONString();
                dataArray.clear();

                //Võ đài sinh tử
                dataArray.add(player.haveRewardVDST);
                dataArray.add(player.thoiVangVoDaiSinhTu);
                dataArray.add(player.lastTimePKVoDaiSinhTu);
                dataArray.add(player.timePKVDST);
                String dataVoDaiSinhTu = dataArray.toJSONString();
                dataArray.clear();

                //Data item event
                dataArray.add(player.itemEvent.remainingTVGSCount);
                dataArray.add(player.itemEvent.lastTVGSTime);
                dataArray.add(player.itemEvent.remainingHHCount);
                dataArray.add(player.itemEvent.lastHHTime);
                dataArray.add(player.itemEvent.remainingBNCount);
                dataArray.add(player.itemEvent.lastBNTime);
                JSONArray trickOrTreatList = new JSONArray();
                if (player.npcsTrickOrTreat != null) {
                    for (int npcId : player.npcsTrickOrTreat) {
                        trickOrTreatList.add(npcId);
                    }
                }
                dataArray.add(trickOrTreatList);
                dataArray.add(player.lastTimeTrickOrTreat);
                String dataItemEvent = dataArray.toJSONString();
                dataArray.clear();

                //Data Luyện Tập
                dataArray.add(player.levelLuyenTap);
                dataArray.add(player.dangKyTapTuDong);
                dataArray.add(player.mapIdDangTapTuDong);
                dataArray.add(player.tnsmLuyenTap);
                if (player.isOffline) {
                    dataArray.add(player.lastTimeOffline);
                } else {
                    dataArray.add(System.currentTimeMillis());
                }
                dataArray.add(player.traning.getTop());
                dataArray.add(player.traning.getTime());
                dataArray.add(player.traning.getLastTime());
                dataArray.add(player.traning.getLastTop());
                dataArray.add(player.traning.getLastRewardTime());

                String dataLuyenTap = dataArray.toJSONString();
                dataArray.clear();

                //data nhiệm vụ bang hàng ngày
                dataArray.add(player.playerTask.clanTask.template != null ? player.playerTask.clanTask.template.id : -1);
                dataArray.add(player.playerTask.clanTask.receivedTime);
                dataArray.add(player.playerTask.clanTask.count);
                dataArray.add(player.playerTask.clanTask.maxCount);
                dataArray.add(player.playerTask.clanTask.leftTask);
                dataArray.add(player.playerTask.clanTask.level);
                String clanTask = dataArray.toJSONString();
                dataArray.clear();

                //data vip
                dataArray.add(player.timesPerDayCuuSat);
                dataArray.add(player.lastTimeCuuSat);
                dataArray.add(player.nhanDeTuNangVIP);
                dataArray.add(player.nhanVangNangVIP);
                dataArray.add(player.nhanSKHVIP);
                String dataVip = dataArray.toJSONString();
                dataArray.clear();

                //Data doanh trại
                dataArray.add(player.lastTimeJoinDT);
                String doanhtrai = dataArray.toJSONString();
                dataArray.clear();

                //data achievement
                if (player.achievement != null) {
                    for (Template.AchievementQuest aq : player.achievement.getAchievementList()) {
                        JSONArray a = new JSONArray();
                        a.add(aq.completed);
                        a.add(aq.isRecieve);
                        dataArray.add(a.toJSONString());
                        a.clear();
                    }
                }
                String achievement = dataArray.toJSONString();
                dataArray.clear();

                //gift code
                for (String code : player.giftCode.rewards) {
                    dataArray.add(code);
                }
                String giftCode = dataArray.toJSONString();
                dataArray.clear();
                // Shop santa danh hiệu
                JSONArray dataShopDanhHieu = new JSONArray();
                dataShopDanhHieu.add(player.effect.getPointDaiGiaMoiNhu());
                dataShopDanhHieu.add(player.effect.getPointTrumUocRong());
                dataShopDanhHieu.add(player.effect.getPointTrumSanBoss());
                dataShopDanhHieu.add(player.effect.getPointThanhDapDo());
                dataShopDanhHieu.add(player.effect.getPointNongDanChamChi());
                dataShopDanhHieu.add(player.effect.getPointOngThanVeChai());
                dataShopDanhHieu.add(player.effect.getPointBiMocSachTui());
                dataShopDanhHieu.add(player.effect.getPointPhanCung());
                String shopDanhHieu = dataShopDanhHieu.toJSONString();
                dataShopDanhHieu.clear();

                // Bùa Miễn phí
                JSONArray bua = new JSONArray();
                bua.add(player.luotNhanBuaMienPhi);
                bua.add(player.diemDanhSuKien);
                String buarandom = bua.toJSONString();
                bua.clear();

                JSONArray diemsk = new JSONArray();
                diemsk.add(player.diemsukien);
                diemsk.add(player.phaobong);
                diemsk.add(player.lixi);
                diemsk.add(player.mocVongQuay);
                String skhe = diemsk.toJSONString();
                diemsk.clear();

                JSONArray hocKyNang = new JSONArray();
                hocKyNang.add(player.hocKyNang.ItemTemplateSkillId);
                hocKyNang.add(player.hocKyNang.Level);
                hocKyNang.add(player.hocKyNang.PotentialLearn);
                hocKyNang.add(player.hocKyNang.Time);
                String sHocKyNang = hocKyNang.toJSONString();
                boolean isAdminSave = player.getSession() != null && player.getSession().isAdmin;

                String query = "update player set head = ?, have_tennis_space_ship = ?, "
                        + "clan_id = ?, data_inventory = ?, data_location = ?, data_point = ?, data_magic_tree = ?, "
                        + "items_body = ?, items_bag = ?, items_box = ?, items_box_lucky_round = ?, items_daban = ?, friends = ?, "
                        + "enemies = ?, data_intrinsic = ?, data_item_time = ?, devndung_time = ?, data_task = ?, data_mabu_egg = ?,bill_data=?, black_data=?, pet = ?, "
                        + "data_black_ball = ?, data_side_task = ?, data_danh_hieu = ?, data_charm = ?, skills = ?, skills_shortcut = ?, notify = ?, "
                        + "baovetaikhoan = ?, data_card = ?, lasttimepkcommeson = ?, bandokhobau = ?, doanhtrai = ?, conduongrandoc = ?, masterDoesNotAttack = ?, "
                        + "nhanthoivang = ?, ruonggo = ?, sieuthanthuy = ?, vodaisinhtu = ?, rongxuong = ?, data_item_event = ?, data_luyentap = ?, data_clan_task = ?, data_vip = ?, "
                        + "rank = ?, data_achievement = ?, giftcode = ?,danh_hieu_shop = ?, firstTimeLogin = ? ,buarandom = ?, dien_sukien = ?, banhtet = ?, "
                        + "banhchung = ?, info = ?, hoc_ky_nang = ?, boughtSkills = ?,data_KOL = ?,loadtimetop=?,point_pokemon=?,daubuoi=?"
                        + " where id = ?";

                AlyraManager.executeUpdate(query,
                        player.head,
                        player.haveTennisSpaceShip,
                        (player.clan != null ? player.clan.id : -1),
                        inventory,
                        location,
                        point,
                        magicTree,
                        itemsBody,
                        itemsBag,
                        itemsBox,
                        itemsBoxLuckyRound,
                        itemsDaBan,
                        friend,
                        enemy,
                        intrinsic,
                        itemTime,
                        DevNdung,
                        task,
                        mabuEgg,
                        billEgg,
                        blackEgg,
                        pet,
                        dataBlackBall,
                        sideTask,
                        Danhieu,
                        charm,
                        skills,
                        skillShortcut,
                        player.notify,
                        dataBVTK,
                        dataCard,
                        player.lastPkCommesonTime,
                        dataBDKB,
                        doanhtrai,
                        dataCDRD,
                        player.doesNotAttack,
                        dataNhanThoiVang,
                        dataRuongGo,
                        dataSieuThanThuy,
                        dataVoDaiSinhTu,
                        player.lastTimeShenronAppeared,
                        dataItemEvent,
                        dataLuyenTap,
                        clanTask,
                        dataVip,
                        player.superRank.rank,
                        achievement,
                        giftCode,
                        shopDanhHieu,
                        Util.toDateString(player.firstTimeLogin),
                        buarandom,
                        skhe,
                        player.banhtet,
                        player.banhtrung,
                        "Vàng tươi: " + (isAdminSave ? Util.formatNumber(2000) : Util.formatNumber(player.inventory.gold)) + " (" + (isAdminSave ? Util.numberToMoney(2000) : Util.numberToMoney(player.inventory.gold))
                        + ") | Thỏi vàng: " + (isAdminSave ? 0 : Util.formatNumber(thoiVangBag + thoiVangBox)) + " (Bag: " + (isAdminSave ? 0 : Util.formatNumber(thoiVangBag)) + " - Box: " + (isAdminSave ? 0 : Util.formatNumber(thoiVangBox))
                        + ") | Ngọc rồng 3s: " + (isAdminSave ? 0 : Util.formatNumber(ngocRong3sBag + ngocRong3sBox)) + " (Bag: " + (isAdminSave ? 0 : Util.formatNumber(ngocRong3sBag)) + " - Box: " + (isAdminSave ? 0 : Util.formatNumber(ngocRong3sBox))
                        + ") | Ngọc rồng 4s: " + Util.formatNumber(ngocRong4sBag + ngocRong4sBox) + " (Bag: " + Util.formatNumber(ngocRong4sBag) + " - Box: " + Util.formatNumber(ngocRong4sBox) + ")",
                        sHocKyNang,
                        "[]",
                        dataKOL,
                        dataTime,
                        data_poke, player.daubuoi,
                        player.id);
//                SuperRankRepo.upsertData(player);
                if (player.isOffline) {
                    Logger.log(Logger.PURPLE, TimeUtil.getCurrHour() + "h" + TimeUtil.getCurrMin() + "m: Player " + player.name + " updated successfully! " + (System.currentTimeMillis() - st) + "ms\n");
                    player.dispose();
                } else {
                    Logger.success(TimeUtil.getCurrHour() + "h" + TimeUtil.getCurrMin() + "m: Player " + player.name + " saved successfully! " + (System.currentTimeMillis() - st) + "ms\n");
                }
            } catch (Exception e) {
                Logger.logException(PlayerDAO.class, e, "SAVE-FAILED (data NOT persisted) player " + player.name + " id=" + player.id);
            }
        }
    }

    public static boolean checkLogout(Connection con, Player player) {
        long lastTimeLogout = 0;
        long lastTimeLogin = 0;
        try {
            PreparedStatement ps = con.prepareStatement("select * from account where id = ? limit 1");
            ps.setInt(1, player.getSession().userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                lastTimeLogin = rs.getTimestamp("last_time_login").getTime();
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            return false;
        }
        return lastTimeLogout > lastTimeLogin;
    }

    /** Giao dịch phải dừng nếu không đọc được trạng thái tài khoản. */
    public static boolean checkLogoutForTrade(Connection con, Player player) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "select last_time_logout, last_time_login from account where id = ? limit 1")) {
            ps.setInt(1, player.getSession().userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return true;
                java.sql.Timestamp logout = rs.getTimestamp("last_time_logout");
                java.sql.Timestamp login = rs.getTimestamp("last_time_login");
                return login == null || (logout != null && logout.after(login));
            }
        }
    }

    public static boolean addPointEvent(Player player, int num) {
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement("update account set diem_da_nhan = (diem_da_nhan + ?) where id = ?")) {
            ps.setInt(1, num);
            ps.setInt(2, player.getSession().userId);
            ps.executeUpdate();
            player.getSession().diemReceive += num;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean subcash(Player player, int num) {
        return debitWallet(player, num, true, false);
    }

    private static boolean debitWallet(Player player, int amount, boolean cash, boolean activate) {
        if (player == null || player.getSession() == null || amount < 0 || (amount == 0 && !activate)) return false;
        MySession session = player.getSession();
        synchronized (MySession.accountLock(session.uu)) {
            int balance = cash ? session.cash : session.sotien;
            if (balance < amount) return false;
            try (Connection connection = AlyraManager.getConnection()) {
                if (!database.AccountWallet.debit(connection, cash ? "cash" : "sotien", session.userId, amount, activate)) return false;
                if (cash) session.cash -= amount;
                else session.sotien -= amount;
                if (activate) session.actived = true;
                return true;
            } catch (Exception e) {
                Logger.logException(PlayerDAO.class, e, "Wallet debit failed for account " + session.userId);
                return false;
            }
        }
    }

    /** Debit and VIP entitlement are one conditional SQL update; callers grant items only on success. */
    public static boolean purchaseVip(Player player, int tier) {
        if (player == null || player.getSession() == null || tier < 1 || tier > 3) return false;
        MySession session = player.getSession();
        int cost = switch (tier) { case 1 -> 50_000; case 2 -> 200_000; default -> 500_000; };
        synchronized (MySession.accountLock(session.uu)) {
            boolean received = switch (tier) { case 1 -> session.hasReceivedVIP; case 2 -> session.hasReceivedVIP1; default -> session.hasReceivedVIP2; };
            if (received || session.sotien < cost) return false;
            long now = System.currentTimeMillis();
            try (Connection connection = AlyraManager.getConnection()) {
                if (!database.AccountWallet.purchaseVip(connection, session.userId, tier, now)) return false;
                session.sotien -= cost;
                switch (tier) {
                    case 1 -> { session.vip = 1; session.hasReceivedVIP = true; session.lastTimeReceivedVIP = now; }
                    case 2 -> { session.vip1 = 1; session.hasReceivedVIP1 = true; session.lastTimeReceivedVIP1 = now; }
                    default -> { session.vip2 = 1; session.hasReceivedVIP2 = true; session.lastTimeReceivedVIP2 = now; }
                }
                return true;
            } catch (Exception e) {
                Logger.logException(PlayerDAO.class, e, "VIP purchase failed for account " + session.userId);
                return false;
            }
        }
    }


    public static class AccountTarget {
        public int accountId = -1;
        public String username = "";
        public long playerId = -1;
        public String playerName = "";
    }

    public static AccountTarget resolveAccountTarget(String identifier) {
        AccountTarget target = new AccountTarget();
        if (identifier == null || identifier.trim().isEmpty()) {
            return target;
        }
        identifier = identifier.trim();

        // 1. Kiểm tra player đang online trước
        if (Client.gI() != null) {
            Player p = Client.gI().getPlayerByUserName(identifier);
            if (p != null && p.getSession() != null) {
                target.accountId = p.getSession().userId;
                target.username = p.getSession().uu != null ? p.getSession().uu : identifier;
                target.playerId = p.id;
                target.playerName = p.name != null ? p.name : "";
                return target;
            }
            p = Client.gI().getPlayer(identifier);
            if (p != null && p.getSession() != null) {
                target.accountId = p.getSession().userId;
                target.username = p.getSession().uu != null ? p.getSession().uu : "";
                target.playerId = p.id;
                target.playerName = p.name != null ? p.name : identifier;
                return target;
            }
        }

        // 2. Tra cứu trong CSDL qua AlyraManager
        try (Connection con = AlyraManager.getConnection()) {
            // 2.1 Theo account.username
            try (PreparedStatement ps = con.prepareStatement("SELECT id, username FROM account WHERE username = ?")) {
                ps.setString(1, identifier);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        target.accountId = rs.getInt("id");
                        target.username = rs.getString("username");
                    }
                }
            }

            // 2.2 Theo player.name
            if (target.accountId == -1) {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT a.id, a.username, p.id AS p_id, p.name AS p_name "
                        + "FROM player p JOIN account a ON p.account_id = a.id WHERE p.name = ? LIMIT 1")) {
                    ps.setString(1, identifier);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            target.accountId = rs.getInt("id");
                            target.username = rs.getString("username");
                            target.playerId = rs.getLong("p_id");
                            target.playerName = rs.getString("p_name");
                            return target;
                        }
                    }
                }
            }

            // 2.3 Nếu là số (ID tài khoản hoặc ID nhân vật)
            if (target.accountId == -1) {
                try {
                    long idNum = Long.parseLong(identifier);
                    try (PreparedStatement ps = con.prepareStatement("SELECT id, username FROM account WHERE id = ?")) {
                        ps.setLong(1, idNum);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                target.accountId = rs.getInt("id");
                                target.username = rs.getString("username");
                            }
                        }
                    }
                    if (target.accountId == -1) {
                        try (PreparedStatement ps = con.prepareStatement(
                                "SELECT a.id, a.username, p.id AS p_id, p.name AS p_name "
                                + "FROM player p JOIN account a ON p.account_id = a.id WHERE p.id = ? LIMIT 1")) {
                            ps.setLong(1, idNum);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    target.accountId = rs.getInt("id");
                                    target.username = rs.getString("username");
                                    target.playerId = rs.getLong("p_id");
                                    target.playerName = rs.getString("p_name");
                                    return target;
                                }
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // 2.4 Nếu có accountId nhưng chưa có playerName
            if (target.accountId != -1 && target.playerName.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT id, name FROM player WHERE account_id = ? LIMIT 1")) {
                    ps.setInt(1, target.accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            target.playerId = rs.getLong("id");
                            target.playerName = rs.getString("name");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e, "resolveAccountTarget: " + identifier);
        }
        return target;
    }

    public static boolean buffVnd(String identifier, int vnd, int totalVnd) {
        return buffVnd(identifier, vnd, totalVnd, vnd);
    }

    public static boolean buffVnd(String identifier, int vnd, int totalVnd, int sotien) {
        AccountTarget target = resolveAccountTarget(identifier);
        if (target == null || target.accountId <= 0) {
            return false;
        }

        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE account SET cash = (cash + ?), danap = (danap + ?), sotien = (sotien + ?) WHERE id = ?")) {
            ps.setInt(1, Math.max(0, vnd));
            ps.setInt(2, Math.max(0, totalVnd));
            ps.setInt(3, Math.max(0, sotien));
            ps.setInt(4, target.accountId);
            int rows = ps.executeUpdate();
            if (rows <= 0) {
                return false;
            }
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e, "buffVnd " + identifier);
            return false;
        }

        // Cập nhật mốc nạp
        if (totalVnd > 0 && target.username != null && !target.username.isEmpty()) {
            GiftProgressService.gI().onChargedExternally(target.username, totalVnd);
        }

        // Cập nhật session cho người chơi nếu đang online
        if (Client.gI() != null) {
            Player pl = Client.gI().getPlayerByUser(target.accountId);
            if (pl == null && target.username != null && !target.username.isEmpty()) {
                pl = Client.gI().getPlayerByUserName(target.username);
            }
            if (pl == null && target.playerName != null && !target.playerName.isEmpty()) {
                pl = Client.gI().getPlayer(target.playerName);
            }
            if (pl != null && pl.getSession() != null) {
                if (vnd > 0) {
                    pl.getSession().cash += vnd;
                    Service.gI().sendThongBao(pl, "Bạn vừa được cộng " + Util.format(vnd) + " VNĐ (Cash) vào tài khoản!");
                }
                if (totalVnd > 0) {
                    pl.getSession().danap += totalVnd;
                    Service.gI().sendThongBao(pl, "Bạn vừa được cộng " + Util.format(totalVnd) + " Tổng Nạp!");
                }
                if (sotien > 0) {
                    pl.getSession().sotien += sotien;
                    Service.gI().sendThongBao(pl, "Bạn vừa được cộng " + Util.format(sotien) + " Số Tiền vào tài khoản!");
                }
            }
        }
        return true;
    }

    public static int addCashDanapAndDanapGoc(String username, int cashDanap, int danapGoc) {
        return addCashDanapAndDanapGoc(username, cashDanap, danapGoc, cashDanap);
    }

    public static int addCashDanapAndDanapGoc(String username, int cashDanap, int danapGoc, int sotien) {
        AccountTarget target = resolveAccountTarget(username);
        if (target == null || target.accountId <= 0) {
            return -1;
        }
        boolean ok = buffVnd(username, cashDanap, cashDanap + danapGoc, sotien);
        return ok ? target.accountId : -1;
    }


    public static long getTotalDanap(int accountId) {
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement("select danap from account where id = ?")) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("danap");
                }
            }
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e, "getTotalDanap " + accountId);
        }
        return -1;
    }

    /** {@link #findPlayerIdByName(String)}: không có nhân vật nào mang tên đó. */
    public static final long PLAYER_NAME_NOT_FOUND = -1L;
    /** {@link #findPlayerIdByName(String)}: có nhiều hơn 1 nhân vật trùng tên, không tự chọn hộ. */
    public static final long PLAYER_NAME_AMBIGUOUS = -2L;
    /** {@link #findPlayerIdByName(String)}: lỗi truy vấn database. */
    public static final long PLAYER_NAME_DB_ERROR = -3L;

    public static long findPlayerIdByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return PLAYER_NAME_NOT_FOUND;
        }
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "select id from player where name = ? order by id limit 2")) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return PLAYER_NAME_NOT_FOUND;
                }
                long id = rs.getLong("id");
                if (rs.next()) {
                    Logger.error("Có nhiều nhân vật trùng tên \"" + name.trim() + "\", không thể tra id\n");
                    return PLAYER_NAME_AMBIGUOUS;
                }
                return id;
            }
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e, "findPlayerIdByName " + name);
            return PLAYER_NAME_DB_ERROR;
        }
    }

    public static String findPlayerNameById(long playerId) {
        if (playerId <= 0L) {
            return null;
        }
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement("select name from player where id = ?")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e, "findPlayerNameById " + playerId);
        }
        return null;
    }

    public static void LogAddPoint(String name, int id, int point, String type) {
        System.out.println(name + " - " + id + " - " + point + " - " + type);
//        try {
//            NDVDB.executeUpdate("INSERT INTO histotyevent(name, id_account, event_point, type) VALUES ()", "cc", "cc", "cc", "cc");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    public static boolean createPlAo(int userId, String name, byte gender, int hair) {
        try {
            JSONArray dataArray = new JSONArray();

            dataArray.add(200000); //vàng
            dataArray.add(100); //ngọc xanh
            dataArray.add(0); //hồng ngọc
            dataArray.add(0); //point
            dataArray.add(0); //event

            String inventory = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(Util.nextInt(173)); //map
            dataArray.add(100); //x
            dataArray.add(384); //y
            String location = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0); //giới hạn sức mạnh
            dataArray.add(2000); //sức mạnh
            dataArray.add(2000); //tiềm năng
            dataArray.add(1000); //thể lực
            dataArray.add(1000); //thể lực đầy
            dataArray.add(gender == 0 ? 200 : 100); //hp gốc
            dataArray.add(gender == 1 ? 200 : 100); //ki gốc
            dataArray.add(gender == 2 ? 15 : 10); //sức đánh gốc
            dataArray.add(0); //giáp gốc
            dataArray.add(0); //chí mạng gốc
            dataArray.add(0); //năng động
            dataArray.add(1000000); //hp hiện tại
            dataArray.add(gender == 1 ? 200 : 100); //ki hiện tại
            String point = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(1); //level
            dataArray.add(5); //curent pea
            dataArray.add(0); //is upgrade
            dataArray.add(new Date().getTime()); //last time harvest
            dataArray.add(new Date().getTime()); //last time upgrade
            String magicTree = dataArray.toJSONString();
            dataArray.clear();
            /**
             *
             * [
             * {"temp_id":"1","option":[[5,7],[7,3]],"create_time":"49238749283748957""},
             * {"temp_id":"1","option":[[5,7],[7,3]],"create_time":"49238749283748957""},
             * {"temp_id":"-1","option":[],"create_time":"0""}, ... ]
             */

            int idAo = gender == 0 ? 0 : gender == 1 ? 1 : 2;
            int idQuan = gender == 0 ? 6 : gender == 1 ? 7 : 8;
            int def = gender == 2 ? 3 : 2;
            int hp = gender == 0 ? 30 : 20;

            JSONArray item = new JSONArray();
            JSONArray options = new JSONArray();
            JSONArray opt = new JSONArray();
            for (int i = 0; i < 11; i++) {
                if (i == 0) { //áo
                    opt.add(47); //id option
                    opt.add(def); //param option
                    item.add(idAo); //id item
                    item.add(1); //số lượng
                    options.add(opt.toJSONString());
                    opt.clear();
                } else if (i == 1) { //quần
                    opt.add(6); //id option
                    opt.add(hp); //param option
                    item.add(idQuan); //id item
                    item.add(1); //số lượng
                    options.add(opt.toJSONString());
                    opt.clear();
                } else {
                    item.add(-1); //id item
                    item.add(0); //số lượng
                }
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBody = dataArray.toJSONString();
            dataArray.clear();

            for (int i = 0; i < 20; i++) {
                if (i == 0) { //thỏi vàng
                    opt.add(1); //id option
                    opt.add(500); //param option
                    item.add(521); //id item
                    item.add(1); //số lượng
                    options.add(opt.toJSONString());
                    opt.clear();
                } else {
                    item.add(-1); //id item
                    item.add(0); //số lượng
                }
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBag = dataArray.toJSONString();
            dataArray.clear();

            for (int i = 0; i < 20; i++) {
                if (i == 0) { //rada
                    opt.add(14); //id option
                    opt.add(1); //param option
                    item.add(12); //id item
                    item.add(1); //số lượng
                    options.add(opt.toJSONString());
                    opt.clear();
                } else {
                    item.add(-1); //id item
                    item.add(0); //số lượng
                }
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBox = dataArray.toJSONString();
            dataArray.clear();

            for (int i = 0; i < 110; i++) {
                item.add(-1); //id item
                item.add(0); //số lượng
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsBoxLuckyRound = dataArray.toJSONString();
            dataArray.clear();

            for (int i = 0; i < 110; i++) {
                item.add(-1); //id item
                item.add(0); //số lượng
                item.add(options.toJSONString()); //full option item
                item.add(System.currentTimeMillis()); //thời gian item được tạo
                dataArray.add(item.toJSONString());
                options.clear();
                item.clear();
            }
            String itemsDaBan = dataArray.toJSONString();
            dataArray.clear();

            String friends = dataArray.toJSONString();
            String enemies = dataArray.toJSONString();

            dataArray.add(0); //id nội tại
            dataArray.add(0); //chỉ số 1
            dataArray.add(0); //chỉ số 2
            dataArray.add(0); //số lần mở
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            String intrinsic = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0); //bổ huyết
            dataArray.add(0); //bổ khí
            dataArray.add(0); //giáp xên
            dataArray.add(0); //cuồng nộ
            dataArray.add(0); //ẩn danh
            dataArray.add(0); //bổ huyết
            dataArray.add(0); //bổ khí
            dataArray.add(0); //giáp xên
            dataArray.add(0); //cuồng nộ
            dataArray.add(0); //ẩn danh
            dataArray.add(0); //mở giới hạn sức mạnh
            dataArray.add(0); //máy dò
            dataArray.add(0); //thức ăn cold
            dataArray.add(0); //icon thức ăn cold
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            dataArray.add(0); //
            String itemTime = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0); //
            String itemTime_ndung = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(31); //id nhiệm vụ
            dataArray.add(0); //index nhiệm vụ con
            dataArray.add(0); //số lượng đã làm
            String task = dataArray.toJSONString();
            dataArray.clear();

            String mabuEgg = dataArray.toJSONString();

            dataArray.add(System.currentTimeMillis()); //bùa trí tuệ
            dataArray.add(System.currentTimeMillis()); //bùa mạnh mẽ
            dataArray.add(System.currentTimeMillis()); //bùa da trâu
            dataArray.add(System.currentTimeMillis()); //bùa oai hùng
            dataArray.add(System.currentTimeMillis()); //bùa bất tử
            dataArray.add(System.currentTimeMillis()); //bùa dẻo dai
            dataArray.add(System.currentTimeMillis()); //bùa thu hút
            dataArray.add(System.currentTimeMillis()); //bùa đệ tử
            dataArray.add(System.currentTimeMillis()); //bùa trí tuệ x3
            dataArray.add(System.currentTimeMillis()); //bùa trí tuệ x4
            String charms = dataArray.toJSONString();
            dataArray.clear();

            int[] skillsArr = gender == 0 ? new int[]{0, 1, 6, 9, 10, 20, 22, 19}
                    : gender == 1 ? new int[]{2, 3, 7, 11, 12, 17, 18, 19}
                    : new int[]{4, 5, 8, 13, 14, 21, 23, 19};
            //[{"temp_id":"4","point":0,"last_time_use":0},]

            JSONArray skill = new JSONArray();
            for (int i = 0; i < skillsArr.length; i++) {
                skill.add(skillsArr[i]); //id skill
                if (i == 0) {
                    skill.add(1); //level skill
                } else {
                    skill.add(0); //level skill
                }
                skill.add(0); //thời gian sử dụng trước đó
                dataArray.add(skill.toString());
                skill.clear();
            }
            String skills = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(gender == 0 ? 0 : gender == 1 ? 2 : 4);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            dataArray.add(-1);
            String skillsShortcut = dataArray.toJSONString();
            dataArray.clear();

            String petData = dataArray.toJSONString();

            JSONArray blackBall = new JSONArray();
            for (int i = 1; i <= 7; i++) {
                blackBall.add(0);
                blackBall.add(0);
                blackBall.add(0);
                dataArray.add(blackBall.toJSONString());
                blackBall.clear();
            }
            String dataBlackBall = dataArray.toString();
            dataArray.clear();

            dataArray.add(-1); //id side task
            dataArray.add(0); //thời gian nhận
            dataArray.add(0); //số lượng đã làm
            dataArray.add(0); //số lượng cần làm
            dataArray.add(20); //số nhiệm vụ còn lại có thể nhận
            dataArray.add(0); //mức độ nhiệm vụ
            String dataSideTask = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0);  // thời gian nhận
            dataArray.add(0);  // Sử Dụng Loa Liên Vũ Trụ 10 Lần Trong Ngày
            dataArray.add(0);  // Nạp Tích Lũy 3250 Ngọc Trong Ngày
            dataArray.add(0);  // Ước Rồng Thần 1 Sao x10 Lần
            dataArray.add(0);  // Hạ Gục Cumber, Black Goku, Cooler, Xên (30 Lần)
            dataArray.add(0);  // Đập 3 Trang Bị +7 Trong Ngày
            dataArray.add(0);  // Top 1 Đại Hội Võ Đài Siêu Hạng
            dataArray.add(0);  // Hoàn Thành 10 Nhiệm Vụ Siêu Khó Tại Bò Mộng
            dataArray.add(0);  // Đánh Bại, Hoặc Cho Xương Sói 20 Lần
            dataArray.add(0);  // Hoàn Thành 5 Lần Nhiệm Vụ Cho Nước Xinbato
            dataArray.add(0);  // Nhặt Đồ 500 Lần Trong Ngày
            dataArray.add(0);  // Tiêu Diệt 30 Lần Boss Ăn Trộm
            dataArray.add(0);  // Tiêu Diệt 30 Lần Boss Ở Dơ
            dataArray.add(0);  // Sử Dụng Loa Liên Vũ Trụ 10 Lần Trong Ngày
            String dataDanhhieu = dataArray.toJSONString();
            dataArray.clear();

            dataArray.add(0);
            dataArray.add(0);
            String dataClan = dataArray.toJSONString();
            dataArray.clear();

            AlyraManager.executeUpdate("insert into player"
                    + "(account_id, name, head, gender, have_tennis_space_ship, clan_id, "
                    + "data_inventory, data_location, data_point, data_magic_tree, items_body, "
                    + "items_bag, items_box, items_box_lucky_round, items_daban, friends, enemies, data_intrinsic, data_item_time, devndung_time,"
                    + "data_task, data_mabu_egg, data_charm, skills, skills_shortcut, pet,"
                    + "data_black_ball, data_side_task, data_danh_hieu, data_clan) "
                    + "values ()", userId, name, hair, gender, 0, -1, inventory, location, point, magicTree,
                    itemsBody, itemsBag, itemsBox, itemsBoxLuckyRound, itemsDaBan, friends, enemies, intrinsic,
                    itemTime, itemTime_ndung, task, mabuEgg, charms, skills, skillsShortcut, petData, dataBlackBall, dataSideTask, dataDanhhieu, dataClan);
            Logger.success("New player created successfully!\n");
            return true;
        } catch (Exception e) {
            Logger.logException(PlayerDAO.class, e, "Lỗi tạo player mới");
            return false;
        }
    }

    public static boolean subvnd(Player player, int num) {
        return debitWallet(player, num, false, false);
    }

    public static boolean subVND2(Player player, int sotien) {
        if (player == null || player.getSession() == null || sotien < 0) return false;
        return debitWallet(player, player.getSession().isAdmin ? 0 : sotien, false, true);
    }

    public static boolean addVndNe(Player player, int sotien) {
        if (player == null) {
            return false;
        }
        MySession session = player.getSession();
        if (session == null) {
            return false;
        }
        String sql = "UPDATE account SET sotien = sotien + ? WHERE id = ?";
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, sotien);
            ps.setInt(2, session.userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                session.sotien += sotien;
                return true;
            } else {
                Logger.log("Không tìm thấy account với id=" + session.userId);
            }

        } catch (SQLException e) {
            Logger.logException(PlayerDAO.class, e, "Lỗi update vnd " + player.name);
        }
        return false;
    }

    public static boolean Addvnd(String username, int num) {
        return buffVnd(username, num, 0);
    }

    public static boolean Addtotalvnd(String username, int num) {
        return buffVnd(username, 0, num);
    }

    public static boolean Addsotien(String username, int num) {
        return buffVnd(username, 0, 0, num);
    }

    public static boolean updateMocNap(String username, int num) {
        // try-with-resources: trước đây connection/statement bị rò rỉ khi executeUpdate ném exception
        try (Connection con = AlyraManager.getConnection();
             PreparedStatement ps = con.prepareStatement("update account set mocnap = ? where username = ?")) {
            username = username.trim();
            ps.setInt(1, num);
            ps.setString(2, username);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(" Lỗi của BktNè ở hàm updateMocNap, gặp lỗi này kêu bkt fix ");
            return false;
        }
    }

    public static boolean Addtotalvnd2(String username, int num) {
        return buffVnd(username, 0, num);
    }

    public static void subVIP(Player player) {
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE account SET vip = ?, hasReceivedVIP = 1, lastTimeReceivedVIP = ? WHERE id = ?")) {
            // Thực thi câu lệnh SQL
            ps.setInt(1, player.getSession().vip);
            ps.setLong(2, player.getSession().lastTimeReceivedVIP = System.currentTimeMillis());
            ps.setInt(3, player.getSession().userId);
            ps.executeUpdate();
            player.getSession().hasReceivedVIP = true;
        } catch (SQLException e) {
            // Ghi lại log nếu có lỗi
            Logger.logException(PlayerDAO.class, e, "Lỗi update hasReceivedVIP " + player.name);
        }
    }

    public static void subVIP1(Player player) {
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE account SET vip1 = ?, hasReceivedVIP1 = 1, lastTimeReceivedVIP1 = ? WHERE id = ?")) {
            // Thực thi câu lệnh SQL
            ps.setInt(1, player.getSession().vip1);
            ps.setLong(2, player.getSession().lastTimeReceivedVIP1 = System.currentTimeMillis());
            ps.setInt(3, player.getSession().userId);
            ps.executeUpdate();
            player.getSession().hasReceivedVIP1 = true;
        } catch (SQLException e) {
            // Ghi lại log nếu có lỗi
            Logger.logException(PlayerDAO.class, e, "Lỗi update hasReceivedVIP1 " + player.name);
        }
    }

    public static void subVIP2(Player player) {
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE account SET vip2 = ?, hasReceivedVIP2 = 1, lastTimeReceivedVIP2 = ? WHERE id = ?")) {
            // Thực thi câu lệnh SQL
            ps.setInt(1, player.getSession().vip2);
            ps.setLong(2, player.getSession().lastTimeReceivedVIP2 = System.currentTimeMillis());
            ps.setInt(3, player.getSession().userId);
            ps.executeUpdate();
            player.getSession().hasReceivedVIP2 = true;
        } catch (SQLException e) {
            // Ghi lại log nếu có lỗi
            Logger.logException(PlayerDAO.class, e, "Lỗi update hasReceivedVIP2 " + player.name);
        }
    }

    public static boolean addBossPoint(Player player, int num) {
        if (player == null || player.getSession() == null) {
            return false;
        }
        String sql = "UPDATE player SET sanboss = sanboss + ? WHERE id = ?";
        try (Connection con = AlyraManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, num);
            ps.setInt(2, (int) player.id);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                // nếu muốn lưu tạm trong RAM cũng có thể cộng thêm
                player.sanBoss += num;
                Logger.success("Cộng " + num + " điểm săn boss cho " + player.name + " thành công.");
                return true;
            } else {
                Logger.error("Không tìm thấy player id=" + player.id + " để cộng điểm săn boss.");
            }

        } catch (SQLException e) {
            Logger.logException(PlayerDAO.class, e, "Lỗi addBossPoint cho " + player.name);
        }
        return false;
    }

}
