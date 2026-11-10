package services.tournament;

import managers.tournament.GuruGuildWarManager;
import models.item.Item;
import models.player.Player;
import models.tournament.guru_guild_war.GuruGuildWarMatch;
import services.ItemService;
import services.Service;
import services.map.ChangeMapService;
import services.player.InventoryService;
import services.player.PlayerService;
import utils.Util;

/**
 * Service cung cấp các API tương tác cho tính năng Đại Chiến Bang Hội Đảo Guru
 * @author GameDesigner_Ben
 */
public class GuruGuildWarService {

    private static GuruGuildWarService instance;

    public static GuruGuildWarService gI() {
        if (instance == null) {
            instance = new GuruGuildWarService();
        }
        return instance;
    }

    /**
     * Kiểm tra người chơi có đang trong trận chiến Guru War không
     */
    public boolean isInWar(Player player) {
        if (player == null || player.zone == null) return false;
        GuruGuildWarMatch match = GuruGuildWarManager.gI().getMatchByZone(player.zone);
        return match != null && match.isRunning;
    }

    /**
     * Kiểm tra 2 người chơi có đang trong cùng một trận đấu Guru War không
     */
    public boolean isFighting(Player p1, Player p2) {
        if (p1 == null || p2 == null || p1.zone == null || p2.zone == null) return false;
        if (!p1.zone.equals(p2.zone)) return false;
        GuruGuildWarMatch match = GuruGuildWarManager.gI().getMatchByZone(p1.zone);
        return match != null && match.isRunning;
    }

    /**
     * Xác định quan hệ địch - ta theo ID bang hội
     * Cùng bang -> Đồng minh (false)
     * Khác bang -> Kẻ thù có thể tấn công (true)
     */
    public boolean isEnemy(Player p1, Player p2) {
        if (p1 == null || p2 == null) return false;
        if (p1.clan == null || p2.clan == null) return false;
        return p1.clan.id != p2.clan.id;
    }

    /**
     * Báo danh giải đấu
     */
    public void register(Player player) {
        GuruGuildWarManager.gI().register(player);
    }

    /**
     * Tiếp viện (vào lại map thi đấu nếu bị văng hoặc vào muộn)
     */
    public void rejoinWar(Player player) {
        if (player.clan == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có bang hội!");
            return;
        }

        GuruGuildWarMatch match = GuruGuildWarManager.gI().getMatchByPlayer(player);
        if (match == null || !match.isRunning) {
            Service.gI().sendThongBao(player, "Hiện tại bang hội của bạn không có trận đấu nào đang diễn ra!");
            return;
        }

        int targetX = (match.clanRed.id == player.clan.id) ? GuruGuildWarMatch.BASE_RED_X : GuruGuildWarMatch.BASE_BLUE_X;
        int targetY = (match.clanRed.id == player.clan.id) ? GuruGuildWarMatch.BASE_RED_Y : GuruGuildWarMatch.BASE_BLUE_Y;

        ChangeMapService.gI().changeMap(player, match.zone, targetX, targetY);
        // Cài cờ chiến cho người chơi tiếp viện (Cờ 1: Đỏ, Cờ 2: Xanh)
        Service.gI().changeFlag(player, match.clanRed.id == player.clan.id ? 1 : 2);
        Service.gI().sendThongBao(player, "|1|Bạn đã tiếp viện thành công vào Đấu Trường Đảo Guru!");
    }

    /**
     * Xử lý khi người chơi thoát game hoặc mất kết nối trong lúc trận đấu đang diễn ra
     */
    public void onPlayerDisconnect(Player player) {
        if (player == null || player.zone == null) return;
        GuruGuildWarMatch match = GuruGuildWarManager.gI().getMatchByZone(player.zone);
        if (match != null && match.isRunning) {
            match.onPlayerDisconnect(player);
        }
    }

    /**
     * Xử lý khi người chơi chết trong trận
     */
    public void onPlayerDie(Player victim, Player plAtt) {
        if (victim == null || victim.zone == null) return;
        GuruGuildWarMatch match = GuruGuildWarManager.gI().getMatchByZone(victim.zone);
        if (match != null && match.isRunning) {
            match.onPlayerDie(victim, plAtt);
        }
    }

    /**
     * Xử lý khi người chơi gây sát thương trong trận
     */
    public void onPlayerDamage(Player plAtt, Player victim, long damage) {
        if (plAtt == null || victim == null || plAtt.zone == null) return;
        GuruGuildWarMatch match = GuruGuildWarManager.gI().getMatchByZone(plAtt.zone);
        if (match != null && match.isRunning) {
            match.onPlayerDamage(plAtt, victim, damage);
        }
    }

    /**
     * Hồi sinh tại chỗ siêu tốc không giới hạn (Meta server lậu)
     * Tốn 1 Thỏi Vàng hoặc 50.000 Hồng Ngọc
     */
    public void reviveAtPlace(Player player) {
        if (!player.isDie() || !isInWar(player)) return;

        boolean canRevive = false;
        if (InventoryService.gI().getNumItemBag(player, 457) >= 1) {
            InventoryService.gI().subQuantityItemsBag(player, 457, 1);
            InventoryService.gI().sendItemBags(player);
            canRevive = true;
        } else if (player.inventory.gem >= 50_000) {
            player.inventory.gem -= 50_000;
            Service.gI().sendMoney(player);
            canRevive = true;
        } else {
            Service.gI().sendThongBao(player, "Cần 1 Thỏi Vàng hoặc 50.000 Hồng Ngọc để hồi sinh tại chỗ!");
            return;
        }

        if (canRevive) {
            Service.gI().hsChar(player, player.nPoint.hpMax, player.nPoint.mpMax);
            player.setJustRevivaled(); // 1.5s - 2s bất tử tránh shock dame
            Service.gI().sendThongBao(player, "|1|Hồi sinh tại chỗ thành công! Chiến đấu tiếp nào!");
        }
    }

    /**
     * Về Căn Cứ hồi máu miễn phí (khi bấm về nhà lúc chết trong Guru war)
     */
    public void respawnAtBase(Player player) {
        if (!isInWar(player)) return;

        GuruGuildWarMatch match = GuruGuildWarManager.gI().getMatchByZone(player.zone);
        if (match == null) return;

        int targetX = (match.clanRed.id == player.clan.id) ? GuruGuildWarMatch.BASE_RED_X : GuruGuildWarMatch.BASE_BLUE_X;
        int targetY = (match.clanRed.id == player.clan.id) ? GuruGuildWarMatch.BASE_RED_Y : GuruGuildWarMatch.BASE_BLUE_Y;

        // Cập nhật vị trí trước khi gọi hsChar để các người chơi khác nhận đúng tọa độ hồi sinh
        player.location.x = targetX;
        player.location.y = targetY;
        player.setJustRevivaled();
        Service.gI().hsChar(player, player.nPoint.hpMax, player.nPoint.mpMax);
        Service.gI().resetPoint(player, targetX, targetY);
        Service.gI().sendThongBao(player, "|2|Đã trở về Căn Cứ hồi phục 100% sinh lực!");
    }
}
