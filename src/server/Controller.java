package server;

 
import models.boss.Boss;
import managers.boss.BossManager;
import consts.ConstAchievement;
import services.player.ClanService;
import services.ChatGlobalService;
import services.SubMenuService;
import services.Service;
import services.player.IntrinsicService;
import services.FlagBagService;
import services.ItemTimeService;
import services.SkillService;
import services.map.NpcService;
import services.TaskService;
import services.map.ItemMapService;
import services.player.PlayerService;
import services.player.FriendAndEnemyService;
import database.AlyraManager;
import database.AlyraResultSet;
import consts.ConstIgnoreName;
import consts.ConstMap;
import utils.Util;
import data.DataGame;
import network.session.MySession;

import java.io.IOException;

import services.map.ChangeMapService;
import services.func.UseItem;
import services.func.Input;
import consts.ConstNpc;
import consts.ConstTask;
import data.ItemData;
import database.daos.PlayerDAO;
import database.daos.SuperRankDAO;
import models.radar.Card;
import services.RadarService;
import services.map.NpcManager;
import models.player.Player;
import models.matches.PVPService;
import services.AchievementService;
import services.ShopService;
import interfaces.IMessageHandler;
import network.io.Message;
import interfaces.ISession;
import services.dungeon.BlackBallWarService;
import services.tournament.SuperRankService;
import services.dungeon.TrainingService;
import services.map.MapService;
import services.CombineService;
import services.func.LuckyRound;
import services.func.TransactionService;
import models.skill.Skill;
import services.ConsignShopService;
import services.PetService;
import utils.Logger;

public class Controller implements IMessageHandler {

    private int errors;

    private static Controller instance;

