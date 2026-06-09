package dev.saberlabs.persistence.repositories.order;

import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.persistence.records.StoredCoffee;
import dev.saberlabs.persistence.records.StoredOrder;
import dev.saberlabs.repository.implementations.memory.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryOrderRepository")
class InMemoryOrderRepositoryTest {

    private InMemoryOrderRepository repository;

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

    private StoredCoffee buildCoffee(String description) {
        return new StoredCoffee(description,  List.of("Latte + Milk"), 2.50, description);
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("saves an order and makes it retrievable")
        void savesOrder() {
            repository.save(buildOrder("ORD-1"));
            assertEquals(1, repository.findAll().size());
        }

        @Test
        @DisplayName("overwrites existing order with same ID (upsert)")
        void upsertOnSameId() {
            StoredOrder first = buildOrder("ORD-1");
            StoredOrder updated = new StoredOrder(
                    "ORD-1", "CUST-1", buildCoffee("Latte + Milk"), 2.50, OrderStatus.PLACED
            );

            repository.save(first);
            repository.save(updated);

            assertEquals(1, repository.findAll().size());
            assertEquals("Latte + Milk",
                    repository.findById("ORD-1").orElseThrow().coffee().description());
        }

        @Test
        @DisplayName("throws on null order")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }
    }

    @Nested
    @DisplayName("saveAll()")
    class SaveAllTests {

        @Test
        @DisplayName("replaces all orders")
        void replacesAll() {
            repository.save(buildOrder("ORD-1"));
            repository.saveAll(List.of(buildOrder("ORD-2"), buildOrder("ORD-3")));

            List<StoredOrder> all = repository.findAll();
            assertEquals(2, all.size());
            assertTrue(all.stream().anyMatch(o -> o.orderId().equals("ORD-2")));
            assertTrue(all.stream().anyMatch(o -> o.orderId().equals("ORD-3")));
            assertFalse(all.stream().anyMatch(o -> o.orderId().equals("ORD-1")));
        }

        @Test
        @DisplayName("saveAll with empty list clears repository")
        void saveAllEmptyClears() {
            repository.save(buildOrder("ORD-1"));
            repository.saveAll(List.of());

            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        @DisplayName("throws on null list")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> repository.saveAll(null));
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns empty list when repository is empty")
        void emptyWhenNew() {
            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        @DisplayName("returns unmodifiable list")
        void returnsUnmodifiableList() {
            repository.save(buildOrder("ORD-1"));
            List<StoredOrder> all = repository.findAll();

            assertThrows(UnsupportedOperationException.class,
                    () -> all.add(buildOrder("ORD-99")));
        }

        @Test
        @DisplayName("preserves insertion order")
        void preservesInsertionOrder() {
            repository.save(buildOrder("ORD-1"));
            repository.save(buildOrder("ORD-2"));
            repository.save(buildOrder("ORD-3"));

            List<String> ids = repository.findAll().stream()
                    .map(StoredOrder::orderId)
                    .toList();

            assertEquals(List.of("ORD-1", "ORD-2", "ORD-3"), ids);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("finds existing order by ID")
        void findsById() {
            repository.save(buildOrder("ORD-42"));
            Optional<StoredOrder> result = repository.findById("ORD-42");

            assertTrue(result.isPresent());
            assertEquals("ORD-42", result.get().orderId());
        }

        @Test
        @DisplayName("returns empty Optional for unknown ID")
        void emptyForUnknown() {
            assertTrue(repository.findById("ORD-999").isEmpty());
        }

        @Test
        @DisplayName("throws on null ID")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> repository.findById(null));
        }
    }

    @Nested
    @DisplayName("clear()")
    class ClearTests {

        @Test
        @DisplayName("removes all orders")
        void clearsAll() {
            repository.save(buildOrder("ORD-1"));
            repository.save(buildOrder("ORD-2"));
            repository.clear();

            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        @DisplayName("clear on empty repository is safe")
        void clearOnEmptyIsSafe() {
            assertDoesNotThrow(() -> repository.clear());
        }
    }
}
