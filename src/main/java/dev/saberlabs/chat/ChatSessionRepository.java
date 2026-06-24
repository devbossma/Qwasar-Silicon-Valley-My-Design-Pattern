package dev.saberlabs.chat;

import dev.saberlabs.db.DatabaseUtil;
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

/**
 * JDBC repository for ChatSession persistence.
 * *
 * Persists the historical record of sessions (who talked to whom, and
 * the final status) to SQLite. Live matching state — who's currently
 * READY or WAITING right now — is handled separately by {@link BaristaQueue}
 * in memory; this repository only deals with durable storage.
 * *
 * IMPORTANT: the shared {@link Connection} is NEVER placed in a
 * try-with-resources block — only PreparedStatement/ResultSet are
 * auto-closed, consistent with {@link dev.saberlabs.auth.UserRepository}
 * and {@link ChatRepository}.
 */
public class ChatSessionRepository {

    // ================================================================
    // Save / Update
    // ================================================================

    /**
     * Persists a new session (id == 0) or updates an existing one (id != 0).
     *
     * @param session the session to save or update
     * @return the persisted session — with a database-generated ID if it was new
     */
    public @NotNull ChatSession save(@NotNull ChatSession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        return session.id() == 0 ? insert(session) : update(session);
    }

    private @NotNull ChatSession insert(@NotNull ChatSession session) {
        String sql = """
                INSERT INTO chat_sessions (customer_id, barista_id, status, created_at)
                VALUES (?, ?, ?, ?)
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, session.customerId());
            setNullableLong(stmt, 2, session.baristaId());
            stmt.setString(3, session.status().name());
            stmt.setString(4, session.createdAt().toString());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new ChatSession(id, session.customerId(), session.baristaId(),
                            session.status(), session.createdAt());
                }
            }
            throw new RuntimeException("Failed to retrieve generated ID for session");

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to insert session for customer: " + session.customerId(), e);
        }
    }

    private @NotNull ChatSession update(@NotNull ChatSession session) {
        String sql = """
                UPDATE chat_sessions
                SET barista_id = ?, status = ?
                WHERE id = ?
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableLong(stmt, 1, session.baristaId());
            stmt.setString(2, session.status().name());
            stmt.setLong(3, session.id());
            stmt.executeUpdate();
            return session;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update session ID: " + session.id(), e);
        }
    }

    // ================================================================
    // Find
    // ================================================================

    /**
     * Finds a session by its ID.
     *
     * @param id the session ID
     * @return an Optional containing the session, or empty if not found
     */
    public @NotNull Optional<ChatSession> findById(long id) {
        String sql = "SELECT * FROM chat_sessions WHERE id = ?";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find session by ID: " + id, e);
        }
    }

    /**
     * Returns the most recent non-INACTIVE session for a customer, if any.
     * Used by CustomerView's "Start Chat" to resume an existing conversation
     * instead of always creating a new one.
     *
     * @param customerId the customer's User ID
     * @return the most recent WAITING or ACTIVE session, or empty if none
     */
    public @NotNull Optional<ChatSession> findActiveSessionByCustomer(long customerId) {
        String sql = """
                SELECT * FROM chat_sessions
                WHERE customer_id = ? AND status != 'INACTIVE'
                ORDER BY created_at DESC
                LIMIT 1
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find active session for customer: " + customerId, e);
        }
    }

    /**
     * Returns every session in the database, newest first.
     * Used by BaristaView to show the full dashboard: all sessions,
     * their status, and who is responsible for each one.
     *
     * @return unmodifiable list of all sessions
     */
    public @NotNull List<ChatSession> findAll() {
        String sql = "SELECT * FROM chat_sessions ORDER BY created_at DESC";
        List<ChatSession> sessions = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                sessions.add(mapRow(rs));
            }
            return List.copyOf(sessions);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all sessions", e);
        }
    }

    /**
     * Returns all sessions currently assigned to a specific barista
     * that are still ACTIVE — used when a barista logs back in and
     * needs to resume conversations they were already handling.
     *
     * @param baristaId the barista's User ID
     * @return unmodifiable list of that barista's active sessions
     */
    public @NotNull List<ChatSession> findActiveSessionsByBarista(long baristaId) {
        String sql = """
                SELECT * FROM chat_sessions
                WHERE barista_id = ? AND status = 'ACTIVE'
                ORDER BY created_at
                """;
        List<ChatSession> sessions = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, baristaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapRow(rs));
                }
            }
            return List.copyOf(sessions);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find active sessions for barista: " + baristaId, e);
        }
    }

    // ================================================================
    // Private helpers
    // ================================================================

    /**
     * Sets a nullable Long parameter on a PreparedStatement.
     * SQLite needs explicit NULL typing via setNull rather than passing
     * a boxed null directly to setLong.
     */
    private void setNullableLong(@NotNull PreparedStatement stmt, int index,
                                 @Nullable Long value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.BIGINT);
        } else {
            stmt.setLong(index, value);
        }
    }

    /**
     * Maps a ResultSet row to a ChatSession record.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return the mapped ChatSession
     * @throws SQLException if a column cannot be read
     */
    private @NotNull ChatSession mapRow(@NotNull ResultSet rs) throws SQLException {
        long baristaIdRaw = rs.getLong("barista_id");
        Long baristaId = rs.wasNull() ? null : baristaIdRaw;

        return new ChatSession(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                baristaId,
                SessionStatus.valueOf(rs.getString("status")),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}