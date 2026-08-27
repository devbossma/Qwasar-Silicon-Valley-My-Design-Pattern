package dev.saberlabs.framework.business;

import org.jetbrains.annotations.NotNull;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Typed request)
 *
 * Free-text feedback with no dedicated handler annotation, by design — this app has no
 * "feedback" subsystem to route it into, so it deliberately always falls through to
 * {@link BusinessObject#processRequest(RequestType)}, the same way the example framework's
 * "feedback" request type demonstrates the fallback path.
 */
public record FeedbackDetails(@NotNull String text) implements RequestType {
}
