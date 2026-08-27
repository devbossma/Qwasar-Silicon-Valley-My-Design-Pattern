package dev.saberlabs.framework.business;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Request marker)
 *
 * Marker interface for a typed request payload. Unlike
 * {@code dev.saberlabs.framework.example.annotation.RequestType} (a fixed enum matched against
 * a request-type string), routing here is done by the request object's own runtime class — see
 * {@code annotation.RequestMappingMeta}. Adding a new request type is just a new class
 * implementing this interface plus a new concrete annotation; the dispatcher never changes.
 */
public interface RequestType { }
