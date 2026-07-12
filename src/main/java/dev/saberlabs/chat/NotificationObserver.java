package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;

/**
 * Observer interface for real-time notification delivery.
 * Each view registers itself with its current user's ID and
 * filters to only display notifications addressed to that user.
 */
public interface NotificationObserver {
    void onNotificationReceived(@NotNull ChatNotification notification);
}