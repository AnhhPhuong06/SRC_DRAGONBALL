package models.tournament.guru_guild_war;

import models.clan.Clan;
import models.item.Item;
import models.map.Zone;
import models.player.Player;
import services.ItemService;
import services.RadarService;
import services.Service;
import services.map.ChangeMapService;
import services.player.InventoryService;
import utils.Logger;
import utils.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý một trận đấu Đấu Trường Bang Hội Đảo Guru giữa 2 Bang
 * @author GameDesigner_Ben
 */
public class GuruGuildWarMatch {

    public static final int DURATION_MS = 15 * 60 * 1000; // 15 phút
    public static final int MAP_ID = 13; // Đảo Guru
    public static final int BASE_RED_X = 150;
    public static final int BASE_RED_Y = 384;
    public static final int BASE_BLUE_X = 1250;
    public static final int BASE_BLUE_Y = 384;

    public int matchId;
    public Clan clanRed;
    public Clan clanBlue;
    public Zone zone;

    public int scoreRed = 0;
    public int scoreBlue = 0;
    public long totalDamageRed = 0;
    public long totalDamageBlue = 0;

    public long startTime;
    public boolean isRunning;
    public boolean isEnded;
    public String winReason = "";
    public Clan winnerClan;
    public Clan loserClan;
    public long timeKickOut;

    public Player leaderRed;
    public Player leaderBlue;

    public GuruGuildWarMatch(int matchId, Clan clanRed, Clan clanBlue, Zone zone) {
        this.matchId = matchId;
        this.clanRed = clanRed;
        this.clanBlue = clanBlue;
        this.zone = zone;
        this.isRunning = false;
        this.isEnded = false;
    }

    /**
     * Bắt đầu trận chiến
     */
    public void startMatch() {
        this.isRunning = true;
        this.isEnded = false;
        this.startTime = System.currentTimeMillis();
        this.timeKickOut = 0;

        // Tìm Chủ Tướng (Bang Chủ -> Phó Bang -> Thành viên mạnh nhất) của 2 bên
        this.leaderRed = findCommander(clanRed);
        this.leaderBlue = findCommander(clanBlue);

        // Dịch chuyển thành viên 2 phe vào Căn Cứ & Cài cờ chiến (Phe Đỏ: cờ 1, Phe Xanh: cờ 2)
        teleportClanMembers(clanRed, BASE_RED_X, BASE_RED_Y, 1);
        teleportClanMembers(clanBlue, BASE_BLUE_X, BASE_BLUE_Y, 2);

        // Bật hào quang Vương Miện / Chiến Thần cho Chủ Tướng
        setupLeaderAura(this.leaderRed);
        setupLeaderAura(this.leaderBlue);

        // Thông báo cho cả 2 bang
        String startMsg = "|1|[ĐẠI CHIẾN ĐẢO GURU] Trận đấu đã bắt đầu!\n"
                + "|2|Thời gian: 15 phút.\n"
                + "|7|MỤC TIÊU: Bảo vệ Chủ Tướng phe mình và TRẢM CHỦ TƯỚNG đối phương!";
        managers.tournament.GuruGuildWarManager.sendThongBaoClan(clanRed, startMsg);
        managers.tournament.GuruGuildWarManager.sendThongBaoClan(clanBlue, startMsg);

        notifyZone("|1|TRẬN ĐẤU CHÍNH THỨC BẮT ĐẦU! BẢO VỆ CHỦ TƯỚNG!");
    }

    private Player findCommander(Clan clan) {
        if (clan == null) return null;
        // 1. Ưu tiên Bang Chủ online
        for (Player p : clan.membersInGame) {
            if (p != null && clan.isLeader(p)) {
                return p;
            }
        }
        // 2. Nếu Bang Chủ vắng mặt, chọn Phó Bang Chủ làm Quyền Chủ Tướng
        for (Player p : clan.membersInGame) {
            if (p != null && clan.isDeputy(p)) {
                return p;
            }
        }
        // 3. Nếu không có cả Phó Bang, chọn người chơi có Sức Mạnh cao nhất trong trận
        Player best = null;
        for (Player p : clan.membersInGame) {
            if (p != null) {
                if (best == null || (p.nPoint != null && (best.nPoint == null || p.nPoint.power > best.nPoint.power))) {
                    best = p;
                }
            }
        }
        return best;
    }

