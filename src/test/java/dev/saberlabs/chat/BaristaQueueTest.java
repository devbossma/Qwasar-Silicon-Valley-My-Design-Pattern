package dev.saberlabs.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BaristaQueue — FIFO Matching Logic")
class BaristaQueueTest {

    private BaristaQueue queue;

    @BeforeEach
    void setUp() {
        queue = new BaristaQueue();
    }

    // =================================================================
    // Barista ready first, then customer arrives
    // =================================================================

    @Nested
    @DisplayName("Barista ready before customer")
    class BaristaReadyFirst {

        @Test
        @DisplayName("barista with no waiting customers stays READY, returns empty")
        void baristaReadyWithNoCustomers() {
            Optional<ChatSession> result = queue.baristaReady(100L);

            assertTrue(result.isEmpty());
            assertEquals(1, queue.readyCount());
            assertEquals(0, queue.waitingCount());
        }

        @Test
        @DisplayName("customer arriving after a ready barista is matched immediately")
        void customerMatchedToReadyBarista() {
            queue.baristaReady(100L);

            ChatSession waiting = withId(ChatSession.newWaitingSession(1L), 1L);
            ChatSession result = queue.customerWaiting(waiting);

            assertTrue(result.isActive());
            assertEquals(100L, result.baristaId());
            assertEquals(0, queue.readyCount(), "Barista should be removed from ready pool");
            assertEquals(0, queue.waitingCount(), "Customer should not be queued");
        }

        @Test
        @DisplayName("second customer waits when the only ready barista was already matched")
        void secondCustomerWaitsAfterBaristaTaken() {
            queue.baristaReady(100L);

            ChatSession first = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));
            ChatSession second = queue.customerWaiting(withId(ChatSession.newWaitingSession(2L), 2L));

