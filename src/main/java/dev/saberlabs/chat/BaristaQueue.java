package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordinates the live barista-pool chat queue: matches WAITING customers
 * to READY baristas, FIFO on both sides.
 *
 * This is the "ready/waiting" runtime equivalent of {@link dev.saberlabs.multithread.OrderQueue},
 * but for chat session assignment rather than coffee orders. Live state
 * lives here in memory; {@link ChatSessionRepository} persists the
 * historical record to SQLite separately.
 *
 * Thread safety: a single {@link ReentrantLock} guards both queues since
 * an assignment touches both atomically (dequeue customer + dequeue barista
 * together, or neither).
 */
public class BaristaQueue {

    private final ReentrantLock lock = new ReentrantLock(true);

    /** Baristas currently READY, FIFO — oldest-ready served first. */
    private final Deque<Long> readyBaristas = new ArrayDeque<>();

    /** Sessions currently WAITING, FIFO — oldest-waiting served first. */
    private final Deque<ChatSession> waitingSessions = new ArrayDeque<>();

    /** Live view of which barista is handling which session, if ACTIVE. */
    private final Map<Long, Long> activeAssignments = new LinkedHashMap<>(); // sessionId -> baristaId

    /**
     * Marks a barista as READY and immediately tries to match them
     * with the longest-waiting customer, if any.
     *
     * @param baristaId the barista becoming available
     * @return the session they were matched with, or empty if none waiting
     */
    public @NotNull Optional<ChatSession> baristaReady(long baristaId) {
        lock.lock();
        try {
            if (!waitingSessions.isEmpty()) {
                ChatSession session = waitingSessions.poll();
                ChatSession active = session.assignTo(baristaId);
                activeAssignments.put(active.id(), baristaId);
                return Optional.of(active);
            }
            readyBaristas.offer(baristaId);
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enqueues a new customer session and immediately tries to match it
     * with the longest-ready barista, if any.
     *
     * @param session a newly created WAITING session (must have id != 0,
     *                already persisted by the caller)
     * @return the same session, now ACTIVE if a barista was available,
     *         or unchanged (still WAITING) otherwise
     */
    public @NotNull ChatSession customerWaiting(@NotNull ChatSession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        lock.lock();
        try {
            if (!readyBaristas.isEmpty()) {
                long baristaId = readyBaristas.poll();
                ChatSession active = session.assignTo(baristaId);
                activeAssignments.put(active.id(), baristaId);
                return active;
            }
            waitingSessions.offer(session);
            return session;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Called when a session ends (order fulfilled / conversation closed).
     * Frees the barista and immediately tries to match them with the
     * next waiting customer, if any.
     *
     * @param sessionId the session that just ended
     * @return the barista's next session if one was waiting, or empty
     *         if the barista returns to the READY pool with no work
     */
    public @NotNull Optional<ChatSession> sessionEnded(long sessionId) {
        lock.lock();
        try {
            Long baristaId = activeAssignments.remove(sessionId);
            if (baristaId == null) {
                return Optional.empty(); // session wasn't active, nothing to free
            }
            return baristaReady(baristaId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a barista from the queue entirely (e.g. logging out).
     * Does not affect any session they are already ACTIVE on.
     *
     * @param baristaId the barista going OFFLINE
     */
    public void baristaOffline(long baristaId) {
        lock.lock();
        try {
            readyBaristas.remove(baristaId);
        } finally {
            lock.unlock();
        }
    }

    /** @return number of customers currently waiting for a barista */
    public int waitingCount() {
        lock.lock();
        try { return waitingSessions.size(); }
        finally { lock.unlock(); }
    }

    /** @return number of baristas currently idle and ready */
    public int readyCount() {
        lock.lock();
        try { return readyBaristas.size(); }
        finally { lock.unlock(); }
    }
}