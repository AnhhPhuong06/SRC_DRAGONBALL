package services.top;

import database.AlyraManager;
import java.sql.Connection;
import java.sql.SQLException;
import models.player.Player;
import services.Service;
import services.top.TopManager.TopTemplate;
import utils.Logger;
import utils.TimeUtil;
import utils.Util;

/** The persistent hunting score is separate from spendable title/event points. */
public final class BossHuntRanking {
    public static final int LIMIT = 10;
    public static final String QUERY = "SELECT p.id,p.sanboss AS score FROM player p "
            + "JOIN account a ON a.id=p.account_id "
            + "WHERE p.sanboss>0 AND a.is_admin=0 AND a.ban=0 "
            + "ORDER BY p.sanboss DESC,p.id ASC LIMIT 100";

    private BossHuntRanking() {}

    public static void show(Player viewer) {
        if (viewer == null || viewer.getSession() == null) return;
        try (Connection con = AlyraManager.getConnection()) {
            con.setReadOnly(true);
            Service.gI().showListTop(viewer, load(con));
        } catch (Exception e) {
            Logger.logException(BossHuntRanking.class, e, "Lỗi đọc bảng xếp hạng săn boss");
            Service.gI().sendThongBao(viewer, "Chưa tải được bảng xếp hạng săn boss. Vui lòng thử lại sau.");
        }
    }

    /** Each opening reads committed scores; no player save, score reset, or reward campaign. */
    public static TopTemplate load(Connection con) throws SQLException {
        TopTemplate table = new TopTemplate(-1, "Top Săn Boss", QUERY);
        String updated = "|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm");
        try (var ps = con.prepareStatement(QUERY)) {
            ps.setQueryTimeout(5);
            try (var rs = ps.executeQuery()) {
                while (table.tops.size() < LIMIT && rs.next()) {
                    int id = rs.getInt("id");
                    long score = rs.getLong("score");
                    if (score <= 0) continue;
                    try {
                        TOP top = TOP.builder().id_player(id).build().findPlayer(con, id);
                        Player ranked = top.getPlayer();
                        if (ranked == null || ranked.isBot || ranked.isAdmin()) continue;
                        top.setValue(score);
                        top.setInfo1("Điểm săn boss: " + Util.format(score));
                        top.setInfo2("Điểm săn boss tích lũy\n" + updated);
                        table.tops.add(top);
                    } catch (Exception rowError) {
                        Logger.logException(BossHuntRanking.class, rowError, "Bỏ qua dữ liệu nhân vật lỗi trong top săn boss: " + id);
                    }
                }
            }
        }
        return table;
    }
}
