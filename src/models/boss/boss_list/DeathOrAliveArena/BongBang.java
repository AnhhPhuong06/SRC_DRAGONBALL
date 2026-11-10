package models.boss.boss_list.DeathOrAliveArena;

 


import consts.BossID;
import models.boss.BossesData;
import static consts.BossType.PHOBAN;
import models.player.Player;

public class BongBang extends DeathOrAliveArena {

    public BongBang(Player player) throws Exception {
        super(PHOBAN, BossID.BONG_BANG, BossesData.BONG_BANG);
        this.playerAtt = player;
    }
}
