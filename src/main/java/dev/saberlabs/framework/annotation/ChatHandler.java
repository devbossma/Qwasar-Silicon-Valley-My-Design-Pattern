package dev.saberlabs.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Concrete request annotation)
 *
 * Marks the method on a {@code BusinessObject} that handles {@link RequestType#CHAT} requests.
 */
@RequestMappingMeta(RequestType.CHAT)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatHandler {
}
