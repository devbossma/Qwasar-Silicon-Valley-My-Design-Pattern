package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.chat.SessionStatus;
import dev.saberlabs.chat.repositories.ChatSessionRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryChatSessionRepository implements ChatSessionRepository {

    private final List<ChatSession> sessions = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @Override
    public synchronized @NotNull ChatSession save(@NotNull ChatSession session) {
        if (session.id() == 0) {
            ChatSession inserted = new ChatSession(
                    idCounter.incrementAndGet(), session.customerId(),
                    session.baristaId(), session.status(), session.createdAt());
            sessions.add(inserted);
            return inserted;
        }
        sessions.removeIf(s -> s.id() == session.id());
        sessions.add(session);
        return session;
    }

    @Override
    public synchronized @NotNull Optional<ChatSession> findById(long id) {
        return sessions.stream().filter(s -> s.id() == id).findFirst();
    }

    @Override
    public synchronized @NotNull Optional<ChatSession> findActiveSessionByCustomer(long customerId) {
        return sessions.stream()
                .filter(s -> s.customerId() == customerId && s.status() != SessionStatus.INACTIVE)
                .max((a, b) -> a.createdAt().compareTo(b.createdAt()));
    }

    @Override
    public synchronized @NotNull List<ChatSession> findAll() {
        return List.copyOf(sessions);
    }

    @Override
    public synchronized @NotNull List<ChatSession> findActiveSessionsByBarista(long baristaId) {
        return sessions.stream()
                .filter(s -> s.status() == SessionStatus.ACTIVE
                        && baristaId == (s.baristaId() == null ? -1 : s.baristaId()))
                .toList();
    }

    public synchronized void clear() {
        sessions.clear();
    }
}