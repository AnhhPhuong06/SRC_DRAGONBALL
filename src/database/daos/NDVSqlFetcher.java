package database.daos;


import models.radar.OptionCard;
import models.radar.Card;
import database.AlyraManager;
import database.AlyraResultSet;
import consts.ConstPlayer;
import data.DataGame;
import java.io.BufferedWriter;
import java.io.FileWriter;
import models.clan.Clan;
import models.clan.ClanMember;
import models.item.Item;
import models.item.ItemTime;
import models.npc.specialnpc.MabuEgg;
import models.npc.specialnpc.BillEgg;
import models.npc.specialnpc.BlackEgg;
import models.npc.specialnpc.MagicTree;
import models.player.Enemy;
import models.player.Friend;
import models.player.Fusion;
import models.player.Pet;
import models.player.Player;
import models.skill.Skill;
import models.task.TaskMain;
import server.Client;
import server.Manager;
import network.session.MySession;
import models.AntiLogin;
import services.player.ClanService;
import services.player.IntrinsicService;
import services.ItemService;
import services.map.MapService;
import services.Service;
import services.TaskService;
import utils.Logger;
import utils.SkillUtil;
import utils.TimeUtil;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import models.Template.AchievementQuest;

import utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import services.player.PlayerService;
import services.top.TopAutoService;

public class NDVSqlFetcher {

    public static synchronized Player login(MySession session, AntiLogin al) {
        if (TopAutoService.gI().checkTopRunning()) {
            Service.gI().sendThongBaoOK(session, "Đang tiến hành trao quà TOP vui lòng quay lại sau!");
        }
        Player player = null;
        AlyraResultSet rs = null;
        Player plInGame;
        try {
            rs = AlyraManager.executeQuery("select * from account where username = ? and password = ?", session.uu, session.pp);
            if (rs.first()) {
                session.userId = rs.getInt("account.id");
                session.isAdmin = rs.getBoolean("is_admin");
                if (!services.LoginAccessConfig.canLogin(session.isAdmin)) {
                    Service.gI().sendThongBaoOK(session, services.LoginAccessConfig.DENIED_MESSAGE);
                    return null;
                }
                if (Client.gI().getPlayerByUser(session.userId) != null) {
                    Client.gI().kickSession(Client.gI().getPlayerByUser(session.userId).getSession());
                    Client.gI().kickSession(session);
                    Service.gI().sendLoginFail(session, true);
                    return null;
                }
                session.lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                session.actived = rs.getBoolean("active");
                session.goldBar = rs.getInt("account.thoi_vang");
                session.luotquay = rs.getInt("account.luotquay");
                session.gold = rs.getLong("account.vang");
                session.eventPoint = rs.getInt("account.event_point");
                session.bdPlayer = rs.getDouble("account.bd_player");
                session.cash = rs.getInt("cash");
                session.danap = rs.getInt("danap");
                session.mocNap = rs.getInt("mocnap");
                session.diemReceive = rs.getInt("diem_da_nhan");
                session.sotien = rs.getInt("sotien");
                session.vip = rs.getInt("vip");
                session.vip1 = rs.getInt("vip1");
                session.vip2 = rs.getInt("vip2");
                session.hasReceivedVIP = rs.getBoolean("hasReceivedVIP");
                session.hasReceivedVIP1 = rs.getBoolean("hasReceivedVIP1");
                session.hasReceivedVIP2 = rs.getBoolean("hasReceivedVIP2");
                session.lastTimeReceivedVIP = rs.getLong("lastTimeReceivedVIP");
                session.lastTimeReceivedVIP1 = rs.getLong("lastTimeReceivedVIP1");
                session.lastTimeReceivedVIP2 = rs.getLong("lastTimeReceivedVIP2");
                long lastTimeLogin = rs.getTimestamp("last_time_login").getTime();
                int secondsPass1 = (int) ((System.currentTimeMillis() - lastTimeLogin) / 1000);
                long lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                int secondsPass = (int) ((System.currentTimeMillis() - lastTimeLogout) / 1000);
                if (!session.isAdmin && secondsPass < Manager.SECOND_WAIT_LOGIN) {
                    Service.gI().sendThongBaoOK(session, "Vui lòng chờ " + (Manager.SECOND_WAIT_LOGIN - secondsPass) + "s");
                    return null;
                }
                long createTime = rs.getTimestamp("create_time").getTime();
                int deltaTime = (int) ((System.currentTimeMillis() - createTime) / 1000);
                if (rs.getBoolean("ban")) {
                    Service.gI().sendThongBaoOK(session, "Tài khoản này đang bị khóa. Liên hệ Admin để biết thêm thông tin");
                } else if (rs.getTimestamp("last_time_login").getTime() > session.lastTimeLogout) {

                    String logoutQuery = "UPDATE account SET last_time_logout = ? WHERE id = ?;";
                    AlyraManager.executeUpdate(logoutQuery, new Timestamp(System.currentTimeMillis()), session.userId);
                    Service.gI().sendThongBaoOK(session, "Vui lòng đăng nhập lại");

                } else if (Client.gI().getPlayerByUser(session.userId) != null) {
                    plInGame = Client.gI().getPlayerByUser(session.userId);
                    Client.gI().kickSession(plInGame.getSession());
                    Client.gI().kickSession(session);
                    Service.gI().sendThongBaoOK(session, "Ai đó đang đăng nhập tài khoản?");
                } else {
                    if (!session.isAdmin && secondsPass < Manager.SECOND_WAIT_LOGIN) {
                        Service.gI().sendThongBaoOK(session, "Vui lòng chờ " + (Manager.SECOND_WAIT_LOGIN - secondsPass) + "s");
                    } else {
                        rs = AlyraManager.executeQuery("select * from player where account_id = ? limit 1", session.userId);
                        if (!rs.first()) {
                            //-28 -4 version data game
                            DataGame.sendVersionGame(session);
                            //-31 data item background
                            DataGame.sendDataItemBG(session);
                            Service.gI().switchToCreateChar(session);
                        } else {
                            plInGame = Client.gI().getPlayerByUser(session.userId);
                            if (plInGame != null) {
                                Client.gI().kickSession(plInGame.getSession());
                                Client.gI().kickSession(session);
                                return null;
                            }
                            if ((player = loadPlayer(rs, false)) != null) {
                                player.isPlayer = true;
                                player.deltaTime = deltaTime;
                                player.isNewMember = !Util.isTimeDifferenceGreaterThanNDays(createTime, 45);
                                try {
                                    AlyraManager.executeUpdate("UPDATE account SET last_time_login=?, ip_address=? WHERE id=?",
                                            new Timestamp(System.currentTimeMillis()), session.ipAddress, session.userId);
                                } catch (Exception e) {
                                    Client.gI().kickSession(session);
                                    player.dispose();
                                    return null;
                                }
                                } else {
                                Logger.error("Nạp nhân vật thất bại cho account " + session.userId + "\n");
                                Service.gI().sendThongBaoOK(session, "Không tải được dữ liệu nhân vật, vui lòng thử lại sau hoặc liên hệ Admin.");
                                Client.gI().kickSession(session);

                            }
                        }
                    }
                }
                al.reset();
            } else {
                Service.gI().sendThongBaoOK(session, "Thông tin tài khoản hoặc mật khẩu không chính xác");
                Service.gI().sendLoginFail(session, false);
                al.wrong();
            }
        } catch (Exception e) {
            Logger.error(session.uu);
            if (player != null) {
                player.dispose();
                player = null;
            }
            Logger.logException(NDVSqlFetcher.class, e);
            Service.gI().sendThongBaoOK(session, "Có lỗi khi đăng nhập, vui lòng thử lại sau.");
        } finally {
            if (rs != null) {
                rs.dispose();
            }
        }
        return player;
    }

    public static Player loadById(long id) {
        Player player = null;
        AlyraResultSet rs = null;
        try {
            // Thực thi truy vấn SQL và lấy kết quả
            rs = AlyraManager.executeQuery("select * from player where id = ? limit 1", id);

            // Nếu có dữ liệu và dữ liệu hợp lệ
            if (rs.first()) {
                // Gọi hàm loadPlayer để xử lý dữ liệu và tạo đối tượng player
                player = loadPlayer(rs, true);
                if (player != null) {
                    player.isOffline = true;
                    player.idMark.setLoadedAllDataPlayer(true);
                }
            }
        } catch (Exception e) {
            // Nếu có lỗi, chắc chắn giải phóng tài nguyên và log lỗi
            if (player != null) {
                player.dispose();
                player = null;
            }
            Logger.logException(NDVSqlFetcher.class, e);
        } finally {
            // Đảm bảo giải phóng tài nguyên ResultSet
            if (rs != null) {
                rs.dispose();
            }
        }
        return player;
    }

