package services.func;

 
import Bot.NewBot;
import models.clan.Clan;
import models.clan.ClanMember;
import database.AlyraManager;
import consts.ConstNpc;
import database.daos.PlayerDAO;
import models.item.Item;
import models.map.Zone;
import models.npc.Npc;
import services.map.NpcManager;
import models.player.Player;
import network.io.Message;
import interfaces.ISession;
import server.Client;
import services.Service;
import services.func.minigame.CSMM;
import services.GiftCodeService;
import services.player.InventoryService;
import services.ItemService;
import services.map.NpcService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.item.Item.ItemOption;
import models.npc.TaiXiu;
import models.npc.TaiXiuXocDiaProLuxuryVip999;
import models.player.Inventory;
import server.Manager;
import services.player.ClanService;
import services.map.ChangeMapService;
import services.player.PlayerService;
import utils.Util;

public class Input {

    private static final Map<Integer, Object> PLAYER_ID_OBJECT = new java.util.concurrent.ConcurrentHashMap<>();

    public static final int BETCHAN = 5166;
    public static final int BETLE = 5167;
    public static final int BET3D1T = 5168;
    public static final int BET3T1D = 5169;
    public static final int BET4T = 5170;
    public static final int BET4D = 5171;

    public static final int TAIXIU_TAI = 5192;
    public static final int TAIXIU_XIU = 5193;
    public static final int CHANGE_PASSWORD = 500;
    public static final int GIFT_CODE = 501;
    public static final int FIND_PLAYER = 502;
    public static final int CHANGE_NAME = 503;
    public static final int CHOOSE_LEVEL_BDKB = 504;
    public static final int NAP_THE = 505;
    public static final int CHANGE_NAME_BY_ITEM = 506;
    public static final int GIVE_IT = 507;
    public static final int GET_IT = 508;
    public static final int DANGKY = 509;
    public static final int CHOOSE_LEVEL_KGHD = 510;
    public static final int CHOOSE_LEVEL_CDRD = 511;
    public static final int DISSOLUTION_CLAN = 513;
    public static final int TRADE_MANH_VO = 514;
    public static final int DUNGLOA = 517;

    public static final byte NUMERIC = 0;
    public static final byte ANY = 1;
    public static final byte PASSWORD = 2;
    public static final byte MBV = 23;
    public static final byte BANSLL = 24;
    public static final byte BANGHOI = 25;
    public static final int CON_SO_MAY_MAN_VANG = 50098;
    public static final int CON_SO_MAY_MAN_NGOC = 50900;

    public static final int BOTQUAI = 206783;
    public static final int BOTITEM = 206762;
    public static final int BOTBOSS = 2067683;
    public static final int BOTQUAI_NAPPA = 206784;
    public static final int BOTQUAI_TUONGLAI = 206785;
    public static final int BOTQUAI_COLD = 206786;
    public static final int BUFFVND = 586537;
    public static final int SEND_ITEM_OP = 555;

    public static final int TANG_NGOC_XANH = 5356326;
    public static final int TANG_NGOC_HONG = 4365478;
    public static final int USE_GOLD_BAR = 43654718;
    public static final int DUNG_PHIEU_VND_10K = 43654719;
    public static final int DUNG_PHIEU_VND_8K = 43654720;
    public static final int CSMMNE = 4365479;
    public static final int DONATE_CAPSULE_BANG = 4365480;
    public static final int ADMIN_AUTH = 9999999;

    private static Input intance;

    private Input() {

    }

    public static Input gI() {
        if (intance == null) {
            intance = new Input();
        }
        return intance;
    }

