package models.map;

/*
 * @Author: DienCoLamCoi
 * @Description: Äiá»‡n CÆ¡ LÃ¢m CÃ²i - ChuyÃªn cung cáº¥p thiáº¿t bá»‹ Ä‘iá»‡n cÆ¡ uy tÃ­n cháº¥t lÆ°á»£ng cao.
 * @Group Zalo: Giao lÆ°u chia sáº» kinh nghiá»‡m code - https://zalo.me/g/lsqfzx907
 */


import consts.ConstMap;
import models.Template;
import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import managers.boss.BossManager;
import consts.ConstMob;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import services.dungeon.MajinBuuService;
import models.dungeon.BlackBallWar;
import models.dungeon.RedRibbonHQ;
import services.dungeon.RedRibbonHQService;
import models.mob.Mob;
import models.npc.Npc;
import models.npc.NpcFactory;
import models.player.Player;
import server.Manager;
import services.Service;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import models.map.EffectMap;
import models.mob.bigboss_list.GaChinCua;
import models.mob.bigboss_list.GauTuongCuop;
import models.mob.bigboss_list.Hirudegarn;
import models.mob.bigboss_list.NguaChinLmao;
import models.mob.bigboss_list.Piano;
import models.mob.bigboss_list.RobotBaoVe;
import models.mob.bigboss_list.VoiChinNga;
import models.mob.bigboss_list.VuaBachTuoc;
import models.dungeon.TreasureUnderSea;
import services.dungeon.TreasureUnderSeaService;
import services.dungeon.BlackBallWarService;
import models.dungeon.SnakeWay;
import services.dungeon.SnakeWayService;
import models.dungeon.DestronGas;
import services.dungeon.DestronGasService;
import models.dungeon.MajinBuu14H;
import services.dungeon.MajinBuu14HService;
import services.dungeon.SuperDivineWaterService;
import services.map.MapService;
import utils.Logger;

public class Map implements Runnable {

    public static final byte T_EMPTY = 0;
    public static final byte T_TOP = 2;
    private static final int SIZE = 24;

    public int mapId;
    public String mapName;

    public byte planetId;
    public String planetName;

    public byte tileId;
    public byte bgId;
    public byte bgType;
    public byte type;

    public int[][] tileMap;
    public int[] tileTop;
    public int mapWidth;
    public int mapHeight;

    public List<Zone> zones;
    public List<WayPoint> wayPoints;
    public List<Npc> npcs;

    public int tmw;
    public int tmh;
    public int pxh;
    public int pxw;
    public int[] types;
    public int[] maps;
    
    public List<EffectMap> effMap;

    private byte[] bgItemDataCache;
    private boolean bgItemDataLoaded;

    public byte[] getBgItemData() {
        if (!this.bgItemDataLoaded) {
            try {
                this.bgItemDataCache = utils.FileIO.readFile("data/map/item_bg_map_data/" + this.mapId);
            } catch (Exception e) {
                this.bgItemDataCache = null;
            }
            this.bgItemDataLoaded = true;
        }
        return this.bgItemDataCache;
    }

    public Map(int mapId, String mapName, byte planetId,
            byte tileId, byte bgId, byte bgType, byte type, int[][] tileMap,
            int[] tileTop, int zones, int maxPlayer, List<WayPoint> wayPoints, List<EffectMap> effMap) {
        this.mapId = mapId;
        this.mapName = mapName;
        this.planetId = planetId;
        this.planetName = Service.gI().get_HanhTinh(planetId);
        this.tileId = tileId;
        this.bgId = bgId;
        this.bgType = bgType;
        this.type = type;
        this.tileMap = tileMap;
        this.tileTop = tileTop;
        this.zones = new ArrayList<>();
        this.wayPoints = wayPoints;
        this.effMap = effMap;
        try {
            this.mapHeight = tileMap.length * SIZE;
            this.mapWidth = tileMap[0].length * SIZE;
        } catch (Exception ignored) {
        }
        this.readTileMap(mapId);
        this.initZone(zones, maxPlayer);
        this.initItem();
        this.initTrapMap();
    }

