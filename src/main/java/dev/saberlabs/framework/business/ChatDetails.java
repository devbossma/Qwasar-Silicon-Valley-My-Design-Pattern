package dev.saberlabs.framework.business;

import org.jetbrains.annotations.NotNull;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Typed request)
 *
 * Carries the real session/sender identity a chat message needs — the same fields
 * {@link dev.saberlabs.chat.ChatService#sendMessage} itself requires — instead of a bare
 * {@code String} with no way to say who sent it or in which session.
 */
public record ChatDetails(long sessionId, long senderId, @NotNull String senderName, @NotNull String content)
        implements RequestType {
}
