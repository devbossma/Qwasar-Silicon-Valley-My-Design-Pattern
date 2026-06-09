package dev.saberlabs.persistence.repositories.implementations.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.saberlabs.persistence.PersistenceException;
import dev.saberlabs.persistence.records.StoredOrder;
import dev.saberlabs.persistence.repositories.OrderRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JSON file-backed repository for order snapshots.
 */
public class FileOrderRepository implements OrderRepository {

    /**
     * In Java, TypeReference is a utility class primarily provided by the Jackson library to capture, retain,
     * and pass generic type information at runtime, bypassing the limitations of Java's type erasure
     */
    private static final TypeReference<List<StoredOrder>> ORDER_LIST = new TypeReference<>() {};

    private final Path filePath;
    private final ObjectMapper objectMapper;

    public FileOrderRepository(@NotNull Path filePath) {
        this(filePath, new ObjectMapper());
    }

    public FileOrderRepository(@NotNull Path filePath, @NotNull ObjectMapper objectMapper) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper cannot be null");
    }

    @Override
    public synchronized void save(@NotNull StoredOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");
        List<StoredOrder> orders = new ArrayList<>(findAll().stream()
                .filter(existing -> !existing.orderId().equals(order.orderId()))
                .toList());
        orders.add(order);
        write(orders);
    }

    @Override
    public synchronized void saveAll(@NotNull List<StoredOrder> orders) {
        Objects.requireNonNull(orders, "Orders cannot be null");
        write(orders);
    }

    @Override
    public synchronized @NotNull List<StoredOrder> findAll() {
        if (Files.notExists(filePath)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(filePath.toFile(), ORDER_LIST);
        } catch (Exception e) {
            throw new PersistenceException("Could not read orders from " + filePath, e);
        }
    }

    @Override
    public synchronized @NotNull Optional<StoredOrder> findById(@NotNull String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        return findAll().stream()
                .filter(order -> order.orderId().equals(orderId))
                .findFirst();
    }

    @Override
    public synchronized void clear() {
        write(List.of());
    }

    private void write(@NotNull List<StoredOrder> orders) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), orders);
        } catch (IOException e) {
            throw new PersistenceException("Could not write orders to " + filePath, e);
        }
    }
}
