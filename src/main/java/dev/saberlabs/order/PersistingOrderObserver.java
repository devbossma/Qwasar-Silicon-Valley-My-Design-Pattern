package dev.saberlabs.order;

import dev.saberlabs.chat.ChatNotificationService;
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
    @NotNull private final ChatNotificationService notificationService;

    public PersistingOrderObserver(@NotNull ChatOrderRepository orderRepository,
                                   @NotNull ChatNotificationService notificationService) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    @Override
    public void update(@NotNull Order order, @NotNull OrderStatus event) {
        if (event == OrderStatus.PLACED || event == OrderStatus.PREPARING) return;

        orderRepository.findById(order.getOrderId()).ifPresent(stored -> {
            orderRepository.updateStatus(order.getOrderId(), event.name());

            switch (event) {
                case READY -> notificationService.notifyOrderReady(
                        stored.customerId(),
                        order.getOrderId(),
                        order.getCoffee().getDescription());

                case FULFILLED -> notificationService.notifyOrderFulfilled(
                        stored.customerId(), order.getOrderId());

                default -> { }
            }
        });
    }
}
