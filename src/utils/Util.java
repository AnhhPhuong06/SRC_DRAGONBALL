package utils;


import managers.boss.BossManager;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.*;
import models.mob.Mob;
import models.npc.Npc;
import models.player.Player;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.ArrayUtils;
import java.time.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import models.item.Item;
import models.item.Item.ItemOption;
import models.map.ItemMap;
import models.map.Zone;
import services.ItemService;
import services.map.MapService;

public class Util {

    private static final Random rand;
    private static final SimpleDateFormat dateFormat;
    private static SimpleDateFormat dateFormatDay;
    private static final Locale locale = new Locale("vi", "VN");
    private static final NumberFormat num = NumberFormat.getInstance(locale);

    static {
        rand = new Random();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        dateFormatDay = new SimpleDateFormat("yyyy-MM-dd");
        dateFormatDay.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static String format(double power) {
        return num.format(power);
    }

    public static long maxIntValue(double a) {
        if (a > Long.MAX_VALUE) {
            a = Long.MAX_VALUE;
        }
        return (long) a;
    }

    public static String toDateString(Date date) {
        try {
            return Util.dateFormat.format(date);
        } catch (Exception e) {
            Date now = new Date();
            return dateFormat.format(now);
        }
    }
    public static Item hopquachinchu(int tempId) {
        Item trugnthu = ItemService.gI().createNewItem((short) tempId);
        if (trugnthu != null) {
            if (Arrays.asList(679, 678, 677, 676, 724, 675, 464, 583, 582, 581, 580, 425).contains(trugnthu.template.id)) {
                trugnthu.itemOptions.add(new ItemOption(50, Util.nextInt(20, 34)));
                trugnthu.itemOptions.add(new ItemOption(77, Util.nextInt(20, 34)));
                trugnthu.itemOptions.add(new ItemOption(103, Util.nextInt(20, 34)));
                trugnthu.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(99, 100)) {
                    trugnthu.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
            } else if (trugnthu.template.id == 1553) { // Xử lý đặc biệt cho ID 1553
                trugnthu.itemOptions.add(new ItemOption(50, 24));
                trugnthu.itemOptions.add(new ItemOption(77, 23));
                trugnthu.itemOptions.add(new ItemOption(103, 23));
                trugnthu.itemOptions.add(new ItemOption(5, 12));
                trugnthu.itemOptions.add(new ItemOption(95, 10));
                trugnthu.itemOptions.add(new ItemOption(101, 30));
                trugnthu.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(99, 100)) {
                    trugnthu.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
            } else {
                trugnthu.itemOptions.add(new ItemOption(30, 0));
            }
        }
        return trugnthu;
    }

    public static synchronized boolean compareDay(Date now, Date when) {
        if (now == null || when == null) {
            // Handle the case where one or both dates are null
            //     System.out.println("Error: One or both of the dates are null.");
            return false;
        }

        try {
            Date date1 = Util.dateFormatDay.parse(Util.dateFormatDay.format(now));
            Date date2 = Util.dateFormatDay.parse(Util.dateFormatDay.format(when));
            return !date1.equals(date2) && !date1.before(date2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static double myGetDistcance(int x1, int y1, int x2, int y2) {
        int deltaX = x2 - x1;
        int deltaY = y2 - y1;
        return Math.abs(Math.sqrt(deltaX * deltaX + deltaY * deltaY));
    }

    public static String mumber(long number) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        return decimalFormat.format(number).replace(',', '.');
    }

    public static Item sendDo(int itemId, int sql, List<Item.ItemOption> ios) {
        Item item = ItemService.gI().createNewItem((short) itemId);
        item.itemOptions.addAll(ios);
        item.itemOptions.add(new Item.ItemOption(107, sql));
        return item;
    }

    public static int createIdBossClone(int idPlayer) {
        return -idPlayer - 1_000_000_000;
    }

    public static Zone randomAllMap() {
        int mapId = Util.nextInt(0, 20);
        while (mapId == 5) {
            mapId = Util.nextInt(0, 20);
        }
        Zone zoneJoin = MapService.gI().getMapWithRandZone(mapId);
        int tries = 0;
        while ((zoneJoin == null || zoneJoin.zoneId == 0 || zoneJoin.map.mapId == 5) && tries < 50) {
            try {
                mapId = Util.nextInt(0, 20);
                while (mapId == 5) {
                    mapId = Util.nextInt(0, 20);
                }
                zoneJoin = MapService.gI().getMapWithRandZone(mapId);
            } catch (Exception e) {
            }
            tries++;
        }
        return zoneJoin;
    }

    public static int highlightsItem(boolean highlights, int value) {
        double highlightsNumber = 1.1;
        return highlights ? (int) (value * highlightsNumber) : value;
    }

    public static String strSQL(final String str) {
        return str.replaceAll("['\"\\\\%]", "\\\\$0");
    }

    public static boolean contains(String[] arr, String key) {
        return Arrays.toString(arr).contains(key);
    }

    public static int[] pickNRandInArr(int[] array, int n) {
        List<Integer> list = new ArrayList<>(array.length);
        for (int i : array) {
            list.add(i);
        }
        Collections.shuffle(list);
        int[] answer = new int[n];
        for (int i = 0; i < n; i++) {
            answer[i] = list.get(i);
        }
        Arrays.sort(answer);
        return answer;
    }

    public static ItemMap ratiItemkaio(Zone zone, int tempId, int quantity, int x, int y, long playerId) {
        ItemMap it = new ItemMap(zone, tempId, quantity, x, zone.map.yPhysicInTop(x, y - 24), playerId);
        List<Integer> ao = Arrays.asList(232, 236, 240);
        List<Integer> quan = Arrays.asList(244, 248, 252);
        List<Integer> gang = Arrays.asList(256, 260, 264);
        List<Integer> giay = Arrays.asList(268, 272, 276);
        int ntl = 280;
        if (ao.contains(tempId)) {
            it.options.add(new ItemOption(47, highlightsItem(it.itemTemplate.gender == 2, Util.nextInt(330, 400))));
        }
        if (quan.contains(tempId)) {
            it.options.add(new ItemOption(6, highlightsItem(it.itemTemplate.gender == 0, Util.nextInt(20000, 30000))));
        }
        if (gang.contains(tempId)) {
            it.options.add(new ItemOption(0, highlightsItem(it.itemTemplate.gender == 2, Util.nextInt(1550, 1700))));
        }
        if (giay.contains(tempId)) {
            it.options.add(new ItemOption(7, highlightsItem(it.itemTemplate.gender == 1, Util.nextInt(20000, 30000))));
        }
        if (ntl == tempId) {
            it.options.add(new ItemOption(14, Util.nextInt(10, 12)));
        }
        it.options.add(new ItemOption(86, 0));
        it.options.add(new ItemOption(107, new Random().nextInt(2) + 0));
        it.options.add(new ItemOption(190,1));
        return it;
    }

    public static ItemMap ratiDTL(Zone zone, int tempId, int quantity, int x, int y, long playerId) {
    ItemMap it = new ItemMap(zone, tempId, quantity, x, zone.map.yPhysicInTop(x, y - 24), playerId);

    List<Integer> ao = Arrays.asList(555, 557, 559);
    List<Integer> quan = Arrays.asList(556, 558, 560);
    List<Integer> gang = Arrays.asList(562, 564, 566);
    List<Integer> giay = Arrays.asList(563, 565, 567);
    int ntl = 561;

    if (ao.contains(tempId)) {
        it.options.add(new ItemOption(47, highlightsItem(it.itemTemplate.gender == 2, new Random().nextInt(501) + 1300)));
    }
    if (quan.contains(tempId)) {
        it.options.add(new ItemOption(22, highlightsItem(it.itemTemplate.gender == 0, new Random().nextInt(11) + 45)));
    }
    if (gang.contains(tempId)) {
        it.options.add(new ItemOption(0, highlightsItem(it.itemTemplate.gender == 2, new Random().nextInt(1001) + 3500)));
    }
    if (giay.contains(tempId)) {
        it.options.add(new ItemOption(23, highlightsItem(it.itemTemplate.gender == 1, new Random().nextInt(11) + 35)));
    }
    //Tỉ lệ rơi cho NTL: 2/10
    if (ntl == tempId && Util.isTrue(20, 100)) {
        it.options.add(new ItemOption(14, new Random().nextInt(2) + 15));
    }
    it.options.add(new ItemOption(209, 0)); // đồ rơi từ boss
    it.options.add(new ItemOption(87, 0)); // đồ rơi từ boss
    it.options.add(new ItemOption(21, 18)); // ycsm 18 tỉ

    if (Util.isTrue(30, 100)) { // tỉ lệ ra spl
        it.options.add(new ItemOption(107, 1));
    } else if (Util.isTrue(4, 100)) {
        it.options.add(new ItemOption(107, new Random().nextInt(4)));
    } else {
        it.options.add(new ItemOption(107, new Random().nextInt(3)));
    }

    it.options.add(new ItemOption(190, 1));
    return it;
}
    public static ItemMap ratiDHD(Zone zone, int tempId, int quantity, int x, int y, long playerId) {
        ItemMap it = new ItemMap(zone, tempId, quantity, x, zone.map.yPhysicInTop(x, y - 24), playerId);
        List<Integer> ao = Arrays.asList(650, 652, 654);
        List<Integer> quan = Arrays.asList(651, 653, 655);
        List<Integer> gang = Arrays.asList(657, 659, 661);
        List<Integer> giay = Arrays.asList(658, 660, 662);
        int nhd = 656;
        if (ao.contains(tempId)) {
            it.options.add(new ItemOption(47, highlightsItem(it.itemTemplate.gender == 2, new Random().nextInt(501) + 1400)));
        }
        if (quan.contains(tempId)) {
            it.options.add(new ItemOption(22, highlightsItem(it.itemTemplate.gender == 0, new Random().nextInt(11) + 50)));
        }
        if (gang.contains(tempId)) {
            it.options.add(new ItemOption(0, highlightsItem(it.itemTemplate.gender == 2, new Random().nextInt(1001) + 3600)));
        }
        if (giay.contains(tempId)) {
            it.options.add(new ItemOption(23, highlightsItem(it.itemTemplate.gender == 1, new Random().nextInt(11) + 40)));
        }
        if (nhd == tempId) {
            it.options.add(new ItemOption(14, new Random().nextInt(2) + 17));
        }
        it.options.add(new ItemOption(209, 1)); // đồ rơi từ boss
        it.options.add(new ItemOption(21, 18)); // ycsm 18 tỉ
        it.options.add(new ItemOption(87, 0)); // ko thể gd
        if (Util.isTrue(30, 100)) {// tỉ lệ ra spl
            it.options.add(new ItemOption(107, 1));
        } else if (Util.isTrue(4, 100)) {
            it.options.add(new ItemOption(107, new Random().nextInt(4)));
        } else {
            it.options.add(new ItemOption(107, new Random().nextInt(3)));
        }
        return it;
    }

    public static ItemMap ratiItemluonglong(Zone zone, int tempId, int quantity, int x, int y, long playerId) {
        ItemMap it = new ItemMap(zone, tempId, quantity, x, zone.map.yPhysicInTop(x, y - 24), playerId);
        List<Integer> ao = Arrays.asList(233, 237, 241);
        List<Integer> quan = Arrays.asList(245, 249, 253);
        List<Integer> gang = Arrays.asList(257, 261, 265);
        List<Integer> giay = Arrays.asList(269, 273, 277);
        int ntl = 281;
        if (ao.contains(tempId)) {
            it.options.add(new ItemOption(47, highlightsItem(it.itemTemplate.gender == 2, Util.nextInt(450, 500))));
        }
        if (quan.contains(tempId)) {
            it.options.add(new ItemOption(6, highlightsItem(it.itemTemplate.gender == 0, Util.nextInt(24000, 30000))));
        }
        if (gang.contains(tempId)) {
            it.options.add(new ItemOption(0, highlightsItem(it.itemTemplate.gender == 2, Util.nextInt(2250, 2400))));
        }
        if (giay.contains(tempId)) {
            it.options.add(new ItemOption(7, highlightsItem(it.itemTemplate.gender == 1, Util.nextInt(24000, 30000))));
        }
        if (ntl == tempId) {
            it.options.add(new ItemOption(14, Util.nextInt(11, 13)));
        }
        it.options.add(new ItemOption(86, 0));
        it.options.add(new ItemOption(107, new Random().nextInt(2) + 3));
        it.options.add(new ItemOption(190,2));
        return it;
    }

    public static String msToTime(long ms) {
        if (ms <= 0) {
            return "0s";
        }
        long day = ms / (1000 * 60 * 60 * 24);
        ms = ms % (1000 * 60 * 60 * 24);
        long hour = ms / (1000 * 60 * 60);
        ms = ms % (1000 * 60 * 60);
        long min = ms / (1000 * 60);
        ms = ms % (1000 * 60);
        long sec = ms / 1000;
        StringBuilder time = new StringBuilder();
        if (day > 0) {
            time.append(day).append(" ngày, ");
        }
        if (hour > 0) {
            time.append(hour).append(" giờ, ");
        }
        if (min > 0) {
            time.append(min).append(" phút, ");
        }
        if (sec > 0) {
            time.append(sec).append(" giây");
        }
        String timeStr = time.toString();
        if (timeStr.endsWith(", ")) {
            timeStr = timeStr.substring(0, timeStr.length() - 2);
        }
        return timeStr.isEmpty() ? "0s" : timeStr;
    }

    public static String msToTimeNdung(long ms) {
        ms = ms - System.currentTimeMillis();
        if (ms < 0) {
            ms = 0;
        }
        long sec;
        long min;
        long hour;
        long hour_in_day;
        long day;
        sec = ms / 1000;
        min = (sec / 60);
        sec = sec % 60;
        hour = (min / 60);
        hour_in_day = hour % 24;
        min = min % 60;
        day = hour / 24;
        if (day != 0) {
            return String.valueOf(day) + " ngày, " + String.valueOf(hour_in_day) + " giờ, " + String.valueOf(min) + " phút, " + String.valueOf(sec) + " giây";
        } else if (hour != 0) {
            return String.valueOf(hour) + " giờ, " + String.valueOf(min) + " phút, " + String.valueOf(sec) + " giây";
        } else if (min != 0) {
            return String.valueOf(min) + " phút, " + String.valueOf(sec) + " giây";
        } else if (sec != 0) {
            return String.valueOf(sec) + " giây";
        } else {
            return "0s";
        }
    }

    public static String numberToMoney(long power) {
        Locale locale = new Locale("vi", "VN");
        NumberFormat num = NumberFormat.getInstance(locale);
        num.setMaximumFractionDigits(1);
        if (power >= 1000000000) {
            return num.format((double) power / 1000000000) + " Tỷ";
        } else if (power >= 1000000) {
            return num.format((double) power / 1000000) + " Tr";
        } else if (power >= 1000) {
            return num.format((double) power / 1000) + " k";
        } else {
            return num.format(power);
        }
    }

    public static String powerToString(long power) {
        Locale locale = new Locale("vi", "VN");
        NumberFormat num = NumberFormat.getInstance(locale);
        num.setMaximumFractionDigits(1);
        if (power >= 1000000000) {
            return num.format((double) power / 1000000000) + " Tỷ";
        } else if (power >= 1000000) {
            return num.format((double) power / 1000000) + " Tr";
        } else if (power >= 1000) {
            return num.format((double) power / 1000) + " k";
        } else {
            return num.format(power);
        }
    }

    public static int getDistance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static int getDistance(Player pl1, Player pl2) {
        return getDistance(pl1.location.x, pl1.location.y, pl2.location.x, pl2.location.y);
    }

    public static int getDistance(Player pl, Npc npc) {
        return getDistance(pl.location.x, pl.location.y, npc.cx, npc.cy);
    }

    public static int getDistance(Player pl, Mob mob) {
        return getDistance(pl.location.x, pl.location.y, mob.location.x, mob.location.y);
    }

    public static int getDistance(Mob mob1, Mob mob2) {
        return getDistance(mob1.location.x, mob1.location.y, mob2.location.x, mob2.location.y);
    }

    public static int nextInt(int from, int to) {
        if (to <= from) {
            return from;
        }
        return from + rand.nextInt(to - from + 1);
    }
    public static int nextIntSafe(int from, int to) {
    if (to <= from) {
        return from;
    }
    return from + new Random().nextInt(to - from);
}


    public static int nextInt(int max) {
        if (max <= 0) {
            return 0;
        }
        return rand.nextInt(max);
    }

    public static long nextLong(long from, long to) {
        return from + rand.nextLong(to - from + 1);
    }

    public static long nextLong(long max) {
        return rand.nextLong(max);
    }

    public static int nextInt(int[] percen) {
        int next = nextInt(1000), i;
        for (i = 0; i < percen.length; i++) {
            if (next < percen[i]) {
                return i;
            }
            next -= percen[i];
        }
        return i;
    }

    public static int getOne(int n1, int n2) {
        return rand.nextInt() % 2 == 0 ? n1 : n2;
    }

    public static String replace(String text, String regex, String replacement) {
        return text.replace(regex, replacement);
    }

    public static boolean isTrue(long ratioPercentage, long totalPercentage) {
        long num = Util.nextLong(totalPercentage);
        return num < ratioPercentage;
    }

    public static boolean isTrue(float ratioPercentage, long totalPercentage) {
        if (ratioPercentage < 1) {
            ratioPercentage *= 100;
            totalPercentage *= 100;
        }
        return isTrue((long) ratioPercentage, totalPercentage);
    }

    public static boolean isTrue(long ratioPercentage, long totalPercentage, int accuracy) {
        return Util.nextLong(totalPercentage * accuracy) < ratioPercentage && Util.nextInt(accuracy) == 0;
    }

    public static boolean isTrue(float ratioPercentage, long totalPercentage, int accuracy) {
        if (ratioPercentage < 1) {
            ratioPercentage *= 100;
            totalPercentage *= 100;
        }
        return isTrue((long) ratioPercentage, totalPercentage, accuracy);
    }

    public static boolean haveSpecialCharacter(String text) {
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        boolean b = m.find();
        return b || text.contains(" ");
    }

    public static boolean canDoWithTime(long lastTime, long miniTimeTarget) {
        return System.currentTimeMillis() - lastTime > miniTimeTarget;
    }

    private static final char[] SOURCE_CHARACTERS = {'À', 'Á', 'Â', 'Ã', 'È', 'É',
        'Ê', 'Ì', 'Í', 'Ò', 'Ó', 'Ô', 'Õ', 'Ù', 'Ú', 'Ý', 'à', 'á', 'â',
        'ã', 'è', 'é', 'ê', 'ì', 'í', 'ò', 'ó', 'ô', 'õ', 'ù', 'ú', 'ý',
        'Ă', 'ă', 'Đ', 'đ', 'Ĩ', 'ĩ', 'Ũ', 'ũ', 'Ơ', 'ơ', 'Ư', 'ư', 'Ạ',
        'ạ', 'Ả', 'ả', 'Ấ', 'ấ', 'Ầ', 'ầ', 'Ẩ', 'ẩ', 'Ẫ', 'ẫ', 'Ậ', 'ậ',
        'Ắ', 'ắ', 'Ằ', 'ằ', 'Ẳ', 'ẳ', 'Ẵ', 'ẵ', 'Ặ', 'ặ', 'Ẹ', 'ẹ', 'Ẻ',
        'ẻ', 'Ẽ', 'ẽ', 'Ế', 'ế', 'Ề', 'ề', 'Ể', 'ể', 'Ễ', 'ễ', 'Ệ', 'ệ',
        'Ỉ', 'ỉ', 'Ị', 'ị', 'Ọ', 'ọ', 'Ỏ', 'ỏ', 'Ố', 'ố', 'Ồ', 'ồ', 'Ổ',
        'ổ', 'Ỗ', 'ỗ', 'Ộ', 'ộ', 'Ớ', 'ớ', 'Ờ', 'ờ', 'Ở', 'ở', 'Ỡ', 'ỡ',
        'Ợ', 'ợ', 'Ụ', 'ụ', 'Ủ', 'ủ', 'Ứ', 'ứ', 'Ừ', 'ừ', 'Ử', 'ử', 'Ữ',
        'ữ', 'Ự', 'ự',};

    private static final char[] DESTINATION_CHARACTERS = {'A', 'A', 'A', 'A', 'E',
        'E', 'E', 'I', 'I', 'O', 'O', 'O', 'O', 'U', 'U', 'Y', 'a', 'a',
        'a', 'a', 'e', 'e', 'e', 'i', 'i', 'o', 'o', 'o', 'o', 'u', 'u',
        'y', 'A', 'a', 'D', 'd', 'I', 'i', 'U', 'u', 'O', 'o', 'U', 'u',
        'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A',
        'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'E', 'e',
        'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'E',
        'e', 'I', 'i', 'I', 'i', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o',
        'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'O',
        'o', 'O', 'o', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u',
        'U', 'u', 'U', 'u',};

    public static char removeAccent(char ch) {
        int index = Arrays.binarySearch(SOURCE_CHARACTERS, ch);
        if (index >= 0) {
            ch = DESTINATION_CHARACTERS[index];
        }
        return ch;
    }

    public static String removeAccent(String str) {
        StringBuilder sb = new StringBuilder(str);
        for (int i = 0; i < sb.length(); i++) {
            sb.setCharAt(i, removeAccent(sb.charAt(i)));
        }
        return sb.toString();
    }

    public static Object[] addArray(Object[]... arrays) {
        if (arrays == null || arrays.length == 0) {
            return null;
        }
        if (arrays.length == 1) {
            return arrays[0];
        }
        Object[] arr0 = arrays[0];
        for (int i = 1; i < arrays.length; i++) {
            arr0 = ArrayUtils.addAll(arr0, arrays[i]);
        }
        return arr0;
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
            return null;
        }
    }

    public static int randomBossId() {
        int bossId = Util.nextInt(-1000000, -100000);
        while (BossManager.gI().getBossById(bossId) != null) {
            bossId = Util.nextInt(-1000000, 100000);
        }
        return bossId;
    }

    public static boolean isAfterMidnight(long currenttimemillis) {
        Instant instant = Instant.ofEpochMilli(currenttimemillis);
        ZoneId zoneId = TimeUtil.ZONE_VN;
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);
        LocalDate otherDate = zonedDateTime.toLocalDate();
        LocalDate currentDate = LocalDate.now(zoneId);
        return currentDate.isAfter(otherDate);
    }

    public static boolean isAfterReset3AM(long currenttimemillis) {
        if (currenttimemillis <= 0) {
            return false;
        }
        ZoneId zoneId = TimeUtil.ZONE_VN;
        long shift = 3L * 60 * 60 * 1000; // Lùi 3 tiếng để mốc 3h sáng tương đương mốc 0h (midnight)
        LocalDate lastResetDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currenttimemillis - shift), zoneId).toLocalDate();
        LocalDate currentResetDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() - shift), zoneId).toLocalDate();
        return currentResetDate.isAfter(lastResetDate);
    }

    public static boolean isTimeDifferenceGreaterThanNDays(long setTime, int nDays) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - setTime;
        long daysDifference = timeDifference / 86400000;
        return daysDifference >= nDays;
    }

    public static String formatNumber(long j) {
        long j2 = (j / 1000) + 1;
        String str = "";
        int i = 0;
        while (((long) i) < j2) {
            if (j >= 1000) {
                long j3 = j % 1000;
                if (j3 == 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(".000");
                    stringBuilder.append(str);
                    str = stringBuilder.toString();
                } else {
                    StringBuilder stringBuilder2;
                    String str2;
                    if (j3 < 10) {
                        stringBuilder2 = new StringBuilder();
                        str2 = ".00";
                    } else if (j3 < 100) {
                        stringBuilder2 = new StringBuilder();
                        str2 = ".0";
                    } else {
                        stringBuilder2 = new StringBuilder();
                        str2 = ".";
                    }
                    stringBuilder2.append(str2);
                    stringBuilder2.append(j3);
                    stringBuilder2.append(str);
                    str = stringBuilder2.toString();
                }
                j /= 1000;
                i++;
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(j);
                stringBuilder3.append(str);
                return stringBuilder3.toString();
            }
        }
        return str;
    }

    public static void threadPool(Runnable task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                Logger.error(e + "\n");
            } finally {
                executor.shutdown();
            }
        });
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return bytes / 1024 + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return bytes / (1024 * 1024) + " MB";
        } else {
            return bytes / (1024 * 1024 * 1024) + " GB";
        }
    }

    public static void setTimeout(Runnable runnable, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (InterruptedException e) {
            }
        }).start();
    }

    public static String addSlashes(String input) {
        input = input.replace("\\", "\\\\");
        input = input.replace("'", "\\'");
        input = input.replace("\"", "\\\"");
        input = input.replace("\b", "\\b");
        input = input.replace("\n", "\\n");
        input = input.replace("\r", "\\r");
        input = input.replace("\t", "\\t");

        return input;
    }

    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Long.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String roundToTwoDecimals(double num) {
        double roundedNumber = Math.round(num * 100.0) / 100.0;
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        return decimalFormat.format(roundedNumber);
    }

    public static String dvp(long j) {
        long j2 = (j / 1000) + 1;
        String str = "";
        int i = 0;
        while (((long) i) < j2) {
            if (j >= 1000) {
                long j3 = j % 1000;
                if (j3 == 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(".000");
                    stringBuilder.append(str);
                    str = stringBuilder.toString();
                } else {
                    StringBuilder stringBuilder2;
                    String str2;
                    if (j3 < 10) {
                        stringBuilder2 = new StringBuilder();
                        str2 = ".00";
                    } else if (j3 < 100) {
                        stringBuilder2 = new StringBuilder();
                        str2 = ".0";
                    } else {
                        stringBuilder2 = new StringBuilder();
                        str2 = ".";
                    }
                    stringBuilder2.append(str2);
                    stringBuilder2.append(j3);
                    stringBuilder2.append(str);
                    str = stringBuilder2.toString();
                }
                j /= 1000;
                i++;
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(j);
                stringBuilder3.append(str);
                return stringBuilder3.toString();
            }
        }
        return str;
    }

    public static String dvp2(double n) {
        if (n >= 1000) {
            String[] suffixes = {"", "k", "m", "b"};
            int index = 0;
            while (n >= 1000 && index < suffixes.length - 1) {
                index++;
                n /= 1000.0;
            }
            return String.format("%.2f%s", n, suffixes[index]);
        }
        return String.valueOf((int) n);
    }

    public static ItemMap saoPhaLe(Zone zone, int tempId, int quantity, int x, int y, int playerId) {
        ItemMap it = new ItemMap(zone, tempId, quantity, x, y, playerId);
        switch (tempId) {
            case 441 ->
                it.options.add(new Item.ItemOption(95, 5));
            case 442 ->
                it.options.add(new Item.ItemOption(96, 5));
            case 443 ->
                it.options.add(new Item.ItemOption(97, 5));
            case 444 ->
                it.options.add(new Item.ItemOption(99, 3));
            case 445 ->
                it.options.add(new Item.ItemOption(98, 3));
            case 446 ->
                it.options.add(new Item.ItemOption(100, 5));
            case 447 ->
                it.options.add(new Item.ItemOption(101, 5));
            case 459 -> {
                it.options.add(new Item.ItemOption(112, 80));
                it.options.add(new Item.ItemOption(93, 90));
                it.options.add(new Item.ItemOption(30, 1));
            }
        }
        return it;
    }

    public static Item saoPhaLe(int tempId, int quantity) {
        Item it = ItemService.gI().createNewItem((short) tempId, quantity);
        switch (tempId) {
            case 441 ->
                it.itemOptions.add(new Item.ItemOption(95, 5));
            case 442 ->
                it.itemOptions.add(new Item.ItemOption(96, 5));
            case 443 ->
                it.itemOptions.add(new Item.ItemOption(97, 5));
            case 444 ->
                it.itemOptions.add(new Item.ItemOption(99, 3));
            case 445 ->
                it.itemOptions.add(new Item.ItemOption(98, 3));
            case 446 ->
                it.itemOptions.add(new Item.ItemOption(100, 5));
            case 447 ->
                it.itemOptions.add(new Item.ItemOption(101, 5));
            case 459 -> {
                it.itemOptions.add(new Item.ItemOption(112, 80));
                it.itemOptions.add(new Item.ItemOption(93, 90));
                it.itemOptions.add(new Item.ItemOption(30, 1));
            }
        }
        return it;
    }
}
