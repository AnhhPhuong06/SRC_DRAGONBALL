-- 129 Spine portrait items for SuperVIP-SCR-ByBen.
-- Item IDs: 2048-2176; appearance IDs: 10000-10128.
-- Matches the Agnilasa migration schema: part and leg contain the appearance ID.
-- No cai_trang, gold_sell, gem_sell, ruby_sell, TypeEvent, or isGender columns are used.
-- Safe to re-run: existing item IDs are never overwritten.
SET NAMES utf8mb4;
START TRANSACTION;

-- Item 2048: appearance 10000, B30001 (Pilaf Variant).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2048, 5, 3, 'Spine Pilaf Variant <spine=10000>', 'Spine portrait appearance 10000 (B30001).', 0, 2127, 10000, 0, 0, 0, 0, -1, -1, 10000
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2048);

-- Item 2049: appearance 10001, B30002 (Bardock).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2049, 5, 3, 'Spine Bardock <spine=10001>', 'Spine portrait appearance 10001 (B30002).', 0, 2127, 10001, 0, 0, 0, 0, -1, -1, 10001
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2049);

-- Item 2050: appearance 10002, B30003 (WestKaioshin).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2050, 5, 3, 'Spine WestKaioshin <spine=10002>', 'Spine portrait appearance 10002 (B30003).', 0, 2127, 10002, 0, 0, 0, 0, -1, -1, 10002
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2050);

-- Item 2051: appearance 10003, B30004 (TienShinhan).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2051, 5, 3, 'Spine TienShinhan <spine=10003>', 'Spine portrait appearance 10003 (B30004).', 0, 2127, 10003, 0, 0, 0, 0, -1, -1, 10003
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2051);

-- Item 2052: appearance 10004, B30005 (CaptainGinyu).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2052, 5, 3, 'Spine CaptainGinyu <spine=10004>', 'Spine portrait appearance 10004 (B30005).', 0, 2127, 10004, 0, 0, 0, 0, -1, -1, 10004
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2052);

-- Item 2053: appearance 10005, B30006 (Nappa IronHead).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2053, 5, 3, 'Spine Nappa IronHead <spine=10005>', 'Spine portrait appearance 10005 (B30006).', 0, 2127, 10005, 0, 0, 0, 0, -1, -1, 10005
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2053);

-- Item 2054: appearance 10006, B30007 (Nappa IronBone).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2054, 5, 3, 'Spine Nappa IronBone <spine=10006>', 'Spine portrait appearance 10006 (B30007).', 0, 2127, 10006, 0, 0, 0, 0, -1, -1, 10006
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2054);

-- Item 2055: appearance 10007, B30008 (MrSatan Hero).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2055, 5, 3, 'Spine MrSatan Hero <spine=10007>', 'Spine portrait appearance 10007 (B30008).', 0, 2127, 10007, 0, 0, 0, 0, -1, -1, 10007
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2055);

-- Item 2056: appearance 10008, B30009 (Recoome).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2056, 5, 3, 'Spine Recoome <spine=10008>', 'Spine portrait appearance 10008 (B30009).', 0, 2127, 10008, 0, 0, 0, 0, -1, -1, 10008
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2056);

-- Item 2057: appearance 10009, B30010 (Android21).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2057, 5, 3, 'Spine Android21 <spine=10009>', 'Spine portrait appearance 10009 (B30010).', 0, 2127, 10009, 0, 0, 0, 0, -1, -1, 10009
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2057);

-- Item 2058: appearance 10010, B30011 (Portrait B30011).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2058, 5, 3, 'Spine Portrait B30011 <spine=10010>', 'Spine portrait appearance 10010 (B30011).', 0, 2127, 10010, 0, 0, 0, 0, -1, -1, 10010
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2058);

-- Item 2059: appearance 10011, B30012 (Oolong).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2059, 5, 3, 'Spine Oolong <spine=10011>', 'Spine portrait appearance 10011 (B30012).', 0, 2127, 10011, 0, 0, 0, 0, -1, -1, 10011
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2059);

-- Item 2060: appearance 10012, B30013 (Bardock).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2060, 5, 3, 'Spine Bardock <spine=10012>', 'Spine portrait appearance 10012 (B30013).', 0, 2127, 10012, 0, 0, 0, 0, -1, -1, 10012
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2060);

-- Item 2061: appearance 10013, B30014 (Nappa IronArmor).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2061, 5, 3, 'Spine Nappa IronArmor <spine=10013>', 'Spine portrait appearance 10013 (B30014).', 0, 2127, 10013, 0, 0, 0, 0, -1, -1, 10013
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2061);

-- Item 2062: appearance 10014, B30015 (Ribrianne).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2062, 5, 3, 'Spine Ribrianne <spine=10014>', 'Spine portrait appearance 10014 (B30015).', 0, 2127, 10014, 0, 0, 0, 0, -1, -1, 10014
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2062);

