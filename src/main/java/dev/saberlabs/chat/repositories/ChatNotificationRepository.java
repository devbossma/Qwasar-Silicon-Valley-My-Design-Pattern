package dev.saberlabs.chat.repositories;

import dev.saberlabs.chat.ChatNotification;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatNotificationRepository {
    @NotNull
    ChatNotification save(@NotNull ChatNotification notification);
    @NotNull List<ChatNotification> findByUser(long userId);
    @NotNull List<ChatNotification> findUnreadByUser(long userId);
    void markAllReadForUser(long userId);
}
