package Bot;

public class BotConfig {

    public static final int DEF_HP = 1_000_000;
    public static final int DEF_DAME = 50_000;
    public static final int DEF_MP = 1_000_000;
    public static final int DEF_POWER = 1_000_000;
    public static final int DEF_TIEMNANG = 0;
    public static final int DEF_CRIT = 10;
    public static final int DEF_DEF = 100;
    public boolean nhuAccMoi = true;

    /** Hành vi — áp cho MỌI bot, không phụ thuộc {@link #custom}. */
    public boolean nuoiDeTu = true;   // tự nhận + nuôi đệ tử như người chơi
    public boolean phaPlayer;         // chủ động đánh người chơi thật gặp trong khu
    public boolean chatTheGioi = true; // thỉnh thoảng chat cho giống người thật

    public boolean custom;
    public int gender;
    public long hp;
    public long mp;
    public long dame;
    public long power;
    public long tiemNang;
    public int crit;
    public int def;
    public int head;
    public int body;
    public int leg;
    public int intrinsicId;

    public BotConfig() {
    }
}
