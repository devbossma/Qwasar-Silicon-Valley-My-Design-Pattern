package dev.saberlabs.framework.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pattern 11: REFLECTION FRAMEWORK — EXAMPLE (Meta-annotation)
 *
 * Marks another annotation as a request-handler annotation, carrying the
 * {@link RequestType} it routes. {@code InteractionHandler} only considers
 * method annotations that are themselves annotated with this one, and
 * matches by comparing {@link #value()} against the resolved request type —
 * not by the annotation's name.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMappingMeta {
    RequestType value();
}