    public void doInput(Player player, Message msg) {
        String[] text;
        try {
    
            int count = msg.reader().readByte() & 0xFF;
            if (count < 1 || count > 16) {
                return; // số ô nhập bất thường -> gói rác, bỏ qua
            }
            text = new String[count];
            for (int i = 0; i < count; i++) {
                text[i] = msg.reader().readUTF();
            }
        } catch (java.io.IOException e) {
 
            return;
        }
        int typeInput = player.idMark.getTypeInput();
        if (typeInput == BETCHAN || typeInput == BETLE || typeInput == BET3D1T || typeInput == BET3T1D
                || typeInput == BET4T || typeInput == BET4D || typeInput == TAIXIU_TAI || typeInput == TAIXIU_XIU
                || typeInput == CSMMNE) {
            if (!player.isAdmin() && (player.getSession() == null || !player.getSession().actived)) {
                Service.gI().sendThongBao(player, "Bạn chưa mở thành viên!");
                return;
            }
        }
        try {
            if (typeInput == BETCHAN || typeInput == BETLE || typeInput == BET3D1T || typeInput == BET4T
                    || typeInput == BET4D) {
                if (TaiXiuXocDiaProLuxuryVip999.gI().settling || TaiXiuXocDiaProLuxuryVip999.gI().baoTri) {
                    Service.gI().sendThongBao(player, "Trò chơi đang tổng kết, vui lòng đặt cược sau!");
                    return;
                }
            }
            switch (typeInput) {
                case BETCHAN:
                    try {
                        int bet = Integer.parseInt(text[0]);
                        if (bet >= 1000 && bet <= 1000000000) {
                            if (player.inventory.ruby >= bet) {
                                player.inventory.ruby -= bet;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(bet) + " hồng ngọc vào CHẴN");
                                player.betChan += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().totalChan += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().addPlayerChan(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ tiền để chơi.");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1K - nhiều nhất 1TR");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng chỉ nhập số\nCược ít nhất 1K - nhiều nhất 1TR");
                    }
                    break;

                case BETLE:
                    try {
                        int bet = Integer.parseInt(text[0]);
                        if (bet >= 1000 && bet <= 1000000000) {
                            if (player.inventory.ruby >= bet) {
                                player.inventory.ruby -= bet;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(bet) + " hồng ngọc vào LẺ");
                                player.betLe += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().totalLe += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().addPlayerLe(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ tiền để chơi.");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1K - nhiều nhất 1TR");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng chỉ nhập số\nCược ít nhất 1K - nhiều nhất 1TR");
                    }
                    break;

                case BET3D1T:
                    try {
                        int bet = Integer.parseInt(text[0]);
                        if (bet >= 1000 && bet <= 1000000000) {
                            if (player.inventory.ruby >= bet) {
                                player.inventory.ruby -= bet;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(bet) + " hồng ngọc vào 3 Đỏ 1 Trắng");
                                player.bet3Do1Trang += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().total3Do1Trang += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().addPlayer3Do1Trang(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ tiền để chơi.");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1K - nhiều nhất 1TR");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng chỉ nhập số\nCược ít nhất 1K - nhiều nhất 1TR");
                    }
                    break;

                case BET3T1D:
                    try {
                        int bet = Integer.parseInt(text[0]);
                        if (bet >= 1000 && bet <= 1000000000) {
                            if (player.inventory.ruby >= bet) {
                                player.inventory.ruby -= bet;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(bet) + " hồng ngọc vào 3 Trắng 1 Đỏ");
                                player.bet3Trang1Do += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().total3Trang1Do += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().addPlayer3Trang1Do(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ tiền để chơi.");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1K - nhiều nhất 1TR");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng chỉ nhập số\nCược ít nhất 1K - nhiều nhất 1TR");
                    }
                    break;

                case BET4T:
                    try {
                        int bet = Integer.parseInt(text[0]);
                        if (bet >= 1000 && bet <= 1000000000) {
                            if (player.inventory.ruby >= bet) {
                                player.inventory.ruby -= bet;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(bet) + " hồng ngọc vào Tứ Tử Trắng");
                                player.betTuTuTrang += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuTrang += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().addPlayerTuTuTrang(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ tiền để chơi.");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1K - nhiều nhất 1TR");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng chỉ nhập số\nCược ít nhất 1K - nhiều nhất 1TR");
                    }
                    break;

                case BET4D:
                    try {
                        int bet = Integer.parseInt(text[0]);
                        if (bet >= 1000 && bet <= 1000000000) {
                            if (player.inventory.ruby >= bet) {
                                player.inventory.ruby -= bet;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(bet) + " hồng ngọc vào Tứ Tử Đỏ");
                                player.betTuTuDo += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().totalTuTuDo += bet;
                                TaiXiuXocDiaProLuxuryVip999.gI().addPlayerTuTuDo(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ tiền để chơi.");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1K - nhiều nhất 1TR");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng chỉ nhập số\nCược ít nhất 1K - nhiều nhất 1TR");
                    }
                    break;

                case CSMMNE:
                    try {
                        int num = Integer.parseInt(text[0]);
                        if (num > 0 && num < 1000) {
                            CSMM.gI().Register(player, num);
                        } else {
                            Service.gI().sendThongBao(player, "Vui lòng chỉ nhập số từ 1-1000");
                        }
                    } catch (Exception e) {
                        Service.gI().sendThongBao(player, "Bạn phải nhập vào 1 con số từ 1-999");
                    }
                    break;
                case USE_GOLD_BAR:
                    try {
                        int goldBar = Integer.parseInt(text[0]);
                        if (goldBar > 0 && goldBar < Integer.MAX_VALUE) {
                            int totalThoiVang = InventoryService.gI().getNumItemBag(player, 457);
                            if (totalThoiVang >= goldBar) {
                                long goldAdd = goldBar * 100_000_000L;
                                if (player.inventory.gold + goldAdd > Inventory.LIMIT_GOLD) {
                                    Service.gI().sendThongBaoOK(player, "Số vàng sau khi sử dụng vượt quá giới hạn cho phép. Max là 2000 tỷ!!");
                                    return;
                                }
                                player.inventory.gold += goldAdd;
                                Service.gI().sendMoney(player);
                                InventoryService.gI().subQuantityItemsBag(player, 457, goldBar);
                                models.task.Task_Kol.tick(player, goldBar, 10); // KOL 10 (tiêu 5000 thỏi vàng)
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendThongBaoOK(player, "Đã dùng " + goldBar + " thỏi vàng nhận được " + Util.numberToMoney(goldAdd) + " vàng.");
                            } else {
                                Service.gI().sendThongBaoOK(player, "Thỏi vàng trong hành trang không đủ!");
                            }
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBaoOK(player, "Vui lòng chỉ nhập số");
                    }
                    break;

                case DUNG_PHIEU_VND_10K:
                    UseItem.gI().dungPhieuVnd(player, 1340, Integer.parseInt(text[0].trim()), 10000);
                    break;

                case DUNG_PHIEU_VND_8K:
                    UseItem.gI().dungPhieuVnd(player, 1341, Integer.parseInt(text[0].trim()), 8000);
                    break;

                case TANG_NGOC_HONG:
                    Player pl = Client.gI().getPlayer(text[0]);
                    int numRuby = Integer.parseInt((text[1]));
                    if (pl != null) {
                        if (numRuby >= 1000 && numRuby <= 100000 && player.inventory.ruby >= numRuby) {
                            Item item = InventoryService.gI().findVeTangNgochong(player);
                            if (item == null || item.itemOptions == null) {
                                Service.gI().sendThongBao(player, "Bạn không có Vé tặng hồng ngọc!");
                                return;
                            }
                            ItemOption option = item.itemOptions.stream().filter(io -> io != null && io.optionTemplate != null && io.optionTemplate.id == 31 && io.param > 0).findAny().orElse(null);
                            int maxNumOption = numRuby / 10;
                            if (option == null || option.param < maxNumOption) {
                                Service.gI().sendThongBao(player, "Không đủ số lượt để tặng.");
                                return;
                            }
                            player.inventory.subRuby(numRuby);
                            PlayerService.gI().sendInfoHpMpMoney(player);
                            pl.inventory.ruby += numRuby * 90 / 100;
                            PlayerService.gI().sendInfoHpMpMoney(pl);
                            Service.gI().sendThongBao(player, "Tặng Hồng ngọc thành công");
                            Service.gI().sendThongBao(pl, "Bạn được " + player.name + " tặng " + (numRuby * 90 / 100) + " Hồng ngọc");
                            option.param -= maxNumOption;
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Không đủ hoặc quá giới hạn Hồng ngọc cho phép tặng (1K-100K hồng ngọc)");
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc đang offline");
                    }
                    break;
                case TANG_NGOC_XANH:
                    pl = Client.gI().getPlayer(text[0]);
                    int numGem = Integer.parseInt((text[1]));
                    if (pl != null) {
                        if (numGem >= 1000 && numGem <= 1000000 && player.inventory.gem >= numGem) {
                            Item item = InventoryService.gI().findVeTangNgocxanh(player);
                            if (item == null || item.itemOptions == null) {
                                Service.gI().sendThongBao(player, "Bạn không có Vé tặng ngọc xanh!");
                                return;
                            }
                            ItemOption option = item.itemOptions.stream().filter(io -> io != null && io.optionTemplate != null && io.optionTemplate.id == 31 && io.param > 0).findAny().orElse(null);
                            int maxNumOption = numGem / 10;
                            if (option == null || option.param < maxNumOption) {
                                Service.gI().sendThongBao(player, "Không đủ số lượt để tặng.");
                                return;
                            }
                            player.inventory.subGem(numGem);
                            PlayerService.gI().sendInfoHpMpMoney(player);
                            pl.inventory.gem += numGem * 90 / 100;
                            PlayerService.gI().sendInfoHpMpMoney(pl);
                            Service.gI().sendThongBao(player, "Tặng ngọc xanh thành công");
                            Service.gI().sendThongBao(pl, "Bạn được " + player.name + " tặng " + (numGem * 90 / 100) + " Ngọc xanh");
                            option.param -= maxNumOption;
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Không đủ hoặc quá giới hạn Ngọc xanh cho phép tặng (1K-1Tr ngọc xanh)");
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc đang offline");
                    }
                    break;
                case SEND_ITEM_OP:
                    if (player.isAdmin()) {
                        Player pBuffItem = Client.gI().getPlayer(text[0]);
                        int idItemBuff = Integer.parseInt(text[1]);
                        String idOptionBuff = text[2].trim();
                        int slItemBuff = Integer.parseInt(text[3]);
                        try {
                            if (pBuffItem != null) {
                                if (ItemService.gI().getTemplate((short) idItemBuff) == null) {
                                    Service.gI().sendThongBao(player, "Item id " + idItemBuff + " không tồn tại");
                                    break;
                                }
                                String txtBuff = "Admin Buff Bẩn Cho: " + pBuffItem.name + "\b";
                                Item itemBuffTemplate = ItemService.gI().createNewItem((short) idItemBuff, slItemBuff);
                                if (!idOptionBuff.isEmpty()) {
                                    String arr[] = idOptionBuff.split("v");
                                    for (String arr1 : arr) {
                                        String[] arr2 = arr1.split("-");
                                        int idoption = Integer.parseInt(arr2[0].trim());
                                        int param = Integer.parseInt(arr2[1].trim());
                                        if (ItemService.gI().getItemOptionTemplate(idoption) == null) {
                                            Service.gI().sendThongBao(player, "ID Option " + idoption + " không tồn tại");
                                            return;
                                        }
                                        itemBuffTemplate.itemOptions.add(new Item.ItemOption(idoption, param));
                                    }

                                }
                                txtBuff += "x" + slItemBuff + " " + itemBuffTemplate.template.name + "\b";
                                InventoryService.gI().addItemBag(pBuffItem, itemBuffTemplate);
                                InventoryService.gI().sendItemBags(pBuffItem);
                                NpcService.gI().createTutorial(player, 24, txtBuff);
                                if (player.id != pBuffItem.id) {
                                    NpcService.gI().createTutorial(pBuffItem, 24, txtBuff);
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Player không online");
                            }
                        } catch (NumberFormatException e) {
                            Service.gI().sendThongBao(player, "Đã có lỗi xảy ra vui lòng thử lại");
                        }

                    }
                    break;
                case DUNGLOA:
                    if (text.length == 0 || text[0] == null || text[0].trim().isEmpty()) {
                        Service.gI().sendThongBao(player, "Nội dung không được để trống");
                        break;
                    }
                    String contentServer = text[0];
                    Service.gI().sendThongBaoAllPlayer(contentServer);
                    break;

                case TAIXIU_TAI:
                    try {
                        String rawInput = text[0].trim().replaceAll("[\\.,\\s_]", "");
                        long multiplier = 1;
                        if (rawInput.endsWith("k") || rawInput.endsWith("K")) {
                            multiplier = 1000;
                            rawInput = rawInput.substring(0, rawInput.length() - 1);
                        } else if (rawInput.endsWith("m") || rawInput.endsWith("M")) {
                            multiplier = 1000000;
                            rawInput = rawInput.substring(0, rawInput.length() - 1);
                        }
                        long taixiu_Tai = Long.parseLong(rawInput) * multiplier;
                        if (taixiu_Tai >= 10 && taixiu_Tai <= 100_000) {
                            int totalThoiVang = InventoryService.gI().getNumItemBagWithoutOption(player, 457, 30);
                            if (totalThoiVang < taixiu_Tai) {
                                if (InventoryService.gI().getNumItemBag(player, 457) >= taixiu_Tai) {
                                    Service.gI().sendThongBao(player, "Thỏi vàng có option 30 (khóa) không thể dùng để chơi tài xỉu.");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ thỏi vàng cược.");
                                }
                                return;
                            }
                            if (Util.canDoWithTime(TaiXiu.gI().lastTimeEnd, TaiXiu.TIME_END)) {
                                Service.gI().sendThongBaoOK(player, "Đã qua lượt mới, vui lòng đặt cược lại.");
                                return;
                            }
                            Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(taixiu_Tai) + " thỏi vàng vào cửa Tài");
                            TaiXiu.gI().addTai(player, taixiu_Tai);
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 10 - nhiều nhất 100,000 thỏi vàng");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số lượng nhập không hợp lệ.");
                    }
                    break;

                case TAIXIU_XIU:
                    try {
                        String rawInput = text[0].trim().replaceAll("[\\.,\\s_]", "");
                        long multiplier = 1;
                        if (rawInput.endsWith("k") || rawInput.endsWith("K")) {
                            multiplier = 1000;
                            rawInput = rawInput.substring(0, rawInput.length() - 1);
                        } else if (rawInput.endsWith("m") || rawInput.endsWith("M")) {
                            multiplier = 1000000;
                            rawInput = rawInput.substring(0, rawInput.length() - 1);
                        }
                        long taixiu_Xiu = Long.parseLong(rawInput) * multiplier;
                        if (taixiu_Xiu >= 10 && taixiu_Xiu <= 100_000) {
                            int totalThoiVang = InventoryService.gI().getNumItemBagWithoutOption(player, 457, 30);
                            if (totalThoiVang < taixiu_Xiu) {
                                if (InventoryService.gI().getNumItemBag(player, 457) >= taixiu_Xiu) {
                                    Service.gI().sendThongBaoOK(player, "Thỏi vàng có option 30 (khóa) không thể dùng để chơi tài xỉu.");
                                } else {
                                    Service.gI().sendThongBaoOK(player, "Bạn không đủ thỏi vàng cược.");
                                }
                                return;
                            }
                            if (Util.canDoWithTime(TaiXiu.gI().lastTimeEnd, TaiXiu.TIME_END)) {
                                Service.gI().sendThongBaoOK(player, "Đã qua lượt mới, vui lòng đặt cược lại.");
                                return;
                            }
                            Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.format(taixiu_Xiu) + " thỏi vàng vào cửa Xỉu");
                            TaiXiu.gI().addXiu(player, taixiu_Xiu);
                        } else {
                            Service.gI().sendThongBao(player, "Cược ít nhất 10 - nhiều nhất 100,000 thỏi vàng");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số lượng nhập không hợp lệ.");
                    }
                    break;
                case BUFFVND:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    try {
                        String targetInput = text[0].trim();
                        int addvnd = 0;
                        int addtotalvnd = 0;
                        int addsotien = 0;
                        if (text.length > 1 && !text[1].trim().isEmpty()) {
                            addvnd = Integer.parseInt(text[1].trim());
                        }
                        if (text.length > 2 && !text[2].trim().isEmpty()) {
                            addtotalvnd = Integer.parseInt(text[2].trim());
                        }
                        if (text.length > 3 && !text[3].trim().isEmpty()) {
                            addsotien = Integer.parseInt(text[3].trim());
                        }
                        if (addvnd <= 0 && addtotalvnd <= 0 && addsotien <= 0) {
                            Service.gI().sendThongBao(player, "Vui lòng nhập số VNĐ, Tổng nạp hoặc Số Tiền lớn hơn 0!");
                            break;
                        }
                        PlayerDAO.AccountTarget target = PlayerDAO.resolveAccountTarget(targetInput);
                        if (target.accountId == -1) {
                            Service.gI().sendThongBao(player, "Không tìm thấy tài khoản hoặc nhân vật: " + targetInput);
                            break;
                        }
                        boolean ok = PlayerDAO.buffVnd(targetInput, addvnd, addtotalvnd, addsotien);
                        if (ok) {
                            String displayName = !target.playerName.isEmpty() ? target.playerName : target.username;
                            StringBuilder sb = new StringBuilder();
                            sb.append("Đã buff thành công cho ").append(displayName).append(" (TK: ").append(target.username).append("):\n");
                            if (addvnd > 0) sb.append("+ ").append(Util.format(addvnd)).append(" VNĐ (Cash)\n");
                            if (addtotalvnd > 0) sb.append("+ ").append(Util.format(addtotalvnd)).append(" Tổng Nạp\n");
                            if (addsotien > 0) sb.append("+ ").append(Util.format(addsotien)).append(" Số Tiền\n");
                            Service.gI().sendThongBao(player, sb.toString().trim());
                        } else {
                            Service.gI().sendThongBao(player, "Buff thất bại! Kiểm tra lại database.");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số tiền không hợp lệ! Vui lòng chỉ nhập số.");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Service.gI().sendThongBao(player, "Có lỗi xảy ra: " + e.getMessage());
                    }
                    break;
                case BOTQUAI_NAPPA:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    int slot = Integer.parseInt(text[0]);
                    new Thread(() -> {
                        NewBot.gI().runBot(1, slot);
                    }).start();
                    break;
                case BOTQUAI_TUONGLAI:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    slot = Integer.parseInt(text[0]);
                    new Thread(() -> {
                        NewBot.gI().runBot(2, slot);
                    }).start();
                    break;
                case BOTQUAI_COLD:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    slot = Integer.parseInt(text[0]);
                    new Thread(() -> {
                        NewBot.gI().runBot(3, slot);
                    }).start();
                    break;
                case BOTBOSS:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    slot = Integer.parseInt(text[0]);
                    new Thread(() -> {
                        // Săn boss là type 4 (Sanb) theo switch trong Bot.update();
                        // để 2 là ra bot Tương Lai chứ không phải bot săn boss.
                        NewBot.gI().runBot(4, slot);
                    }).start();
                    break;
                case BOTQUAI:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    slot = Integer.parseInt(text[0]);
                    new Thread(() -> {
                        NewBot.gI().runBot(0, slot);
                    }).start();
                    break;
                case CON_SO_MAY_MAN_VANG:
                    int CSMM2 = Integer.parseInt(text[0]);
                    if (CSMM2 >= MiniGame.gI().MiniGame_S1.min && CSMM2 <= MiniGame.gI().MiniGame_S1.max && MiniGame.gI().MiniGame_S1.second > 10) {
                        MiniGame.gI().MiniGame_S1.newData(player, CSMM2, 1);
                    }
                    break;
                case GIVE_IT:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    String name = text[0];
                    int id = Integer.parseInt(text[1]);
                    int op = Integer.parseInt(text[2]);
                    int pr = Integer.parseInt(text[3]);
                    int q = Integer.parseInt(text[4]);

                    if (ItemService.gI().getTemplate((short) id) == null) {
                        Service.gI().sendThongBao(player, "Item id " + id + " không tồn tại");
                        break;
                    }
                    if (ItemService.gI().getItemOptionTemplate(op) == null) {
                        Service.gI().sendThongBao(player, "ID Option " + op + " không tồn tại");
                        break;
                    }

                    if (Client.gI().getPlayer(name) != null) {
                        if (ItemService.gI().getTemplate((short) id) == null) {
                            Service.gI().sendThongBao(player, "Item id " + id + " không tồn tại");
                            break;
                        }
                        Item item = ItemService.gI().createNewItem(((short) id));
                        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) id);
                        if (!ops.isEmpty()) {
                            item.itemOptions = ops;
                        }
                        item.quantity = q;
                        item.itemOptions.add(new Item.ItemOption(op, pr));
                        InventoryService.gI().addItemBag(Client.gI().getPlayer(name), item);
                        InventoryService.gI().sendItemBags(Client.gI().getPlayer(name));
                        Service.gI().sendThongBao(Client.gI().getPlayer(name), "Nhận " + item.template.name + " từ " + player.name);

                    } else {
                        Service.gI().sendThongBao(player, "Không online");
                    }
                    break;
                case GET_IT:
                    id = Integer.parseInt(text[0]);
                    op = Integer.parseInt(text[1]);
                    pr = Integer.parseInt(text[2]);
                    q = Integer.parseInt(text[3]);

                    if (player.isAdmin()) {
                        if (ItemService.gI().getTemplate((short) id) == null) {
                            Service.gI().sendThongBao(player, "Item id " + id + " không tồn tại");
                            break;
                        }
                        if (ItemService.gI().getItemOptionTemplate(op) == null) {
                            Service.gI().sendThongBao(player, "ID Option " + op + " không tồn tại");
                            break;
                        }
                        Item item = ItemService.gI().createNewItem(((short) id));
                        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) id);
                        if (!ops.isEmpty()) {
                            item.itemOptions = ops;
                        }
                        item.quantity = q;
                        item.itemOptions.add(new Item.ItemOption(op, pr));
                        InventoryService.gI().addItemBag(player, item);
                        InventoryService.gI().sendItemBags(player);
                        Service.gI().sendThongBao(player, "Nhận " + item.template.name + " !");

                    } else {
                        Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
                    }
                    break;
                case CHANGE_PASSWORD:
                    Service.gI().changePassword(player, text[0], text[1], text[2]);
                    break;
                case ADMIN_AUTH:
                    server.Command.gI().verifyAdminPassword(player, text[0]);
                    break;
                case GIFT_CODE:
                    GiftCodeService.gI().giftCode(player, text[0]);
                    break;
                case FIND_PLAYER:
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    pl = Client.gI().getPlayer(text[0]);
                    if (pl != null) {
                        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_FIND_PLAYER, -1, "Ngài muốn..?",
                                new String[]{"Đi tới\n" + pl.name, "Gọi " + pl.name + "\ntới đây", "Đổi tên", "Ban"},
                                pl);
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc đang offline");
                    }
                    break;
                case CHANGE_NAME: {
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Bạn không có quyền sử dụng chức năng này!");
                        break;
                    }
                    Player plChanged = (Player) PLAYER_ID_OBJECT.remove((int) player.id);
                    if (plChanged != null) {
                        if (AlyraManager.executeQuery("select * from player where name = ?", text[0]).next()) {
                            Service.gI().sendThongBao(player, "Tên nhân vật đã tồn tại");
                        } else {
                            plChanged.name = text[0];
                            AlyraManager.executeUpdate("update player set name = ? where id = ?", plChanged.name, plChanged.id);
                            Service.gI().player(plChanged);
                            Service.gI().Send_Caitrang(plChanged);
                            Service.gI().sendFlagBag(plChanged);
                            Zone zone = plChanged.zone;
                            ChangeMapService.gI().changeMap(plChanged, zone, plChanged.location.x, plChanged.location.y);
                            Service.gI().sendThongBao(plChanged, "Chúc mừng bạn đã có cái tên mới đẹp đẽ hơn tên ban đầu");
                            Service.gI().sendThongBao(player, "Đổi tên người chơi thành công");
                        }
                    }
                }
                break;
                case CHANGE_NAME_BY_ITEM: {
                    if (player != null) {
                        if (AlyraManager.executeQuery("select * from player where name = ?", text[0]).next()) {
                            Service.gI().sendThongBao(player, "Tên nhân vật đã tồn tại");
                            createFormChangeNameByItem(player);
                        } else if (Util.haveSpecialCharacter(text[0])) {
                            Service.gI().sendThongBaoOK(player, "Tên nhân vật không được chứa ký tự đặc biệt");
                        } else if (text[0].length() < 5) {
                            Service.gI().sendThongBaoOK(player, "Tên nhân vật quá ngắn");
                        } else if (text[0].length() > 10) {
                            Service.gI().sendThongBaoOK(player, "Tên nhân vật chỉ đồng ý các ký tự a-z, 0-9 và chiều dài từ 5 đến 10 ký tự");
                        } else {
                            Item theDoiTen = InventoryService.gI().findItem(player.inventory.itemsBag, 2006);
                            if (theDoiTen == null) {
                        theDoiTen = InventoryService.gI().findItem(player.inventory.itemsBag, 1935);
                            }
                            if (theDoiTen == null) {
                                Service.gI().sendThongBao(player, "Không tìm thấy thẻ đổi tên");
                            } else {
                                InventoryService.gI().subQuantityItemsBag(player, theDoiTen, 1);
                                player.name = text[0].toLowerCase();
                                AlyraManager.executeUpdate("update player set name = ? where id = ?", player.name, player.id);
                                Service.gI().player(player);
                                Service.gI().Send_Caitrang(player);
                                Service.gI().sendFlagBag(player);
                                Zone zone = player.zone;
                                ChangeMapService.gI().changeMap(player, zone, player.location.x, player.location.y);
                                Service.gI().sendThongBao(player, "Chúc mừng bạn đã có cái tên mới đẹp đẽ hơn tên ban đầu");
                            }
                        }
                    }
                }
                break;
                case CHOOSE_LEVEL_BDKB:
                    int level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.QUY_LAO_KAME, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, ConstNpc.MENU_ACCEPT_GO_TO_BDKB,
                                    "Con có chắc muốn đến\nhang kho báu cấp độ " + level + " ?",
                                    new String[]{"Đồng ý", "Từ chối"}, level);
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }

                    break;
                case CHOOSE_LEVEL_KGHD:
                    level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.MR_POPO, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, 2,
                                    "Cậu có chắc muốn đến\nDestron Gas cấp độ " + level + " ?",
                                    new String[]{"Đồng ý", "Từ chối"}, level);
                        }
                    }
                    break;
                case CHOOSE_LEVEL_CDRD:
                    level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.THAN_VU_TRU, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, 3,
                                    "Con có chắc muốn đến\ncon đường rắn độc cấp độ " + level + " ?",
                                    new String[]{"Đồng ý", "Từ chối"}, level);
                        }
                    }
                    break;
                case MBV:
                    int mbv = Integer.parseInt(text[0]);
                    int nmbv = Integer.parseInt(text[1]);
                    int rembv = Integer.parseInt(text[2]);
                    if ((mbv + "").length() != 6 || (nmbv + "").length() != 6 || (rembv + "").length() != 6) {
                        Service.gI().sendThongBao(player, "Trêu bố mày à?");
                    } else if (player.mbv == 0) {
                        Service.gI().sendThongBao(player, "Bạn chưa cài mã bảo vệ!");
                    } else if (player.mbv != mbv) {
                        Service.gI().sendThongBao(player, "Mã bảo vệ không đúng");
                    } else if (nmbv != rembv) {
                        Service.gI().sendThongBao(player, "Mã bảo vệ không trùng khớp");
                    } else {
                        player.mbv = nmbv;
                        Service.gI().sendThongBao(player, "Đổi mã bảo vệ thành công!");
                    }
                    break;
                case BANSLL:
                    // Check if the input contains only numbers
                    String input = text[0];
                    if (!input.matches("^[0-9\\\\s,\\\\.]+$")) {
                        Service.gI().sendThongBao(player, "Vui lòng nhập số hợp lệ (không có ký tự đặc biệt).");
                        break;
                    }

                    // Proceed with the original logic if the input is valid
                    int sltv;
                    try {
                        sltv = Math.abs(Integer.parseInt(input));
                    } catch (NumberFormatException ex) {
                        Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
                        break;
                    }
                    if (sltv <= 0) {
                        Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
                        break;
                    }
                    long cost = (long) sltv * 100000000;

                    int totalTV = InventoryService.gI().getNumItemBag(player, 457);
                    if (totalTV < sltv) {
                        Service.gI().sendThongBao(player, "Bạn chỉ có " + totalTV + " Thỏi vàng");
                    } else {
                        if (player.inventory.gold + cost > Inventory.LIMIT_GOLD) {
                            int slban = (int) ((Inventory.LIMIT_GOLD - player.inventory.gold) / 100000000);
                            if (slban < 1) {
                                Service.gI().sendThongBao(player, "Vàng sau khi bán vượt quá giới hạn");
                            } else if (slban < 2) {
                                Service.gI().sendThongBao(player, "Bạn chỉ có thể bán 1 Thỏi vàng");
                            } else {
                                Service.gI().sendThongBao(player, "Số lượng trong khoảng 1 tới " + slban);
                            }
                        } else {
                            InventoryService.gI().subQuantityItemsBag(player, 457, sltv);
                            InventoryService.gI().sendItemBags(player);
                            player.inventory.gold += cost;
                            Service.gI().sendMoney(player);
                            Service.gI().sendThongBao(player, "Đã bán " + sltv + " Thỏi vàng thu được " + Util.numberToMoney(cost) + " vàng");
                        }
                    }
                    break;

                case BANGHOI:
                    Clan clan = player.clan;
                    if (clan != null) {
                        ClanMember cm = clan.getClanMember((int) player.id);
                        if (clan.isLeader(player)) {
                            if (clan.canUpdateClan(player)) {
                                String tenvt = text[0];
                                if (!Util.haveSpecialCharacter(tenvt) && tenvt.length() > 1 && tenvt.length() < 5) {
                                    clan.name2 = tenvt;
                                    clan.update();
                                    Service.gI().sendThongBao(player, "[" + tenvt + "] OK");
                                } else {
                                    Service.gI().sendThongBaoOK(player, "Chỉ chấp nhận các ký tự a-z, 0-9 và chiều dài từ 2 đến 4 ký tự");
                                }
                            }
                        }
                    }
                    break;
                case DISSOLUTION_CLAN:
                    String xacNhan = text[0];
                    if (xacNhan.equalsIgnoreCase("OK")) {
                        clan = player.clan;
                        if (clan != null && clan.isLeader(player)) {
                            if (clan.members.size() > 1) {
                                Service.gI().sendThongBao(player, "Bang hội vẫn còn thành viên, không thể giải tán.");
                                return;
                            }
                            clan.deleteDB(clan.id);
                            Manager.CLANS.remove(clan);
                            player.clan = null;
                            player.clanMember = null;
                            ClanService.gI().sendMyClan(player);
                            ClanService.gI().sendClanId(player);
                            Service.gI().sendThongBao(player, "Bang hội đã giải tán thành công.");
                        } else {
                            Service.gI().sendThongBao(player, "Chỉ trưởng bang mới có thể giải tán bang hội.");
                        }
                    }
                    break;
                case DONATE_CAPSULE_BANG:
                    try {
                        int csDonate = Integer.parseInt(text[0]);
                        if (csDonate < 1 || csDonate > Integer.MAX_VALUE) {
                            Service.gI().sendThongBaoOK(player, "Số capsule không hợp lệ");
                            return;
                        }
                        Item csBang = InventoryService.gI().findItemBag(player, 1342);
                        if (csBang == null || csBang.quantity < csDonate) {
                            Service.gI().sendThongBaoOK(player, "Số lượng capsule không đủ để thực hiện");
                            return;
                        }
                        InventoryService.gI().subQuantityItemsBag(player, csBang, csDonate);
                        InventoryService.gI().sendItemBags(player);
                        player.clan.capsuleClan += csDonate;
                        player.clanMember.memberPoint = csBang.quantity;
                        player.clanMember.clanPoint += csDonate;
                        player.clan.sendMyClanForAllMember();
                        Service.gI().sendThongBaoOK(player, "Quyên góp thành công " + Util.format(csDonate) + " capsule cho bang");
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBaoOK(player, "Số capsule không hợp lệ");
                    }
                    break;
                case TRADE_MANH_VO:
                    try {
                        int qManhVo = Integer.parseInt(text[0].trim());
                        Trade tr = TransactionService.PLAYER_TRADE.get(player);
                        int idx = player.idMark.getIndexItemTrade();
                        player.idMark.setIndexItemTrade(-1);
                        if (tr != null && idx >= 0) {
                            Item it = tr.getItemBag(player, (byte) idx);
                            if (it != null && it.isNotNullItem() && it.isManhVoBongTai() && it.hasOption(31)) {
                                int max = it.getOptionParam(31);
                                if (qManhVo <= 0 || qManhVo > max) {
                                    tr.removeItemTradeOnly(player, (byte) idx);
                                    Service.gI().sendThongBao(player, "Số lượng không hợp lệ! (Tối đa " + max + ")");
                                    return;
                                }
                                tr.addItemTrade(player, (byte) idx, qManhVo);
                            } else {
                                tr.removeItemTradeOnly(player, (byte) idx);
                            }
                        }
                    } catch (Exception e) {
                        Trade tr = TransactionService.PLAYER_TRADE.get(player);
                        int idx = player.idMark.getIndexItemTrade();
                        player.idMark.setIndexItemTrade(-1);
                        if (tr != null && idx >= 0) {
                            tr.removeItemTradeOnly(player, (byte) idx);
                        }
                        Service.gI().sendThongBao(player, "Vui lòng nhập số hợp lệ!");
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            // Nhập sai định dạng số ở bất kỳ form nào -> báo lỗi thay vì crash luồng xử lý.
            if (player != null) {
                Service.gI().sendThongBao(player, "Dữ liệu nhập không hợp lệ, vui lòng nhập đúng định dạng số!");
            }
        } catch (Exception e) {
            utils.Logger.logException(Input.class, e, "Lỗi xử lý nhập liệu (doInput)");
        }
    }

    public void createForm(Player pl, int typeInput, String title, SubInput... subInputs) {
        pl.idMark.setTypeInput(typeInput);
        Message msg = null;
        try {
            msg = new Message(-125);
            msg.writer().writeUTF(title);
            msg.writer().writeByte(subInputs.length);
            for (SubInput si : subInputs) {
                msg.writer().writeUTF(si.name);
                msg.writer().writeByte(si.typeInput);
            }
            pl.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void createForm(ISession session, int typeInput, String title, SubInput... subInputs) {
        Message msg = null;
        try {
            msg = new Message(-125);
            msg.writer().writeUTF(title);
            msg.writer().writeByte(subInputs.length);
            for (SubInput si : subInputs) {
                msg.writer().writeUTF(si.name);
                msg.writer().writeByte(si.typeInput);
            }
            session.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void createFormUseGoldBar(Player pl) {
        createForm(pl, USE_GOLD_BAR, "Sử dụng thỏi vàng nhanh\n1 Thỏi Vàng = 100Tr Vàng . Max hành trang chứa được 2000 Tỷ", new SubInput("Nhập số thỏi vàng muốn sử dụng", NUMERIC));
    }

    /** Form nhập số lượng phiếu đổi VNĐ (1340: 10k, 1341: 8k) muốn dùng một lần. */
    public void createFormDungPhieuVnd(Player pl, int itemId, int donGia) {
        int dangCo = InventoryService.gI().getNumItemBag(pl, itemId);
        createForm(pl, itemId == 1340 ? DUNG_PHIEU_VND_10K : DUNG_PHIEU_VND_8K,
                "Phiếu đổi VNĐ\n1 phiếu = " + Util.numberToMoney(donGia) + " VNĐ\nĐang có " + dangCo + " phiếu",
                new SubInput("Nhập số lượng muốn dùng", NUMERIC));
    }

    public void createFormAdminAuth(Player pl) {
        createForm(pl, ADMIN_AUTH, "Xác thực Admin\nNhập mật khẩu để mở khoá lệnh.", new SubInput("Mật khẩu", PASSWORD));
    }

    public void createFormTangRuby(Player pl) {
        createForm(pl, TANG_NGOC_HONG, "Tặng ngọc", new SubInput("Tên nhân vật", ANY),
                new SubInput("Số Hồng Ngọc Muốn Tặng", NUMERIC));
    }

    public void createFormTangGem(Player pl) {
        createForm(pl, TANG_NGOC_XANH, "Tặng ngọc", new SubInput("Tên nhân vật", ANY),
                new SubInput("Số Ngọc xanh Muốn Tặng", NUMERIC));
    }

    public void createFormCSMM(Player player) {
        createForm(player, CSMMNE,
                "Trò chơi con số ", new SubInput("Nhập số lựa chọn : (1-999)", NUMERIC));
    }

    public void BETCHAN_xocdia(Player pl) {
        createForm(pl, BETCHAN, "Chọn số hồng ngọc đặt Chẵn\nTối thiểu 1K - Tối đa 1Tr", new SubInput("Số Hồng ngọc cược", ANY));
    }

    public void BETLE_xocdia(Player pl) {
        createForm(pl, BETLE, "Chọn số hồng ngọc đặt Lẻ\nTối thiểu 1K - Tối đa 1Tr", new SubInput("Số Hồng ngọc cược", ANY));
    }

    public void BET3D1T_xocdia(Player pl) {
        createForm(pl, BET3D1T, "Chọn số hồng ngọc đặt 3 Đỏ 1 Trắng\nTối thiểu 1K - Tối đa 1Tr", new SubInput("Số Hồng ngọc cược", ANY));
    }

    public void BET3T1D_xocdia(Player pl) {
        createForm(pl, BET3T1D, "Chọn số hồng ngọc đặt 3 Trắng 1 Đỏ\nTối thiểu 1K - Tối đa 1Tr", new SubInput("Số Hồng ngọc cược", ANY));
    }

    public void BET4D_xocdia(Player pl) {
        createForm(pl, BET4D, "Chọn số hồng ngọc đặt Tứ Tử Đỏ\nTối thiểu 1K - Tối đa 1Tr", new SubInput("Số Hồng ngọc cược", ANY));
    }

    public void BET4T_xocdia(Player pl) {
        createForm(pl, BET4T, "Chọn số hồng ngọc đặt Tứ Tử Trắng\nTối thiểu 1K - Tối đa 1Tr", new SubInput("Số Hồng ngọc cược", ANY));
    }

    public void taixiu_Tai(Player pl) {
        createForm(pl, TAIXIU_TAI, "Đặt cược Tài Xỉu (Cửa Tài)", new SubInput("Số thỏi vàng cược (10 -> 100,000)", NUMERIC));
    }

    public void taixiu_Xiu(Player pl) {
        createForm(pl, TAIXIU_XIU, "Đặt cược Tài Xỉu (Cửa Xỉu)", new SubInput("Số thỏi vàng cược (10 -> 100,000)", NUMERIC));
    }

    public void createFormBuffVND(Player player) {
        createForm(player, BUFFVND, "Buff VNĐ, Tổng Nạp & Số Tiền",
                new SubInput("Tên TK hoặc Tên NV", ANY),
                new SubInput("VNĐ (CASH) CẦN BUFF", NUMERIC),
                new SubInput("TỔNG NẠP CẦN BUFF", NUMERIC),
                new SubInput("SỐ TIỀN CẦN BUFF", NUMERIC));
    }

    public void createFormBotQuaiNappa(Player pl) {
        createForm(pl, BOTQUAI_NAPPA, "Buff Bot Quái",
                new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotQuaiTuonglai(Player pl) {
        createForm(pl, BOTQUAI_TUONGLAI, "Buff Bot Quái",
                new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotQuaiCold(Player pl) {
        createForm(pl, BOTQUAI_COLD, "Buff Bot Quái",
                new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormSenditem1(Player pl) {
        createForm(pl, SEND_ITEM_OP, "Tặng Vật Phẩm",
                new SubInput("Tên người chơi", ANY),
                new SubInput("Id Item", ANY),
                new SubInput("Chuỗi Optin ID-PramvID-Pram : 50-20v30-1", ANY),
                new SubInput("Số lượng", ANY));
    }

    public void createFormBotQuai(Player pl) {
        createForm(pl, BOTQUAI, "Buff Bot Quái",
                new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotBoss(Player pl) {
        createForm(pl, BOTBOSS, "Buff Bot Boss",
                new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotItem(Player pl) {
        createForm(pl, BOTITEM, "Buff Bot Item",
                new SubInput("số lượng bot", NUMERIC),
                new SubInput("id item cần bán", NUMERIC),
                new SubInput("id item trao đổi", NUMERIC),
                new SubInput("số lượng yêu cầu trao đổi", NUMERIC));
    }

    public void createFormDonateCapsuleBang(Player pl) {
        Item item = InventoryService.gI().findItemBag(pl, 1342);
        createForm(pl, DONATE_CAPSULE_BANG, "Donate Capsule Bang"
                + "\n(Capsule của bạn sẽ được quyên góp vào bang hội)\nSố Capsule hiện có: " + (item != null ? item.quantity : 0), new SubInput("Nhập số lượng capsule muốn quyên góp", NUMERIC));
    }

    public void createFormConSoMayMan_Gem(Player pl) {
        createForm(pl, CON_SO_MAY_MAN_NGOC, "Hãy chọn 1 số từ 0 đến 99 giá 5 ngọc", new SubInput("Số bạn chọn", NUMERIC));
    }

    public void createFormThongBao(Player pl) {
        createForm(pl, DUNGLOA, "Chat Toàn Sever", new SubInput("Nội Dung", NUMERIC));
    }

    public void createFormChangePassword(Player pl) {
        createForm(pl, CHANGE_PASSWORD, "Đổi mật khẩu", new SubInput("Mật khẩu cũ", PASSWORD),
                new SubInput("Mật khẩu mới", PASSWORD),
                new SubInput("Nhập lại mật khẩu mới", PASSWORD));
    }

    public void createFormGiveItem(Player pl) {
        createForm(pl, GIVE_IT, "Tặng vật phẩm", new SubInput("Tên", ANY), new SubInput("Id Item", ANY), new SubInput("ID OPTION", ANY), new SubInput("PARAM", ANY), new SubInput("Số lượng", ANY));
    }

    public void createFormGetItem(Player pl) {
        createForm(pl, GET_IT, "Get vật phẩm", new SubInput("Id Item", ANY), new SubInput("ID OPTION", ANY), new SubInput("PARAM", ANY), new SubInput("Số lượng", ANY));
    }

    public void createFormGiftCode(Player pl) {
        createForm(pl, GIFT_CODE, "Giftcode", new SubInput("Gift-code", ANY));
    }

    public void createFormMBV(Player pl) {
        createForm(pl, MBV, "Đồ ngu! Đồ ăn hại! Cút mẹ mày đi!", new SubInput("Nhập Mã Bảo Vệ Đã Quên", NUMERIC), new SubInput("Nhập Mã Bảo Vệ Mới", NUMERIC), new SubInput("Nhập Lại Mã Bảo Vệ Mới", NUMERIC));
    }

    public void createFormBangHoi(Player pl) {
        createForm(pl, BANGHOI, "Nhập tên viết tắt bang hội", new SubInput("Tên viết tắt từ 2 đến 4 kí tự", ANY));
    }

    public void createFormFindPlayer(Player pl) {
        createForm(pl, FIND_PLAYER, "Tìm kiếm người chơi", new SubInput("Tên người chơi", ANY));
    }

    public void createFormNapThe(Player pl, byte loaiThe) {
        pl.idMark.setLoaiThe(loaiThe);
        createForm(pl, NAP_THE, "Nạp thẻ", new SubInput("Mã thẻ", ANY), new SubInput("Seri", ANY));
    }

    public void createFormChangeName(Player pl, Player plChanged) {
        PLAYER_ID_OBJECT.put((int) pl.id, plChanged);
        createForm(pl, CHANGE_NAME, "Đổi tên " + plChanged.name, new SubInput("Tên mới", ANY));
    }

    public void createFormConSoMayMan_Gold(Player pl) {
        createForm(pl, CON_SO_MAY_MAN_VANG, "Hãy chọn 1 số từ 0 đến 99 giá 1.000.000 vàng", new SubInput("Số bạn chọn", NUMERIC));
    }

    public void createFormChangeNameByItem(Player pl) {
        createForm(pl, CHANGE_NAME_BY_ITEM, "Đổi tên " + pl.name, new SubInput("Tên mới", ANY));
    }

    public void createFormChooseLevelBDKB(Player pl) {
        createForm(pl, CHOOSE_LEVEL_BDKB, "Hãy chọn cấp độ hang kho báu từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormChooseLevelCDRD(Player pl) {
        createForm(pl, CHOOSE_LEVEL_CDRD, "Hãy chọn cấp độ từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormChooseLevelKGHD(Player pl) {
        createForm(pl, CHOOSE_LEVEL_KGHD, "Hãy chọn cấp độ từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormBanSLL(Player pl) {
        createForm(pl, BANSLL, "Bạn muốn bán bao nhiêu [Thỏi vàng] ?", new SubInput("Số lượng", NUMERIC));
    }

    public void createFormGiaiTanBangHoi(Player pl) {
        createForm(pl, DISSOLUTION_CLAN, "Nhập OK để xác nhận giải tán bang hội.", new SubInput("", ANY));
    }

    public void createFormTradeManhVo(Player pl, int maxQuantity) {
        createForm(pl, TRADE_MANH_VO, "Nhập số lượng (Tối đa: " + maxQuantity + ")",
                new SubInput("", NUMERIC));
    }

    public static class SubInput {

        private String name;
        private byte typeInput;

        public SubInput(String name, byte typeInput) {
            this.name = name;
            this.typeInput = typeInput;
        }
    }

}