    private void setupLeaderAura(Player leader) {
        if (leader != null) {
            leader.idAuraTest = 10; // Hào quang vàng hoàng kim cho Chủ Tướng
            RadarService.gI().sendAura(leader, leader.getAura(), leader.getEffFront());
            Service.gI().sendThongBao(leader, "|1|BẠN LÀ CHỦ TƯỚNG! Nếu bạn ngã xuống, Bang sẽ THUA K.O LẬP TỨC!");
        }
    }

    private void removeLeaderAura(Player leader) {
        if (leader != null) {
            leader.idAuraTest = -1;
            RadarService.gI().sendAura(leader, leader.getAura(), leader.getEffFront());
        }
    }

    public void teleportClanMembers(Clan clan, int x, int y, int flag) {
        if (clan == null || zone == null) return;
        List<Player> members = new ArrayList<>(clan.membersInGame);
        for (Player p : members) {
            if (p != null && p.zone != null && !p.isDie()) {
                ChangeMapService.gI().changeMap(p, zone, x, y);
                Service.gI().changeFlag(p, flag);
                Service.gI().sendThongBao(p, "|2|Bạn đã được điều động đến Đấu Trường Đảo Guru!");
            }
        }
    }

    /**
     * Ghi nhận sát thương gây ra
     */
    public void onPlayerDamage(Player plAtt, Player victim, long damage) {
        if (!isRunning || plAtt == null || victim == null) return;
        if (plAtt.clan == null || victim.clan == null) return;

        if (plAtt.clan.id == clanRed.id) {
            totalDamageRed += damage;
        } else if (plAtt.clan.id == clanBlue.id) {
            totalDamageBlue += damage;
        }
    }

    /**
     * Xử lý khi có người chơi ngã xuống
     */
    public void onPlayerDie(Player victim, Player plAtt) {
        if (!isRunning || isEnded) return;
        if (victim == null || victim.clan == null) return;

        // Cập nhật điểm hạ gục
        if (plAtt != null && plAtt.clan != null) {
            if (plAtt.clan.id == clanRed.id && victim.clan.id == clanBlue.id) {
                scoreRed++;
            } else if (plAtt.clan.id == clanBlue.id && victim.clan.id == clanRed.id) {
                scoreBlue++;
            }
            notifyZone(plAtt.name + " (" + plAtt.clan.name + ") đã hạ gục " + victim.name + "! Tỷ số: ["
                    + clanRed.name + "] " + scoreRed + " - " + scoreBlue + " [" + clanBlue.name + "]");
        }

        // KIỂM TRA ĐIỀU KIỆN TRẢM TƯỚNG (Bang Chủ / Chủ Tướng bị tiêu diệt)
        if (victim.clan.isLeader(victim) || victim.equals(leaderRed) || victim.equals(leaderBlue)) {
            Clan win = (victim.clan.id == clanRed.id) ? clanBlue : clanRed;
            Clan lose = (victim.clan.id == clanRed.id) ? clanRed : clanBlue;
            finishMatch(win, lose, "TRẢM_TƯỚNG", victim);
        }
    }

    /**
     * Xử lý khi người chơi thoát game hoặc mất kết nối trong trận đấu
     */
    public void onPlayerDisconnect(Player player) {
        if (!isRunning || isEnded || player == null) return;

        if (player.equals(leaderRed) || (clanRed != null && clanRed.isLeader(player))) {
            notifyZone("|7|Chủ Tướng phe Đỏ [" + player.name + "] đã rời chiến trường!");
            finishMatch(clanBlue, clanRed, "TRẢM_TƯỚNG", player);
        } else if (player.equals(leaderBlue) || (clanBlue != null && clanBlue.isLeader(player))) {
            notifyZone("|7|Chủ Tướng phe Xanh [" + player.name + "] đã rời chiến trường!");
            finishMatch(clanRed, clanBlue, "TRẢM_TƯỚNG", player);
        } else {
            notifyZone(player.name + " (" + (player.clan != null ? player.clan.name : "") + ") đã ngắt kết nối và rời trận!");
        }
    }

