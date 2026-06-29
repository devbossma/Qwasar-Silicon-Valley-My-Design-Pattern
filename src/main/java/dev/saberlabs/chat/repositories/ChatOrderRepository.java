package dev.saberlabs.chat.repositories;

import dev.saberlabs.order.StoredOrder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for persisted order tracking — which barista
 * and chat session an order moved through, separate from the JSON-based
 * Order/Customer snapshot persistence in the original persistence package.
 */
public interface ChatOrderRepository {

    @NotNull String nextOrderId();

    @NotNull StoredOrder save(@NotNull StoredOrder order);

    void updateAssignmentAndStatus(@NotNull String orderId,
                                   Long baristaId,
                                   Long sessionId,
                                   @NotNull String status);

    void updateStatus(@NotNull String orderId, @NotNull String status);

    @NotNull Optional<StoredOrder> findById(@NotNull String id);

    @NotNull List<StoredOrder> findByCustomerAndStatus(long customerId, @NotNull String status);

    @NotNull List<StoredOrder> findByCustomer(long customerId);

    @NotNull List<StoredOrder> findBySessionId(long sessionId);

    @NotNull List<StoredOrder> findAll();
}