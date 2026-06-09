package dev.saberlabs.repository.implementations.memory;

import dev.saberlabs.persistence.records.StoredOrder;
import dev.saberlabs.repository.OrderRepository;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory order repository useful for demos and mapper checks.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, StoredOrder> orders = new LinkedHashMap<>();

    @Override
    public synchronized void save(@NotNull StoredOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");
        orders.put(order.orderId(), order);
    }

    @Override
    public synchronized void saveAll(@NotNull List<StoredOrder> orders) {
        Objects.requireNonNull(orders, "Orders cannot be null");
        this.orders.clear();
        orders.forEach(this::save);
    }

    @Override
    public synchronized @NotNull List<StoredOrder> findAll() {
        return List.copyOf(orders.values());
    }

    @Override
    public synchronized @NotNull Optional<StoredOrder> findById(@NotNull String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public synchronized void clear() {
        orders.clear();
    }
}
