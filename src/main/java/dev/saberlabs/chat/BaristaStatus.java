package dev.saberlabs.chat;

/**
 * Pattern: status enum for a human Barista's availability in the chat queue.
 * *
 * - READY   — barista is available to take a new chat session
 * - BUSY    — barista is currently handling a chat session
 * - OFFLINE — barista is not available for chat sessions
 */
public enum BaristaStatus {
    READY,
    BUSY,
    OFFLINE
}