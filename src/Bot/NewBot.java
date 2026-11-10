package Bot;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import models.Template;
import models.Template.FlagBag;
import server.Manager;


public class NewBot {
    public static NewBot i;
    
    public boolean LOAD_PART = true;
    public int MAXPART = 0;
    /** [head, leg, body, gender, idCaiTrang] — cột 4 để mặc đúng bộ cải trang đang hiển thị. */
    public static int[][] PARTBOT = new int[Manager.ITEM_TEMPLATES.size()][5];
    
    private final String[] FIRST_NAMES = {"an", "bao", "cuong", "duc", "hai", "linh", "minh", "nam", "quoc", "tuan",
            "ngoc", "hoang", "khanh", "trung", "dat", "duy", "hanh", "hung", "son", "trong",
            "phuc", "quang", "thanh", "thien", "trinh", "viet", "binh", "diep", "han", "hieu",
            "huy", "linh", "nguyen", "phong", "phuoc", "quyen", "tam", "thao", "thi", "thu", "van",
            "phuong", "thuy", "thoai", "thong", "trang", "truc", "trinh", "truong", "tuyen", "uy",
            "y", "yen", "yen", "dung", "duong", "hieu", "huy", "huong", "khoa", "lam", "linh",
            "mai", "nhan", "nhi", "phu", "quyen", "thanh", "truc", "trung", "tuan", "tung",
            "tuyen", "van", "vi", "vu", "an", "bich", "binh", "bong", "chi", "chinh", "cho",
            "chung", "du", "duy", "duyen", "giang", "giau", "hong", "huong", "huyen", "khuong", "lai"};

    private final String[] LAST_NAMES = {"nguyen", "tran", "le", "pham", "hoang", "huynh", "phan", "vo", "dang", "bui",
            "vu", "dinh", "do", "dao", "huu", "trinh", "truong", "ngoc", "hoai", "nhat",
            "phong", "phuc", "quyen", "tam", "thao", "thi", "thu", "van", "dung", "duong",
            "hieu", "huy", "huong", "khoa", "lam", "linh", "mai", "nhan", "nhi", "phu",
            "quyen", "thanh", "truc", "trung", "tuan", "tung", "tuyen", "van", "vi", "vu",
            "anh", "bao", "binh", "chinh", "chung", "du", "duy", "duyen", "giang", "giau",
            "hong", "huong", "huyen", "khuong", "lai", "lan", "luan", "nhon", "phieu", "phuoc",
            "quoc", "sang", "sau", "suong", "tam", "thang", "thao", "thien", "thu", "tinh",
            "tung", "tuyen", "uyen", "vinh", "xuan", "yen", "quynh", "anh", "hoai", "minh"};
    
    
    public static NewBot gI(){
         if(i == null){
             i = new NewBot();
         }
         return i;
    }
    
    public void LoadPart(){
        if(LOAD_PART){
            int i = 0;
               for(Template.ItemTemplate it : Manager.ITEM_TEMPLATES){
              if(it.type == 5){
                  if(it.head != -1 && it.leg != -1 && it.body != -1 && it.leg != 194){
                    PARTBOT[i][0] = it.head;
                    PARTBOT[i][1] = it.leg;
                    PARTBOT[i][2] = it.body;
                    PARTBOT[i][3] = it.gender;
                    PARTBOT[i][4] = it.id;
                    i ++;
                    MAXPART ++;
                    }
             }
        }
        LOAD_PART = false;
     }
   }
   
   /** Ký tự hợp lệ cho tên nhân vật trong game. */
   private static final char[] KY_TU = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
   /** Luật đặt tên của game: chỉ a-z0-9, dài 5..10 (xem Input.createFormChangeName). */
   private static final int TEN_MIN = 5;
   private static final int TEN_MAX = 10;
   private static final java.util.concurrent.atomic.AtomicInteger DEM_TEN =
           new java.util.concurrent.atomic.AtomicInteger(1);


   public String Getname(){
     Random rd = new Random();
     for (int lan = 0; lan < 50; lan++) {
         String ten = taoTen(rd);
         if (!trungTen(ten)) {
             return ten;
         }
     }
     // Bí quá thì dùng số tăng dần — chắc chắn không trùng
     String ten = "bot" + DEM_TEN.getAndIncrement();
     return ten.length() > TEN_MAX ? ten.substring(ten.length() - TEN_MAX) : ten;
   }

