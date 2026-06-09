package dev.saberlabs.persistence.records;

import dev.saberlabs.models.LoyaltyTier;
import org.jetbrains.annotations.NotNull;

/**
 * Persisted customer snapshot.
 *
 * @param id          unique customer ID
 * @param name        customer display name
 * @param totalOrders fulfilled order count
 * @param loyaltyTier tier restored from total orders
 */
public record StoredCustomer(
        @NotNull String id,
        @NotNull String name,
        int totalOrders,
        @NotNull LoyaltyTier loyaltyTier
) {
}
