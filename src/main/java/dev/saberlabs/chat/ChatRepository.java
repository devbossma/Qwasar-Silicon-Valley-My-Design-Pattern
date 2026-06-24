package dev.saberlabs.chat;

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
import java.util.Optional;

/**
 * JDBC repository for ChatMessage persistence, scoped by session.
 * *
 * IMPORTANT: the shared Connection is never placed in try-with-resources —
 * only PreparedStatement/ResultSet are auto-closed, consistent with the
 * rest of the repositories in this project.
 */
public class ChatRepository {

    /**
     * Persists a chat message to the database.
     *
     * @param message the message to save
     * @return the saved message with the database-generated ID
     */
    public @NotNull ChatMessage save(@NotNull ChatMessage message) {
        Objects.requireNonNull(message, "Message cannot be null");
        String sql = """
                INSERT INTO messages (session_id, sender_id, sender_name, content, timestamp, order_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, message.sessionId());
            stmt.setLong(2, message.senderId());
            stmt.setString(3, message.senderName());
            stmt.setString(4, message.content());
            stmt.setString(5, message.timestamp().toString());
            stmt.setString(6, message.orderId());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new ChatMessage(id, message.sessionId(), message.senderId(),
                            message.senderName(), message.content(),
                            message.timestamp(), message.orderId());
                }
            }
            throw new RuntimeException("Failed to retrieve generated ID for message");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message from: "
                    + message.senderName(), e);
        }
    }

    /**
     * Returns all messages in a session, ordered oldest to newest.
     * This is the primary lookup — "load the conversation for this session."
     *
     * @param sessionId the session to load messages for
     * @return unmodifiable list of messages in that session
     */
    public @NotNull List<ChatMessage> findBySessionId(long sessionId) {
        String sql = """
                SELECT * FROM messages
                WHERE session_id = ?
                ORDER BY timestamp
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

    /**
     * Returns every message ever sent, across all sessions, newest first.
     * Used by ManagerView to inspect the entire chat history.
     *
     * @return unmodifiable list of all messages
     */
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

    /**
     * Returns all messages associated with a specific order, across any session.
     *
     * @param orderId the order ID to filter by
     * @return unmodifiable list of messages for that order
     */
    public @NotNull List<ChatMessage> findByOrderId(@NotNull String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        String sql = """
                SELECT * FROM messages
                WHERE order_id = ?
                ORDER BY timestamp
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

    /**
     * Returns the total number of messages in a session.
     *
     * @param sessionId the session to count messages for
     * @return message count for that session
     */
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

    /**
     * Maps a ResultSet row to a ChatMessage record.
     */
    private @NotNull ChatMessage mapRow(@NotNull ResultSet rs) throws SQLException {
        return new ChatMessage(
                rs.getLong("id"),
                rs.getLong("session_id"),
                rs.getLong("sender_id"),
                rs.getString("sender_name"),
                rs.getString("content"),
                LocalDateTime.parse(rs.getString("timestamp")),
                rs.getString("order_id")
        );
    }

    public @NotNull List<ChatMessage> findBySenderId(long senderId) {
        String sql = "SELECT * FROM messages WHERE sender_id = ?";
        List<ChatMessage> messages = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, senderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
            return List.copyOf(messages);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve messages for sender: " + senderId, e);
        }
    }

    /**
     * Finds the single message that announced a given order (the one
     * created via {@link ChatMessage#orderId()} when the order was placed).
     * Used to recover which session an order belongs to, since Order itself
     * has no direct reference back to chat.
     *
     * @param orderId the order ID to look up
     * @return the first message tagged with that order ID, if any
     */
    public @NotNull Optional<ChatMessage> findFirstByOrderId(@NotNull String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        List<ChatMessage> matches = findByOrderId(orderId);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }
}