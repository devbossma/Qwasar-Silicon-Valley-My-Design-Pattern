package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.repositories.ChatOrderRepository;
import dev.saberlabs.db.DatabaseUtil;
import dev.saberlabs.order.StoredOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLite-backed implementation of {@link ChatOrderRepository}.
 *
 * Also owns order ID continuity: {@link #nextOrderId()} queries the
 * highest existing trailing number in the orders table rather than
 * relying on an in-memory counter, so IDs survive application restarts.
 *
 * IMPORTANT: the shared Connection is never placed in try-with-resources —
 * only PreparedStatement/ResultSet are auto-closed.
 */
public class SqliteChatOrderRepository implements ChatOrderRepository {

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    @Override
    public synchronized @NotNull String nextOrderId() {
        String sql = "SELECT id FROM orders";
        int maxId = 0;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                maxId = Math.max(maxId, extractTrailingNumber(rs.getString("id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to determine next order ID", e);
        }
        return "ORD-" + (maxId + 1);
    }

    private int extractTrailingNumber(@NotNull String id) {
        Matcher matcher = TRAILING_NUMBER.matcher(id);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    @Override
    public @NotNull StoredOrder save(@NotNull StoredOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");
        String sql = """
                INSERT INTO orders
                    (id, customer_id, barista_id, session_id, base_coffee, extras, total, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    barista_id = excluded.barista_id,
                    session_id = excluded.session_id,
                    status     = excluded.status
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, order.id());
            stmt.setLong(2, order.customerId());
            setNullableLong(stmt, 3, order.baristaId());
            setNullableLong(stmt, 4, order.sessionId());
            stmt.setString(5, order.baseCoffee());
            stmt.setString(6, String.join(",", order.extras()));
            stmt.setDouble(7, order.total());
            stmt.setString(8, order.status());
            stmt.setString(9, order.createdAt().toString());
            stmt.executeUpdate();
            return order;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save order: " + order.id(), e);
        }
    }

    @Override
    public void updateAssignmentAndStatus(@NotNull String orderId,
                                          @Nullable Long baristaId,
                                          @Nullable Long sessionId,
                                          @NotNull String status) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        String sql = """
                UPDATE orders
                SET barista_id = ?, session_id = ?, status = ?
                WHERE id = ?
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableLong(stmt, 1, baristaId);
            setNullableLong(stmt, 2, sessionId);
            stmt.setString(3, status);
            stmt.setString(4, orderId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update order: " + orderId, e);
        }
    }

    @Override
    public void updateStatus(@NotNull String orderId, @NotNull String status) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, orderId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update order status: " + orderId, e);
        }
    }

    @Override
    public @NotNull Optional<StoredOrder> findById(@NotNull String id) {
        Objects.requireNonNull(id, "Order ID cannot be null");
        String sql = "SELECT * FROM orders WHERE id = ?";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order by ID: " + id, e);
        }
    }

    @Override
    public @NotNull List<StoredOrder> findByCustomerAndStatus(long customerId,
                                                              @NotNull String status) {
        Objects.requireNonNull(status, "Status cannot be null");
        String sql = """
                SELECT * FROM orders
                WHERE customer_id = ? AND status = ?
                ORDER BY created_at ASC
                """;
        List<StoredOrder> orders = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, customerId);
            stmt.setString(2, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
            return List.copyOf(orders);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find orders for customer: " + customerId, e);
        }
    }

    @Override
    public @NotNull List<StoredOrder> findByCustomer(long customerId) {
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY created_at DESC";
        List<StoredOrder> orders = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
            return List.copyOf(orders);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find orders for customer: " + customerId, e);
        }
    }

    @Override
    public @NotNull List<StoredOrder> findBySessionId(long sessionId) {
        String sql = "SELECT * FROM orders WHERE session_id = ? ORDER BY created_at ASC";
        List<StoredOrder> orders = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
            return List.copyOf(orders);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find orders for session: " + sessionId, e);
        }
    }

    @Override
    public @NotNull List<StoredOrder> findAll() {
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";
        List<StoredOrder> orders = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                orders.add(mapRow(rs));
            }
            return List.copyOf(orders);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all orders", e);
        }
    }

    private void setNullableLong(@NotNull PreparedStatement stmt, int index,
                                 @Nullable Long value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.BIGINT);
        } else {
            stmt.setLong(index, value);
        }
    }

    private @NotNull StoredOrder mapRow(@NotNull ResultSet rs) throws SQLException {
        long baristaIdRaw = rs.getLong("barista_id");
        Long baristaId = rs.wasNull() ? null : baristaIdRaw;

        long sessionIdRaw = rs.getLong("session_id");
        Long sessionId = rs.wasNull() ? null : sessionIdRaw;

        String extrasRaw = rs.getString("extras");
        List<String> extras = (extrasRaw == null || extrasRaw.isBlank())
                ? List.of()
                : List.of(extrasRaw.split(","));

        return new StoredOrder(
                rs.getString("id"),
                rs.getLong("customer_id"),
                baristaId,
                sessionId,
                rs.getString("base_coffee"),
                extras,
                rs.getDouble("total"),
                rs.getString("status"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}