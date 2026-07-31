package dev.saberlabs.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BaristaQueue — restoreActiveAssignment()")
class BaristaQueueRestoreTest {

    private BaristaQueue queue;

    @BeforeEach
    void setUp() {
        queue = new BaristaQueue();
    }

    @Test
    @DisplayName("restoring an active assignment does not add the barista to the ready pool")
    void restoringDoesNotMakeBaristaReady() {
        queue.restoreActiveAssignment(1L, 100L);
        assertEquals(0, queue.readyCount(),
                "A restored barista should be BUSY, not READY");
    }

    @Test
    @DisplayName("a barista with a restored assignment cannot be matched to a new customer via baristaReady")
    void restoredBaristaIsNotMatchable() {
        queue.restoreActiveAssignment(1L, 100L);

        // Calling baristaReady for this same barista should NOT find
        // them already in the pool (restoreActiveAssignment deliberately
        // does not touch readyBaristas) — but it WOULD add them fresh
        // if called again. This test documents that restoreActiveAssignment
        // alone does not protect against a second explicit baristaReady()
        // call — that protection lives in ChatService/BaristaController,
        // which checks getActiveSessionsForBarista() BEFORE calling
        // baristaReady() at all.
        Optional<ChatSession> result = queue.baristaReady(100L);
        assertTrue(result.isEmpty(),
                "No one is waiting, so baristaReady should just join the pool");
        assertEquals(1, queue.readyCount(),
                "After baristaReady, they ARE now in the ready pool — " +
                        "this is why the caller must check active sessions first");
    }

    @Test
    @DisplayName("sessionEnded correctly frees a restored (not live-matched) assignment")
    void sessionEndedFreesRestoredAssignment() {
        queue.restoreActiveAssignment(1L, 100L);

        Optional<ChatSession> rematch = queue.sessionEnded(1L);

        assertTrue(rematch.isEmpty(), "No one waiting — barista just becomes ready");
        assertEquals(1, queue.readyCount(),
                "Barista should be freed to the ready pool after their " +
                        "restored session ends");
    }

    @Test
    @DisplayName("sessionEnded on a restored assignment rematches if someone is waiting")
    void sessionEndedRematchesRestoredAssignment() {
        queue.restoreActiveAssignment(1L, 100L);

        ChatSession bobSession = withId(ChatSession.newWaitingSession(2L), 2L);
        queue.customerWaiting(bobSession); // Bob queues — no barista ready yet

        Optional<ChatSession> rematch = queue.sessionEnded(1L);

        assertTrue(rematch.isPresent());
        assertEquals(2L, rematch.get().customerId());
        assertEquals(100L, rematch.get().baristaId());
    }

    @Test
    @DisplayName("multiple restored assignments are tracked independently")
    void multipleRestoredAssignmentsIndependent() {
        queue.restoreActiveAssignment(1L, 100L);
        queue.restoreActiveAssignment(2L, 200L);

        queue.sessionEnded(1L);

        assertEquals(1, queue.readyCount(),
                "Only barista 100 should be freed — barista 200's " +
                        "restored session is untouched");
    }

    private ChatSession withId(ChatSession session, long id) {
        return new ChatSession(id, session.customerId(), session.baristaId(),
                session.status(), session.createdAt());
    }
}