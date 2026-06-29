package dev.saberlabs.auth.repositories.implementations.sqlite;

import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.auth.repositories.UserRepository;
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
 * SQLite-backed implementation of {@link UserRepository}.
 *
 * IMPORTANT: the shared Connection is never placed in try-with-resources —
 * only PreparedStatement/ResultSet are auto-closed. DatabaseUtil owns the
 * connection lifecycle exclusively.
 */
public class SqliteUserRepository implements UserRepository {

    @Override
    public @NotNull User save(@NotNull User user) {
        Objects.requireNonNull(user, "User cannot be null");
        String sql = """
                INSERT INTO users (username, password, role, created_at)
                VALUES (?, ?, ?, ?)
                """;
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.username());
            stmt.setString(2, user.passwordHash());
            stmt.setString(3, user.role().name());
            stmt.setString(4, user.createdAt().toString());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new User(id, user.username(), user.passwordHash(),
                            user.role(), user.createdAt());
                }
            }
            throw new RuntimeException("Failed to retrieve generated ID for user: "
                    + user.username());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.username(), e);
        }
    }

    @Override
    public @NotNull Optional<User> findByUsername(@NotNull String username) {
        Objects.requireNonNull(username, "Username cannot be null");
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username: " + username, e);
        }
    }

    @Override
    public @NotNull Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
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
            throw new RuntimeException("Failed to find user by ID: " + id, e);
        }
    }

    @Override
    public @NotNull List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at ASC";
        List<User> users = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return List.copyOf(users);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all users", e);
        }
    }

    @Override
    public @NotNull List<User> findByRole(@NotNull Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY created_at ASC";
        List<User> users = new ArrayList<>();
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
            return List.copyOf(users);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find users by role: " + role, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user with ID: " + id, e);
        }
    }

    @Override
    public boolean existsByUsername(@NotNull String username) {
        Objects.requireNonNull(username, "Username cannot be null");
        return findByUsername(username).isPresent();
    }

    @Override
    public void updatePassword(@NotNull User user) {
        Objects.requireNonNull(user, "User cannot be null");
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        Connection conn = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.passwordHash());
            stmt.setLong(2, user.id());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new RuntimeException("No user found with ID: " + user.id());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password for user ID: " + user.id(), e);
        }
    }

    private @NotNull User mapRow(@NotNull ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                Role.valueOf(rs.getString("role")),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}