package server.web.panel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static server.web.panel.PanelSupport.*;

/** Private panel-owned files only; never imports website/PHP or legacy backups. */
final class PanelFiles {
    static final int MAX_CONFIG = 1024 * 1024;
    private static final String BACKUP = "website-[0-9]{13}-[a-f0-9]{8}\\.json";
    private PanelFiles() {}

    static Path root() throws IOException {
        Path root = Path.of(System.getProperty("webpanel.data", "data/webpanel")).toAbsolutePath().normalize();
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (cwd.startsWith(root) || root.startsWith(cwd.resolve("web"))
                || root.startsWith(cwd.resolve("data/web"))) {
            throw error(500, "webpanel.data phải là thư mục riêng, ngoài web và data/web");
        }
        rejectLinks(root);
        Files.createDirectories(root);
        makePrivate(root, true);
        return root;
    }

    private static void rejectLinks(Path path) throws IOException {
        for (Path p = path; p != null; p = p.getParent()) {
            if (Files.isSymbolicLink(p)) throw error(500, "Không dùng liên kết tượng trưng cho dữ liệu panel");
        }
    }

    private static Path file(String name) throws IOException {
        if (!name.matches("[A-Za-z0-9.-]+")) throw error(400, "Tên file không hợp lệ");
        Path path = root().resolve(name);
        rejectLinks(path);
        return path;
    }

    static boolean exists(String name) throws IOException {
        return Files.isRegularFile(file(name), LinkOption.NOFOLLOW_LINKS);
    }

    static byte[] read(String name, int max) throws IOException {
        Path path = file(name);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw error(500, "Dữ liệu panel không hợp lệ");
        makePrivate(path, false);
        try (var in = Files.newInputStream(path)) {
            byte[] bytes = in.readNBytes(max + 1);
            if (bytes.length > max) throw error(500, "File dữ liệu panel quá lớn");
            return bytes;
        }
    }

    static JsonObject readJson(String name) throws IOException {
        byte[] bytes = read(name, MAX_CONFIG);
        if (bytes == null) return new JsonObject();
        return parse(bytes);
    }

    static JsonObject parse(byte[] bytes) {
        try {
            return new JsonParser().parse(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw error(500, "File cấu hình panel không hợp lệ; không tự ghi đè");
        }
    }

    static void writeJson(String name, JsonObject object) throws IOException {
        write(name, GSON.toJson(object).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static void write(String name, byte[] bytes) throws IOException {
        atomic(file(name), bytes);
    }

    private static void atomic(Path target, byte[] bytes) throws IOException {
        Path temp = Files.createTempFile(target.getParent(), ".panel-", ".tmp");
        try {
            makePrivate(temp, false);
            try (FileChannel out = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) out.write(buffer);
                out.force(true);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw error(500, "Ổ đĩa không hỗ trợ ghi cấu hình nguyên tử");
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static Path backupDir() throws IOException {
        Path dir = root().resolve("backups");
        rejectLinks(dir);
        Files.createDirectories(dir);
        makePrivate(dir, true);
        return dir;
    }

    static void backupWebsite() throws IOException {
        byte[] bytes = read("website.json", MAX_CONFIG);
        if (bytes == null) return;
        String name = "website-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ".json";
        atomic(backupDir().resolve(name), bytes);
        List<Map<String, Object>> backups = backups();
        for (int i = 25; i < backups.size(); i++) Files.deleteIfExists(backupDir().resolve((String) backups.get(i).get("name")));
    }

    static List<Map<String, Object>> backups() throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(backupDir())) {
            for (Path p : paths) {
                String name = p.getFileName().toString();
                if (!name.matches(BACKUP) || !Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) continue;
                makePrivate(p, false);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", name);
                row.put("size", Files.size(p));
                row.put("time", Instant.ofEpochMilli(Long.parseLong(name.substring(8, 21))).toString());
                result.add(row);
            }
        }
        result.sort(Comparator.comparing(row -> (String) row.get("name"), Comparator.reverseOrder()));
        return result;
    }

    static JsonObject restoreWebsite(String name) throws IOException {
        if (name.isEmpty()) {
            List<Map<String, Object>> list = backups();
            if (list.isEmpty()) throw error(404, "Chưa có bản sao lưu cấu hình Java panel");
            name = (String) list.get(0).get("name");
        }
        if (!name.matches(BACKUP)) throw error(400, "Chỉ chấp nhận tên bản sao lưu của Java panel");
        Path path = backupDir().resolve(name);
        rejectLinks(path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw error(404, "Không có bản sao lưu này");
        try (var in = Files.newInputStream(path)) {
            byte[] bytes = in.readNBytes(MAX_CONFIG + 1);
            if (bytes.length > MAX_CONFIG) throw error(400, "Bản sao lưu quá lớn");
            return parse(bytes);
        }
    }

    /** POSIX 0700/0600, or an owner-only Windows ACL; fail rather than silently expose secrets. */
    private static void makePrivate(Path path, boolean directory) throws IOException {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posix != null) {
            posix.setPermissions(PosixFilePermissions.fromString(directory ? "rwx------" : "rw-------"));
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (acl == null) throw error(500, "Ổ đĩa không hỗ trợ quyền truy cập riêng tư");
        AclEntry.Builder entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                .setPrincipal(acl.getOwner()).setPermissions(EnumSet.allOf(AclEntryPermission.class));
        if (directory) entry.setFlags(AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.FILE_INHERIT);
        acl.setAcl(List.of(entry.build()));
    }
}