    private static synchronized Player loadPlayer(AlyraResultSet rs, boolean isOffline) throws Exception {
        Player player = null;
        try {
            long plHp;
            long plMp;
            JSONArray dataArray;

            player = new Player();

            //base info
            player.id = rs.getInt("id");
            player.name = rs.getString("name");
            player.head = rs.getShort("head");
            player.gender = rs.getByte("gender");
            if (player.head == -1) {
                switch (player.gender) {
                    case 0 ->
                        player.head = 64;
                    case 1 ->
                        player.head = 9;
                    case 2 ->
                        player.head = 6;
                }
            }
            player.haveTennisSpaceShip = rs.getBoolean("have_tennis_space_ship");
            player.daubuoi = rs.getInt("daubuoi");
            int clanId = rs.getInt("clan_id");
            if (clanId != -1) {
                try {
                    Clan clan = ClanService.gI().getClanById(clanId);
                    for (ClanMember cm : clan.getMembers()) {
                        if (cm.id == player.id) {
                            if (!isOffline) {
                                clan.addMemberOnline(player);
                            }
                            player.clan = clan;
                            player.clanMember = cm;
                            break;
                        }
                    }
                } catch (Exception e) {
                    player.clan = null;
                }
            }
            dataArray = (JSONArray) JSONValue.parse(rs.getString("dien_sukien"));
            if (dataArray != null) {
                player.diemsukien = dataArray.size() > 0 ? Integer.parseInt(dataArray.get(0).toString()) : 0;
                player.phaobong = dataArray.size() > 1 ? Integer.parseInt(dataArray.get(1).toString()) : 0;
                player.lixi = dataArray.size() > 2 ? Integer.parseInt(dataArray.get(2).toString()) : 0;
                player.mocVongQuay = dataArray.size() > 3 ? Integer.parseInt(dataArray.get(3).toString()) : 0;
                dataArray.clear();
            }

            player.banhtet = rs.getLong("banhtet");
            player.banhtrung = rs.getLong("banhchung");
            //data danh hiệu shop
            dataArray = (JSONArray) JSONValue.parse(rs.getString("danh_hieu_shop"));
            int dh1 = Integer.parseInt(String.valueOf(dataArray.get(0)));
            int dh2 = Integer.parseInt(String.valueOf(dataArray.get(1)));
            int dh3 = Integer.parseInt(String.valueOf(dataArray.get(2)));
            int dh4 = Integer.parseInt(String.valueOf(dataArray.get(3)));
            int dh5 = Integer.parseInt(String.valueOf(dataArray.get(4)));
            int dh6 = Integer.parseInt(String.valueOf(dataArray.get(5)));
            int dh7 = Integer.parseInt(String.valueOf(dataArray.get(6)));
            int dh8 = Integer.parseInt(String.valueOf(dataArray.get(7)));

            player.effect.setPointDaiGiaMoiNhu(dh1);
            player.effect.setPointTrumUocRong(dh2);
            player.effect.setPointTrumSanBoss(dh3);
            player.effect.setPointThanhDapDo(dh4);
            player.effect.setPointNongDanChamChi(dh5);
            player.effect.setPointOngThanVeChai(dh6);
            player.effect.setPointBiMocSachTui(dh7);
            player.effect.setPointPhanCung(dh8);
            dataArray.clear();
            //Nhận bùa ngẫu nhiên
            dataArray = (JSONArray) JSONValue.parse(rs.getString("buarandom"));
            int b1 = Integer.parseInt(String.valueOf(dataArray.get(0)));
            int b2 = Integer.parseInt(String.valueOf(dataArray.get(1)));
            player.luotNhanBuaMienPhi = b1;
            player.diemDanhSuKien = b2;
            dataArray.clear();
            //data kim lượng
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_inventory"));
                player.inventory.gold = Long.parseLong(String.valueOf(dataArray.get(0)));
                player.inventory.gem = Integer.parseInt(String.valueOf(dataArray.get(1)));
                player.inventory.ruby = Integer.parseInt(String.valueOf(dataArray.get(2)));
                player.inventory.coupon = dataArray.size() >= 4 ? Integer.parseInt(String.valueOf(dataArray.get(3))) : 0;
                player.inventory.event = 0;
                player.inventory.diemDT = dataArray.size() >= 6 ? Integer.parseInt(String.valueOf(dataArray.get(5))) : 0;
                player.inventory.diemXen = dataArray.size() >= 7 ? Integer.parseInt(String.valueOf(dataArray.get(6))) : 0;
                dataArray.clear();
            } catch (Exception e) {
                Object parsed = JSONValue.parse(rs.getString("data_inventory"));
                if (parsed instanceof JSONObject ob) {
                    player.inventory.gold = ob.containsKey("gold") ? Long.parseLong(String.valueOf(ob.get("gold"))) : 0L;
                    player.inventory.gem = ob.containsKey("gem") ? Integer.parseInt(String.valueOf(ob.get("gem"))) : 0;
                    player.inventory.ruby = ob.containsKey("ruby") ? Integer.parseInt(String.valueOf(ob.get("ruby"))) : 0;
                    player.inventory.coupon = 0;
                    player.inventory.event = 0;
                    player.inventory.diemDT = 0;
                    player.inventory.diemXen = 0;
                }
            }

            PlayerService.gI().dailyLogin(player);

            player.firstTimeLogin = rs.getTimestamp("firstTimeLogin");

            //data tọa độ
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_location"));
            int mapId = Integer.parseInt(String.valueOf(dataArray.get(0)));
            player.location.x = Integer.parseInt(String.valueOf(dataArray.get(1)));
            player.location.y = Integer.parseInt(String.valueOf(dataArray.get(2)));
            player.location.lastTimeplayerMove = System.currentTimeMillis();
            player.map182Deadline = dataArray.size() >= 4 ? Long.parseLong(String.valueOf(dataArray.get(3))) : 0L;
            player.hitBossLv20Time = dataArray.size() >= 5 ? Long.parseLong(String.valueOf(dataArray.get(4))) : 0L;
            player.lastTimeWinGuruWar = dataArray.size() >= 6 ? Long.parseLong(String.valueOf(dataArray.get(5))) : 0L;
            player.isBuffGuruWar = (player.lastTimeWinGuruWar > 0 && (System.currentTimeMillis() - player.lastTimeWinGuruWar < 24 * 60 * 60 * 1000L));
            if (mapId == 51 || MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId) || MapService.gI().isMapSieuThanhThuy(mapId) || MapService.gI().isMapMabu2H(mapId)) {
                mapId = player.gender + 21;
                player.location.x = 300;
                player.location.y = 336;
            }
            if (MapService.gI().isMapMaBu(mapId)) {
                if (!TimeUtil.isMabuOpen()) {
                    mapId = player.gender + 21;
                    player.location.x = 300;
                    player.location.y = 336;
                }
            }
            if (mapId == 112) {
                player.location.y = 408;
            } else if (mapId == 129 || mapId == 113) {
                player.location.y = 360;
            }
            if (mapId == 49) {
                mapId = 45;
                player.location.x = 359;
                player.location.y = 408;
            }

            if (MapService.gI().isMapVaoBangVatPham(mapId)) {
                mapId = player.gender + 21;
                player.location.x = 300;
                player.location.y = 336;
                player.map182Deadline = 0;
            }

            if (mapId == 181) {
                mapId = player.gender + 21;
                player.location.x = 300;
                player.location.y = 336;
            }
            player.zone = MapService.gI().getMapCanJoin(player, mapId, -1);
            dataArray.clear();

            //data chỉ số
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_point"));
            player.nPoint.limitPower = Byte.parseByte(String.valueOf(dataArray.get(0)));
            player.nPoint.power = Long.parseLong(String.valueOf(dataArray.get(1)));
            player.nPoint.tiemNang = Long.parseLong(String.valueOf(dataArray.get(2)));
            player.nPoint.stamina = Short.parseShort(String.valueOf(dataArray.get(3)));
            player.nPoint.maxStamina = Short.parseShort(String.valueOf(dataArray.get(4)));
            player.nPoint.hpg = Long.parseLong(String.valueOf(dataArray.get(5)));
            player.nPoint.mpg = Long.parseLong(String.valueOf(dataArray.get(6)));
            player.nPoint.dameg = Long.parseLong(String.valueOf(dataArray.get(7)));
            player.nPoint.defg = Integer.parseInt(String.valueOf(dataArray.get(8)));
            player.nPoint.critg = Byte.parseByte(String.valueOf(dataArray.get(9)));
            dataArray.get(10); //** Năng động
            plHp = Long.parseLong(String.valueOf(dataArray.get(11)));
            plMp = Long.parseLong(String.valueOf(dataArray.get(12)));
            dataArray.clear();

