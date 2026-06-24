package dev.saberlabs.auth;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when authentication fails — wrong username or incorrect password.
 *
 * Kept separate from IllegalArgumentException so callers can distinguish
 * between invalid input (IAE) and failed authentication (AuthException).
 */
public class AuthException extends RuntimeException {

    public AuthException(@NotNull String message) {
        super(message);
    }

    public AuthException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}