import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Load the configured JDBC driver through the packaged JAR's real classpath. No DB connection. */
public final class JdbcDriverLoadTest {
    public static void main(String[] args) throws Exception {
        Path config = Path.of(args.length == 0 ? "data/config/alyra.properties.example" : args[0]);
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(config)) {
            properties.load(input);
        }
        String name = properties.getProperty("database.driver");
        Class<?> driver = Class.forName(name);
        if (!java.sql.Driver.class.isAssignableFrom(driver)) {
            throw new AssertionError("Configured class is not a JDBC driver: " + name);
        }
        System.out.println("PASS: packaged JDBC driver loads: " + name);
    }
}
