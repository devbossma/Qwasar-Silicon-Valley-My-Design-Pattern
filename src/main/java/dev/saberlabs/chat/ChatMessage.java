package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public record ChatMessage(
        long id,
        @NotNull MessageType type,
        long sessionId,
        long senderId,
        @NotNull String senderName,
        @NotNull String content,
        @NotNull LocalDateTime timestamp,
        @Nullable String orderId
) {
    public ChatMessage {
        if (senderName.isBlank()) throw new IllegalArgumentException("Sender name cannot be blank");
        if (content.isBlank())    throw new IllegalArgumentException("Content cannot be blank");
    }

    public static @NotNull ChatMessage of(
        @NotNull MessageType type,
        long sessionId,
        long senderId,
        @NotNull String senderName,
        @NotNull String content,
        @Nullable String orderId
    ) {
        return new ChatMessage(
                0,type, sessionId, senderId, senderName, content,  LocalDateTime.now(), orderId
        );
    }

    public boolean isOrderMessage() { return orderId != null; }

    @Override
    public @NotNull String toString() {
        String emoji = switch (type) {
            case CHAT_MESSAGE   -> senderId == 0 ? "💬" : "👤";
            case SYSTEM_MESSAGE -> "⚙️";
        };
        return String.format("[%02d:%02d] %s %s: %s",
                timestamp.getHour(), timestamp.getMinute(),
                emoji, senderName, content);
    }
}