-- Item 2063: appearance 10015, B30016 (Videl).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2063, 5, 3, 'Spine Videl <spine=10015>', 'Spine portrait appearance 10015 (B30016).', 0, 2127, 10015, 0, 0, 0, 0, -1, -1, 10015
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2063);

-- Item 2064: appearance 10016, B30017 (Bulma).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2064, 5, 3, 'Spine Bulma <spine=10016>', 'Spine portrait appearance 10016 (B30017).', 0, 2127, 10016, 0, 0, 0, 0, -1, -1, 10016
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2064);

-- Item 2065: appearance 10017, B30018 (Portrait B30018).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2065, 5, 3, 'Spine Portrait B30018 <spine=10017>', 'Spine portrait appearance 10017 (B30018).', 0, 2127, 10017, 0, 0, 0, 0, -1, -1, 10017
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2065);

-- Item 2066: appearance 10018, B30019 (SonGoku SS3).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2066, 5, 3, 'Spine SonGoku SS3 <spine=10018>', 'Spine portrait appearance 10018 (B30019).', 0, 2127, 10018, 0, 0, 0, 0, -1, -1, 10018
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2066);

-- Item 2067: appearance 10019, B30020 (SonGoku Kid).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2067, 5, 3, 'Spine SonGoku Kid <spine=10019>', 'Spine portrait appearance 10019 (B30020).', 0, 2127, 10019, 0, 0, 0, 0, -1, -1, 10019
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2067);

-- Item 2068: appearance 10020, B30021 (SonGohan).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2068, 5, 3, 'Spine SonGohan <spine=10020>', 'Spine portrait appearance 10020 (B30021).', 0, 2127, 10020, 0, 0, 0, 0, -1, -1, 10020
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2068);

-- Item 2069: appearance 10021, B30022 (Portrait B30022).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2069, 5, 3, 'Spine Portrait B30022 <spine=10021>', 'Spine portrait appearance 10021 (B30022).', 0, 2127, 10021, 0, 0, 0, 0, -1, -1, 10021
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2069);

-- Item 2070: appearance 10022, B30025 (BabaWitch).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2070, 5, 3, 'Spine BabaWitch <spine=10022>', 'Spine portrait appearance 10022 (B30025).', 0, 2127, 10022, 0, 0, 0, 0, -1, -1, 10022
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2070);

-- Item 2071: appearance 10023, B30026 (Whis).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2071, 5, 3, 'Spine Whis <spine=10023>', 'Spine portrait appearance 10023 (B30026).', 0, 2127, 10023, 0, 0, 0, 0, -1, -1, 10023
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2071);

-- Item 2072: appearance 10024, B30027 (SouthKaioshin).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2072, 5, 3, 'Spine SouthKaioshin <spine=10024>', 'Spine portrait appearance 10024 (B30027).', 0, 2127, 10024, 0, 0, 0, 0, -1, -1, 10024
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2072);

-- Item 2073: appearance 10025, B30028 (Nappa IronFist).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2073, 5, 3, 'Spine Nappa IronFist <spine=10025>', 'Spine portrait appearance 10025 (B30028).', 0, 2127, 10025, 0, 0, 0, 0, -1, -1, 10025
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2073);

-- Item 2074: appearance 10026, B30029 (Pilaf Variant).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2074, 5, 3, 'Spine Pilaf Variant <spine=10026>', 'Spine portrait appearance 10026 (B30029).', 0, 2127, 10026, 0, 0, 0, 0, -1, -1, 10026
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2074);

-- Item 2075: appearance 10027, B30030 (Portrait B30030).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2075, 5, 3, 'Spine Portrait B30030 <spine=10027>', 'Spine portrait appearance 10027 (B30030).', 0, 2127, 10027, 0, 0, 0, 0, -1, -1, 10027
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2075);

-- Item 2076: appearance 10028, B30031 (Vegito).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2076, 5, 3, 'Spine Vegito <spine=10028>', 'Spine portrait appearance 10028 (B30031).', 0, 2127, 10028, 0, 0, 0, 0, -1, -1, 10028
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2076);

-- Item 2077: appearance 10029, B30032 (MrSatan Star).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2077, 5, 3, 'Spine MrSatan Star <spine=10029>', 'Spine portrait appearance 10029 (B30032).', 0, 2127, 10029, 0, 0, 0, 0, -1, -1, 10029
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2077);

-- Item 2078: appearance 10030, B30034 (MrSatan Dark).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2078, 5, 3, 'Spine MrSatan Dark <spine=10030>', 'Spine portrait appearance 10030 (B30034).', 0, 2127, 10030, 0, 0, 0, 0, -1, -1, 10030
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2078);

