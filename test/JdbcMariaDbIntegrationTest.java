import database.AlyraManager;
import database.AlyraResultSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/** Real MariaDB + production Hikari/adapter test, limited to an explicit CI fixture. */
public final class JdbcMariaDbIntegrationTest {
    public static void main(String[] args) throws Exception {
        if (!"true".equals(System.getenv("NRO_JDBC_INTEGRATION"))) {
            throw new IllegalStateException("Run only in the isolated CI fixture with NRO_JDBC_INTEGRATION=true");
        }
        Path config = Path.of("data/config/alyra.properties");
        if (Files.exists(config)) throw new IllegalStateException("Refusing to replace existing config");
        Properties values = new Properties();
        values.setProperty("database.driver", "org.mariadb.jdbc.Driver");
        values.setProperty("database.host", "127.0.0.1");
        values.setProperty("database.port", "3307");
        values.setProperty("database.name", "jdbc_test");
        values.setProperty("database.name_data", "jdbc_test");
        values.setProperty("database.user", "jdbc_ci");
        values.setProperty("database.pass", System.getenv("JDBC_CI_PASSWORD"));
        values.setProperty("database.min", "1");
        values.setProperty("database.max", "2");
        values.setProperty("database.lifetime", "1800000");
        Files.createDirectories(config.getParent());
        try (var output = Files.newOutputStream(config)) { values.store(output, "Ephemeral CI only"); }
        String table = "jdbc_fixture_" + Long.toUnsignedString(System.nanoTime());
        try {
            AlyraManager.executeUpdate("CREATE TABLE " + table
                    + " (id INT PRIMARY KEY, text_value VARCHAR(255), long_value BIGINT) CHARACTER SET utf8mb4");
            String text = "Ng\u1ecdc R\u1ed3ng \ud83d\udc09";
            AlyraManager.executeUpdate("INSERT INTO " + table + " VALUES (?, ?, ?)", 1, text, 4000000000L);
            AlyraResultSet rows = AlyraManager.executeQuery("SELECT * FROM " + table + " WHERE id = ?", 1);
            if (!rows.first() || !text.equals(rows.getString("text_value"))
                    || rows.getLong("long_value") != 4000000000L || rows.getInt(table + ".id") != 1) {
                throw new AssertionError("Disconnected adapter lost Unicode, numeric values or qualified columns");
            }
            rows.dispose();
            try (Connection connection = AlyraManager.getConnection_Data();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT * FROM " + table)) {
                if (!result.first() || !result.last() || result.getInt("id") != 1) {
                    throw new AssertionError("Scrollable data connection failed");
                }
            }
            try (Connection connection = AlyraManager.getConnection()) {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE " + table + " SET long_value = 0");
                }
                connection.rollback();
            }
            rows = AlyraManager.executeQuery("SELECT long_value FROM " + table);
            if (!rows.first() || rows.getLong("long_value") != 4000000000L) {
                throw new AssertionError("Transaction rollback failed");
            }
            rows.dispose();
            System.out.println("PASS: real MariaDB connection, both Hikari pools, Unicode, prepared statements, scrollable cursors, adapter and rollback");
        } finally {
            try { AlyraManager.executeUpdate("DROP TABLE IF EXISTS " + table); }
            finally { AlyraManager.close(); AlyraManager.shutdownDataPool(); }
        }
    }
}
