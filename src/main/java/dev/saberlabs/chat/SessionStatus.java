package dev.saberlabs.chat;

/**
 * Status of a chat session in the barista-pool queue.
 * *
 * WAITING  — customer is queued, no barista assigned yet
 * ACTIVE   — a barista is assigned and the conversation is ongoing
 * INACTIVE — the session ended (order fulfilled or customer left)
 */
public enum SessionStatus {
    WAITING,
    ACTIVE,
    INACTIVE
}