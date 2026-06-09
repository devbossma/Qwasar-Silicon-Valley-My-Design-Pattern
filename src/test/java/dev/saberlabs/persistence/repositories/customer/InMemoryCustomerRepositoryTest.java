package dev.saberlabs.persistence.repositories.customer;

import dev.saberlabs.models.LoyaltyTier;
import dev.saberlabs.persistence.records.StoredCustomer;
import dev.saberlabs.repository.implementations.memory.InMemoryCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryCustomerRepository")
class InMemoryCustomerRepositoryTest {

    private InMemoryCustomerRepository repository;

    private StoredCustomer buildCustomer(String id) {
        return new StoredCustomer(
                id, "Customer " + id, 4, LoyaltyTier.SILVER);
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryCustomerRepository();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("saves a customer and makes it retrievable")
        void savesCustomer() {
            repository.save(buildCustomer("CUST-1"));
            assertEquals(1, repository.findAll().size());
        }

        @Test
        @DisplayName("overwrites existing customer with same ID (upsert)")
        void upsertOnSameId() {
            repository.save(buildCustomer("CUST-1"));
            StoredCustomer updated = new StoredCustomer(
                    "CUST-1", "Alice Updated", 12, LoyaltyTier.GOLD);
            repository.save(updated);

            assertEquals(1, repository.findAll().size());
            assertEquals("Alice Updated",
                    repository.findById("CUST-1").orElseThrow().name());
            assertEquals(LoyaltyTier.GOLD,
                    repository.findById("CUST-1").orElseThrow().loyaltyTier());
        }

        @Test
        @DisplayName("throws on null customer")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }
    }

    @Nested
    @DisplayName("saveAll()")
    class SaveAllTests {

        @Test
        @DisplayName("replaces all customers")
        void replacesAll() {
            repository.save(buildCustomer("CUST-1"));
            repository.saveAll(List.of(
                    buildCustomer("CUST-2"),
                    buildCustomer("CUST-3")));

            assertEquals(2, repository.findAll().size());
            assertTrue(repository.findById("CUST-1").isEmpty());
        }

        @Test
        @DisplayName("saveAll with empty list clears repository")
        void saveAllEmptyClears() {
            repository.save(buildCustomer("CUST-1"));
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
        void returnsUnmodifiable() {
            repository.save(buildCustomer("CUST-1"));
            List<StoredCustomer> all = repository.findAll();

            assertThrows(UnsupportedOperationException.class,
                    () -> all.add(buildCustomer("CUST-99")));
        }

        @Test
        @DisplayName("preserves insertion order")
        void preservesInsertionOrder() {
            repository.save(buildCustomer("CUST-1"));
            repository.save(buildCustomer("CUST-2"));
            repository.save(buildCustomer("CUST-3"));

            List<String> ids = repository.findAll().stream()
                    .map(StoredCustomer::id)
                    .toList();
            assertEquals(List.of("CUST-1", "CUST-2", "CUST-3"), ids);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("finds existing customer by ID")
        void findsById() {
            repository.save(buildCustomer("CUST-42"));
            Optional<StoredCustomer> result = repository.findById("CUST-42");

            assertTrue(result.isPresent());
            assertEquals("CUST-42", result.get().id());
        }

        @Test
        @DisplayName("returns empty Optional for unknown ID")
        void emptyForUnknown() {
            assertTrue(repository.findById("CUST-999").isEmpty());
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
        @DisplayName("removes all customers")
        void clearsAll() {
            repository.save(buildCustomer("CUST-1"));
            repository.save(buildCustomer("CUST-2"));
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
