package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.chat.repositories.ChatOrderRepository;
import dev.saberlabs.order.StoredOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryChatOrderRepository implements ChatOrderRepository {

    private final List<StoredOrder> orders = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    @Override
    public synchronized @NotNull String nextOrderId() {
        return "ORD-" + idCounter.incrementAndGet();
    }

    @Override
    public synchronized @NotNull StoredOrder save(@NotNull StoredOrder order) {
        orders.removeIf(o -> o.id().equals(order.id()));
        orders.add(order);
        return order;
    }

    @Override
    public synchronized void updateAssignmentAndStatus(@NotNull String orderId,
                                                       @Nullable Long baristaId,
                                                       @Nullable Long sessionId,
                                                       @NotNull String status) {
        findById(orderId).ifPresent(existing -> {
            orders.removeIf(o -> o.id().equals(orderId));
            orders.add(new StoredOrder(existing.id(), existing.customerId(), baristaId,
                    sessionId, existing.baseCoffee(), existing.extras(),
                    existing.total(), status, existing.createdAt()));
        });
    }

    @Override
    public synchronized void updateStatus(@NotNull String orderId, @NotNull String status) {
        findById(orderId).ifPresent(existing -> {
            orders.removeIf(o -> o.id().equals(orderId));
            orders.add(new StoredOrder(existing.id(), existing.customerId(),
                    existing.baristaId(), existing.sessionId(), existing.baseCoffee(),
                    existing.extras(), existing.total(), status, existing.createdAt()));
        });
    }

    @Override
    public synchronized @NotNull Optional<StoredOrder> findById(@NotNull String id) {
        return orders.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    @Override
    public synchronized @NotNull List<StoredOrder> findByCustomerAndStatus(long customerId,
                                                                           @NotNull String status) {
        return orders.stream()
                .filter(o -> o.customerId() == customerId && o.status().equals(status))
                .toList();
    }

    @Override
    public synchronized @NotNull List<StoredOrder> findByCustomer(long customerId) {
        return orders.stream().filter(o -> o.customerId() == customerId).toList();
    }

    @Override
    public synchronized @NotNull List<StoredOrder> findBySessionId(long sessionId) {
        return orders.stream()
                .filter(o -> sessionId == (o.sessionId() == null ? -1 : o.sessionId()))
                .toList();
    }

    @Override
    public synchronized @NotNull List<StoredOrder> findAll() {
        return List.copyOf(orders);
    }

    public synchronized void clear() {
        orders.clear();
    }
}