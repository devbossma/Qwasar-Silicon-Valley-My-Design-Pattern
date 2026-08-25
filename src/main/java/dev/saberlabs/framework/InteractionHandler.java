package dev.saberlabs.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * REFLECTION FRAMEWORK (Dispatcher)
 *
 * Uses reflection to find, on a {@link BusinessObject}, the method whose annotation
 * is both meta-annotated with {@link RequestMappingMeta} and named for the given
 * request type by convention: {@code requestType} "order" maps to an annotation
 * simple-named {@code OrderHandler}, "chat" to {@code ChatHandler}, and so on
 * (capitalize the request type and append "Handler"). The meta-annotation is what
 * makes an annotation eligible at all; the naming convention is just the routing key
 * used to pick the right one among possibly several eligible annotations.
 *
 * @see BusinessObject
 * @see RequestMappingMeta
 * @see OrderHandler
 * @see ChatHandler
 */
public class InteractionHandler {

    /**
     * Dispatches a request to the appropriate method on the given business object.
     *
     * @param businessObject The business object to handle the request.
     * @param requestType    The type of request (e.g., "order", "chat", ).
     * @param request        The request data to be processed.
     */
    public void handleInteraction(BusinessObject businessObject, String requestType, String request) {
        String expectedAnnotationName = expectedAnnotationSimpleName(requestType);
        Method[] methods = businessObject.getClass().getMethods();

        for (Method method : methods) {
            for (Annotation annotation : method.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.isAnnotationPresent(RequestMappingMeta.class)
                        && annotationType.getSimpleName().equals(expectedAnnotationName)) {
                    ReflectionUtil.invokeMethod(businessObject, method.getName(), request);
                    return;
                }
            }
        }

        System.out.println("No handler found for request type: " + requestType);
    }

    /**
     * Generates the expected simple name of the handler annotation for a given request type.
     *
     * @param requestType The type of request (e.g., "order", "chat").
     * @return The expected simple name of the handler annotation (e.g., "OrderHandler", "ChatHandler").
     */
    private static String expectedAnnotationSimpleName(String requestType) {
        String capitalized = requestType.isEmpty()
                ? requestType
                : Character.toUpperCase(requestType.charAt(0)) + requestType.substring(1);
        return capitalized + "Handler";
    }
}
