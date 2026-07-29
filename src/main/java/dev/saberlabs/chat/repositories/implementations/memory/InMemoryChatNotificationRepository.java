package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.chat.ChatNotification;
import dev.saberlabs.chat.repositories.ChatNotificationRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryChatNotificationRepository implements ChatNotificationRepository {

    private final List<ChatNotification> notifications = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @Override
    public synchronized @NotNull ChatNotification save(@NotNull ChatNotification notification) {
        ChatNotification saved = new ChatNotification(
                idCounter.incrementAndGet(), notification.userId(), notification.content(),
                notification.type(), notification.referenceId(), notification.isRead(),
                notification.timestamp());
        notifications.add(saved);
        return saved;
    }

    @Override
    public synchronized @NotNull List<ChatNotification> findByUser(long userId) {
        return notifications.stream().filter(n -> n.userId() == userId).toList();
    }

    @Override
    public synchronized @NotNull List<ChatNotification> findUnreadByUser(long userId) {
        return notifications.stream()
                .filter(n -> n.userId() == userId && !n.isRead())
                .toList();
    }

    @Override
    public synchronized void markAllReadForUser(long userId) {
        for (int i = 0; i < notifications.size(); i++) {
            ChatNotification n = notifications.get(i);
            if (n.userId() == userId && !n.isRead()) {
                notifications.set(i, new ChatNotification(
                        n.id(), n.userId(), n.content(), n.type(),
                        n.referenceId(), true, n.timestamp()));
            }
        }
    }

    public synchronized void clear() {
        notifications.clear();
    }
}