package services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.regex.Pattern;
import server.Manager;

public final class LoginAccessConfig {
    private static final Path CONFIG_PATH = Path.of("data/config/alyra.properties");
    public static final String DENIED_MESSAGE = "Máy chủ hiện chỉ cho phép tài khoản admin đăng nhập. Vui lòng quay lại sau.";

    private LoginAccessConfig() {}

    public static boolean isAdminOnly() {
        // server.debug là cờ chặn đăng nhập cũ; panel quản lý cả hai để tắt mở nhất quán.
        return Manager.ONLY_ADMIN || Manager.DEBUG;
    }

    public static boolean canLogin(boolean isAdmin) {
        return isAdmin || !isAdminOnly();
    }

    public static synchronized void load() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
        }
        Manager.ONLY_ADMIN = Boolean.parseBoolean(properties.getProperty("server.onlyadmin", "false").trim());
        Manager.DEBUG = Integer.parseInt(properties.getProperty("server.debug", "0").trim()) != 0;
    }

    public static synchronized void setAdminOnly(boolean enabled) throws IOException {
        String content = Files.readString(CONFIG_PATH, StandardCharsets.ISO_8859_1);
        content = updateProperty(content, "server.onlyadmin", String.valueOf(enabled));
        content = updateProperty(content, "server.debug", "0");
        Path target = CONFIG_PATH.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), "login-access-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.ISO_8859_1);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        Manager.ONLY_ADMIN = enabled;
        Manager.DEBUG = false;
    }

    private static String updateProperty(String content, String key, String value) {
        Pattern pattern = Pattern.compile("(?m)^[\\t ]*" + Pattern.quote(key) + "[\\t ]*[:=][^\\r\\n]*");
        if (pattern.matcher(content).find()) {
            return pattern.matcher(content).replaceAll(key + "=" + value);
        }
        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        return content + (content.isEmpty() || content.endsWith("\n") || content.endsWith("\r") ? "" : newline)
                + key + "=" + value + newline;
    }
}
