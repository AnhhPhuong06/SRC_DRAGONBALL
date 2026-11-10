package managers.boss;

 
import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import models.boss.boss_list.Nappa.Rambo;
import models.boss.boss_list.Nappa.MapDauDinh;
import models.boss.boss_list.Nappa.Kuku;
import models.boss.boss_list.Android.Android19;
import models.boss.boss_list.Android.Pic;
import models.boss.boss_list.Android.Android14;
import models.boss.boss_list.Android.Poc;
import models.boss.boss_list.Android.Android13;
import models.boss.boss_list.Android.KingKong;
import models.boss.boss_list.Android.DrKore;
import models.boss.boss_list.Android.Android15;
import models.boss.boss_list.GoldenFrieza.DeathBeam1;
import models.boss.boss_list.GoldenFrieza.DeathBeam2;
import models.boss.boss_list.GoldenFrieza.DeathBeam3;
import models.boss.boss_list.GoldenFrieza.DeathBeam4;
import models.boss.boss_list.GoldenFrieza.DeathBeam5;
import models.boss.boss_list.GoldenFrieza.GoldenFrieza;
import models.boss.boss_list.Cooler.Cooler;
import models.boss.boss_list.Cell.SieuBoHung;
import models.boss.boss_list.Cell.XenBoHung;
import models.boss.boss_list.Broly.Broly;
//import models.boss.boss_list.ChristmasEvent.OngGiaNoel;
//import models.boss.boss_list.TaoPaiPai.TaoPaiPai;
import models.boss.boss_list.Frieza.Fide;
import models.boss.boss_list.MajinBuu12H.Mabu;
import models.boss.boss_list.MajinBuu12H.BuiBui;
import models.boss.boss_list.MajinBuu12H.BuiBui2;
import models.boss.boss_list.MajinBuu12H.Cadic;
import models.boss.boss_list.MajinBuu12H.Drabura;
import models.boss.boss_list.MajinBuu12H.Drabura2;
import models.boss.boss_list.MajinBuu12H.Drabura3;
import models.boss.boss_list.MajinBuu12H.Goku;
import models.boss.boss_list.MajinBuu12H.Yacon;
import models.boss.boss_list.MajinBuu14H.Mabu2H;
import models.boss.boss_list.MajinBuu14H.SuperBu;
import models.boss.boss_list.GinyuForce.SO1;
import models.boss.boss_list.GinyuForce.SO2;
import models.boss.boss_list.GinyuForce.SO3;
import models.boss.boss_list.GinyuForce.SO4;
import models.boss.boss_list.GinyuForce.TDT;
import models.boss.boss_list.NamekGinyuForce.SO1_NM;
import models.boss.boss_list.NamekGinyuForce.SO2_NM;
import models.boss.boss_list.NamekGinyuForce.SO3_NM;
import models.boss.boss_list.NamekGinyuForce.SO4_NM;
import models.boss.boss_list.NamekGinyuForce.TDT_NM;
import models.boss.boss_list.Earth.BIDO;
import models.boss.boss_list.Earth.BOJACK;
import models.boss.boss_list.Earth.BUJIN;
import models.boss.boss_list.Earth.KOGU;
import models.boss.boss_list.Earth.SUPER_BOJACK;
import models.boss.boss_list.Earth.ZANGYA;
import models.boss.boss_list.Yardart.CHIENBINH0;
import models.boss.boss_list.Yardart.CHIENBINH1;
import models.boss.boss_list.Yardart.CHIENBINH2;
import models.boss.boss_list.Yardart.CHIENBINH3;
import models.boss.boss_list.Yardart.CHIENBINH4;
import models.boss.boss_list.Yardart.CHIENBINH5;
import models.boss.boss_list.Yardart.DOITRUONG5;
import models.boss.boss_list.Yardart.TANBINH0;
import models.boss.boss_list.Yardart.TANBINH1;
import models.boss.boss_list.Yardart.TANBINH2;
import models.boss.boss_list.Yardart.TANBINH3;
import models.boss.boss_list.Yardart.TANBINH4;
import models.boss.boss_list.Yardart.TANBINH5;
import models.boss.boss_list.Yardart.TAPSU0;
import models.boss.boss_list.Yardart.TAPSU1;
import models.boss.boss_list.Yardart.TAPSU2;
import models.boss.boss_list.Yardart.TAPSU3;
import models.boss.boss_list.Yardart.TAPSU4;
import models.boss.boss_list.Cell.XENCON1;
import models.boss.boss_list.Cell.XENCON2;
import models.boss.boss_list.Cell.XENCON3;
import models.boss.boss_list.Cell.XENCON4;
import models.boss.boss_list.Cell.XENCON5;
import models.boss.boss_list.Cell.XENCON6;
import models.boss.boss_list.Cell.XENCON7;
import models.player.Player;
import network.io.Message;
import services.map.MapService;

