package dev.saberlabs.framework.business;

import dev.saberlabs.models.Order;
import org.jetbrains.annotations.NotNull;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Typed request)
 *
 * Carries an already-built {@link Order} — real customer, real coffee, a real order ID —
 * rather than a free-text description a handler would have to parse. Whoever constructs this
 * (a test, a future real caller) supplies real domain data; the handler that receives it
 * ({@code CoffeeShopFacade.handleOrder}) never has to invent a placeholder customer the way the
 * example framework's String-only contract forced it to.
 */
public record OrderDetails(@NotNull Order order) implements RequestType {
}
