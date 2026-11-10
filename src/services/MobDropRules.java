package services;

import com.google.gson.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import models.item.Item;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Pet;
import models.player.Player;
import utils.Util;

/** Additive mob drops; immutable snapshots are published only after a complete file save. */
public final class MobDropRules {
    public static final int MAX_RULES = 100;
    public record Option(int id, int param) {}
    public record Rule(int id, String name, boolean enabled, int mapId, int mobId, int itemId,
                       double chance, int minQuantity, int maxQuantity, List<Option> options) {
        public Rule {
            if (id < 0 || name == null || name.isBlank() || name.length() > 100 || name.indexOf('\0') >= 0
                    || mapId < -1 || mapId > 32767 || mobId < -1 || mobId == 0 || mobId > 254
                    || itemId < 0 || itemId > 32767 || !Double.isFinite(chance) || chance < 0 || chance > 100
                    || BigDecimal.valueOf(chance).stripTrailingZeros().scale() > 4
                    || minQuantity < 1 || maxQuantity < minQuantity || maxQuantity > 99999
                    || options == null || options.size() > 32) throw new IllegalArgumentException("Quy tắc drop không hợp lệ");
            Set<Integer> ids = new HashSet<>();
            for (Option option : options) {
                if (option == null || option.id() < 0 || option.id() > 255 || option.param() < 0
                        || option.param() > 65535 || !ids.add(option.id())) throw new IllegalArgumentException("Option drop trùng hoặc vượt giới hạn");
            }
            options = List.copyOf(options);
        }
        public Rule withId(int value) { return new Rule(value, name, enabled, mapId, mobId, itemId, chance, minQuantity, maxQuantity, options); }
    }
    public record State(long revision, int nextId, List<Rule> rules) {
        public State { rules = List.copyOf(rules); }
    }
    public record Reward(int itemId, int quantity, List<Option> options) {}
    private static class Holder { static final MobDropRules INSTANCE = new MobDropRules(Path.of("data/config/mob_drop_rules.json")); }
    public static MobDropRules get() { return Holder.INSTANCE; }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private volatile State state = new State(0, 1, List.of());
    private volatile String loadError;

    public MobDropRules(Path path) {
        this.path = path;
        try { reload(); }
        catch (IOException | RuntimeException error) { loadError = "Không đọc được cấu hình drop tùy chỉnh. Kiểm tra file cấu hình trước khi lưu."; }
    }
    public State state() { return state; }
    public String loadError() { return loadError; }