   private String taoTen(Random rd) {
     int soHauTo = 2 + rd.nextInt(2); // 2-3 ký tự ngẫu nhiên
     String goc = FIRST_NAMES[rd.nextInt(FIRST_NAMES.length)]
             + LAST_NAMES[rd.nextInt(LAST_NAMES.length)];
     int maxGoc = TEN_MAX - soHauTo;
     if (goc.length() > maxGoc) {
         goc = goc.substring(0, maxGoc);
     }
     StringBuilder sb = new StringBuilder(goc);
     for (int i = 0; i < soHauTo; i++) {
         sb.append(KY_TU[rd.nextInt(KY_TU.length)]);
     }
     while (sb.length() < TEN_MIN) {
         sb.append(KY_TU[rd.nextInt(KY_TU.length)]);
     }
     return sb.toString();
   }

   private boolean trungTen(String ten) {
     try {
         return server.Client.gI().getPlayer(ten) != null;
     } catch (Exception e) {
         return false;
     }
   }
   
   public int getIndex(int gender){
        if (MAXPART <= 0) {
            return -1;
        }
        Random rd = new Random();
        for (int tries = 0; tries < 100; tries++) {
            int r = rd.nextInt(MAXPART);
            if (PARTBOT[r][3] == gender) {
                return r;
            }
        }
        for (int r = 0; r < MAXPART; r++) {
            if (PARTBOT[r][3] == gender) {
                return r;
            }
        }
        return rd.nextInt(MAXPART); // không có bộ nào hợp giới tính -> lấy đại
   }
  
   
   public void runBot(int type, int slot) {
       runBot(type, slot, null);
   }

