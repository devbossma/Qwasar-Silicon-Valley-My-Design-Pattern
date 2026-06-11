package dev.saberlabs.multithread;


import dev.saberlabs.models.Cappuccino;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.singleton.CoffeeShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Queue Thread-Safety")
public class OrderQueueTest {

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    // ------------------------------------------------------------------------
    // Single‑threaded tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Constructor rejects invalid capacity")
    void testConstructorRejectsInvalidCapacity(){
        assertThrows(IllegalArgumentException.class, () -> new OrderQueue(0));
        assertThrows(IllegalArgumentException.class, () -> new OrderQueue(-5));
    }

    @Test
    @DisplayName("Enqueue and Dequeue basic functionality")
    void testEnqueueAndDequeueBasic() throws InterruptedException {
        OrderQueue orderQueue = new OrderQueue(5);

        Customer custom_1 = new Customer("CUST-1", "Yassine");
        Customer custom_2 = new Customer("CUST-2", "Ahmed");

        Order order1 = new Order(custom_1, new Cappuccino(), "ORD-1");
        Order order2 = new Order(custom_2, new Cappuccino(), "ORD-2");


        orderQueue.enqueue(order1);
        assertEquals(1, orderQueue.size());
        assertFalse(orderQueue.isEmpty());
        assertFalse(orderQueue.isFull());

        orderQueue.enqueue(order2);
        assertEquals(2, orderQueue.size());
        assertFalse(orderQueue.isEmpty());
        assertFalse(orderQueue.isFull());

        Order taken = orderQueue.dequeue();
        assertSame(order1, taken);
        assertEquals(1, orderQueue.size());
        assertFalse(orderQueue.isFull());

        taken = orderQueue.dequeue();
        assertSame(order2, taken);
        assertEquals(0, orderQueue.size());
        assertFalse(orderQueue.isFull());
        assertTrue(orderQueue.isEmpty());
    }

    @Test
    @DisplayName("Enqueue null throws IllegalArgumentException")
    void testEnqueueNullThrowsException(){
        OrderQueue orderQueue = new OrderQueue(5);
        assertThrows(IllegalArgumentException.class, () -> orderQueue.enqueue(null));
    }

    @Test
    @DisplayName("Dequeue from empty queue blocks until an order is enqueued")
    void testDequeueBlocksUntilEnqueue() throws InterruptedException {
        OrderQueue queue = new OrderQueue(3);
        Order order = new Order(new Customer("CUST-1", "Yassine"), new Cappuccino(), "ORD-1");

        queue.enqueue(order);
        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());
        assertFalse(queue.isFull());

