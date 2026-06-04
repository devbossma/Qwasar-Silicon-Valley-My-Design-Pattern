package dev.saberlabs.persistence.repositories.implimentatioins.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.saberlabs.persistence.PersistenceException;
import dev.saberlabs.persistence.records.StoredCustomer;
import dev.saberlabs.persistence.repositories.CustomerRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JSON file-backed repository for customer snapshots.
 */
public class FileCustomerRepository implements CustomerRepository {

    private static final TypeReference<List<StoredCustomer>> CUSTOMER_LIST = new TypeReference<>() {
    };

    private final Path filePath;
    private final ObjectMapper objectMapper;

    public FileCustomerRepository(@NotNull Path filePath) {
        this(filePath, new ObjectMapper());
    }

    public FileCustomerRepository(@NotNull Path filePath, @NotNull ObjectMapper objectMapper) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper cannot be null");
    }

    @Override
    public synchronized void save(@NotNull StoredCustomer customer) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        List<StoredCustomer> customers = findAll().stream()
                .filter(existing -> !existing.id().equals(customer.id()))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        customers.add(customer);
        write(customers);
    }

    @Override
    public synchronized void saveAll(@NotNull List<StoredCustomer> customers) {
        Objects.requireNonNull(customers, "Customers cannot be null");
        write(customers);
    }

    @Override
    public synchronized @NotNull List<StoredCustomer> findAll() {
        if (Files.notExists(filePath)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(filePath.toFile(), CUSTOMER_LIST);
        } catch (IOException e) {
            throw new PersistenceException("Could not read customers from " + filePath, e);
        }
    }

    @Override
    public synchronized @NotNull Optional<StoredCustomer> findById(@NotNull String customerId) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        return findAll().stream()
                .filter(customer -> customer.id().equals(customerId))
                .findFirst();
    }

    @Override
    public synchronized void clear() {
        write(List.of());
    }

    private void write(@NotNull List<StoredCustomer> customers) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), customers);
        } catch (IOException e) {
            throw new PersistenceException("Could not write customers to " + filePath, e);
        }
    }
}
