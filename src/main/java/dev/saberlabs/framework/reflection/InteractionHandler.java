package dev.saberlabs.framework.reflection;

import dev.saberlabs.framework.BusinessObject;
import dev.saberlabs.framework.annotation.RequestMappingMeta;
import dev.saberlabs.framework.annotation.RequestType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Dispatcher)
 * *
 * Uses reflection to find, on a {@link BusinessObject}, the method whose annotation is
 * meta-annotated with {@link RequestMappingMeta} and whose {@link RequestMappingMeta#value()}
 * matches the resolved {@link RequestType} for the given request type string. If the request
 * type string doesn't resolve to a known {@link RequestType}, or no method claims the resolved
 * type, dispatch falls back to {@link BusinessObject#processRequest(String)} — the default
 * handler every business object must provide.
 * <p>
 * Note this is a caller-invoked dispatcher, not an inversion-of-control framework: application
 * code decides when to call {@link #handleInteraction}, rather than this class owning an entry
 * point that calls into application code on its own. See {@code framework/doc.md}.
 *
 * @see BusinessObject
 * @see RequestMappingMeta
 * @see RequestType
 */
public class InteractionHandler {

    private static final String ORDER_MARKER = "/order";

    /**
     * Classifies the raw request text — the only judgment call this framework makes on the
     * caller's behalf — then dispatches exactly like {@link #handleInteraction(BusinessObject, String, String)}.
     * <p>
     * The classification itself is deliberately minimal and generic, and it's purely syntactic —
     * no business vocabulary involved:
     * <ul>
     *   <li>the text's first whitespace-delimited token equals {@code "/order"}, exactly ->
     *       {@code "order"}</li>
     *   <li>the first token starts with {@code "/"} but isn't {@code "/order"} -> the raw token
     *       itself, e.g. {@code "/menu"}. No {@link RequestType} will ever resolve to that, so
     *       this always falls through to {@link BusinessObject#processRequest(String)} below —
     *       a business object gets a real chance to respond to a command it doesn't recognize,
     *       instead of the text silently being treated as a chat message</li>
     *   <li>anything else -> {@code "chat"}</li>
     * </ul>
     * Any business that takes orders through a chat-style text channel needs this same split
     * before it can act, so it belongs here rather than being reimplemented by every
     * {@link BusinessObject}. Everything past that — what an order actually looks like, what a
     * reply should say, which commands (if any) it recognizes — is real business logic, and
     * stays in the business object's own handler methods, not in this class.
     * <p>
     * An explicit marker (rather than a bare {@code "order"} prefix) is deliberate: free-form
     * chat text can legitimately start with a real word that reads like a command — "order latte
     * from this place was amazing" is a compliment, not an order for a latte with four unknown
     * extras — and no keyword heuristic can tell those apart. A marker no ordinary sentence would
     * ever start with (the same convention chat apps use for slash-commands) sidesteps the
     * ambiguity entirely instead of trying to guess intent from natural language.
     *
     * @param businessObject the business object to handle the request
     * @param request        the raw request text, not yet known to be an order or a chat message
     */
    public void handleInteraction(BusinessObject businessObject, String request) {
        Objects.requireNonNull(request, "Request cannot be null");
        String trimmed = request.trim();
        String firstToken = trimmed.split("\\s+", 2)[0];

        String requestType;
        if (firstToken.equalsIgnoreCase(ORDER_MARKER)) {
            requestType = "order";
        } else if (firstToken.startsWith("/")) {
            requestType = firstToken;
        } else {
            requestType = "chat";
        }

        handleInteraction(businessObject, requestType, request);
    }

    /**
     * Dispatches a request to the appropriate method on the given business object, falling
     * back to {@link BusinessObject#processRequest(String)} when no annotated method claims
     * the request type.
     *
     * @param businessObject The business object to handle the request.
     * @param requestType    The type of request (e.g., "order", "chat").
     * @param request        The request data to be processed.
     */
    public void handleInteraction(BusinessObject businessObject, String requestType, String request) {
        RequestType type;
        try {
            type = RequestType.valueOf(requestType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            businessObject.processRequest(request);
            return;
        }

        for (Method method : businessObject.getClass().getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                RequestMappingMeta meta = annotation.annotationType().getAnnotation(RequestMappingMeta.class);
                if (meta != null && meta.value() == type) {
                    ReflectionUtil.invokeMethod(businessObject, method.getName(), request);
                    return;
                }
            }
        }

        // If no method was found for the request type, fall back to the default handler.
        businessObject.processRequest(request);
    }
}