    public synchronized void reload() throws IOException {
        if (!Files.exists(path)) {
            if (state.revision() != 0) throw new IOException("Thiếu file cấu hình drop đã lưu");
            loadError = null;
            return;
        }
        if (Files.size(path) > 1_048_576) throw new IOException("File drop quá lớn");
        try {
            JsonObject json = new JsonParser().parse(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported drop version");
            long revision = json.get("revision").getAsLong();
            int nextId = json.get("nextId").getAsInt();
            List<Rule> rules = new ArrayList<>();
            Set<Integer> ids = new HashSet<>();
            for (JsonElement entry : json.getAsJsonArray("rules")) {
                JsonObject r = entry.getAsJsonObject();
                List<Option> options = new ArrayList<>();
                for (JsonElement element : r.getAsJsonArray("options")) {
                    JsonObject option = element.getAsJsonObject();
                    options.add(new Option(option.get("id").getAsInt(), option.get("param").getAsInt()));
                }
                Rule rule = new Rule(r.get("id").getAsInt(), r.get("name").getAsString(), r.get("enabled").getAsBoolean(),
                        r.get("mapId").getAsInt(), r.get("mobId").getAsInt(), r.get("itemId").getAsInt(),
                        r.get("chance").getAsDouble(), r.get("minQuantity").getAsInt(), r.get("maxQuantity").getAsInt(), options);
                if (rule.id() <= 0 || !ids.add(rule.id())) throw new IllegalArgumentException("Duplicate drop ID");
                rules.add(rule);
            }
            if (revision < 0 || nextId <= ids.stream().mapToInt(Integer::intValue).max().orElse(0)
                    || rules.size() > MAX_RULES) throw new IllegalArgumentException("Invalid drop state");
            state = new State(revision, nextId, rules);
            loadError = null;
        } catch (RuntimeException error) { throw new IOException("Cấu hình drop không hợp lệ", error); }
    }
    public synchronized Rule save(Rule rule, long revision) throws IOException {
        checkRevision(revision);
        List<Rule> rules = new ArrayList<>(state.rules());
        int nextId = state.nextId();
        if (rule.id() == 0) {
            if (rules.size() >= MAX_RULES || nextId == Integer.MAX_VALUE) throw new IllegalArgumentException("Đã đạt giới hạn 100 quy tắc drop");
            rule = rule.withId(nextId++);
            rules.add(rule);
        } else {
            int index = -1;
            for (int i = 0; i < rules.size(); i++) if (rules.get(i).id() == rule.id()) index = i;
            if (index < 0) throw new NoSuchElementException("Không tìm thấy quy tắc drop");
            rules.set(index, rule);
        }
        persist(new State(state.revision() + 1, nextId, rules));
        return rule;
    }
    public synchronized void delete(int id, long revision) throws IOException {
        checkRevision(revision);
        List<Rule> rules = new ArrayList<>(state.rules());
        if (!rules.removeIf(rule -> rule.id() == id)) throw new NoSuchElementException("Không tìm thấy quy tắc drop");
        persist(new State(state.revision() + 1, state.nextId(), rules));
    }
    private void checkRevision(long revision) throws IOException {
        if (loadError != null) throw new IOException(loadError);
        if (revision != state.revision()) throw new ConcurrentModificationException("Danh sách drop đã thay đổi. Tải lại trước khi lưu.");
    }
    private void persist(State next) throws IOException {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("version", 1); json.put("revision", next.revision()); json.put("nextId", next.nextId()); json.put("rules", next.rules());
        DropConfigFiles.write(path, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        state = next;
    }
    public List<Reward> roll(int mapId, int mobId, IntSupplier chanceRoll, IntBinaryOperator quantityRoll) {
        List<Reward> rewards = new ArrayList<>();
        for (Rule rule : state.rules()) {
            if (!rule.enabled() || rule.chance() <= 0 || (rule.mapId() != -1 && rule.mapId() != mapId)
                    || (rule.mobId() != -1 && rule.mobId() != mobId)) continue;
            int threshold = (int) Math.round(rule.chance() * 10_000);
            if (threshold < 1_000_000 && chanceRoll.getAsInt() >= threshold) continue;
            int quantity = rule.minQuantity() == rule.maxQuantity() ? rule.minQuantity()
                    : quantityRoll.applyAsInt(rule.minQuantity(), rule.maxQuantity());
            rewards.add(new Reward(rule.itemId(), quantity, rule.options()));
        }
        return rewards;
    }
    public void appendDrops(List<ItemMap> drops, Zone zone, int mobId, Player killer, int x, int y) {
        Player owner = killer instanceof Pet pet ? pet.master : killer;
        if (owner == null || !owner.isPl() || mobId == 0) return;
        for (Reward reward : roll(zone.map.mapId, mobId, () -> Util.nextInt(1_000_000), Util::nextInt)) {
            if (ItemService.gI().getTemplate(reward.itemId()) == null) continue;
            boolean valid = reward.options().stream().allMatch(option -> ItemService.gI().getItemOptionTemplate(option.id()) != null);
            if (!valid) continue;
            ItemMap item = new ItemMap(zone, reward.itemId(), reward.quantity(), x, y, owner.id);
            for (Option option : reward.options()) item.options.add(new Item.ItemOption(option.id(), option.param()));
            drops.add(item);
        }
    }
}
