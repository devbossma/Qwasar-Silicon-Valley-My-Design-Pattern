package dev.saberlabs.framework;

/**
 * REFLECTION FRAMEWORK (Business contract)
 *
 * Common interface implemented by any business type (coffee shop, bookstore, etc.)
 * that wants to be dispatched into by the {@link InteractionHandler}.
 */
public interface BusinessObject {
    void processRequest(String request);
}
