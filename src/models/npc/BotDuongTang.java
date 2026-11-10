package models.npc;


import models.map.Zone;
import models.player.Player;
import services.map.ChangeMapService;
import services.player.PlayerService;

public class BotDuongTang extends NonInteractiveNPC {

    /** Skin bot Đường Tăng — chỉnh 3 ID này nếu muốn skin khác. */
    private static final short SKIN_HEAD = 467;
    private static final short SKIN_BODY = 468;
    private static final short SKIN_LEG  = 469;
    /** Flag / cờ hiệu hiển thị cạnh tên bot. */
    private static final short SKIN_FLAG = 4544;

    /** Chỉ move khi cách master > khoảng cách này — nhỏ = bot theo sát hơn. */
    private static final int MOVE_DISTANCE = 15;
    /** Tối thiểu bao lâu giữa 2 lần move (ms) — nhỏ = phản ứng nhanh hơn. */
    private static final int MIN_MOVE_INTERVAL = 250;
    /** Khoảng cách bot đứng cạnh master khi đã tới nơi (nhỏ = dán sát). */
    private static final int STAND_OFFSET = 5;
    /** Bước đi bình thường (đi bộ chậm) và bước catch-up khi player rời xa. */
    private static final int STEP_WALK = 40;
    private static final int STEP_CATCHUP = 80;
    /** Nếu cách xa hơn ngưỡng này thì tăng tốc bắt kịp. */
    private static final int CATCHUP_THRESHOLD = 100;

    public Player master;
    private long lastMoveTime;

    public BotDuongTang(Player master) {
        this.master = master;
        this.name = "Đường Tăng";
        this.gender = 0;
        this.id = -(Math.abs(master.id) * 100 + 49); // id âm, unique per master, tránh đụng player thật
        // nPoint đã được Player() khởi tạo — set chỉ số tối thiểu
        this.nPoint.hpMax = 5000;
        this.nPoint.hpg = 5000;
        this.nPoint.hp = 5000;
        this.nPoint.setFullHpMp();
    }

    @Override public short getHead()    { return SKIN_HEAD; }
    @Override public short getBody()    { return SKIN_BODY; }
    @Override public short getLeg()     { return SKIN_LEG;  }
    @Override public short getFlagBag() { return SKIN_FLAG; }

    /** Tick bởi Zone.udNonInteractiveNPC() mỗi frame. Follow master. */
    @Override
    public void update() {
        try {
            if (master == null || master.zone == null || master.location == null) return;
            // Chốt chặn cuối: master hết phiên hộ tống (hết giờ / huỷ / thoát game / lỗi
            // nuốt exception) mà bot chưa được dọn → tự xoá. Không có chốt này thì bot
            // mồ côi cứ bám master đi khắp map mãi mãi.
            if (!models.npc.npc_list.DuongTang.isDangDatDuong(master.id)) {
                dispose();
                return;
            }
            // Master đổi map/zone → chuyển map cùng
            if (this.zone != master.zone) {
                // Không bao giờ đi vào map nhà (21/22/23) — nơi player hay treo máy.
                if (master.zone.map != null && services.map.MapService.gI().isHome(master.zone.map.mapId)) {
                    dispose();
                    return;
                }
                spawnAt(master.zone);
                return;
            }
            // Cùng zone → move từng bước theo master (đi bộ chậm)
            long now = System.currentTimeMillis();
            if (now - lastMoveTime < MIN_MOVE_INTERVAL) return;
            int dx = master.location.x - this.location.x;
            int dy = master.location.y - this.location.y;
            if (Math.abs(dx) < MOVE_DISTANCE && Math.abs(dy) < MOVE_DISTANCE) return;

            // Đi từng bước ngắn thay vì teleport tới master — chậm rãi, có cảm giác đi bộ.
            // Nếu player rời xa > CATCHUP_THRESHOLD thì tăng tốc để bắt kịp, tránh mất event.
            int step = Math.abs(dx) >= CATCHUP_THRESHOLD ? STEP_CATCHUP : STEP_WALK;
            int newX;
            if (Math.abs(dx) <= step) {
                // Sắp tới nơi — đứng lệch nhẹ cạnh master, đừng vượt
                newX = master.location.x + (dx > 0 ? -STAND_OFFSET : STAND_OFFSET);
            } else {
                newX = this.location.x + (dx > 0 ? step : -step);
            }
            int newY = master.location.y;
            PlayerService.gI().playerMove(this, newX, newY);
            lastMoveTime = now;
        } catch (Exception ignored) {
        }
    }

    /** Đưa bot vào 1 zone (thường là zone của master). */
    public void spawnAt(Zone zone) {
        try {
            if (master != null && master.location != null) {
                this.location.x = master.location.x + 30;
                this.location.y = master.location.y;
            }
            ChangeMapService.gI().goToMap(this, zone);
            if (this.zone != null) {
                this.zone.load_Me_To_Another(this);
            }
        } catch (Exception ignored) {
        }
    }

    /** Xóa bot khỏi world (kết thúc hoặc timeout). */
    public void dispose() {
        try {
            Zone z = this.zone;
            if (z != null) {
                z.removePlayer(this);
                this.zone = null;
                network.io.Message msg = null;
                try {
                    msg = new network.io.Message(-6);
                    msg.writer().writeInt((int) this.id);
                    for (Player pl : z.getPlayers()) {
                        if (pl != null && pl.isPl() && pl.getSession() != null) {
                            pl.sendMessage(msg);
                        }
                    }
                } finally {
                    if (msg != null) {
                        msg.cleanup();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        this.master = null;
    }
}