    /**
     * Vòng lặp cập nhật trận đấu (gọi mỗi giây từ Manager)
     */
    public void update() {
        long now = System.currentTimeMillis();

        if (isRunning && !isEnded) {
            long elapsed = now - startTime;
            if (elapsed >= DURATION_MS) {
                // Hết giờ 15 phút -> Phân định thắng thua
                if (scoreRed > scoreBlue) {
                    finishMatch(clanRed, clanBlue, "KILL_SCORE", null);
                } else if (scoreBlue > scoreRed) {
                    finishMatch(clanBlue, clanRed, "KILL_SCORE", null);
                } else {
                    // Hòa điểm hạ gục -> So sánh Tổng Sát Thương
                    Clan win = (totalDamageRed >= totalDamageBlue) ? clanRed : clanBlue;
                    Clan lose = (win == clanRed) ? clanBlue : clanRed;
                    finishMatch(win, lose, "TỔNG_SÁT_THƯƠNG", null);
                }
            }
        }

        // Nếu đã kết thúc và hết thời gian nán lại -> Kick về làng
        if (isEnded && timeKickOut > 0 && now >= timeKickOut) {
            kickOutAllPlayers();
            timeKickOut = 0; // Đánh dấu đã kick xong
        }
    }

    /**
     * Kết thúc trận đấu và trao phần thưởng
     */
    public void finishMatch(Clan winner, Clan loser, String reason, Player deadLeader) {
        if (isEnded) return;
        this.isRunning = false;
        this.isEnded = true;
        this.winnerClan = winner;
        this.loserClan = loser;
        this.winReason = reason;
        this.timeKickOut = System.currentTimeMillis() + 15_000; // 15 giây ăn mừng trước khi đưa về làng

        // Gỡ hào quang Bang Chủ
        removeLeaderAura(leaderRed);
        removeLeaderAura(leaderBlue);

        // Chuẩn bị thông báo loa thế giới
        String broadcast;
        if ("TRẢM_TƯỚNG".equals(reason)) {
            String deadLeaderName = deadLeader != null ? deadLeader.name : "Chủ Tướng";
            broadcast = "|1|CÚ SỐC TẠI ĐẢO GURU!\n"
                    + "Bang [" + winner.name + "] đã Trảm Tướng [" + deadLeaderName + "] của Bang [" + loser.name
                    + "] và giành CHIẾN THẮNG K.O TUYỆT ĐỐI!";
        } else if ("KILL_SCORE".equals(reason)) {
            broadcast = "|2|HẾT GIỜ 15 PHÚT TẠI ĐẢO GURU!\n"
                    + "Bang [" + winner.name + "] xuất sắc đè bẹp Bang [" + loser.name
                    + "] với tỷ số (" + (winner == clanRed ? scoreRed + " - " + scoreBlue : scoreBlue + " - " + scoreRed)
                    + ") và giành CHIẾN THẮNG!";
        } else {
            broadcast = "|2|KẾT QUẢ ĐẠI CHIẾN ĐẢO GURU!\n"
                    + "Hòa điểm số! Bang [" + winner.name + "] chiến thắng Bang [" + loser.name
                    + "] nhờ có tổng sát thương áp đảo!";
        }
        Service.gI().sendThongBaoAllPlayer(broadcast);

        // Trao thưởng cho Bang Thắng
        rewardWinner(winner);

        // Trao quà an ủi cho Bang Thua
        rewardLoser(loser);

        notifyZone("|1|Trận đấu kết thúc! Toàn bộ người chơi sẽ được chuyển về làng sau 15 giây.");
    }

    private void giveRewardItem(Player p, Item item, String noticeFull) {
        if (!InventoryService.gI().addItemBag(p, item)) {
            InventoryService.gI().addItemGift(p, item);
            if (noticeFull != null && !noticeFull.isEmpty()) {
                Service.gI().sendThongBao(p, noticeFull);
            }
        }
    }

