package Bot;

import models.item.Item;
import utils.Util;


public class BotProgress {

    public static final int HE_SO_UP = 7;

    /** Mốc sức mạnh chuyển giai đoạn. */
    public static final long MOC_CAY_CUOC = 100_000L;
    public static final long MOC_LEN_DO = 50_000_000L;
    public static final long MOC_SAN_BOSS = 1_000_000_000L;
    private static final int CAP_DO_TOI_DA = 8;
    private static final int OPT_CAP_DO = 72;
    private static final int OPT_SAO_PHA_LE = 107;
    private static final int MAX_SAO = 10;

    private static final int[] OPT_EP_SAO = {77, 103, 50, 94, 108, 80, 81};

    private static final long NHIP_MS = 5_000;

    private static final long TRAN_KY_THUAT = Long.MAX_VALUE / 4;

    private static final long TRAN_SUC_MANH = 999_999_999_999_999L;

    private static long cong(long a, long b) {
        long t = a + b;
        return (t < 0 || t > TRAN_KY_THUAT) ? TRAN_KY_THUAT : t;
    }

    private final Bot bot;
    private long lastTick;
    private int oDangDap; // xoay vòng qua các ô đồ cho đều

    public BotProgress(Bot bot) {
        this.bot = bot;
        // Lệch nhịp giữa các bot để không cùng lúc mua/đập hàng loạt
        this.lastTick = System.currentTimeMillis() - Util.nextInt(0, (int) NHIP_MS);
    }

    /** Gọi mỗi nhịp update; bên trong tự giãn nhịp. */
    public void capNhat() {
        if (System.currentTimeMillis() - lastTick < NHIP_MS) {
            return;
        }
        lastTick = System.currentTimeMillis();
        try {
            thuNhap();
            muaDo();
            dapDo();
            epDo();
        } catch (Exception ignored) {
        }
    }

    /** Đủ cứng thì mới đi săn boss, y như người chơi không dám đi boss lúc còn yếu. */
    public boolean duocSanBoss() {
        return bot.nPoint != null && bot.nPoint.power >= MOC_SAN_BOSS;
    }

    public String tenGiaiDoan() {
        long sm = bot.nPoint == null ? 0 : bot.nPoint.power;
        if (sm >= MOC_SAN_BOSS) {
            return "Săn boss";
        }
        if (sm >= MOC_LEN_DO) {
            return "Lên đồ";
        }
        if (sm >= MOC_CAY_CUOC) {
            return "Cày cuốc";
        }
        return "Tân thủ";
    }

    private void thuNhap() {
        if (bot.inventory == null || bot.nPoint == null) {
            return;
        }
        long sm = Math.max(bot.nPoint.power, 1000);
        long them = Math.max(500, sm / 150) * HE_SO_UP;
        if (bot.nPoint.power < TRAN_SUC_MANH) {
            bot.nPoint.power = Math.min(bot.nPoint.power + them, TRAN_SUC_MANH);
            bot.nPoint.tiemNang = cong(bot.nPoint.tiemNang, them);
        }

        // Vàng/ngọc của bot: không áp trần người chơi, chỉ chặn ở mức tràn số
        long vang = Math.max(20_000, Math.min(sm / 10, 20_000_000L));
        bot.inventory.gold = cong(bot.inventory.gold, vang);
        if (Util.isTrue(30, 100)) {
            long themNgoc = 1 + Math.min(sm / 20_000_000L, 50);
            long ngoc = (long) bot.inventory.gem + themNgoc;
            bot.inventory.gem = (int) Math.min(ngoc, Integer.MAX_VALUE); // gem là int
        }
    }

