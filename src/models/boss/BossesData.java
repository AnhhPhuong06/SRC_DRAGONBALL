package models.boss;


import consts.AppearType;
import consts.BossID;
import consts.ConstPlayer;
import models.skill.Skill;
import utils.Util;

public class BossesData {

    private static final int[][] FULL_DRAGON = new int[][]{{Skill.DRAGON, 1}, {Skill.DRAGON, 2}, {Skill.DRAGON, 3}, {Skill.DRAGON, 4}, {Skill.DRAGON, 5}, {Skill.DRAGON, 6}, {Skill.DRAGON, 7}};
    private static final int[][] FULL_DEMON = new int[][]{{Skill.DEMON, 1}, {Skill.DEMON, 2}, {Skill.DEMON, 3}, {Skill.DEMON, 4}, {Skill.DEMON, 5}, {Skill.DEMON, 6}, {Skill.DEMON, 7}};
    private static final int[][] FULL_GALICK = new int[][]{{Skill.GALICK, 1}, {Skill.GALICK, 2}, {Skill.GALICK, 3}, {Skill.GALICK, 4}, {Skill.GALICK, 5}, {Skill.GALICK, 6}, {Skill.GALICK, 7}};
    private static final int[][] FULL_KAMEJOKO = new int[][]{{Skill.KAMEJOKO, 1}, {Skill.KAMEJOKO, 2}, {Skill.KAMEJOKO, 3}, {Skill.KAMEJOKO, 4}, {Skill.KAMEJOKO, 5}, {Skill.KAMEJOKO, 6}, {Skill.KAMEJOKO, 7}};
    private static final int[][] FULL_MASENKO = new int[][]{{Skill.MASENKO, 1}, {Skill.MASENKO, 2}, {Skill.MASENKO, 3}, {Skill.MASENKO, 4}, {Skill.MASENKO, 5}, {Skill.MASENKO, 6}, {Skill.MASENKO, 7}};
    private static final int[][] FULL_ANTOMIC = new int[][]{{Skill.ANTOMIC, 1}, {Skill.ANTOMIC, 2}, {Skill.ANTOMIC, 3}, {Skill.ANTOMIC, 4}, {Skill.ANTOMIC, 5}, {Skill.ANTOMIC, 6}, {Skill.ANTOMIC, 7}};
    private static final int[][] FULL_LIENHOAN = new int[][]{{Skill.LIEN_HOAN, 1}, {Skill.LIEN_HOAN, 2}, {Skill.LIEN_HOAN, 3}, {Skill.LIEN_HOAN, 4}, {Skill.LIEN_HOAN, 5}, {Skill.LIEN_HOAN, 6}, {Skill.LIEN_HOAN, 7}};
    private static final int[][] FULL_TDHS = new int[][]{{Skill.THAI_DUONG_HA_SAN, 1}, {Skill.THAI_DUONG_HA_SAN, 2}, {Skill.THAI_DUONG_HA_SAN, 3}, {Skill.THAI_DUONG_HA_SAN, 4}, {Skill.THAI_DUONG_HA_SAN, 5}, {Skill.THAI_DUONG_HA_SAN, 6}, {Skill.THAI_DUONG_HA_SAN, 7}};

    private static final int REST_1_S = 1;
    private static final int REST_2_S = 2;
    private static final int REST_5_S = 5;
    private static final int REST_10_S = 10;
    private static final int REST_20_S = 20;
    private static final int REST_30_S = 30;
    private static final int REST_1_M = 60;
    private static final int REST_2_M = 120;
    private static final int REST_5_M = 300;
    private static final int REST_10_M = 600;
    private static final int REST_15_M = 900;
    private static final int REST_20_M = 1200;
    private static final int REST_30_M = 1800;
    private static final int REST_24_H = 86400;