import java.util.ArrayList;
import java.util.List;
//import models.boss.AnTrom;
import models.boss.boss_list.Black.BlackGoku;
import models.boss.boss_list.BossRongNhi.RongNhi1Sao;
import models.boss.boss_list.BossRongNhi.RongNhi2Sao;
import models.boss.boss_list.BossRongNhi.RongNhi3Sao;
import models.boss.boss_list.BossRongNhi.RongNhi4Sao;
import models.boss.boss_list.BossRongNhi.RongNhi5Sao;
import models.boss.boss_list.BossRongNhi.RongNhi6Sao;
import models.boss.boss_list.BossRongNhi.RongNhi7Sao;
import models.boss.boss_list.ChristmasEvent.BrolySSJ3;
import models.boss.boss_list.ChristmasEvent.CoolerVang;
import models.boss.boss_list.ChristmasEvent.SuperXaydaGod;
import models.boss.boss_list.ChristmasEvent.TuanLoc;
import models.boss.boss_list.Cooler.Chiller;
import models.boss.boss_list.Cooler.Frost;
import models.boss.boss_list.Hit.Hit;
import models.boss.boss_list.HungVuongEvent.SonTinh;
import models.boss.boss_list.HungVuongEvent.ThuyTinh;
import models.boss.boss_list.NewBoss.*;
import models.boss.boss_list.Rocket.Kochiro;
import models.boss.boss_list.Rocket.Meowth;
import models.boss.boss_list.Rocket.Musashi;
import models.boss.boss_list.TrungThuEvent.Gogeta;
import models.boss.boss_list.TrungThuEvent.KhiDot;
import models.boss.boss_list.TrungThuEvent.Rong1SAO;
import models.boss.boss_list.TrungThuEvent.ThoDaiCa;
import models.boss.boss_list.huydiet.Cumber;
import models.boss.boss_list.huydiet.berus;
import models.boss.boss_list.huydiet.champa;
import models.boss.boss_list.huydiet.vados;
import models.boss.boss_list.huydiet.whis;
import models.boss.boss_list.vodaisc.VDBill;
import models.boss.boss_list.vodaisc.VDChampa;
import models.boss.boss_list.vodaisc.VDDaiThienSu;
import models.boss.boss_list.vodaisc.VDVados;
import models.boss.boss_list.vodaisc.VDWhis;
import models.boss.boss_list.pocolo.Pocolo;
import models.boss.boss_list.Zeno.Zeno;
import models.boss.boss_list.Zeno.HauVe1;
import models.boss.boss_list.Zeno.HauVe2;
import models.boss.boss_list.HalloweenEvent.MaTroi;
import models.boss.boss_list.HalloweenEvent.Doi;
import models.boss.boss_list.HalloweenEvent.BiMa;
import models.boss.boss_list.HalloweenEvent.Janemba;
import models.boss.boss_list.SuperBoss.*;
import models.map.Zone;
import server.Maintenance;
import utils.Logger;
import utils.Util;

public class BossManager implements Runnable {

    private static BossManager instance;
    public static byte ratioReward = 10;

    public static BossManager gI() {
        if (instance == null) {
            instance = new BossManager();
        }
        return instance;
    }

