package dev.saberlabs.chat.repositories;

import dev.saberlabs.chat.ChatSession;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for persisted ChatSessions.
 */
public interface ChatSessionRepository {

    @NotNull ChatSession save(@NotNull ChatSession session);

    @NotNull Optional<ChatSession> findById(long id);

    @NotNull Optional<ChatSession> findActiveSessionByCustomer(long customerId);

    @NotNull List<ChatSession> findAll();

    @NotNull List<ChatSession> findActiveSessionsByBarista(long baristaId);
}