package dev.saberlabs.framework.business.annotation;

import dev.saberlabs.framework.business.RequestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Meta-annotation)
 *
 * Marks another annotation as a request-handler annotation, carrying the concrete
 * {@link RequestType} class it routes. {@code InteractionHandler} matches by checking
 * whether the incoming request is an instance of {@link #value()} — a compiler-checked class
 * reference — rather than comparing an enum constant resolved from a string.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMappingMeta {
    Class<? extends RequestType> value();
}
