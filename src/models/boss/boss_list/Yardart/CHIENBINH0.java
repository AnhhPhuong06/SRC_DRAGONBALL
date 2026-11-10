package models.boss.boss_list.Yardart;

 


import consts.BossID;
import models.boss.BossesData;
import static consts.BossType.YARDART;

public class CHIENBINH0 extends Yardart {

    public CHIENBINH0() throws Exception {
        super(YARDART, BossID.CHIEN_BINH_0, BossesData.CHIEN_BINH_0);
    }

    @Override
    protected void init() {
        x = 170;
        x2 = 240;
        y = 456;
        y2 = 456;
        range = 1000;
        range2 = 150;
        timeHoiHP = 20000;
        rewardRatio = 3;
    }

}
