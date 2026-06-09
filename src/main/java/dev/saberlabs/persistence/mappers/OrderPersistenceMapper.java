package dev.saberlabs.persistence.mappers;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.persistence.PersistenceException;
import dev.saberlabs.persistence.records.StoredOrder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Converts Order objects to/from persisted snapshots.
 */
public class OrderPersistenceMapper {

    private final CoffeePersistenceMapper coffeeMapper;

    public OrderPersistenceMapper() {
        this(new CoffeePersistenceMapper());
    }

    public OrderPersistenceMapper(@NotNull CoffeePersistenceMapper coffeeMapper) {
        this.coffeeMapper = Objects.requireNonNull(coffeeMapper, "Coffee mapper cannot be null");
    }

    public @NotNull StoredOrder toStoredOrder(@NotNull Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        OrderStatus status = order.getStatus() == null ? null : order.getStatus();
        return new StoredOrder(
                order.getOrderId(),
                order.getCustomer().getId(),
                coffeeMapper.toStoredCoffee(order.getCoffee()),
                order.getFinalPrice(),
                status
        );
    }

    public @NotNull Order toOrder(
            @NotNull StoredOrder storedOrder,
            @NotNull Map<String, Customer> customersById
    ) {
        Objects.requireNonNull(storedOrder, "Stored order cannot be null");
        Objects.requireNonNull(customersById, "Customers map cannot be null");

        Customer customer = customersById.get(storedOrder.customerId());
        if (customer == null) {
            throw new PersistenceException("No customer found for order: " + storedOrder.orderId());
        }

        Coffee coffee = coffeeMapper.toCoffee(storedOrder.coffee());
        Order order = new Order(customer, coffee, storedOrder.orderId());
        order.setFinalPrice(storedOrder.finalPrice());
        if (storedOrder.status() != null) {
            order.restoreStatus(storedOrder.status());
        }
        return order;
    }
}