-- Item 2079: appearance 10031, B30036 (ChiChi).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2079, 5, 3, 'Spine ChiChi <spine=10031>', 'Spine portrait appearance 10031 (B30036).', 0, 2127, 10031, 0, 0, 0, 0, -1, -1, 10031
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2079);

-- Item 2080: appearance 10032, B30037 (MrSatan Iron).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2080, 5, 3, 'Spine MrSatan Iron <spine=10032>', 'Spine portrait appearance 10032 (B30037).', 0, 2127, 10032, 0, 0, 0, 0, -1, -1, 10032
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2080);

-- Item 2081: appearance 10033, B30039 (Burter).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2081, 5, 3, 'Spine Burter <spine=10033>', 'Spine portrait appearance 10033 (B30039).', 0, 2127, 10033, 0, 0, 0, 0, -1, -1, 10033
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2081);

-- Item 2082: appearance 10034, B30040 (GokuBlack).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2082, 5, 3, 'Spine GokuBlack <spine=10034>', 'Spine portrait appearance 10034 (B30040).', 0, 2127, 10034, 0, 0, 0, 0, -1, -1, 10034
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2082);

-- Item 2083: appearance 10035, B30041 (Piccolo).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2083, 5, 3, 'Spine Piccolo <spine=10035>', 'Spine portrait appearance 10035 (B30041).', 0, 2127, 10035, 0, 0, 0, 0, -1, -1, 10035
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2083);

-- Item 2084: appearance 10036, B30042 (Chiaotzu).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2084, 5, 3, 'Spine Chiaotzu <spine=10036>', 'Spine portrait appearance 10036 (B30042).', 0, 2127, 10036, 0, 0, 0, 0, -1, -1, 10036
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2084);

-- Item 2085: appearance 10037, B30045 (Yamcha).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2085, 5, 3, 'Spine Yamcha <spine=10037>', 'Spine portrait appearance 10037 (B30045).', 0, 2127, 10037, 0, 0, 0, 0, -1, -1, 10037
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2085);

-- Item 2086: appearance 10038, B30046 (GokuBlack Rose).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2086, 5, 3, 'Spine GokuBlack Rose <spine=10038>', 'Spine portrait appearance 10038 (B30046).', 0, 2127, 10038, 0, 0, 0, 0, -1, -1, 10038
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2086);

-- Item 2087: appearance 10039, B30047 (MrSatan Champion).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2087, 5, 3, 'Spine MrSatan Champion <spine=10039>', 'Spine portrait appearance 10039 (B30047).', 0, 2127, 10039, 0, 0, 0, 0, -1, -1, 10039
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2087);

-- Item 2088: appearance 10040, B30048 (EastKaioshin).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2088, 5, 3, 'Spine EastKaioshin <spine=10040>', 'Spine portrait appearance 10040 (B30048).', 0, 2127, 10040, 0, 0, 0, 0, -1, -1, 10040
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2088);

-- Item 2089: appearance 10041, B30049 (Nappa IronBravery).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2089, 5, 3, 'Spine Nappa IronBravery <spine=10041>', 'Spine portrait appearance 10041 (B30049).', 0, 2127, 10041, 0, 0, 0, 0, -1, -1, 10041
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2089);

-- Item 2090: appearance 10042, B30050 (SonGoku).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2090, 5, 3, 'Spine SonGoku <spine=10042>', 'Spine portrait appearance 10042 (B30050).', 0, 2127, 10042, 0, 0, 0, 0, -1, -1, 10042
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2090);

-- Item 2091: appearance 10043, B30051 (KingKai).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2091, 5, 3, 'Spine KingKai <spine=10043>', 'Spine portrait appearance 10043 (B30051).', 0, 2127, 10043, 0, 0, 0, 0, -1, -1, 10043
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2091);

-- Item 2092: appearance 10044, B30052 (Dende).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2092, 5, 3, 'Spine Dende <spine=10044>', 'Spine portrait appearance 10044 (B30052).', 0, 2127, 10044, 0, 0, 0, 0, -1, -1, 10044
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2092);

-- Item 2093: appearance 10045, B30053 (Android20 DrGero).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2093, 5, 3, 'Spine Android20 DrGero <spine=10045>', 'Spine portrait appearance 10045 (B30053).', 0, 2127, 10045, 0, 0, 0, 0, -1, -1, 10045
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2093);

-- Item 2094: appearance 10046, B30054 (GoldenCooler).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2094, 5, 3, 'Spine GoldenCooler <spine=10046>', 'Spine portrait appearance 10046 (B30054).', 0, 2127, 10046, 0, 0, 0, 0, -1, -1, 10046
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2094);