    private void rewardWinner(Clan clan) {
        if (clan == null) return;
        clan.capsuleClan += 2000; // +2.000 Capsule / Danh Vọng bang

        for (Player p : clan.membersInGame) {
            if (p != null && isInMatchZone(p)) {
                try {
                    boolean isLeader = clan.isLeader(p) || p.equals(leaderRed) || p.equals(leaderBlue);

                    // Thỏi Vàng (ID 457)
                    int goldBars = isLeader ? 500 : 50;
                    Item tv = ItemService.gI().createNewItem((short) 457, goldBars);
                    giveRewardItem(p, tv, "Hành trang đầy! " + goldBars + " Thỏi Vàng đã chuyển vào Rương Phụ.");

                    if (isLeader) {
                        // 5 Rương Huyền Bí (ID 1009)
                        Item ruong = ItemService.gI().createNewItem((short) 1009, 5);
                        giveRewardItem(p, ruong, "Hành trang đầy! 5 Rương Huyền Bí đã chuyển vào Rương Phụ.");
                    } else {
                        // 20 Mảnh Porata C2 (ID 933) + 10 Mảnh Porata C3 (ID 1493)
                        Item p2 = ItemService.gI().createNewItem((short) 933, 20);
                        p2.itemOptions.add(new Item.ItemOption(31, 20));
                        Item p3 = ItemService.gI().createNewItem((short) 1493, 10);
                        p3.itemOptions.add(new Item.ItemOption(31, 10));
                        giveRewardItem(p, p2, null);
                        giveRewardItem(p, p3, null);

                        // 5 Đá sao pha lê (ID 441 - 447)
                        short daId = (short) (441 + Util.nextInt(0, 6));
                        Item da = ItemService.gI().createNewItem(daId, 5);
                        giveRewardItem(p, da, null);
                    }

                    // Kích hoạt Buff Thần Guru (+20% SD đánh quái trong 24h)
                    p.isBuffGuruWar = true;
                    p.lastTimeWinGuruWar = System.currentTimeMillis();

                    InventoryService.gI().sendItemBags(p);
                    Service.gI().sendThongBao(p, "|1|CHÚC MỪNG CHIẾN THẮNG!\n"
                            + "Bạn nhận được quà thắng giải và Buff Thần Guru (+20% SĐ đánh quái) trong 24h!");
                } catch (Exception e) {
                    Logger.logException(GuruGuildWarMatch.class, e, "Lỗi trao thưởng winner clan");
                }
            }
        }
    }

    private void rewardLoser(Clan clan) {
        if (clan == null) return;
        clan.capsuleClan += 500; // +500 Capsule bang an ủi

        for (Player p : clan.membersInGame) {
            if (p != null && isInMatchZone(p)) {
                try {
                    // Quà an ủi: 10 Thỏi Vàng + 5 Mảnh Porata C2
                    Item tv = ItemService.gI().createNewItem((short) 457, 10);
                    Item p2 = ItemService.gI().createNewItem((short) 933, 5);
                    p2.itemOptions.add(new Item.ItemOption(31, 5));
                    giveRewardItem(p, tv, "Hành trang đầy! 10 Thỏi Vàng an ủi đã chuyển vào Rương Phụ.");
                    giveRewardItem(p, p2, null);
                    InventoryService.gI().sendItemBags(p);

                    Service.gI().sendThongBao(p, "|7|Rất tiếc bang hội đã bại trận!\n"
                            + "Bạn nhận được 10 Thỏi Vàng + 5 Mảnh Porata an ủi. Hãy phục thù vào ngày mai!");
                } catch (Exception e) {
                    Logger.logException(GuruGuildWarMatch.class, e, "Lỗi trao thưởng loser clan");
                }
            }
        }
    }

    public void kickOutAllPlayers() {
        if (zone == null) return;
        List<Player> players = new ArrayList<>(zone.getPlayers());
        for (Player p : players) {
            if (p != null) {
                // Tháo cờ chiến
                Service.gI().changeFlag(p, 0);
                // Đưa về Đảo Guru khu 0
                ChangeMapService.gI().changeMap(p, 13, 0, 600, 384);
            }
        }
    }

    public boolean isInMatchZone(Player p) {
        return p != null && p.zone != null && p.zone.equals(this.zone);
    }

    public boolean isFighting(Player p1, Player p2) {
        return isRunning && isInMatchZone(p1) && isInMatchZone(p2);
    }

    public boolean isEnemy(Player p1, Player p2) {
        if (p1 == null || p2 == null || p1.clan == null || p2.clan == null) return false;
        return p1.clan.id != p2.clan.id;
    }

    public void notifyZone(String text) {
        if (zone == null) return;
        for (Player p : zone.getPlayers()) {
            if (p != null) {
                Service.gI().sendThongBao(p, text);
            }
        }
    }
}
