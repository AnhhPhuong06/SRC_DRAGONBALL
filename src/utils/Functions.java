package utils;

import java.util.Random;

import models.player.Player;

import java.util.regex.Pattern;

public class Functions {

    private static final String REGEX = "\\b(dkm|đkm|đbrr|địt|đĩ|đỹ|cm|cmm|lồn|buồi|cc|ôm cl|mẹ mày|cặc|đụ|fuck|damn|clmm|dcmm|cl|tml|đ*t|c*c|dit|d*t|c.a.c|l.o.n|c.ặ.c|l.ồ.n|b.u.ồ.i|bu*i|đặc cầu|đồn lầu|bú cu|buscu|đm|cc|đb|db|lol|nhu lon|nhu cac|vc|vl|vãi|đéo|đờ mờ|đờ cờ mờ|clgt|dell|mẹ|cứt|shit|idiot|khốn|xiên chết|cụ|giao phối|thiểu năng|ngáo|chó|dog|đcmm|vcl|vkl|đ!t|d!t|đỵt|dyt|ngu|óc|.com|.net|.online|.vn|.pw|.pro|.org|.info|.ml|.ga|.gq|.cf|.fun|.xyz|.io|.club)\\b";
    private static final Pattern pattern = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);

    public static boolean isSpam(Player player, String text) {
        return pattern.matcher(text.toLowerCase()).find() && (!player.name.equals("Ngọc Rồng Online"));
    }

    public static int maxint(long n) {
        return (int) (n > Integer.MAX_VALUE ? Integer.MAX_VALUE : n);
    }

    public static String generateRandomCharacters(int quantity) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < quantity; i++) {
            int type = random.nextInt(2);
            char generatedChar;

            if (type == 0) {
                generatedChar = (char) (random.nextInt(10) + '0');
            } else {
                generatedChar = (char) (random.nextInt(26) + 'A');
            }

            sb.append(generatedChar);
        }

        return sb.toString();
    }

    public static void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
        }
    }

}
