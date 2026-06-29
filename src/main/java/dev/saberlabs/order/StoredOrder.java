package dev.saberlabs.order;

import dev.saberlabs.chat.repositories.ChatOrderRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persisted snapshot of an Order, decoupled from the live domain object
 * so the database can be the authority on order ID continuity across
 * application restarts - see the {@link ChatOrderRepository#nextOrderId()} method.
 *
 * @param id          the order ID, e.g. "ORD-1" (also the primary key)
 * @param customerId  the User ID of the customer who placed the order
 * @param baristaId   the User ID of the barista who sent it to the kitchen, or null if not yet
 * @param sessionId   the chat session this order originated from, or null if placed outside chat
 * @param baseCoffee  the base coffee type, e.g. "Espresso"
 * @param extras      the decorator extras applied, e.g. ["milk", "sugar"]
 * @param total       the final price after Strategy pricing
 * @param status      PLACED, READY, FULFILLED, or CANCELLED
 * @param createdAt   when the order was placed
 */
public record StoredOrder(
        @NotNull String id,
        long customerId,
        @Nullable Long baristaId,
        @Nullable Long sessionId,
        @NotNull String baseCoffee,
        @NotNull List<String> extras,
        double total,
        @NotNull String status,
        @NotNull LocalDateTime createdAt
) {
}