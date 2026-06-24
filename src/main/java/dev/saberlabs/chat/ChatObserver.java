package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;

/**
 * Pattern: OBSERVER (Observer interface)
 *
 * Implemented by any component that wants to be notified when a new
 * chat message is sent — console views now, JavaFX views in Part 2.
 */
public interface ChatObserver {

    /**
     * Called whenever a new chat message is sent through the ChatService.
     *
     * @param message the newly sent message
     */
    void onMessageReceived(@NotNull ChatMessage message);
}