   public void runBot(int type, int slot, BotConfig cfg) {
    LoadPart();
    if (MAXPART <= 0) {
        utils.Logger.warning("[BotAI] Không có trang phục (item type 5) hợp lệ để tạo bot.\n");
        return;
    }
    List<FlagBag> validFlagBags = Manager.gI().FLAGS_BAGS.stream()
        .filter(flag -> flag.id < 79)
        .collect(Collectors.toList());
    if (validFlagBags.isEmpty()) {
        utils.Logger.warning("[BotAI] Không có cờ hợp lệ để tạo bot.\n");
        return;
    }
    boolean tuyChinh = cfg != null && cfg.custom;
    Random rd = new Random();
    final int theHe = BotManager.gI().theHeHienTai();

    for (int i = 0; i < slot; i++) {
        // Admin bấm "Xóa tất cả bot" giữa chừng -> dừng ngay, đừng đẻ tiếp
        if (BotManager.gI().theHeHienTai() != theHe) {
            utils.Logger.warning("[BotAI] Dừng tạo bot vì đã có lệnh xóa toàn bộ.\n");
            return;
        }
        int Gender = (tuyChinh && cfg.gender >= 0 && cfg.gender <= 2)
                ? cfg.gender : rd.nextInt(3);
        int Random1 = getIndex(Gender);
        if (Random1 < 0) {
            return;
        }
        int head = PARTBOT[Random1][0];
        int leg = PARTBOT[Random1][1];
        int body = PARTBOT[Random1][2];
        if (tuyChinh) {
            if (cfg.head >= 0) head = cfg.head;
            if (cfg.body >= 0) body = cfg.body;
            if (cfg.leg >= 0) leg = cfg.leg;
        }

        int flag = validFlagBags.get(rd.nextInt(validFlagBags.size())).id;

        Bot b = new Bot((short) head, (short) body, (short) leg, type, Getname(), (short) flag);
        Sanb bos = new Sanb(b);
        Mobb mo1 = new Mobb(b);
        BotNappa botnappa = new BotNappa(b);
        BotTuonglai bottuonglai = new BotTuonglai(b);
        BotCold1 botcold = new BotCold1(b);
        b.mo1 = mo1;
        b.botnappa = botnappa;
        b.bottuonglai = bottuonglai;
        b.botcold = botcold;
        b.boss = bos;
        b.cfg = cfg;
        b.brain = new BotBrain(b);
        if (cfg != null && cfg.phaPlayer) {

            b.typePk = consts.ConstPlayer.PK_ALL;
        }

        b.gender = (byte) Gender;
        boolean accMoi = !tuyChinh && (cfg == null || cfg.nhuAccMoi);

        try {
            if (accMoi) {

                BotOutfit.moODo(b);
            } else {
                BotOutfit.macDo(b, rd.nextInt(9), PARTBOT[Random1][4]);
            }
        } catch (Exception ignored) {
        }
        b.nPoint.limitPower = 8;
        if (accMoi) {

            b.nPoint.power = 1000 + rd.nextInt(2000);
            b.nPoint.tiemNang = 0;
            b.nPoint.dameg = 100;
            b.nPoint.hpg = 300;
            b.nPoint.hpMax = 300;
            b.nPoint.hp = 300;
            b.nPoint.mpg = 300;
            b.nPoint.mpMax = 300;
            b.nPoint.mp = 300;
            b.nPoint.critg = 0;
            b.nPoint.defg = 0;
            b.inventory.gold = 0;
            b.inventory.gem = 0;
        } else if (tuyChinh) {
            b.nPoint.power = cfg.power;
            b.nPoint.tiemNang = cfg.tiemNang;
            b.nPoint.dameg = cfg.dame;
            b.nPoint.mpg = cfg.mp;
            b.nPoint.mpMax = cfg.mp;
            b.nPoint.mp = cfg.mp;
            b.nPoint.hpg = cfg.hp;
            b.nPoint.hpMax = cfg.hp;
            b.nPoint.hp = cfg.hp;
            b.nPoint.critg = cfg.crit;
            b.nPoint.defg = cfg.def;
        } else {
            int congThem = rd.nextInt(50000000);
            b.nPoint.power = 1000 + congThem;
            b.nPoint.tiemNang = 20000000 + congThem;
            b.nPoint.dameg = 10000000;
            b.nPoint.mpg = 2000000000;
            b.nPoint.mpMax = 2000000000;
            b.nPoint.mp = 2000000000;
            b.nPoint.hpg = 10000000;
            b.nPoint.hpMax = 10000000;
            b.nPoint.hp = 10000000;
            b.nPoint.critg = 10;
            b.nPoint.defg = 10;
        }
        b.nPoint.maxStamina = 20000;
        b.nPoint.stamina = 20000;

        if (tuyChinh && cfg.intrinsicId >= 0) {
            apDungNoiTai(b, cfg.intrinsicId);
        }
        capNhiemVuBanDau(b);

        b.leakSkill();

        if (b.playerSkill == null || b.playerSkill.skills == null || b.playerSkill.skills.size() < 2) {
            utils.Logger.warning("[BotAI] Bỏ qua 1 bot: không nạp đủ kỹ năng cho hệ " + Gender + ".\n");
            continue;
        }

        b.joinMap();

        if (b.zone == null) {
            utils.Logger.warning("[BotAI] Bỏ qua 1 bot: không tìm được khu trống để vào.\n");
            continue;
        }

        BotManager.gI().bot.add(b);
        // Vào luôn danh sách người chơi online để được đếm/tra cứu như người thật
        server.Client.gI().putBot(b);
    }
}

   private void capNhiemVuBanDau(Bot b) {
       try {
           if (b.playerTask == null) {
               return;
           }
           long sm = b.nPoint != null ? b.nPoint.power : 0;
           int moc;
           if (sm < 10_000L) {
               moc = 0; // acc mới: bắt đầu từ nhiệm vụ đầu tiên
           } else if (sm < 1_000_000L) {
               moc = new Random().nextInt(5);
           } else if (sm < 50_000_000L) {
               moc = 5 + new Random().nextInt(10);
           } else if (sm < 1_000_000_000L) {
               moc = 15 + new Random().nextInt(10);
           } else {
               moc = 25 + new Random().nextInt(10);
           }
           models.task.TaskMain tm = services.TaskService.gI().getTaskMainById(b, moc);
           if (tm != null) {
               tm.index = 0;
               b.playerTask.taskMain = tm;
           }
       } catch (Exception ignored) {
       }
   }

   /** Gắn nội tại theo id; sai id thì bỏ qua chứ không làm hỏng cả lô bot. */
   private void apDungNoiTai(Bot b, int intrinsicId) {
       try {
           // getIntrinsicById đã trả về một bản sao mới nên gán thẳng được
           models.intrinsic.Intrinsic it =
                   services.player.IntrinsicService.gI().getIntrinsicById(intrinsicId);
           if (it != null && b.playerIntrinsic != null) {
               b.playerIntrinsic.intrinsic = it;
           }
       } catch (Exception ignored) {
       }
   }
}