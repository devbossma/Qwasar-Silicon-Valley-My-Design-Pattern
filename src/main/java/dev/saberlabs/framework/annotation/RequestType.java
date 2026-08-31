package dev.saberlabs.framework.annotation;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Request type)
 *
 * The set of request types the framework knows how to route. Carried as the
 * {@link RequestMappingMeta#value()} of a concrete handler annotation
 * ({@link OrderHandler}, {@link ChatHandler}) so routing is a real,
 * compiler-checked comparison instead of a string-matching convention.
 * Adding a new request type is a new constant here plus a new concrete
 * annotation — no changes needed to the dispatch logic itself.
 */
public enum RequestType {
    ORDER,
    CHAT
}
