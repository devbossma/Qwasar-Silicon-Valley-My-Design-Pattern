package dev.saberlabs.framework.business.annotation;

import dev.saberlabs.framework.business.ChatDetails;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Concrete request annotation)
 *
 * Marks the method on a {@code BusinessObject} that handles {@link ChatDetails} requests.
 */
@RequestMappingMeta(ChatDetails.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatHandler {
}