    private void initZone(int nZone, int maxPlayer) {
        switch (this.type) {
            case ConstMap.MAP_OFFLINE ->
                nZone = 1;
            case ConstMap.MAP_BLACK_BALL_WAR ->
                nZone = BlackBallWar.AVAILABLE;
            case ConstMap.MAP_MA_BU ->
                nZone = MajinBuuService.AVAILABLE;
            case ConstMap.MAP_MABU_14H ->
                nZone = 7;
            case ConstMap.MAP_DOANH_TRAI ->
                nZone = RedRibbonHQ.AVAILABLE;
            case ConstMap.MAP_BAN_DO_KHO_BAU ->
                nZone = TreasureUnderSea.AVAILABLE;
            case ConstMap.MAP_CON_DUONG_RAN_DOC ->
                nZone = SnakeWay.AVAILABLE;
            case ConstMap.MAP_KHI_GAS_HUY_DIET ->
                nZone = DestronGas.AVAILABLE;
        }

        // Phó bản Halloween (map 181): mỗi clan 1 zone riêng -> tạo nhiều zone
        if (this.mapId == services.dungeon.HalloweenDungeonService.MAP_ID) {
            nZone = models.dungeon.HalloweenDungeon.AVAILABLE;
        }

        // Đảo Guru (map 13): tạo thêm các khu (tối thiểu 25 khu) làm Đấu Trường Bang Hội
        if (this.mapId == 13) {
            nZone = Math.max(nZone, 25);
        }

        for (int i = 0; i < nZone; i++) {
            Zone zone = new Zone(this, i, maxPlayer);
            this.zones.add(zone);
            switch (this.type) {
                case ConstMap.MAP_BLACK_BALL_WAR ->
                    BlackBallWarService.gI().addMapBlackBallWar(i, zone);
                case ConstMap.MAP_DOANH_TRAI ->
                    RedRibbonHQService.gI().addMapDoanhTrai(i, zone);
                case ConstMap.MAP_BAN_DO_KHO_BAU ->
                    TreasureUnderSeaService.gI().addMapBanDoKhoBau(i, zone);
                case ConstMap.MAP_CON_DUONG_RAN_DOC ->
                    SnakeWayService.gI().addMapConDuongRanDoc(i, zone);
                case ConstMap.MAP_KHI_GAS_HUY_DIET ->
                    DestronGasService.gI().addMapKhiGasHuyDiet(i, zone);
                case ConstMap.MAP_TAY_KARIN ->
                    SuperDivineWaterService.gI().addZone(zone);
                case ConstMap.MAP_MABU_14H ->
                    MajinBuu14HService.gI().addMapMaBu2H(i, zone);
            }
        }
    }

    public void initNpc(byte[] npcId, short[] npcX, short[] npcY) {
        this.npcs = new ArrayList<>();
        for (int i = 0; i < npcId.length; i++) {
            this.npcs.add(NpcFactory.createNPC(this.mapId, 1, npcX[i], npcY[i], npcId[i]));
        }

        // Tự động sinh NPC Trọng Tài tại Đảo Guru (map 13) nếu chưa có
        if (this.mapId == 13) {
            boolean hasTrongTai = false;
            for (models.npc.Npc npc : this.npcs) {
                if (npc != null && npc.tempId == consts.ConstNpc.TRONG_TAI) {
                    hasTrongTai = true;
                    break;
                }
            }
            if (!hasTrongTai) {
                this.npcs.add(models.npc.NpcFactory.createNPC(this.mapId, 1, 600, 384, consts.ConstNpc.TRONG_TAI));
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                long st = System.currentTimeMillis();
                for (Zone zone : this.zones) {
                    try {
                        zone.update();
                    } catch (Exception e) {
                        Logger.logException(Map.class, e, "Lá»—i update zone");
                    }
                }
                long timeDo = System.currentTimeMillis() - st;
                if (1000 - timeDo > 0) {
                    Thread.sleep(1000 - timeDo);
                }
            } catch (Exception e) {
                Logger.logException(Map.class, e, "Lá»—i update map " + this.mapName);
            }
        }
    }

