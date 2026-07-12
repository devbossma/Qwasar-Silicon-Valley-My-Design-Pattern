package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.LocalDateTime;

/**
 * A user-scoped event notification — belongs to a specific user,
 * NOT to a session. Survives across sessions and views.
 */
public record ChatNotification(
        long id,
        long userId,
        @NotNull String content,
        @NotNull NotificationType type,
        @Nullable String referenceId,  // orderId or sessionId
        boolean isRead,
        @NotNull LocalDateTime timestamp
) {
    public static @NotNull ChatNotification of(long userId,
                                               @NotNull String content,
                                               @NotNull NotificationType type,
                                               @Nullable String referenceId) {
        return new ChatNotification(0, userId, content, type, referenceId,
                false, LocalDateTime.now());
    }

    @Override
    public @NotNull String toString() {
        return String.format("[%02d:%02d] 🔔 %s",
                timestamp.getHour(), timestamp.getMinute(), content);
    }
}