    private void muaDo() {
        if (bot.inventory == null || bot.nPoint == null) {
            return;
        }
        if (bot.nPoint.power < MOC_CAY_CUOC) {
            return; // tân thủ còn nghèo, cày đã
        }
        BotOutfit.moODo(bot);
        for (int type = 0; type <= 4; type++) {
            Item hienTai = bot.inventory.itemsBody.get(type);
            if (hienTai != null && hienTai.isNotNullItem()) {
                continue;
            }
            long gia = giaMon(bot.nPoint.power);
            if (bot.inventory.gold < gia) {
                return;
            }
            if (BotOutfit.muaMon(bot, type, 0)) {
                bot.inventory.gold -= gia;
            }
            return; // mỗi lượt sắm một món cho giống người chơi
        }
        // Đủ đồ rồi thì sắm phụ kiện cho đẹp
        if (bot.nPoint.power >= MOC_LEN_DO) {
            Item caiTrang = bot.inventory.itemsBody.get(5);
            if ((caiTrang == null || !caiTrang.isNotNullItem()) && bot.inventory.gold >= 50_000_000L) {
                if (BotOutfit.muaMon(bot, 5, 0)) {
                    bot.inventory.gold -= 50_000_000L;
                }
            }
        }
    }

    private static long giaMon(long sucManh) {
        return Math.max(100_000L, Math.min(sucManh / 2, 200_000_000L));
    }

    private void dapDo() {
        if (bot.inventory == null || bot.nPoint == null || bot.nPoint.power < MOC_CAY_CUOC) {
            return;
        }
        BotOutfit.moODo(bot);
        // Xoay vòng qua 5 ô trang bị để không dồn hết vào một món
        for (int i = 0; i < 5; i++) {
            int o = (oDangDap + i) % 5;
            Item it = bot.inventory.itemsBody.get(o);
            if (it == null || !it.isNotNullItem()) {
                continue;
            }
            oDangDap = (o + 1) % 5;
            if (nangCapDo(it)) {
                return;
            }
            if (bot.nPoint.power >= MOC_LEN_DO && phaLeHoa(it)) {
                return;
            }
        }
    }

    private boolean nangCapDo(Item it) {
        int cap = it.getOptionParam(OPT_CAP_DO);
        if (cap >= CAP_DO_TOI_DA) {
            return false;
        }
        long gia = 1_000_000L * (cap + 1) * (cap + 1);
        if (bot.inventory.gold < gia) {
            return false;
        }
        bot.inventory.gold -= gia;
        // Càng cao càng khó, trượt thì mất phí y như người chơi
        if (!Util.isTrue(Math.max(10, 90 - cap * 10), 100)) {
            return true;
        }
        it.addOptionParam(OPT_CAP_DO, 1);
        return true;
    }

    private boolean phaLeHoa(Item it) {
        int sao = it.getOptionParam(OPT_SAO_PHA_LE);
        if (sao >= MAX_SAO) {
            return false;
        }
        int gem = (sao + 1) * 5;
        long gia = 5_000_000L * (sao + 1);
        if (bot.inventory.gem < gem || bot.inventory.gold < gia) {
            return false;
        }
        bot.inventory.gem -= gem;
        bot.inventory.gold -= gia;
        if (!Util.isTrue(Math.max(5, 80 - sao * 8), 100)) {
            return true; // trượt
        }
        it.addOptionParam(OPT_SAO_PHA_LE, 1);
        return true;
    }

    private void epDo() {
        if (bot.inventory == null || bot.nPoint == null || bot.nPoint.power < MOC_LEN_DO) {
            return;
        }
        BotOutfit.moODo(bot);
        for (int o = 0; o <= 4; o++) {
            Item it = bot.inventory.itemsBody.get(o);
            if (it == null || !it.isNotNullItem()) {
                continue;
            }
            int sao = it.getOptionParam(OPT_SAO_PHA_LE);
            if (sao <= 0) {
                continue;
            }
            int daEp = demOptionEp(it);
            if (daEp >= sao) {
                continue; // hết lỗ trống
            }
            int gem = 10 * (daEp + 1);
            if (bot.inventory.gem < gem) {
                return;
            }
            bot.inventory.gem -= gem;
            int opt = OPT_EP_SAO[Util.nextInt(0, OPT_EP_SAO.length - 1)];
            it.addOptionParam(opt, Util.nextInt(1, 5));
            return; // mỗi lượt ép một lỗ
        }
    }

    private static int demOptionEp(Item it) {
        int n = 0;
        for (Item.ItemOption io : it.itemOptions) {
            if (io == null || io.optionTemplate == null) {
                continue;
            }
            for (int opt : OPT_EP_SAO) {
                if (io.optionTemplate.id == opt) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }
}