    public void initMob(byte[] mobTemp, byte[] mobLevel, long[] mobHp, short[] mobX, short[] mobY) {
        for (int i = 0; i < mobTemp.length; i++) {
            int mobTempId = mobTemp[i];
            Template.MobTemplate temp = Manager.getMobTemplateByTemp(mobTempId);
            if (temp != null) {
                Mob mob = new Mob();
                mob.id = i;
                mob.tempId = mobTemp[i];
                mob.level = mobLevel[i];
                mob.point.setHpFull(mobHp[i]);
                mob.location.x = mobX[i];
                mob.location.y = mobY[i];
                mob.point.sethp(mob.point.getHpFull());
                mob.pDame = temp.percentDame;
                mob.pTiemNang = temp.percentTiemNang;
                mob.type = temp.type;
                mob.setTiemNang();
                for (Zone zone : this.zones) {
                    Mob mobZone;
                    switch (mob.tempId) {
                        case ConstMob.HIRUDEGARN ->
                            mobZone = new Hirudegarn(mob);
                        case ConstMob.VUA_BACH_TUOC ->
                            mobZone = new VuaBachTuoc(mob);
                        case ConstMob.ROBOT_BAO_VE ->
                            mobZone = new RobotBaoVe(mob);
                        case ConstMob.GAU_TUONG_CUOP ->
                            mobZone = new GauTuongCuop(mob);
                        case ConstMob.VOI_CHIN_NGA ->
                            mobZone = new VoiChinNga(mob);
                        case ConstMob.GA_CHIN_CUA ->
                            mobZone = new GaChinCua(mob);
                        case ConstMob.NGUA_CHIN_LMAO ->
                            mobZone = new NguaChinLmao(mob);
                        case ConstMob.PIANO ->
                            mobZone = new Piano(mob);
                        default ->
                            mobZone = new Mob(mob);
                    }

                    mobZone.zone = zone;
                    zone.mobs.add(mobZone);
                }
            }
        }
    }

    public void initMob(List<Mob> mobs) {
        for (Zone zone : zones) {
            for (Mob m : mobs) {
                Mob mob = new Mob(m);
                mob.zone = zone;
                zone.mobs.add(mob);
            }
        }
    }

    private void initTrapMap() {
        for (Zone zone : zones) {
            TrapMap trap = null;
            switch (this.mapId) {
                case 135 -> {
                    trap = new TrapMap();
                    trap.x = 260;
                    trap.y = 960;
                    trap.w = 740;
                    trap.h = 72;
                    trap.effectId = 49; //xiÃªn
                    zone.trapMaps.add(trap);
                }

            }
        }
    }

    private void initItem() {
        for (Zone zone : zones) {
            ItemMap itemMap = null;
            switch (this.mapId) {
                case 21 ->
                    itemMap = new ItemMap(zone, 74, 1, 633, 315, -1);
                case 22 ->
                    itemMap = new ItemMap(zone, 74, 1, 56, 315, -1);
                case 23 ->
                    itemMap = new ItemMap(zone, 74, 1, 633, 320, -1);
                case 39 ->
                    itemMap = new ItemMap(zone, 78, 1, 70, 288, -1);
                case 40 ->
                    itemMap = new ItemMap(zone, 78, 1, 70, 264, -1);
                case 41 ->
                    itemMap = new ItemMap(zone, 78, 1, 70, 288, -1);
                case 42 ->
                    itemMap = new ItemMap(zone, 78, 1, 70, 288, -1);
                case 43 ->
                    itemMap = new ItemMap(zone, 78, 1, 70, 264, -1);
                case 44 ->
                    itemMap = new ItemMap(zone, 78, 1, 70, 288, -1);
                case 85 -> //1 sao Ä‘en
                    itemMap = new ItemMap(zone, 372, 1, 0, 0, -1);
                case 86 -> //2 sao Ä‘en
                    itemMap = new ItemMap(zone, 373, 1, 0, 0, -1);
                case 87 -> //3 sao Ä‘en
                    itemMap = new ItemMap(zone, 374, 1, 0, 0, -1);
                case 88 -> //4 sao Ä‘en
                    itemMap = new ItemMap(zone, 375, 1, 0, 0, -1);
                case 89 -> //5 sao Ä‘en
                    itemMap = new ItemMap(zone, 376, 1, 0, 0, -1);
                case 90 -> //6 sao Ä‘en
                    itemMap = new ItemMap(zone, 377, 1, 0, 0, -1);
                case 91 -> //7 sao Ä‘en
                    itemMap = new ItemMap(zone, 378, 1, 0, 0, -1);
            }
        }

    }

