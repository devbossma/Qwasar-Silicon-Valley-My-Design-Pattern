package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ChatNotification;
import dev.saberlabs.chat.NotificationType;
import dev.saberlabs.chat.repositories.ChatNotificationRepository;
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
 * SQLite-backed implementation of {@link ChatNotificationRepository}.
 *
 * IMPORTANT: the shared Connection is never placed in try-with-resources —
 * only PreparedStatement/ResultSet are auto-closed.
 */
public class SqliteChatNotificationRepository implements ChatNotificationRepository {

    @Override
    public @NotNull ChatNotification save(@NotNull ChatNotification notification) {
        Objects.requireNonNull(notification, "Notification cannot be null");
        String sql = """
                INSERT INTO notifications (user_id, content, type, reference_id, is_read, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, notification.userId());
            stmt.setString(2, notification.content());
            stmt.setString(3, notification.type().name());
            stmt.setString(4, notification.referenceId());
            stmt.setInt(5, notification.isRead() ? 1 : 0);
            stmt.setString(6, notification.timestamp().toString());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new ChatNotification(
                            id,
                            notification.userId(),
                            notification.content(),
                            notification.type(),
                            notification.referenceId(),
                            notification.isRead(),
                            notification.timestamp()
                    );
                }
            }
            throw new RuntimeException("Failed to retrieve generated ID for notification");

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save notification for user: " + notification.userId(), e);
        }
    }

    @Override
    public @NotNull List<ChatNotification> findByUser(long userId) {
        String sql = """
                SELECT * FROM notifications
                WHERE user_id = ?
                ORDER BY timestamp DESC
                """;
        List<ChatNotification> notifications = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
            return List.copyOf(notifications);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve notifications for user: " + userId, e);
        }
    }

    @Override
    public @NotNull List<ChatNotification> findUnreadByUser(long userId) {
        String sql = """
                SELECT * FROM notifications
                WHERE user_id = ? AND is_read = 0
                ORDER BY timestamp ASC
                """;
        List<ChatNotification> notifications = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
            return List.copyOf(notifications);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve unread notifications for user: " + userId, e);
        }
    }

    @Override
    public void markAllReadForUser(long userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            int updated = stmt.executeUpdate();
            System.out.printf("[ChatNotificationRepository] Marked %d notification(s) " +
                    "as read for user %d.%n", updated, userId);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to mark notifications as read for user: " + userId, e);
        }
    }

    private @NotNull ChatNotification mapRow(@NotNull ResultSet rs) throws SQLException {
        return new ChatNotification(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("content"),
                NotificationType.valueOf(rs.getString("type")),
                rs.getString("reference_id"),
                rs.getInt("is_read") == 1,
                LocalDateTime.parse(rs.getString("timestamp"))
        );
    }
}