    //************************************************************************** Boss nappa
    public static final BossData BE_NA = new BossData(
            "Bé na", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1595, 1596, 1597, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{5000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25,
                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70,
                71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99,
                100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, // map join
            new int[][]{},
            new String[]{}, // text chat 1
            new String[]{"|-1|Tùng tùng xèng xèng"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData TAUPAYPAY = new BossData(
            "Tàu Pảy Pảy", //name
            ConstPlayer.XAYDA, //gender
            new short[]{92, 93, 94, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1, //dame
            new long[]{10000}, //hp
            new int[]{47}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DRAGON, 2, 2000},
                {Skill.DRAGON, 3, 3000},
                {Skill.DRAGON, 7, 7000},
                {Skill.KAMEJOKO, 7, 2000},
                {Skill.KAMEJOKO, 6, 1800},
                {Skill.KAMEJOKO, 4, 1500},
                {Skill.KAMEJOKO, 2, 1000}
            },
            new String[]{"|-1|Ta sẽ dạy ngươi vài chiêu",
                "|-1|Ta sẽ đánh hết sức, ngươi cẩn thận nhé"}, //text chat 1
            new String[]{"|-1|Haizzzzz",
                "|-1|Hahaha",
                "|-1|Xem đây"}, //text chat 2
            new String[]{"|-1|OK ta chịu thua"}, //text chat 3
            REST_1_S
    );
    public static final BossData KUKU = new BossData(
            "Kuku", //name
            ConstPlayer.XAYDA, //gender
            new short[]{159, 160, 161, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            9000, //dame
            new long[]{2000000}, //hp
            new int[]{68, 69, 70, 71, 72}, //map join
            new int[][]{
                {Skill.MASENKO, 3, 1000},
                {Skill.LIEN_HOAN, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Ta sẽ tàn sát khu này trong vòng 5 phút nữa",
                "|-1|Haha, mày đây rồi",
                "|-1|Tao đã có lệnh của đại ca Fide rồi",
                "|-1|Tao hơn hẳn mày, nhìn máy đo đi",
                "|-1|Mày yếu đi đó, với sức chiến đấu đó sao có thể thắng được tao",
                "|-1|Định chạy trốn hả, hử"
            }, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M //second rest
    );
    public static final BossData UBU = new BossData(
            "Đệ Tử Ubu", //name
            ConstPlayer.XAYDA, //gender
            new short[]{946, 947, 948, -1, -1, -1}, //outfit
            50000, //dame
            new long[]{1000000000L}, //hp
            new int[]{3,4,5,6,27,28,29,30,9,11,12,13,10,34,33,32,31,16,17,18,19,20,37,38,36,35,24,25,26}, //map
            new int[][]{{Skill.MASENKO, 3, 1000}, {Skill.LIEN_HOAN, 7, 1000}}, //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Ta sẽ tàn sát khu này trong vòng 5 phút nữa", "|-1|Haha, mấy thằng ngu đây rồi", "|-1|Tao đã có lệnh của đại ca Key Vàng Bulma rồi", "|-1|Chết con đĩ mẹ mày đi", "|-1|Mày gà vãi cứt với sức chiến đấu đó sao có thể thắng được tao", "|-1|Định chạy trốn hả, thằng lồn này"}, //text chat 2
            new String[]{}, //text chat 3
            REST_10_M
    );
    public static final BossData MAP_DAU_DINH = new BossData(
            "Mập Đầu Đinh", //name
            ConstPlayer.XAYDA, //gender
            new short[]{165, 166, 167, -1, -1, -1}, //outfit
            10000, //dame
            new long[]{5000000L}, //hp
            new int[]{63,64,65,66,67}, //map
            new int[][]{{Skill.GALICK, 7, 1000}, {Skill.ANTOMIC, 7, 10000}}, //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|HAHAHA", "|-1|Tao chỉ cần 10 giây để giết hết bọn mày", "|-1|Được rồi tao sẽ thổi bay hết", "|-1|Chết hết đi cho tao", "|-1|Ta sẽ tàn sát khu này trong vòng 5 phút nữa", "|-1|Tao sẽ giết hết bọn mày"}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M
    );

    public static final BossData RAMBO = new BossData(
            "Rambo", //name
            ConstPlayer.XAYDA, //gender
            new short[]{162, 163, 164, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            12400, //dame
            new long[]{7000000}, //hp
            new int[]{74, 75, 76, 77}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.KAMEJOKO, 1, 1000},},//skill //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|HAHAHA",
                "|-1|Tao chỉ cần 10 giây để giết hết bọn mày",
                "|-1|Thấy ta đẹp trai không",
                "|-1|Mày sợ tao chưa",
                "|-1|Ta sẽ tàn sát khu này trong vòng 5 phút nữa",
                "|-1|Tao sẽ giết hết bọn mày"}, //text chat 2
            new String[]{"|-1|Ôi bạn ơi..."}, //text chat 3
            REST_15_M //second rest
    );

    //========================TDST========================
    public static final BossData SO_4 = new BossData(
            "Số 4", //name
            ConstPlayer.XAYDA, //gender
            new short[]{168, 169, 170, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{20000000}, //hp
            new int[]{79, 81, 82, 83}, //map join
            //           new int[]{86}, //map join
            new int[][]{
                {Skill.LIEN_HOAN, 7, 1000},
                {Skill.MASENKO, 7, 1000},
                {Skill.THOI_MIEN, 7, 100000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả",
                "|-1|Ê cố lên nhóc",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|Chán",
                "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Chỉ là bọn con nít",
                "|-1|HAHAHA"
            }, //text chat 2
            new String[]{"|-1|Fide gọi ta về, ngươi có ngon thì chờ ở đây",
                "|2|Để tao xử nó cho"}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData SO_3 = new BossData(
            "Số 3", //name
            ConstPlayer.XAYDA, //gender
            new short[]{174, 175, 176, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{30000000}, //hp
            new int[]{79, 81, 82, 83}, //map join
            new int[][]{
                {Skill.LIEN_HOAN, 7, 1000},
                {Skill.ANTOMIC, 4, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả",
                "|-1|Ê cố lên nhóc",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|Chán",
                "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Chỉ là bọn con nít",
                "|-1|HAHAHA"
            }, //text chat 2
            new String[]{"|-1|Fide gọi ta về, ngươi có ngon thì chờ ở đây",
                "|1|Để tao xử nó cho",
                "|0|Để tao xử nó cho"}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    // Để tao xử nó cho S2, S1
    public static final BossData SO_2 = new BossData(
            "Số 2", //name
            ConstPlayer.XAYDA, //gender
            new short[]{171, 172, 173, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{40000000}, //hp
            new int[]{79, 81, 82, 83}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.ANTOMIC, 3, 3000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả",
                "|-1|Ê cố lên nhóc",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|Chán",
                "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Chỉ là bọn con nít",
                "|-1|HAHAHA"
            }, //text chat 2
            new String[]{"|3|Để tao xử nó cho"}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData SO_1 = new BossData(
            "Số 1", //name
            ConstPlayer.XAYDA, //gender
            new short[]{177, 178, 179, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1200, //dame
            new long[]{50000000}, //hp
            new int[]{79, 81, 82, 83}, //map join
            new int[][]{
                {Skill.LIEN_HOAN, 7, 1000},
                {Skill.KAMEJOKO, 4, 10000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả",
                "|-1|Ê cố lên nhóc",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|Chán",
                "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Chỉ là bọn con nít",
                "|-1|HAHAHA"
            }, //text chat 2
            new String[]{"|3|Để tao xử nó cho"}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TIEU_DOI_TRUONG = new BossData(
            "Tiểu đội trưởng", //name
            ConstPlayer.XAYDA, //gender
            new short[]{180, 181, 182, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1300, //dame
            new long[]{60000000}, //hp
            new int[]{79, 81, 82, 83}, //map join
            new int[][]{
                {Skill.MASENKO, 7, 1000},
                {Skill.GALICK, 7, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả",
                "|-1|Ê cố lên nhóc",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|Chán",
                "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Chỉ là bọn con nít",
                "|-1|HAHAHA"
            }, //text chat 2
            new String[]{"|-1|Fide gọi ta về, ngươi có ngon thì chờ ở đây"}, //text chat 3
            REST_15_M,
            new int[]{BossID.SO_2, BossID.SO_1, BossID.SO_3, BossID.SO_4} //type appear
    );

    public static final BossData SO_4_NM = new BossData(
            "Số 4", //name
            ConstPlayer.XAYDA, //gender
            new short[]{168, 169, 170, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{10000000}, //hp
            new int[]{7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43}, //map join
            //           new int[]{86}, //map join
            new int[][]{
                {Skill.LIEN_HOAN, 7, 1000},
                {Skill.MASENKO, 7, 1000},
                {Skill.THOI_MIEN, 7, 100000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?", "|-1|Ê cố lên nhóc",
                "|-1|Chán", "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Các ngươi không nhúc nhích được sao?",
                "|-1|HAHAHAHA", "|-1|Chỉ là bọn con nít"
            }, //text chat 2
            new String[]{"|-1|Cay quá!",
                "|-1|Ta mà lại thua được sao?",
                "|-1|Hãy trả thù cho ta!"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData SO_3_NM = new BossData(
            "Số 3", //name
            ConstPlayer.XAYDA, //gender
            new short[]{174, 175, 176, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1800, //dame
            new long[]{10000000}, //hp
            new int[]{7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43}, //map join
            new int[][]{
                {Skill.LIEN_HOAN, 7, 1000},
                {Skill.ANTOMIC, 4, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?", "|-1|Ê cố lên nhóc",
                "|-1|Chán", "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|HAHAHAHA", "|-1|Chỉ là bọn con nít"
            }, //text chat 2
            new String[]{"|-1|Cay quá!",
                "|-1|Ta mà lại thua được sao?",
                "|-1|Hãy trả thù cho ta!"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData SO_2_NM = new BossData(
            "Số 2", //name
            ConstPlayer.XAYDA, //gender
            new short[]{171, 172, 173, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            2020, //dame
            new long[]{10000000}, //hp
            new int[]{7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.ANTOMIC, 3, 3000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?", "|-1|Ê cố lên nhóc",
                "|-1|Chán", "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|HAHAHAHA", "|-1|Chỉ là bọn con nít"
            }, //text chat 2
            new String[]{"|-1|Cay quá!"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData SO_1_NM = new BossData(
            "Số 1", //name
            ConstPlayer.XAYDA, //gender
            new short[]{177, 178, 179, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            2500, //dame
            new long[]{10000000}, //hp
            new int[]{7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43}, //map join
            new int[][]{
                {Skill.LIEN_HOAN, 7, 1000},
                {Skill.KAMEJOKO, 4, 10000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?", "|-1|Ê cố lên nhóc",
                "|-1|Chán", "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|HAHAHAHA", "|-1|Chỉ là bọn con nít"
            }, //text chat 2
            new String[]{"|-1|Hãy trả thù cho ta!"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TIEU_DOI_TRUONG_NM = new BossData(
            "Tiểu đội trưởng", //name
            ConstPlayer.XAYDA, //gender
            new short[]{180, 181, 182, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            3000, //dame
            new long[]{10000000}, //hp
            new int[]{7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43}, //map join
            new int[][]{
                {Skill.SOCOLA, 7, 1000},
                {Skill.MASENKO, 7, 1000},
                {Skill.GALICK, 7, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?", "|-1|Ê cố lên nhóc",
                "|-1|Chán", "|-1|Đại ca Fide có nhầm không nhỉ",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|HAHAHAHA", "|-1|Chỉ là bọn con nít"
            }, //text chat 2
            new String[]{"|-1|Cay quá!"
            }, //text chat 3
            REST_15_M,
            new int[]{BossID.SO_4_NM, BossID.SO_3_NM, BossID.SO_2_NM, BossID.SO_1_NM,} //type appear
    );

    //========================FIDE========================
    public static final BossData FIDE_DAI_CA_1 = new BossData(
            "Fide đại ca 1", //name
            ConstPlayer.XAYDA, //gender
            new short[]{183, 184, 185, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            3200, //dame
            new long[]{20000000}, //hp
            new int[]{80}, //map join
            (int[][]) Util.addArray(FULL_ANTOMIC, FULL_GALICK), //skill
            new String[]{"|-2|Fide!!!, với những gì ngươi đã làm với người Xayda và Namek...",
                "|-2|Ta phán ngươi tội: tử hình",
                "|-1|Khẩu khí ngang tàng lắm",
                "|-1|Xem bản lĩnh của ngươi thế nào đã"
            }, //text chat 1
            new String[]{"|-1|Các ngươi tới số rồi mới gặp phải ta",
                "|-1|Trận địa pháo mini"
            }, //text chat 2
            new String[]{"|-1|Ác quỷ biến hình, hây aaaa..."}, //text chat 3
            REST_15_M //second rest
    );

    public static final BossData FIDE_DAI_CA_2 = new BossData(
            "Fide đại ca 2", //name
            ConstPlayer.XAYDA, //gender
            new short[]{186, 187, 188, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            5000, //dame
            new long[]{30000000}, //hp
            new int[]{80}, //map join
            (int[][]) Util.addArray(FULL_ANTOMIC, FULL_LIENHOAN), //skill
            new String[]{"|-1|Hê hê, cẩn thận đi",
                "|-1|Nếu đã biến thành thế này thì ta sẽ không nhùn nhặn như trước đâu"
            }, //text chat 1
            new String[]{"|-1|Oải rồi hả?",
                "|-1|Ê cố lên nhóc",
                "|-1|Ôi, xin lỗi nhé. Sức mạnh lớn quá nên ta cũng chẳng điều khiển nổi nữa!",
                "|-1|Hahaha! Ấn tượng đấy! Tên nào cũng lủi rất nhanh!",
                "|-2|A...Tốc độ nhanh quá!",
                "|-1|Hình như... mày không phải là một thằng nhóc bình thường!",
                "|-1|Mấy đòn vừa rồi, nói thật là cũng đau đấy!",
                "|-1|Nhưng tiếc rằng đối thủ của mày lại là Fide này...",
                "|-2|Chết tiệt.. chúng ta đã đánh giá quá thấp sức mạnh của hắn!!",
                "|-2|Đồ..Đồ quái vật..!",
                "|-2|Tốc độ kinh hoàng quá! Ai mà né nổi chứ!",}, //text chat 2
            new String[]{"|-1|Ác quỷ biến hình, Graaaaa...."}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );

    public static final BossData FIDE_DAI_CA_3 = new BossData(
            "Fide đại ca 3", //name
            ConstPlayer.XAYDA, //gender
            new short[]{189, 190, 191, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            12200, //dame
            new long[]{40000000}, //hp
            new int[]{80}, //map join
            (int[][]) Util.addArray(FULL_MASENKO, FULL_LIENHOAN), //skill
            new String[]{"|-1|Ta sẽ cho các ngươi thấy đâu mới là sức mạnh của ta!!"}, //text chat 1
            new String[]{"|-1|Ta nói các ngươi rồi! Sức mạnh này của ta còn đáng sợ hơn địa ngục!!",
                "|-1|Ta chơi thêm chút nữa chắc ngươi chóng mặt buồn nôn mất!!",
                "|-2|Ăn gì mà khỏe thế!",
                "|-2|Chết đi Fide!!!!",
                "|-1|Hô hô hô hô",
                "|-1|Chán thật! Khí của ngươi sắp hết rồi. Để ta tiễn ngươi về địa ngục!",
                "|-1|Ngươi quá tự cao rồi đấy,xem ta đây!",}, //text chat 2
            new String[]{"|-1|Lũ khốn..",
                "|-1|..Một ngày nào đó ta sẽ quay lại và trả thù các ngươi",
                "|-1|Nhớ mặt tao đấy !",}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );

    //************************************************************************** Boss Android
    public static final BossData DR_KORE = new BossData(
            "Dr.Kôrê", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{255, 256, 257, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            12000, //dame
            new long[]{10000000}, //hp
            new int[]{96, 94, 93}, //map join
            new int[][]{
                {Skill.THOI_MIEN, 3, 10000},
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.LIEN_HOAN, 7, 1000},},//skill
            new String[]{"|-2|Chào anh! em đứng đây từ chiều",
                "|-1|Quái lạ! Sao chúng biết rõ tung tích của bọn ta thế nhỉ?",
                "|-1|Chúng còn biết chính xác ta sẽ xuất hiện ở đây để đón đánh nữa!",
                "|-1|Chúng mày là ai từ đâu tới?Cho tao xin cái địa chỉ",
                "|-2|Điều ấy biết hay không cũng không còn quan trọng nữa",
                "|-1|Ừ bọn bây chỉ là hạng tôm tép ta chẳng cần biết tên làm gì!",
                "|-1|Số 19! Xuất chiêu đi nào",
                "|0|Okê đại ca, em sẽ xử lý bọn này trong vòng 2 tiếng."
            }, //text chat 1
            new String[]{"|-1|Oải rồi hả?",
                "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Mi khá đấy, nhưng so với ta cũng chỉ là hạng tôm tép",
                "|-1|Lôi Công Trảo",
                "|-1|Cho dù ngươi có mạnh đến đâu.. thì cũng không đánh bại được rôbốt bọn ta",
                "|-2|Lão già khôn thật!!",
                "|-2|Hừ! Lão già khốn kiếp!",}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M, //second rest
            new int[]{BossID.ANDROID_19}
    );

    public static final BossData ANDROID_19 = new BossData(
            "Android 19", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{249, 250, 251, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            12200, //dame
            new long[]{10000000}, //hp
            new int[]{96, 94, 93}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 10000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?",
                "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Ngươi sẽ không bao giờ thắng được đâu!!",
                "|-2|Ngươi vừa hút được nhiều rồi đấy, nhưng giờ thì đừng hòng!!",}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );
    public static final BossData WHIS_1 = new BossData(
            "Whis", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{838, 839, 840, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{1000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 24, 27, 28, 29, 30, 42}, //map join
            new int[][]{
                {Skill.THOI_MIEN, 3, 10000},
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.LIEN_HOAN, 7, 1000},},//skill
            new String[]{"|-2|Chào anh! em đứng đây từ chiều",
                "|-1|Quái lạ! Sao chúng biết rõ tung tích của bọn ta thế nhỉ?",
                "|-1|Chúng còn biết chính xác ta sẽ xuất hiện ở đây để đón đánh nữa!",
                "|-1|Chúng mày là ai từ đâu tới?Cho tao xin cái địa chỉ",
                "|-2|Điều ấy biết hay không cũng không còn quan trọng nữa",
                "|-1|Ừ bọn bây chỉ là hạng tôm tép ta chẳng cần biết tên làm gì!",
                "|-1|Số 19! Xuất chiêu đi nào",
                "|0|Okê đại ca, em sẽ xử lý bọn này trong vòng 2 tiếng."
            }, //text chat 1
            new String[]{"|-1|Oải rồi hả?",
                "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Mi khá đấy, nhưng so với ta cũng chỉ là hạng tôm tép",
                "|-1|Lôi Công Trảo",
                "|-1|Cho dù ngươi có mạnh đến đâu.. thì cũng không đánh bại được rôbốt bọn ta",
                "|-2|Lão già khôn thật!!",
                "|-2|Hừ! Lão già khốn kiếp!",}, //text chat 2
            new String[]{}, //text chat 3
            REST_20_M, //second rest
            new int[]{BossID.BERUS}
    );
    public static final BossData BERUS = new BossData(
            "Beerus", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{508, 509, 510, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{1000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 24, 27, 28, 29, 30, 42}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 10000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?",
                "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Ngươi sẽ không bao giờ thắng được đâu!!",
                "|-2|Ngươi vừa hút được nhiều rồi đấy, nhưng giờ thì đừng hòng!!",}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );
    public static final BossData VADOS = new BossData(
            "Vados", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{530, 531, 532, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{1000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 24, 27, 28, 29, 30, 42}, //map join
            new int[][]{
                {Skill.THOI_MIEN, 3, 10000},
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.LIEN_HOAN, 7, 1000},},//skill
            new String[]{"|-2|Chào anh! em đứng đây từ chiều",
                "|-1|Quái lạ! Sao chúng biết rõ tung tích của bọn ta thế nhỉ?",
                "|-1|Chúng còn biết chính xác ta sẽ xuất hiện ở đây để đón đánh nữa!",
                "|-1|Chúng mày là ai từ đâu tới?Cho tao xin cái địa chỉ",
                "|-2|Điều ấy biết hay không cũng không còn quan trọng nữa",
                "|-1|Ừ bọn bây chỉ là hạng tôm tép ta chẳng cần biết tên làm gì!",
                "|-1|Số 19! Xuất chiêu đi nào",
                "|0|Okê đại ca, em sẽ xử lý bọn này trong vòng 2 tiếng."
            }, //text chat 1
            new String[]{"|-1|Oải rồi hả?",
                "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Mi khá đấy, nhưng so với ta cũng chỉ là hạng tôm tép",
                "|-1|Lôi Công Trảo",
                "|-1|Cho dù ngươi có mạnh đến đâu.. thì cũng không đánh bại được rôbốt bọn ta",
                "|-2|Lão già khôn thật!!",
                "|-2|Hừ! Lão già khốn kiếp!",}, //text chat 2
            new String[]{}, //text chat 3
            REST_20_M, //second rest
            new int[]{BossID.CHAMPA}
    );
    public static final BossData CHAMPA = new BossData(
            "Champa", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{511, 512, 513, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{1000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 24, 27, 28, 29, 30, 42}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 10000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?",
                "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Ngươi sẽ không bao giờ thắng được đâu!!",
                "|-2|Ngươi vừa hút được nhiều rồi đấy, nhưng giờ thì đừng hòng!!",}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );
    //**************************************************************************
    public static final BossData ANDROID_13 = new BossData(
            "Android 13", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{252, 253, 254, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            12005, //dame
            new long[]{5000000}, //hp
            new int[]{104}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.LIEN_HOAN, 7, 1000},},//skill
            new String[]{"|-1|Sôn..gôku",
                "|-2|Lại là tiến sĩ Kôrê à.. rốt cuộc ông ta đã tạo ra bao nhiêu rôbốt nhân tạo thế nhỉ?",
                "|-1|Bọn ta là rôbốt sát thủ, sinh ra từ máy tính ngài Kôrê,..",
                "|-1|..cho một mục tiêu duy nhất là giết Sôngôku!",
                "|-2|Máy tính? Để giết Gôku sao?",
                "|-1|Mong muốn trả thù Gôku của ngài Kôrê đã được lưu hết vào máy tính..",
                "|-1|.., Bọn ta sinh ra từ lòng căm thù ngày một tăng bên trong chiếc máy tính có chứa mong muốn trả thù",
                "|-1|Mục tiêu của bọn ta chỉ là Gôku, nhưng mà.. nếu ngươi mà cản đường thì là chuyện khác!",}, //text chat 1
            new String[]{"|-1|Sao thế hả? Ta mới chỉ khởi động thôi mà!",
                "|-2|Ngươi đánh giá thấp bọn ta quá đấy!",
                "|-2|Đừng có tưởng bở, lũ sâu bọ!",
                "|-1|Nếu có ý định gây trở ngại cho cuộc chiến giữa ta và Sôngôku, thì ta cũng sẽ giết ngươi ngay lập tức",
                "|-2|Ngươi tưởng ta để cho ngươi giết được ta ngay à?",
                "|-2|Đúng là mạnh mồm thật đấy!",
                "|-2|Đỡ này",}, //text chat 2
            new String[]{"|-1|Sô..Sông...gôku....."}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData ANDROID_14 = new BossData(
            "Android 14", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{246, 247, 248, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            12000, //dame
            new long[]{7000000}, //hp
            new int[]{104}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.LIEN_HOAN, 7, 1000},},//skill
            new String[]{"|-2|Các ngươi là ai?",
                "|-2|Ta không thể cảm nhận được khí của các ngươi, các ngươi không phải là con người đúng chứ?",
                "|-2|Ta hiểu rồi, các ngươi là rôbốt sát thủ do tiến sĩ Kôrê tạo ra chứ gì?"
            }, //text chat 1
            new String[]{}, //text chat 2
            new String[]{"|0|Số 14 và số 15 tiêu tùng cả rồi à?"}, //text chat 3
            REST_15_M,
            new int[]{BossID.ANDROID_13, BossID.ANDROID_15}
    );

    public static final BossData ANDROID_15 = new BossData(
            "Android 15", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{261, 262, 263, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10200, //dame
            new long[]{10000000}, //hp
            new int[]{104}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.LIEN_HOAN, 7, 1000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{"|-2|Thì ra vẫn chỉ là một đống sắt vụn!"}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );
//**************************************************************************

    public static final BossData PIC = new BossData(
            "Pic", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{237, 238, 239, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            7020, //dame
            new long[]{7000000}, //hp
            new int[]{97, 98, 99}, //map join
            (int[][]) Util.addArray(FULL_GALICK, FULL_KAMEJOKO), //skill
            new String[]{"|-1|Chào! Có Gôku ở đây không?",
                "|-3|Tôi không nghĩ Gôku ở đây đâu!",
                "|-2|Biến khỏi đây đi, Gôku không có ở đây đâu!",
                "|-1|Bọn ta cũng nghĩ vậy, ngươi nói cho bọn ta biết hắn ở đâu được không!?",
                "|-2|Ngươi nghĩ bọn ta sẽ nói sao??",
                "|-1|Nếu ngươi không chịu nói bọn ta sẽ phải ra tay.."
            }, //text chat 1
            new String[]{"|-1|Ngươi thực sự rất mạnh dù không phải là một rôbốt. Ngươi không phải là Piccôlô",
                "|-1|Nhưng ta không quan tâm ngươi là ai, ta chỉ cần biết Gôku đang ở đâu!",
                "|-1|Sao ngươi không chịu nói cho ta biết chứ!?",
                "|-2|Mục đích của ngươi không phải là giết Gôku sao? Vì vậy ta không nói cho ngươi biết cậu ấy đang ở đâu",
                "|-1|Vậy thì ta bắt buộc phải tiếp tục đánh buộc ngươi phải nói ra!",
                "|-1|Lần này ta không nương tay đâu!",
                "|-2|Ngươi thực sự rất nhanh. Nhưng chưa đủ thực lực đâu!!",
                "|-1|Cái gì!? Đó là điều ngu ngốc nhất ta từng nghe.. ta là chiến binh mạnh nhất từ trước đến giờ.!",
                "|1|Hắn thực sự rất mạnh, đúng là cuộc chiến cân sức",
                "|-3|Pic, trả nhẽ cậu lại để thua mấy tên nhóc vặt này sao?"
            }, //text chat 2
            new String[]{"|1|Pic tiêu rồi, tớ lên trước nhé!",
                "|-3|Okê, xin cứ tự nhiên"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData POC = new BossData(
            "Poc", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{240, 241, 242, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            8000, //dame
            new long[]{10000000}, //hp
            new int[]{97, 98, 99}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO), //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Đừng tưởng ta đây là con gái mà dễ bắt nạt nhé",
                "|-1|Khôn hồn thì chỉ chỗ Gôku cho bọn ta nhanh đi",
                "|-3|Coi kìa, một lũ xúm lại bắt nạt một cô gái kìa..",
                "|-1|Đừng có mà trọng nam khinh nữ",
                "|-2|Tại sao cô gái xinh đẹp thế này mà lại là rôbốt nhỉ?"
            }, //text chat 2
            new String[]{"|-2|Cô gái xinh đẹp vậy mà lại bị tên tiến sĩ Kôrê biến thành người máy.."}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData KING_KONG = new BossData(
            "King Kong", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{243, 244, 245, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{12000000}, //hp
            new int[]{97, 98, 99}, //map join
            (int[][]) Util.addArray(FULL_LIENHOAN, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Mau đền mạng cho những người bạn của ta",
                "|-1|Sức mạnh của ta chênh nhau với các ngươi một trời một vực đấy!",
                "|-1|Thằng kia đừng để bọn nó trói tao !"
            }, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M,
            new int[]{BossID.PIC, BossID.POC}
    );
    //************************************************************************** Boss cell

    public static final BossData XEN_BO_HUNG_1 = new BossData(
            "Xên bọ hung 1",
            ConstPlayer.XAYDA,
            new short[]{228, 229, 230, -1, -1, -1},
            1000,
            new long[]{50000000},
            new int[]{100},
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 10000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 10000}},//skill
            new String[]{"|-2|Cái gì kia vậy!? Đó là loài gì thế!!?",
                "|-1|Hôm nay sẽ là ngày đáng nhớ đây!",
                "|-1|Ta sẽ hấp thụ số 17 và 18 để đạt được dạng hoàn hảo!",
                "|-1|Sao vậy, Picôlô? Nếu ngươi muốn ngăn ta lại thì xong lên đi chứ!?"
            }, //text chat 1
            new String[]{"|-2|Hắn làm ta bất ngờ đấy! Khốn kiếp!",
                "|-2|Tên đó muốn hấp thụ số 17 và 18 sao?",
                "|-1|Đến đây nào! Khi kết hợp với ta, ngươi sẽ trở nên bất bại, trở thành một thể sống hoàn mỹ!",
                "|-2|Mơ đi, sao ta phải để ngươi hấp thu hả!?",
                "|-2|Dù muốn hay không, ngươi cũng sẽ bị ta hấp thu thôi..",
                "|-2|Chúng ta không thể để hắn đạt đến dạng hoàn hảo được!",
                "|-2|Mục tiêu của hắn không phải là Sôngôku.., mà là phá hủy cả vũ trụ này!",
                "|-1|Làm đứt đuôi ta ư? Đừng quên ta có tế bào của Picôlô!!",
                "|-1|Ta có thể tái tạo.. mọi bộ phận cơ thể!",
                "|-2|Vậy thì để ngăn cản ngươi đạt đến dạng hoàn hảo, ta phải tiêu diệt ngươi!",
                "|-2|Hắn quá mạnh! Mình có thể làm được gì đây!?",
                "|-1|Có vẻ như ta đã trở nên quá mạnh, chắc là ta đã giết nhiều người hơn dự tính!!",
                "|-1|Ngươi không thể thắng nổi ta! Từ bỏ đi!!",
                "|-1|Đến lúc ta hấp thu ngươi rồi",
                "|-2|Đồ quái vật chết tiệt...",
                "|-1|Hê hê hê, rồi ngươi sẽ trở thành một phần của con quái vật này thôi!",
                "|-1|Lại thêm một tên ngốc nữa chán sống!"
            }, //text chat 2
            new String[]{"|-2|Khốn kiếp, Pic.. hắn bị Cell hấp thu rồi!!"}, //text chat 3
            REST_15_M
    );

    public static final BossData XEN_BO_HUNG_2 = new BossData(
            "Xên bọ hung 2",
            ConstPlayer.XAYDA,
            new short[]{231, 232, 233, -1, -1, -1},
            1000,
            new long[]{70000000},
            new int[]{100},
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 10000}}, //skill
            new String[]{}, //text chat 1
            new String[]{"|-2|Nguy rồi... thực sự nguy to rồi!",
                "|-1|Các ngươi nghĩ có thể chạy được sao!?",
                "|-1|Muốn chạy khỏi ta thì đừng hòng!!",
                "|-1|Ta cũng ngạc nhiên với tốc độ của mình! Đó tất nhiên là do ta hấp thụ được số 17!",
                "|-2|Hắn nhanh quá!!",
                "|-1|Ta muốn thử xem sức mạnh này đến đâu...",
                "|-1|Hmm.. có vẻ như sức mạnh này đã tăng lên gấp bội!",
                "|-1|Đã đến lúc ta đạt đến trạng thái hoàn hảo.!",
                "|-1|Có vẻ như ngươi muốn bị ép hơn là tự nguyện!!",
                "|-2|Bây giờ ta chưa thể thắng được ngươi!! Nhưng ngươi đừng hòng huyênh hoang!!!",
                "|-1|Muốn chạy à!!? Ta sẽ không để ngươi thoát đâu!!",}, //text chat 2
            new String[]{"|-1|Đến lúc rồi!"}, //text chat 3
            AppearType.ANOTHER_LEVEL
    );

    public static final BossData XEN_BO_HUNG_3 = new BossData(
            "Xên hoàn thiện",
            ConstPlayer.XAYDA,
            new short[]{234, 235, 236, -1, -1, -1},
            1000,
            new long[]{100000000},
            new int[]{100},
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 10000},
                {Skill.LIEN_HOAN, 7, 10000},
                {Skill.THOI_MIEN, 7, 100000}},
            //skill

            new String[]{"|-2|Cuối cùng hắn cũng đã biến đổi",
                "|-2|Khốn kiếp! Phải kết liễu hắn ngay lúc này!"
            }, //text chat 1
            new String[]{"|-2|Cell đã đạt đến dạng hoàn hảo rồi!",
                "|-2|Đồ khốn, sao ngươi dám làm vậy với số 18!!",
                "|-2|Không ấn tượng lắm với dạng hoàn hảo của ngươi..",
                "|-2|Sao hắn không hề hấn gì?",
                "|-1|Xin lỗi.. Ngươi có thể giúp ta làm nóng cơ thể lên không !?",
                "|-2|Tình hình nguy cấp rồi!",
                "|-2|Khốn kiếp! Ngươi không chú tâm vào trận đấu!",
                "|-1|Thì ta đã bảo đây chỉ là làm nóng cơ thể mà!!",
                "|-1|Giờ ngươi chỉ là rác rưởi mà thôi!",
                "|-2|Không thể nào! Ngươi dù sao cũng chỉ là đồ sâu bọ!",}, //text chat 2
            new String[]{"|-1|Oái.. không...",
                "|-1|Cơ thể hoàn hảo của ta!!",
                "|-1|Ta không tin chuyện này sẽ xảy ra!!",
                "|-1|Đồ khốn kiếp!! Rồi ngươi sẽ phải trả giá"
            }, //text chat 3
            AppearType.ANOTHER_LEVEL
    );

    public static final BossData SIEU_BO_HUNG_1 = new BossData(
            "Xên Hoàn Thiện",
            ConstPlayer.XAYDA,
            new short[]{234, 235, 236, -1, -1, -1},
            20000,
            new long[]{500000000},
            new int[]{103},
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 20000},
                {Skill.GALICK, 7, 1000},
                {Skill.THAI_DUONG_HA_SAN, 7, 50000}
            },
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_20_M,
            new int[]{BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3, BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6, BossID.XEN_CON_7}
    );

    public static final BossData SIEU_BO_HUNG_2 = new BossData(
            "Siêu Bọ Hung",
            ConstPlayer.XAYDA,
            new short[]{234, 235, 236, -1, -1, -1},
            30000,
            new long[]{600000000},
            new int[]{103},
            new int[][]{
                {Skill.DEMON, 3, 1}, {Skill.DEMON, 6, 2}, {Skill.DRAGON, 7, 3}, {Skill.DRAGON, 1, 4}, {Skill.GALICK, 5, 5},
                {Skill.KAMEJOKO, 7, 6}, {Skill.KAMEJOKO, 6, 7}, {Skill.KAMEJOKO, 5, 8}, {Skill.KAMEJOKO, 4, 9}, {Skill.KAMEJOKO, 3, 10}, {Skill.KAMEJOKO, 2, 11}, {Skill.KAMEJOKO, 1, 12},
                {Skill.ANTOMIC, 1, 13}, {Skill.ANTOMIC, 2, 14}, {Skill.ANTOMIC, 3, 15}, {Skill.ANTOMIC, 4, 16}, {Skill.ANTOMIC, 5, 17}, {Skill.ANTOMIC, 6, 19}, {Skill.ANTOMIC, 7, 20},
                {Skill.MASENKO, 1, 21}, {Skill.MASENKO, 5, 22}, {Skill.MASENKO, 6, 23},
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},
                {Skill.KHIEN_NANG_LUONG, 7, 180000},
                {Skill.THOI_MIEN, 7, 30000}},
            //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_20_M
    );

    public static final BossData XEN_CON_1 = new BossData(
            "Xên con 1", //name
            ConstPlayer.XAYDA, //gender
            new short[]{264, 265, 266, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1500, //dame
            new long[]{5000000}, //hp
            new int[]{103}, //map join
            (int[][]) Util.addArray(FULL_DEMON, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData XEN_CON_2 = new BossData(
            "Xên con 2", //name
            ConstPlayer.XAYDA, //gender
            new short[]{264, 265, 266, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{5000000}, //hp
            new int[]{103}, //map join
            (int[][]) Util.addArray(FULL_DEMON, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData XEN_CON_3 = new BossData(
            "Xên con 3", //name
            ConstPlayer.XAYDA, //gender
            new short[]{264, 265, 266, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1500, //dame
            new long[]{5000000}, //hp
            new int[]{103}, //map join
            (int[][]) Util.addArray(FULL_DEMON, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData XEN_CON_4 = new BossData(
            "Xên con 4", //name
            ConstPlayer.XAYDA, //gender
            new short[]{264, 265, 266, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1500, //dame
            new long[]{5000000}, //hp
            new int[]{103}, //map join
            (int[][]) Util.addArray(FULL_DEMON, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData XEN_CON_5 = new BossData(
            "Xên con 5", //name
            ConstPlayer.XAYDA, //gender
            new short[]{264, 265, 266, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1500, //dame
            new long[]{5000000}, //hp
            new int[]{103}, //map join
            (int[][]) Util.addArray(FULL_DEMON, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData XEN_CON_6 = new BossData(
            "Xên con 6", //name
            ConstPlayer.XAYDA, //gender
            new short[]{264, 265, 266, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{5000000}, //hp
            new int[]{103}, //map join
            (int[][]) Util.addArray(FULL_DEMON, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData XEN_CON_7 = new BossData(
            "Xên con 7", //name
            ConstPlayer.XAYDA, //gender
            new short[]{264, 265, 266, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{5000000}, //hp
            new int[]{103}, //map join
            (int[][]) Util.addArray(FULL_DEMON, FULL_MASENKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );
    public static final BossData ANTROM = new BossData(
            "Ăn trộm 100",
            ConstPlayer.TRAI_DAT,
            new short[]{618, 619, 620, -1, -1, -1},
            1,
            new long[]{100},
            new int[]{5},
            new int[][]{
                {Skill.THAI_DUONG_HA_SAN, 3, 20000}},
            new String[]{"|-1|Tới giờ làm việc, lụm lụm", "|-1|Cảm giác mình vào phải khu người nghèo :))"}, //text chat 1
            new String[]{"|-1|Ái chà vàng vàng", "|-1|Không làm vẫn có ăn :))", "|-2|Giám ăn trộm giữa ban ngày thế à", "|-2|Cút ngay không là ăn đòn"}, //text chat 2
            new String[]{"|-1|Híc lần sau ta sẽ cho ngươi phá sản",
                "|-2|Chừa thói ăn trộm nghe chưa"}, //text chat 3
            REST_1_S
    );
    public static final BossData COLLER_VANG = new BossData(
            "Cooler vàng", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{709, 710, 711, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000000}, //hp
            new int[]{184}, //map join
            (int[][]) Util.addArray(FULL_GALICK, FULL_KAMEJOKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_20_M //second rest
    );

    //========================BLACK GOKU========================
    public static final BossData BLACK_GOKU = new BossData(
            "Black Goku", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{550, 551, 552, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{500000000}, //hp
            new int[]{102, 92, 93, 94, 96, 97, 98, 99, 100}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}},
            new String[]{"|-1|Ta là Sôn Gô Ku",
                "|-1|Cơ thể này,sức mạnh này",
                "|-1|Ta khá thích việc loại bỏ các ngươi",
                "|-1|Mau chấp nhận số phận đi lũ sâu bọ"
            }, //text chat 1
            new String[]{"|-1|Các ngươi chỉ có vậy thôi sao?",
                "|-1|Đúng là loài người thấp kém",
                "|-2|Ngươi nói như thể ngươi không phải con người vậy?",
                "|-2|Chiếc nhẫn kia lẽ nào ngươi là một Kaioshin?!",
                "|-1|Các ngươi không nên biết quá nhiều",
                "|-2|Xem đòn đánh của ta đây !",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!"
            }, //text chat 2
            new String[]{"|-1|Biến hình! Super Saiyan Rose"}, //text chat 3
            REST_20_M //second rest
    );

    public static final BossData SUPER_BLACK_GOKU = new BossData(
            "Super Black Goku", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{553, 551, 552, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20000, //dame
            new long[]{600000000}, //hp
            new int[]{102, 92, 93, 94, 96, 97, 98, 99, 100}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}},
            //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Ta chính là người mang thân thể của Songoku",
                "|-1|Sức mạnh của ta là không có giới hạn",
                "|-1|Ta sẽ thống trị vũ trụ",
                "|-1|Để ta nói cho nghe,người Sayan sau khi hồi phục sức mạnh sẽ tăng lên rất nhiều",
                "|-2|Tại sao ngươi lại lấy thân thể của songoku chứ?"
            }, //text chat 2

            new String[]{"|-1|Chúng ta sẽ gặp lại nhau sớm thôi",
                "|-2|Ngươi nói gì chứ?"}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );

    public static final BossData MABU = new BossData(
            "Mabư mập",
            ConstPlayer.XAYDA,
            new short[]{297, 298, 299, -1, -1, -1},
            50000,
            new long[]{500000000},
            new int[]{127},
            new int[][]{
                {Skill.KAMEJOKO, 3, 5000},
                {Skill.DRAGON, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo",
                "|-1|Úm ba la xì bùa"}, //text chat 2
            new String[]{"|-1|Biến hình"}, //text chat 3
            REST_10_M
    );

    public static final BossData SUPER_BU = new BossData(
            "Super Bư",
            ConstPlayer.XAYDA,
            new short[]{421, 422, 423, -1, -1, -1},
            50000,
            new long[]{500000000},
            new int[]{127, 128},
            new int[][]{
                {Skill.KAMEJOKO, 3, 5000},
                {Skill.DRAGON, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{"|-1|Biến hình"}, //text chat 3
            REST_10_M
    );

    public static final BossData BU_TENK = new BossData(
            "Bư Tênk",
            ConstPlayer.XAYDA,
            new short[]{424, 425, 426, -1, -1, -1},
            50000,
            new long[]{500000000},
            new int[]{127},
            new int[][]{
                {Skill.KAMEJOKO, 3, 5000},
                {Skill.DRAGON, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo",
                "|-1|Ui da đau bụng quá"}, //text chat 2
            new String[]{"|-1|Biến hình"}, //text chat 3
            REST_10_M
    );

    public static final BossData BU_HAN = new BossData(
            "Bư Han",
            ConstPlayer.XAYDA,
            new short[]{427, 428, 429, -1, -1, -1},
            50000,
            new long[]{500000000},
            new int[]{127},
            new int[][]{
                {Skill.KAMEJOKO, 3, 5000},
                {Skill.DRAGON, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{"|-1|Biến hình"}, //text chat 3
            REST_10_M
    );

    public static final BossData KID_BU = new BossData(
            "Kid Bư",
            ConstPlayer.XAYDA,
            new short[]{439, 440, 441, -1, -1, -1},
            50000,
            new long[]{500000000},
            new int[]{127},
            new int[][]{
                {Skill.KAMEJOKO, 3, 5000},
                {Skill.DRAGON, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{"|-1|Biến hình"}, //text chat 3
            REST_10_M
    );

    public static final BossData SUPER_BU_BUNG = new BossData(
            "Super Bư",
            ConstPlayer.XAYDA,
            new short[]{421, 422, 423, -1, -1, -1},
            50000,
            new long[]{500000000},
            new int[]{127, 128},
            new int[][]{
                {Skill.KAMEJOKO, 3, 5000},
                {Skill.DRAGON, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            REST_10_S
    );

    //========================BOSS 12H========================
    public static final BossData CADIC_12H = new BossData(
            "Ca Đíc", //name
            ConstPlayer.XAYDA, //gender
            new short[]{103, 16, 17, -1, 1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{5000000}, //hp
            new int[]{119}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.ANTOMIC, 7, 1000},},//skill
            new String[]{"|-1| ",
                "|-1|KhA'ng, cAn `in thoi m>i, xe m>i, nhA m>i thA sao",
                "|-1| ",
                "|-1| ",
                "|-1| "},
            new String[]{},
            new String[]{"|-1|Nghca lA sao nh%?"}, //text chat 3
            REST_5_M,
            new int[]{BossID.GOKU, BossID.CADIC_12H}
    );

    public static final BossData MABU_12H = new BossData(
            "Mabư",
            ConstPlayer.XAYDA,
            new short[]{297, 298, 299, -1, -1, -1},
            10000,
            new long[]{50000000},
            new int[]{120},
            new int[][]{
                {Skill.GALICK, 7, 1000}},
            new String[]{"|-1|Bư! Bư! Bư!",
                "|-1|Bư! Bư!",}, //text chat 1
            new String[]{"|-1|Oe Oe"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_M,
            new int[]{BossID.DRABURA_3}
    );

    public static final BossData GOKU = new BossData(
            "Gôcu", //name
            ConstPlayer.XAYDA, //gender
            new short[]{101, 65, 66, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{50000000}, //hp
            new int[]{119}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.KAMEJOKO, 7, 1000}},//skill
            new String[]{"|-1|Tỉnh lại đi Cađíc!",
                "|-1| ",
                "|-1|Đừng lùa gà nữa",
                "|-1|Một Server là quá đủ rồi!",
                "|-1|Các em chỉ có 10k ăn sáng thôi",
                "|-1| ",
                "|-1| ",
                "|-1| "}, //text chat 1
            new String[]{"|-1|Cađíc tỉnh lại đi!",
                "|-1|Trời ơi...Hắn định giết mọi người ở đây chắc?",
                "|-1|Cađíc! dừng tay lại! Cậu điên mất rồi!",
                "|-1|Cađíc tỉnh lại đi!",}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData CADIC = new BossData(
            "Ca Đít", //name
            ConstPlayer.XAYDA, //gender
            new short[]{103, 16, 17, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{50000000}, //hp
            new int[]{119}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.ANTOMIC, 7, 1000},},//skill
            new String[]{"|-1| ",
                "|-1|Không, còn điện thoại mới, xe mới, nhà mới thì sao",
                "|-1| ",
                "|-1| ",
                "|-1| ",
                "|-1|Khônggggggggggggggggggg!",
                "|-1|Tối nay ta chuẩn bị ăn gà ngập mồm rồi",
                "|-1| ",
                "|-1|Từ giờ hãy gọi ta là CađícMẹc"}, //text chat 1
            new String[]{"|-1|Chúng ta sẽ 1 mất 1 còn!",
                "|-1|Hãy xem đây",
                "|-1|Ngươi có một tội rất lớn là...",
                "|-1|Dám qua mặt ta",
                "|-1|Kakalốt! Ta chờ đợi giây phút này đã từ lâu!"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData DRABURA = new BossData(
            "Drabura",
            ConstPlayer.XAYDA,
            new short[]{418, 419, 420, -1, -1, -1},
            10000,
            new long[]{50000000},
            new int[]{114},
            new int[][]{
                {Skill.GALICK, 7, 1000}},
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{"|-1|Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi"}, //text chat 3
            REST_1_M
    );
    public static final BossData DRABURA_2 = new BossData(
            "Drabura",
            ConstPlayer.XAYDA,
            new short[]{418, 419, 420, -1, -1, -1},
            20000,
            new long[]{50000000},
            new int[]{119},
            new int[][]{
                {Skill.GALICK, 7, 1000}},
            new String[]{"|-1|Ta đã trở lại, lợi hại gấp hai, hahaha"}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{"|-1|Hêhê..ta chẳng cần tốn sức đánh với các ngươi nữa",
                "|-1|Mà ta sẽ để cho các ngươi tự thanh toán lẫn nhau, xin chào",
                "|-2|Hắn nói sẽ để chúng ta tự tiêu diệt lẫn nhau",
                "|-2|Nghĩa là sao nhỉ?"}, //text chat 3
            REST_5_M,
            new int[]{BossID.GOKU, BossID.CADIC}
    );

    public static final BossData DRABURA_3 = new BossData(
            "Drabura",
            ConstPlayer.XAYDA,
            new short[]{418, 419, 420, -1, -1, -1},
            10000,
            new long[]{50000000},
            new int[]{114},
            new int[][]{
                {Skill.GALICK, 7, 1000},},
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{"|-1|Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi"}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData BUI_BUI = new BossData(
            "Bui Bui",
            ConstPlayer.XAYDA,
            new short[]{451, 452, 453, -1, -1, -1},
            20000,
            new long[]{50000000},
            new int[]{115},
            new int[][]{
                {Skill.GALICK, 7, 10000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Hãy xem đây nhóc"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_M
    );

    public static final BossData BUI_BUI_2 = new BossData(
            "Bui Bui",
            ConstPlayer.XAYDA,
            new short[]{451, 452, 453, -1, -1, -1},
            20000,
            new long[]{50000000},
            new int[]{117},
            new int[][]{
                {Skill.GALICK, 7, 10000}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Trọng lực bây giờ đã tăng gấp 10 lần",
                "|-1|Đó là điều kiện không gian lý tưởng của ta",
                "|-1|Nhưng lại rất bất lợi cho bọn mi"}, //text chat 2
            new String[]{"|-1|Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi"}, //text chat 3
            REST_1_M
    );

    public static final BossData YACON = new BossData(
            "Ya côn",
            ConstPlayer.XAYDA,
            new short[]{415, 416, 417, -1, -1, -1},
            20000,
            new long[]{50000000},
            new int[]{118},
            new int[][]{
                {Skill.GALICK, 7, 100}},
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_M
    );

    public static final BossData SOI_HEC_QUYN = new BossData(
            "Sói hẹc quyn", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{394, 395, 396, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{100000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData O_DO = new BossData(
            "Ở dơ", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{400, 401, 402, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            3000, //dame
            new long[]{250000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData XINBATO = new BossData(
            "Xinbatô", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{359, 360, 361, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            6000, //dame
            new long[]{500000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData CHA_PA = new BossData(
            "Cha pa", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{362, 363, 364, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            9000, //dame
            new long[]{1000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );
    public static final BossData PON_PUT = new BossData(
            "Pon put", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{365, 366, 367, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            15000, //dame
            new long[]{2500000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData CHAN_XU = new BossData(
            "Chan xư", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{371, 372, 373, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20000, //dame
            new long[]{5000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData TAU_PAY_PAY = new BossData(
            "Tàu Pảy Pảy", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{92, 93, 94, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{20000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData YAMCHA = new BossData(
            "Yamcha", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{374, 375, 376, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            40000, //dame
            new long[]{200000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData JACKY_CHUN = new BossData(
            "Jacky Chun", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{356, 357, 358, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            50000, //dame
            new long[]{200000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData THIEN_XIN_HANG = new BossData(
            "Thiên xin hăng", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{368, 369, 370, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            100000, //dame
            new long[]{200000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );
    public static final BossData THIEN_XIN_HANG_CLONE = new BossData(
            "Thiên xin hăng", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{368, 369, 370, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            100000, //dame
            new long[]{200000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK, FULL_TDHS), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData LIU_LIU = new BossData(
            "Liu Liu", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{397, 398, 399, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            250000, //dame
            new long[]{200000000}, //hp
            new int[]{168}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_S //second rest
    );

    public static final BossData TAU_PAY_PAY_DONG_NAM_KARIN = new BossData(
            "Tàu Pảy Pảy", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{338, 339, 340, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10, //dame
            new long[]{10000}, //hp
            new int[]{111}, //map join
            (int[][]) Util.addArray(FULL_GALICK, FULL_KAMEJOKO), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_20_M //second rest
    );

    public static final BossData BUJIN = new BossData(
            "Bujin", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{341, 342, 343, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            17000, //dame
            new long[]{10000000}, //hp
            new int[]{3, 4, 5, 6, 27, 28, 29, 30}, //map join
            new int[][]{
                {Skill.DEMON, 7, 1000},
                {Skill.MASENKO, 7, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?", "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Các ngươi không nhúc nhích được sao?",
                "|-1|HAHAHAHA", "|-1|Chỉ là bọn con nít"
            }, //text chat 2
            new String[]{"|-1|Cay quá!",
                "|-1|Ta mà lại thua được sao?",
                "|-1|Hãy trả thù cho ta!"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData KOGU = new BossData(
            "Kogu", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{329, 330, 331, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            18000, //dame
            new long[]{10000000}, //hp
            new int[]{3, 4, 5, 6, 27, 28, 29, 30}, //map join
            new int[][]{
                {Skill.DRAGON, 7, 1000},
                {Skill.TROI, 4, 50000},
                {Skill.ANTOMIC, 4, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Trói"
            }, //text chat 2
            new String[]{"|-1|Cứu"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData ZANGYA = new BossData(
            "Zangya", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{332, 333, 334, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20720, //dame
            new long[]{10000000}, //hp
            new int[]{3, 4, 5, 6, 27, 28, 29, 30}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.TROI, 5, 50000},
                {Skill.ANTOMIC, 3, 3000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Trói"
            }, //text chat 2
            new String[]{"|-1|Cứu"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData BIDO = new BossData(
            "Bido", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{335, 336, 337, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            25000, //dame
            new long[]{10000000}, //hp
            new int[]{3, 4, 5, 6, 27, 28, 29, 30}, //map join
            new int[][]{
                {Skill.DRAGON, 7, 1000},
                {Skill.KAMEJOKO, 4, 10000},},//skill//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Oải rồi hả?", "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Một mình tao chấp hết tụi bây",
                "|-1|HAHAHAHA", "|-1|Chỉ là bọn con nít"
            }, //text chat 2
            new String[]{"|-1|Cay quá!"
            }, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData BOJACK = new BossData(
            "Bojack", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{323, 324, 325, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{10000000}, //hp
            new int[]{3, 4, 5, 6, 27, 28, 29, 30}, //map join
            new int[][]{
                {Skill.TROI, 7, 120000},
                {Skill.MASENKO, 7, 1000},
                {Skill.GALICK, 7, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Hahaha"
            }, //text chat 2
            new String[]{"|-1|Hahaha"
            }, //text chat 3
            REST_15_M,
            new int[]{BossID.BUJIN, BossID.KOGU, BossID.BIDO, BossID.ZANGYA,} //type appear
    );

    public static final BossData SUPER_BOJACK = new BossData(
            "Siêu Bojack", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{326, 327, 328, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{10000000}, //hp
            new int[]{3, 4, 5, 6, 27, 28, 29, 30}, //map join
            new int[][]{
                {Skill.THOI_MIEN, 7, 100000},
                {Skill.KHIEN_NANG_LUONG, 7, 100000},
                {Skill.GALICK, 7, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Hahaha"
            }, //text chat 2
            new String[]{"|-1|Hahaha"
            }, //text chat 3
            REST_15_M,
            AppearType.ANOTHER_LEVEL //type appear
    );

    public static final BossData SUPER_BOJACK_2 = new BossData(
            "Siêu Bojack", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{326, 327, 328, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{10000000}, //hp
            new int[]{3, 4, 5, 6, 27, 28, 29, 30}, //map join
            new int[][]{
                {Skill.TROI, 3, 60000},
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.GALICK, 7, 1000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Hahaha"
            }, //text chat 2
            new String[]{"|-1|Hahaha"
            }, //text chat 3
            REST_20_M
    );

    public static final BossData TAP_SU_0 = new BossData(
            "Tập sự-0", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{526, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{350000}, //hp
            new int[]{131}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 500},
                {Skill.ANTOMIC, 1, 500},
                {Skill.KAMEJOKO, 1, 500},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAP_SU_1 = new BossData(
            "Tập sự-1", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{526, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{350000}, //hp
            new int[]{131}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAP_SU_2 = new BossData(
            "Tập sự-2", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{526, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{350000}, //hp
            new int[]{131}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAP_SU_3 = new BossData(
            "Tập sự-3", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{526, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{350000}, //hp
            new int[]{131}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAP_SU_4 = new BossData(
            "Tập sự-4", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{526, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{350000}, //hp
            new int[]{131}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAN_BINH_5 = new BossData(
            "Tân binh-5", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{527, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{450000}, //hp
            new int[]{131}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S,
            new int[]{BossID.TAP_SU_0, BossID.TAP_SU_1, BossID.TAP_SU_2, BossID.TAP_SU_3, BossID.TAP_SU_4} //type appear
    );

    public static final BossData TAN_BINH_0 = new BossData(
            "Tân binh-0", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{527, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{450000}, //hp
            new int[]{132}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAN_BINH_1 = new BossData(
            "Tân binh-1", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{527, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{450000}, //hp
            new int[]{132}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAN_BINH_2 = new BossData(
            "Tân binh-2", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{527, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{450000}, //hp
            new int[]{132}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAN_BINH_3 = new BossData(
            "Tân binh-3", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{527, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{450000}, //hp
            new int[]{132}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData TAN_BINH_4 = new BossData(
            "Tân binh-4", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{527, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{450000}, //hp
            new int[]{132}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData CHIEN_BINH_5 = new BossData(
            "Chiến binh-5", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{528, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000}, //hp
            new int[]{132}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S,
            new int[]{BossID.TAN_BINH_0, BossID.TAN_BINH_1, BossID.TAN_BINH_2, BossID.TAN_BINH_3, BossID.TAN_BINH_4} //type appear
    );

    public static final BossData CHIEN_BINH_0 = new BossData(
            "Chiến binh-0", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{528, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000}, //hp
            new int[]{133}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData CHIEN_BINH_1 = new BossData(
            "Chiến binh-1", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{528, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000}, //hp
            new int[]{133}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData CHIEN_BINH_2 = new BossData(
            "Chiến binh-2", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{528, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000}, //hp
            new int[]{133}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData CHIEN_BINH_3 = new BossData(
            "Chiến binh-3", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{528, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000}, //hp
            new int[]{133}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData CHIEN_BINH_4 = new BossData(
            "Chiến binh-4", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{528, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000}, //hp
            new int[]{133}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER //type appear
    );

    public static final BossData DOI_TRUONG_5 = new BossData(
            "Đội trưởng-5", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{529, 525, 524, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{1000000}, //hp
            new int[]{133}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},
                {Skill.DEMON, 1, 1000},
                {Skill.GALICK, 1, 1000},
                {Skill.MASENKO, 1, 1000},
                {Skill.ANTOMIC, 1, 1000},
                {Skill.KAMEJOKO, 1, 1000},
                {Skill.LIEN_HOAN, 1, 1000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 30000},},//skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Khí công pháo"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S,
            new int[]{BossID.CHIEN_BINH_0, BossID.CHIEN_BINH_1, BossID.CHIEN_BINH_2, BossID.CHIEN_BINH_3, BossID.CHIEN_BINH_4} //type appear
    );

    public static final BossData KHIDOT = new BossData(
            "Khỉ đột", //name
            ConstPlayer.XAYDA, //gender
            new short[]{198, 193, 194, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{200000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.ANTOMIC, 7, 3000},
                {Skill.BIEN_KHI, 7, 60000},},//skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M //second rest
    );

    public static final BossData NGUYETTHAN = new BossData(
            "Nguyệt thần", //name
            ConstPlayer.XAYDA, //gender
            new short[]{2058, 2059, 2060, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{50000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M,//second rest
            new int[]{BossID.NHATTHAN}
    );

    public static final BossData NHATTTHAN = new BossData(
            "Nhật thần", //name
            ConstPlayer.XAYDA, //gender
            new short[]{2065, 2066, 2067, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{50000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData DRACULA = new BossData(
            "Đracula", //name
            ConstPlayer.XAYDA, //gender
            new short[]{353, 354, 355, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000000000, //dame
            new long[]{200000000}, //hp
            new int[]{112}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|He he he",
                "|-1|Ta sẽ xé xác ngươi ra thành trăm mảnh",
                "|-1|Xem các ngươi mạnh đến đâu"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S
    );

    public static final BossData NGUOI_VO_HINH = new BossData(
            "Người vô hình", //name
            ConstPlayer.XAYDA, //gender
            new short[]{377, 378, 379, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000000000, //dame
            new long[]{200000000}, //hp
            new int[]{112}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|He he he",
                "|-1|Ta sẽ xé xác ngươi ra thành trăm mảnh",
                "|-1|Xem các ngươi mạnh đến đâu"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S
    );

    public static final BossData BONG_BANG = new BossData(
            "Bông băng", //name
            ConstPlayer.XAYDA, //gender
            new short[]{350, 351, 352, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000000000, //dame
            new long[]{200000000}, //hp
            new int[]{112}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|He he he",
                "|-1|Ta sẽ xé xác ngươi ra thành trăm mảnh",
                "|-1|Xem các ngươi mạnh đến đâu"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S
    );

    public static final BossData VUA_QUY_SA_TANG = new BossData(
            "Vua Quỷ Sa tăng", //name
            ConstPlayer.XAYDA, //gender
            new short[]{344, 345, 346, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000000000, //dame
            new long[]{200000000}, //hp
            new int[]{112}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|He he he",
                "|-1|Ta sẽ xé xác ngươi ra thành trăm mảnh",
                "|-1|Xem các ngươi mạnh đến đâu"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S
    );

    public static final BossData THO_DAU_BAC = new BossData(
            "Thỏ Đầu Bạc", //name
            ConstPlayer.XAYDA, //gender
            new short[]{347, 348, 349, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000000000, //dame
            new long[]{200000000}, //hp
            new int[]{112}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|He he he",
                "|-1|Ta sẽ xé xác ngươi ra thành trăm mảnh",
                "|-1|Xem các ngươi mạnh đến đâu"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S
    );

    public static final BossData ODO = new BossData(
            "Ở dơ", //name
            ConstPlayer.XAYDA, //gender
            new short[]{400, 401, 402, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            100, //dame
            new long[]{500000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.GALICK, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, //text chat 1
            new String[]{"|-1|Khà khà"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S
    );
    public static final BossData CUMBER = new BossData(
            "Cumber", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1254, 1255, 1256, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000000}, //hp
            new int[]{155}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Cumber Vĩ Đại"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            REST_20_M //second rest
    );
    public static final BossData CUMBER_2 = new BossData(
            "Super Cumber", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1257, 1255, 1256, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{600000000}, //hp
            new int[]{155}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );
    public static final BossData CHILLER = new BossData(
            "Chilled", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1024, 1025, 1026, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20000, //dame
            new long[]{500000000}, //hp
            new int[]{161}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            REST_20_M //second rest
    );
    public static final BossData CHILLER_2 = new BossData(
            "Chilled 2", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1021, 1022, 1023, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20000, //dame
            new long[]{600000000}, //hp
            new int[]{161}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );
    public static final BossData FROST = new BossData(
            "Frost 1", //name
            ConstPlayer.XAYDA, //gender
            new short[]{493, 494, 495, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{300000000}, //hp
            new int[]{105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            REST_15_M //second rest
    );
    public static final BossData FROST_2 = new BossData(
            "Frost 2", //name
            ConstPlayer.XAYDA, //gender
            new short[]{496, 497, 498, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{320000000}, //hp
            new int[]{105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );
    public static final BossData FROST_3 = new BossData(
            "Frost 3", //name
            ConstPlayer.XAYDA, //gender
            new short[]{499, 500, 501, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{350000000}, //hp
            new int[]{105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );

    public static final BossData COOLER = new BossData(
            "Cooler", //name
            ConstPlayer.XAYDA, //gender
            new short[]{317, 318, 319, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20000, //dame
            new long[]{300000000}, //hp
            new int[]{105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!"
            }, //text chat 2
            new String[]{"|-1|Nãy giờ ta chưa thèm tung hết sức đâu",
                "|-1|Biến hình, hây aaaa..."}, //text chat 3
            REST_15_M //second rest
    );

    public static final BossData COOLER_2 = new BossData(
            "Cooler 2", //name
            ConstPlayer.XAYDA, //gender
            new short[]{320, 321, 322, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20000, //dame
            new long[]{350000000}, //hp
            new int[]{105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.GALICK, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Frost Demons"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            AppearType.ANOTHER_LEVEL //type appear
    );

    public static final BossData ONG_GIA_NOEL = new BossData(
            "Ông già Noel", //name
            ConstPlayer.XAYDA, //gender
            new short[]{657, 658, 659, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            5000000, //dame
            new long[]{500}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|Mé ri chịch mệt",
                "|-1|Hô hô hô",
                "|-1|Giáng sinh vui vẻ!",
                "|-1|Ngọc Rồng Online"}, //text chat 2
            new String[]{"|-1|Giáng sinh vui vẻ!",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            REST_1_M //type appear
    );

    public static final BossData THUY_TINH = new BossData(
            "Thủy Tinh", //name
            ConstPlayer.XAYDA, //gender
            new short[]{311, 312, 313, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{50000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{"|-1|Trả Mị Nương lại cho ta",
                "|-1|Ta cho nước dâng chìm cả lũ bây giờ"}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M,//second rest
            new int[]{BossID.SON_TINH}
    );
    public static final BossData GOGETA = new BossData(
            "Gogeta", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1390, 1391, 1392, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{2000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{"|-1|Trả Mị Nương lại cho ta",
                "|-1|Ta cho nước dâng chìm cả lũ bây giờ"}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M,//second rest
            new int[]{BossID.RONGSAO}
    );
    public static final BossData RONGSAO = new BossData(
            "Rồng 1 Sao", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1494, 1495, 1496, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{2000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{"|-1|Còn lâu á, chậm chân ráng chịu đi cưng",
                "|-1|Ta thách, chiêu này quá quen rồi"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );
    public static final BossData SON_TINH = new BossData(
            "Sơn Tinh", //name
            ConstPlayer.XAYDA, //gender
            new short[]{314, 315, 316, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{2000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{"|-1|Còn lâu á, chậm chân ráng chịu đi cưng",
                "|-1|Ta thách, chiêu này quá quen rồi"}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );
    public static final BossData BROLYSSJ3 = new BossData(
            "Broly SSJ3", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1482, 1483, 1484, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{2000000000}, //hp
            new int[]{184}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{"|-1|Trả Mị Nương lại cho ta",
                "|-1|Ta cho nước dâng chìm cả lũ bây giờ"}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M//second rest
    );

    public static final BossData SUPERXAYDAGOD = new BossData(
            "Super Xayda God", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1479, 1480, 1481, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{2000000000}, //hp
            new int[]{184}, //map join
            new int[][]{
                {Skill.GALICK, 1, 2000},
                {Skill.ANTOMIC, 1, 6000},},
            new String[]{}, //text chat 1
            new String[]{"|-1|Còn lâu á, chậm chân ráng chịu đi cưng",
                "|-1|Ta thách, chiêu này quá quen rồi"}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M
    );

    public static final BossData KARIN = new BossData(
            "Karin", //name
            ConstPlayer.XAYDA, //gender
            new short[]{89, 90, 91, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1, //dame
            new long[]{500}, //hp
            new int[]{46}, //map join
            new int[][]{
                {Skill.GALICK, 1, 1000}},
            new String[]{"|-1|Ta sẽ dạy ngươi vài chiêu",
                "|-1|Ta sẽ đánh hết sức, ngươi cẩn thận nhé"}, //text chat 1
            new String[]{"|-1|Haizzzzz",
                "|-1|Hahaha",
                "|-1|Xem đây"}, //text chat 2
            new String[]{"|-1|OK ta chịu thua"}, //text chat 3
            REST_1_S
    );

    public static final BossData YAJIRO = new BossData(
            "Yajirô", //name
            ConstPlayer.XAYDA, //gender
            new short[]{77, 78, 79, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1100, //dame
            new long[]{1100}, //hp
            new int[]{46}, //map join
            new int[][]{
                {Skill.GALICK, 1, 1000}},
            new String[]{"|-1|Ngon nhào vô",
                "|-1|Cho mi biết sự lợi hại của ta"}, //text chat 1
            new String[]{"|-1|Haizzzzz",
                "|-1|Hahaha",
                "|-1|Xem đây"}, //text chat 2
            new String[]{"|-1|Ngươi thật lợi hại"}, //text chat 3
            REST_1_S
    );

    public static final BossData MRPOPO = new BossData(
            "Mr.PôPô", //name
            ConstPlayer.XAYDA, //gender
            new short[]{77, 78, 79, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1100, //dame
            new long[]{5100}, //hp
            new int[]{46}, //map join
            new int[][]{{Skill.GALICK, 1, 30000},
            {Skill.KAMEJOKO, 1, 30000},
            {Skill.THAI_DUONG_HA_SAN, 3, 30000},},
            new String[]{"|-1|Đánh trúng ta 1 cái coi như ngươi thắng",
                "|-1|Đánh trúng ta 3 cái coi như ngươi thắng"}, //text chat 1
            new String[]{"|-1|Haizzzzz",
                "|-1|Hahaha",
                "|-1|Xem đây",
                "|-1|Phù.."}, //text chat 2
            new String[]{"|-1|Thua thì thua"}, //text chat 3
            REST_1_S
    );

    public static final BossData THUONG_DE = new BossData(
            "Thượng đế", //name
            ConstPlayer.XAYDA, //gender
            new short[]{86, 87, 88, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{1000}, //hp
            new int[]{49}, //map join
            new int[][]{
                {Skill.DRAGON, 1, 1000},},
            new String[]{"|-1|Ta sẽ dạy võ cho con trong phòng tập thời gian này",
                "|-1|Con hãy đánh hết sức nhé, ta sẽ không nương tay đâu"}, //text chat 1
            new String[]{"|-1|Haizzzzz",
                "|-1|Hahaha",
                "|-1|Xem đây"}, //text chat 2
            new String[]{"|-1|Ta rất tự hào về con"}, //text chat 3
            REST_1_S
    );

    public static final BossData KHI_BUBBLES = new BossData(
            "Khỉ Bubbles", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{95, 96, 97, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            30000, //dame
            new long[]{30000}, //hp
            new int[]{48}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.BIEN_KHI, 7, 1800000}}, //skill
            new String[]{"|-1|Ù ù khẹt khẹt",
                "|-1|Ù ù khẹt khẹt"}, //text chat 1
            new String[]{"|-1|Ù ù khẹt khẹt",
                "|-1|khẹt khẹt",
                "|-1|ù ù khẹc khẹc",
                "|-1|khẹc khẹc",
                "|-1|éc éc",}, //text chat 2
            new String[]{"|-1|Éc Éc Éc Éc!"}, //text chat 3
            REST_1_S //second rest
    );

    public static final BossData THAN_VU_TRU = new BossData(
            "Thần Vũ Trụ", //name
            ConstPlayer.XAYDA, //gender
            new short[]{98, 99, 100, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            45000, //dame
            new long[]{45000}, //hp
            new int[]{48}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.THAI_DUONG_HA_SAN, 7, 30000},},
            new String[]{"|-1|Ta sẽ dạy ngươi chiêu kaio-ken",
                "|-1|Ngươi cũng to gan lắm"}, //text chat 1
            new String[]{"|-1|Haizzzzz",
                "|-1|Hahaha",
                "|-1|Xem đây"}, //text chat 2
            new String[]{"|-1|Tại hôm nay ta...ta hơi bị đau bụng"}, //text chat 3
            REST_1_S
    );

    public static final BossData TO_SU_KAIO = new BossData(
            "Tổ sư Kaio", //name
            ConstPlayer.XAYDA, //gender
            new short[]{448, 449, 450, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            45000, //dame
            new long[]{45000}, //hp
            new int[]{50}, //map join
            new int[][]{
                {Skill.GALICK, 1, 60000},},
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_S
    );

    public static final BossData WHIS = new BossData(
            "Whis", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{838, 839, 840, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{2000000000}, //hp
            new int[]{154}, //map join
            new int[][]{
                {Skill.DRAGON, 7, 500}},//skill
            new String[]{"|-1|Ta sẽ dạy ngươi vài chiêu"}, //text chat 1
            new String[]{"|-1|Xem đây",
                "|-1|Haizzzzz",
                "|-1|Hahaha", "|-1|AAAAAAAAAA"}, //text chat 2
            new String[]{}, //text chat 3
            REST_1_M
    );

    public static final BossData GOLDEN_FRIEZA = new BossData(
            "Fide Vàng", //name
            ConstPlayer.XAYDA, //gender
            new short[]{502, 503, 504, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            100000000, //dame
            new long[]{2000000000}, //hp
            new int[]{6}, //map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.KAMEJOKO, 1, 1000}, {Skill.KAMEJOKO, 2, 1000}, {Skill.KAMEJOKO, 3, 1000}, {Skill.KAMEJOKO, 4, 1000}, {Skill.KAMEJOKO, 5, 1000}, {Skill.KAMEJOKO, 6, 1000}, {Skill.KAMEJOKO, 7, 1000},
                {Skill.MASENKO, 1, 1000}, {Skill.MASENKO, 2, 1000}, {Skill.MASENKO, 3, 1000}, {Skill.MASENKO, 4, 1000}, {Skill.MASENKO, 5, 1000}, {Skill.MASENKO, 6, 1000}, {Skill.MASENKO, 7, 1000},
                {Skill.ANTOMIC, 1, 1000}, {Skill.ANTOMIC, 2, 1000}, {Skill.ANTOMIC, 3, 1000}, {Skill.ANTOMIC, 4, 1000}, {Skill.ANTOMIC, 5, 1000}, {Skill.ANTOMIC, 6, 1000}, {Skill.ANTOMIC, 7, 1000},}, //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|He he he",
                "|-1|Ta sẽ xé xác ngươi ra thành trăm mảnh",
                "|-1|Xem các ngươi mạnh đến đâu"}, //text chat 2
            new String[]{}, //text chat 3
            REST_5_M,
            new int[]{BossID.DEATH_BEAM_1, BossID.DEATH_BEAM_2, BossID.DEATH_BEAM_3, BossID.DEATH_BEAM_4, BossID.DEATH_BEAM_5}
    );

    public static final BossData DEATH_BEAM = new BossData(
            "$", //name
            ConstPlayer.XAYDA, //gender
            new short[]{609, 610, 611, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1_000_000_000, //dame
            new long[]{2000000000}, //hp
            new int[]{6}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            AppearType.CALL_BY_ANOTHER
    );

    public static final BossData LAN_CON = new BossData(
            "Lân con", //name
            ConstPlayer.XAYDA, //gender
            new short[]{763, 764, 765, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            5000000, //dame
            new long[]{2000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|Tùng tùng xèng xèng"}, //text chat 2
            new String[]{"|-1|Ngọc Rồng Online"}, //text chat 3
            REST_1_M //type appear
    );
    public static final BossData TUAN_LOC = new BossData(
            "Tuần Lộc", //name
            ConstPlayer.XAYDA, //gender
            new short[]{718, 719, 720, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            5000000, //dame
            new long[]{2000000000}, //hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110}, //map join
            new int[][]{},
            new String[]{}, //text chat 1
            new String[]{"|-1|Ọc ọc ọc ọc ọc cái con cặc"}, //text chat 2
            new String[]{"|-1|Ngọc Rồng Online"}, //text chat 3
            REST_1_M //type appear
    );

    /*-------------------------------------------New Boss------------------------------------------------*/
    public static BossData SOI_HEC_QUYN_NEW = new BossData(
            "Sói hẹc quyn", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{394, 395, 396, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            1000, //dame
            new long[]{100}, //hp
            new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|À húuuuuuuuuu",
                "|-1|Ư a ư a đói quá",
                "|-1|Thèm núc xương quá"}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M //second rest
    );

    public static BossData O_DO_NEW = new BossData(
            "Ở dơ", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{400, 401, 402, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            3000, //dame
            new long[]{100}, //hp
            new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M //second rest
    );

    public static BossData XINBATO_NEW = new BossData(
            "Xinbatô", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{359, 360, 361, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            6000, //dame
            new long[]{100}, //hp
            new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, //map join
            (int[][]) Util.addArray(FULL_KAMEJOKO, FULL_GALICK), //skill
            new String[]{}, //text chat 1
            new String[]{"|-1|Xibala xibachao",
                "|-1|Ư a ư a khát nước quá",
                "|-1|Phân tâm quá"}, //text chat 2
            new String[]{}, //text chat 3
            REST_15_M //second rest
    );
    public static final BossData RONG_1_SAO = new BossData(
            "Rồng Nhí", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1625, 1626, 1627, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{2000000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, // map join
            new int[][]{
                {Skill.DRAGON, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, // text chat 1
            new String[]{"|-1|Ngọc Rồng Online mãi đỉnh !!"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData RONG_2_SAO = new BossData(
            "Rồng Nhí", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1628, 1629, 1630, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{2000000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, // map join
            new int[][]{
                {Skill.DRAGON, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, // text chat 1
            new String[]{"|-1|Ngọc Rồng Online mãi đỉnh !!"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData RONG_3_SAO = new BossData(
            "Rồng Nhí", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1631, 1632, 1633, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{2000000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, // map join
            new int[][]{
                {Skill.DRAGON, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, // text chat 1
            new String[]{"|-1|Ngọc Rồng Online mãi đỉnh !!"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData RONG_4_SAO = new BossData(
            "Rồng Nhí", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1634, 1635, 1636, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{2000000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, // map join
            new int[][]{
                {Skill.DRAGON, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, // text chat 1
            new String[]{"|-1|Ngọc Rồng Online mãi đỉnh !!"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData RONG_5_SAO = new BossData(
            "Rồng Nhí", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1637, 1638, 1639, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{2000000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, // map join
            new int[][]{
                {Skill.DRAGON, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, // text chat 1
            new String[]{"|-1|Ngọc Rồng Online mãi đỉnh !!"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData RONG_6_SAO = new BossData(
            "Rồng Nhí", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1640, 1641, 1642, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{2000000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, // map join
            new int[][]{
                {Skill.DRAGON, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, // text chat 1
            new String[]{"|-1|Ngọc Rồng Online mãi đỉnh !!"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData RONG_7_SAO = new BossData(
            "Rồng Nhí", // name
            ConstPlayer.XAYDA, // gender
            new short[]{1643, 1644, 1645, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            5000000, // dame
            new long[]{2000000000}, // hp
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, // map join
            new int[][]{
                {Skill.DRAGON, 7, Util.nextInt(5000, 10000)}},
            new String[]{}, // text chat 1
            new String[]{"|-1|Ngọc Rồng Online mãi đỉnh !!"}, // text chat 2
            new String[]{}, // text chat 3
            REST_1_M // type appear
    );
    public static final BossData POCOLONE = new BossData(
            "Đại Vương Pocolo", //name
            ConstPlayer.XAYDA, //gender
            new short[]{1652, 1653, 1654, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            5000000, //dame
            new long[]{2_000_000_000}, //hp
            new int[]{146}, //map join
            new int[][]{
                {Skill.MASENKO, 7, 10000},
                {Skill.DEMON, 7, 1000}}, //skill
            new String[]{"|-1|Ta sẽ cho chúng bây biết sức mạnh thực sự của dân tộc Cumber Vĩ Đại"
            }, //text chat 1
            new String[]{"|-1|Tụi mày có giỏi thì xông vào cứu hắn đi",
                "|-1|Trận địa pháo mini",
                "|-1|Nên giải quyết con sâu nào trước đây",
                "|-1|HA HA HA",
                "|-1|Ta chính là Vũ Trụ Đệ Nhất Cao Thủ",
                "|-1|Xí hụt ha ha",
                "|-1|Ta đã giấu hết ngọc rồng rồi, các ngươi tìm vô ích hahaha",
                "|-1|Chúng mày nghĩ kiến lại thắng nổi khủng long sao",
                "|-1|Hô hô hô",
                "|-1|Được thôi, nếu muốn chết đến vậy, ta rất vui lòng!!",}, //text chat 2
            new String[]{"|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Mọi chuyện chưa kết thúc đâu",
                "|-1|Ngọc Rồng Online"}, //text chat 3
            REST_15_M //second rest
    );
    public static final BossData HIT = new BossData(
            "Hit Bóng Tối", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{520, 521, 522, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            180000, //dame
            new long[]{100000000}, //hp
            new int[]{102, 92, 93, 94, 96, 97, 98, 99, 100}, //map join
            (int[][]) Util.addArray(FULL_GALICK, FULL_KAMEJOKO, FULL_TDHS), //skill
            new String[]{"|-1|Nro"
            }, //text chat 1
            new String[]{"|-1|Nro!"
            }, //text chat 2
            new String[]{"|-1|Nro"}, //text chat 3
            REST_10_M //second rest
    );
    public static final BossData KOCHIRO = new BossData(
            "Kochiro", // name
            ConstPlayer.TRAI_DAT, // gender
            new short[]{1716, 1717, 1718, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            10000, // dame
            new long[]{500}, // hp
            new int[]{1, 2, 3, 4, 6, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 42, 43, 44}, // map join
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.ANTOMIC, 3, 3000},}, // skill//skill
            new String[]{}, // text chat 1
            new String[]{"|-1|Mấy nhóc k nạp tiền mà pem tao à?", "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Nếu người thành tâm muốn biết?",
                "|-1|Thì ta cũng đút buồi trả lời hahaha", "|-1|Chỉ là bọn cứt cứt k nạp tiền đòi đánh ta"
            }, // text chat 2
            new String[]{"|-1|Hãm lồn thật sự!"
            }, // text chat 3
            AppearType.APPEAR_WITH_ANOTHER // type appear
    );

    public static final BossData MUSASHI = new BossData(
            "Musashi", // name
            ConstPlayer.TRAI_DAT, // gender
            new short[]{1719, 1720, 1721, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            10000, // dame
            new long[]{500}, // hp
            new int[]{1, 2, 3, 4, 6, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 42, 43, 44}, // map join
            new int[][]{
                {Skill.DRAGON, 7, 1000},
                {Skill.KAMEJOKO, 4, 10000},}, // skill//skill
            new String[]{}, // text chat 1
            new String[]{"|-1|Mấy nhóc k nạp tiền mà pem tao à?", "|-1|Ê cố lên nhóc",
                "|-1|Chán",
                "|-1|Nếu người thành tâm muốn biết?",
                "|-1|Thì ta cũng đút buồi trả lời hahaha", "|-1|Chỉ là bọn cứt cứt k nạp tiền đòi đánh ta"
            }, // text chat 2
            new String[]{"|-1|Hãm lồn thật sự!"
            }, // text chat 3
            AppearType.APPEAR_WITH_ANOTHER // type appear
    );

    public static final BossData MEOWTH = new BossData(
            "Meowth", // name
            ConstPlayer.TRAI_DAT, // gender
            new short[]{1722, 1723, 1724, -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}
            10000, // dame
            new long[]{500}, // hp
            new int[]{1, 2, 3, 4, 6, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 42, 43, 44}, // map join
            new int[][]{
                {Skill.MASENKO, 7, 1000},
                {Skill.GALICK, 7, 1000},}, // skill
            new String[]{}, // text chat 1
            new String[]{"|-1|Hahaha"
            }, // text chat 2
            new String[]{"|-1|Hahaha"
            }, // text chat 3
            REST_15_M,
            new int[]{BossID.KOCHIRO, BossID.MUSASHI,} // type appear
    );

    //========================VO DAI SIEU CAP (map 145)========================
    public static final BossData VD_CHAMPA = new BossData(
            "Champa", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{511, 512, 513, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20_000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 10000},}, //skill
            new String[]{"|-1|Ngươi dám thách thức ta sao?"}, //text chat 1
            new String[]{"|-1|Hahaha", "|-1|Yếu thế thôi à?"}, //text chat 2
            new String[]{"|-1|Khốn kiếp..."}, //text chat 3
            REST_20_M,
            new int[]{BossID.VD_VADOS, BossID.VD_BILL, BossID.VD_WHIS, BossID.VD_DAI_THIEN_SU}
    );

    public static final BossData VD_VADOS = new BossData(
            "Vados", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{530, 531, 532, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20_000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THOI_MIEN, 1, 60000},
                {Skill.LIEN_HOAN, 7, 1000},}, //skill
            new String[]{"|-1|Hộ vệ Champa, mời ngươi giao chiến!"}, //text chat 1
            new String[]{"|-1|Cố lên nhóc", "|-1|Chán quá"}, //text chat 2
            new String[]{"|-1|Champa-sama..."}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData VD_BILL = new BossData(
            "Beerus", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{508, 509, 510, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20_000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 10000},}, //skill
            new String[]{"|-1|Thần hủy diệt đã đến!"}, //text chat 1
            new String[]{"|-1|Hakai!", "|-1|Ngươi sẽ bị hủy diệt"}, //text chat 2
            new String[]{"|-1|Không thể tin được..."}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData VD_WHIS = new BossData(
            "Whis", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{838, 839, 840, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20_000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.THOI_MIEN, 1, 60000},
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.LIEN_HOAN, 7, 1000},}, //skill
            new String[]{"|-1|Beerus-sama, để con xử lý tụi nhỏ này"}, //text chat 1
            new String[]{"|-1|Hahaha", "|-1|Khá lắm"}, //text chat 2
            new String[]{"|-1|Trời ơi..."}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData VD_DAI_THIEN_SU = new BossData(
            "Đại Thiên Sứ", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{703, 704, 705, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            20_000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 500},
                {Skill.MASENKO, 7, 1000},
                {Skill.LIEN_HOAN, 7, 1000},
                {Skill.ANTOMIC, 7, 2000},}, //skill
            new String[]{"|-1|Các ngươi đã đi quá giới hạn rồi"}, //text chat 1
            new String[]{"|-1|Sức mạnh tuyệt đối", "|-1|Hãy biết phận của mình"}, //text chat 2
            new String[]{"|-1|Thật bất ngờ..."}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );
    public static final BossData ZENO = new BossData(
            "Zeno", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{2245, 2246, 2247, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10_000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 1000},
                {Skill.ANTOMIC, 7, 2000},
                {Skill.THAI_DUONG_HA_SAN, 7},
                {Skill.LIEN_HOAN, 7, 10000},}, //skill
            new String[]{"|-1|Kẻ nào dám gây loạn trước mặt Vua của tất cả?"}, //text chat 1
            new String[]{"|-1|Thú vị đấy", "|-1|Hộ vệ, ra tay đi"}, //text chat 2
            new String[]{"|-1|Ngươi... khá lắm"}, //text chat 3
            REST_20_M,
            new int[]{BossID.HAU_VE_1, BossID.HAU_VE_2}
    );

    public static final BossData HAU_VE_1 = new BossData(
            "Hộ vệ Zeno 1", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{2218, 2219, 2220, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},}, //skill
            new String[]{"|-1|Đã có ta bảo vệ Ngài Zeno!"}, //text chat 1
            new String[]{"|-1|Lui lại!", "|-1|Không ai được lại gần"}, //text chat 2
            new String[]{"|-1|Zeno-sama, thứ lỗi..."}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    public static final BossData HAU_VE_2 = new BossData(
            "Hộ vệ Zeno 2", //name
            ConstPlayer.TRAI_DAT, //gender
            new short[]{2218, 2219, 2220, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
            10000, //dame
            new long[]{500000000}, //hp
            new int[]{145}, //map join
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},}, //skill
            new String[]{"|-1|Bảo vệ Ngài Zeno bằng mọi giá!"}, //text chat 1
            new String[]{"|-1|Đừng hòng!", "|-1|Ta sẽ chặn ngươi"}, //text chat 2
            new String[]{"|-1|Ta đã làm hết sức..."}, //text chat 3
            AppearType.APPEAR_WITH_ANOTHER
    );

    //************************************************************************** Boss Halloween
    public static final BossData MA_TROI = new BossData(
            "Ma Trơi",
            ConstPlayer.TRAI_DAT,
            new short[]{651, 652, 653, -1, -1, -1},
            20000,
            new long[]{10000000},
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25,
                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70,
                71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99,
                100, 102, 103, 104, 105, 106, 107, 108, 109, 110},
            new int[][]{
                {Skill.DRAGON, 7, 1000},
                {Skill.KAMEJOKO, 7, 2000},
                {Skill.THAI_DUONG_HA_SAN, 7, 15000},
                {Skill.LIEN_HOAN, 7, 2000}
            },
            new String[]{"|-1|Halloween đến rồi, ai sẽ là nạn nhân tiếp theo của ta?", "|-1|Khè khè... đêm nay ai dám đối đầu với Ma Trơi?"},
            new String[]{"|-1|Linh hồn của ngươi sẽ thuộc về ta!", "|-1|Ngươi không thoát khỏi bóng tối đâu!"},
            new String[]{"|-1|Ta sẽ trở lại vào đêm mai...", "|-1|Ngươi khá lắm, hẹn gặp lại ở cõi âm!"},
            REST_15_M
    );

    public static final BossData DOI_MA_CA_RONG = new BossData(
            "Dơi Ma Cà Rồng",
            ConstPlayer.XAYDA,
            new short[]{654, 655, 656, -1, -1, -1},
            30000,
            new long[]{10000000},
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25,
                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70,
                71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99,
                100, 102, 103, 104, 105, 106, 107, 108, 109, 110},
            new int[][]{
                {Skill.GALICK, 7, 1000},
                {Skill.ANTOMIC, 7, 2000},
                {Skill.MASENKO, 7, 3000},
                {Skill.LIEN_HOAN, 7, 1500}
            },
            new String[]{"|-1|Mùi máu tươi thơm ngon quá!", "|-1|Bá tước Dracula đã thức tỉnh!"},
            new String[]{"|-1|Hãy nếm thử nanh vuốt của ma cà rồng!", "|-1|Máu của ngươi sẽ giúp ta trường sinh!"},
            new String[]{"|-1|Ánh sáng mặt trời... chói chang quá!", "|-1|Ta sẽ tan biến vào bóng đêm..."},
            REST_15_M
    );

    public static final BossData BI_MA = new BossData(
            "Bí Ma",
            ConstPlayer.NAMEC,
            new short[]{760, 761, 762, -1, -1, -1},
            40000,
            new long[]{10000000},
            new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25,
                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 63, 64, 65, 66, 67, 68, 69, 70,
                71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 92, 93, 94, 96, 97, 98, 99,
                100, 102, 103, 104, 105, 106, 107, 108, 109, 110, 168},
            new int[][]{
                {Skill.DEMON, 7, 1000},
                {Skill.MASENKO, 7, 2000},
                {Skill.KAIOKEN, 7, 10000},
                {Skill.DICH_CHUYEN_TUC_THOI, 7, 15000},
                {Skill.LIEN_HOAN, 7, 2000}
            },
            new String[]{"|-1|Ha ha ha! Quỷ Bí Ngô đã giáng trần!", "|-1|Halloween rùng rợn đang chờ các ngươi!"},
            new String[]{"|-1|Kẹo hay bị ghẹo?", "|-1|Ta là chúa tể của những quả bí ngô ma quái!"},
            new String[]{"|-1|Không thể nào... quả bí của ta bị vỡ rồi...", "|-1|Hãy đợi đấy, Halloween năm sau ta sẽ trở lại!"},
            REST_15_M
    );

    //************************************************************************** Boss Địa Ngục
    public static final BossData JANEMBA_DIA_NGUC = new BossData(
            "Janemba Địa Ngục",
            ConstPlayer.NAMEC,
            new short[]{344, 345, 346, -1, -1, -1},
            30000,
            new long[]{1000000000L},
            new int[]{174, 179, 180},
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},},
            new String[]{"|-1|Bọn sâu bọ dám xâm nhập Địa Ngục sao?", "|-1|Ta là Janemba - Chúa tể của cõi chết!"},
            new String[]{"|-1|Địa ngục sẽ là mồ chôn của các ngươi!", "|-1|Hãy nếm thử sức mạnh của quỷ dữ!", "|-1|Ngươi không thể thoát khỏi Địa Ngục đâu!"},
            new String[]{"|-1|Không thể nào... ta bị đánh bại ở chính lãnh địa của mình...", "|-1|Địa Ngục... sẽ nuốt chửng các ngươi..."},
            REST_20_M
    );

    //************************************************************************** Boss Dragon Ball Super
    public static final BossData GOHAN_BEAST = new BossData(
            "Gohan Beast",
            ConstPlayer.TRAI_DAT,
            new short[]{1655, 1658, 1659, -1, -1, -1},
            30000,//dame boss gây ra 
            new long[]{1000000000L},
            new int[]{174, 179, 180},
           new int[][]{//skill boss dùng
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},},
            new String[]{"|-1|Ta sẽ bảo vệ mọi người bằng tất cả sinh mạng!", "|-1|Hãy nếm thử sức mạnh của Gohan Beast!"},
            new String[]{"|-1|Ma Không Pháo (Makankosappo)!!", "|-1|Ngươi không theo kịp tốc độ của ta đâu!", "|-1|Hây aaaaa!"},
            new String[]{"|-1|Sức mạnh này... vẫn chưa đạt tới giới hạn...", "|-1|Ta sẽ trở lại mạnh mẽ hơn!"},
            REST_20_M
    );

    public static final BossData CELL_MAX = new BossData(
            "Cell Max",
            ConstPlayer.XAYDA,
            new short[]{231, 232, 233, -1, -1, -1},
            35000,
            new long[]{1000000000L},
            new int[]{174, 179, 180},
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},},
            new String[]{"|-1|ROOOAAARRRR!!", "|-1|HUỶ DIỆT! TA SẼ TIÊU DIỆT TẤT CẢ!"},
            new String[]{"|-1|GRAAAAA! CHẾT HẾT ĐI LŨ PHÀM NHÂN!", "|-1|KHÔNG AI CÓ THỂ NGĂN CẢN ĐƯỢC CELL MAX!", "|-1|BÙM BÙM BÙM!"},
            new String[]{"|-1|KHÔÔÔÔNNGGG!! ĐIỂM YẾU TRÊN ĐẦU CỦA TA...", "|-1|TA SẼ TỰ PHÁ HUỶ TOÀN BỘ..."},
            REST_20_M
    );

    public static final BossData BROLY_SUPER = new BossData(
            "Broly Cương Nộ",
            ConstPlayer.XAYDA,
            new short[]{2177, 2178, 2179, -1, -1, -1},
            32000,
            new long[]{1000000000L},
            new int[]{174, 179, 180},
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},},
            new String[]{"|-1|Kakarot... KAKAROTTTT!", "|-1|Ta sẽ nghiền nát tất cả các ngươi!"},
            new String[]{"|-1|GRAAAAA! SỨC MẠNH VÔ HẠN CỦA CHIẾN BINH HUYỀN THOẠI!", "|-1|Chết đi lũ sâu bọ!", "|-1|Graaaaaaaa!"},
            new String[]{"|-1|Hự... ta đau đầu quá...", "|-1|Một ngày nào đó ta sẽ kiểm soát được sức mạnh này!"},
            REST_20_M
    );

    public static final BossData BLACK_FRIEZA = new BossData(
            "Black Frieza",
            ConstPlayer.XAYDA,
            new short[]{2224, 2225, 2226, -1, -1, -1},
            35000,
            new long[]{1000000000L},
            new int[]{174, 179, 180},
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},},
            new String[]{"|-1|Hô hô hô, 10 năm khổ luyện trong Phòng Tập Thời Gian...", "|-1|Các ngươi chuẩn bị chứng kiến dạng tối thượng: Black Frieza!"},
            new String[]{"|-1|Chỉ một đòn là đủ tiễn cả Goku và Vegeta!", "|-1|Quá yếu ớt! Hoàng đế vũ trụ là bất khả chiến bại!", "|-1|Chết đi trong tuyệt vọng nào!"},
            new String[]{"|-1|Không thể nào! Hoàng đế như ta sao có thể thất bại...", "|-1|Các ngươi hãy đợi đấy, ta sẽ quay lại hủy diệt tất cả!"},
            REST_20_M
    );

    public static final BossData MORO = new BossData(
            "Phù Thủy Moro",
            ConstPlayer.NAMEC,
            new short[]{1383, 1386, 1387, -1, -1, -1},
            30000,
            new long[]{1000000000L},
            new int[]{174, 179, 180},
            new int[][]{
                {Skill.KAMEJOKO, 7, 10000},
                {Skill.THAI_DUONG_HA_SAN, 3},
                {Skill.LIEN_HOAN, 7, 1000},},
            new String[]{"|-1|Khí và sinh lực của hành tinh này... đều thuộc về ta!", "|-1|Lũ phàm trần các ngươi chỉ là thức ăn bồi bổ ma thuật của ta!"},
            new String[]{"|-1|Càng chiến đấu, ta càng hấp thụ thêm nhiều năng lượng!", "|-1|Hahaha, các ngươi cảm thấy sức mạnh đang cạn dần chưa?", "|-1|Ma thuật cổ đại vô địch!"},
            new String[]{"|-1|Năng lượng... ta cần thêm sinh lực...", "|-1|Ma pháp của ta không thể bị dập tắt!"},
            REST_20_M
    );
}

