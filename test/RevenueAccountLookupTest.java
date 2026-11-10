package server.web.panel;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

/** Synthetic JDBC: exercises account lookup and wallet SQL without touching a live account. */
public final class RevenueAccountLookupTest {
    static int checks;

    public static void main(String[] args) throws Exception {
        Fixture f = new Fixture("upper");
        var row = AccountPlayerStore.one(f.connection(), "SELECT id FROM account WHERE username=?", "ben");
        check(AccountPlayerStore.value(row, "id") == 71, "Uppercase schema ID must not silently become account 0");
        for (String casing : List.of("lower", "upper", "mixed")) {
            f = new Fixture(casing);
            Connection c = f.connection();
            int id = RevenueDataApi.moneyAccountId(c, "ben");
            check(id == 71, "Typed lookup resolves exact username in " + casing + " schema");
            check("ben".equals(AccountPlayerStore.one(c, "SELECT username FROM account WHERE id=?", id).get("username")),
                    "Edit lock resolves username using ID, independent of schema casing");
            var locked = AccountPlayerStore.one(c,
                    "SELECT id,username,ban,cash,danap,sotien,coin,thoi_vang FROM account WHERE id=? FOR UPDATE", id);
            RevenueDataApi.adjustMoney(c, locked, 500, true);
            check(f.balance == 1500, "Add updates sotien on correct account");
            locked = AccountPlayerStore.one(c,
                    "SELECT id,username,ban,cash,danap,sotien,coin,thoi_vang FROM account WHERE id=? FOR UPDATE", id);
            RevenueDataApi.adjustMoney(c, locked, 1500, false);
            check(f.balance == 0 && f.writes == 2, "Subtract uses current locked sotien balance");
        }
        f = new Fixture("mixed"); f.exists = false;
        reject(f, 404, "Không tìm thấy tên đăng nhập", "Missing username has an explicit account lookup error");
        f = new Fixture("mixed"); f.duplicates = true;
        reject(f, 409, "bị trùng", "Duplicate usernames never credit an arbitrary account");
        for (Long bad : Arrays.asList(null, -1L, 2147483648L)) {
            f = new Fixture("upper"); f.id = bad;
            reject(f, 500, "ID tài khoản không hợp lệ", "Invalid/missing IDs cannot become zero or wrap");
        }
        // Bound username is never interpreted as SQL, nor matched against a character name.
        f = new Fixture("lower");
        try { RevenueDataApi.moneyAccountId(f.connection(), "ben' OR 1=1 --"); throw new AssertionError("Injection matched"); }
        catch (PanelSupport.PanelException e) { check(e.status == 404 && f.writes == 0, "Parameterized exact username only"); }
        System.out.println("PASS: " + checks + " account ID lookup, schema casing, wallet add/subtract and missing/duplicate account checks");
    }

    static void reject(Fixture f, int status, String message, String label) throws Exception {
        try { RevenueDataApi.moneyAccountId(f.connection(), "ben"); throw new AssertionError(label); }
        catch (PanelSupport.PanelException e) {
            check(e.status == status && e.getMessage().contains(message), label);
        }
        check(f.writes == 0 && f.balance == 1000, "Rejected lookup never writes money");
    }
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }

    static final class Fixture {
        final String casing;
        Long id = 71L;
        long balance = 1000;
        boolean exists = true, duplicates;
        int writes;
        Fixture(String casing) { this.casing = casing; }
        Connection connection() {
            return proxy(Connection.class, (p,m,a) -> {
                if (m.getName().equals("prepareStatement")) {
                    String sql = (String) a[0];
                    Map<Integer,Object> params = new HashMap<>();
                    return proxy(PreparedStatement.class, (s,sm,sa) -> {
                        switch (sm.getName()) {
                            case "setString", "setObject" -> { params.put((Integer) sa[0], sa[1]); return null; }
                            case "executeQuery" -> {
                                check(sql.startsWith("SELECT ") && sql.contains(" FROM account WHERE "), "Expected account SELECT");
                                boolean byName = sql.contains("username=?");
                                boolean found = exists && (byName ? "ben".equals(params.get(1)) : Objects.equals(((Number)params.get(1)).longValue(), id));
                                LinkedHashMap<String,Object> values = new LinkedHashMap<>();
                                if (sql.startsWith("SELECT id FROM")) values.put("id", id);
                                else if (sql.startsWith("SELECT username FROM")) values.put("username", "ben");
                                else {
                                    values.put("id", id); values.put("username", "ben"); values.put("ban", false);
                                    values.put("cash", 999999); values.put("danap", 1000000); values.put("sotien", balance);
                                    values.put("coin", 0); values.put("thoi_vang", 0);
                                }
                                return result(values, found ? (duplicates ? 2 : 1) : 0);
                            }
                            case "executeUpdate" -> {
                                check(sql.equals("UPDATE account SET sotien=? WHERE id=?"), "Only game sotien wallet changes");
                                check(((Number)params.get(2)).longValue() == 71, "Write uses actual account ID, never zero");
                                writes++; balance = ((Number)params.get(1)).longValue(); return 1;
                            }
                            case "close" -> { return null; }
                            default -> throw new AssertionError("Unexpected statement method: " + sm.getName());
                        }
                    });
                }
                if (m.getName().equals("close")) return null;
                throw new AssertionError("Unexpected connection method: " + m.getName());
            });
        }
        ResultSet result(LinkedHashMap<String,Object> values, int count) {
            var names = new ArrayList<>(values.keySet()); var data = new ArrayList<>(values.values());
            int[] cursor = {-1}; boolean[] wasNull = {false};
            return proxy(ResultSet.class, (r,m,a) -> {
                switch (m.getName()) {
                    case "next" -> { return ++cursor[0] < count; }
                    case "wasNull" -> { return wasNull[0]; }
                    case "getLong", "getObject" -> {
                        Object value = data.get((Integer)a[0]-1); wasNull[0] = value == null;
                        return m.getName().equals("getLong") ? (value == null ? 0L : ((Number)value).longValue()) : value;
                    }
                    case "getMetaData" -> {
                        return proxy(ResultSetMetaData.class, (md,mm,ma) -> {
                            if (mm.getName().equals("getColumnCount")) return names.size();
                            if (mm.getName().equals("getColumnLabel")) {
                                String name = names.get((Integer)ma[0]-1);
                                return casing.equals("upper") ? name.toUpperCase(Locale.ROOT)
                                        : casing.equals("mixed") ? name.substring(0,1).toUpperCase(Locale.ROOT)+name.substring(1) : name;
                            }
                            throw new AssertionError(mm.getName());
                        });
                    }
                    case "close" -> { return null; }
                    default -> throw new AssertionError("Unexpected result method: " + m.getName());
                }
            });
        }
    }
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
