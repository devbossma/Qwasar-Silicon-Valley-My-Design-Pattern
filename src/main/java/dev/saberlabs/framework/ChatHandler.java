package dev.saberlabs.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Concrete request annotation)
 *
 * Marks the method on a {@link BusinessObject} that handles "chat" requests.
 */
@RequestMappingMeta
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatHandler {
}
