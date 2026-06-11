package dev.saberlabs.multithread;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Barista -- Consumer Thread")
class BaristaTest {

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    private Order createOrder(String customerId, String customerName) {
        Customer customer = new Customer(customerId, customerName);
        CoffeeShop.getInstance().registerObserver(customer);
        return new Order(customer, new Espresso(),
                CoffeeShop.getInstance().nextOrderId());
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor throws on null name")
    void constructorRejectsNullName() {
        OrderQueue queue = new OrderQueue(5);
        assertThrows(IllegalArgumentException.class,
                () -> new Barista(null, queue));
    }

    @Test
    @DisplayName("Constructor throws on null queue")
    void constructorRejectsNullQueue() {
        assertThrows(IllegalArgumentException.class,
                () -> new Barista("Barista-1", null));
    }

    @Test
    @DisplayName("getName returns correct name")
    void getNameReturnsCorrectName() {
        Barista barista = new Barista("Barista-X", new OrderQueue(5));
        assertEquals("Barista-X", barista.getName());
    }

    @Test
    @DisplayName("Initial state: running=true, ordersCompleted=0")
    void initialState() {
        Barista barista = new Barista("B", new OrderQueue(5));
        assertTrue(barista.isRunning());
        assertEquals(0, barista.getOrdersCompleted());
    }

    // -----------------------------------------------------------------------
    // Shutdown
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("shutdown() sets running to false")
    void shutdownSetsRunningFalse() {
        Barista barista = new Barista("B", new OrderQueue(5));
        barista.shutdown();
        assertFalse(barista.isRunning());
    }

    // -----------------------------------------------------------------------
    // Order processing
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Barista processes one order and sets status to FULFILLED")
    void processesOneOrder() throws InterruptedException {

        // Set up a queue and add one order
        OrderQueue queue = new OrderQueue(5);
        Order order = createOrder("CUST-1", "Alice");
        queue.enqueue(order);

        // Start the barista thread to process the order
        Barista barista = new Barista("Barista-1", queue);
        Thread thread = new Thread(barista, "Barista-1");
        thread.start();

        // Wait for processing -- preparation + sleep takes up to 5 seconds
        Thread.sleep(5000);

        // Signal shutdown and wait for thread to finish
        barista.shutdown();
        thread.interrupt();
        thread.join(2000);

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
        assertEquals(1, barista.getOrdersCompleted());
    }

    @Test
    @DisplayName("Barista increments customer loyalty on FULFILLED")
    void incrementsCustomerLoyalty() throws InterruptedException {
        // Set up a queue and add one order for Alice
        OrderQueue queue = new OrderQueue(5);
        Customer alice = new Customer("CUST-1", "Alice");
        CoffeeShop.getInstance().registerObserver(alice);
        Order order = new Order(alice, new Espresso(),
                CoffeeShop.getInstance().nextOrderId());
        queue.enqueue(order);

        Barista barista = new Barista("Barista-1", queue);
        Thread thread = new Thread(barista, "Barista-1");
        thread.start();

        Thread.sleep(5000);

        barista.shutdown();
        thread.interrupt();
        thread.join(2000);

        assertEquals(1, alice.getTotalOrders());
    }

    @Test
    @DisplayName("Barista processes multiple orders sequentially")
    void processesMultipleOrders() throws InterruptedException {
        OrderQueue queue = new OrderQueue(5);
        Order o1 = createOrder("CUST-1", "Alice");
        Order o2 = createOrder("CUST-2", "Bob");
        Order o3 = createOrder("CUST-3", "Charlie");
        queue.enqueue(o1);
        queue.enqueue(o2);
        queue.enqueue(o3);

        Barista barista = new Barista("Barista-1", queue);
        Thread thread = new Thread(barista, "Barista-1");
        thread.start();

        // 3 orders × up to 3 seconds each + buffer
        Thread.sleep(12000);

        barista.shutdown();
        thread.interrupt();
        thread.join(2000);

        assertEquals(3, barista.getOrdersCompleted());
        assertEquals(OrderStatus.FULFILLED, o1.getStatus());
        assertEquals(OrderStatus.FULFILLED, o2.getStatus());
        assertEquals(OrderStatus.FULFILLED, o3.getStatus());
    }

    // -----------------------------------------------------------------------
    // Blocking behaviour
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Barista blocks on empty queue and resumes when order arrives")
    void blocksOnEmptyQueueResumesOnEnqueue() throws InterruptedException {
        OrderQueue queue = new OrderQueue(5);
        Order order = createOrder("CUST-1", "Alice");
        CountDownLatch orderProcessed = new CountDownLatch(1);

        Barista barista = new Barista("Barista-1", queue) {
            // We can't easily intercept, so we check ordersCompleted after the fact
        };
        Thread thread = new Thread(() -> {
            barista.run();
            orderProcessed.countDown();
        }, "Barista-1");
        thread.start();

        // Barista is blocked -- no orders yet
        Thread.sleep(1000);
        assertEquals(0, barista.getOrdersCompleted());

        // Add an order -- barista should wake up
        queue.enqueue(order);

        Thread.sleep(5000);

        barista.shutdown();
        thread.interrupt();
        thread.join(2000);

        assertEquals(1, barista.getOrdersCompleted());
    }

    // -----------------------------------------------------------------------
    // Graceful shutdown -- drain remaining orders
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Barista drains queue after shutdown() before exiting")
    void drainsQueueAfterShutdown() throws InterruptedException {
        OrderQueue queue = new OrderQueue(5);
        queue.enqueue(createOrder("CUST-1", "Alice"));
        queue.enqueue(createOrder("CUST-2", "Bob"));

        Barista barista = new Barista("Barista-1", queue);
        Thread thread = new Thread(barista, "Barista-1");
        thread.start();

        // Signal shutdown immediately -- barista should still drain
        barista.shutdown();

        thread.join(15000);

        assertFalse(thread.isAlive());
        assertEquals(2, barista.getOrdersCompleted());
        assertTrue(queue.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Interruption
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Barista stops when interrupted while waiting on empty queue")
    void stopsOnInterruptWhileBlocking() throws InterruptedException {
        OrderQueue queue = new OrderQueue(5);
        Barista barista = new Barista("Barista-1", queue);
        Thread thread = new Thread(barista, "Barista-1");
        thread.start();

        // Let the barista block on empty queue
        Thread.sleep(500);
        thread.interrupt();
        thread.join(2000);

        assertFalse(thread.isAlive());
        assertEquals(0, barista.getOrdersCompleted());
    }

    // -----------------------------------------------------------------------
    // Multiple baristas sharing one queue
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Two baristas share work from the same queue without double-processing")
    void twoBaristasShareWork() throws InterruptedException {
        OrderQueue queue = new OrderQueue(10);
        int orderCount = 6;

        for (int i = 0; i < orderCount; i++) {
            queue.enqueue(createOrder("CUST-" + i, "Customer-" + i));
        }

        Barista b1 = new Barista("Barista-1", queue);
        Barista b2 = new Barista("Barista-2", queue);
        Thread t1 = new Thread(b1, "Barista-1");
        Thread t2 = new Thread(b2, "Barista-2");
        t1.start();
        t2.start();

        // Wait for all orders -- 6 × 3 seconds + buffer
        Thread.sleep(20000);

        b1.shutdown();
        b2.shutdown();
        t1.interrupt();
        t2.interrupt();
        t1.join(2000);
        t2.join(2000);

        int total = b1.getOrdersCompleted() + b2.getOrdersCompleted();
        assertEquals(orderCount, total, "Total completed should equal total enqueued");
        assertTrue(b1.getOrdersCompleted() > 0, "Barista-1 should have processed some orders");
        assertTrue(b2.getOrdersCompleted() > 0, "Barista-2 should have processed some orders");
        assertTrue(queue.isEmpty());
    }
}