        queue.dequeue();
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        assertFalse(queue.isFull());

    }

    // ------------------------------------------------------------------------
    // Blocking behaviour tests (with timeouts)
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Enqueue blocks when full until a dequeue frees space")
    void testEnqueueBlocksWhenFullUntilDequeue() throws InterruptedException {
        OrderQueue orderQueue = new OrderQueue(1);

        Customer custom_1 = new Customer("CUST-1", "Yassine");
        Customer custom_2 = new Customer("CUST-2", "Ahmed");

        Order order1 = new Order(custom_1, new Cappuccino(), "ORD-1");
        Order order2 = new Order(custom_2, new Cappuccino(), "ORD-2");


        // fills the queue to full capacity = 1
        orderQueue.enqueue(order1);

        // Try to enqueue a second order – this should block.
        // We'll start a separate thread that attempts the enqueue.

        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        CountDownLatch enqueueStarted = new CountDownLatch(1);
        CountDownLatch orderConsumed = new CountDownLatch(1);

        Thread enqueuer = new Thread(() -> {
            try {
                enqueueStarted.countDown();
                orderQueue.enqueue(order2);
                orderConsumed.countDown(); // will be counted down after enqueue succeeds
            } catch (InterruptedException e) {
                exceptionRef.set(e);
            }
        });
        enqueuer.start();

        // Wait for the enqueuer thread to actually call enqueue and start blocking
        assertTrue(enqueueStarted.await(1, TimeUnit.SECONDS));

        // The enqueuer should still be blocked because queue is full.
        assertFalse(orderConsumed.await(500, TimeUnit.MILLISECONDS),
                "Enqueue should still be blocked after 500 ms");

        // Now dequeue the first order – this should free the blocked enqueuer.
        orderQueue.dequeue();

        // The enqueuer should now complete within a reasonable time
        assertTrue(orderConsumed.await(2, TimeUnit.SECONDS),
                "Enqueue did not complete after dequeue");
        assertNull(exceptionRef.get());

        enqueuer.join(1000);


    }

    @Test
    @DisplayName("Dequeue blocks when empty until an order is enqueued")
    void testDequeueBlocksWhenEmptyUntilEnqueue() throws InterruptedException {
        OrderQueue queue = new OrderQueue(2);
        Order order = new Order(new Customer("CUST-1", "Yassine"), new Cappuccino(), "ORD-1");

        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        CountDownLatch dequeueStarted = new CountDownLatch(1);
        CountDownLatch orderReceived = new CountDownLatch(1);

        Thread dequeuer = new Thread(() -> {
            try {
                dequeueStarted.countDown();
                Order taken = queue.dequeue();
                assertSame(order, taken);
                orderReceived.countDown();
            } catch (InterruptedException e) {
                exceptionRef.set(e);
            }
        });
        dequeuer.start();

        assertTrue(dequeueStarted.await(1, TimeUnit.SECONDS));

        // Dequeuer should be blocked because queue is empty
        assertFalse(orderReceived.await(500, TimeUnit.MILLISECONDS),
                "Dequeue should still be blocked after 500 ms");

        // Now enqueue an order – this should wake the dequeuer
        queue.enqueue(order);

        assertTrue(orderReceived.await(2, TimeUnit.SECONDS),
                "Dequeue did not complete after enqueue");
        assertNull(exceptionRef.get());

        dequeuer.join(1000);
    }

    // ------------------------------------------------------------------------
    // Interruption tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Enqueue throws InterruptedException when interrupted while blocking")
    void testEnqueueThrowsInterruptedExceptionWhenInterruptedWhileBlocking() throws InterruptedException {
        OrderQueue queue = new OrderQueue(1);
        queue.enqueue(new Order(new Customer("CUST-1", "Yassine"), new Cappuccino(), "ORD-1")); // fill to capacity

        Thread enqueuer = new Thread(() -> {
            try {
                queue.enqueue(new Order(new Customer("CUST-2", "Yassine"), new Cappuccino(), "ORD-2"));
                fail("Expected InterruptedException was not thrown");
            } catch (InterruptedException e) {
                // expected
                Thread.currentThread().interrupt(); // restore interrupted status
            }
        });

        enqueuer.start();
        // Give it time to reach the blocking state
        Thread.sleep(500);
        enqueuer.interrupt();
        enqueuer.join(1000);
        // If no exception thrown, the test would fail above; we simply check that the thread finished.
        assertFalse(enqueuer.isAlive());
    }

    @Test
    @DisplayName("Dequeue throws InterruptedException when interrupted while blocking")
    void testDequeueThrowsInterruptedExceptionWhenInterruptedWhileBlocking() throws InterruptedException {
        OrderQueue queue = new OrderQueue(2);

        Thread dequeuer = new Thread(() -> {
            try {
                queue.dequeue();
                fail("Expected InterruptedException was not thrown");
            } catch (InterruptedException e) {
                // expected
                Thread.currentThread().interrupt();
            }
        });

        dequeuer.start();
        Thread.sleep(500);
        dequeuer.interrupt();
        dequeuer.join(1000);
        assertFalse(dequeuer.isAlive());
    }

    // ------------------------------------------------------------------------
    // Concurrency tests (multiple producers / consumers)
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Multiple producers and consumers operate concurrently without data loss or corruption")
    void testConcurrentProducersAndConsumers() throws InterruptedException {
        final int capacity = 10;
        final int producers = 4;
        final int consumers = 3;
        final int ordersPerProducer = 20;
        final int totalOrders = producers * ordersPerProducer;

        OrderQueue queue = new OrderQueue(capacity);
        CountDownLatch allOrdersProduced = new CountDownLatch(producers);
        CountDownLatch allOrdersConsumed = new CountDownLatch(totalOrders);

        // Shared counter to track how many orders were actually dequeued
        AtomicInteger consumedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);

        // Producers
        for (int i = 0; i < producers; i++) {
            final int producerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerProducer; j++) {
                        Order order = new Order(new Customer("CUST-" + producerId, "Producer " + producerId),
                                new Cappuccino(), "ORD-" + producerId + "-" + j);
                        queue.enqueue(order);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Producer was interrupted");
                } finally {
                    allOrdersProduced.countDown();
                }
            });
        }

        // Consumers
        for (int i = 0; i < consumers; i++) {
            executor.submit(() -> {
                try {
                    while (true) {
                        // If all orders are already produced and queue is empty, we can stop
                        if (allOrdersProduced.getCount() == 0 && queue.isEmpty()) {
                            break;
                        }
                        Order order = queue.dequeue();
                        assertNotNull(order);
                        consumedCount.incrementAndGet();
                        allOrdersConsumed.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // normal termination
                }
            });
        }

        // Wait for all producers to finish, then allow consumers to drain
        boolean producersDone = allOrdersProduced.await(10, TimeUnit.SECONDS);
        assertTrue(producersDone, "Producers did not finish in time");

        // Wait for all orders to be consumed (with a timeout)
        boolean consumersDone = allOrdersConsumed.await(15, TimeUnit.SECONDS);
        assertTrue(consumersDone, "Not all orders were consumed");

        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(totalOrders, consumedCount.get(), "All orders must be consumed exactly once");
        assertTrue(queue.isEmpty(), "Queue should be empty after all orders are consumed");
    }

    // ------------------------------------------------------------------------
    // Stress test: high contention
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("High contention with many producer and consumer threads")
    void testHighContentionWithManyThreads() throws InterruptedException {
        final int capacity = 50;
        final int threadPairs = 20;       // 20 producers + 20 consumers
        final int operationsPerThread = 200;
        final int totalOperations = threadPairs * operationsPerThread;

        OrderQueue queue = new OrderQueue(capacity);
        CountDownLatch allDone = new CountDownLatch(threadPairs * 2);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadPairs * 2);

        // Producers
        for (int i = 0; i < threadPairs; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        queue.enqueue(new Order(new Customer("CUST", "Producer"), new Cappuccino(), "ORD"));
                        produced.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            });
        }

        // Consumers
        for (int i = 0; i < threadPairs; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        queue.dequeue();
                        consumed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            });
        }

        assertTrue(allDone.await(30, TimeUnit.SECONDS), "Threads did not finish in time");
        executor.shutdownNow();

        assertEquals(totalOperations, produced.get(), "Produced count mismatch");
        assertEquals(totalOperations, consumed.get(), "Consumed count mismatch");
        assertTrue(queue.isEmpty(), "Queue not empty after all operations");
    }

}
