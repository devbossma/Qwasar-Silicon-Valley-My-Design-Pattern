package dev.saberlabs.persistence.repositories;

import dev.saberlabs.persistence.records.StoredOrder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for persisted orders.
 */
public interface OrderRepository {

    void save(@NotNull StoredOrder order);

    void saveAll(@NotNull List<StoredOrder> orders);

    @NotNull List<StoredOrder> findAll();

    @NotNull Optional<StoredOrder> findById(@NotNull String orderId);

    void clear();
}
