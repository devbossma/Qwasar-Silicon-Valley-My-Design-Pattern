package dev.saberlabs.framework.example.reflection;

import dev.saberlabs.framework.example.BusinessObject;
import dev.saberlabs.framework.example.annotation.RequestMappingMeta;
import dev.saberlabs.framework.example.annotation.RequestType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * REFLECTION FRAMEWORK — EXAMPLE (Dispatcher)
 *
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

    /**
     * Dispatches a request to the appropriate method on the given business object, falling
     * back to {@link BusinessObject#processRequest(String)} when no annotated method claims
     * the request type.
     *
     * @param businessObject The business object to handle the request.
     * @param requestType    The type of request (e.g., "order", "chat", "feedback").
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

        businessObject.processRequest(request);
    }
}
