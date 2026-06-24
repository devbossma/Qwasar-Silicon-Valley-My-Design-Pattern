package dev.saberlabs.auth;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * Represents an authenticated user in the coffee shop chat application.
 *
 * @param id           unique identifier from the database
 * @param username     unique login name
 * @param passwordHash SHA-256 hashed password — never stored as plain text
 * @param role         the user's role — determines access and view
 * @param createdAt    when the account was created
 */
public record User(
        long id,
        @NotNull String username,
        @NotNull String passwordHash,
        @NotNull Role role,
        @NotNull LocalDateTime createdAt
) {
    /**
     * Compact constructor — validates required fields.
     */
    public User {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
    }

    /**
     * Returns true if this user has the given role.
     *
     * @param requiredRole the role to check against
     * @return true if the user's role matches
     */
    public boolean hasRole(@NotNull Role requiredRole) {
        return this.role == requiredRole;
    }

    /**
     * Returns true if this user is a manager.
     */
    public boolean isManager() {
        return role == Role.MANAGER;
    }

    /**
     * Returns true if this user is a barista.
     */
    public boolean isBarista() {
        return role == Role.BARISTA;
    }

    /**
     * Returns true if this user is a customer.
     */
    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    /**
     * Returns a display-friendly string without exposing the password hash.
     */
    @Override
    public @NotNull String toString() {
        return String.format("User[id=%d, username=%s, role=%s, createdAt=%s]",
                id, username, role, createdAt);
    }

    public long getId() {
        return id;
    }
}