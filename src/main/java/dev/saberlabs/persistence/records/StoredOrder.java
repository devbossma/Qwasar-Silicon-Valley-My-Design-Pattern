package dev.saberlabs.persistence.records;

import dev.saberlabs.models.OrderStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persisted order snapshot.
 *
 * @param orderId    unique order ID
 * @param customerId owner customer ID
 * @param coffee     persisted coffee snapshot
 * @param finalPrice price after strategy discount
 * @param status     persisted status name, or null if unset
 */
public record  StoredOrder(
        @NotNull String orderId,
        @NotNull String customerId,
        @NotNull StoredCoffee coffee,
        double finalPrice,
        @Nullable OrderStatus status
) { }
