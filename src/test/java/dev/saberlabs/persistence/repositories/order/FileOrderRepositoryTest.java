package dev.saberlabs.persistence.repositories.order;

import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.persistence.records.StoredCoffee;
import dev.saberlabs.persistence.records.StoredOrder;
import dev.saberlabs.persistence.repositories.implementations.file.FileOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileOrderRepository")
class FileOrderRepositoryTest {

    @TempDir
    Path tempDir;

    private FileOrderRepository repository;

    private StoredOrder buildOrder(String orderId) {
        return new StoredOrder(
                orderId,
                "CUST-1",
                new StoredCoffee("Espresso", List.of(),
                        2.50,
                        "Espresso"),
                2.50,
                OrderStatus.PLACED
        );
    }

    @BeforeEach
    void setUp() {
        repository = new FileOrderRepository(tempDir.resolve("orders.json"));
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("saves and persists an order to disk")
        void savesToDisk() {
            repository.save(buildOrder("ORD-1"));

            // New instance — reads from disk
            FileOrderRepository fresh = new FileOrderRepository(
                    tempDir.resolve("orders.json"));
            assertEquals(1, fresh.findAll().size());
            assertEquals("ORD-1", fresh.findAll().getFirst().orderId());
        }

        @Test
        @DisplayName("upserts — overwriting order with same ID")
        void upsertOverwritesExisting() {
            repository.save(buildOrder("ORD-1"));
            StoredOrder updated = new StoredOrder(
                    "ORD-1", "CUST-1", new StoredCoffee("Latte", List.of(), 4.00, "Latte"),
                    4.00, OrderStatus.PLACED
            );
            repository.save(updated);

            assertEquals(1, repository.findAll().size());
            assertEquals("Latte", repository.findById("ORD-1").orElseThrow().coffee().baseType());
        }

        @Test
        @DisplayName("throws on null order")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns empty list when file does not exist")
        void emptyWhenNoFile() {
            FileOrderRepository fresh = new FileOrderRepository(
                    tempDir.resolve("nonexistent.json"));
            assertTrue(fresh.findAll().isEmpty());
        }

        @Test
        @DisplayName("persists multiple orders and reads them back")
        void persistsMultiple() {
            repository.save(buildOrder("ORD-1"));
            repository.save(buildOrder("ORD-2"));
            repository.save(buildOrder("ORD-3"));

            assertEquals(3, repository.findAll().size());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("finds an order by ID after persistence")
        void findsPersistedOrder() {
            repository.save(buildOrder("ORD-7"));
            assertTrue(repository.findById("ORD-7").isPresent());
        }

        @Test
        @DisplayName("returns empty Optional for unknown ID")
        void emptyForUnknown() {
            assertTrue(repository.findById("ORD-999").isEmpty());
        }

        @Test
        @DisplayName("throws on null ID")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> repository.findById(null));
        }
    }

    @Nested
    @DisplayName("saveAll()")
    class SaveAllTests {

        @Test
        @DisplayName("replaces all orders on disk")
        void replacesOnDisk() {
            repository.save(buildOrder("ORD-1"));
            repository.saveAll(List.of(
                    buildOrder("ORD-2"),
                    buildOrder("ORD-3")));

            assertEquals(2, repository.findAll().size());
            assertTrue(repository.findById("ORD-1").isEmpty());
        }

        @Test
        @DisplayName("saveAll with empty list clears file")
        void saveAllEmptyClears() {
            repository.save(buildOrder("ORD-1"));
            repository.saveAll(List.of());
            assertTrue(repository.findAll().isEmpty());
        }
    }

    @Nested
    @DisplayName("clear()")
    class ClearTests {

        @Test
        @DisplayName("clears all persisted orders")
        void clearsAll() {
            repository.save(buildOrder("ORD-1"));
            repository.save(buildOrder("ORD-2"));
            repository.clear();

            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        @DisplayName("creates nested directories if they do not exist")
        void createsDirectories() {
            FileOrderRepository nested = new FileOrderRepository(
                    tempDir.resolve("deep/nested/dir/orders.json"));

            assertDoesNotThrow(() -> nested.save(buildOrder("ORD-1")));
            assertEquals(1, nested.findAll().size());
        }
    }
}