-- Item 2095: appearance 10047, B30055 (EmperorPilaf).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2095, 5, 3, 'Spine EmperorPilaf <spine=10047>', 'Spine portrait appearance 10047 (B30055).', 0, 2127, 10047, 0, 0, 0, 0, -1, -1, 10047
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2095);

-- Item 2096: appearance 10048, B30056 (Vegeta SS1).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2096, 5, 3, 'Spine Vegeta SS1 <spine=10048>', 'Spine portrait appearance 10048 (B30056).', 0, 2127, 10048, 0, 0, 0, 0, -1, -1, 10048
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2096);

-- Item 2097: appearance 10049, B30057 (Pilaf Variant).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2097, 5, 3, 'Spine Pilaf Variant <spine=10049>', 'Spine portrait appearance 10049 (B30057).', 0, 2127, 10049, 0, 0, 0, 0, -1, -1, 10049
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2097);

-- Item 2098: appearance 10050, B30058 (Frieza).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2098, 5, 3, 'Spine Frieza <spine=10050>', 'Spine portrait appearance 10050 (B30058).', 0, 2127, 10050, 0, 0, 0, 0, -1, -1, 10050
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2098);

-- Item 2099: appearance 10051, B30059 (Hit).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2099, 5, 3, 'Spine Hit <spine=10051>', 'Spine portrait appearance 10051 (B30059).', 0, 2127, 10051, 0, 0, 0, 0, -1, -1, 10051
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2099);

-- Item 2100: appearance 10052, B30060 (Android16).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2100, 5, 3, 'Spine Android16 <spine=10052>', 'Spine portrait appearance 10052 (B30060).', 0, 2127, 10052, 0, 0, 0, 0, -1, -1, 10052
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2100);

-- Item 2101: appearance 10053, B30061 (MasterRoshi MaxPower).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2101, 5, 3, 'Spine MasterRoshi MaxPower <spine=10053>', 'Spine portrait appearance 10053 (B30061).', 0, 2127, 10053, 0, 0, 0, 0, -1, -1, 10053
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2101);

-- Item 2102: appearance 10054, B30062 (SonGoku SS1).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2102, 5, 3, 'Spine SonGoku SS1 <spine=10054>', 'Spine portrait appearance 10054 (B30062).', 0, 2127, 10054, 0, 0, 0, 0, -1, -1, 10054
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2102);

-- Item 2103: appearance 10055, B30063 (Cooler).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2103, 5, 3, 'Spine Cooler <spine=10055>', 'Spine portrait appearance 10055 (B30063).', 0, 2127, 10055, 0, 0, 0, 0, -1, -1, 10055
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2103);

-- Item 2104: appearance 10056, B30064 (Vegeta).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2104, 5, 3, 'Spine Vegeta <spine=10056>', 'Spine portrait appearance 10056 (B30064).', 0, 2127, 10056, 0, 0, 0, 0, -1, -1, 10056
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2104);

-- Item 2105: appearance 10057, B30065 (SonGohan SS2).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2105, 5, 3, 'Spine SonGohan SS2 <spine=10057>', 'Spine portrait appearance 10057 (B30065).', 0, 2127, 10057, 0, 0, 0, 0, -1, -1, 10057
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2105);

-- Item 2106: appearance 10058, B30066 (Portrait B30066).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2106, 5, 3, 'Spine Portrait B30066 <spine=10058>', 'Spine portrait appearance 10058 (B30066).', 0, 2127, 10058, 0, 0, 0, 0, -1, -1, 10058
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2106);

-- Item 2107: appearance 10059, B30067 (Pilaf Variant).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2107, 5, 3, 'Spine Pilaf Variant <spine=10059>', 'Spine portrait appearance 10059 (B30067).', 0, 2127, 10059, 0, 0, 0, 0, -1, -1, 10059
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2107);

-- Item 2108: appearance 10060, B30068 (BabaFortuneTeller).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2108, 5, 3, 'Spine BabaFortuneTeller <spine=10060>', 'Spine portrait appearance 10060 (B30068).', 0, 2127, 10060, 0, 0, 0, 0, -1, -1, 10060
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2108);

-- Item 2109: appearance 10061, B30069 (Raditz).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2109, 5, 3, 'Spine Raditz <spine=10061>', 'Spine portrait appearance 10061 (B30069).', 0, 2127, 10061, 0, 0, 0, 0, -1, -1, 10061
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2109);

-- Item 2110: appearance 10062, B30070 (Jeice).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2110, 5, 3, 'Spine Jeice <spine=10062>', 'Spine portrait appearance 10062 (B30070).', 0, 2127, 10062, 0, 0, 0, 0, -1, -1, 10062
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2110);

-- Item 2111: appearance 10063, B30071 (MasterRoshi).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2111, 5, 3, 'Spine MasterRoshi <spine=10063>', 'Spine portrait appearance 10063 (B30071).', 0, 2127, 10063, 0, 0, 0, 0, -1, -1, 10063
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2111);