    public void initBoss() {
        for (Zone zone : zones) {
            short bossId = -1;
            switch (this.mapId) {
                case 114 ->
                    bossId = BossID.DRABURA;
                case 115 ->
                    bossId = BossID.BUI_BUI;
                case 117 ->
                    bossId = BossID.BUI_BUI_2;
                case 118 ->
                    bossId = BossID.YA_CON;
                case 119 ->
                    bossId = BossID.DRABURA_2;
                case 120 ->
                    bossId = BossID.MABU_12H;
                case 127 ->
                    bossId = BossID.MABU;
                case 128 ->
                    bossId = BossID.SUPERBU;
                case 131 ->
                    bossId = BossID.TAN_BINH_5;
                case 132 ->
                    bossId = BossID.CHIEN_BINH_5;
                case 133 ->
                    bossId = BossID.DOI_TRUONG_5;
            }
            if (bossId != -1) {
                Boss boss = BossManager.gI().createBoss(bossId);
                if (boss == null) {
                    continue;
                }
                boss.zoneFinal = zone;
                boss.respawn();
                boss.changeToTypePK();
                boss.changeStatus(BossStatus.JOIN_MAP);
            }
        }
    }

    public short mapIdNextMabu(short mapId) {
        return switch (mapId) {
            case 114 ->
                115;
            case 115 ->
                117;
            case 117 ->
                118;
            case 118 ->
                119;
            case 119 ->
                120;
            default ->
                -1;
        };
    }

    public Npc getNpc(Player player, int tempId) {
        for (Npc npc : npcs) {
            if (npc.tempId == tempId && (MapService.gI().isMapBlackBallWar(mapId) || Util.getDistance(player, npc) <= 60)) {
                return npc;
            }
        }
        return null;
    }

    //--------------------------------------------------------------------------
    /** Kẹp hoành độ vào trong bản đồ (cách mép 24px). */
    public int kepX(int x) {
        if (mapWidth <= 48) {
            return x;
        }
        return x < 24 ? 24 : (x > mapWidth - 24 ? mapWidth - 24 : x);
    }

    /** Kẹp tung độ vào trong bản đồ (cách mép 24px). */
    public int kepY(int y) {
        if (mapHeight <= 48) {
            return y;
        }
        return y < 24 ? 24 : (y > mapHeight - 24 ? mapHeight - 24 : y);
    }

    public int yPhysicInTop(int x, int y) {
        try {
            int rX = (int) x / SIZE;
            int rY = 0;
            if (isTileTop(tileMap[y / SIZE][rX])) {
                return y;
            }
            for (int i = y / SIZE; i < tileMap.length; i++) {
                if (isTileTop(tileMap[i][rX])) {
                    rY = i * SIZE;
                    break;
                }
            }
            return rY;
        } catch (Exception e) {
            return y;
        }
    }

    public int LastY(int cx, int cy) {
        int num = 0;
        int ySd = 0;
        int xSd = cx;
        if (this.tileTypeAt(cx, cy, 2)) {
            return cy;
        }
        while (num < 30) {
            num++;
            ySd += 24;
            if (this.tileTypeAt(xSd, ySd, 2)) {
                if (ySd % 24 != 0) {
                    ySd -= ySd % 24;
                }
                break;
            }
        }
        return ySd;
    }

    public boolean tileTypeAt(int x, int y, int type) {
        try {
            return (types[y / 24 * tmw + x / 24] & type) == type;
        } catch (Exception e) {
            return false;
        }
    }

    public int[] moveXY(Player player) {
        int xsd = player.location.x / 24;
        int ysd = player.location.y / 24;
        int p = this.mapId == 103 ? 4 : 3;
        if (tileMap[ysd][xsd] != 0) {
            if (tileMap[ysd - p][xsd] != 0) {
                if (tileMap[LastY(player.location.x, player.location.y - p * 24) / 24][xsd] != 0) {
                    return new int[]{
                        player.xSend,
                        player.ySend
                    };
                } else {
                    return new int[]{
                        player.xSend,
                        LastY(player.location.x, 120)
                    };
                }
            }
            return new int[]{
                player.location.x,
                ysd
            };
        }
        if (LastY(player.location.x, player.location.y) >= pxh - 24) {
            return new int[]{
                player.xSend,
                player.ySend
            };
        }
        return new int[]{
            player.location.x,
            ysd
        };
    }

    private boolean isTileTop(int tileMap) {
        for (int i = 0; i < tileTop.length; i++) {
            if (tileTop[i] == tileMap) {
                return true;
            }
        }
        return false;
    }

    public final void readTileMap(int mapId) {
        try {
            try (DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_map_data/" + mapId))) {
                dis.readByte();
                tmw = dis.readByte();
                tmh = dis.readByte();
                pxw = tmw * SIZE;
                pxh = tmh * SIZE;
                maps = new int[tmw * tmh];
                for (int j = 0; j < maps.length; j++) {
                    maps[j] = dis.readByte();
                }
                types = new int[maps.length];
            }
        } catch (IOException e) {
        }
    }
}

