package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.chat.SessionStatus;
import dev.saberlabs.chat.repositories.ChatSessionRepository;
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
 * SQLite-backed implementation of {@link ChatSessionRepository}.
 *
 * IMPORTANT: the shared Connection is never placed in try-with-resources —
 * only PreparedStatement/ResultSet are auto-closed.
 */
public class SqliteChatSessionRepository implements ChatSessionRepository {

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public @NotNull List<ChatSession> findActiveSessionsByBarista(long baristaId) {
        String sql = """
                SELECT * FROM chat_sessions
                WHERE barista_id = ? AND status = 'ACTIVE'
                ORDER BY created_at ASC
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

    private void setNullableLong(@NotNull PreparedStatement stmt, int index,
                                 @Nullable Long value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.BIGINT);
        } else {
            stmt.setLong(index, value);
        }
    }

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