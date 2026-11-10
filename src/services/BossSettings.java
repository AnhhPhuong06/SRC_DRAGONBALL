package services;

import com.google.gson.*;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import models.boss.BossData;
import models.boss.BossesData;

/** Persistent overrides for existing boss stages. Never constructs or registers a boss. */
public final class BossSettings {
    public static final long MAX_STAT = 1_000_000_000_000_000L;
    public record Config(String name, int gender, long dame, List<Long> hp, List<Integer> mapJoin,
                         List<Integer> outfit, int secondsRest) {
        public Config {
            hp = List.copyOf(hp); mapJoin = List.copyOf(mapJoin); outfit = List.copyOf(outfit);
        }
    }
    public record Entry(String key, Config config, boolean customized, String appearType) {}
    public record State(long revision, List<Entry> templates, String loadError) {}
    private static class Holder {
        static final BossSettings INSTANCE = new BossSettings(Path.of("data/config/boss_settings.json"), definitions());
    }
    public static BossSettings get() { return Holder.INSTANCE; }

    private final Path file;
    private final Map<String, BossData> definitions;
    private final Map<String, Config> defaults = new TreeMap<>();
    private Map<String, Config> overrides = new TreeMap<>();
    private long revision;
    private String loadError = "";

    BossSettings(Path file, Map<String, BossData> definitions) {
        this.file = file;
        this.definitions = new TreeMap<>(definitions);
        definitions.forEach((key, data) -> defaults.put(key, read(data)));
        if (!Files.exists(file)) return;
        try {
            JsonObject root = new JsonParser().parse(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (whole(root, "version") != 1) throw new IllegalArgumentException("version");
            long loadedRevision = whole(root, "revision");
            if (loadedRevision < 0 || loadedRevision >= 9_007_199_254_740_991L) throw new IllegalArgumentException("revision");
            Map<String, Config> loaded = new TreeMap<>();
            for (var pair : root.getAsJsonObject("overrides").entrySet()) {
                requireKey(pair.getKey());
                Config value = decode(pair.getValue().getAsJsonObject());
                validate(value);
                loaded.put(pair.getKey(), value);
            }
            // Publish only after the entire file has been checked.
            overrides = loaded; revision = loadedRevision;
            loaded.forEach((key, value) -> apply(this.definitions.get(key), value));
        } catch (Exception badFile) {
            loadError = "Không đọc được boss_settings.json. Hãy kiểm tra file trước khi lưu cấu hình boss.";
            System.err.println("[BossSettings] Configuration rejected: " + badFile.getClass().getSimpleName());
        }
    }

    private static Map<String, BossData> definitions() {
        Map<String, BossData> result = new TreeMap<>();
        try {
            for (var field : BossesData.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == BossData.class) {
                    BossData data = (BossData) field.get(null);
                    if (data != null) result.put(field.getName(), data);
                }
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        return result;
    }
    public synchronized State state() {
        List<Entry> rows = new ArrayList<>();
        definitions.forEach((key, data) -> rows.add(new Entry(key, read(data), overrides.containsKey(key),
                data.getTypeAppear() == null ? "UNKNOWN" : data.getTypeAppear().name())));
        return new State(revision, List.copyOf(rows), loadError);
    }
    public synchronized Config config(String key) { requireKey(key); return read(definitions.get(key)); }
    public synchronized List<String> keysFor(BossData[] data) {
        if (data == null) return List.of();
        List<String> keys = new ArrayList<>();
        for (BossData stage : data) definitions.forEach((key, value) -> { if (value == stage && !keys.contains(key)) keys.add(key); });
        return List.copyOf(keys);
    }
    public synchronized void save(String key, Config value, long expectedRevision) throws IOException {
        checkRevision(expectedRevision); requireKey(key); validate(value);
        Map<String, Config> next = new TreeMap<>(overrides); next.put(key, value);
        persist(next);
        overrides = next; revision++;
        apply(definitions.get(key), value);
    }
    public synchronized void reset(String key, long expectedRevision) throws IOException {
        checkRevision(expectedRevision); requireKey(key);
        Map<String, Config> next = new TreeMap<>(overrides); next.remove(key);
        persist(next);
        overrides = next; revision++;
        apply(definitions.get(key), defaults.get(key));
    }
    private void persist(Map<String, Config> next) throws IOException {
        var root = new LinkedHashMap<String, Object>();
        root.put("version", 1); root.put("revision", revision + 1); root.put("overrides", next);
        byte[] bytes = new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8);
        DropConfigFiles.write(file, bytes);
    }
    private void checkRevision(long expected) throws IOException {
        if (!loadError.isEmpty()) throw new IOException(loadError);
        if (revision != expected) throw new ConcurrentModificationException("Cấu hình boss đã thay đổi. Tải lại trước khi lưu.");
        if (revision >= 9_007_199_254_740_990L) throw new IOException("Revision exhausted");
    }
    private void requireKey(String key) {
        if (!definitions.containsKey(key)) throw new NoSuchElementException("Không tìm thấy mẫu boss: " + key);
    }
    private static Config read(BossData data) {
        return new Config(data.getName(), data.getGender(), data.getDame(),
                data.getHp() == null ? List.of() : Arrays.stream(data.getHp()).boxed().toList(),
                data.getMapJoin() == null ? List.of() : Arrays.stream(data.getMapJoin()).boxed().toList(),
                shorts(data.getOutfit()), data.getSecondsRest());
    }
    private static List<Integer> shorts(short[] values) {
        if (values == null) return List.of();
        List<Integer> result = new ArrayList<>(); for (short value : values) result.add((int) value); return result;
    }
    private static void apply(BossData target, Config value) {
        target.setName(value.name()); target.setGender((byte) value.gender()); target.setDame(value.dame());
        target.setHp(value.hp().stream().mapToLong(Long::longValue).toArray());
        target.setMapJoin(value.mapJoin().stream().mapToInt(Integer::intValue).toArray());
        short[] outfit = new short[value.outfit().size()];
        for (int i = 0; i < outfit.length; i++) outfit[i] = value.outfit().get(i).shortValue();
        target.setOutfit(outfit); target.setSecondsRest(value.secondsRest());
    }
    public static void validate(Config value) {
        if (value.name() == null || value.name().isBlank() || value.name().length() > 100 || value.name().chars().anyMatch(c -> c < 32))
            throw new IllegalArgumentException("Tên boss phải có 1–100 ký tự, không xuống dòng.");
        if (value.name().replace("%%", "").replace("%d", "").contains("%"))
            throw new IllegalArgumentException("Tên boss chỉ hỗ trợ %d và %%.");
        try { String.format(Locale.ROOT, value.name(), 0); }
        catch (IllegalFormatException error) { throw new IllegalArgumentException("Tên boss có ký hiệu % không hợp lệ; dùng %d cho số hoặc %% cho dấu %."); }
        if (value.gender() < 0 || value.gender() > 3) throw new IllegalArgumentException("Hành tinh phải từ 0 đến 3.");
        if (value.dame() < 0 || value.dame() > MAX_STAT) throw new IllegalArgumentException("Sát thương ngoài giới hạn.");
        if (value.hp().isEmpty() || value.hp().size() > 64 || value.hp().stream().anyMatch(v -> v < 1 || v > MAX_STAT))
            throw new IllegalArgumentException("Cần 1–64 giá trị HP dương, tối đa " + MAX_STAT + ".");
        if (value.secondsRest() < 0 || value.secondsRest() > 31_536_000) throw new IllegalArgumentException("Thời gian nghỉ phải từ 0 đến 31536000 giây.");
        if (value.mapJoin().size() > 256 || new HashSet<>(value.mapJoin()).size() != value.mapJoin().size()
                || value.mapJoin().stream().anyMatch(v -> v < -1 || v > 32767))
            throw new IllegalArgumentException("Danh sách map không hợp lệ hoặc trùng ID.");
        if (value.outfit().size() != 6 || value.outfit().stream().anyMatch(v -> v < -1 || v > 32767))
            throw new IllegalArgumentException("Ngoại hình cần 6 ID từ -1 đến 32767: đầu, thân, chân, túi, hào quang, hiệu ứng.");
    }
    // Gson 2.8.2 cannot deserialize records: decode strictly and keep integer precision.
    private static long whole(JsonObject object, String key) { return object.get(key).getAsBigDecimal().longValueExact(); }
    private static List<Long> longs(JsonObject object, String key) {
        List<Long> result = new ArrayList<>(); for (var value : object.getAsJsonArray(key)) result.add(value.getAsBigDecimal().longValueExact()); return result;
    }
    private static List<Integer> ints(JsonObject object, String key) { return longs(object, key).stream().map(Math::toIntExact).toList(); }
    private static Config decode(JsonObject object) {
        return new Config(object.get("name").getAsString(), Math.toIntExact(whole(object, "gender")), whole(object, "dame"),
                longs(object, "hp"), ints(object, "mapJoin"), ints(object, "outfit"), Math.toIntExact(whole(object, "secondsRest")));
    }
}
