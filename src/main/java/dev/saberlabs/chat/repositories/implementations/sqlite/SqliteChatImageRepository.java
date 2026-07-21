package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ImageUpload;
import dev.saberlabs.chat.repositories.ChatImageRepository;
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
 * SQLite-backed implementation of {@link ChatImageRepository}.
 * Stores image data as BLOB — no filesystem dependency.
 *
 * IMPORTANT: the shared Connection is never placed in try-with-resources —
 * only PreparedStatement/ResultSet are auto-closed.
 */
public class SqliteChatImageRepository implements ChatImageRepository {

    @Override
    public @NotNull ImageUpload save(@NotNull ImageUpload image) {
        Objects.requireNonNull(image, "Image cannot be null");
        String sql = """
                INSERT INTO image_uploads (session_id, sender_id, filename, data, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, image.sessionId());
            stmt.setLong(2, image.senderId());
            stmt.setString(3, image.filename());
            stmt.setBytes(4, image.data());
            stmt.setString(5, image.timestamp().toString());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new ImageUpload(
                            id,
                            image.sessionId(),
                            image.senderId(),
                            image.filename(),
                            image.data(),
                            image.timestamp()
                    );
                }
            }
            throw new RuntimeException("Failed to retrieve generated ID for image upload");

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save image: " + image.filename(), e);
        }
    }

    @Override
    public @NotNull List<ImageUpload> findBySessionId(long sessionId) {
        String sql = """
                SELECT * FROM image_uploads
                WHERE session_id = ?
                ORDER BY timestamp ASC
                """;
        Connection conn = DatabaseUtil.getConnection();
        List<ImageUpload> images = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    images.add(mapRow(rs));
                }
            }
            return List.copyOf(images);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve images for session: " + sessionId, e);
        }
    }

    @Override
    public @NotNull List<ImageUpload> findBySenderId(long senderId) {
        String sql = """
                SELECT * FROM image_uploads
                WHERE sender_id = ?
                ORDER BY timestamp DESC
                """;
        Connection conn = DatabaseUtil.getConnection();
        List<ImageUpload> images = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, senderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    images.add(mapRow(rs));
                }
            }
            return List.copyOf(images);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve images for sender: " + senderId, e);
        }
    }

    @Override
    public @NotNull List<ImageUpload> findAll() {
        String sql = "SELECT * FROM image_uploads ORDER BY timestamp DESC";
        Connection conn = DatabaseUtil.getConnection();
        List<ImageUpload> images = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                images.add(mapRow(rs));
            }
            return List.copyOf(images);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all images", e);
        }
    }

    private @NotNull ImageUpload mapRow(@NotNull ResultSet rs) throws SQLException {
        return new ImageUpload(
                rs.getLong("id"),
                rs.getLong("session_id"),
                rs.getLong("sender_id"),
                rs.getString("filename"),
                rs.getBytes("data"),
                LocalDateTime.parse(rs.getString("timestamp"))
        );
    }
}