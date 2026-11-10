package services.func;

import java.util.List;

import database.daos.GiftDataDAO;
import database.daos.GiftDataDAO.Mail;
import database.daos.GiftDataDAO.Reward;
import database.daos.PlayerGiftDAO;
import database.daos.PlayerGiftDAO.GiftState;
import models.player.Player;
import server.Client;
import utils.Logger;

public final class GiftMailService {

    private static GiftMailService instance;

    private GiftMailService() {
    }

    public static GiftMailService gI() {
        if (instance == null) {
            instance = new GiftMailService();
        }
        return instance;
    }


    public int deliverTemplate(long playerId, int templateId) {
        if (playerId <= 0L || templateId <= 0) {
            return -1;
        }
        try {
            Mail template = GiftDataDAO.snapshot().mailTemplate(templateId);
            if (template == null) {
                Logger.error("gift99: khong tim thay gift_mail_template id=" + templateId
                        + " (hoac dang active = 0), khong gui duoc thu\n");
                return -1;
            }
            if (template.mailKind != 0) {
                Logger.error("gift99: gift_mail_template id=" + templateId + " la thu toan server"
                        + " (mailKind = " + template.mailKind + "), khong the gui rieng cho nguoi choi\n");
                return -1;
            }
            int mailId = GiftDataDAO.sendTemplateMail(playerId, templateId);
            if (mailId > 0) {
                notifyMailbox(playerId);
            }
            return mailId;
        } catch (Exception e) {
            Logger.logException(GiftMailService.class, e,
                    "gift mail deliverTemplate player=" + playerId + " template=" + templateId);
            return -1;
        }
    }

    public boolean deliverTemplateOnce(Player player, int templateId) {
        if (player == null || templateId <= 0) {
            return false;
        }
        try {
            GiftState state = PlayerGiftDAO.load(player.id);
            if (state == null) {
                return false;
            }
            if (!state.markClaimed(PlayerGiftDAO.KIND_MAIL_SENT, templateId)) {
                return false; // already delivered to this player
            }
            int mailId = deliverTemplate(player.id, templateId);
            if (mailId <= 0) {
                state.unmarkClaimed(PlayerGiftDAO.KIND_MAIL_SENT, templateId);
                return false;
            }

            PlayerGiftDAO.save(player.id);
            return true;
        } catch (Exception e) {
            Logger.logException(GiftMailService.class, e,
                    "gift mail deliverTemplateOnce player=" + player.id + " template=" + templateId);
            return false;
        }
    }

    public int deliver(long playerId, String sender, String title, String body, List<Reward> rewards,
            int expireDays) {
        if (playerId <= 0L) {
            return -1;
        }
        try {
            long expireTime = expireDays > 0
                    ? System.currentTimeMillis() + (expireDays * 86400000L)
                    : 0L;
            int mailId = GiftDataDAO.insertPersonalMail(playerId, sender, title, body, rewards,
                    expireTime);
            if (mailId > 0) {
                notifyMailbox(playerId);
            }
            return mailId;
        } catch (Exception e) {
            Logger.logException(GiftMailService.class, e, "gift mail deliver player=" + playerId);
            return -1;
        }
    }

    public boolean deliverFirstChargeMail(Player player) {
        if (player == null) {
            return false;
        }
        int templateId = GiftDataDAO.snapshot().firstChargeMailTemplate();
        if (templateId <= 0) {
            return false; // feature off
        }
        return deliverTemplateOnce(player, templateId);
    }

    private void notifyMailbox(long playerId) {
        try {
            Player online = Client.gI().getPlayer(playerId);
            if (online != null) {
                GiftSystemService.gI().sendMailUnreadCount(online);
            }
        } catch (Exception e) {
            Logger.logException(GiftMailService.class, e, "gift mail badge player=" + playerId);
        }
    }
}
