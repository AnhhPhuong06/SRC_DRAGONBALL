package models.map;

 
import consts.ConstTask;
import models.boss.Boss;
import consts.BossID;
import models.item.Item;
import models.mob.Mob;
import models.npc.Npc;
import services.map.NpcManager;
import models.player.Player;
import network.io.Message;
import models.boss.boss_list.Training.TrainingBoss;
import consts.ConstMob;
import services.map.ItemMapService;
import services.ItemService;
import services.map.MapService;
import services.player.PlayerService;
import services.Service;
import services.TaskService;
import services.player.InventoryService;
import utils.FileIO;
import utils.Logger;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import models.map.EffectMap;
import models.map.vetinh.Satellite;
import models.npc.NonInteractiveNPC;

public class Zone {

    public static final byte PLAYERS_TIEU_CHUAN_TRONG_MAP = 7;

    public int countItemAppeaerd = 0;

    public Map map;
    public int zoneId;
    public int maxPlayer;
    public int shenronType = -1;

    @Getter
    private final List<Player> nonInteractiveNPCs; //npc
    @Getter
    private final List<Player> humanoids; //player, boss, pet
    @Getter
    private final List<Player> notBosses; //player, pet
    @Getter
    private final List<Player> players; //player
    private final List<Player> bosses; //boss
    private final List<Player> pets; //pet

    public final List<Mob> mobs;
    public final List<ItemMap> items;

    public final java.util.concurrent.atomic.AtomicBoolean updating = new java.util.concurrent.atomic.AtomicBoolean(false);

    public long lastTimeDropBlackBall;
    public boolean finishBlackBallWar;
    public boolean finishMapMaBu;

    public boolean isbulon1Alive = true;
    public boolean isbulon2Alive = true;
    public boolean isTUTAlive = true;
    public boolean isGoldenFriezaAlive;

    public boolean isCompeting;
    public String rankName1;
    public String rankName2;
    public int rank1;
    public int rank2;

    public List<TrapMap> trapMaps;
    public List<MaBuHold> maBuHolds;
    public boolean finishMap22h;

    @Setter
    @Getter
    public Player Npc;

    public boolean isFullPlayer() {
        return this.players.size() >= this.maxPlayer;
    }

    // Phương thức lấy danh sách các item trong zone
    public List<ItemMap> getItemsInZone() {
        return this.items;
    }

    public long getTotalHP() {
        long total = 0;
        synchronized (mobs) {
            for (Mob mob : mobs) {
                if (!mob.isDie()) {
                    total += mob.point.hp;
                }
            }
        }
        synchronized (players) {
            for (Player pl : players) {
                if (pl.nPoint != null && !pl.isDie()) {
                    total += pl.nPoint.hp;
                }
            }
        }
        synchronized (pets) {
            for (Player pl : pets) {
                if (pl.nPoint != null && !pl.isDie()) {
                    total += pl.nPoint.hp;
                }
            }
        }
        return total;
    }

    public boolean isActive() {
        if (!this.players.isEmpty() || !this.bosses.isEmpty() || !this.items.isEmpty() || !this.nonInteractiveNPCs.isEmpty()) {
            return true;
        }
        for (Mob mob : this.mobs) {
            if (mob.isDie() || mob.status == 0) {
                return true;
            }
        }
        return false;
    }

    private void udMob() {
        for (Mob m : this.mobs) {
            try {
                m.update();
            } catch (Exception e) {
                Logger.logException(Zone.class, e, "Lỗi update mobs");
            }
        }
    }

    private void udNonInteractiveNPC() {
        if (this.nonInteractiveNPCs.isEmpty()) {
            return;
        }
        try {
            for (Player pl : this.nonInteractiveNPCs) {
                if (pl != null && pl.zone != null) {
                    pl.update();
                }
            }
        } catch (Exception e) {
            Logger.logException(Zone.class, e, "Lỗi update npcs");
        }
    }

    private void udItem() {
        if (this.items.isEmpty()) {
            return;
        }
        try {
            for (ItemMap item : this.items) {
                try {
                    if (item != null && item.itemTemplate != null) {
                        item.update();
                    } else if (item != null) {
                        this.items.remove(item);
                    }
                } catch (Exception e) {
                    Logger.logException(Zone.class, e, "Lỗi item");
                }
            }
        } catch (Exception e) {
            Logger.logException(Zone.class, e, "Lỗi update items");
        }
    }

