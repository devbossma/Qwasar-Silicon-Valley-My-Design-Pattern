package dev.saberlabs.framework.reflection;

import dev.saberlabs.framework.BusinessObject;
import dev.saberlabs.framework.annotation.RequestMappingMeta;
import dev.saberlabs.framework.annotation.RequestType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
 * The {@code Class -> RequestType -> Method} mapping is resolved once per business object
 * <em>class</em> (not per instance, and not per call) and cached, since annotations are static
 * metadata that can never change for a given class at runtime. Every request after the first one
 * for a given class is a plain map lookup instead of a fresh {@code getMethods()} scan — real
 * chat traffic runs this dispatcher for every customer keystroke, so re-scanning on every single
 * call would be wasted work. This is the same lookup-cache shape real dispatch frameworks use
 * (e.g. Spring MVC's handler mapping cache).
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
     * One resolved handler-method map per business object class, built the first time that class
     * is dispatched to and reused for every request after that. Shared across all
     * {@code InteractionHandler} instances (there's no per-instance state to key it by, and the
     * result only ever depends on the class), and safe under concurrent access since it's built
     * from immutable reflection metadata.
     */
    private static final Map<Class<?>, Map<RequestType, Method>> HANDLER_CACHE = new ConcurrentHashMap<>();

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

        Method method = HANDLER_CACHE
                .computeIfAbsent(businessObject.getClass(), InteractionHandler::resolveHandlers)
                .get(type);

        if (method != null) {
            ReflectionUtil.invokeMethod(businessObject, method.getName(), request);
            return;
        }

        // No method was found for the request type -> fall back to the default handler.
        businessObject.processRequest(request);
    }

    /**
     * Scans every public method on {@code businessObjectClass} once and builds the complete
     * {@link RequestType} -> {@link Method} map for it, so {@link #handleInteraction} never has
     * to walk {@code getMethods()} again for that class. When two methods claim the same
     * {@link RequestType}, the first one {@code getMethods()} returns wins, keeping this the same
     * "first match" behavior the uncached, per-call scan used to have.
     */
    private static Map<RequestType, Method> resolveHandlers(Class<?> businessObjectClass) {
        Map<RequestType, Method> handlers = new EnumMap<>(RequestType.class);
        for (Method method : businessObjectClass.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                RequestMappingMeta meta = annotation.annotationType().getAnnotation(RequestMappingMeta.class);
                if (meta != null) {
                    handlers.putIfAbsent(meta.value(), method);
                }
            }
        }
        return handlers;
    }
}
