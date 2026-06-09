package dev.saberlabs.multithread;

import dev.saberlabs.models.Order;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe bounded queue implementing the Producer-Consumer pattern.
 * *
 * Producers (CustomerThreads) call {@link #enqueue(Order)} to add orders.
 * Consumers (Baristas) call {@link #dequeue()} to take orders.
 * *
 * Uses a fair {@link ReentrantLock} with two {@link Condition}s:
 * - {@code notFull}  — producers wait here when the queue is at capacity.
 * - {@code notEmpty} — consumers wait here when the queue is empty.
 * *
 * Why ReentrantLock over synchronized:
 * - Two separate Condition objects allow targeted signaling:
 *   a producer only wakes consumers, a consumer only wakes producers.
 * - Fair mode prevents thread starvation.
 * - Explicit lock/unlock makes the synchronization intent visible.
 */
public class OrderQueue {

    private final Queue<Order> queue;
    private final int capacity;

    /** Guards all queue state. Fair mode — longest-waiting thread goes first. */
    private final ReentrantLock lock = new ReentrantLock(true);

    /** Signaled when the queue transitions from full --> not full. */
    private final Condition notFull = lock.newCondition();

    /** Signaled when the queue transitions from empty --> not empty. */
    private final Condition notEmpty = lock.newCondition();

    /**
     * Creates a bounded OrderQueue.
     *
     * @param capacity maximum number of orders the queue can hold, must be > 0
     * @throws IllegalArgumentException if capacity is zero or negative
     */
    public OrderQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.queue = new LinkedList<>();
    }

    /**
     * Adds an order to the tail of the queue.
     * *
     * Blocks if the queue is at capacity until a consumer dequeues an order
     * and signals {@code notFull}.
     *
     * @param order the order to enqueue, must not be null
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void enqueue(@NotNull Order order) throws InterruptedException {
        Objects.requireNonNull(order, "Order cannot be null");

        // Acquire the lock before modifying the queue to ensure thread safety.
        lock.lock();
        try {
            // Wait until there is space in the queue to add the new order.
            // This loop handles spurious wakeups and ensures the condition is re-checked after being signaled.
            while (queue.size() == capacity) {
                System.out.printf("[OrderQueue] Full (%d/%d) — %s waiting...%n",
                        queue.size(), capacity, order.getCustomer().getName());
                notFull.await();
            }
            queue.add(order);
            System.out.printf("[OrderQueue] Enqueued: %s for %s (%d/%d)%n",
                    order.getCoffee().getDescription(),
                    order.getCustomer().getName(),
                    queue.size(), capacity);
            notEmpty.signal();
        } finally {
            // Always release the lock in a finally block to avoid deadlocks, even if an exception occurs.
            lock.unlock();
        }
    }

    /**
     * Removes and returns the order at the head of the queue.
     * *
     * Blocks if the queue is empty until a producer enqueues an order
     * and signals {@code notEmpty}.
     *
     * @return the next order to prepare
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public @NotNull Order dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                System.out.println("[OrderQueue] Empty — barista waiting...");
                notEmpty.await();
            }
            Order order = queue.poll();
            System.out.printf("[OrderQueue] Dequeued: %s for %s (%d/%d)%n",
                    order.getCoffee().getDescription(),
                    order.getCustomer().getName(),
                    queue.size(), capacity);
            notFull.signal();
            return order;
        } finally {
            lock.unlock();
        }
    }

    /** @return current number of orders in the queue, thread-safe */
    public int size() {
        lock.lock();
        try { return queue.size(); }
        finally { lock.unlock(); }
    }

    /** @return true if the queue has no pending orders, thread-safe */
    public boolean isEmpty() {
        lock.lock();
        try { return queue.isEmpty(); }
        finally { lock.unlock(); }
    }

    /** @return true if the queue is at full capacity, thread-safe */
    public boolean isFull() {
        lock.lock();
        try { return queue.size() == capacity; }
        finally { lock.unlock(); }
    }

    /** @return the maximum number of orders this queue can hold */
    public int getCapacity() {
        return capacity;
    }
}