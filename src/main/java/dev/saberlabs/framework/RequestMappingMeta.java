package dev.saberlabs.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Meta-annotation)
 *
 * Marks another annotation as a request-handler annotation. {@link InteractionHandler}
 * only considers method annotations that are themselves annotated with this one.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMappingMeta {
}
