package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.repositories.ChatRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryChatRepository implements ChatRepository {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @Override
    public synchronized @NotNull ChatMessage save(@NotNull ChatMessage message) {
        ChatMessage saved = new ChatMessage(
                idCounter.incrementAndGet(),
                message.type(),
                message.sessionId(),
                message.senderId(),
                message.senderName(),
                message.content(),
                message.timestamp(),
                message.orderId()
        );
        messages.add(saved);
        return saved;
    }

    @Override
    public synchronized @NotNull List<ChatMessage> findBySessionId(long sessionId) {
        return messages.stream()
                .filter(m -> m.sessionId() == sessionId)
                .toList();
    }

    @Override
    public synchronized @NotNull List<ChatMessage> findAll() {
        return List.copyOf(messages);
    }

    @Override
    public synchronized @NotNull List<ChatMessage> findByOrderId(@NotNull String orderId) {
        return messages.stream()
                .filter(m -> orderId.equals(m.orderId()))
                .toList();
    }

    @Override
    public synchronized int countBySessionId(long sessionId) {
        return (int) messages.stream().filter(m -> m.sessionId() == sessionId).count();
    }

    public synchronized void clear() {
        messages.clear();
    }
}