            //data đậu thần
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_magic_tree"));
            byte level = Byte.parseByte(String.valueOf(dataArray.get(0)));
            byte currPea = Byte.parseByte(String.valueOf(dataArray.get(1)));
            boolean isUpgrade = Byte.parseByte(String.valueOf(dataArray.get(2))) == 1;
            long lastTimeHarvest = Long.parseLong(String.valueOf(dataArray.get(3)));
            long lastTimeUpgrade = Long.parseLong(String.valueOf(dataArray.get(4)));
            player.magicTree = new MagicTree(player, level, currPea, lastTimeHarvest, isUpgrade, lastTimeUpgrade);
            dataArray.clear();

            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_KOL"));
            // Player cũ có thể chỉ có [count, maxCount, level] (3 phần tử) — thiếu 14 flag reward.
            // Đọc bảo vệ theo size; thiếu → mặc định chưa nhận thưởng.
            int kolSize = dataArray != null ? dataArray.size() : 0;
            if (kolSize > 0) player.playerTask.task_kol.count = Long.parseLong(String.valueOf(dataArray.get(0)));
            if (kolSize > 1) player.playerTask.task_kol.maxCount = Long.parseLong(String.valueOf(dataArray.get(1)));
            if (kolSize > 2) player.playerTask.task_kol.level = Integer.parseInt(String.valueOf(dataArray.get(2)));
            player.playerTask.task_kol.sanitize();
            // Đọc đủ 40 + 40 flag, khớp thứ tự SAVE (PlayerDAO): isRewarded[0..N) rồi isVipRewarded[0..N).
            int rewardLen = player.playerTask.task_kol.isRewarded.length;
            for (int i = 0; i < rewardLen; i++) {
                int idx = 3 + i;
                if (idx >= kolSize) break;
                int val = Integer.parseInt(String.valueOf(dataArray.get(idx)));
                player.playerTask.task_kol.isRewarded[i] = (val == 1);
            }
            for (int i = 0; i < player.playerTask.task_kol.isVipRewarded.length; i++) {
                int idx = 3 + rewardLen + i;
                if (idx >= kolSize) break;
                int val = Integer.parseInt(String.valueOf(dataArray.get(idx)));
                player.playerTask.task_kol.isVipRewarded[i] = (val == 1);
            }
            if (dataArray != null) dataArray.clear();

            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("loadtimetop"));
                player.lasttimeTD = Long.parseLong(String.valueOf(dataArray.get(0)));
                player.diemmaydam = Long.parseLong(String.valueOf(dataArray.get(1)));
            } catch (Exception e) {
            }

            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("point_pokemon"));
                player.point_pokemon = Integer.parseInt(String.valueOf(dataArray.get(0)));
            } catch (Exception e) {
            }

            //data phần thưởng sao đen
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_black_ball"));
            JSONArray dataBlackBall;
            for (int i = 0; i < dataArray.size() && i < player.rewardBlackBall.timeOutOfDateReward.length; i++) {
                dataBlackBall = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(i)));
                player.rewardBlackBall.timeOutOfDateReward[i] = Long.parseLong(String.valueOf(dataBlackBall.get(0)));
                player.rewardBlackBall.lastTimeGetReward[i] = Long.parseLong(String.valueOf(dataBlackBall.get(1)));
                try {
                    player.rewardBlackBall.quantilyBlackBall[i] = dataBlackBall.get(2) != null ? Integer.parseInt(String.valueOf(dataBlackBall.get(2))) : 0;
                } catch (NumberFormatException e) {
                    player.rewardBlackBall.quantilyBlackBall[i] = player.rewardBlackBall.timeOutOfDateReward[i] != 0 ? 1 : 0;
                }
                dataBlackBall.clear();
            }
            dataArray.clear();

            //data body
            dataArray = (JSONArray) JSONValue.parse(rs.getString("items_body"));
            for (int i = 0; i < dataArray.size(); i++) {
                Item item;
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(i).toString());
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1 && ItemService.gI().getTemplate(tempId) != null) {
                    item = ItemService.gI().createItemFromData(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                    JSONArray options = (JSONArray) JSONValue.parse(String.valueOf(dataItem.get(2)).replaceAll("\"", ""));
                    for (int j = 0; j < options.size(); j++) {
                        JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(options.get(j)));
                        item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                Integer.parseInt(String.valueOf(opt.get(1)))));
                    }
                    long cTime = 0;
                    if (dataItem.size() > 3 && dataItem.get(3) != null) {
                        try {
                            cTime = Long.parseLong(String.valueOf(dataItem.get(3)));
                        } catch (Exception ignored) {}
                    }
                    item.createTime = cTime;
                    if (ItemService.gI().isOutOfDateTime(item)) {
                        item = ItemService.gI().createItemNull();
                    }
                } else {
                    item = ItemService.gI().createItemNull();
                }
                player.inventory.itemsBody.add(item);
            }
            if (player.inventory.itemsBody.size() == 10) {
                player.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
            if (player.inventory.itemsBody.size() == 11) {
                player.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
            if (player.inventory.itemsBody.size() == 12) {
                player.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
            dataArray.clear();

            //data bag
            dataArray = (JSONArray) JSONValue.parse(rs.getString("items_bag"));
            for (int i = 0; i < dataArray.size(); i++) {
                Item item;
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(i).toString());
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1 && ItemService.gI().getTemplate(tempId) != null) {
                    item = ItemService.gI().createItemFromData(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                    JSONArray options = (JSONArray) JSONValue.parse(String.valueOf(dataItem.get(2)).replaceAll("\"", ""));
                    for (int j = 0; j < options.size(); j++) {
                        JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(options.get(j)));
                        item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))), Integer.parseInt(String.valueOf(opt.get(1)))));
                    }
                    long cTime = 0;
                    if (dataItem.size() > 3 && dataItem.get(3) != null) {
                        try {
                            cTime = Long.parseLong(String.valueOf(dataItem.get(3)));
                        } catch (Exception ignored) {}
                    }
                    item.createTime = cTime;
                    if (ItemService.gI().isOutOfDateTime(item)) {
                        item = ItemService.gI().createItemNull();
                    }
                } else {
                    item = ItemService.gI().createItemNull();
                }
                player.inventory.itemsBag.add(item);
            }
            dataArray.clear();

            //data box
            dataArray = (JSONArray) JSONValue.parse(rs.getString("items_box"));
            for (int i = 0; i < dataArray.size(); i++) {
                Item item;
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(i).toString());
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1 && ItemService.gI().getTemplate(tempId) != null) {
                    item = ItemService.gI().createItemFromData(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                    JSONArray options = (JSONArray) JSONValue.parse(String.valueOf(dataItem.get(2)).replaceAll("\"", ""));
                    for (int j = 0; j < options.size(); j++) {
                        JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(options.get(j)));
                        item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                Integer.parseInt(String.valueOf(opt.get(1)))));
                    }
                    long cTime = 0;
                    if (dataItem.size() > 3 && dataItem.get(3) != null) {
                        try {
                            cTime = Long.parseLong(String.valueOf(dataItem.get(3)));
                        } catch (Exception ignored) {}
                    }
                    item.createTime = cTime;
                    if (item.template.id == 2132) {
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

                        try {
                            Date currentDate = new Date(item.createTime);
                            Date startDate = formatter.parse("15/03/2024");
                            Date endDate = formatter.parse("28/03/2024");
                            if (currentDate.compareTo(startDate) >= 0 && currentDate.compareTo(endDate) <= 0) {
                                System.out.println("Thu hồi cải trang rồng lộn bug.");
                                item = ItemService.gI().createItemNull();
                            }
                        } catch (ParseException e) {
                        }
                    }
                    if (ItemService.gI().isOutOfDateTime(item)) {
                        item = ItemService.gI().createItemNull();
                    }
                } else {
                    item = ItemService.gI().createItemNull();
                }
                player.inventory.itemsBox.add(item);
            }
            dataArray.clear();

            //data box lucky round
            dataArray = (JSONArray) JSONValue.parse(rs.getString("items_box_lucky_round"));
            for (int i = 0; i < dataArray.size(); i++) {
                Item item;
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(i).toString());
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1 && ItemService.gI().getTemplate(tempId) != null) {
                    item = ItemService.gI().createItemFromData(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                    JSONArray options = (JSONArray) JSONValue.parse(String.valueOf(dataItem.get(2)).replaceAll("\"", ""));
                    for (int j = 0; j < options.size(); j++) {
                        JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(options.get(j)));
                        item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                Integer.parseInt(String.valueOf(opt.get(1)))));
                    }
                    long cTime = 0;
                    if (dataItem.size() > 3 && dataItem.get(3) != null) {
                        try {
                            cTime = Long.parseLong(String.valueOf(dataItem.get(3)));
                        } catch (Exception ignored) {}
                    }
                    item.createTime = cTime;
                    if (dataItem.size() == 5 && dataItem.get(4) != null) {
                        item.message = dataItem.get(4).toString();
                    }
                    if (ItemService.gI().isOutOfDateTime(item)) {
                        continue;
                    }
                    player.inventory.itemsBoxCrackBall.add(item);
                }
            }
            dataArray.clear();

            //data item da ban
            dataArray = (JSONArray) JSONValue.parse(rs.getString("items_daban"));
            for (int i = 0; i < dataArray.size() && i < 20; i++) {
                Item item;
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(i).toString());
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1 && ItemService.gI().getTemplate(tempId) != null) {
                    item = ItemService.gI().createItemFromData(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                    JSONArray options = (JSONArray) JSONValue.parse(String.valueOf(dataItem.get(2)).replaceAll("\"", ""));
                    for (int j = 0; j < options.size(); j++) {
                        JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(options.get(j)));
                        item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                Integer.parseInt(String.valueOf(opt.get(1)))));
                    }
                    // 26/06/2023 - Giảm Ngày Trong Shop
                    long cTime = 0;
                    if (dataItem.size() > 3 && dataItem.get(3) != null) {
                        try {
                            cTime = Long.parseLong(String.valueOf(dataItem.get(3)));
                        } catch (Exception ignored) {}
                    }
                    item.createTime = cTime;
                    if (item.template.id == 2132) {
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

                        try {
                            Date currentDate = new Date(item.createTime);
                            Date startDate = formatter.parse("15/03/2024");
                            Date endDate = formatter.parse("28/03/2024");
                            if (currentDate.compareTo(startDate) >= 0 && currentDate.compareTo(endDate) <= 0) {
                                System.out.println("Thu hồi cải trang rồng lộn bug.");
                                item = ItemService.gI().createItemNull();
                            }
                        } catch (ParseException e) {
                        }
                    }
                    if (!ItemService.gI().isOutOfDateTime(item)) {
                        player.inventory.itemsDaBan.add(item);
                    }
                    //player.inventory.itemsDaBan.add(item);
                }
            }
            dataArray.clear();

            //data friends
            dataArray = (JSONArray) JSONValue.parse(rs.getString("friends"));
            if (dataArray != null) {
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONArray dataFE = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    Friend friend = new Friend();
                    friend.id = Integer.parseInt(String.valueOf(dataFE.get(0)));
                    friend.name = String.valueOf(dataFE.get(1));
                    friend.head = Short.parseShort(String.valueOf(dataFE.get(2)));
                    friend.body = Short.parseShort(String.valueOf(dataFE.get(3)));
                    friend.leg = Short.parseShort(String.valueOf(dataFE.get(4)));
                    friend.bag = Byte.parseByte(String.valueOf(dataFE.get(5)));
                    friend.power = Long.parseLong(String.valueOf(dataFE.get(6)));
                    player.friends.add(friend);
                    dataFE.clear();
                }
                dataArray.clear();
            }

            //data enemies
            dataArray = (JSONArray) JSONValue.parse(rs.getString("enemies"));
            if (dataArray != null) {
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONArray dataFE = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    Enemy enemy = new Enemy();
                    enemy.id = Integer.parseInt(String.valueOf(dataFE.get(0)));
                    enemy.name = String.valueOf(dataFE.get(1));
                    enemy.head = Short.parseShort(String.valueOf(dataFE.get(2)));
                    enemy.body = Short.parseShort(String.valueOf(dataFE.get(3)));
                    enemy.leg = Short.parseShort(String.valueOf(dataFE.get(4)));
                    enemy.bag = Byte.parseByte(String.valueOf(dataFE.get(5)));
                    enemy.power = Long.parseLong(String.valueOf(dataFE.get(6)));
                    player.enemies.add(enemy);
                    dataFE.clear();
                }
                dataArray.clear();
            }

            //data nội tại
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_intrinsic"));
                if (dataArray != null && dataArray.size() >= 4) {
                    byte intrinsicId = Byte.parseByte(dataArray.get(0).toString());
                    player.playerIntrinsic.intrinsic = IntrinsicService.gI().getIntrinsicById(intrinsicId);
                    if (player.playerIntrinsic.intrinsic == null) {
                        player.playerIntrinsic.intrinsic = IntrinsicService.gI().getIntrinsicById(0);
                    }
                    if (player.playerIntrinsic.intrinsic != null) {
                        player.playerIntrinsic.intrinsic.param1 = Short.parseShort(dataArray.get(1).toString());
                        player.playerIntrinsic.countOpen = Byte.parseByte(dataArray.get(2).toString());
                        player.playerIntrinsic.intrinsic.param2 = Short.parseShort(dataArray.get(3).toString());
                    }
                    dataArray.clear();
                }
            } catch (Exception e) {
            }

            // data item time Dev Minh Dũng
            dataArray = (JSONArray) JSONValue.parse(rs.getString("devndung_time"));
            int timeco4la = Integer.parseInt(String.valueOf(dataArray.get(0)));
            int timechung = Integer.parseInt(String.valueOf(dataArray.get(1)));
            int timetet = Integer.parseInt(String.valueOf(dataArray.get(2)));
            int nguqua = Integer.parseInt(String.valueOf(dataArray.get(3)));
            int miakl = Integer.parseInt(String.valueOf(dataArray.get(4)));
            int miathom = Integer.parseInt(String.valueOf(dataArray.get(5)));
            int miasau = Integer.parseInt(String.valueOf(dataArray.get(6)));
            player.itemTime.lastTimeUseCo4La = System.currentTimeMillis() - (ItemTime.TIME_CO - timeco4la);
            player.itemTime.banhchunglastTime = System.currentTimeMillis() - (ItemTime.DEVNDUNG - timechung);
            player.itemTime.banhtetlastTime = System.currentTimeMillis() - (ItemTime.DEVNDUNG - timetet);
            player.itemTime.nguqualastTime = System.currentTimeMillis() - (ItemTime.DEVNDUNG - nguqua);
            player.itemTime.timemiakl = System.currentTimeMillis() - (ItemTime.TIME_ITEM - miakl);
            player.itemTime.timemiathom = System.currentTimeMillis() - (ItemTime.TIME_ITEM - miathom);
            player.itemTime.timemiasaurieng = System.currentTimeMillis() - (ItemTime.TIME_ITEM - miasau);
            player.itemTime.isUseCo4La = timeco4la != 0;
            player.itemTime.banhchung = timechung != 0;
            player.itemTime.banhtet = timetet != 0;
            player.itemTime.nguqua = nguqua != 0;
            player.itemTime.miakl = miakl != 0;
            player.itemTime.miathom = miathom != 0;
            player.itemTime.miasaurieng = miasau != 0;
            if (dataArray.size() > 7) {
                int timeKeoMotMat = Integer.parseInt(String.valueOf(dataArray.get(7)));
                player.itemTime.lastTimeKeoMotMat = System.currentTimeMillis() - (models.item.ItemTime.TIME_HALLOWEEN_BUFF - timeKeoMotMat);
                player.itemTime.isKeoMotMat = timeKeoMotMat > 0;
            }
            if (dataArray.size() > 8) {
                int timeSupBi = Integer.parseInt(String.valueOf(dataArray.get(8)));
                player.itemTime.lastTimeSupBiHacAm = System.currentTimeMillis() - (models.item.ItemTime.TIME_HALLOWEEN_BUFF - timeSupBi);
                player.itemTime.isSupBiHacAm = timeSupBi > 0;
            }
            if (dataArray.size() > 9) {
                int timeBanhGato = Integer.parseInt(String.valueOf(dataArray.get(9)));
                player.itemTime.lastTimeBanhGatoNhen = System.currentTimeMillis() - (models.item.ItemTime.TIME_HALLOWEEN_BUFF - timeBanhGato);
                player.itemTime.isBanhGatoNhen = timeBanhGato > 0;
            }
            if (dataArray.size() > 10) {
                int timeHamburger = Integer.parseInt(String.valueOf(dataArray.get(10)));
                player.itemTime.lastTimeHamburgerSau = System.currentTimeMillis() - (models.item.ItemTime.TIME_HALLOWEEN_BUFF - timeHamburger);
                player.itemTime.isHamburgerSau = timeHamburger > 0;
            }

            dataArray.clear();

            //data item time
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_item_time"));
            int timeUseTDLT = 0;
            int timeOpenPower = 0;
            int timeMayDo = 0;
            int timeMayDo2 = 0;
            int timeMeal = 0;
            int iconMeal = 0;
            int timeUseCMS = 0;
            int timeUseGTPT = 0;
            int timeUseDK = 0;
            int timeUseRX = 0;
            int timeMeal2 = 0;
            int iconMeal2 = 0;
            int timeUseNCD = 0;
            int timeKhauTrang = 0;
            int timeTnDeTu = 0;
            int timehoadao = 0;
            int timehoamai = 0;
            int timeBuaTnSm = 0;
            int timeBoHuyet = Integer.parseInt(String.valueOf(dataArray.get(0)));
            int timeBoHuyet2 = Integer.parseInt(String.valueOf(dataArray.get(1)));
            int timeBoKhi = Integer.parseInt(String.valueOf(dataArray.get(2)));
            int timeBoKhi2 = Integer.parseInt(String.valueOf(dataArray.get(3)));
            int timeGiapXen = Integer.parseInt(String.valueOf(dataArray.get(4)));
            int timeGiapXen2 = Integer.parseInt(String.valueOf(dataArray.get(5)));
            int timeCuongNo = Integer.parseInt(String.valueOf(dataArray.get(6)));
            int timeCuongNo2 = Integer.parseInt(String.valueOf(dataArray.get(7)));
            int timeAnDanh = Integer.parseInt(String.valueOf(dataArray.get(8)));
            int timeAnDanh2 = Integer.parseInt(String.valueOf(dataArray.get(9)));
            if (dataArray.size() > 10) {
                timeOpenPower = Integer.parseInt(String.valueOf(dataArray.get(10)));
            }
            if (dataArray.size() > 11) {
                timeMayDo = Integer.parseInt(String.valueOf(dataArray.get(11)));
            }
            if (dataArray.size() > 12) {
                timeMayDo2 = Integer.parseInt(String.valueOf(dataArray.get(12)));
            }
            if (dataArray.size() > 13) {
            }
            if (dataArray.size() > 14) {
                timeMeal = Integer.parseInt(String.valueOf(dataArray.get(14)));
            }
            if (dataArray.size() > 15) {
                iconMeal = Integer.parseInt(String.valueOf(dataArray.get(15)));
            }
            if (dataArray.size() > 16) {
                timeUseTDLT = Integer.parseInt(String.valueOf(dataArray.get(16)));
            }
            if (dataArray.size() > 17) {
                timeUseCMS = Integer.parseInt(String.valueOf(dataArray.get(17)));
            }
            if (dataArray.size() > 18) {
                timeUseGTPT = Integer.parseInt(String.valueOf(dataArray.get(18)));
            }
            if (dataArray.size() > 19) {
                timeUseDK = Integer.parseInt(String.valueOf(dataArray.get(19)));
            }
            if (dataArray.size() > 20) {
                timeUseRX = Integer.parseInt(String.valueOf(dataArray.get(20)));
            }
            if (dataArray.size() > 21) {
                timeMeal2 = Integer.parseInt(String.valueOf(dataArray.get(21)));
            }
            if (dataArray.size() > 22) {
                iconMeal2 = Integer.parseInt(String.valueOf(dataArray.get(22)));
            }
            if (dataArray.size() > 23) {
            }
            if (dataArray.size() > 24) {
                timeUseNCD = Integer.parseInt(String.valueOf(dataArray.get(24)));
            }
            if (dataArray.size() > 25) {
            }
            if (dataArray.size() > 26) {
            }
            if (dataArray.size() > 27) {
                timeKhauTrang = Integer.parseInt(dataArray.get(27).toString());
            }
            if (dataArray.size() > 28) {
                timeTnDeTu = Integer.parseInt(dataArray.get(28).toString());
            }
            if (dataArray.size() > 29) {
                timehoadao = Integer.parseInt(String.valueOf(dataArray.get(29)));
            }
            if (dataArray.size() > 30) {
                timehoamai = Integer.parseInt(String.valueOf(dataArray.get(30)));
            }
            if (dataArray.size() > 31) {
                timeBuaTnSm = Integer.parseInt(String.valueOf(dataArray.get(31)));
            }
            int timeTnsmX = 0;
            int heSoTnsmX = 0;
            if (dataArray.size() > 33) {
                timeTnsmX = Integer.parseInt(String.valueOf(dataArray.get(32)));
                heSoTnsmX = Integer.parseInt(String.valueOf(dataArray.get(33)));
            }
            player.itemTime.lastTimeBoHuyet = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet);
            player.itemTime.lastTimeBoKhi = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi);
            player.itemTime.lastTimeGiapXen = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen);
            player.itemTime.lastTimeCuongNo = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo);
            player.itemTime.lastTimeAnDanh = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeAnDanh);
            player.itemTime.lastTimeBoHuyet2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet2);
            player.itemTime.lastTimeBoKhi2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi2);
            player.itemTime.lastTimeGiapXen2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen2);
            player.itemTime.lastTimeCuongNo2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo2);
            player.itemTime.lastTimeAnDanh2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeAnDanh2);
            player.itemTime.lastTimeOpenPower = System.currentTimeMillis() - (ItemTime.TIME_OPEN_POWER - timeOpenPower);
            player.itemTime.lastTimeUseMayDo = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - timeMayDo);
            player.itemTime.lastTimeUseMayDo2 = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO2 - timeMayDo2);
            player.itemTime.lastTimeEatMeal = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeMeal);
            player.itemTime.timeTDLT = timeUseTDLT * 60 * 1000;
            player.itemTime.lastTimeUseTDLT = System.currentTimeMillis();
            player.itemTime.lastTimeUseCMS = System.currentTimeMillis() - (ItemTime.TIME_CMS - timeUseCMS);
            player.itemTime.lastTimeUseGTPT = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeUseGTPT);
            player.itemTime.lastTimeUseDK = System.currentTimeMillis() - (ItemTime.TIME_DK - timeUseDK);
            player.itemTime.timeRX = timeUseRX * 60 * 1000;
            player.itemTime.lastTimeUseRX = System.currentTimeMillis();
            player.itemTime.lastTimeEatMeal2 = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeMeal2);
            player.itemTime.lastTimeUseNCD = System.currentTimeMillis() - (ItemTime.TIME_NCD - timeUseNCD);
            player.itemTime.lastTimeBoHuyet = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet);
            player.itemTime.lastTimeKhauTrang = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - timeKhauTrang);
            player.itemTime.lastTimeTnDeTu = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - timeTnDeTu);
            player.itemTime.lastTimeXimuoihoadao = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timehoadao);
            player.itemTime.lastTimeXimuoihoamai = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timehoamai);
            player.itemTime.lastTimeBuaTNSM = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBuaTnSm);
            player.itemTime.lastTimeTnsmX = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeTnsmX);
            player.itemTime.tnsmX = timeTnsmX != 0 ? heSoTnsmX : 0;

            player.itemTime.iconMeal = iconMeal;
            player.itemTime.isEatMeal = timeMeal != 0;
            player.itemTime.isUseBoHuyet = timeBoHuyet != 0;
            player.itemTime.isUseBoKhi = timeBoKhi != 0;
            player.itemTime.isUseGiapXen = timeGiapXen != 0;
            player.itemTime.isUseCuongNo = timeCuongNo != 0;
            player.itemTime.isUseAnDanh = timeAnDanh != 0;
            player.itemTime.isUseBoHuyet2 = timeBoHuyet2 != 0;
            player.itemTime.isUseBoKhi2 = timeBoKhi2 != 0;
            player.itemTime.isUseGiapXen2 = timeGiapXen2 != 0;
            player.itemTime.isUseCuongNo2 = timeCuongNo2 != 0;
            player.itemTime.isUseAnDanh2 = timeAnDanh2 != 0;
            player.itemTime.isOpenPower = timeOpenPower != 0;
            player.itemTime.isUseMayDo = timeMayDo != 0;
            player.itemTime.isUseMayDo2 = timeMayDo2 != 0;
            player.itemTime.isUseTDLT = timeUseTDLT != 0;
            player.itemTime.isUseCMS = timeUseCMS != 0;
            player.itemTime.isUseGTPT = timeUseGTPT != 0;
            player.itemTime.isUseDK = timeUseDK != 0;
            player.itemTime.isUseRX = timeUseRX != 0;
            player.itemTime.iconMeal2 = iconMeal2;
            player.itemTime.isEatMeal2 = timeMeal2 != 0;
            player.itemTime.isUseNCD = timeUseNCD != 0;
            player.itemTime.isKhauTrang = timeKhauTrang != 0;
            player.itemTime.isTnDeTu = timeTnDeTu != 0;
            player.itemTime.isXimuoihoadao = timehoadao != 0;
            player.itemTime.isXimuoihoamai = timehoamai != 0;
            player.itemTime.isBuaTNSM = timeBuaTnSm != 0;
            dataArray.clear();

            // data nhiệm vụ
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_task"));

            if (dataArray != null && dataArray.size() >= 3) {
                TaskMain taskMain = TaskService.gI().getTaskMainById(player, Byte.parseByte(String.valueOf(dataArray.get(0))));
                taskMain.index = Byte.parseByte(String.valueOf(dataArray.get(1)));

                // Kiểm tra index có hợp lệ không trước khi truy cập subTasks
                if (taskMain.index >= 0 && taskMain.index < taskMain.subTasks.size()) {
                    taskMain.subTasks.get(taskMain.index).count = Short.parseShort(String.valueOf(dataArray.get(2)));
                }

                if (dataArray.size() > 3) {
                    taskMain.lastTime = Long.parseLong(String.valueOf(dataArray.get(3)));
                } else {
                    taskMain.lastTime = System.currentTimeMillis();
                }
                player.playerTask.taskMain = taskMain;
            }

            if (dataArray != null) {
                dataArray.clear();
            }

            //data nhiệm vụ hàng ngày
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_side_task"));
            String format = "dd-MM-yyyy";
            long receivedTime = Long.parseLong(String.valueOf(dataArray.get(1)));
            Date date = new Date(receivedTime);
            if (TimeUtil.formatTime(date, format).equals(TimeUtil.formatTime(new Date(), format))) {
                player.playerTask.sideTask.template = TaskService.gI().getSideTaskTemplateById(Integer.parseInt(String.valueOf(dataArray.get(0))));
                player.playerTask.sideTask.count = Integer.parseInt(String.valueOf(dataArray.get(2)));
                player.playerTask.sideTask.maxCount = Integer.parseInt(String.valueOf(dataArray.get(3)));
                player.playerTask.sideTask.leftTask = Integer.parseInt(String.valueOf(dataArray.get(4)));
                player.playerTask.sideTask.level = Integer.parseInt(String.valueOf(dataArray.get(5)));
                player.playerTask.sideTask.receivedTime = receivedTime;
            }

            //data trứng bư
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_mabu_egg"));
            if (!dataArray.isEmpty()) {
                player.mabuEgg = new MabuEgg(player, Long.parseLong(String.valueOf(dataArray.get(0))),
                        Long.parseLong(String.valueOf(dataArray.get(1))));
            }
            dataArray.clear();

            //data trứng bill
            dataArray = (JSONArray) JSONValue.parse(rs.getString("bill_data"));
            if (dataArray.size() != 0) {
                player.billEgg = new BillEgg(player, Long.parseLong(String.valueOf(dataArray.get(0))),
                        Long.parseLong(String.valueOf(dataArray.get(1))));
            }
            dataArray.clear();
            //data trứng black
            try {
                String blackDataStr = rs.getString("black_data");
                if (blackDataStr != null && !blackDataStr.isEmpty()) {
                    dataArray = (JSONArray) JSONValue.parse(blackDataStr);
                    if (dataArray != null) {
                        if (dataArray.size() != 0) {
                            player.blackEgg = new BlackEgg(player, Long.parseLong(String.valueOf(dataArray.get(0))),
                                    Long.parseLong(String.valueOf(dataArray.get(1))));
                        }
                        dataArray.clear();
                    }
                    
                }
            } catch (Exception e) {
                Logger.error("Load black_data failed: " + e + "\n");
            }
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_danh_hieu"));
            String Reset = "dd-MM-yyyy";
            long Time = Long.parseLong(String.valueOf(dataArray.get(1)));
            Date datatime = new Date(Time);
            if (TimeUtil.formatTime(datatime, Reset).equals(TimeUtil.formatTime(new Date(), Reset))) {
                player.playerTask.taskdh.Nap = Integer.parseInt(String.valueOf(dataArray.get(0)));
                player.playerTask.taskdh.Shenron = Integer.parseInt(String.valueOf(dataArray.get(2)));
                player.playerTask.taskdh.Hagucboss = Integer.parseInt(String.valueOf(dataArray.get(3)));
                player.playerTask.taskdh.DapDo = Integer.parseInt(String.valueOf(dataArray.get(4)));
                player.playerTask.taskdh.SieuHang = Integer.parseInt(String.valueOf(dataArray.get(5)));
                player.playerTask.taskdh.TaskBoMong = Integer.parseInt(String.valueOf(dataArray.get(6)));
                player.playerTask.taskdh.ChoSuong = Integer.parseInt(String.valueOf(dataArray.get(7)));
                player.playerTask.taskdh.ChoNuoc = Integer.parseInt(String.valueOf(dataArray.get(8)));
                player.playerTask.taskdh.NhatDo = Integer.parseInt(String.valueOf(dataArray.get(9)));
                player.playerTask.taskdh.AnTrom = Integer.parseInt(String.valueOf(dataArray.get(10)));
                player.playerTask.taskdh.ODo = Integer.parseInt(String.valueOf(dataArray.get(11)));
                player.playerTask.taskdh.DungLoa = Integer.parseInt(String.valueOf(dataArray.get(12)));
                player.playerTask.taskdh.ResetTime = Time;
            }
            dataArray.clear();

            //data bùa
            dataArray = (JSONArray) JSONValue.parse(rs.getString("data_charm"));
            player.charms.tdTriTue = Long.parseLong(String.valueOf(dataArray.get(0)));
            player.charms.tdManhMe = Long.parseLong(String.valueOf(dataArray.get(1)));
            player.charms.tdDaTrau = Long.parseLong(String.valueOf(dataArray.get(2)));
            player.charms.tdOaiHung = Long.parseLong(String.valueOf(dataArray.get(3)));
            player.charms.tdBatTu = Long.parseLong(String.valueOf(dataArray.get(4)));
            player.charms.tdDeoDai = Long.parseLong(String.valueOf(dataArray.get(5)));
            player.charms.tdThuHut = Long.parseLong(String.valueOf(dataArray.get(6)));
            player.charms.tdDeTu = Long.parseLong(String.valueOf(dataArray.get(7)));
            player.charms.tdTriTue3 = Long.parseLong(String.valueOf(dataArray.get(8)));
            player.charms.tdTriTue4 = Long.parseLong(String.valueOf(dataArray.get(9)));
            dataArray.clear();

            //data skill
            dataArray = (JSONArray) JSONValue.parse(rs.getString("skills"));
            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray dataSkill = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(i)));
                int tempId = Integer.parseInt(String.valueOf(dataSkill.get(0)));
                byte point = Byte.parseByte(String.valueOf(dataSkill.get(1)));
                Skill skill;
                if (point != 0) {
                    skill = SkillUtil.createSkill(tempId, point, player.gender);
                } else {
                    skill = SkillUtil.createSkillLevel0(tempId);
                }
                if (skill == null) {
                    skill = SkillUtil.createSkillLevel0(tempId);
                }
                skill.lastTimeUseThisSkill = Long.parseLong(String.valueOf(dataSkill.get(2)));
                if (dataSkill.size() > 3) {
                    skill.currLevel = Short.parseShort(String.valueOf(dataSkill.get(3)));
                }
                player.playerSkill.skills.add(skill);
            }
            dataArray.clear();

            //data skill shortcut
            dataArray = (JSONArray) JSONValue.parse(rs.getString("skills_shortcut"));
            for (int i = 0; i < dataArray.size() && i < player.playerSkill.skillShortCut.length; i++) {
                player.playerSkill.skillShortCut[i] = Byte.parseByte(String.valueOf(dataArray.get(i)));
            }

            for (int i : player.playerSkill.skillShortCut) {
                if (player.playerSkill.getSkillbyId(i) != null && player.playerSkill.getSkillbyId(i).damage > 0) {
                    player.playerSkill.skillSelect = player.playerSkill.getSkillbyId(i);
                    break;
                }
            }
            if (player.playerSkill.skillSelect == null) {
                player.playerSkill.skillSelect = player.playerSkill.getSkillbyId(player.gender == ConstPlayer.TRAI_DAT
                        ? Skill.DRAGON : (player.gender == ConstPlayer.NAMEC ? Skill.DEMON : Skill.GALICK));
            }
            dataArray.clear();

            //notify
            player.notify = rs.getString("notify");

            //data pet
            JSONArray petData = (JSONArray) JSONValue.parse(rs.getString("pet"));
            if (petData != null && !petData.isEmpty()) {
                dataArray = (JSONArray) JSONValue.parse(String.valueOf(petData.get(0)));
                Pet pet = new Pet(player);
                pet.id = -player.id;
                pet.typePet = Byte.parseByte(String.valueOf(dataArray.get(0)));
                pet.gender = Byte.parseByte(String.valueOf(dataArray.get(1)));
                pet.name = String.valueOf(dataArray.get(2));
                player.fusion.typeFusion = Byte.parseByte(String.valueOf(dataArray.get(3)));
                player.fusion.lastTimeFusion = System.currentTimeMillis()
                        - (Fusion.TIME_FUSION - Integer.parseInt(String.valueOf(dataArray.get(4))));
                pet.status = Byte.parseByte(String.valueOf(dataArray.get(5)));

                //data chỉ số
                dataArray = (JSONArray) JSONValue.parse(String.valueOf(petData.get(1)));
                pet.nPoint.limitPower = Byte.parseByte(String.valueOf(dataArray.get(0)));
                pet.nPoint.power = Long.parseLong(String.valueOf(dataArray.get(1)));
                pet.nPoint.tiemNang = Long.parseLong(String.valueOf(dataArray.get(2)));
                pet.nPoint.stamina = Short.parseShort(String.valueOf(dataArray.get(3)));
                pet.nPoint.maxStamina = Short.parseShort(String.valueOf(dataArray.get(4)));
                pet.nPoint.hpg = Long.parseLong(String.valueOf(dataArray.get(5)));
                pet.nPoint.mpg = Long.parseLong(String.valueOf(dataArray.get(6)));
                pet.nPoint.dameg = Long.parseLong(String.valueOf(dataArray.get(7)));
                pet.nPoint.defg = Integer.parseInt(String.valueOf(dataArray.get(8)));
                pet.nPoint.critg = Integer.parseInt(String.valueOf(dataArray.get(9)));
                long hp = Long.parseLong(String.valueOf(dataArray.get(10)));
                long mp = Long.parseLong(String.valueOf(dataArray.get(11)));

                //data body
                dataArray = (JSONArray) JSONValue.parse(String.valueOf(petData.get(2)));
                for (int i = 0; i < dataArray.size(); i++) {
                    Item item;
                    JSONArray dataItem = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId != -1 && ItemService.gI().getTemplate(tempId) != null) {
                        item = ItemService.gI().createItemFromData(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                        JSONArray options = (JSONArray) JSONValue.parse(String.valueOf(dataItem.get(2)).replaceAll("\"", ""));
                        for (int j = 0; j < options.size(); j++) {
                            JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(options.get(j)));
                            item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                    Integer.parseInt(String.valueOf(opt.get(1)))));
                        }
                        long cTime = 0;
                        if (dataItem.size() > 3 && dataItem.get(3) != null) {
                            try {
                                cTime = Long.parseLong(String.valueOf(dataItem.get(3)));
                            } catch (Exception ignored) {}
                        }
                        item.createTime = cTime;
                        if (item.template.id == 2132) {
                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

                            try {
                                Date currentDate = new Date(item.createTime);
                                Date startDate = formatter.parse("15/03/2024");
                                Date endDate = formatter.parse("28/03/2024");
                                if (currentDate.compareTo(startDate) >= 0 && currentDate.compareTo(endDate) <= 0) {
                                    System.out.println("Thu hồi cải trang rồng lộn bug.");
                                    item = ItemService.gI().createItemNull();
                                }
                            } catch (ParseException e) {
                            }
                        }
                        if (ItemService.gI().isOutOfDateTime(item)) {
                            item = ItemService.gI().createItemNull();
                        }
                    } else {
                        item = ItemService.gI().createItemNull();
                    }
                    pet.inventory.itemsBody.add(item);
                }
                int minSlots = pet.typePet == 0 ? 7 : 9;
                while (pet.inventory.itemsBody.size() < minSlots) {
                    pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
                }

                //data skills
                dataArray = (JSONArray) JSONValue.parse(String.valueOf(petData.get(3)));
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONArray skillTemp = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    int tempId = Integer.parseInt(String.valueOf(skillTemp.get(0)));
                    byte point = Byte.parseByte(String.valueOf(skillTemp.get(1)));
                    Skill skill;
                    if (point != 0) {
                        skill = SkillUtil.createSkill(tempId, point);
                    } else {
                        skill = SkillUtil.createSkillLevel0(tempId);
                    }
                    if (skill == null) {
                        skill = SkillUtil.createSkillLevel0(tempId);
                    }
                    if (skillTemp.size() > 3) {
                        skill.lastTimeUseThisSkill = Long.parseLong(String.valueOf(skillTemp.get(2)));
                    }
                    if (skillTemp.size() > 3) {
                        skill.currLevel = Short.parseShort(String.valueOf(skillTemp.get(3)));
                    }
                    switch (skill.template.id) {
                        case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC ->
                            skill.coolDown = 1000;
                    }
                    pet.playerSkill.skills.add(skill);
                }
                if (pet.playerSkill.skills.size() < 5) {
                    pet.playerSkill.skills.add(4, SkillUtil.createSkillLevel0(-1));
                }
                if (pet.playerSkill.skills.size() < 6) {
                    pet.playerSkill.skills.add(5, SkillUtil.createSkillLevel0(-1));
                }
                if (pet.playerSkill.skills.size() < 7) {
                    pet.playerSkill.skills.add(6, SkillUtil.createSkillLevel0(-1));
                }
                pet.nPoint.hp = hp;
                pet.nPoint.mp = mp;
                player.pet = pet;
            }

            //Data bảo vệ tài khoản
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("baovetaikhoan"));
                player.mbv = Integer.parseInt(dataArray.get(0).toString());
                player.baovetaikhoan = Boolean.parseBoolean(dataArray.get(1).toString());
                player.mbvtime = Long.parseLong(dataArray.get(2).toString());
                if (dataArray.size() > 3) {
                    player.isOffPorataOutfit = Boolean.parseBoolean(dataArray.get(3).toString());
                }
            } catch (Exception e) {
                player.mbv = 0;
                player.baovetaikhoan = false;
                player.mbvtime = System.currentTimeMillis();
                player.isOffPorataOutfit = false;
            }

            // data rada card
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_card"));
                if (dataArray != null) {
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject obj = (JSONObject) dataArray.get(i);
                        if (obj == null) {
                            continue;
                        }
                        player.Cards.add(new Card(Short.parseShort(obj.get("id").toString()), Byte.parseByte(obj.get("amount").toString()), Byte.parseByte(obj.get("max").toString()), Byte.parseByte(obj.get("level").toString()), loadOptionCard((JSONArray) JSONValue.parse(obj.get("option").toString())), Byte.parseByte(obj.get("used").toString())));
                    }
                    dataArray.clear();
                }
            } catch (Exception e) {
            }

            //data PK Commeson
            player.lastPkCommesonTime = rs.getLong("lasttimepkcommeson");

            //Data BDKB
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("bandokhobau"));
                player.timesPerDayBDKB = Integer.parseInt(dataArray.get(0).toString());
                player.lastTimeJoinBDKB = Long.parseLong(dataArray.get(1).toString());
            } catch (Exception e) {
                player.timesPerDayBDKB = 0;
                player.lastTimeJoinBDKB = System.currentTimeMillis();
            }

            //Data doanh trại
            try {
                player.lastTimeJoinDT = rs.getLong("doanhtrai");
            } catch (Exception e) {
                player.lastTimeJoinDT = 0;
            }

            //Data CDRD
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("conduongrandoc"));
                player.joinCDRD = Boolean.parseBoolean(dataArray.get(0).toString());
                player.lastTimeJoinCDRD = Long.parseLong(dataArray.get(1).toString());
                player.talkToThuongDe = Boolean.parseBoolean(dataArray.get(2).toString());
                player.talkToThanMeo = Boolean.parseBoolean(dataArray.get(2).toString());
                if (player.clan.ConDuongRanDoc == null || player.lastTimeJoinCDRD != player.clan.lastTimeOpenConDuongRanDoc) {
                    player.joinCDRD = false;
                    player.talkToThuongDe = false;
                    player.talkToThanMeo = false;
                }
            } catch (Exception e) {
                player.joinCDRD = false;
                player.lastTimeJoinCDRD = 0;
                player.talkToThuongDe = false;
                player.talkToThanMeo = false;
            }

            // Sư phụ không tấn công
            try {
                player.doesNotAttack = rs.getBoolean("masterDoesAttack");
                player.lastTimePlayerNotAttack = System.currentTimeMillis();
            } catch (Exception e) {
                player.doesNotAttack = false;
                player.lastTimePlayerNotAttack = System.currentTimeMillis();
            }

            //data Nhận Thỏi Vàng
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("nhanthoivang"));
                player.danhanthoivang = Boolean.parseBoolean(dataArray.get(0).toString());
                player.lastRewardGoldBarTime = Long.parseLong(dataArray.get(1).toString());
            } catch (Exception e) {
                player.danhanthoivang = false;
                player.lastRewardGoldBarTime = 0;
            }

            //data Rương gỗ
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("ruonggo"));
                player.levelWoodChest = Integer.parseInt(dataArray.get(0).toString());
                player.goldChallenge = Long.parseLong(dataArray.get(1).toString());
                player.rubyChallenge = Long.parseLong(dataArray.get(2).toString());
                player.lastTimeRewardWoodChest = Long.parseLong(dataArray.get(3).toString());
                player.lastTimePKDHVT23 = Long.parseLong(dataArray.get(4).toString());
            } catch (Exception e) {
                player.levelWoodChest = 0;
                player.goldChallenge = 50000;
                player.rubyChallenge = 1;
                player.lastTimeRewardWoodChest = System.currentTimeMillis();
                player.lastTimePKDHVT23 = 0;
            }

            //data Siêu Thần Thủy
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("sieuthanthuy"));
                player.winSTT = Boolean.parseBoolean(dataArray.get(0).toString());
                player.lastTimeWinSTT = Long.parseLong(dataArray.get(1).toString());
                player.callBossPocolo = Boolean.parseBoolean(dataArray.get(2).toString());
            } catch (Exception e) {
            }

            //data Võ đài sinh tử
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("vodaisinhtu"));
                player.haveRewardVDST = Boolean.parseBoolean(dataArray.get(0).toString());
                player.thoiVangVoDaiSinhTu = Integer.parseInt(dataArray.get(1).toString());
                player.lastTimePKVoDaiSinhTu = Long.parseLong(dataArray.get(2).toString());
                player.timePKVDST = Long.parseLong(dataArray.get(3).toString());
            } catch (Exception e) {
            }

            //Thời gian gọi rồng
            player.lastTimeShenronAppeared = rs.getLong("rongxuong");

            //data item event
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_item_event"));
                player.itemEvent.remainingTVGSCount = Integer.parseInt(dataArray.get(0).toString());
                player.itemEvent.lastTVGSTime = Long.parseLong(dataArray.get(1).toString());
                player.itemEvent.remainingHHCount = Integer.parseInt(dataArray.get(2).toString());
                player.itemEvent.lastHHTime = Long.parseLong(dataArray.get(3).toString());
                player.itemEvent.remainingBNCount = Integer.parseInt(dataArray.get(4).toString());
                player.itemEvent.lastBNTime = Long.parseLong(dataArray.get(5).toString());
                if (dataArray.size() > 6 && dataArray.get(6) instanceof JSONArray) {
                    JSONArray arr = (JSONArray) dataArray.get(6);
                    for (Object obj : arr) {
                        player.npcsTrickOrTreat.add(Integer.parseInt(obj.toString()));
                    }
                }
                if (dataArray.size() > 7) {
                    player.lastTimeTrickOrTreat = Long.parseLong(dataArray.get(7).toString());
                }
                player.checkResetTrickOrTreat();
            } catch (Exception e) {
                player.itemEvent.remainingTVGSCount = 0;
                player.itemEvent.lastTVGSTime = 0;
                player.itemEvent.remainingHHCount = 0;
                player.itemEvent.lastHHTime = 0;
                player.itemEvent.remainingBNCount = 0;
                player.itemEvent.lastBNTime = 0;
                player.lastTimeTrickOrTreat = 0;
            }

            //data luyện tập
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_luyentap"));
                player.levelLuyenTap = Integer.parseInt(dataArray.get(0).toString());
                player.dangKyTapTuDong = Boolean.parseBoolean(dataArray.get(1).toString());
                player.mapIdDangTapTuDong = Integer.parseInt(dataArray.get(2).toString());
                player.tnsmLuyenTap = Integer.parseInt(dataArray.get(3).toString());
                player.lastTimeOffline = Long.parseLong(dataArray.get(4).toString());
                if (dataArray.size() > 5) {
                    player.traning.setTop(Integer.parseInt(dataArray.get(5).toString()));
                    player.traning.setTime(Integer.parseInt(dataArray.get(6).toString()));
                    player.traning.setLastTime(Long.parseLong(dataArray.get(7).toString()));
                    player.traning.setLastTop(Integer.parseInt(dataArray.get(8).toString()));
                    player.traning.setLastRewardTime(Long.parseLong(dataArray.get(9).toString()));
                }
            } catch (Exception e) {
                player.levelLuyenTap = 0;
                player.dangKyTapTuDong = false;
                player.mapIdDangTapTuDong = -1;
                player.tnsmLuyenTap = 0;
                player.lastTimeOffline = System.currentTimeMillis();
            }

            //data nhiệm vụ bang hàng ngày
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_clan_task"));
                format = "dd-MM-yyyy";
                receivedTime = Long.parseLong(String.valueOf(dataArray.get(1)));
                date = new Date(receivedTime);
                if (TimeUtil.formatTime(date, format).equals(TimeUtil.formatTime(new Date(), format))) {
                    player.playerTask.clanTask.template = TaskService.gI().getClanTaskTemplateById(Integer.parseInt(String.valueOf(dataArray.get(0))));
                    player.playerTask.clanTask.count = Integer.parseInt(String.valueOf(dataArray.get(2)));
                    player.playerTask.clanTask.maxCount = Integer.parseInt(String.valueOf(dataArray.get(3)));
                    player.playerTask.clanTask.leftTask = Integer.parseInt(String.valueOf(dataArray.get(4)));
                    player.playerTask.clanTask.level = Integer.parseInt(String.valueOf(dataArray.get(5)));
                    player.playerTask.clanTask.receivedTime = receivedTime;
                }
            } catch (Exception e) {
            }

            //data vip
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_vip"));
                player.timesPerDayCuuSat = Integer.parseInt(String.valueOf(dataArray.get(0)));
                player.lastTimeCuuSat = Long.parseLong(String.valueOf(dataArray.get(1)));
                player.nhanDeTuNangVIP = Boolean.parseBoolean(String.valueOf(dataArray.get(2)));
                player.nhanVangNangVIP = Boolean.parseBoolean(String.valueOf(dataArray.get(3)));
                if (dataArray.size() > 4) {
                    player.nhanSKHVIP = Boolean.parseBoolean(String.valueOf(dataArray.get(4)));
                }
            } catch (Exception e) {
            }

            //data super rank
            SuperRankDAO.loadData(player);
            if (Util.isAfterMidnight(player.superRank.lastPKTime)) {
                if (player.superRank.ticket < 3) {
                    player.superRank.ticket++;
                }
                player.superRank.lastPKTime = System.currentTimeMillis();
            }

            //data achievement
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data_achievement"));
                for (int i = 0; i < Manager.ACHIEVEMENT_TEMPLATE.size(); i++) {
                    AchievementQuest aq;
                    if (i < dataArray.size()) {
                        JSONArray data = (JSONArray) JSONValue.parse(dataArray.get(i).toString());
                        aq = new AchievementQuest(Long.parseLong(data.get(0).toString()), Boolean.parseBoolean(data.get(1).toString()));
                    } else {
                        aq = new AchievementQuest(0, false);
                    }
                    player.achievement.add(aq);
                }
                dataArray.clear();
            } catch (Exception e) {
            }

            //Giftcode
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("giftcode"));
                for (Object code : dataArray) {
                    player.giftCode.add((String) code);
                }
                dataArray.clear();
            } catch (Exception e) {
            }
            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("boughtSkills"));
                for (Object code : dataArray) {
                    player.BoughtSkills.add((int) code);
                }
                dataArray.clear();
            } catch (Exception e) {
            }

            try {
                dataArray = (JSONArray) JSONValue.parse(rs.getString("hoc_ky_nang"));
                player.hocKyNang.ItemTemplateSkillId = Short.parseShort(String.valueOf(dataArray.get(0)));
                player.hocKyNang.Level = Integer.parseInt(String.valueOf(dataArray.get(1)));
                player.hocKyNang.PotentialLearn = Long.parseLong(String.valueOf(dataArray.get(2)));
                player.hocKyNang.Time = Long.parseLong(String.valueOf(dataArray.get(3)));

                dataArray.clear();
            } catch (Exception e) {
            }

            player.nPoint.hp = plHp;
            player.nPoint.mp = plMp;
            player.idMark.setLoadedAllDataPlayer(true);

            cleanDuplicateItemOptions(player);
        } catch (Exception e) {
            if (player != null) {
                player.dispose();
                player = null;
            }
            throw e;
        }
        return player;
    }

    private static void cleanDuplicateItemOptions(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        normalizeTicketList(player.inventory.itemsBody);
        normalizeTicketList(player.inventory.itemsBag);
        normalizeTicketList(player.inventory.itemsBox);
        normalizeTicketList(player.inventory.itemsBoxCrackBall);
        normalizeTicketList(player.inventory.itemsDaBan);
    }

    private static void normalizeTicketList(List<Item> items) {
        if (items == null) {
            return;
        }
        for (Item it : items) {
            if (it != null && it.template != null && it.template.id == 1430) {
                ItemService.gI().normalizeTicketOptions(it);
            }
        }
    }

    public static List<OptionCard> loadOptionCard(JSONArray json) {
        List<OptionCard> ops = new ArrayList<>();
        try {
            for (Object o : json) {
                JSONObject ob = (JSONObject) o;
                if (ob != null) {
                    ops.add(new OptionCard(Integer.parseInt(ob.get("id").toString()), Integer.parseInt(ob.get("param").toString()), Byte.parseByte(ob.get("active").toString())));
                }
            }
        } catch (NumberFormatException e) {
        }
        return ops;
    }
}
