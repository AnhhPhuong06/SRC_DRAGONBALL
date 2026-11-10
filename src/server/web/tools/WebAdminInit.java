package server.web.tools;

import java.io.Console;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/** Creates panel credentials without connecting to a database or starting the game. */
public final class WebAdminInit {
    private WebAdminInit() {}

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || Arrays.asList(args).contains("--help")) {
            System.out.println("Usage: init_web_admin.bat --init [--username NAME] [--port PORT] [--host HOST]");
            System.out.println("Creates data/config/web_admin.properties only if it does not exist.");
            System.out.println("An interactive console can enter the password privately; otherwise a random password is saved.");
            System.out.println("No database writes, schema changes, game startup or network requests.");
            return;
        }
        String username = "admin", host = "127.0.0.1";
        int port = 8888;
        boolean init = false;
        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if ("--init".equals(option)) { init = true; continue; }
            if (index + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + option);
            String value = args[++index];
            switch (option) {
                case "--username" -> username = value;
                case "--host" -> host = value;
                case "--port" -> port = Integer.parseInt(value);
                default -> throw new IllegalArgumentException("Unknown option: " + option);
            }
        }
        if (!init || username.isBlank() || host.isBlank() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Use --help for valid arguments");
        }
        Path config = Path.of("data/config/web_admin.properties");
        if (Files.exists(config)) throw new IOException("Config already exists; edit it directly. Nothing was overwritten.");
        Console console = System.console();
        char[] entered = console == null ? null : console.readPassword("Panel password (blank = random): ");
        String password;
        try {
            if (entered == null || new String(entered).isBlank()) {
                byte[] bytes = new byte[24];
                new SecureRandom().nextBytes(bytes);
                password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            } else {
                password = new String(entered);
                if (!password.equals(password.trim())) throw new IllegalArgumentException("Password must not start/end with whitespace");
                if (password.length() < 12) throw new IllegalArgumentException("Use a password of at least 12 characters");
            }
        } finally {
            if (entered != null) Arrays.fill(entered, '\0');
        }
        Properties values = new Properties();
        values.setProperty("host", host);
        values.setProperty("port", String.valueOf(port));
        values.setProperty("username", username);
        values.setProperty("password", password);
        Files.createDirectories(config.getParent());
        try (OutputStream output = Files.newOutputStream(config, StandardOpenOption.CREATE_NEW)) {
            values.store(output, "Panel admin credentials - private, never publish this file");
        }
        System.out.println("Created " + config + ". Password is stored in this private file, not printed.");
    }
}
