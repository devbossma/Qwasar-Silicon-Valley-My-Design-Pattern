package dev.saberlabs.multithread;

import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.singleton.CoffeeShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomerThread -- Producer Thread")
class CustomerThreadTest {

    private List<CoffeeCreator> menu;

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
        menu = List.of(
                new EspressoCreator(),
                new CappuccinoCreator(),
                new LatteCreator()
        );
    }

    private Customer createCustomer(String id, String name) {
        Customer customer = new Customer(id, name);
        CoffeeShop.getInstance().registerObserver(customer);
        return customer;
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor rejects null customer")
    void constructorRejectsNullCustomer() {
        OrderQueue queue = new OrderQueue(5);
        assertThrows(IllegalArgumentException.class,
                () -> new CustomerThread(null, queue, menu, 1));
    }

    @Test
    @DisplayName("Constructor rejects null queue")
    void constructorRejectsNullQueue() {
        Customer alice = createCustomer("CUST-1", "Alice");
        assertThrows(IllegalArgumentException.class,
                () -> new CustomerThread(alice, null, menu, 1));
    }

    @Test
    @DisplayName("Constructor rejects null creators list")
    void constructorRejectsNullCreators() {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(5);
        assertThrows(IllegalArgumentException.class,
                () -> new CustomerThread(alice, queue, null, 1));
    }

    @Test
    @DisplayName("Constructor rejects zero or negative order count")
    void constructorRejectsInvalidOrderCount() {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(5);
        assertThrows(IllegalArgumentException.class,
                () -> new CustomerThread(alice, queue, menu, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CustomerThread(alice, queue, menu, -3));
    }

    @Test
    @DisplayName("getCustomer returns the correct customer")
    void getCustomerReturnsCorrectCustomer() {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(5);
        CustomerThread ct = new CustomerThread(alice, queue, menu, 1);
        assertSame(alice, ct.getCustomer());
    }

    @Test
    @DisplayName("getNumberOfOrders returns the configured count")
    void getNumberOfOrdersReturnsConfiguredCount() {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(10);
        CustomerThread ct = new CustomerThread(alice, queue, menu, 5);
        assertEquals(5, ct.getNumberOfOrders());
    }

    // -----------------------------------------------------------------------
    // Order placement
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Places the exact number of configured orders")
    void placesExactNumberOfOrders() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(10);
        CustomerThread ct = new CustomerThread(alice, queue, menu, 3);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(10000);

        assertEquals(3, queue.size());
    }

    @Test
    @DisplayName("Places a single order correctly")
    void placesSingleOrder() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(5);
        CustomerThread ct = new CustomerThread(alice, queue, menu, 1);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(5000);

        assertEquals(1, queue.size());
        var order = queue.dequeue();
        assertSame(alice, order.getCustomer());
        assertNotNull(order.getCoffee());
        assertTrue(order.getCoffee().getCost() > 0);
    }

    @Test
    @DisplayName("All orders belong to the correct customer")
    void allOrdersBelongToCorrectCustomer() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(10);
        CustomerThread ct = new CustomerThread(alice, queue, menu, 5);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(15000);

        assertEquals(5, queue.size());
        for (int i = 0; i < 5; i++) {
            assertSame(alice, queue.dequeue().getCustomer());
        }
    }

    @Test
    @DisplayName("Orders have valid coffee with positive cost")
    void ordersHaveValidCoffee() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        OrderQueue queue = new OrderQueue(10);
        CustomerThread ct = new CustomerThread(alice, queue, menu, 3);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(10000);

        for (int i = 0; i < 3; i++) {
            var order = queue.dequeue();
            assertNotNull(order.getCoffee().getDescription());
            assertTrue(order.getCoffee().getCost() >= 2.50,
                    "Cost should be at least base espresso price");
            assertNotNull(order.getOrderId());
        }
    }

    @Test
    @DisplayName("Order price reflects customer loyalty tier")
    void orderPriceReflectsLoyaltyTier() throws InterruptedException {
        // Bring Alice to Gold tier first
        Customer alice = createCustomer("CUST-1", "Alice");
        for (int i = 0; i < 11; i++) {
            alice.incrementOrders();
        }

        OrderQueue queue = new OrderQueue(5);
        // Use only EspressoCreator for predictable pricing
        CustomerThread ct = new CustomerThread(alice, queue,
                List.of(new EspressoCreator()), 1);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(5000);

        // Gold: 20% off -- espresso base is $2.50 + optional extras
        var order = queue.dequeue();
        assertTrue(order.getFinalPrice() <= 2.50,
                "Gold member should pay at most base espresso price");
    }

    // -----------------------------------------------------------------------
    // Blocking behaviour
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Customer blocks when queue is full and resumes when space opens")
    void blocksOnFullQueue() throws InterruptedException {
        OrderQueue smallQueue = new OrderQueue(2);
        Customer alice = createCustomer("CUST-1", "Alice");
        CustomerThread ct = new CustomerThread(alice, smallQueue,
                List.of(new EspressoCreator()), 5);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();

        // Let producer fill the queue
        Thread.sleep(2000);
        assertEquals(2, smallQueue.size(), "Queue should be at capacity");

        // Drain one -- producer should be unblocked and refill
        smallQueue.dequeue();
        Thread.sleep(2000);
        assertTrue(smallQueue.size() > 0, "Producer should have resumed");

        // Drain everything so producer can finish
        while (thread.isAlive()) {
            if (!smallQueue.isEmpty()) smallQueue.dequeue();
            Thread.sleep(300);
        }
        thread.join(5000);
        assertFalse(thread.isAlive());
    }

    // -----------------------------------------------------------------------
    // Interruption
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("CustomerThread stops gracefully on interruption")
    void stopsGracefullyOnInterruption() throws InterruptedException {
        OrderQueue tinyQueue = new OrderQueue(1);
        Customer alice = createCustomer("CUST-1", "Alice");
        CustomerThread ct = new CustomerThread(alice, tinyQueue,
                List.of(new EspressoCreator()), 100);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();

        // Let it place one order and block on full queue
        Thread.sleep(1000);
        thread.interrupt();
        thread.join(3000);

        assertFalse(thread.isAlive());
    }

    // -----------------------------------------------------------------------
    // Multiple concurrent customers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Multiple customers place orders concurrently without data loss")
    void multipleConcurrentCustomers() throws InterruptedException {
        int customerCount = 5;
        int ordersEach = 3;
        OrderQueue queue = new OrderQueue(50);
        CountDownLatch allDone = new CountDownLatch(customerCount);

        for (int i = 0; i < customerCount; i++) {
            Customer customer = createCustomer("CUST-" + i, "Customer-" + i);
            CustomerThread ct = new CustomerThread(customer, queue, menu, ordersEach);
            new Thread(() -> {
                ct.run();
                allDone.countDown();
            }, "Customer-" + customer.getName()).start();
        }

        boolean completed = allDone.await(20, TimeUnit.SECONDS);
        assertTrue(completed, "Not all customers finished in time");
        assertEquals(customerCount * ordersEach, queue.size(),
                "Total orders in queue should match total placed");
    }

    @Test
    @DisplayName("Concurrent customers produce unique order IDs")
    void concurrentCustomersProduceUniqueOrderIds() throws InterruptedException {
        int customerCount = 4;
        int ordersEach = 5;
        OrderQueue queue = new OrderQueue(100);
        CountDownLatch allDone = new CountDownLatch(customerCount);

        for (int i = 0; i < customerCount; i++) {
            Customer customer = createCustomer("CUST-" + i, "Customer-" + i);
            CustomerThread ct = new CustomerThread(customer, queue, menu, ordersEach);
            new Thread(() -> {
                ct.run();
                allDone.countDown();
            }).start();
        }

        allDone.await(20, TimeUnit.SECONDS);

        // Collect all order IDs
        java.util.List<String> ids = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            ids.add(queue.dequeue().getOrderId());
        }

        long uniqueCount = ids.stream().distinct().count();
        assertEquals(ids.size(), uniqueCount,
                "All order IDs must be unique across concurrent customers");
    }
}