-- Item 2112: appearance 10064, B30072 (Trunks).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2112, 5, 3, 'Spine Trunks <spine=10064>', 'Spine portrait appearance 10064 (B30072).', 0, 2127, 10064, 0, 0, 0, 0, -1, -1, 10064
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2112);

-- Item 2113: appearance 10065, B30073 (Portrait B30073).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2113, 5, 3, 'Spine Portrait B30073 <spine=10065>', 'Spine portrait appearance 10065 (B30073).', 0, 2127, 10065, 0, 0, 0, 0, -1, -1, 10065
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2113);

-- Item 2114: appearance 10066, B30074 (Vegito SSBlue).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2114, 5, 3, 'Spine Vegito SSBlue <spine=10066>', 'Spine portrait appearance 10066 (B30074).', 0, 2127, 10066, 0, 0, 0, 0, -1, -1, 10066
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2114);

-- Item 2115: appearance 10067, B30075 (KibitoKai).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2115, 5, 3, 'Spine KibitoKai <spine=10067>', 'Spine portrait appearance 10067 (B30075).', 0, 2127, 10067, 0, 0, 0, 0, -1, -1, 10067
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2115);

-- Item 2116: appearance 10068, B30076 (NorthKaioshin).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2116, 5, 3, 'Spine NorthKaioshin <spine=10068>', 'Spine portrait appearance 10068 (B30076).', 0, 2127, 10068, 0, 0, 0, 0, -1, -1, 10068
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2116);

-- Item 2117: appearance 10069, B30077 (Gogeta).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2117, 5, 3, 'Spine Gogeta <spine=10069>', 'Spine portrait appearance 10069 (B30077).', 0, 2127, 10069, 0, 0, 0, 0, -1, -1, 10069
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2117);

-- Item 2118: appearance 10070, B30078 (MajinBuu).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2118, 5, 3, 'Spine MajinBuu <spine=10070>', 'Spine portrait appearance 10070 (B30078).', 0, 2127, 10070, 0, 0, 0, 0, -1, -1, 10070
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2118);

-- Item 2119: appearance 10071, B30079 (Dyspo).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2119, 5, 3, 'Spine Dyspo <spine=10071>', 'Spine portrait appearance 10071 (B30079).', 0, 2127, 10071, 0, 0, 0, 0, -1, -1, 10071
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2119);

-- Item 2120: appearance 10072, B30080 (Cell).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2120, 5, 3, 'Spine Cell <spine=10072>', 'Spine portrait appearance 10072 (B30080).', 0, 2127, 10072, 0, 0, 0, 0, -1, -1, 10072
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2120);

-- Item 2121: appearance 10073, B30081 (Portrait B30081).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2121, 5, 3, 'Spine Portrait B30081 <spine=10073>', 'Spine portrait appearance 10073 (B30081).', 0, 2127, 10073, 0, 0, 0, 0, -1, -1, 10073
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2121);

-- Item 2122: appearance 10074, B30082 (SonGohan Kid).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2122, 5, 3, 'Spine SonGohan Kid <spine=10074>', 'Spine portrait appearance 10074 (B30082).', 0, 2127, 10074, 0, 0, 0, 0, -1, -1, 10074
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2122);

-- Item 2123: appearance 10075, B30083 (SonGoten).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2123, 5, 3, 'Spine SonGoten <spine=10075>', 'Spine portrait appearance 10075 (B30083).', 0, 2127, 10075, 0, 0, 0, 0, -1, -1, 10075
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2123);

-- Item 2124: appearance 10076, B30084 (Guldo).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2124, 5, 3, 'Spine Guldo <spine=10076>', 'Spine portrait appearance 10076 (B30084).', 0, 2127, 10076, 0, 0, 0, 0, -1, -1, 10076
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2124);

-- Item 2125: appearance 10077, B30085 (Krillin).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2125, 5, 3, 'Spine Krillin <spine=10077>', 'Spine portrait appearance 10077 (B30085).', 0, 2127, 10077, 0, 0, 0, 0, -1, -1, 10077
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2125);

-- Item 2126: appearance 10078, B30086 (BabaPriestess).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2126, 5, 3, 'Spine BabaPriestess <spine=10078>', 'Spine portrait appearance 10078 (B30086).', 0, 2127, 10078, 0, 0, 0, 0, -1, -1, 10078
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2126);

-- Item 2127: appearance 10079, B30087 (OxKing).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2127, 5, 3, 'Spine OxKing <spine=10079>', 'Spine portrait appearance 10079 (B30087).', 0, 2127, 10079, 0, 0, 0, 0, -1, -1, 10079
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2127);

