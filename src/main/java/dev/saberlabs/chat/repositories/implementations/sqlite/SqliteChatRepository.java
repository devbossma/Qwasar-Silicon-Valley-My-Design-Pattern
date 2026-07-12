package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.MessageType;
import dev.saberlabs.chat.repositories.ChatRepository;
import dev.saberlabs.db.DatabaseUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQLite-backed implementation of {@link ChatRepository}.
 * Reads and writes the {@code type} column on every message row.
 */
public class SqliteChatRepository implements ChatRepository {

    @Override
    public @NotNull ChatMessage save(@NotNull ChatMessage message) {
        Objects.requireNonNull(message, "Message cannot be null");
        String sql = """
                INSERT INTO messages
                    (type, session_id, sender_id, sender_name, content, timestamp, order_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.type().name());
            stmt.setLong(2, message.sessionId());
            stmt.setLong(3, message.senderId());
            stmt.setString(4, message.senderName());
            stmt.setString(5, message.content());
            stmt.setString(6, message.timestamp().toString());
            stmt.setString(7, message.orderId());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new ChatMessage(
                            id,
                            message.type(),
                            message.sessionId(),
                            message.senderId(),
                            message.senderName(),
                            message.content(),
                            message.timestamp(),
                            message.orderId());
                }
            }
            throw new RuntimeException("Failed to retrieve generated ID for message");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message from: "
                    + message.senderName(), e);
        }
    }

    @Override
    public @NotNull List<ChatMessage> findBySessionId(long sessionId) {
        String sql = """
                SELECT * FROM messages
                WHERE session_id = ?
                ORDER BY timestamp ASC
                """;
        List<ChatMessage> messages = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
            return List.copyOf(messages);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve messages for session: " + sessionId, e);
        }
    }

    @Override
    public @NotNull List<ChatMessage> findAll() {
        String sql = "SELECT * FROM messages ORDER BY timestamp DESC";
        List<ChatMessage> messages = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }
            return List.copyOf(messages);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all messages", e);
        }
    }

    @Override
    public @NotNull List<ChatMessage> findByOrderId(@NotNull String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        String sql = """
                SELECT * FROM messages
                WHERE order_id = ?
                ORDER BY timestamp ASC
                """;
        List<ChatMessage> messages = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
            return List.copyOf(messages);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve messages for order: " + orderId, e);
        }
    }

    @Override
    public int countBySessionId(long sessionId) {
        String sql = "SELECT COUNT(*) FROM messages WHERE session_id = ?";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to count messages for session: " + sessionId, e);
        }
    }

    private @NotNull ChatMessage mapRow(@NotNull ResultSet rs) throws SQLException {
        String typeRaw = rs.getString("type");
        MessageType type = (typeRaw == null)
                ? MessageType.CHAT_MESSAGE
                : MessageType.valueOf(typeRaw);

        return new ChatMessage(
                rs.getLong("id"),
                type,
                rs.getLong("session_id"),
                rs.getLong("sender_id"),
                rs.getString("sender_name"),
                rs.getString("content"),
                LocalDateTime.parse(rs.getString("timestamp")),
                rs.getString("order_id")
        );
    }
}