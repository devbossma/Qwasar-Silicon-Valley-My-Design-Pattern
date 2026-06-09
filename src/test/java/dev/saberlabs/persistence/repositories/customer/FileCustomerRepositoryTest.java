package dev.saberlabs.persistence.repositories.customer;

import dev.saberlabs.models.LoyaltyTier;
import dev.saberlabs.persistence.records.StoredCustomer;
import dev.saberlabs.persistence.repositories.implementations.file.FileCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileCustomerRepository")
class FileCustomerRepositoryTest {

    @TempDir
    Path tempDir;

    private FileCustomerRepository repository;

    private StoredCustomer buildCustomer(String id) {
        return new StoredCustomer(
                "CUST-" + id, "Customer " + id, 4, LoyaltyTier.SILVER);
    }

    @BeforeEach
    void setUp() {
        repository = new FileCustomerRepository(tempDir.resolve("customers.json"));
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("saves and persists a customer to disk")
        void savesToDisk() {
            repository.save(buildCustomer("CUST-1"));

            FileCustomerRepository fresh = new FileCustomerRepository(
                    tempDir.resolve("customers.json"));
            assertEquals(1, fresh.findAll().size());
        }

        @Test
        @DisplayName("upserts — overwrites customer with same ID")
        void upsertOverwritesExisting() {
            repository.save(buildCustomer("1"));
            StoredCustomer updated = new StoredCustomer(
                    "CUST-1", "Alice Gold", 15, LoyaltyTier.GOLD); // totalOrders then LoyaltyTier

            repository.save(updated);

            assertEquals(1, repository.findAll().size());
            assertEquals(LoyaltyTier.GOLD,
                    repository.findById("CUST-1").orElseThrow().loyaltyTier());
            assertEquals(15,
                    repository.findById("CUST-1").orElseThrow().totalOrders());
        }

        @Test
        @DisplayName("throws on null customer")
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
            FileCustomerRepository fresh = new FileCustomerRepository(
                    tempDir.resolve("nonexistent.json"));
            assertTrue(fresh.findAll().isEmpty());
        }

        @Test
        @DisplayName("persists all loyalty tiers correctly")
        void persistsAllTiers() {
            repository.save(new StoredCustomer("CUST-1", "Regular",3,  LoyaltyTier.REGULAR));
            repository.save(new StoredCustomer("CUST-2", "Silver", 8, LoyaltyTier.SILVER));
            repository.save(new StoredCustomer("CUST-3", "Gold", 15, LoyaltyTier.GOLD));

            List<StoredCustomer> all = repository.findAll();
            assertEquals(3, all.size());
            assertEquals(LoyaltyTier.REGULAR, all.get(0).loyaltyTier());
            assertEquals(LoyaltyTier.SILVER, all.get(1).loyaltyTier());
            assertEquals(LoyaltyTier.GOLD, all.get(2).loyaltyTier());
        }
    }

    @Nested
    @DisplayName("clear()")
    class ClearTests {

        @Test
        @DisplayName("clears all persisted customers")
        void clearsAll() {
            repository.save(buildCustomer("CUST-1"));
            repository.save(buildCustomer("CUST-2"));
            repository.clear();

            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        @DisplayName("creates nested directories if they do not exist")
        void createsDirectories() {
            FileCustomerRepository nested = new FileCustomerRepository(
                    tempDir.resolve("a/b/c/customers.json"));

            assertDoesNotThrow(() -> nested.save(buildCustomer("CUST-1")));
            assertEquals(1, nested.findAll().size());
        }
    }
}