-- Item 2128: appearance 10080, B30088 (Portrait B30088).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2128, 5, 3, 'Spine Portrait B30088 <spine=10080>', 'Spine portrait appearance 10080 (B30088).', 0, 2127, 10080, 0, 0, 0, 0, -1, -1, 10080
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2128);

-- Item 2129: appearance 10081, B30089 (Giru).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2129, 5, 3, 'Spine Giru <spine=10081>', 'Spine portrait appearance 10081 (B30089).', 0, 2127, 10081, 0, 0, 0, 0, -1, -1, 10081
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2129);

-- Item 2130: appearance 10082, B30090 (Vados).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2130, 5, 3, 'Spine Vados <spine=10082>', 'Spine portrait appearance 10082 (B30090).', 0, 2127, 10082, 0, 0, 0, 0, -1, -1, 10082
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2130);

-- Item 2131: appearance 10083, B30091 (Toppo GodOfDestruction).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2131, 5, 3, 'Spine Toppo GodOfDestruction <spine=10083>', 'Spine portrait appearance 10083 (B30091).', 0, 2127, 10083, 0, 0, 0, 0, -1, -1, 10083
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2131);

-- Item 2132: appearance 10084, B30092 (GoldenFrieza).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2132, 5, 3, 'Spine GoldenFrieza <spine=10084>', 'Spine portrait appearance 10084 (B30092).', 0, 2127, 10084, 0, 0, 0, 0, -1, -1, 10084
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2132);

-- Item 2133: appearance 10085, B30093 (Broly).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2133, 5, 3, 'Spine Broly <spine=10085>', 'Spine portrait appearance 10085 (B30093).', 0, 2127, 10085, 0, 0, 0, 0, -1, -1, 10085
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2133);

-- Item 2134: appearance 10086, B30094 (Rozie).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2134, 5, 3, 'Spine Rozie <spine=10086>', 'Spine portrait appearance 10086 (B30094).', 0, 2127, 10086, 0, 0, 0, 0, -1, -1, 10086
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2134);

-- Item 2135: appearance 10087, B30095 (Android17).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2135, 5, 3, 'Spine Android17 <spine=10087>', 'Spine portrait appearance 10087 (B30095).', 0, 2127, 10087, 0, 0, 0, 0, -1, -1, 10087
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2135);

-- Item 2136: appearance 10088, B30096 (Toppo).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2136, 5, 3, 'Spine Toppo <spine=10088>', 'Spine portrait appearance 10088 (B30096).', 0, 2127, 10088, 0, 0, 0, 0, -1, -1, 10088
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2136);

-- Item 2137: appearance 10089, B30097 (Android18).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2137, 5, 3, 'Spine Android18 <spine=10089>', 'Spine portrait appearance 10089 (B30097).', 0, 2127, 10089, 0, 0, 0, 0, -1, -1, 10089
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2137);

-- Item 2138: appearance 10090, B30098 (GrandPriest).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2138, 5, 3, 'Spine GrandPriest <spine=10090>', 'Spine portrait appearance 10090 (B30098).', 0, 2127, 10090, 0, 0, 0, 0, -1, -1, 10090
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2138);

-- Item 2139: appearance 10091, B30099 (Vegeta SSBlue).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2139, 5, 3, 'Spine Vegeta SSBlue <spine=10091>', 'Spine portrait appearance 10091 (B30099).', 0, 2127, 10091, 0, 0, 0, 0, -1, -1, 10091
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2139);

-- Item 2140: appearance 10092, B30100 (BlackFrieza).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2140, 5, 3, 'Spine BlackFrieza <spine=10092>', 'Spine portrait appearance 10092 (B30100).', 0, 2127, 10092, 0, 0, 0, 0, -1, -1, 10092
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2140);

-- Item 2141: appearance 10093, B30101 (Vegeta SSGod).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2141, 5, 3, 'Spine Vegeta SSGod <spine=10093>', 'Spine portrait appearance 10093 (B30101).', 0, 2127, 10093, 0, 0, 0, 0, -1, -1, 10093
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2141);

-- Item 2142: appearance 10094, B30102 (Gotenks).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2142, 5, 3, 'Spine Gotenks <spine=10094>', 'Spine portrait appearance 10094 (B30102).', 0, 2127, 10094, 0, 0, 0, 0, -1, -1, 10094
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2142);

-- Item 2143: appearance 10095, B30103 (Beerus).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2143, 5, 3, 'Spine Beerus <spine=10095>', 'Spine portrait appearance 10095 (B30103).', 0, 2127, 10095, 0, 0, 0, 0, -1, -1, 10095
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2143);

-- Item 2144: appearance 10096, B30104 (SonGoku SSGod).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2144, 5, 3, 'Spine SonGoku SSGod <spine=10096>', 'Spine portrait appearance 10096 (B30104).', 0, 2127, 10096, 0, 0, 0, 0, -1, -1, 10096
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2144);

