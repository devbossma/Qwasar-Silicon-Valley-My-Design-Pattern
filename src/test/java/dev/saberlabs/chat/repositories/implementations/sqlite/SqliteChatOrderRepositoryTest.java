package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.db.DatabaseUtil;
import dev.saberlabs.order.StoredOrder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises SqliteChatOrderRepository against a real temp-file SQLite database,
 * following the pattern established by SqliteUserRepositoryTest.
 */
@DisplayName("SqliteChatOrderRepository")
class SqliteChatOrderRepositoryTest {

    private Path tempDbFile;
    private SqliteChatOrderRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        DatabaseUtil.closeAllConnections();
        tempDbFile = Files.createTempFile("coffee-chat-order-repo-test-", ".db");
        Files.deleteIfExists(tempDbFile);
        DatabaseUtil.setDbPathForTesting(tempDbFile.toString());
        DatabaseUtil.initialize();
        repository = new SqliteChatOrderRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseUtil.closeAllConnections();
        Files.deleteIfExists(tempDbFile);
    }

    private StoredOrder buildOrder(String id, long customerId, String status) {
        return new StoredOrder(id, customerId, null, null,
                "Espresso", List.of(), 2.50, status, LocalDateTime.now());
    }

    @Nested
    @DisplayName("nextOrderId()")
    class NextOrderIdTests {

        @Test
        @DisplayName("returns ORD-1 when no orders exist")
        void returnsOrd1WhenEmpty() {
            assertEquals("ORD-1", repository.nextOrderId());
        }

        @Test
        @DisplayName("increments based on the highest existing trailing number")
        void incrementsBasedOnExisting() {
            repository.save(buildOrder("ORD-1", 1L, "PLACED"));
            repository.save(buildOrder("ORD-5", 1L, "PLACED"));

            assertEquals("ORD-6", repository.nextOrderId());
        }
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("inserts a new order with all fields preserved")
        void insertsNewOrder() {
            StoredOrder saved = repository.save(
                    new StoredOrder("ORD-1", 1L, 100L, 5L, "Latte",
                            List.of("milk", "sugar"), 3.25, "PLACED", LocalDateTime.now()));

            Optional<StoredOrder> found = repository.findById("ORD-1");
            assertTrue(found.isPresent());
            assertEquals(List.of("milk", "sugar"), found.get().extras());
            assertEquals(100L, found.get().baristaId());
            assertEquals(5L, found.get().sessionId());
            assertEquals("ORD-1", saved.id());
        }

        @Test
        @DisplayName("upserts on conflicting ID, updating barista/session/status only")
        void upsertsOnConflict() {
            repository.save(buildOrder("ORD-1", 1L, "PLACED"));
            repository.save(new StoredOrder("ORD-1", 1L, 200L, 9L,
                    "Espresso", List.of(), 2.50, "PREPARING", LocalDateTime.now()));

            Optional<StoredOrder> found = repository.findById("ORD-1");
            assertTrue(found.isPresent());
            assertEquals(200L, found.get().baristaId());
            assertEquals(9L, found.get().sessionId());
            assertEquals("PREPARING", found.get().status());
        }
    }

    @Nested
    @DisplayName("updateAssignmentAndStatus()")
    class UpdateAssignmentAndStatusTests {

        @Test
        @DisplayName("updates barista, session, and status together")
        void updatesAllThreeFields() {
            repository.save(buildOrder("ORD-1", 1L, "PLACED"));

            repository.updateAssignmentAndStatus("ORD-1", 100L, 5L, "PREPARING");

            Optional<StoredOrder> found = repository.findById("ORD-1");
            assertTrue(found.isPresent());
            assertEquals(100L, found.get().baristaId());
            assertEquals(5L, found.get().sessionId());
            assertEquals("PREPARING", found.get().status());
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("updates only the status field")
        void updatesStatusOnly() {
            repository.save(buildOrder("ORD-1", 1L, "PLACED"));

            repository.updateStatus("ORD-1", "READY");

            assertEquals("READY", repository.findById("ORD-1").orElseThrow().status());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns empty for an unknown ID")
        void returnsEmptyWhenNotFound() {
            assertTrue(repository.findById("ORD-999").isEmpty());
        }
    }

    @Nested
    @DisplayName("findByCustomerAndStatus()")
    class FindByCustomerAndStatusTests {

        @Test
        @DisplayName("filters by both customer and status")
        void filtersCorrectly() {
            repository.save(buildOrder("ORD-1", 1L, "PLACED"));
            repository.save(buildOrder("ORD-2", 1L, "FULFILLED"));
            repository.save(buildOrder("ORD-3", 2L, "PLACED"));

            List<StoredOrder> results = repository.findByCustomerAndStatus(1L, "PLACED");

            assertEquals(1, results.size());
            assertEquals("ORD-1", results.get(0).id());
        }
    }

    @Nested
    @DisplayName("findByCustomer()")
    class FindByCustomerTests {

        @Test
        @DisplayName("returns every order for the given customer")
        void returnsOrdersForCustomer() {
            repository.save(buildOrder("ORD-1", 1L, "PLACED"));
            repository.save(buildOrder("ORD-2", 1L, "FULFILLED"));
            repository.save(buildOrder("ORD-3", 2L, "PLACED"));

            assertEquals(2, repository.findByCustomer(1L).size());
        }
    }

    @Nested
    @DisplayName("findBySessionId()")
    class FindBySessionIdTests {

        @Test
        @DisplayName("returns every order placed within the given session")
        void returnsOrdersForSession() {
            repository.save(new StoredOrder("ORD-1", 1L, null, 5L,
                    "Espresso", List.of(), 2.50, "PLACED", LocalDateTime.now()));
            repository.save(buildOrder("ORD-2", 1L, "PLACED"));

            List<StoredOrder> results = repository.findBySessionId(5L);

            assertEquals(1, results.size());
            assertEquals("ORD-1", results.get(0).id());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns every saved order")
        void returnsAllOrders() {
            repository.save(buildOrder("ORD-1", 1L, "PLACED"));
            repository.save(buildOrder("ORD-2", 2L, "PLACED"));

            assertEquals(2, repository.findAll().size());
        }

        @Test
        @DisplayName("returns an empty list when there are no orders")
        void returnsEmptyWhenNoOrders() {
            assertTrue(repository.findAll().isEmpty());
        }
    }
}
