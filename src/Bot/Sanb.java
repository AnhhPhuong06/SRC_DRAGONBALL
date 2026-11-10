package Bot;

import consts.ConstPlayer;
import java.util.Random;
import managers.boss.BossManager;
import models.boss.Boss;
import models.mob.Mob;
import models.skill.Skill;
import services.SkillService;
import services.map.ChangeMapService;
import services.map.MapService;
import services.player.PlayerService;
import utils.Util;

public class Sanb {

    public Bot bot;

    public Boss bossAttack;

    public long lastTimeSkill1;

    public Sanb(Bot b) {
        this.bot = b;
    }

    public void update() {
        this.SanBot();
    }

    public boolean isMap(int mapId) {
        return (MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId)
                || MapService.gI().isMapBanDoKhoBau(mapId) || MapService.gI().isMapMaBu(mapId)
                || MapService.gI().isMapMaBu(mapId)
                || MapService.gI().isMapConDuongRanDoc(mapId));
    }

    public void GetBoss(int status) {
        if (this.bossAttack == null || this.bossAttack.isDie()) {
            // Chưa có boss nào trên server: nextInt(0) sẽ ném lỗi mỗi tick, và boss chưa
            // vào map thì zone null -> NPE. Cứ chờ lượt sau thay vì nổ liên tục.
            java.util.List<Boss> ds = BossManager.gI().getBosses();
            if (ds == null || ds.isEmpty()) {
                this.bossAttack = null;
                return;
            }
            this.bossAttack = ds.get(new Random().nextInt(ds.size()));
            if (this.bossAttack == null || this.bossAttack.zone == null) {
                this.bossAttack = null;
                return;
            }

            boolean bosAction = (!this.bossAttack.isDie() && !this.isMap(this.bossAttack.zone.map.mapId) && !this.bossAttack.zone.isFullPlayer() && this.bossAttack.zone.mobs.size() >= 1);
            if (bosAction) {
                ChangeMapService.gI().goToMap(this.bot, this.bossAttack.zone);
                this.bot.datViTriNgauNhien(); // goToMap không gán toạ độ -> bot nằm ở 0,0
                this.bot.zone.load_Me_To_Another(this.bot);
            }

            if (!bosAction && status < 3) {
                this.bossAttack = null;
                this.GetBoss(status + 1);
            } else if (!bosAction) {
                BotManager.gI().xoaBot(this.bot); // xoaBot đã tự exitMap + gỡ đệ tử
                this.bossAttack = null;
            }
        }
    }

    public void GetSkil() {
        if (Util.isTrue(50, 100)) {
            this.bot.playerSkill.skillSelect = this.bot.playerSkill.skills.get(0);
        } else {
            this.bot.playerSkill.skillSelect = this.bot.playerSkill.skills.get(1);
        }
        if (this.lastTimeSkill1 < System.currentTimeMillis() - 50000) {
            switch (this.bot.gender) {
                case ConstPlayer.XAYDA:
                    this.bot.useSkill(Skill.BIEN_KHI);
                    break;
                case ConstPlayer.TRAI_DAT:
                    this.bot.useSkill(Skill.QUA_CAU_KENH_KHI);
                    break;
                case ConstPlayer.NAMEC:
                    this.bot.useSkill(Skill.MAKANKOSAPPO);
                    break;
            }
            this.lastTimeSkill1 = System.currentTimeMillis() - new Random().nextInt(150000);
        }
    }

    private int getGroundY(int targetX, int targetY) {
        try {
            if (this.bot.zone == null || this.bot.zone.map == null) {
                return targetY;
            }
            if (this.bot.zone.map.mapId == 72) {
                return 312;
            }
            int groundY = this.bot.zone.map.yPhysicInTop(targetX, Math.max(0, targetY - 24));
            if (groundY <= 0) {
                groundY = this.bot.zone.map.yPhysicInTop(targetX, 0);
            }
            if (groundY <= 0 && this.bot.zone.mobs != null && !this.bot.zone.mobs.isEmpty()) {
                for (Mob m : this.bot.zone.mobs) {
                    if (m != null && m.location != null && m.location.y > groundY) {
                        groundY = m.location.y;
                    }
                }
            }
            if (groundY <= 0) {
                groundY = (this.bot.location != null && this.bot.location.y > 0) ? this.bot.location.y : targetY;
            }
            return groundY;
        } catch (Exception e) {
            return targetY;
        }
    }

    public void SanBot() {
        this.GetBoss(0);
        this.GetSkil();
        if (this.bossAttack != null && !this.bossAttack.isDie() && this.bossAttack.zone != null
                && this.bot.zone != null && this.bot.zone.equals(this.bossAttack.zone)) {
            if (this.bot.UseLastTimeSkill()) {
                int targetX = this.bossAttack.location.x + (this.bot.location.x > this.bossAttack.location.x ? 40 : -40);
                if (this.bot.zone.map != null) {
                    targetX = Math.max(24, Math.min(targetX, this.bot.zone.map.mapWidth - 24));
                }
                int targetY = getGroundY(targetX, this.bossAttack.location.y);
                if (Math.abs(this.bot.location.x - this.bossAttack.location.x) > 60 || Math.abs(this.bot.location.y - targetY) > 50) {
                    PlayerService.gI().playerMove(this.bot, targetX, targetY);
                }
                SkillService.gI().useSkill(this.bot, this.bossAttack, null, -1, null);
            }
        }
    }
}
