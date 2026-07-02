package dev.saberlabs.order;

import dev.saberlabs.chat.repositories.ChatOrderRepository;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.observer.OrderObserver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pattern: OBSERVER (Concrete Observer)
 *
 * Mirrors every Order status transition into the persisted orders table.
 * Registered once at startup alongside Customer observers — whenever
 * the worker Barista thread calls order.setStatus(), this observer
 * fires and writes the new status to SQLite via ChatOrderRepository.
 *
 * This keeps the Barista thread (multithreading project) completely
 * unaware of persistence — it only calls order.setStatus() as always,
 * and this observer handles the database side as a side effect.
 *
 * Registered in CoffeeChatApp via:
 *   shop.registerObserver(new PersistingOrderObserver(orderRepository));
 */
public class PersistingOrderObserver implements OrderObserver {

    @NotNull private final ChatOrderRepository orderRepository;

    public PersistingOrderObserver(@NotNull ChatOrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(
                orderRepository, "ChatOrderRepository cannot be null");
    }

    @Override
    public void update(@NotNull Order order, @NotNull OrderStatus event) {
        // Skip PLACED — that's already persisted by ChatService.handleOrderCommand()
        // Skip PREPARING — that's already persisted by ChatService.sendOrderToKitchen()
        // Mirror everything else: READY, FULFILLED, CANCELLED
        if (event == OrderStatus.PLACED || event == OrderStatus.PREPARING) {
            return;
        }

        try {
            orderRepository.updateStatus(order.getOrderId(), event.name());
        } catch (RuntimeException e) {
            // The order may not exist in the orders table if it was placed
            // outside the chat flow (e.g. via CLI or tests) — log but don't crash
            System.err.printf("[PersistingOrderObserver] Could not update status for " +
                    "order %s to %s: %s%n", order.getOrderId(), event, e.getMessage());
        }
    }
}
