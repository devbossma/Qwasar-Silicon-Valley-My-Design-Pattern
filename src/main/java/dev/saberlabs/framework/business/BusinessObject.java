package dev.saberlabs.framework.business;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Business contract)
 *
 * The real contract this application's own business types implement — typed requests
 * ({@link RequestType} implementors) instead of the example framework's bare {@code String}.
 * {@link #processRequest(RequestType)} is still the default/fallback handler: invoked whenever
 * no method annotated for the request's runtime class is found.
 */
public interface BusinessObject {
    void processRequest(RequestType request);
}
