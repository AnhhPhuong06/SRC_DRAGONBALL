package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/** Conditional single-row operations: balance validation and mutation are atomic in MySQL. */
public final class AccountWallet {
    private AccountWallet() {}

    public static boolean debit(Connection connection, String currency, int accountId, int amount,
            boolean activate) throws SQLException {
        if (!Set.of("sotien", "cash").contains(currency) || accountId <= 0 || amount < 0
                || (amount == 0 && !activate) || (activate && !currency.equals("sotien"))) return false;
        String sql = "UPDATE account SET " + currency + "=" + currency + "-?"
                + (activate ? ", active=1" : "") + " WHERE id=? AND " + currency + ">=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, amount);
            statement.setInt(2, accountId);
            statement.setInt(3, amount);
            return statement.executeUpdate() == 1;
        }
    }

    public static boolean purchaseVip(Connection connection, int accountId, int tier, long now) throws SQLException {
        if (accountId <= 0 || tier < 1 || tier > 3) return false;
        int cost = switch (tier) { case 1 -> 50_000; case 2 -> 200_000; default -> 500_000; };
        String suffix = tier == 1 ? "" : Integer.toString(tier - 1);
        String flag = "hasReceivedVIP" + suffix;
        String sql = "UPDATE account SET sotien=sotien-?, vip" + suffix + "=1, " + flag
                + "=1, lastTimeReceivedVIP" + suffix + "=? WHERE id=? AND sotien>=? AND COALESCE(" + flag + ",0)=0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, cost);
            statement.setLong(2, now);
            statement.setInt(3, accountId);
            statement.setInt(4, cost);
            return statement.executeUpdate() == 1;
        }
    }
}
