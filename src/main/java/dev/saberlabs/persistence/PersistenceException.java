package dev.saberlabs.persistence;

/**
 * Runtime wrapper for persistence I/O and mapping failures.
 */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceException(String message) {
        super(message);
    }
}
