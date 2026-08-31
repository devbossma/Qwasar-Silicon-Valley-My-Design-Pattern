package dev.saberlabs.framework;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Business contract)
 *
 * Common interface implemented by any business type (coffee shop, bookstore, etc.) that wants
 * to be dispatched into by {@code dev.saberlabs.framework.reflection.InteractionHandler}. A
 * single {@code String} request, routed by a {@code requestType} string — see
 * {@code framework/doc.md} for why the request payload stays a plain {@code String} rather than
 * a typed object: a {@code BusinessObject} represents one whole business, so its methods are
 * this app's real, public seam, not an internal detail free to invent its own vocabulary.
 *
 * {@link #processRequest(String)} is the default/fallback handler: it's invoked whenever no
 * method annotated for the resolved request type is found, so it's a real part of the contract
 * every implementor exercises, not a stub kept only to satisfy the interface.
 */
public interface BusinessObject {
    void processRequest(String request);
}
