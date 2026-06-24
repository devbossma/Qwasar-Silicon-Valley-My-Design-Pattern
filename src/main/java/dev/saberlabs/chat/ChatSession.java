package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Represents a chat conversation between one customer and, once assigned,
 * one barista. Sessions are matched via a FIFO queue: the longest-waiting
 * customer is paired with the next barista to become READY.
 *
 * @param id          unique identifier (0 if not yet persisted)
 * @param customerId  the User ID of the customer who started this session
 * @param baristaId   the User ID of the assigned barista, or null while WAITING
 * @param status      WAITING, ACTIVE, or INACTIVE
 * @param createdAt   when the session was created
 */
public record ChatSession(
        long id,
        long customerId,
        @Nullable Long baristaId,
        @NotNull SessionStatus status,
        @NotNull LocalDateTime createdAt
) {
    /**
     * Factory method — creates a new WAITING session for a customer.
     */
    public static @NotNull ChatSession newWaitingSession(long customerId) {
        return new ChatSession(0, customerId, null, SessionStatus.WAITING,
                LocalDateTime.now());
    }

    /**
     * Returns a copy of this session assigned to the given barista, now ACTIVE.
     */
    public @NotNull ChatSession assignTo(long baristaId) {
        return new ChatSession(id, customerId, baristaId, SessionStatus.ACTIVE, createdAt);
    }

    /**
     * Returns a copy of this session marked INACTIVE.
     */
    public @NotNull ChatSession deactivate() {
        return new ChatSession(id, customerId, baristaId, SessionStatus.INACTIVE, createdAt);
    }

    public boolean isWaiting()  { return status == SessionStatus.WAITING; }
    public boolean isActive()   { return status == SessionStatus.ACTIVE; }
    public boolean isInactive() { return status == SessionStatus.INACTIVE; }
}