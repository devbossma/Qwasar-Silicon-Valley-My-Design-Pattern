package dev.saberlabs.auth;

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
 * JDBC repository for User persistence.
 *
 * Handles all database operations for the users table:
 * save, find by username, find by ID, find all, delete.
 *
 * IMPORTANT: The shared {@link Connection} from {@link DatabaseUtil#getConnection()}
 * is NEVER placed in a try-with-resources block — DatabaseUtil owns its lifecycle
 * exclusively. Only PreparedStatement/ResultSet are auto-closed here.
 *
 * Write operations use PreparedStatement to prevent SQL injection.
 */
public class UserRepository {

    // ================================================================
    // Save
    // ================================================================

    /**
     * Persists a new user to the database.
     * The user's ID is ignored — the database generates it via AUTOINCREMENT.
     *
     * @param user the user to save
     * @return the saved user with the database-generated ID
     * @throws RuntimeException if the username already exists or a DB error occurs
     */
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

    // ================================================================
    // Find
    // ================================================================

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to search for
     * @return an Optional containing the user, or empty if not found
     */
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

    /**
     * Finds a user by their unique ID.
     *
     * @param id the user ID to search for
     * @return an Optional containing the user, or empty if not found
     */
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

    /**
     * Returns all users in the database.
     * Used by the Manager view to list all accounts.
     *
     * @return unmodifiable list of all users
     */
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

    /**
     * Returns all users with a specific role.
     *
     * @param role the role to filter by
     * @return unmodifiable list of users with the given role
     */
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

    // ================================================================
    // Delete
    // ================================================================

    /**
     * Deletes a user by their ID.
     * Used by the Manager view to remove accounts.
     *
     * @param id the ID of the user to delete
     * @return true if a user was deleted, false if no user with that ID existed
     */
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

    /**
     * Returns true if a username is already taken.
     *
     * @param username the username to check
     * @return true if the username exists in the database
     */
    public boolean existsByUsername(@NotNull String username) {
        Objects.requireNonNull(username, "Username cannot be null");
        return findByUsername(username).isPresent();
    }

    // ================================================================
    // Private helpers
    // ================================================================

    /**
     * Maps a ResultSet row to a User record.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return the mapped User
     * @throws SQLException if a column cannot be read
     */
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