-- Item 2145: appearance 10097, B30105 (Vegeta UltraEgo).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2145, 5, 3, 'Spine Vegeta UltraEgo <spine=10097>', 'Spine portrait appearance 10097 (B30105).', 0, 2127, 10097, 0, 0, 0, 0, -1, -1, 10097
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2145);

-- Item 2146: appearance 10098, B30106 (GeniusBulma).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2146, 5, 3, 'Spine GeniusBulma <spine=10098>', 'Spine portrait appearance 10098 (B30106).', 0, 2127, 10098, 0, 0, 0, 0, -1, -1, 10098
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2146);

-- Item 2147: appearance 10099, B30107 (SonGoku SSBlue).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2147, 5, 3, 'Spine SonGoku SSBlue <spine=10099>', 'Spine portrait appearance 10099 (B30107).', 0, 2127, 10099, 0, 0, 0, 0, -1, -1, 10099
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2147);

-- Item 2148: appearance 10100, B30108 (FusedZamasu).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2148, 5, 3, 'Spine FusedZamasu <spine=10100>', 'Spine portrait appearance 10100 (B30108).', 0, 2127, 10100, 0, 0, 0, 0, -1, -1, 10100
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2148);

-- Item 2149: appearance 10101, B30109 (Partner 40510).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2149, 5, 3, 'Spine Partner 40510 <spine=10101>', 'Spine portrait appearance 10101 (B30109).', 0, 2127, 10101, 0, 0, 0, 0, -1, -1, 10101
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2149);

-- Item 2150: appearance 10102, B30110 (CellMax).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2150, 5, 3, 'Spine CellMax <spine=10102>', 'Spine portrait appearance 10102 (B30110).', 0, 2127, 10102, 0, 0, 0, 0, -1, -1, 10102
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2150);

-- Item 2151: appearance 10103, B30111 (Jiren).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2151, 5, 3, 'Spine Jiren <spine=10103>', 'Spine portrait appearance 10103 (B30111).', 0, 2127, 10103, 0, 0, 0, 0, -1, -1, 10103
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2151);

-- Item 2152: appearance 10104, B30112 (MajinBuu).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2152, 5, 3, 'Spine MajinBuu <spine=10104>', 'Spine portrait appearance 10104 (B30112).', 0, 2127, 10104, 0, 0, 0, 0, -1, -1, 10104
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2152);

-- Item 2153: appearance 10105, B30113 (Vegito and Gogeta).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2153, 5, 3, 'Spine Vegito and Gogeta <spine=10105>', 'Spine portrait appearance 10105 (B30113).', 0, 2127, 10105, 0, 0, 0, 0, -1, -1, 10105
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2153);

-- Item 2154: appearance 10106, B30114 (SonGoku UltraInstinct).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2154, 5, 3, 'Spine SonGoku UltraInstinct <spine=10106>', 'Spine portrait appearance 10106 (B30114).', 0, 2127, 10106, 0, 0, 0, 0, -1, -1, 10106
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2154);

-- Item 2155: appearance 10107, B30115 (Rozie and Ribrianne).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2155, 5, 3, 'Spine Rozie and Ribrianne <spine=10107>', 'Spine portrait appearance 10107 (B30115).', 0, 2127, 10107, 0, 0, 0, 0, -1, -1, 10107
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2155);

-- Item 2156: appearance 10108, B30116 (SonGohan Beast).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2156, 5, 3, 'Spine SonGohan Beast <spine=10108>', 'Spine portrait appearance 10108 (B30116).', 0, 2127, 10108, 0, 0, 0, 0, -1, -1, 10108
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2156);

-- Item 2157: appearance 10109, B50000 (Android17).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2157, 5, 3, 'Spine Android17 <spine=10109>', 'Spine portrait appearance 10109 (B50000).', 0, 2127, 10109, 0, 0, 0, 0, -1, -1, 10109
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2157);

-- Item 2158: appearance 10110, B50001 (Vados).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2158, 5, 3, 'Spine Vados <spine=10110>', 'Spine portrait appearance 10110 (B50001).', 0, 2127, 10110, 0, 0, 0, 0, -1, -1, 10110
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2158);

-- Item 2159: appearance 10111, B50002 (Broly).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2159, 5, 3, 'Spine Broly <spine=10111>', 'Spine portrait appearance 10111 (B50002).', 0, 2127, 10111, 0, 0, 0, 0, -1, -1, 10111
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2159);

-- Item 2160: appearance 10112, B50003 (Portrait).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2160, 5, 3, 'Spine Portrait <spine=10112>', 'Spine portrait appearance 10112 (B50003).', 0, 2127, 10112, 0, 0, 0, 0, -1, -1, 10112
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2160);

