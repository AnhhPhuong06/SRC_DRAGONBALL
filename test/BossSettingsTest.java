package services;

import java.nio.file.*;
import java.util.*;
import models.boss.BossData;

public final class BossSettingsTest {
    private static int checks;
    static BossData stage() { return new BossData("Original", (byte) 0, new short[]{1,2,3,-1,-1,-1},
            10, new long[]{100,200}, new int[]{1}, new int[][]{{0,1,1000}}, new String[]{"hello"}, new String[0], new String[0], 60); }
    static BossSettings.Config config(String name, long hp) { return new BossSettings.Config(name, 2, 123,
            List.of(hp), List.of(2,3), List.of(10,11,12,-1,-1,-1), 120); }
    public static void main(String[] args) throws Exception {
        Path file = Path.of("data/config/test_boss.json");
        BossData original = stage(); var store = new BossSettings(file, Map.of("FIRST", original));
        var defaults = store.config("FIRST");
        check(store.state().revision()==0 && !Files.exists(file), "Opening editor does not create config");
        var saved = config("Boss %d", 900_000_000_001L);
        store.save("FIRST",saved,0);
        check(store.state().revision()==1 && store.config("FIRST").equals(saved),"Saved config published");
        check(original.getHp()[0]==900_000_000_001L && original.getSecondsRest()==120,"Runtime stage references updated");
        check(original.getSkillTemp()[0][2]==1000 && original.getTextS()[0].equals("hello"),"Skills and dialogue preserved");
        check(store.keysFor(new BossData[]{original, original}).equals(List.of("FIRST")),"Runtime maps by identity without duplicate aliases");
        check(store.keysFor(new BossData[]{stage()}).isEmpty(),"Different instance with same values is not mapped");
        var fresh = new BossSettings(file, Map.of("FIRST",stage()));
        check(fresh.config("FIRST").equals(saved) && fresh.state().revision()==1,"Restart restores exact long values and arrays");
        byte[] bytes=Files.readAllBytes(file);
        reject(ConcurrentModificationException.class,()->store.save("FIRST",config("Stale",1),0));
        reject(NoSuchElementException.class,()->store.save("../../outside",saved,1));
        reject(IllegalArgumentException.class,()->store.save("FIRST",config("Zero HP",0),1));
        for (String name : List.of("", "Bad %s", "%999999999d", "%n", "%d %d", "New\nline"))
            reject(IllegalArgumentException.class,()->store.save("FIRST",config(name,100),1));
        reject(IllegalArgumentException.class,()->BossSettings.validate(new BossSettings.Config("x",0,1,List.of(1L),List.of(1,1),List.of(1,2,3,-1,-1,-1),1)));
        reject(IllegalArgumentException.class,()->BossSettings.validate(new BossSettings.Config("x",0,1,List.of(1L),List.of(1),List.of(1,2,3),1)));
        check(Arrays.equals(bytes,Files.readAllBytes(file)) && store.config("FIRST").equals(saved),"Rejected edits leave disk and runtime unchanged");
        store.reset("FIRST",1);
        check(store.config("FIRST").equals(defaults) && !store.state().templates().get(0).customized(),"Reset restores original fields");
        check(!Files.readString(file).contains("Boss %d"),"Reset removes persisted override");
        // One invalid entry rejects the complete file, including an earlier valid entry.
        Path broken=Path.of("data/config/broken_boss.json");
        String json=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(broken,json.replace("\"overrides\": {","\"overrides\": {\"UNKNOWN\": {},"));
        BossData untouched=stage(); var bad=new BossSettings(broken,Map.of("FIRST",untouched));
        check(!bad.state().loadError().isEmpty() && untouched.getHp()[0]==100,"Invalid file applies no partial overrides");
        reject(java.io.IOException.class,()->bad.save("FIRST",saved,1));
        Path blocker=Path.of("data/config/not_a_directory");Files.writeString(blocker,"block");
        var io=new BossSettings(blocker.resolve("boss.json"),Map.of("FIRST",stage()));
        reject(java.io.IOException.class,()->io.save("FIRST",saved,0));
        check(io.state().revision()==0 && io.config("FIRST").equals(defaults),"Failed atomic write publishes nothing");
        System.out.println("PASS: "+checks+" boss settings persistence, validation, conflict and failure checks");
    }
    interface Work { void run() throws Exception; }
    static void reject(Class<? extends Exception> type, Work work) throws Exception {
        try { work.run(); throw new AssertionError("Expected "+type); } catch(Exception error) { check(type.isInstance(error),"Expected "+type+", got "+error); }
    }
    static void check(boolean ok,String label) { checks++;if(!ok)throw new AssertionError(label); }
}
