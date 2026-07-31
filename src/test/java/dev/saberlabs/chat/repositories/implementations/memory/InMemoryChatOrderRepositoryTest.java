package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.order.StoredOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryChatOrderRepository")
class InMemoryChatOrderRepositoryTest {

    private InMemoryChatOrderRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryChatOrderRepository();
    }

    private StoredOrder buildOrder(String id, long customerId, String status) {
        return new StoredOrder(id, customerId, null, null,
                "espresso", List.of(), 2.50, status, LocalDateTime.now());
    }

    @Nested
    @DisplayName("nextOrderId()")
    class NextOrderIdTests {

        @Test
        @DisplayName("returns ORD-1 when no orders exist")
        void returnsOrd1WhenEmpty() {
            assertEquals("ORD-1", repo.nextOrderId());
        }

        @Test
        @DisplayName("increments based on existing orders' trailing numbers")
        void incrementsBasedOnExisting() {
            repo.save(buildOrder("ORD-1", 1L, "PLACED"));
            repo.save(buildOrder("ORD-2", 1L, "PLACED"));

            assertEquals("ORD-3", repo.nextOrderId());
        }

        @Test
        @DisplayName("handles gaps correctly — picks max, not count")
        void handlesGapsCorrectly() {
            repo.save(buildOrder("ORD-1", 1L, "PLACED"));
            repo.save(buildOrder("ORD-5", 1L, "PLACED")); // gap: 2,3,4 missing

            assertEquals("ORD-6", repo.nextOrderId());
        }
    }

    @Nested
    @DisplayName("updateAssignmentAndStatus()")
    class UpdateAssignmentTests {

        @Test
        @DisplayName("updates barista_id, session_id, and status together")
        void updatesAllThreeFields() {
            repo.save(buildOrder("ORD-1", 1L, "PLACED"));
            repo.updateAssignmentAndStatus("ORD-1", 100L, 5L, "PREPARING");

            var updated = repo.findById("ORD-1");
            assertTrue(updated.isPresent());
            assertEquals(100L, updated.get().baristaId());
            assertEquals(5L, updated.get().sessionId());
            assertEquals("PREPARING", updated.get().status());
        }

        @Test
        @DisplayName("does nothing when the order does not exist")
        void doesNothingForMissingOrder() {
            assertDoesNotThrow(() ->
                    repo.updateAssignmentAndStatus("ORD-999", 100L, 5L, "PREPARING"));
        }
    }

    @Nested
    @DisplayName("findByCustomerAndStatus()")
    class FindByCustomerAndStatusTests {

        @Test
        @DisplayName("filters correctly by both customer and status")
        void filtersCorrectly() {
            repo.save(buildOrder("ORD-1", 1L, "PLACED"));
            repo.save(buildOrder("ORD-2", 1L, "FULFILLED"));
            repo.save(buildOrder("ORD-3", 2L, "PLACED"));

            List<StoredOrder> results = repo.findByCustomerAndStatus(1L, "PLACED");
            assertEquals(1, results.size());
            assertEquals("ORD-1", results.get(0).id());
        }
    }
}