    public BossManager() {
        this.bosses = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    protected final List<Boss> bosses;

    public void addBoss(Boss boss) {
        this.bosses.add(boss);
    }

    public void removeBoss(Boss boss) {
        this.bosses.remove(boss);
    }

    public void removeHalloweenBoss(Boss boss) {
        if (boss == null || !event.event_list.Halloween.isBoss((int) boss.id)) return;
        if (boss.zone != null) services.map.ChangeMapService.gI().exitMap(boss);
        boss.changeStatus(BossStatus.REST);
        this.bosses.remove(boss);
    }

    public void removeHalloweenBosses() {
        for (Boss boss : this.bosses) removeHalloweenBoss(boss);
    }

    public List<Boss> getBosses() {
        return this.bosses;
    }

    /** Số lượng boss theo trạng thái: [alive, respawn, wait]. Dùng cho Dashboard. */
    public int[] getBossStatusCounts() {
        int alive = 0, respawn = 0, waiting = 0;
        for (Boss b : this.bosses) {
            if (b == null || b.bossStatus == null) {
                continue;
            }
            switch (b.bossStatus) {
                case REST ->
                    waiting++;
                case RESPAWN ->
                    respawn++;
                default ->
                    alive++;
            }
        }
        return new int[]{alive, respawn, waiting};
    }

    /** Hồi sinh ngay mọi boss đang nghỉ (chỉ boss tự spawn được — DEFAULT_APPEAR). */
    public void respawnAllRestingBosses() {
        for (Boss b : new java.util.ArrayList<>(this.bosses)) {
            if (b == null || b.bossStatus != BossStatus.REST) {
                continue;
            }
            try {
                if (b.data == null || b.data.length == 0
                        || b.data[0].getTypeAppear() != consts.AppearType.DEFAULT_APPEAR) {
                    continue;
                }
                b.lastTimeRest = 0;
                b.changeStatus(BossStatus.RESPAWN);
            } catch (Exception ignored) {
            }
        }
    }

    /** Reset toàn bộ boss: hạ những boss đang hoạt động để chúng vào lại chu trình hồi sinh. */
    public void resetAllBosses() {
        for (Boss b : new java.util.ArrayList<>(this.bosses)) {
            if (b == null || b.bossStatus == null || b.bossStatus == BossStatus.REST) {
                continue;
            }
            try {
                if (b.nPoint != null) {
                    b.nPoint.hp = 0;
                }
                b.changeStatus(BossStatus.DIE);
            } catch (Exception ignored) {
            }
        }
    }

    public void loadBoss() {
        //this.createBoss(BossID.HIT);
        this.createBoss(BossID.FROST);
        this.createBoss(BossID.TIEU_DOI_TRUONG);
        this.createBoss(BossID.TIEU_DOI_TRUONG_NM);
        this.createBoss(BossID.BOJACK);
        this.createBoss(BossID.SUPER_BOJACK);
        this.createBoss(BossID.KING_KONG);
        this.createBoss(BossID.XEN_BO_HUNG);
        this.createBoss(BossID.SIEU_BO_HUNG);
        this.createBoss(BossID.KUKU, 8);
        this.createBoss(BossID.MAP_DAU_DINH, 8);
        this.createBoss(BossID.RAMBO, 8);
        this.createBoss(BossID.FIDE);
        this.createBoss(BossID.ANDROID_14);
        this.createBoss(BossID.DR_KORE);
        this.createBoss(BossID.COOLER);
        this.createBoss(BossID.BLACK_GOKU, 3);
        this.createBoss(BossID.BROLY, 2);
        this.createBoss(BossID.CHILLER);
        this.createBoss(BossID.CUMBER);
        this.createBoss(BossID.VD_CHAMPA);
        this.createBoss(BossID.ZENO);
        // VD_VADOS / VD_BILL / VD_WHIS / VD_DAI_THIEN_SU do Champa tạo qua bossAppearTogether
        // (cùng xuất hiện 1 khu, active từng con 1). Không tạo trực tiếp ở đây để tránh trùng.
    //    this.createBoss(BossID.RONG_1_SAO);
    //    this.createBoss(BossID.RONG_2_SAO);
    //    this.createBoss(BossID.RONG_3_SAO);
    //    this.createBoss(BossID.RONG_4_SAO);
    //    this.createBoss(BossID.RONG_5_SAO);
    //    this.createBoss(BossID.RONG_6_SAO);
    //    this.createBoss(BossID.RONG_7_SAO);
    //    this.createBoss(BossID.SON_TINH);
    //    this.createBoss(BossID.THUY_TINH);
    //    this.createBoss(BossID.KHIDOT);
    //    this.createBoss(BossID.THODAICA, 10);
    //    this.createBoss(BossID.GOGETA);
    //    this.createBoss(BossID.RONGSAO);
    //    this.createBoss(BossID.MEOWTH, 3);
    //    this.createBoss(BossID.WHIS_1);
       this.createBoss(BossID.GOHAN_BEAST);
       this.createBoss(BossID.CELL_MAX);
       this.createBoss(BossID.BROLY_SUPER);
       this.createBoss(BossID.BLACK_FRIEZA);
       this.createBoss(BossID.MORO);
        this.createBoss(-998877, 5);
        // Tạo 1 con Sói Hẹc Quyn (Chó Rách), 1 con Ở Dơ (Ba Đô) và 1 con Xinbatô (Xibachao)
        try {
            new ChoRach();
        } catch (Exception ignored) {
        }
        try {
            new Xibachao();
        } catch (Exception ignored) {
        }
        try {
            new BaDo();
        } catch (Exception ignored) {
        }
    }

    public void createBoss(int bossID, int total) {
        for (int i = 0; i < total; i++) {
            createBoss(bossID);
        }
    }

    public Boss createBoss(int bossID) {
        if (!event.EventManager.HALLOWEEN && event.event_list.Halloween.isBoss(bossID)) return null;
        try {
            return switch (bossID) {
                case BossID.BERUS ->
                    new berus();
                case BossID.WHIS_1 ->
                    new whis();
                case BossID.VD_CHAMPA ->
                    new VDChampa();
                case BossID.VD_VADOS ->
                    new VDVados();
                case BossID.VD_BILL ->
                    new VDBill();
                case BossID.VD_WHIS ->
                    new VDWhis();
                case BossID.VD_DAI_THIEN_SU ->
                    new VDDaiThienSu();
                case BossID.ZENO ->
                    new Zeno();
                case BossID.HAU_VE_1 ->
                    new HauVe1();
                case BossID.HAU_VE_2 ->
                    new HauVe2();
                case BossID.KOCHIRO ->
                    new Kochiro();
                case BossID.MEOWTH ->
                    new Meowth();
                case BossID.MUSASHI ->
                    new Musashi();
            //    case BossID.KHIDOT ->
            //        new KhiDot();
                case BossID.THODAICA ->
                    new ThoDaiCa();
                case BossID.GOGETA ->
                    new Gogeta();
                case BossID.RONGSAO ->
                    new Rong1SAO();
                case BossID.HIT ->
                    new Hit();
                case BossID.CUMBER ->
                    new Cumber();
                case BossID.TUAN_LOC ->
                    new TuanLoc();               
                case BossID.COLLER_GOLD ->
                    new CoolerVang();
                case BossID.GOKUGOD ->
                    new SuperXaydaGod();
                case BossID.BROLYSSJ ->
                    new BrolySSJ3();
                case BossID.CHILLER ->
                    new Chiller();
                case BossID.FROST ->
                    new Frost();
                case BossID.BLACK_GOKU ->
                    new BlackGoku();
                case BossID.TAP_SU_0 ->
                    new TAPSU0();
                case BossID.TAP_SU_1 ->
                    new TAPSU1();
                case BossID.TAP_SU_2 ->
                    new TAPSU2();
                case BossID.TAP_SU_3 ->
                    new TAPSU3();
                case BossID.TAP_SU_4 ->
                    new TAPSU4();
                case BossID.TAN_BINH_5 ->
                    new TANBINH5();
                case BossID.TAN_BINH_0 ->
                    new TANBINH0();
                case BossID.TAN_BINH_1 ->
                    new TANBINH1();
                case BossID.TAN_BINH_2 ->
                    new TANBINH2();
                case BossID.TAN_BINH_3 ->
                    new TANBINH3();
                case BossID.TAN_BINH_4 ->
                    new TANBINH4();
                case BossID.CHIEN_BINH_5 ->
                    new CHIENBINH5();
                case BossID.CHIEN_BINH_0 ->
                    new CHIENBINH0();
                case BossID.CHIEN_BINH_1 ->
                    new CHIENBINH1();
                case BossID.CHIEN_BINH_2 ->
                    new CHIENBINH2();
                case BossID.CHIEN_BINH_3 ->
                    new CHIENBINH3();
                case BossID.CHIEN_BINH_4 ->
                    new CHIENBINH4();
                case BossID.DOI_TRUONG_5 ->
                    new DOITRUONG5();
                case BossID.SO_4 ->
                    new SO4();
                case BossID.SO_3 ->
                    new SO3();
                case BossID.SO_2 ->
                    new SO2();
                case BossID.SO_1 ->
                    new SO1();
                case BossID.TIEU_DOI_TRUONG ->
                    new TDT();
                case BossID.SO_4_NM ->
                    new SO4_NM();
                case BossID.SO_3_NM ->
                    new SO3_NM();
                case BossID.SO_2_NM ->
                    new SO2_NM();
                case BossID.SO_1_NM ->
                    new SO1_NM();
                case BossID.TIEU_DOI_TRUONG_NM ->
                    new TDT_NM();
                case BossID.BUJIN ->
                    new BUJIN();
                case BossID.KOGU ->
                    new KOGU();
                case BossID.ZANGYA ->
                    new ZANGYA();
                case BossID.BIDO ->
                    new BIDO();
                case BossID.BOJACK ->
                    new BOJACK();
                case BossID.SUPER_BOJACK ->
                    new SUPER_BOJACK();
                case BossID.KUKU ->
                    new Kuku();
                case BossID.MAP_DAU_DINH ->
                    new MapDauDinh();
                case BossID.RAMBO ->
                    new Rambo();
                case BossID.DRABURA ->
                    new Drabura();
                case BossID.BUI_BUI ->
                    new BuiBui();
                case BossID.BUI_BUI_2 ->
                    new BuiBui2();
                case BossID.YA_CON ->
                    new Yacon();
                case BossID.DRABURA_2 ->
                    new Drabura2();
                case BossID.GOKU ->
                    new Goku();
                case BossID.CADIC ->
                    new Cadic();
                case BossID.MABU_12H ->
                    new Mabu();
                case BossID.DRABURA_3 ->
                    new Drabura3();
                case BossID.MABU ->
                    new Mabu2H();
                case BossID.SUPERBU ->
                    new SuperBu();
                case BossID.FIDE ->
                    new Fide();
                case BossID.DR_KORE ->
                    new DrKore();
                case BossID.ANDROID_19 ->
                    new Android19();
                case BossID.ANDROID_13 ->
                    new Android13();
                case BossID.ANDROID_14 ->
                    new Android14();
                case BossID.ANDROID_15 ->
                    new Android15();
                case BossID.PIC ->
                    new Pic();
                case BossID.POC ->
                    new Poc();
                case BossID.KING_KONG ->
                    new KingKong();
                case BossID.XEN_BO_HUNG ->
                    new XenBoHung();
                case BossID.SIEU_BO_HUNG ->
                    new SieuBoHung();
                case BossID.XEN_CON_1 ->
                    new XENCON1();
                case BossID.XEN_CON_2 ->
                    new XENCON2();
                case BossID.XEN_CON_3 ->
                    new XENCON3();
                case BossID.XEN_CON_4 ->
                    new XENCON4();
                case BossID.XEN_CON_5 ->
                    new XENCON5();
                case BossID.XEN_CON_6 ->
                    new XENCON6();
                case BossID.XEN_CON_7 ->
                    new XENCON7();
                case BossID.COOLER ->
                    new Cooler();
                case BossID.BROLY ->
                    new Broly();
                case BossID.DEATH_BEAM_1 ->
                    new DeathBeam1();
                case BossID.DEATH_BEAM_2 ->
                    new DeathBeam2();
                case BossID.DEATH_BEAM_3 ->
                    new DeathBeam3();
                case BossID.DEATH_BEAM_4 ->
                    new DeathBeam4();
                case BossID.DEATH_BEAM_5 ->
                    new DeathBeam5();
                // boss sk
            //    case BossID.RONG_1_SAO ->
            //        new RongNhi1Sao();
            //    case BossID.RONG_2_SAO ->
            //        new RongNhi2Sao();
            //    case BossID.RONG_3_SAO ->
            //        new RongNhi3Sao();
            //    case BossID.RONG_4_SAO ->
            //        new RongNhi4Sao();
            //    case BossID.RONG_5_SAO ->
            //        new RongNhi5Sao();
            //    case BossID.RONG_6_SAO ->
            //        new RongNhi6Sao();
            //    case BossID.RONG_7_SAO ->
            //        new RongNhi7Sao();
            //    case BossID.SON_TINH ->
            //        new SonTinh();
            //    case BossID.THUY_TINH ->
            //        new ThuyTinh();
                case BossID.MATROI ->
                    new MaTroi();
                case BossID.DOI ->
                    new Doi();
                case BossID.BIMA ->
                    new BiMa();
                case BossID.JANEMBA ->
                    new Janemba();
                case BossID.GOHAN_BEAST ->
                    new GohanBeast();
                case BossID.CELL_MAX ->
                    new CellMax();
                case BossID.BROLY_SUPER ->
                    new BrolySuper();
                case BossID.BLACK_FRIEZA ->
                    new BlackFrieza();
                case BossID.MORO ->
                    new Moro();
                default ->
                    null;
            };
        } catch (Exception e) {
            Logger.error(e + "\n");
            return null;
        }
    }

    public Boss getBoss(int id) {
        try {
            Boss boss = this.bosses.get(id);
            if (boss != null) {
                return boss;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public void showListBoss(Player player) {
        if (!player.isAdmin()) {
            return;
        }
        player.idMark.setMenuType(3);
        Message msg;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Boss");
            msg.writer().writeByte((int) bosses.stream().filter(boss -> !MapService.gI().isMapBossFinal(boss.data[0].getMapJoin()[0]) && !MapService.gI().isMapHuyDiet(boss.data[0].getMapJoin()[0]) && !MapService.gI().isMapYardart(boss.data[0].getMapJoin()[0]) && !MapService.gI().isMapMaBu(boss.data[0].getMapJoin()[0]) && !MapService.gI().isMapBlackBallWar(boss.data[0].getMapJoin()[0])).count());
            for (int i = 0; i < bosses.size(); i++) {
                Boss boss = this.bosses.get(i);
                if (MapService.gI().isMapBossFinal(boss.data[0].getMapJoin()[0]) || MapService.gI().isMapYardart(boss.data[0].getMapJoin()[0]) || MapService.gI().isMapHuyDiet(boss.data[0].getMapJoin()[0]) || MapService.gI().isMapMaBu(boss.data[0].getMapJoin()[0]) || MapService.gI().isMapBlackBallWar(boss.data[0].getMapJoin()[0])) {
                    continue;
                }
                msg.writer().writeInt(i);
                msg.writer().writeInt(i);
                msg.writer().writeShort(boss.data[0].getOutfit()[0]);
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(boss.data[0].getOutfit()[1]);
                msg.writer().writeShort(boss.data[0].getOutfit()[2]);
                msg.writer().writeUTF(boss.data[0].getName());
                if (boss.zone != null) {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(boss.zone.map.mapName + "(" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId + "");
                } else {
                    msg.writer().writeUTF(BossStatus.REST.toString());
                    long respawnMillis = boss.lastTimeRest + boss.data[0].getSecondsRest() * 1000L;
                    long msLeft = Math.max(0, respawnMillis - System.currentTimeMillis());
                    long secLeft = msLeft / 1000;
                    msg.writer().writeUTF("Hồi sinh sau: " + secLeft + " giây");
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public Boss getBossById(int bossId) {
        return this.bosses.stream().filter(boss -> boss.id == bossId && !boss.isDie()).findFirst().orElse(null);
    }

    public boolean checkBosses(Zone zone, int BossID) {
        return this.bosses.stream().filter(boss -> boss.id == BossID && boss.zone != null && boss.zone.equals(zone) && !boss.isDie()).findFirst().orElse(null) != null;
    }

    public Player findBossClone(Player player) {
        return player.zone.getBosses().stream().filter(boss -> boss.id < -100_000_000 && !boss.isDie()).findFirst().orElse(null);
    }

    public Boss getBossById(int bossId, int mapId, int zoneId) {
        return this.bosses.stream().filter(boss -> boss.id == bossId && boss.zone != null && boss.zone.map.mapId == mapId && boss.zone.zoneId == zoneId && !boss.isDie()).findFirst().orElse(null);
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();
                updateBosses();
                if (150 - (System.currentTimeMillis() - st) > 0) {
                    Thread.sleep(150 - (System.currentTimeMillis() - st));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void updateBosses() {
        List<Boss> snapshot = new ArrayList<>(this.bosses);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Boss boss = snapshot.get(i);
            try {
                if (!event.EventManager.HALLOWEEN && event.event_list.Halloween.isBoss((int) boss.id)) {
                    removeHalloweenBoss(boss);
                    continue;
                }
                boss.update();
            } catch (Exception e) {
                System.out.println("[BOSS-ERR] " + boss.name + " status=" + boss.bossStatus
                        + " lvl=" + boss.currentLevel + " : " + e);
                e.printStackTrace();
            }
        }
    }

}