            assertTrue(first.isActive());
            assertTrue(second.isWaiting());
            assertEquals(1, queue.waitingCount());
        }
    }

    // =================================================================
    // Customer waits first, then barista becomes ready
    // =================================================================

    @Nested
    @DisplayName("Customer waiting before barista")
    class CustomerWaitingFirst {

        @Test
        @DisplayName("customer with no ready baristas stays WAITING")
        void customerWaitsWithNoBaristas() {
            ChatSession session = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));

            assertTrue(session.isWaiting());
            assertEquals(1, queue.waitingCount());
            assertEquals(0, queue.readyCount());
        }

        @Test
        @DisplayName("barista becoming ready is matched immediately to the waiting customer")
        void baristaMatchedToWaitingCustomer() {
            queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));

            Optional<ChatSession> result = queue.baristaReady(100L);

            assertTrue(result.isPresent());
            assertTrue(result.get().isActive());
            assertEquals(100L, result.get().baristaId());
            assertEquals(0, queue.waitingCount());
            assertEquals(0, queue.readyCount());
        }

        @Test
        @DisplayName("second barista stays ready when only one customer was waiting")
        void secondBaristaStaysReadyAfterFirstMatched() {
            queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));

            queue.baristaReady(100L); // matched
            Optional<ChatSession> second = queue.baristaReady(200L); // nothing left

            assertTrue(second.isEmpty());
            assertEquals(1, queue.readyCount());
        }
    }

    // =================================================================
    // FIFO ordering guarantees
    // =================================================================

    @Nested
    @DisplayName("FIFO ordering")
    class FifoOrdering {

        @Test
        @DisplayName("longest-waiting customer is matched first")
        void oldestWaitingCustomerMatchedFirst() {
            ChatSession alice = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));
            ChatSession bob   = queue.customerWaiting(withId(ChatSession.newWaitingSession(2L), 2L));
            ChatSession carol = queue.customerWaiting(withId(ChatSession.newWaitingSession(3L), 3L));

            assertTrue(alice.isWaiting());
            assertTrue(bob.isWaiting());
            assertTrue(carol.isWaiting());

            Optional<ChatSession> matched = queue.baristaReady(100L);

            assertTrue(matched.isPresent());
            assertEquals(1L, matched.get().customerId(), "Alice (first in) should be matched first");
        }

        @Test
        @DisplayName("longest-ready barista is matched first")
        void oldestReadyBaristaMatchedFirst() {
            queue.baristaReady(100L); // ready first
            queue.baristaReady(200L); // ready second

            ChatSession result = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));

            assertEquals(100L, result.baristaId(), "First barista to go READY should be matched first");
        }

        @Test
        @DisplayName("multiple customers and baristas match in FIFO pairs")
        void multipleCustomersAndBaristasMatchInOrder() {
            queue.baristaReady(100L);
            queue.baristaReady(200L);
            queue.baristaReady(300L);

            ChatSession alice = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));
            ChatSession bob   = queue.customerWaiting(withId(ChatSession.newWaitingSession(2L), 2L));
            ChatSession carol = queue.customerWaiting(withId(ChatSession.newWaitingSession(3L), 3L));

            assertEquals(100L, alice.baristaId());
            assertEquals(200L, bob.baristaId());
            assertEquals(300L, carol.baristaId());
            assertEquals(0, queue.readyCount());
            assertEquals(0, queue.waitingCount());
        }
    }

    // =================================================================
    // Session ended — barista freed and re-matched
    // =================================================================

    @Nested
    @DisplayName("sessionEnded()")
    class SessionEndedTests {

        @Test
        @DisplayName("ending a session frees the barista back to READY when nobody is waiting")
        void freesBaristaToReadyPool() {
            queue.baristaReady(100L);
            ChatSession active = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));

            Optional<ChatSession> result = queue.sessionEnded(active.id());

            assertTrue(result.isEmpty(), "No one waiting — barista just becomes ready");
            assertEquals(1, queue.readyCount());
        }

        @Test
        @DisplayName("ending a session immediately reassigns the freed barista to the next waiting customer")
        void reassignsFreedBaristaToNextWaitingCustomer() {
            queue.baristaReady(100L);
            ChatSession aliceSession = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));
            // Bob queues while Sara (100L) is busy with Alice
            ChatSession bobSession = queue.customerWaiting(withId(ChatSession.newWaitingSession(2L), 2L));

            assertTrue(bobSession.isWaiting());

            Optional<ChatSession> reassigned = queue.sessionEnded(aliceSession.id());

            assertTrue(reassigned.isPresent());
            assertEquals(2L, reassigned.get().customerId());
            assertEquals(100L, reassigned.get().baristaId());
            assertEquals(0, queue.waitingCount());
        }

        @Test
        @DisplayName("ending a session that was never active is a no-op")
        void endingUnknownSessionIsNoOp() {
            Optional<ChatSession> result = queue.sessionEnded(999L);

            assertTrue(result.isEmpty());
            assertEquals(0, queue.readyCount());
            assertEquals(0, queue.waitingCount());
        }

        @Test
        @DisplayName("ending the same session twice does not double-free the barista")
        void endingSameSessionTwiceIsSafe() {
            queue.baristaReady(100L);
            ChatSession session = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));

            queue.sessionEnded(session.id());
            Optional<ChatSession> secondCall = queue.sessionEnded(session.id());

            assertTrue(secondCall.isEmpty());
            assertEquals(1, queue.readyCount(), "Barista should only be freed once");
        }
    }

    // =================================================================
    // baristaOffline()
    // =================================================================

    @Nested
    @DisplayName("baristaOffline()")
    class BaristaOfflineTests {

        @Test
        @DisplayName("removes a READY barista from the pool")
        void removesReadyBaristaFromPool() {
            queue.baristaReady(100L);
            queue.baristaOffline(100L);

            assertEquals(0, queue.readyCount());
        }

        @Test
        @DisplayName("going offline does not affect an already-ACTIVE assignment")
        void doesNotAffectActiveAssignment() {
            queue.baristaReady(100L);
            ChatSession active = queue.customerWaiting(withId(ChatSession.newWaitingSession(1L), 1L));

            queue.baristaOffline(100L); // barista is BUSY, not in ready pool anyway

            // Session ending should still correctly free/look up barista 100L
            Optional<ChatSession> result = queue.sessionEnded(active.id());
            assertTrue(result.isEmpty()); // freed with nobody waiting
        }

        @Test
        @DisplayName("going offline when not in the pool is a safe no-op")
        void offlineWhenNotInPoolIsNoOp() {
            assertDoesNotThrow(() -> queue.baristaOffline(999L));
            assertEquals(0, queue.readyCount());
        }
    }

    // =================================================================
    // Concurrency — no duplicate or lost matches under contention
    // =================================================================

    @Nested
    @DisplayName("Concurrency")
    class ConcurrencyTests {

        @Test
        @DisplayName("concurrent baristaReady and customerWaiting produce exactly one match per pair, no duplicates")
        void concurrentMatchingProducesNoDuplicates() throws InterruptedException {
            int pairCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(pairCount * 2);
            CountDownLatch latch = new CountDownLatch(pairCount * 2);
            AtomicInteger matchesFromBaristaSide = new AtomicInteger(0);
            AtomicInteger matchesFromCustomerSide = new AtomicInteger(0);

            for (int i = 0; i < pairCount; i++) {
                final long baristaId = 1000L + i;
                final long customerId = i;
                final long sessionId = i;

                executor.submit(() -> {
                    Optional<ChatSession> result = queue.baristaReady(baristaId);
                    if (result.isPresent()) matchesFromBaristaSide.incrementAndGet();
                    latch.countDown();
                });

                executor.submit(() -> {
                    ChatSession result = queue.customerWaiting(
                            withId(ChatSession.newWaitingSession(customerId), sessionId));
                    if (result.isActive()) matchesFromCustomerSide.incrementAndGet();
                    latch.countDown();
                });
            }

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "All concurrent operations should finish in time");

            // Every barista and every customer should be matched exactly once,
            // total matches counted across both sides == pairCount
            assertEquals(pairCount, matchesFromBaristaSide.get() + matchesFromCustomerSide.get(),
                    "Total successful matches should equal total pairs, no duplicates or losses");
            assertEquals(0, queue.readyCount(), "No barista should remain unmatched");
            assertEquals(0, queue.waitingCount(), "No customer should remain unmatched");
        }

        @Test
        @DisplayName("concurrent sessionEnded calls correctly free and rematch baristas without races")
        void concurrentSessionEndedRematchesCorrectly() throws InterruptedException {
            int baristaCount = 10;
            int customerCount = 20; // double — half wait after baristas are busy

            for (int i = 0; i < baristaCount; i++) {
                queue.baristaReady(1000L + i);
            }

            // First batch matches immediately, second batch queues
            for (int i = 0; i < customerCount; i++) {
                queue.customerWaiting(withId(ChatSession.newWaitingSession(i), i));
            }

            assertEquals(baristaCount, customerCount - queue.waitingCount(),
                    "First batch should be matched, rest queued");

            // Now end all the active sessions concurrently — each should free
            // a barista who immediately picks up a waiting customer
            ExecutorService executor = Executors.newFixedThreadPool(baristaCount);
            CountDownLatch latch = new CountDownLatch(baristaCount);

            for (int i = 0; i < baristaCount; i++) {
                final long sessionId = i;
                executor.submit(() -> {
                    queue.sessionEnded(sessionId);
                    latch.countDown();
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed);
            assertEquals(0, queue.waitingCount(),
                    "All originally-queued customers should now be matched");
            assertEquals(0, queue.readyCount(),
                    "All baristas should be busy again with the second batch");
        }

        @Test
        @DisplayName("no barista is ever assigned to two ACTIVE sessions at the same time")
        void noBaristaDoubleAssigned() throws InterruptedException {
            int baristaCount = 20;
            int customerCount = 20;

            ConcurrentHashMap<Long, AtomicInteger> assignmentCountPerBarista = new ConcurrentHashMap<>();

            ExecutorService executor = Executors.newFixedThreadPool(baristaCount + customerCount);
            CountDownLatch latch = new CountDownLatch(baristaCount + customerCount);

            for (int i = 0; i < baristaCount; i++) {
                final long baristaId = 1000L + i;
                executor.submit(() -> {
                    Optional<ChatSession> result = queue.baristaReady(baristaId);
                    result.ifPresent(s ->
                            assignmentCountPerBarista
                                    .computeIfAbsent(baristaId, k -> new AtomicInteger(0))
                                    .incrementAndGet());
                    latch.countDown();
                });
            }

            for (int i = 0; i < customerCount; i++) {
                final long sessionId = i;
                executor.submit(() -> {
                    ChatSession result = queue.customerWaiting(
                            withId(ChatSession.newWaitingSession(sessionId), sessionId));
                    if (result.isActive()) {
                        assignmentCountPerBarista
                                .computeIfAbsent(result.baristaId(), k -> new AtomicInteger(0))
                                .incrementAndGet();
                    }
                    latch.countDown();
                });
            }

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "All operations should finish in time");

            assignmentCountPerBarista.forEach((baristaId, count) ->
                    assertEquals(1, count.get(),
                            "Barista " + baristaId + " was assigned " + count.get()
                                    + " times concurrently — should be exactly 1"));
        }
    }

    // =================================================================
    // Helper
    // =================================================================

    /**
     * ChatSession.newWaitingSession() always sets id=0 since it's not yet
     * persisted. Tests need real distinct IDs to track sessions through
     * the queue, so this helper simulates "after repository.save()".
     */
    private ChatSession withId(ChatSession session, long id) {
        return new ChatSession(id, session.customerId(), session.baristaId(),
                session.status(), session.createdAt());
    }
}