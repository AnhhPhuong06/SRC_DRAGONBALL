import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import network.io.Message;
import network.session.MySession;
import server.Controller;
import server.Manager;
import server.ui.DashboardPanel;
import server.ui.LoginAccessPanel;
import server.ui.ServerGuiUtils;
import services.LoginAccessConfig;
import sun.misc.Unsafe;

/** Run in an isolated fixture directory, never in the real server workspace. */
public class LoginAccessPanelTest {
    private static final Path CONFIG = Path.of("data/config/alyra.properties");
    private static int checks;

    public static void main(String[] args) throws Exception {
        check(!Files.isDirectory(Path.of("src")), "Use an isolated test working directory");
        Files.createDirectories(CONFIG.getParent());
        String fixture = "# configuration comment\r\nserver.onlyadmin = false\r\nserver.debug: 0\r\n"
                + "database.pass=dummy:a=b\\\\value\r\nserver.name=Test\r\n";
        write(fixture);
        LoginAccessConfig.load();
        check(LoginAccessConfig.canLogin(false) && LoginAccessConfig.canLogin(true), "Open mode accepts both roles");
        LoginAccessConfig.setAdminOnly(true);
        check(!LoginAccessConfig.canLogin(false) && LoginAccessConfig.canLogin(true), "Admin-only role policy");
        check(read().equals(fixture.replace("server.onlyadmin = false", "server.onlyadmin=true")
                .replace("server.debug: 0", "server.debug=0")), "Preserve other configuration bytes and comments");
        Manager.ONLY_ADMIN = false;
        Manager.DEBUG = false;
        LoginAccessConfig.load();
        check(LoginAccessConfig.isAdminOnly(), "Reload remembers saved setting");

        write(fixture.replace("server.debug: 0", "server.debug: 1"));
        LoginAccessConfig.load();
        check(LoginAccessConfig.isAdminOnly(), "Legacy debug restriction appears as enabled");
        LoginAccessConfig.setAdminOnly(false);
        check(LoginAccessConfig.canLogin(false) && !Manager.DEBUG && !Manager.ONLY_ADMIN,
                "Turning off removes both legacy login restrictions");
        LoginAccessConfig.load();
        check(LoginAccessConfig.canLogin(false), "Turning off survives reload");

        write("# no login keys\nother.value=untouched");
        LoginAccessConfig.setAdminOnly(true);
        check(read().contains("other.value=untouched\nserver.onlyadmin=true\nserver.debug=0\n"), "Append missing keys");
        LoginAccessConfig.load();
        check(LoginAccessConfig.isAdminOnly(), "Appended setting loads");

        Path moved = CONFIG.resolveSibling("fixture-backup.properties");
        Files.move(CONFIG, moved);
        try {
            LoginAccessConfig.setAdminOnly(false);
            throw new AssertionError("Expected missing configuration failure");
        } catch (IOException expected) {
            check(LoginAccessConfig.isAdminOnly(), "Failed save does not change runtime access");
        } finally {
            Files.move(moved, CONFIG);
        }

        LoginAccessConfig.setAdminOnly(true);
        FakeSession session = (FakeSession) unsafe().allocateInstance(FakeSession.class);
        session.isAdmin = false;
        Controller.gI().createChar(session, null);
        check(session.denied == 1, "Create-character screen cannot bypass enabled restriction");

        write(fixture);
        List<String> log = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            LoginAccessPanel panel = new LoginAccessPanel(log::add);
            JCheckBox checkbox = findCheckbox(panel);
            check(!checkbox.isSelected(), "Panel initially reflects saved open mode");
            checkbox.doClick(0);
            check(checkbox.isSelected() && !LoginAccessConfig.canLogin(false), "Panel enables restriction immediately");
            check(findStatus(panel).getText().contains("Đang bật"), "Panel shows enabled state");
            LoginAccessPanel reloaded = new LoginAccessPanel(log::add);
            check(findCheckbox(reloaded).isSelected(), "New panel reads persisted state");
            checkbox.doClick(0);
            check(!checkbox.isSelected() && LoginAccessConfig.canLogin(false), "Panel disables restriction immediately");
            try {
                Files.move(CONFIG, moved);
                checkbox.doClick(0);
                check(!checkbox.isSelected() && LoginAccessConfig.canLogin(false), "Failed panel save reverts checkbox");
                check(findStatus(panel).getText().contains("Không lưu"), "Panel reports save failure");
                Files.move(moved, CONFIG);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            check(log.size() == 3, "Panel logs enable, disable and save failure");
        });

        SwingUtilities.invokeAndWait(() -> {
            try {
                // Render the actual Dashboard configuration card without constructing its DB/timer-dependent dashboard.
                ServerGuiUtils.setupTheme();
                DashboardPanel dashboard = (DashboardPanel) unsafe().allocateInstance(DashboardPanel.class);
                Method method = DashboardPanel.class.getDeclaredMethod("createConfigurationPanel");
                method.setAccessible(true);
                JPanel card = (JPanel) method.invoke(dashboard);
                render(card, "login-access-off.png");
                findCheckboxByText(card, "Chỉ admin được vào game").doClick(0);
                render(card, "login-access-on.png");
                check(LoginAccessConfig.isAdminOnly(), "Actual Dashboard checkbox controls policy");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("PASS: " + checks + " policy, persistence, panel and character-entry checks");
    }

    private static void render(JPanel panel, String file) throws IOException {
        panel.setSize(640, 345);
        layout(panel);
        BufferedImage image = new BufferedImage(640, 345, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(ServerGuiUtils.BG);
        graphics.fillRect(0, 0, 640, 345);
        panel.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", Path.of(file).toFile());
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) if (component instanceof Container child) layout(child);
    }

    private static JCheckBox findCheckbox(Container container) {
        return findCheckboxByText(container, "Chỉ admin được vào game");
    }

    private static JCheckBox findCheckboxByText(Container container, String text) {
        for (Component child : container.getComponents()) {
            if (child instanceof JCheckBox checkbox && checkbox.getText().equals(text)) return checkbox;
            if (child instanceof Container nested) {
                JCheckBox match = findCheckboxByText(nested, text);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static JLabel findStatus(JPanel panel) { return (JLabel) panel.getComponent(1); }
    private static void write(String content) throws IOException { Files.writeString(CONFIG, content, StandardCharsets.ISO_8859_1); }
    private static String read() throws IOException { return Files.readString(CONFIG, StandardCharsets.ISO_8859_1); }
    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static class FakeSession extends MySession {
        int denied;
        private FakeSession() { super(null); }
        @Override public boolean hasAllowedClientVersion() { return true; }
        @Override public void sendMessage(Message message) {
            try {
                if (message.command == -26 && new DataInputStream(new ByteArrayInputStream(message.getData()))
                        .readUTF().equals(LoginAccessConfig.DENIED_MESSAGE)) denied++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
