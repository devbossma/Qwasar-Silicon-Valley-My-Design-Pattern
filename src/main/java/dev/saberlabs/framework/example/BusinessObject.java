package dev.saberlabs.framework.example;

/**
 * REFLECTION FRAMEWORK — EXAMPLE (Business contract)
 *
 * This is the literal shape the assignment describes: a single {@code String} request routed
 * by a naive {@code requestType} string. Kept here, unmodified in spirit, as the reference
 * implementation of what was asked for — {@code dev.saberlabs.framework.business} is the
 * typed, real version this application actually uses; see {@code framework/doc.md} for why
 * the two are kept separate.
 *
 * Common interface implemented by any business type (coffee shop, bookstore, etc.)
 * that wants to be dispatched into by {@code dev.saberlabs.framework.example.reflection.InteractionHandler}.
 *
 * {@link #processRequest(String)} is the default/fallback handler: it's invoked whenever no
 * method annotated for the resolved request type is found, so it's a real part of the contract
 * every implementor exercises, not a stub kept only to satisfy the interface.
 */
public interface BusinessObject {
    void processRequest(String request);
}