    public static Controller gI() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    @Override
    public void onMessage(ISession s, Message _msg) {
        long st = System.currentTimeMillis();
        MySession _session = (MySession) s;
        _session.lastTimeReadMessage = st;
        Player player = null;
        try {
            player = _session.player;
            if (player != null && (player.disposed || player.beforeDispose)) {       
                return;
            }
            byte cmd = _msg.command;
            switch (cmd) {
                case 99:
                    if (player != null) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Vui lòng kết thúc giao dịch trước khi nhận quà");
                            return;
                        }
                        services.func.GiftSystemService.gI().handle(player, _msg);
                    }
                    break;
                case -100:
                    if (player == null) {
                        return;
                    }
                    if (TaskService.gI().getIdTask(player) < 22) {
                        Service.gI().sendThongBao(player, "Yêu cầu hoàn thành xong nhiệm vụ Fide");
                        return;
                    }
                    if (player.nPoint.power < 20_000_000_000L) {
                        Service.gI().sendThongBao(player, "Yêu cầu đạt 20 tỉ sức mạnh");
                        return;
                    }
                    if (TransactionService.gI().check(player)) {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                        return;
                    }
                    if (player.baovetaikhoan) {
                        Service.gI().sendThongBao(player, "Chức năng bảo vệ đã được bật. Bạn vui lòng kiểm tra lại");
                        return;
                    }
                    byte action = _msg.reader().readByte();
                    switch (action) {
                        case 0:
                            short idItem = _msg.reader().readShort();
                            byte moneyType = _msg.reader().readByte();
                            int money = _msg.reader().readInt();
                            int quantity;
                            if (player.getSession().version >= 222) {
                                quantity = _msg.reader().readInt();
                            } else {
                                quantity = _msg.reader().readByte();
                            }
                            if (quantity > 0) {
                                ConsignShopService.gI().KiGui(player, idItem, money, moneyType, quantity);
                            }
                            break;
                        case 1:
                        case 2:
                            idItem = _msg.reader().readShort();
                            ConsignShopService.gI().claimOrDel(player, action, idItem);
                            break;
                        case 3:
                            idItem = _msg.reader().readShort();
                            _msg.reader().readByte();
                            _msg.reader().readInt();
                            ConsignShopService.gI().buyItem(player, idItem);
                            break;
                        case 4:
                            moneyType = _msg.reader().readByte();
                            money = _msg.reader().readByte();
                            ConsignShopService.gI().openShopKyGui(player, moneyType, money);
                            break;
                        case 5:
                            idItem = _msg.reader().readShort();
                            ConsignShopService.gI().upItemToTop(player, idItem);
                            break;
                        default:
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            break;
                    }
                    break;
                case 127:
                    if (player != null) {
                        byte actionRadar = _msg.reader().readByte();
                        switch (actionRadar) {
                            case 0:
                                RadarService.gI().sendRadar(player, player.Cards);
                                break;
                            case 1:
                                short idC = _msg.reader().readShort();
                                Card card = player.Cards.stream().filter(r -> r != null && r.Id == idC).findFirst().orElse(null);
                                if (card != null) {
                                    boolean isSpecial = (idC == 1591 || idC == 1592 || idC == 1593);
                                    if (isSpecial) {
                                        if (card.Level < 1) {
                                            Service.gI().sendThongBao(player, "Cần sưu tầm tối thiểu 10 thẻ để đạt Cấp 1 mới có thể sử dụng thẻ này");
                                            return;
                                        }
                                    } else {
                                        if (card.Level < 2) {
                                            Service.gI().sendThongBao(player, "Cần sưu tầm đủ mảnh để đạt cấp 2 mới có thể sử dụng thẻ này");
                                            return;
                                        }
                                    }
                                    if (card.Used == 0) {
                                        if (player.Cards.stream().anyMatch(c -> c != null && c.Used == 1)) {
                                            Service.gI().sendThongBao(player, "Số thẻ sử dụng đã đạt tối đa");
                                            return;
                                        }
                                        card.Used = 1;
                                    } else {
                                        card.Used = 0;
                                    }
                                    RadarService.gI().Radar1(player, idC, card.Used);
                                    RadarService.gI().sendAura(player, player.getAura(), player.getEffFront());
                                    Service.gI().point(player);
                                }
                                break;
                        }
                    }
                    break;
                case -105:
                    if (player != null) {
                        if (player.type == 0 && player.maxTime == 30) {
                            ChangeMapService.gI().changeMapBySpaceShip(player, 102, -1, Util.nextInt(60, 200));
                            player.idMark.setGotoFuture(false);
                        } else if (player.type == 1 && player.maxTime == 5) {
                            if (player.idMark != null && player.idMark.isGoToBDKB()) {
                                ChangeMapService.gI().changeMap(player, MapService.gI().getMapCanJoin(player, 135, -1), 35, 35);
                                player.idMark.setGoToBDKB(false);
                            }
                        } else if (player.type == 2 && player.maxTime == 5) {
                            if (MapService.gI().isMapHanhTinhThucVat(player.zone.map.mapId)) {
                                ChangeMapService.gI().changeMap(player, 80, -1, -1, 5);
                            } else {
                                ChangeMapService.gI().changeMap(player, 160, -1, -1, 5);
                            }
                        } else if (player.type == 3 && player.maxTime == 5) {
                            ChangeMapService.gI().changeMap(player, player.idMark.getZoneKhiGasHuyDiet(), player.idMark.getXMapKhiGasHuyDiet(), player.idMark.getYMapKhiGasHuyDiet());
                            player.idMark.setZoneKhiGasHuyDiet(null);
                        } else if (player.type == 4 && player.maxTime == 5) {
                            if (player.idMark != null && player.idMark.isGoToKGHD()) {
                                ChangeMapService.gI().changeMap(player, MapService.gI().getMapCanJoin(player, 149, -1), 100 + (Util.nextInt(-10, 10)), 336);
                                player.idMark.setGoToKGHD(false);
                            }
                        } else if (player.type == 5 && player.maxTime == 5) {
                            ChangeMapService.gI().changeMap(player, MapService.gI().getMapCanJoin(player, 156, -1), 100 + (Util.nextInt(-10, 10)), 336);
                        }
                    }
                    break;
                case 42:
                    //Đăng ký tài khoản nhanh
                    Service.gI().regisAccount(_session, _msg);
                    break;
                case -127:
                    if (player != null) {
                        LuckyRound.gI().readOpenBall(player, _msg);
                    }
                    break;
                case -125:
                    if (player != null) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                        Input.gI().doInput(player, _msg);
                    }
                    break;
                case 112:
                    if (player != null) {
                        IntrinsicService.gI().showMenu(player);
                    }
                    break;
                case -34:
                    if (player != null) {
                        switch (_msg.reader().readByte()) {
                            case 1:
                                if (player.magicTree != null) {
                                    player.magicTree.openMenuTree();
                                } else {
                                    Service.gI().sendThongBao(player, "Cây ma thuật chưa được khởi tạo!");
                                }
                                break;
                            case 2:
                                if (player.magicTree != null) {
                                    player.magicTree.loadMagicTree();
                                } else {
                                    Service.gI().sendThongBao(player, "Cây ma thuật chưa được khởi tạo!");
                                }
                                break;
                        }
                    }
                    break;
                case -99:
                    if (player != null) {
                        FriendAndEnemyService.gI().controllerEnemy(player, _msg);
                    }
                    break;
                case 18:
                    if (player != null) {
                        player.changeMapVIP = true;
                        FriendAndEnemyService.gI().goToPlayerWithYardrat(player, _msg);
                    }
                    break;
                case -72:
                    if (player != null) {
                        FriendAndEnemyService.gI().chatPrivate(player, _msg);
                    }
                    break;
                case -80:
                    if (player != null) {
                        FriendAndEnemyService.gI().controllerFriend(player, _msg);
                    }
                    break;
                case -59:
                    if (player != null) {
                        if (player.baovetaikhoan) {
                            Service.gI().sendThongBao(player, "Chức năng bảo vệ đã được bật. Bạn vui lòng kiểm tra lại");
                            return;
                        }
                        PVPService.gI().controllerThachDau(player, _msg);
                    }
                    break;
                case -86:
                    if (player != null) {
                        TransactionService.gI().controller(player, _msg);
                    }
                    break;
                case -107:
                    if (player != null) {
                        Service.gI().sendChiSoPetGoc(player);
                        Service.gI().showInfoPet(player);
                    }
                    break;
                case -108:
                    if (player != null && player.pet != null) {
                        player.pet.changeStatus(_msg.reader().readByte());
                    }
                    break;
                case 6: //buy item
                    if (player != null && !Maintenance.isRunning) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                        if (player.baovetaikhoan) {
                            Service.gI().sendThongBao(player, "Chức năng bảo vệ đã được bật. Bạn vui lòng kiểm tra lại");
                            return;
                        }
                        byte typeBuy = _msg.reader().readByte();
                        int tempId = _msg.reader().readShort();
//                        int quantity = 0;
//                        try {
//                            quantity = _msg.reader().readShort();
//                        } catch (Exception e) {
//                        }
                        ShopService.gI().takeItem(player, typeBuy, tempId);
                    }
                    break;
                case 7: //sell item
                    if (player != null && !Maintenance.isRunning) {
                        if (player.isTrade) {
                            return;
                        }
                        int typeSell = _msg.reader().readByte();
                        if (typeSell == 0) {
                            ShopService.gI().showConfirmSellItem(player, _msg.reader().readByte(),
                                    _msg.reader().readShort());
                        } else {
                            ShopService.gI().sellItem(player, _msg.reader().readByte(),
                                    _msg.reader().readShort());
                        }
                    }
                    break;
                case 29:
                    if (player != null) {
                        ChangeMapService.gI().openZoneUI(player);
                    }
                    break;
                case 21:
                    if (player != null) {
                        int zoneId = _msg.reader().readByte();
                        ChangeMapService.gI().changeZone(player, zoneId);
                    }
                    break;
                case -71:
                    if (player != null) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                        ChatGlobalService.gI().chat(player, _msg.reader().readUTF());
                    }
                    break;
                case -79:
                    if (player != null) {
                        Service.gI().getPlayerMenu(player, _msg.reader().readInt());
                    }
                    break;
                case -113:
                    if (player != null) {
                        for (int i = 0; i < 10; i++) {
                            try {
                                player.playerSkill.skillShortCut[i] = _msg.reader().readByte();
                            } catch (IOException e) {
                                player.playerSkill.skillShortCut[i] = -1;
                            }
                        }
                        player.playerSkill.sendSkillShortCut();
                    }
                    break;
                case -101:
                    login2(_session, _msg);
                    break;
                case -103:
                    if (player != null) {
                        byte act = _msg.reader().readByte();
                        switch (act) {
                            case 0 ->
                                Service.gI().openFlagUI(player);
                            case 1 ->
                                Service.gI().chooseFlag(player, _msg.reader().readByte());
                        }
                    }
                    break;
                case -7:
                    if (player != null) {
                        // Chụp location vào local: nếu dispose() null hóa player.location giữa chừng
                        // (kick/logout trên thread khác) thì local vẫn trỏ tới object hợp lệ -> không NPE
                        var loc = player.location;
                        if (loc == null) {
                            break;
                        }
                        if (player.isDie()) {
                            Service.gI().charDie(player);
                            return;
                        }
                        if (player.effectSkill != null && player.effectSkill.isHaveEffectSkill()) {
                            return;
                        }
                        int toX = loc.x;
                        int toY = loc.y;
                        try {
                            byte b = _msg.reader().readByte();
                            toX = _msg.reader().readShort();
                            try {
                                toY = _msg.reader().readShort();
                            } catch (IOException ex) {
                            }
                            if (player.zone != null && MapService.gI().isMapBlackBallWar(player.zone.map.mapId)
                                    && Util.getDistance(loc.x, loc.y, toX, toY) > 500) {
                                return;
                            }
                            if (b == 1) {
                                AchievementService.gI().checkDoneTaskFly(player, loc.x - toX);
                            }
                        } catch (IOException e) {
                        }
                        PlayerService.gI().playerMove(player, toX, toY);
                    }
                    break;
                case -74:
                    byte type = _msg.reader().readByte();
                    if (type == 1) {
                        DataGame.sendSizeRes(_session);
                    } else if (type == 2) {
                        DataGame.sendRes(_session);
                    }
                    break;
                case -81:
                    if (player != null) {
                        try {
                            _msg.reader().readByte();
                            int[] indexItem = new int[_msg.reader().readByte()];
                            for (int i = 0; i < indexItem.length; i++) {
                                indexItem[i] = _msg.reader().readByte();
                            }
                            CombineService.gI().showInfoCombine(player, indexItem);
                        } catch (IOException e) {
                        }
                    }
                    break;
                case -87:
                    DataGame.updateData(_session);
                    break;
                case -67:
                    int id = _msg.reader().readInt();
                    DataGame.sendIcon(_session, id);
                    break;
                case 66:
                    DataGame.sendImageByName(_session, _msg.reader().readUTF());
                    break;
                case -66:
                    if (player != null) {
                        int effId = _msg.reader().readShort();
                        int idT = effId;
                        if (player.zone == null) {
                            break;
                        }
                        int shenronType = player.zone.shenronType;
                        if (idT == 25) {
                            idT = 51;
                            if (shenronType != -1 && player.zone.map.mapId != 0 && player.zone.map.mapId != 7 && player.zone.map.mapId != 14) {
                                idT = (shenronType == 1 || shenronType == 0) ? 59 : 60; 
                            }
                        }

                        DataGame.sendEffectTemplate(_session, effId, idT);
                    }
                    break;
                case -62:
                    if (player != null) {
                        FlagBagService.gI().sendIconFlagChoose(player, _msg.reader().readByte());
                    }
                    break;
                case -63:
                    if (player != null) {
                        byte fbid = _msg.reader().readByte();
                        int fbidz = fbid & 0xFF; //Chuyển sang byte không dấu
                        FlagBagService.gI().sendIconEffectFlag(player, fbidz);
                    }
                    break;
                case -32:
                    int bgId = _msg.reader().readShort();
                    DataGame.sendItemBGTemplate(_session, bgId);
                    break;
                case 22:
                    if (player != null) {
                        _msg.reader().readByte();
                        NpcManager.getNpc(ConstNpc.DAU_THAN).confirmMenu(player, _msg.reader().readByte());
                    }
                    break;
                case -33:
                case -23:
                    if (player != null) {
                        ChangeMapService.gI().changeMapWaypoint(player);
                        Service.gI().hideWaitDialog(player);
                    }
                    break;
                case -45:
                    if (player != null) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                        byte status = _msg.reader().readByte();
                        SkillService.gI().useSkill(player, null, null, status, _msg);
                    }
                    break;
                case -46:
                    if (player != null) {
                        ClanService.gI().getClan(player, _msg);
                    }
                    break;
                case -51:
                    if (player != null) {
                        ClanService.gI().clanMessage(player, _msg);
                    }
                    break;
                case -54:
                    if (player != null) {
                        ClanService.gI().clanDonate(player, _msg);
                    }
                    break;
                case -49:
                    if (player != null) {
                        ClanService.gI().joinClan(player, _msg);
                    }
                    break;
                case -50:
                    if (player != null) {
                        ClanService.gI().sendListMemberClan(player, _msg.reader().readInt());
                    }
                    break;
                case -56:
                    if (player != null) {
                        ClanService.gI().clanRemote(player, _msg);
                    }
                    break;
                case -47:
                    if (player != null) {
                        ClanService.gI().sendListClan(player, _msg.reader().readUTF());
                    }
                    break;
                case -55:
                    if (player != null) {
                        ClanService.gI().showMenuLeaveClan(player);
                    }
                    break;
                case -57:
                    if (player != null) {
                        ClanService.gI().clanInvite(player, _msg);
                    }
                    break;
                case -40:
                    if (player != null) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                        UseItem.gI().getItem(_session, _msg);
                    }
                    break;
                case -41:
                    Service.gI().sendCaption(_session, _msg.reader().readByte());
                    break;
                case -43:
                    if (player != null) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                        if (player.baovetaikhoan) {
                            Service.gI().sendThongBao(player, "Chức năng bảo vệ đã được bật. Bạn vui lòng kiểm tra lại");
                            return;
                        }
                        UseItem.gI().doItem(player, _msg);
                    }
                    break;
                case -91:
                    if (player != null) {
                        switch (player.idMark.getTypeChangeMap()) {
                            case ConstMap.CHANGE_CAPSULE -> {
                                UseItem.gI().choseMapCapsule(player, _msg.reader().readByte());
                            }
                            case ConstMap.CHANGE_BLACK_BALL -> {
                                BlackBallWarService.gI().changeMap(player, _msg.reader().readByte());
                            }
                        }
                    }
                    break;
                case -39:
                    if (player != null && player.zone != null) {
                        ChangeMapService.gI().finishLoadMap(player);
                    }
                    break;

                case 11:
                    byte modId = _msg.reader().readByte();
                    DataGame.requestMobTemplate(_session, modId);
                    break;
                case 44:
                    if (player != null) {
                        if (TransactionService.gI().check(player)) {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                        Command.gI().chat(player, _msg.reader().readUTF());
                    }
                    break;
                case 32:
                    if (player != null) {
                        int npcId = _msg.reader().readShort();
                        int select = _msg.reader().readByte();
                        MenuController.gI().doSelectMenu(player, npcId, select);
                    }
                    break;
                case 33:
                    if (player != null) {
                        int npcId = _msg.reader().readShort();
                        MenuController.gI().openMenuNPC(_session, npcId, player);
                    }
                    break;
                case 34:
                    if (player != null) {
                        int selectSkill = _msg.reader().readShort();
                        SkillService.gI().selectSkill(player, selectSkill);
                    }
                    break;
                case 54:
                    if (player != null) {
                        int mobId = _msg.reader().readByte();
                        int masterId = -1;
                        boolean isMobMe = mobId == -1;
                        if (isMobMe) {
                            masterId = _msg.reader().readInt();
                        }
//                        _msg.reader().readByte();
                        Service.gI().attackMob(player, mobId, isMobMe, masterId);
                    }
                    break;
                case -60:
                    if (player != null) {
                        int playerId = _msg.reader().readInt();
//                        _msg.reader().readByte();
                        Service.gI().attackPlayer(player, playerId);
                    }
                    break;
                case -27:
                    _session.sendKey();
                    DataGame.sendVersionRes(_session);
                    break;
                case -111:
                    DataGame.sendDataImageVersion(_session);
                    break;
                case -20:
                    if (player != null && !player.isDie()) {
                        int itemMapId = _msg.reader().readShort();
                        ItemMapService.gI().pickItem(player, itemMapId, false);
                    }
                    break;
                case -28:
                    messageNotMap(_session, _msg);
                    break;
                case -29:
                    messageNotLogin(_session, _msg);
                    break;
                case -30:
                    messageSubCommand(_session, _msg);
                    break;
                case -15: // về nhà
                    if (player != null) {
                        if (services.tournament.GuruGuildWarService.gI().isInWar(player)) {
                            services.tournament.GuruGuildWarService.gI().respawnAtBase(player);
                            break;
                        }
                        int mapId = MapService.gI().isMapMaBu(player.zone.map.mapId) ? 114 : player.gender + 21;
                        ChangeMapService.gI().changeMapBySpaceShip(player, mapId, 0, -1);
                    }
                    break;
                case -16: // hồi sinh
                    if (player != null && !player.isPKDHVT) {
                        if (services.tournament.GuruGuildWarService.gI().isInWar(player)) {
                            services.tournament.GuruGuildWarService.gI().reviveAtPlace(player);
                            break;
                        }
                        PlayerService.gI().hoiSinh(player);
                    }
                    break;
                case -104:
                    if (player != null) {
                        Service.gI().mabaove(player, _msg.reader().readInt());
                    }
                    break;
                case -118:
                    if (player != null) {
                        int _id = _msg.reader().readInt();
                        int menuType = player.idMark.getMenuType();
                        switch (menuType) {
                            case 0, 1, 2 -> {
                                SuperRankService.gI().competing(player, _id);
                            }
                            default -> {
                                if (player.isAdmin()) {
                                    Boss boss = BossManager.gI().getBoss(_id);
                                    if (boss != null) {
                                        ChangeMapService.gI().changeMapYardrat(player, boss.zone, boss.location.x, boss.location.y);
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                                }
                            }
                        }
                    }
                    break;
                case -38: //finish update
                    if (player != null) {
                        finishUpdate(player);
                    }
                    break;
                case 126: //androidPack2
                    break;
                case -78: //checkMMove
                    _msg.reader().readInt(); // second
                    break;
                case -114: //RequestPean
                    break;
                case 27:
//                    short menuid
                    break;
                case -76:
                    AchievementService.gI().confirmAchievement(player, _msg.reader().readByte());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            errors++;
            if (errors <= 5 || errors % 50 == 0) {
                Logger.logException(Controller.class, e);
                if (player != null) {
                    Logger.warning("Player: " + player.name + "\n");
                }
                Logger.warning("Lỗi function: 'onMessage' (lần thứ " + errors + ")\n");
                Logger.warning("Lỗi controller message command: " + _msg.command + "\n");
            }
        } finally {
            _msg.cleanup();
            _msg.dispose();
            long timeDo = System.currentTimeMillis() - st;
            if (timeDo > 1000) {
                Logger.warning(_msg.command + " - TimeOut: " + timeDo + " ms\n");
            }
        }
    }

    public void messageNotLogin(MySession session, Message msg) {
        if (msg != null) {
            try {
//                if (true) {
//                    String key;
//                    try {
//                        key = msg.reader().readUTF();
//                    } catch (IOException e) {
//                        key = "";
//                    }
//                    if (!key.equals("benkutephomaique896")) {
//                        session.disconnect();
//                        return;
//                    }
//                }
                byte cmd = msg.reader().readByte();
                switch (cmd) {
                    case 0:
                        session.login(msg.reader().readUTF(), msg.reader().readUTF(), msg.reader().readUTF());
                        break;
                    case 2:
                        Service.gI().setClientType(session, msg);
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                session.disconnect();
//                Logger.logException(Controller.class, e);
            }
        }
    }

    
    public void messageNotMap(MySession _session, Message _msg) {
        if (_msg != null) {
            Player player = null;
            try {
                player = _session.player;
                byte cmd = _msg.reader().readByte();
                switch (cmd) {
                    case 2:
                        createChar(_session, _msg);
                        break;
                    case 6:
                        DataGame.updateMap(_session);
                        break;
                    case 7:
                        DataGame.updateSkill(_session);
                        break;
                    case 8:
                        ItemData.updateItem(_session);
                        break;
                    case 10:
                        DataGame.sendMapTemp(_session, _msg.reader().readUnsignedByte());
                        break;
                    case 13:
                        // client ok
                        enterMap(player);
                        break;

                    default:
                        break;
                }
            } catch (IOException e) {
                Logger.logException(Controller.class, e);
            }
        }
    }

    public void messageSubCommand(MySession _session, Message _msg) {
        if (_msg != null) {
            Player player = null;
            try {
                player = _session.player;
                byte command = _msg.reader().readByte();
                switch (command) {
                    case 16:
                        byte type = _msg.reader().readByte();
                        short point = _msg.reader().readShort();
                        if (player != null && player.nPoint != null) {
                            player.nPoint.increasePoint(type, point, false);
                        }
                        break;
                    case 18:
                        byte type2 = _msg.reader().readByte();
                        short point2 = _msg.reader().readShort();
                        if (player != null && player.pet != null) {
                            player.pet.nPoint.increasePoint(type2, point2, true);
                        }
                        break;
                    case 64:
                        int playerId = _msg.reader().readInt();
                        int menuId = _msg.reader().readShort();
                        SubMenuService.gI().controller(player, playerId, menuId);
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                Logger.logException(Controller.class, e);
            }
        }
    }

    public void enterMap(Player player) {
        if (player != null && player.isPl()
                && player.playerSkill != null
                && player.playerTask != null
                && player.inventory != null
                && player.nPoint != null) {
            MySession ss = player.getSession();
            if (ss == null || ss.enteredMap) {
                return;
            }
            ss.enteredMap = true;
            Service.gI().player(player);
            Service.gI().Send_Caitrang(player);

            // -64 my flag bag
            Service.gI().sendFlagBag(player);

            // -113 skill shortcut
            player.playerSkill.sendSkillShortCut();
            // item time
            ItemTimeService.gI().sendAllItemTime(player);

            // send current task
            TaskService.gI().sendInfoCurrentTask(player);

            if (TaskService.gI().getIdTask(player) == ConstTask.TASK_0_0) {
                NpcService.gI().createTutorial(player, -1,
                        "Chào mừng " + player.name + " đến với Ngọc Rồng Online\n"
                        + "Nhiệm vụ đầu tiên của bạn là di chuyển\n"
                        + "Bạn hãy di chuyển nhân vật theo mũi tên chỉ hướng");
            } else {
                // -70 thông báo bigmessage
                sendThongBaoServer(player);
            }

            if (player.inventory.itemsBody.get(11).isNotNullItem()) {
                Service.gI().sendChibi(player);
            }

            // Kiểm tra zone có null không trước khi gọi mapInfo
            if (player.zone != null) {
                player.zone.mapInfo(player);
            } else {
                // Xử lý trường hợp player.zone là null (nếu cần thiết)
                //    System.out.println("Player zone is null for player: " + player.name);
            }

            if (player.getSession().version >= 231) {
                for (Skill skill : player.playerSkill.skills) {
                    if (skill.currLevel <= 0 || skill.template.type != 4) {
                        continue;
                    }
                    SkillService.gI().sendCurrLevelSpecial(player, skill);
                }
            }
            Service.gI().sendTimeSkill(player);
            TrainingService.gI().tnsmLuyenTapUp(player);
            player.sendNewPet();
            if (TaskService.gI().getIdTask(player) >= ConstTask.TASK_32_0 && !player.isAdmin() && player.getSession().eventPoint >= 0) {
                ChatGlobalService.gI().chatVip(player, "Trùm server " + player.name + " vừa mới nhậm chức, chúng mày nằm xuống!");
                if (player.getSession().version < 237) {
                    Service.gI().sendThongBaoAllPlayer("Trùm server " + player.name + " vừa mới nhậm chức, chúng mày nằm xuống!");
                }
                ServerNotify.gI().notify("Trùm server " + player.name + " vừa mới nhậm chức, chúng mày nằm xuống!");
            }
            if (player.isAdmin()) {
                Service.gI().sendMessageServer("Admin đã online anh em cần hỗ trợ gì inbox Zalo nhé !!!");
            }
            if (player.getSession() != null && player.getSession().danap > 0) {
                AchievementService.gI().checkDoneTask(player, ConstAchievement.LAN_DAU_NAP_NGOC);
            }
        }
    }

    public void createChar(MySession session, Message msg) {
        if (!session.hasAllowedClientVersion()) {
            Service.gI().sendThongBaoOK(session, services.ClientVersionPolicy.DENIED_MESSAGE);
            return;
        }
        if (!services.LoginAccessConfig.canLogin(session.isAdmin)) {
            Service.gI().sendThongBaoOK(session, services.LoginAccessConfig.DENIED_MESSAGE);
            return;
        }
        if (!Maintenance.isRunning) {
            AlyraResultSet rs = null;
            boolean created = false;
            try {
                String name = msg.reader().readUTF();
                int gender = msg.reader().readByte();
                int hair = msg.reader().readByte();
                if (name.length() >= 5 && name.length() <= 10) {
                    rs = AlyraManager.executeQuery("select * from player where name = ?", name);
                    if (rs.first()) {
                        Service.gI().sendThongBaoOK(session, "Tên nhân vật đã tồn tại");
                    } else {
                        if (Util.haveSpecialCharacter(name)) {
                            Service.gI().sendThongBaoOK(session, "Tên nhân vật không được chứa ký tự đặc biệt");
                        } else {
                            boolean isNotIgnoreName = true;
                            for (String n : ConstIgnoreName.IGNORE_NAME) {
                                if (name.equals(n)) {
                                    Service.gI().sendThongBaoOK(session, "Tên nhân vật đã tồn tại");
                                    isNotIgnoreName = false;
                                    break;
                                }
                            }
                            if (isNotIgnoreName) {
                                PlayerDAO.CreateResult result = PlayerDAO.createNewPlayer(
                                        session.userId, name.toLowerCase(), (byte) gender, hair);
                                if (result == PlayerDAO.CreateResult.CREATED) {
                                    created = true;
                                } else if (result == PlayerDAO.CreateResult.ALREADY_EXISTS) {
                                    created = true;
                                } else {
                                    Service.gI().sendThongBaoOK(session, "Không thể tạo nhân vật, vui lòng thử lại sau.");
                                }
                            }
                        }
                    }
                } else {
                    Service.gI().sendThongBaoOK(session, "Tên nhân vật chỉ đồng ý các ký tự a-z, 0-9 và chiều dài từ 5 đến 10 ký tự");
                }
            } catch (Exception e) {
//                Logger.logException(Controller.class, e);
            } finally {
                if (rs != null) {
                    rs.dispose();
                }
            }
            if (created) {
                session.login(session.uu, session.pp);
                if (session.player != null) {
                    enterMap(session.player);
                }
            }
        }
    }

    public void login2(MySession session, Message msg) {
        Service.gI().switchToRegisterScr(session);
        //  Service.gI().sendThongBaoOK(session, "Đăng Ký Tài Khoản Tại Trang Chủ - Cậu Bé Rồng Online\nhttps://cauberong.com");
    }

    public void sendInfo(MySession session) {
        try {
            Player player = session.player;
            DataGame.sendTileSetInfo(session);
            IntrinsicService.gI().sendInfoIntrinsic(player);
            Service.gI().point(player);
            TaskService.gI().sendTaskMain(player);
            Service.gI().clearMap(player);
            ClanService.gI().sendMyClan(player);
            PlayerService.gI().sendMaxStamina(player);
            PlayerService.gI().sendCurrentStamina(player);
            Service.gI().sendNangDong(player);
            Service.gI().sendHavePet(player);
            Service.gI().sendTopRank(player);
            if (player.superRank != null && player.superRank.rank < 1) {
                player.superRank.rank = SuperRankDAO.getRank((int) player.id);
                player.superRank.lastRewardTime = System.currentTimeMillis();
                SuperRankDAO.insertData(player);
            }
            ServerNotify.gI().sendNotifyTab(player);
            player.setClothes.setup();
            if (player.pet != null) {
                player.pet.setClothes.setup();
            }
            if (player.inventory.itemsBody.get(7).isNotNullItem()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        PetService.Pet2(player, player.getHeadThuCung(), player.getBodyThuCung(),
                                player.getLegThuCung());
                        Service.gI().point(player);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, "Pet update").start();
            } 
            ItemTimeService.gI().sendCanAutoPlay(player);
            player.start();
        } catch (Exception e) {
        }
    }

    public void finishUpdate(Player player) {
        if (player.getSession() != null) {
            player.getSession().finishUpdate = true;
        }
    }

    private void sendThongBaoServer(Player player) {
        NpcService.gI().createTutorial(player, 1139,
                "|7|Chào mừng bạn đến với máy chủ Ngọc Rồng Online\n"
                + "|0|Xem và nhập giftcode tại nhà nha các tình yêu\n"        
                + "|0|Tải game và nạp thẻ tại : nroha.fun");
    }
}
