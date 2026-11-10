package services;

import com.google.gson.Gson;
import consts.BossType;
import java.nio.file.*;
import java.util.*;
import models.boss.*;
import models.player.NPoint;

/** Separate JVM: persisted overrides must load before the first Boss reads secondsRest. */
public final class BossSettingsStartupTest {
    public static void main(String[] args) throws Exception {
        var value = new BossSettings.Config("Persisted boss %d", 2, 123456, List.of(987654321L),
                List.of(47), List.of(10,11,12,-1,-1,-1), 42);
        Files.writeString(Path.of("data/config/boss_settings.json"),new Gson().toJson(Map.of("version",1,"revision",3,"overrides",Map.of("TAUPAYPAY",value))));
        var first = new BaseFixtureBoss();
        if(first.secondsRest!=42 || first.data[0].getHp()[0]!=987654321L)throw new AssertionError("Constructor read default before loading persisted config");
        var second = new TypedFixtureBoss();
        if(second.secondsRest!=42)throw new AssertionError("Typed constructor did not apply persisted setting");
        first.currentLevel=0;
        first.nPoint=new NPoint(first){ @Override public void calPoint(){hpMax=hpg;dame=dameg;} };
        first.initBase();
        if(first.nPoint.hp!=987654321L || first.nPoint.dame!=123456 || !first.name.startsWith("Persisted boss "))
            throw new AssertionError("Actual Boss.initBase did not use configured stats/name");
        var store=BossSettings.get();
        long currentHp=first.nPoint.hp;
        store.save("TAUPAYPAY",new BossSettings.Config("Next boss",0,500,List.of(2000L),List.of(47),value.outfit(),90),3);
        if(first.nPoint.hp!=currentHp || first.nPoint.dame!=123456)throw new AssertionError("Edit modified an active fight");
        first.initBase();
        if(first.nPoint.hp!=2000 || first.nPoint.dame!=500)throw new AssertionError("Next init did not apply new settings");
        int valid=0;
        for(var entry:store.state().templates()) {BossSettings.validate(entry.config());valid++;}
        System.out.println("PASS: both constructors, startup persistence, current/next spawn and "+valid+" source boss stage validations");
    }
    static final class BaseFixtureBoss extends Boss {
        BaseFixtureBoss()throws Exception {super(-9090,BossesData.TAUPAYPAY);}
        @Override public void initSkill() {}
    }
    static final class TypedFixtureBoss extends Boss {
        TypedFixtureBoss()throws Exception {super(BossType.FINAL,-9091,BossesData.TAUPAYPAY);}
        @Override public void initSkill() {}
    }
}
