package dev.saberlabs.persistence.mappers;

import dev.saberlabs.models.Customer;
import dev.saberlabs.persistence.records.StoredCustomer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Converts Customer objects to/from persisted snapshots.
 */
public class CustomerPersistenceMapper {

    public @NotNull StoredCustomer toStoredCustomer(@NotNull Customer customer) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        return new StoredCustomer(
                customer.getId(),
                customer.getName(),
                customer.getTotalOrders(),
                customer.getLoyaltyTier().name()
        );
    }

    public @NotNull Customer toCustomer(@NotNull StoredCustomer storedCustomer) {
        Objects.requireNonNull(storedCustomer, "Stored customer cannot be null");
        Customer customer = new Customer(storedCustomer.id(), storedCustomer.name());
        customer.restoreTotalOrders(storedCustomer.totalOrders());
        return customer;
    }
}
