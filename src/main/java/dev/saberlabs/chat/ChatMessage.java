package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Represents a chat message belonging to exactly one {@link ChatSession}.
 *
 * @param id         unique identifier from the database (0 if not yet persisted)
 * @param sessionId  the session this message belongs to
 * @param senderId   the ID of the user who sent the message
 * @param senderName the display name of the sender
 * @param content    the message text
 * @param timestamp  when the message was sent
 * @param orderId    the associated order ID, or null if not an order message
 */
public record ChatMessage(
        long id,
        long sessionId,
        long senderId,
        @NotNull String senderName,
        @NotNull String content,
        @NotNull LocalDateTime timestamp,
        @Nullable String orderId
) {
    public ChatMessage {
        if (senderName.isBlank()) {
            throw new IllegalArgumentException("Sender name cannot be blank");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be blank");
        }
    }

    /**
     * Factory method — creates a new unsaved message with current timestamp.
     *
     * @param sessionId  the session this message belongs to
     * @param senderId   the sender's user ID
     * @param senderName the sender's display name
     * @param content    the message text
     * @param orderId    the associated order ID, or null
     * @return a new ChatMessage with id=0 and current timestamp
     */
    public static @NotNull ChatMessage of(
            long sessionId,
            long senderId,
            @NotNull String senderName,
            @NotNull String content,
            @Nullable String orderId) {
        return new ChatMessage(0, sessionId, senderId, senderName, content,
                LocalDateTime.now(), orderId);
    }

    public boolean isOrderMessage() {
        return orderId != null;
    }

    @Override
    public String toString() {
        String emoji = orderId != null ? "☕" : "💬";
        return String.format("[%02d:%02d] %s %s: %s",
                timestamp.getHour(), timestamp.getMinute(), emoji, senderName, content);
    }
}