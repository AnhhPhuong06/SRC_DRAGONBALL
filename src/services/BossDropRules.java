package services;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import models.item.Item;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import utils.Util;

/** Additional drops for an exact boss stage. Published only after an atomic file save. */
public final class BossDropRules {
    public static final int MAX_RULES = 500;
    public record Rule(int id, String name, boolean enabled, int mapId, String bossKey, int itemId,
                       double chance, int minQuantity, int maxQuantity, List<MobDropRules.Option> options) {
        public Rule {
            if (bossKey == null || !bossKey.matches("[A-Za-z_][A-Za-z0-9_]{0,99}"))
                throw new IllegalArgumentException("Chọn mẫu/giai đoạn boss hợp lệ");
            // The item, chance, quantity and option limits are identical to ordinary drops.
            var validated = new MobDropRules.Rule(id,name,enabled,mapId,-1,itemId,chance,minQuantity,maxQuantity,options);
            options = validated.options();
        }
        public Rule withId(int value) { return new Rule(value,name,enabled,mapId,bossKey,itemId,chance,minQuantity,maxQuantity,options); }
    }
    public record State(long revision, int nextId, List<Rule> rules) {
        public State { rules = List.copyOf(rules); }
    }
    public record Reward(int itemId, int quantity, List<MobDropRules.Option> options) {}
    private static class Holder { static final BossDropRules INSTANCE = new BossDropRules(Path.of("data/config/boss_drop_rules.json")); }
    public static BossDropRules get() { return Holder.INSTANCE; }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private volatile State state = new State(0,1,List.of());
    private volatile String loadError;

    public BossDropRules(Path path) {
        this.path=path;
        try { reload(); }
        catch (IOException | RuntimeException error) { loadError="Không đọc được boss_drop_rules.json. Kiểm tra file trước khi lưu drop boss."; }
    }
    public State state() { return state; }
    public String loadError() { return loadError; }
    public synchronized void reload() throws IOException {
        if (!Files.exists(path)) {
            if (state.revision()!=0) throw new IOException("Thiếu file drop boss đã lưu");
            loadError=null; return;
        }
        if (Files.size(path)>2_097_152) throw new IOException("File drop boss quá lớn");
        try {
            var json=new JsonParser().parse(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("version").getAsInt()!=1) throw new IllegalArgumentException("version");
            long revision=json.get("revision").getAsLong(); int nextId=json.get("nextId").getAsInt();
            List<Rule> rules=new ArrayList<>(); Set<Integer> ids=new HashSet<>();
            for (var element:json.getAsJsonArray("rules")) {
                var r=element.getAsJsonObject(); List<MobDropRules.Option> options=new ArrayList<>();
                for (var value:r.getAsJsonArray("options")) {
                    var o=value.getAsJsonObject();options.add(new MobDropRules.Option(o.get("id").getAsInt(),o.get("param").getAsInt()));
                }
                var rule=new Rule(r.get("id").getAsInt(),r.get("name").getAsString(),r.get("enabled").getAsBoolean(),
                        r.get("mapId").getAsInt(),r.get("bossKey").getAsString(),r.get("itemId").getAsInt(),
                        r.get("chance").getAsDouble(),r.get("minQuantity").getAsInt(),r.get("maxQuantity").getAsInt(),options);
                if (rule.id()<=0 || !ids.add(rule.id())) throw new IllegalArgumentException("Duplicate rule ID");
                rules.add(rule);
            }
            if (revision<0 || revision>=9_007_199_254_740_991L || nextId<=ids.stream().mapToInt(Integer::intValue).max().orElse(0)
                    || rules.size()>MAX_RULES) throw new IllegalArgumentException("Invalid drop state");
            state=new State(revision,nextId,rules);loadError=null;
        } catch (RuntimeException error) { throw new IOException("Cấu hình drop boss không hợp lệ",error); }
    }
    public synchronized Rule save(Rule rule,long revision) throws IOException {
        checkRevision(revision); List<Rule> rules=new ArrayList<>(state.rules());int nextId=state.nextId();
        if (rule.id()==0) {
            if (rules.size()>=MAX_RULES || nextId==Integer.MAX_VALUE) throw new IllegalArgumentException("Đã đạt giới hạn drop boss");
            rule=rule.withId(nextId++);rules.add(rule);
        } else {
            int index=-1;for(int i=0;i<rules.size();i++)if(rules.get(i).id()==rule.id())index=i;
            if(index<0)throw new NoSuchElementException("Không tìm thấy drop boss");rules.set(index,rule);
        }
        persist(new State(state.revision()+1,nextId,rules));return rule;
    }
    public synchronized void delete(int id,long revision) throws IOException {
        checkRevision(revision);List<Rule> rules=new ArrayList<>(state.rules());
        if(!rules.removeIf(r->r.id()==id))throw new NoSuchElementException("Không tìm thấy drop boss");
        persist(new State(state.revision()+1,state.nextId(),rules));
    }
    private void checkRevision(long revision) throws IOException {
        if(loadError!=null)throw new IOException(loadError);
        if(revision!=state.revision())throw new ConcurrentModificationException("Drop boss đã thay đổi. Tải lại trước khi lưu.");
        if(revision>=9_007_199_254_740_990L)throw new IOException("Revision exhausted");
    }
    private void persist(State next) throws IOException {
        var json=new LinkedHashMap<String,Object>();json.put("version",1);json.put("revision",next.revision());json.put("nextId",next.nextId());json.put("rules",next.rules());
        DropConfigFiles.write(path,GSON.toJson(json).getBytes(StandardCharsets.UTF_8));state=next;
    }
    public List<Reward> roll(int mapId,List<String> bossKeys,IntSupplier chanceRoll,IntBinaryOperator quantityRoll) {
        List<Reward> rewards=new ArrayList<>();
        for(var rule:state.rules()) {
            if(!rule.enabled() || rule.chance()<=0 || !bossKeys.contains(rule.bossKey()) || (rule.mapId()!=-1&&rule.mapId()!=mapId))continue;
            int threshold=(int)Math.round(rule.chance()*10_000);
            if(threshold<1_000_000&&chanceRoll.getAsInt()>=threshold)continue;
            int quantity=rule.minQuantity()==rule.maxQuantity()?rule.minQuantity():quantityRoll.applyAsInt(rule.minQuantity(),rule.maxQuantity());
            rewards.add(new Reward(rule.itemId(),quantity,rule.options()));
        }
        return rewards;
    }
    public void drop(Zone zone,List<String> bossKeys,Player owner,int x,int y) {
        if(zone==null || zone.map==null || owner==null || !owner.isPl() || bossKeys.isEmpty())return;
        int groundY=zone.map.tileMap==null?y:zone.map.yPhysicInTop(x,y-24);
        for(var reward:roll(zone.map.mapId,bossKeys,()->Util.nextInt(1_000_000),Util::nextInt)) {
            if(ItemService.gI().getTemplate(reward.itemId())==null || reward.options().stream().anyMatch(o->ItemService.gI().getItemOptionTemplate(o.id())==null))continue;
            var item=new ItemMap(zone,reward.itemId(),reward.quantity(),x,groundY,owner.id);
            for(var option:reward.options())item.options.add(new Item.ItemOption(option.id(),option.param()));
            Service.gI().dropItemMap(zone,item);
        }
    }
}
