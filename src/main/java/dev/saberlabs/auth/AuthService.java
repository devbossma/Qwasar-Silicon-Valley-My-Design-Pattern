package dev.saberlabs.auth;


import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import dev.saberlabs.auth.repositories.UserRepository;

/**
 * Handles user authentication and registration for the coffee shop chat application.
 * *
 * Responsibilities:
 * - Register new users (CUSTOMER only — BARISTA created by MANAGER)
 * - Login existing users with username/password verification
 * - Hash passwords using SHA-256 — never stored as plain text
 * - Seed the initial MANAGER account if no users exist
 * *
 * Password security:
 * SHA-256 is used for simplicity in this educational context.
 * In production, use Bcrypt or Argon2 with a salt.
 */
public class AuthService {

    private static final String MANAGER_DEFAULT_USERNAME = "manager";
    private static final String MANAGER_DEFAULT_PASSWORD = "manager123";

    @NotNull private final UserRepository userRepository;

    public AuthService(@NotNull UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(
                userRepository, "UserRepository cannot be null");
    }

    // ================================================================
    // Initialization
    // ================================================================

    /**
     * Seeds the default manager account if no users exist in the database.
     * Called once at application startup via DatabaseUtil.initialize().
     * *
     * Default credentials:
     *   username: manager
     *   password: manager123
     */
    public void seedManagerIfAbsent() {
        if (userRepository.findAll().isEmpty()) {
            User manager = new User(
                    0,
                    MANAGER_DEFAULT_USERNAME,
                    hashPassword(MANAGER_DEFAULT_PASSWORD),
                    Role.MANAGER,
                    LocalDateTime.now()
            );
            userRepository.save(manager);
            System.out.printf("[AuthService] Username: %s | Password: %s%n",
                    MANAGER_DEFAULT_USERNAME, MANAGER_DEFAULT_PASSWORD);
        }
    }

    // ================================================================
    // Registration
    // ================================================================

    /**
     * Registers a new CUSTOMER account.
     * Only customers can self-register — baristas are created by the manager.
     *
     * @param username the desired username
     * @param password the plain-text password (will be hashed before storage)
     * @return the newly created and persisted User
     * @throws IllegalArgumentException if username is blank or already taken
     * @throws IllegalArgumentException if password is too short (< 6 chars)
     */
    public @NotNull User register(@NotNull String username,
                                  @NotNull String password) {
        return register(username, password, Role.CUSTOMER);
    }

    /**
     * Registers a new account with a specific role.
     * Only the MANAGER can call this with BARISTA or MANAGER roles.
     *
     * @param username the desired username
     * @param password the plain-text password
     * @param role     the role to assign
     * @return the newly created and persisted User
     * @throws IllegalArgumentException if username is blank or already taken
     * @throws IllegalArgumentException if password is too short
     */
    public @NotNull User register(@NotNull String username,
                                  @NotNull String password,
                                  @NotNull Role role) {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");

        // Validate username
        if(username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }

        if(username.equals(MANAGER_DEFAULT_USERNAME)) {
            throw new IllegalArgumentException("Invalid username");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException(
                    "Username must be at least 3 characters");
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Username can only contain letters, numbers, and underscores");
        }

        // Validate password
        if (password.length() < 6) {
            throw new IllegalArgumentException(
                    "Password must be at least 6 characters");
        }

        // Check username availability
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username already taken: " + username);
        }

        User user = new User(
                0,
                username,
                hashPassword(password),
                role,
                LocalDateTime.now()
        );

        User saved = userRepository.save(user);
        System.out.printf("[AuthService] New %s registered: %s%n",
                role, username);
        return saved;
    }

    // ================================================================
    // Login
    // ================================================================

    /**
     * Authenticates a user with their username and password.
     *
     * @param username the username to authenticate
     * @param password the plain-text password to verify
     * @return the authenticated User
     * @throws AuthException if the username does not exist
     * @throws AuthException if the password is incorrect
     */
    public @NotNull User login(@NotNull String username,
                               @NotNull String password) {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(
                        "No account found for username: " + username));

        if (!verifyPassword(password, user.passwordHash())) {
            throw new AuthException("Incorrect password for: " + username);
        }

        System.out.printf("[AuthService] %s logged in as %s.%n",
                username, user.role());
        return user;
    }

    // ================================================================
    // Manager operations
    // ================================================================

    /**
     * Creates a new BARISTA account. Only callable by the MANAGER.
     *
     * @param manager  the manager performing the action
     * @param username the barista's username
     * @param password the barista's plain-text password
     * @return the newly created barista User
     * @throws SecurityException if the caller is not a MANAGER
     */
    public @NotNull User createBarista(@NotNull User manager,
                                       @NotNull String username,
                                       @NotNull String password) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        if (!manager.isManager()) {
            throw new SecurityException(
                    "Only a MANAGER can create barista accounts");
        }
        return register(username, password, Role.BARISTA);
    }

    /**
     * Deletes a user account. Only callable by the MANAGER.
     * A manager cannot delete their own account.
     *
     * @param manager the manager performing the action
     * @param userId  the ID of the user to delete
     * @throws SecurityException        if the caller is not a MANAGER
     * @throws IllegalArgumentException if the manager tries to delete themselves
     */
    public void deleteUser(@NotNull User manager, long userId) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        if (!manager.isManager()) {
            throw new SecurityException(
                    "Only a MANAGER can delete user accounts");
        }
        if (manager.id() == userId) {
            throw new IllegalArgumentException(
                    "A manager cannot delete their own account");
        }
        boolean deleted = userRepository.deleteById(userId);
        if (!deleted) {
            throw new IllegalArgumentException(
                    "No user found with ID: " + userId);
        }
        System.out.printf("[AuthService] User ID %d deleted by %s.%n",
                userId, manager.username());
    }

    // ================================================================
    // Password hashing
    // ================================================================

    /**
     * Hashes a plain-text password using SHA-256.
     * The result is Base64-encoded for safe storage as a TEXT column.
     *
     * @param plainPassword the plain-text password to hash
     * @return the Base64-encoded SHA-256 hash
     */
    public @NotNull String hashPassword(@NotNull String plainPassword) {
        Objects.requireNonNull(plainPassword, "Password cannot be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored hash.
     *
     * @param plainPassword the plain-text password to verify
     * @param storedHash    the stored SHA-256 hash to compare against
     * @return true if the password matches the hash
     */
    public boolean verifyPassword(@NotNull String plainPassword,
                                  @NotNull String storedHash) {
        Objects.requireNonNull(plainPassword, "Password cannot be null");
        Objects.requireNonNull(storedHash, "Stored hash cannot be null");
        return hashPassword(plainPassword).equals(storedHash);
    }

    /**
     * Changes a user's password after verifying their current one.
     *
     * @param user            the user changing their password
     * @param currentPassword their current plain-text password, for verification
     * @param newPassword     the new plain-text password (will be hashed)
     * @throws AuthException           if the current password is incorrect
     * @throws IllegalArgumentException if the new password is too short
     */
    public void changePassword(@NotNull User user, @NotNull String currentPassword,
                               @NotNull String newPassword) {
        Objects.requireNonNull(user, "User cannot be null");
        if (!verifyPassword(currentPassword, user.passwordHash())) {
            throw new AuthException("Current password is incorrect");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        User updated = new User(user.id(), user.username(),
                hashPassword(newPassword), user.role(), user.createdAt());
        userRepository.updatePassword(updated);
    }
}