-- Item 2161: appearance 10113, B50004 (Portrait).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2161, 5, 3, 'Spine Portrait <spine=10113>', 'Spine portrait appearance 10113 (B50004).', 0, 2127, 10113, 0, 0, 0, 0, -1, -1, 10113
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2161);

-- Item 2162: appearance 10114, B50005 (Portrait).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2162, 5, 3, 'Spine Portrait <spine=10114>', 'Spine portrait appearance 10114 (B50005).', 0, 2127, 10114, 0, 0, 0, 0, -1, -1, 10114
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2162);

-- Item 2163: appearance 10115, B50006 (Hit).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2163, 5, 3, 'Spine Hit <spine=10115>', 'Spine portrait appearance 10115 (B50006).', 0, 2127, 10115, 0, 0, 0, 0, -1, -1, 10115
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2163);

-- Item 2164: appearance 10116, B50007 (Rozie).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2164, 5, 3, 'Spine Rozie <spine=10116>', 'Spine portrait appearance 10116 (B50007).', 0, 2127, 10116, 0, 0, 0, 0, -1, -1, 10116
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2164);

-- Item 2165: appearance 10117, B50008 (BlackFrieza).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2165, 5, 3, 'Spine BlackFrieza <spine=10117>', 'Spine portrait appearance 10117 (B50008).', 0, 2127, 10117, 0, 0, 0, 0, -1, -1, 10117
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2165);

-- Item 2166: appearance 10118, B50009 (Vegito SSBlue).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2166, 5, 3, 'Spine Vegito SSBlue <spine=10118>', 'Spine portrait appearance 10118 (B50009).', 0, 2127, 10118, 0, 0, 0, 0, -1, -1, 10118
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2166);

-- Item 2167: appearance 10119, B50010 (HalloweenCount).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2167, 5, 3, 'Spine HalloweenCount <spine=10119>', 'Spine portrait appearance 10119 (B50010).', 0, 2127, 10119, 0, 0, 0, 0, -1, -1, 10119
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2167);

-- Item 2168: appearance 10120, B50011 (Whis).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2168, 5, 3, 'Spine Whis <spine=10120>', 'Spine portrait appearance 10120 (B50011).', 0, 2127, 10120, 0, 0, 0, 0, -1, -1, 10120
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2168);

-- Item 2169: appearance 10121, B50012 (GrandPriest).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2169, 5, 3, 'Spine GrandPriest <spine=10121>', 'Spine portrait appearance 10121 (B50012).', 0, 2127, 10121, 0, 0, 0, 0, -1, -1, 10121
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2169);

-- Item 2170: appearance 10122, B50013 (Android17).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2170, 5, 3, 'Spine Android17 <spine=10122>', 'Spine portrait appearance 10122 (B50013).', 0, 2127, 10122, 0, 0, 0, 0, -1, -1, 10122
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2170);

-- Item 2171: appearance 10123, B50014 (Rozie).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2171, 5, 3, 'Spine Rozie <spine=10123>', 'Spine portrait appearance 10123 (B50014).', 0, 2127, 10123, 0, 0, 0, 0, -1, -1, 10123
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2171);

-- Item 2172: appearance 10124, B50015 (Vegeta UltraEgo).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2172, 5, 3, 'Spine Vegeta UltraEgo <spine=10124>', 'Spine portrait appearance 10124 (B50015).', 0, 2127, 10124, 0, 0, 0, 0, -1, -1, 10124
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2172);

-- Item 2173: appearance 10125, B50016 (BlackFrieza).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2173, 5, 3, 'Spine BlackFrieza <spine=10125>', 'Spine portrait appearance 10125 (B50016).', 0, 2127, 10125, 0, 0, 0, 0, -1, -1, 10125
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2173);

-- Item 2174: appearance 10126, B50017 (Vegeta SSBlue).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2174, 5, 3, 'Spine Vegeta SSBlue <spine=10126>', 'Spine portrait appearance 10126 (B50017).', 0, 2127, 10126, 0, 0, 0, 0, -1, -1, 10126
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2174);

-- Item 2175: appearance 10127, B50018 (MajinBuu).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2175, 5, 3, 'Spine MajinBuu <spine=10127>', 'Spine portrait appearance 10127 (B50018).', 0, 2127, 10127, 0, 0, 0, 0, -1, -1, 10127
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2175);

-- Item 2176: appearance 10128, B50019 (Portrait).
INSERT INTO item_template (id, type, gender, name, description, level, icon_id, part, is_up_to_up, power_require, gold, gem, head, body, leg)
SELECT 2176, 5, 3, 'Spine Portrait <spine=10128>', 'Spine portrait appearance 10128 (B50019).', 0, 2127, 10128, 0, 0, 0, 0, -1, -1, 10128
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id = 2176);

COMMIT;
