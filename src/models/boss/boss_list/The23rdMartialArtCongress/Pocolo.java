package models.boss.boss_list.The23rdMartialArtCongress;

 


import consts.BossID;
import models.boss.BossesData;
import static consts.BossType.PHOBAN;
import models.player.Player;

public class Pocolo extends The23rdMartialArtCongress {

    public Pocolo(Player player) throws Exception {
        super(PHOBAN, BossID.JACKY_CHUN, BossesData.JACKY_CHUN);
        this.playerAtt = player;
    }
}
