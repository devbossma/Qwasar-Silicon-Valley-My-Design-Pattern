package dev.saberlabs.chat.repositories;

import dev.saberlabs.chat.ChatMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Repository abstraction for persisted ChatMessages.
 */
public interface ChatRepository {

    @NotNull ChatMessage save(@NotNull ChatMessage message);

    @NotNull List<ChatMessage> findBySessionId(long sessionId);

    @NotNull List<ChatMessage> findAll();

    @NotNull List<ChatMessage> findByOrderId(@NotNull String orderId);

    int countBySessionId(long sessionId);
}