package dev.saberlabs.persistence.repositories;

import dev.saberlabs.persistence.records.StoredCustomer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for persisted customers.
 */
public interface CustomerRepository {

    void save(@NotNull StoredCustomer customer);

    void saveAll(@NotNull List<StoredCustomer> customers);

    @NotNull List<StoredCustomer> findAll();

    @NotNull Optional<StoredCustomer> findById(@NotNull String customerId);

    void clear();
}
