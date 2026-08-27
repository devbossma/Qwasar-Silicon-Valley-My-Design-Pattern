package dev.saberlabs.framework.business.reflection;

import dev.saberlabs.framework.business.BusinessObject;
import dev.saberlabs.framework.business.RequestType;
import dev.saberlabs.framework.business.annotation.RequestMappingMeta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * REFLECTION FRAMEWORK — BUSINESS (Dispatcher)
 *
 * Reflects over a {@link BusinessObject}'s methods to find the one whose annotation is
 * meta-annotated with {@link RequestMappingMeta} and whose {@link RequestMappingMeta#value()}
 * is a class the given {@link RequestType} is an instance of. No method claims it —
 * dispatch falls back to {@link BusinessObject#processRequest(RequestType)}.
 * <p>
 * This is still a caller-invoked dispatcher, not inversion of control — see
 * {@code framework/doc.md} for why the live application's own order/chat traffic calls
 * {@code OrderService}/{@code ChatService} directly instead of routing through here: reflection
 * doesn't reduce the caller's work the way a real framework would, so it's reserved for cases
 * that actually need it (a handler discovered by its request type, not known in advance).
 *
 * @see BusinessObject
 * @see RequestMappingMeta
 * @see RequestType
 */
public class InteractionHandler {

    /**
     * Dispatches a request to the appropriate method on the given business object, falling
     * back to {@link BusinessObject#processRequest(RequestType)} when no annotated method
     * claims the request's runtime type.
     *
     * @param businessObject the business object to handle the request
     * @param request        the typed request to dispatch
     * @param <R>            the handler method's return type, or {@code null} when the
     *                       fallback runs (it returns {@code void})
     * @return whatever the matched handler method returns, or {@code null} on fallback
     */
    public <R> R handleInteraction(BusinessObject businessObject, RequestType request) {
        Objects.requireNonNull(businessObject, "Business object cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

        for (Method method : businessObject.getClass().getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                RequestMappingMeta meta = annotation.annotationType().getAnnotation(RequestMappingMeta.class);
                if (meta != null && meta.value().isInstance(request)) {
                    return ReflectionUtil.invokeMethod(businessObject, method, request);
                }
            }
        }

        businessObject.processRequest(request);
        return null;
    }
}
