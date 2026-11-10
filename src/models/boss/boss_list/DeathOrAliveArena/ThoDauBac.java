package models.boss.boss_list.DeathOrAliveArena;

 


import consts.BossID;
import models.boss.BossesData;
import static consts.BossType.PHOBAN;
import models.player.Player;

public class ThoDauBac extends DeathOrAliveArena {

    public ThoDauBac(Player player) throws Exception {
        super(PHOBAN, BossID.THO_DAU_BAC, BossesData.THO_DAU_BAC);
        this.playerAtt = player;
    }
}
