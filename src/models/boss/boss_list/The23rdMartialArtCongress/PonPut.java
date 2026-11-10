package models.boss.boss_list.The23rdMartialArtCongress;

 


import consts.BossID;
import models.boss.BossesData;
import static consts.BossType.PHOBAN;
import models.player.Player;

public class PonPut extends The23rdMartialArtCongress {

    public PonPut(Player player) throws Exception {
        super(PHOBAN, BossID.PON_PUT, BossesData.PON_PUT);
        this.playerAtt = player;
    }
}