    private void udPlayer() {
        for (int i = this.notBosses.size() - 1; i >= 0; i--) {
            Player pl = this.notBosses.get(i);
            if (!pl.isPet && !pl.isBot && !pl.isNewPet) {
                this.notBosses.get(i).update();
            }
        }
    }

    public void update() {
        if (!this.mobs.isEmpty()) {
            udMob();
        }
        if (!this.items.isEmpty()) {
            udItem();
        }
        if (!this.nonInteractiveNPCs.isEmpty()) {
            udNonInteractiveNPC();
        }
    }

    public Zone(Map map, int zoneId, int maxPlayer) {
        this.map = map;
        this.zoneId = zoneId;
        this.maxPlayer = maxPlayer;
        this.nonInteractiveNPCs = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.humanoids = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.notBosses = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.players = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.bosses = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.pets = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.mobs = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.items = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.trapMaps = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.maBuHolds = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    public int getNumOfPlayers() {
        return this.players.size();
    }

    public int getNumOfBosses() {
        return this.bosses.size();
    }

    public List<Player> getBosses() {
        return this.bosses;
    }

    public boolean isBossCanJoin(Boss boss) {
        if (this.map.mapId == 5) {
            return false;
        }
        for (Player b : this.bosses) {
            if (b.id == boss.id) {
                return false;
            }
        }
        return true;
    }

    public void addPlayer(Player player) {
        if (player != null) {
            if (!this.humanoids.contains(player)) {
                this.humanoids.add(player);
            }

            if (player instanceof NonInteractiveNPC && !this.nonInteractiveNPCs.contains(player)) {
                this.nonInteractiveNPCs.add(player);
            }

            if (!player.isBoss && !player.isNewPet && !(player instanceof NonInteractiveNPC) && !this.notBosses.contains(player)) {
                this.notBosses.add(player);
            }

            if (!player.isBoss && !player.isNewPet && !player.isPet && !(player instanceof NonInteractiveNPC) && !this.players.contains(player)) {
                this.players.add(player);
            }

            if (player.isBoss && !this.bosses.contains(player)) {
                this.bosses.add(player);
            }
            if ((player.isPet || player.isNewPet) && !this.pets.contains(player)) {
                this.pets.add(player);
            }
        }
    }


    public void removePlayer(Player player) {
        this.nonInteractiveNPCs.remove(player);
        this.humanoids.remove(player);
        this.notBosses.remove(player);
        this.players.remove(player);
        this.bosses.remove(player);
        this.pets.remove(player);
    }

    public List<ItemMap> getSatellites() {
        return items.stream().filter(i -> i instanceof Satellite).toList();
    }

    public ItemMap getItemMapByItemMapId(int itemId) {
        for (ItemMap item : this.items) {
            if (item != null && item.itemMapId == itemId) {
                return item;
            }
        }
        return null;
    }

    public ItemMap getItemMapByTempId(int tempId) {
        // Món vừa bị dispose có itemTemplate = null -> phải chắn, không thì chỉ cần một
        // món chết nằm trong danh sách là mọi lần tra cứu đều văng NPE.
        for (ItemMap item : this.items) {
            if (item != null && item.itemTemplate != null && item.itemTemplate.id == tempId) {
                return item;
            }
        }
        return null;
    }

    public List<ItemMap> getItemMapsForPlayer(Player player) {
        if ((this.map.mapId >= 42 && this.map.mapId <= 44) || (this.map.mapId >= 39 && this.map.mapId <= 41)) {
            if (getItemMapByTempId(78) == null) {
                int x = 70;
                int y = (this.map.mapId == 43 || this.map.mapId == 40) ? 264 : 288;
                new ItemMap(this, 78, 1, x, y, -1);
            }
        }
        if (this.map.mapId >= 21 && this.map.mapId <= 23) {
            if (getItemMapByTempId(74) == null) {
                int x = (this.map.mapId == 22) ? 56 : 633;
                int y = (this.map.mapId == 23) ? 320 : 315;
                new ItemMap(this, 74, 1, x, y, -1);
            }
        }

        List<ItemMap> list = new ArrayList<>();
        // CopyOnWriteArrayList đã cung cấp iterator snapshot, không cần sao chép lần nữa.
        for (ItemMap item : items) {
            if (item == null || item.itemTemplate == null) {
                continue;
            }
            if (item.itemTemplate.id == 78) {
                int idTask = TaskService.gI().getIdTask(player);
                if (idTask != ConstTask.TASK_3_1 && !(idTask == ConstTask.TASK_3_2 && InventoryService.gI().findItemBag(player, 78) == null)) {
                    continue;
                }
            }
            if (item.itemTemplate.id == 74) {
                if (TaskService.gI().getIdTask(player) < ConstTask.TASK_3_0) {
                    continue;
                }
            }
            if (item.itemTemplate.id == 726 && item.playerId != player.id) {
                continue;
            }
            list.add(item);
        }
        return list;
    }

    public Player getPlayerInMap(long idPlayer) {
        for (Player pl : humanoids) {
            if (pl != null && pl.id == idPlayer) {
                return pl;
            }
        }
        return null;
    }

    public Player getPlayerInMapOffline(Player player, long idPlayer) {
        for (Player pl : bosses) {
            if (pl.id == idPlayer && pl instanceof TrainingBoss && ((TrainingBoss) pl).playerAtt.equals(player)) {
                return pl;
            }
        }
        return null;
    }

    public void pickItem(Player player, int itemMapId) {
        ItemMap itemMap = getItemMapByItemMapId(itemMapId);
        if (itemMap != null && itemMap.itemTemplate != null) {
            if (itemMap.itemTemplate.type == 22 || itemMap.itemTemplate.id == 460 && itemMap.playerId == player.id) {
                return;
            }
            int playerId = Math.abs(itemMap.playerId > 100_000_000 ? 1_000_000_000 - (int) itemMap.playerId : (int) itemMap.playerId);
            if (playerId == player.id || itemMap.playerId == player.id || itemMap.playerId == -1) {
                // Nhặt atomic: gỡ drop khỏi map TRƯỚC khi cộng vào hành trang, chặn 2 người
                // (hoặc magnet + tay) cùng nhặt 1 drop -> nhân bản. Item 74/78 giữ cơ chế nhặt lại.
                boolean repeatPick = (this.map.mapId >= 21 && this.map.mapId <= 23 && itemMap.itemTemplate.id == 74)
                        || (((this.map.mapId >= 42 && this.map.mapId <= 44) || (this.map.mapId >= 39 && this.map.mapId <= 41)) && itemMap.itemTemplate.id == 78);
                if (!repeatPick && !items.remove(itemMap)) {
                    return;
                }
                Item item = ItemService.gI().createItemFromItemMap(itemMap);
                if (item.template.id == 648) {
                    if (!InventoryService.gI().findItemTatVoGiangSinh(player)) {
                        Service.gI().sendThongBao(player, "Cần thêm Tất,vớ giáng sinh");
                        if (!repeatPick) {
                            items.add(itemMap);
                        }
                        return;
                    }
                }

                if (InventoryService.gI().addItemBag(player, item)) {
                    // [KOL] nhặt item: NR 7 sao (id 20) -> KOL 1/16; Capsule/CSKB (id 380) -> KOL 20/27
                    if (item.template.id == 20) {
                        models.task.Task_Kol.tick(player, item.quantity, 2, 16);
                    } else if (item.template.id == 380) {
                        models.task.Task_Kol.tick(player, item.quantity, 20, 27);
                    }
                    int itemType = item.template.type;
                    Message msg;
                    try {
                        msg = new Message(-20);
                        msg.writer().writeShort(itemMapId);
                        switch (itemType) {
                            case 9, 10, 34 -> {
                                msg.writer().writeUTF(item.quantity > Short.MAX_VALUE ? "Bạn vừa nhận được " + Util.formatNumber(item.quantity) + " " + item.template.name : "");
                                PlayerService.gI().sendInfoHpMpMoney(player);
                            }
                            default -> {
                                switch (item.template.id) {
                                    case 73 ->
                                        msg.writer().writeUTF("");
                                    case 74 ->
                                        msg.writer().writeUTF("Bạn mới vừa ăn " + item.template.name);
                                    case 78 -> {
                                        msg.writer().writeUTF("Wow, một cậu bé dễ thương!");
                                        InventoryService.gI().sendItemBags(player);
                                    }
                                    default -> {
                                        if (item.template.type >= 0 && item.template.type < 5) {
                                            msg.writer().writeUTF(item.template.name + " ngon...");
                                        } else {
                                            msg.writer().writeUTF("Bạn nhận được " + item.template.name);
                                        }
                                        if (item.template.id == 648) {
                                            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 649), 1);
                                        }
                                        InventoryService.gI().sendItemBags(player);
                                        player.effect.addPointOngThanVeChai();
                                    }
                                }
                            }

                        }
                        msg.writer().writeShort(item.quantity > Short.MAX_VALUE ? 9999 : item.quantity);
                        player.sendMessage(msg);
                        msg.cleanup();
                        if (itemMap.itemTemplate.id != 78 && itemMap.itemTemplate.id != 74) {
                            Service.gI().sendToAntherMePickItem(player, itemMapId);
                        }
                        if (!(this.map.mapId >= 21 && this.map.mapId <= 23
                                && itemMap.itemTemplate != null && itemMap.itemTemplate.id == 74
                                || ((this.map.mapId >= 42 && this.map.mapId <= 44) || (this.map.mapId >= 39 && this.map.mapId <= 41))
                                && itemMap.itemTemplate != null && itemMap.itemTemplate.id == 78)) {
                            removeItemMap(itemMap);
                        }
                    } catch (Exception e) {
                        Logger.logException(Zone.class, e);
                    }
                } else {
                    if (!repeatPick) {
                        items.add(itemMap);
                    }
                    if (!ItemMapService.gI().isBlackBall(item.template.id) && !ItemMapService.gI().isNamecBall(item.template.id) && !ItemMapService.gI().isNamecBallStone(item.template.id)) {
                        String text = "Hành trang không còn chỗ trống, không thể nhặt thêm";
                        Service.gI().sendThongBao(player, text);
                        return;
                    }
                }
            } else {
                Service.gI().sendThongBao(player, "Không thể nhặt vật phẩm của người khác");
                return;
            }
            TaskService.gI().checkDoneTaskPickItem(player, itemMap);
            TaskService.gI().checkDoneSideTaskPickItem(player, itemMap);
            TaskService.gI().checkDoneClanTaskPickItem(player, itemMap);
            services.func.GiftProgressService.gI().onPickItem(player, itemMap);
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    public void addItem(ItemMap itemMap) {
        if (itemMap != null && !items.contains(itemMap)) {
            items.add(0, itemMap);
        }
    }

    public void removeItemMap(ItemMap itemMap) {
        this.items.remove(itemMap);
    }

    public Player getRandomPlayerInMap() {
        List<Player> plNotVoHinh = new ArrayList();
        //Lỗi
        for (Player pl : this.notBosses) {
            if (pl != null && (pl.effectSkin == null || !pl.effectSkin.isVoHinh) && pl.maBuHold == null && !pl.isMabuHold) {
                plNotVoHinh.add(pl);
            }
        }
        if (!plNotVoHinh.isEmpty()) {
            return plNotVoHinh.get(Util.nextInt(0, plNotVoHinh.size() - 1));
        }
        return null;
    }

    public void load_Me_To_Another(Player player) { //load thông tin người chơi cho những người chơi khác
        try {
            if (player.zone != null) {
                if (MapService.gI().isMapOffline(this.map.mapId)) {
                    //Load boss
                    if (player instanceof TrainingBoss || player instanceof NonInteractiveNPC) {
                        for (int i = players.size() - 1; i >= 0; i--) {
                            Player pl = players.get(i);
                            if (!player.equals(pl) && (player instanceof NonInteractiveNPC
                                    || player instanceof TrainingBoss && ((TrainingBoss) player).playerAtt.equals(pl))) {
                                infoPlayer(pl, player);
                            }
                        }
                    }
                } else {
                    // Dựng gói -5 MỘT lần rồi gửi cho tất cả. Bản cũ gọi infoPlayer cho
                    // từng người nhận nên dựng lại y hệt N lần, mỗi lần tính lại
                    // getAura/getEffFront/getMount/getCurrLevel... của cùng một người.
                    // Chỉ mã cờ 83 mới khác nhau giữa client cũ/mới nên tối đa 2 bản.
                    Message goiThuong = dungGoiInfo(player, false);
                    Message goiBanMoi = (player.getFlagBag() == 83)
                            ? dungGoiInfo(player, true) : goiThuong;
                    for (int i = players.size() - 1; i >= 0; i--) {
                        Player pl = players.get(i);
                        if (pl == null || player.equals(pl)) {
                            continue;
                        }
                        Message goi = laBanMoi(pl) ? goiBanMoi : goiThuong;
                        if (goi != null) {
                            pl.sendMessage(goi);
                        }
                        guiKemInfo(pl, player, null);
                    }
                    if (goiThuong != null) {
                        goiThuong.cleanup();
                    }
                    if (goiBanMoi != null && goiBanMoi != goiThuong) {
                        goiBanMoi.cleanup();
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
    }

    public void load_Another_To_Me(Player player) { //load những player trong map và gửi cho player vào map
        try {
            // Gói cờ (-103) chỉ chứa "mã cờ -> icon", KHÔNG có id người chơi. Gửi kèm
            // từng nhân vật là lặp lại y hệt N lần; khu đông thì riêng nó đã bằng nửa
            // số gói lúc vào map, máy yếu nhận không xuể. Gom lại gửi mỗi loại 1 lần.
            java.util.Set<Byte> coDaGui = new java.util.HashSet<>();
            if (MapService.gI().isMapOffline(this.map.mapId)) {
                //Load boss
                for (int i = this.humanoids.size() - 1; i >= 0; i--) {
                    Player pl = this.humanoids.get(i);
                    if (pl != null && (pl instanceof NonInteractiveNPC
                            || pl instanceof TrainingBoss && ((TrainingBoss) pl).playerAtt.equals(player))) {
                        infoPlayer(player, pl, coDaGui);
                    }
                }
            } else {
                for (int i = this.humanoids.size() - 1; i >= 0; i--) {
                    Player pl = this.humanoids.get(i);
                    if (pl != null && !player.equals(pl)) {
                        infoPlayer(player, pl, coDaGui);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
    }

    public void loadBoss(Boss boss) {
        try {
            if (MapService.gI().isMapOffline(this.map.mapId)) {
                //Load boss
                for (Player pl : this.bosses) {
                    if (!boss.equals(pl) && !pl.isPl() && !pl.isPet && !pl.isNewPet && !pl.isBot) {
                        infoPlayer(boss, pl);
                        infoPlayer(pl, boss);
                    }
                }
            } else {
                for (Player pl : this.bosses) {
                    if (!boss.equals(pl)) {
                        infoPlayer(boss, pl);
                        infoPlayer(pl, boss);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
    }

    private void infoPlayer(Player plReceive, Player plInfo) {
        infoPlayer(plReceive, plInfo, null);
    }

    /**
     * @param coDaGui tập mã cờ đã gửi cho {@code plReceive} trong lượt nạp này; khác
     *                null thì gói cờ trùng sẽ được bỏ qua. Null = gửi như cũ.
     */
    private void infoPlayer(Player plReceive, Player plInfo, java.util.Set<Byte> coDaGui) {
        Message msg = dungGoiInfo(plInfo, laBanMoi(plReceive));
        if (msg != null) {
            plReceive.sendMessage(msg);
            msg.cleanup();
        }
        guiKemInfo(plReceive, plInfo, coDaGui);
    }

    /** Client bản 228 trở lên đổi mã cờ 83 thành 250. */
    private static boolean laBanMoi(Player plReceive) {
        return plReceive != null && plReceive.isPl() && plReceive.getSession() != null
                && plReceive.getSession().version >= 228;
    }

    /**
     * Dựng gói -5 mô tả một nhân vật. Tách riêng để {@link #load_Me_To_Another} dựng
     * MỘT lần rồi gửi cho cả khu — trước đây mỗi người nhận lại dựng lại từ đầu, kéo
     * theo tính lại getAura/getEffFront/getMount... của cùng một người N lần.
     *
     * @param banMoi client >= 228 (chỉ ảnh hưởng mã cờ 83)
     */
    private Message dungGoiInfo(Player plInfo, boolean banMoi) {
        Message msg;
        try {
            msg = new Message(-5);
            msg.writer().writeInt((int) plInfo.id);
            if (plInfo.clan != null) {
                msg.writer().writeInt(plInfo.clan.id);
            } else if (plInfo.isBoss && (plInfo.id == BossID.MABU || plInfo.id == BossID.SUPERBU)) {
                msg.writer().writeInt(-100);
            } else if (plInfo.isCopy) {
                msg.writer().writeInt(-2);
            } else {
                msg.writer().writeInt(-1);
            }
            msg.writer().writeByte(Service.gI().getCurrLevel(plInfo));
            msg.writer().writeBoolean(false);
            msg.writer().writeByte(plInfo.typePk);
            msg.writer().writeByte(plInfo.gender);
            msg.writer().writeByte(plInfo.gender);
            msg.writer().writeShort(plInfo.getHead());
            msg.writer().writeUTF(Service.gI().name(plInfo));
            msg.writeFix(plInfo.nPoint != null ?  plInfo.nPoint.hp  : 100);
            msg.writeFix(plInfo.nPoint != null ? plInfo.nPoint.hpMax  : 100);
            msg.writer().writeShort(plInfo.getBody());
            msg.writer().writeShort(plInfo.getLeg());
            int flagbag = plInfo.getFlagBag();
            if (banMoi && flagbag == 83) {
                flagbag = 250;
            }
            msg.writer().writeByte(flagbag); //bag
            msg.writer().writeByte(-1);
            if (plInfo.location != null) {
                msg.writer().writeShort(plInfo.location.x);
                msg.writer().writeShort(plInfo.location.y);
            }
            msg.writer().writeShort(0); // effbuffhp
            msg.writer().writeShort(0); // effbuffmp

            msg.writer().writeByte(0); // num eff

            if (plInfo.idMark != null) {
                msg.writer().writeByte(plInfo.idMark.getIdSpaceShip());
            }

            msg.writer().writeByte(plInfo.effectSkill != null && plInfo.effectSkill.isMonkey ? 1 : 0);
            msg.writer().writeShort(plInfo.getMount());
            msg.writer().writeByte(plInfo.cFlag);

            msg.writer().writeByte(0);
            msg.writer().writeShort(plInfo.getAura()); //idauraeff
            msg.writer().writeByte(plInfo.getEffFront()); //seteff
            msg.writer().writeShort(plInfo.getHat()); //id hat
            return msg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Các gói phụ đi kèm gói -5: cờ, chibi, vệt chân, và trạng thái chết. */
    private void guiKemInfo(Player plReceive, Player plInfo, java.util.Set<Byte> coDaGui) {
        Message msg;
        if (coDaGui == null || coDaGui.add(plInfo.cFlag)) {
            Service.gI().sendFlagPlayerToMe(plReceive, plInfo);
        }
        try {
            if (plInfo.isPl()) {
                if (plInfo.effectSkill != null && plInfo.effectSkill.isChibi) {
                    Service.gI().sendChibiFollowToMe(plReceive, plInfo);
                } else {
//                    Service.gI().sendPetFollowToMe(plReceive, plInfo);
                }
                if (plInfo.inventory.itemsBody.get(12).isNotNullItem()) {
                    Service.gI().sendFootRv(plInfo, plReceive, (short) plInfo.inventory.itemsBody.get(12).template.id);
                }
            }
        } catch (Exception e) {
        }

        try {
            if (plInfo.isDie()) {
                msg = new Message(-8);
                msg.writer().writeInt((int) plInfo.id);
                msg.writer().writeByte(0);
                msg.writer().writeShort(plInfo.location.x);
                msg.writer().writeShort(plInfo.location.y);
                plReceive.sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {

        }
    }

    public void mapInfo(Player pl) {
        Message msg;
        try {
            msg = new Message(-24);
            msg.writer().writeByte(this.map.mapId);
            msg.writer().writeByte(this.map.planetId);
            msg.writer().writeByte(this.map.tileId);
            msg.writer().writeByte(this.map.bgId);
            msg.writer().writeByte(this.map.type);
            msg.writer().writeUTF(this.map.mapName);
            msg.writer().writeByte(this.zoneId);

            msg.writer().writeShort(pl.location.x);
            msg.writer().writeShort(pl.location.y);

            // waypoint
            try {
                List<WayPoint> wayPoints = this.map.wayPoints;
                msg.writer().writeByte(wayPoints.size());
                for (WayPoint wp : wayPoints) {
                    msg.writer().writeShort(wp.minX);
                    msg.writer().writeShort(wp.minY);
                    msg.writer().writeShort(wp.maxX);
                    msg.writer().writeShort(wp.maxY);
                    msg.writer().writeBoolean(wp.isEnter);
                    msg.writer().writeBoolean(wp.isOffline);
                    msg.writer().writeUTF(wp.name);
                }
            } catch (Exception e) {
                msg.writer().writeByte(0);
            }

            // mob
            try {
                // Tự động kiểm tra và hồi sinh ngay các quái đã hết thời gian chết (5s)
                // trước khi ghi gói mapInfo ở các map thường (tránh tình trạng quái bị ẩn khi vừa vào map).
                // Riêng các phó bản (Doanh Trại, BĐKB, CDRD, Khí Gas, Halloween 181, Tây Karin),
                // quái chết KHÔNG được tự hồi sinh để tránh làm hỏng tiến trình dọn map của người chơi.
                boolean isDungeon = services.map.MapService.gI().isMapPhoBan(this.map.mapId)
                        || this.map.mapId == 181
                        || this.map.type == consts.ConstMap.MAP_DOANH_TRAI
                        || this.map.type == consts.ConstMap.MAP_BAN_DO_KHO_BAU
                        || this.map.type == consts.ConstMap.MAP_CON_DUONG_RAN_DOC
                        || this.map.type == consts.ConstMap.MAP_KHI_GAS_HUY_DIET
                        || this.map.type == consts.ConstMap.MAP_TAY_KARIN;
                if (!isDungeon) {
                    for (Mob mob : this.mobs) {
                        if (mob == null) {
                            continue;
                        }
                        if ((mob.isDie() || mob.status == 0) && !mob.isBigBoss()) {
                            if (Util.canDoWithTime(mob.lastTimeDie, 5000)) {
                                mob.hoiSinh();
                            }
                        }
                        if (!mob.isDie() && mob.status == 0) {
                            mob.status = 5;
                        }
                    }
                }

                List<Mob> mobs = new ArrayList<>(this.mobs);
                if (mobs.size() > 255) {
                    mobs = mobs.subList(0, 255);
                }
                msg.writer().writeByte(mobs.size());
                for (Mob mob : mobs) {
                    msg.writer().writeBoolean(false); //is disable
                    msg.writer().writeBoolean(false); //is dont move
                    msg.writer().writeBoolean(false); //is fire
                    msg.writer().writeBoolean(false); //is ice
                    msg.writer().writeBoolean(false); //is wind
                    msg.writer().writeByte(mob.tempId);
                    msg.writer().writeByte(0); // sys
                    msg.writer().writeLong(mob.point.gethp());
                    msg.writer().writeByte(mob.level);
                    msg.writer().writeLong((mob.point.getHpFull()));
                    msg.writer().writeShort(mob.location.x);
                    msg.writer().writeShort(mob.location.y);
                    msg.writer().writeByte(mob.status);
                    msg.writer().writeByte(mob.lvMob);
                    msg.writer().writeBoolean(mob.tempId == ConstMob.GAU_TUONG_CUOP || mob.tempId >= ConstMob.VOI_CHIN_NGA && mob.tempId <= ConstMob.PIANO); //is bigboss
                }
            } catch (Exception e) {
                Logger.logException(Zone.class, e, "Lỗi mapInfo mobs");
                msg.writer().writeByte(0);
            }

            msg.writer().writeByte(0);

            // npc
            try {
                List<Npc> npcs = NpcManager.getNpcsByMapPlayer(pl);
                msg.writer().writeByte(npcs.size());
                for (Npc npc : npcs) {
                    msg.writer().writeByte(npc.status);
                    msg.writer().writeShort(npc.cx);
                    msg.writer().writeShort(npc.cy);
                    msg.writer().writeByte(npc.tempId);
                    msg.writer().writeShort(npc.avartar);
                }
            } catch (Exception e) {
                msg.writer().writeByte(0);
            }

            // item
            try {
                List<ItemMap> itemsMap = this.getItemMapsForPlayer(pl);
                if (itemsMap.size() > 255) {
                    List<ItemMap> uuTien = new ArrayList<>();
                    List<ItemMap> conLai = new ArrayList<>();
                    for (ItemMap it : itemsMap) {
                        if (it.isBlackBall || it.isNamecBall || it.itemTemplate.id == 74
                                || it.itemTemplate.id == 78 || it.itemTemplate.id == 726) {
                            uuTien.add(it);
                        } else {
                            conLai.add(it);
                        }
                    }
                    for (ItemMap it : conLai) {
                        if (uuTien.size() >= 255) {
                            break;
                        }
                        uuTien.add(it);
                    }
                    itemsMap = uuTien.size() > 255 ? uuTien.subList(0, 255) : uuTien;
                }
                msg.writer().writeByte(itemsMap.size());
                for (ItemMap it : itemsMap) {
                    msg.writer().writeShort(it.itemMapId);
                    msg.writer().writeShort(it.itemTemplate.id);
                    msg.writer().writeShort(it.x);
                    msg.writer().writeShort(it.y);
                    msg.writer().writeInt((int) it.playerId);
                    if (it instanceof Satellite satellite) {
                        msg.writer().writeShort((short) satellite.range);
                    }
                }
            } catch (Exception e) {
                msg.writer().writeByte(0);
            }

            // bg item
            try {
                final byte[] bgItem = this.map.getBgItemData();
                if (bgItem != null) {
                    msg.writer().write(bgItem);
                } else {
                    msg.writer().writeShort(0);
                }
            } catch (Exception e) {
                msg.writer().writeShort(0);
            }

            // eff item
//            try {
//                final byte[] effItem = FileIO.readFile("data/map/eff_map/" + this.map.mapId);
//                msg.writer().write(effItem);
//            } catch (Exception e) {
//                msg.writer().writeShort(0);
//            }
            // eff item
            List<EffectMap> em = this.map.effMap;
            msg.writer().writeShort(em.size());
            for (EffectMap e : em) {
                msg.writer().writeUTF(e.getKey());
                msg.writer().writeUTF(e.getValue());
            }
            msg.writer().writeByte(this.map.bgType);
            msg.writer().writeByte(pl.idMark.getIdSpaceShip());
            msg.writer().writeByte(this.map.mapId == 148 ? 1 : 0);
            pl.sendMessage(msg);
            msg.cleanup();

        } catch (Exception e) {
            Logger.logException(Service.class, e);
        }
    }

    public TrapMap isInTrap(Player player) {
        for (TrapMap trap : this.trapMaps) {
            if (player.location.x >= trap.x && player.location.x <= trap.x + trap.w
                    && player.location.y >= trap.y && player.location.y <= trap.y + trap.h) {
                return trap;
            }
        }
        return null;
    }

    public void sendBigBoss(Player player) {
        for (Mob mob : this.mobs) {
            if (!mob.isDie() && mob.tempId == ConstMob.HIRUDEGARN) {
                if (mob.lvMob >= 1) {
                    Service.gI().sendBigBoss2(player, 6, mob);
                }
                if (mob.lvMob >= 2) {
                    Service.gI().sendBigBoss2(player, 5, mob);
                }
                break;
            }
        }
    }

    public MaBuHold getMaBuHold() {
        for (MaBuHold hold : MapService.gI().getMapById(128).zones.get(this.zoneId).maBuHolds) {
            if (hold.player == null) {
                return hold;
            }
        }
        return null;
    }

    public void setMaBuHold(int slot, int zoneId, Player player) {
        MapService.gI().getMapById(128).zones.get(zoneId).maBuHolds.set(slot, new MaBuHold(slot, player));
    }

    public boolean isKhongCoTrongTaiTrongKhu() {
        boolean kovao = true;
        for (Player pl : players) {
            if (!pl.isPl()) {
                kovao = false;
                break;
            }
            if (pl.zone == null || pl.zone.map == null) {
                continue;
            }
            if ((pl.zone.map.mapId >= 21 && pl.zone.map.mapId <= 23)
                    || pl.zone.map.mapId == 170
                    || pl.zone.map.mapId == 153
                    || pl.zone.map.mapId == 183
                    || pl.zone.map.mapId == 113
                    || pl.zone.map.mapId == 129
                    || MapService.gI().isMapDoanhTrai(pl.zone.map.mapId)
                    || MapService.gI().isMapBlackBallWar(pl.zone.map.mapId)
                    || MapService.gI().isMapBanDoKhoBau(pl.zone.map.mapId)
                    || MapService.gI().isMapKhiGasHuyDiet(pl.zone.map.mapId)
                    || MapService.gI().isMapMaBu(pl.zone.map.mapId)
                    || MapService.gI().isMapOffline(pl.zone.map.mapId)) {
                kovao = false;
            }
        }
        return kovao;
    }

}
