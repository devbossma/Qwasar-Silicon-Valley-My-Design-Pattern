package dev.saberlabs.persistence.records;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Result returned after restoring persisted coffee shop state.
 *
 * @param customers restored customers
 * @param orders    restored orders
 */
public record RestoredCoffeeShopState(
        @NotNull List<Customer> customers,
        @NotNull List<Order> orders
) {
}
