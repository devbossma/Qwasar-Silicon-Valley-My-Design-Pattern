package dev.saberlabs.chat;

import dev.saberlabs.chat.repositories.ChatNotificationRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages user-scoped notifications — events that belong to a specific
 * user regardless of which session or view they're currently in.
 *
 * Responsibilities:
 * - Persists notifications to the database so they survive across sessions
 * - Delivers them in real-time to registered NotificationObservers (views)
 * - Tracks read/unread status so users see missed notifications on next login
 *
 * Separation from ChatService:
 * ChatService owns session-scoped messages (CHAT_MESSAGE, SYSTEM_MESSAGE).
 * ChatNotificationService owns user-scoped events (NOTIFICATION).
 * The two never cross — a notification is never stored in the messages table.
 */
public class ChatNotificationService {

    @NotNull private final ChatNotificationRepository notificationRepository;
    @NotNull private final CopyOnWriteArraySet<NotificationObserver> observers =
            new CopyOnWriteArraySet<>();

    public ChatNotificationService(
            @NotNull ChatNotificationRepository notificationRepository) {
        this.notificationRepository = Objects.requireNonNull(
                notificationRepository, "ChatNotificationRepository cannot be null");
    }

    // ================================================================
    // Send notifications
    // ================================================================

    /**
     * Sends a notification to a specific user — persists it and delivers
     * it in real-time to any registered observer currently viewing as
     * that user.
     */
    public @NotNull ChatNotification notify(long userId,
                                            @NotNull String content,
                                            @NotNull NotificationType type,
                                            @Nullable String referenceId) {
        ChatNotification notification = ChatNotification.of(
                userId, content, type, referenceId);
        ChatNotification saved = notificationRepository.save(notification);
        notifyObservers(saved);
        return saved;
    }

    // ================================================================
    // Convenience methods for each notification type
    // ================================================================

    public void notifyOrderReady(long customerId, @NotNull String orderId,
                                 @NotNull String coffeeDescription) {
        notify(customerId,
                String.format("Your %s is ready for pickup! (Order #%s)",
                        coffeeDescription, orderId),
                NotificationType.ORDER_READY,
                orderId);
    }

    public void notifyOrderFulfilled(long customerId, @NotNull String orderId) {
        notify(customerId,
                "Your order #" + orderId + " has been fulfilled. Enjoy your coffee! ☕",
                NotificationType.ORDER_FULFILLED,
                orderId);
    }

    public void notifySessionMatched(long customerId, long baristaId, long sessionId) {
        notify(customerId,
                "You are now connected with Barista #" + baristaId + "!",
                NotificationType.SESSION_MATCHED,
                String.valueOf(sessionId));
    }

    public void notifyPaymentReceived(long baristaId, @NotNull String orderId,
                                      double amount, @NotNull String customerName) {
        notify(baristaId,
                String.format("✅ Payment of $%.2f received from %s for order #%s",
                        amount, customerName, orderId),
                NotificationType.PAYMENT_RECEIVED,
                orderId);
    }

    public void notifySessionEnded(long baristaId, long sessionId,
                                   @NotNull String customerName) {
        notify(baristaId,
                customerName + " has ended the chat session.",
                NotificationType.SESSION_ENDED,
                String.valueOf(sessionId));
    }

    // ================================================================
    // History / unread
    // ================================================================

    /**
     * Loads all unread notifications for a user — shown on login
     * so missed events from a previous session are never lost.
     */
    public @NotNull List<ChatNotification> getUnread(long userId) {
        return notificationRepository.findUnreadByUser(userId);
    }

    /**
     * Loads full notification history for a user, newest first.
     */
    public @NotNull List<ChatNotification> getHistory(long userId) {
        return notificationRepository.findByUser(userId);
    }

    /**
     * Marks all of a user's notifications as read — called after
     * displaying the unread batch on login.
     */
    public void markAllRead(long userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    // ================================================================
    // Observer management
    // ================================================================

    public void registerObserver(@NotNull NotificationObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        observers.add(observer);
    }

    public void removeObserver(@NotNull NotificationObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        observers.remove(observer);
    }

    private void notifyObservers(@NotNull ChatNotification notification) {
        for (NotificationObserver observer : observers) {
            observer.onNotificationReceived(notification);
        }
    }
}