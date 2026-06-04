package dev.saberlabs.persistence.repositories.implimentatioins.memory;

import dev.saberlabs.persistence.records.StoredCustomer;
import dev.saberlabs.persistence.repositories.CustomerRepository;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory customer repository useful for demos and mapper checks.
 */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, StoredCustomer> customers = new LinkedHashMap<>();

    @Override
    public synchronized void save(@NotNull StoredCustomer customer) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        customers.put(customer.id(), customer);
    }

    @Override
    public synchronized void saveAll(@NotNull List<StoredCustomer> customers) {
        Objects.requireNonNull(customers, "Customers cannot be null");
        this.customers.clear();
        customers.forEach(this::save);
    }

    @Override
    public synchronized @NotNull List<StoredCustomer> findAll() {
        return List.copyOf(customers.values());
    }

    @Override
    public synchronized @NotNull Optional<StoredCustomer> findById(@NotNull String customerId) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        return Optional.ofNullable(customers.get(customerId));
    }

    @Override
    public synchronized void clear() {
        customers.clear();
    }
}
