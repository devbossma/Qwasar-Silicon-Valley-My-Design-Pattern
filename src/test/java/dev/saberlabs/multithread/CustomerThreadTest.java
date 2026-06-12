package dev.saberlabs.multithread;

import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.singleton.CoffeeShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomerThread --- Producer Thread")
class CustomerThreadTest {

    private List<CoffeeCreator> menu;
    private AtomicInteger idCounter;

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
        menu = List.of(
                new EspressoCreator(),
                new CappuccinoCreator(),
                new LatteCreator()
        );
        idCounter = new AtomicInteger(0);
    }

    private Customer createCustomer(String id, String name) {
        Customer customer = new Customer(id, name);
        CoffeeShop.getInstance().registerObserver(customer);
        return customer;
    }

    /** Stub ID generator --- no singleton dependency. */
    private CustomerThread.OrderIdGenerator stubIdGenerator() {
        return () -> "TEST-" + idCounter.incrementAndGet();
    }

    /** Captures orders into a list --- useful for assertions. */
    private List<Order> captureHandler(CustomerThread.OrderHandler... extra) {
        return Collections.synchronizedList(new ArrayList<>());
    }

    /** Builds a CustomerThread with a capturing handler and stub ID generator. */
    private CustomerThread build(Customer customer,
                                 List<Order> captured,
                                 List<CoffeeCreator> creators,
                                 int count) {
        return new CustomerThread(
                customer,
                captured::add,
                stubIdGenerator(),
                creators,
                count
        );
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor rejects null customer")
    void constructorRejectsNullCustomer() {
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerThread(
                        null,
                        order -> {},
                        stubIdGenerator(),
                        menu,
                        1));
    }

    @Test
    @DisplayName("Constructor rejects null OrderHandler")
    void constructorRejectsNullOrderHandler() {
        Customer alice = createCustomer("CUST-1", "Alice");
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerThread(
                        alice,
                        null,
                        stubIdGenerator(),
                        menu,
                        1));
    }

    @Test
    @DisplayName("Constructor rejects null OrderIdGenerator")
    void constructorRejectsNullIdGenerator() {
        Customer alice = createCustomer("CUST-1", "Alice");
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerThread(
                        alice,
                        order -> {},
                        null,
                        menu,
                        1));
    }

    @Test
    @DisplayName("Constructor rejects null creators list")
    void constructorRejectsNullCreators() {
        Customer alice = createCustomer("CUST-1", "Alice");
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerThread(
                        alice,
                        order -> {},
                        stubIdGenerator(),
                        null,
                        1));
    }

    @Test
    @DisplayName("Constructor rejects zero order count")
    void constructorRejectsZeroOrderCount() {
        Customer alice = createCustomer("CUST-1", "Alice");
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerThread(
                        alice,
                        order -> {},
                        stubIdGenerator(),
                        menu,
                        0));
    }

    @Test
    @DisplayName("Constructor rejects negative order count")
    void constructorRejectsNegativeOrderCount() {
        Customer alice = createCustomer("CUST-1", "Alice");
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerThread(
                        alice,
                        order -> {},
                        stubIdGenerator(),
                        menu,
                        -3));
    }

    @Test
    @DisplayName("getCustomer returns the correct customer")
    void getCustomerReturnsCorrectCustomer() {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = new ArrayList<>();
        CustomerThread ct = build(alice, captured, menu, 1);
        assertSame(alice, ct.getCustomer());
    }

    @Test
    @DisplayName("getNumberOfOrders returns the configured count")
    void getNumberOfOrdersReturnsConfiguredCount() {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = new ArrayList<>();
        CustomerThread ct = build(alice, captured, menu, 5);
        assertEquals(5, ct.getNumberOfOrders());
    }

    // -----------------------------------------------------------------------
    // Order placement
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Places the exact number of configured orders")
    void placesExactNumberOfOrders() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CustomerThread ct = build(alice, captured, menu, 3);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(10000);

        assertEquals(3, captured.size());
    }

    @Test
    @DisplayName("Places a single order correctly")
    void placesSingleOrder() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CustomerThread ct = build(alice, captured, menu, 1);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(5000);

        assertEquals(1, captured.size());
        Order order = captured.get(0);
        assertSame(alice, order.getCustomer());
        assertNotNull(order.getCoffee());
        assertTrue(order.getCoffee().getCost() > 0);
    }

    @Test
    @DisplayName("All orders belong to the correct customer")
    void allOrdersBelongToCorrectCustomer() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CustomerThread ct = build(alice, captured, menu, 5);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(15000);

        assertEquals(5, captured.size());
        captured.forEach(order -> assertSame(alice, order.getCustomer()));
    }

    @Test
    @DisplayName("Orders have valid coffee with positive cost")
    void ordersHaveValidCoffee() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CustomerThread ct = build(alice, captured, menu, 3);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(10000);

        captured.forEach(order -> {
            assertNotNull(order.getCoffee().getDescription());
            assertTrue(order.getCoffee().getCost() >= 2.50,
                    "Cost should be at least base espresso price");
            assertNotNull(order.getOrderId());
        });
    }

    @Test
    @DisplayName("Order IDs are generated by the injected generator")
    void orderIdsComesFromInjectedGenerator() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CustomerThread ct = build(alice, captured,
                List.of(new EspressoCreator()), 3);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(10000);

        // All IDs should use the stub generator format
        captured.forEach(order ->
                assertTrue(order.getOrderId().startsWith("TEST-"),
                        "ID should come from stub generator, got: " + order.getOrderId()));
    }

    @Test
    @DisplayName("Order price reflects customer loyalty tier")
    void orderPriceReflectsLoyaltyTier() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        // Bring Alice to Gold tier
        for (int i = 0; i < 11; i++) {
            alice.incrementOrders();
        }

        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CustomerThread ct = new CustomerThread(
                alice,
                order -> captured.add(order),
                stubIdGenerator(),
                List.of(new EspressoCreator()), // predictable pricing
                1);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(5000);

        // Gold: 20% off --- espresso base $2.50, max with all extras = $4.00
        // 20% off $4.00 = $3.20 max
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getFinalPrice() <= 3.20,
                "Gold member price should reflect 20% discount");
    }

    // -----------------------------------------------------------------------
    // OrderHandler is called correctly
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("OrderHandler is called exactly once per order")
    void orderHandlerCalledExactlyOncePerOrder() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        AtomicInteger handlerCallCount = new AtomicInteger(0);

        CustomerThread ct = new CustomerThread(
                alice,
                order -> handlerCallCount.incrementAndGet(),
                stubIdGenerator(),
                List.of(new EspressoCreator()),
                4);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(10000);

        assertEquals(4, handlerCallCount.get(),
                "Handler must be called exactly once per order placed");
    }

    @Test
    @DisplayName("OrderHandler receives orders with correct customer reference")
    void orderHandlerReceivesCorrectCustomer() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());

        CustomerThread ct = new CustomerThread(
                alice,
                order -> captured.add(order),
                stubIdGenerator(),
                List.of(new EspressoCreator()),
                3);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(10000);

        captured.forEach(order ->
                assertSame(alice, order.getCustomer(),
                        "Each order must reference the correct customer"));
    }

    @Test
    @DisplayName("OrderHandler interruptedException propagates correctly")
    void orderHandlerInterruptedExceptionPropagates() throws InterruptedException {
        Customer alice = createCustomer("CUST-1", "Alice");
        AtomicInteger handlerCalls = new AtomicInteger(0);

        CustomerThread ct = new CustomerThread(
                alice,
                order -> {
                    handlerCalls.incrementAndGet();
                    throw new InterruptedException("Simulated interruption");
                },
                stubIdGenerator(),
                List.of(new EspressoCreator()),
                5);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();
        thread.join(5000);

        assertFalse(thread.isAlive());
        // Only one order handled --- interrupted after first
        assertEquals(1, handlerCalls.get(),
                "Thread should stop after handler throws InterruptedException");
    }

    // -----------------------------------------------------------------------
    // Blocking behaviour --- with OrderQueue as handler
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Customer blocks when queue handler is full and resumes when space opens")
    void blocksOnFullQueueHandler() throws InterruptedException {
        OrderQueue smallQueue = new OrderQueue(2);
        Customer alice = createCustomer("CUST-1", "Alice");

        CustomerThread ct = new CustomerThread(
                alice,
                order -> smallQueue.enqueue(order), // queue as handler
                stubIdGenerator(),
                List.of(new EspressoCreator()),
                5);

        Thread thread = new Thread(ct, "Customer-Alice");
        thread.start();

        // Let producer fill the queue
        Thread.sleep(2000);
        assertEquals(2, smallQueue.size(), "Queue should be at capacity");

        // Drain one --- producer should unblock and refill
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
    @DisplayName("CustomerThread stops gracefully on thread interruption")
    void stopsGracefullyOnInterruption() throws InterruptedException {
        OrderQueue tinyQueue = new OrderQueue(1);
        Customer alice = createCustomer("CUST-1", "Alice");

        CustomerThread ct = new CustomerThread(
                alice,
                order -> tinyQueue.enqueue(order),
                stubIdGenerator(),
                List.of(new EspressoCreator()),
                100);

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
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch allDone = new CountDownLatch(customerCount);

        for (int i = 0; i < customerCount; i++) {
            Customer customer = createCustomer("CUST-" + i, "Customer-" + i);
            CustomerThread ct = new CustomerThread(
                    customer,
                    order -> captured.add(order),
                    stubIdGenerator(),
                    menu,
                    ordersEach);

            new Thread(() -> {
                ct.run();
                allDone.countDown();
            }, "Customer-" + customer.getName()).start();
        }

        boolean completed = allDone.await(20, TimeUnit.SECONDS);
        assertTrue(completed, "Not all customers finished in time");
        assertEquals(customerCount * ordersEach, captured.size(),
                "Total captured orders should match total placed");
    }

    @Test
    @DisplayName("Concurrent customers produce unique order IDs")
    void concurrentCustomersProduceUniqueOrderIds() throws InterruptedException {
        int customerCount = 4;
        int ordersEach = 5;
        List<Order> captured = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch allDone = new CountDownLatch(customerCount);
        AtomicInteger sharedIdCounter = new AtomicInteger(0);

        for (int i = 0; i < customerCount; i++) {
            Customer customer = createCustomer("CUST-" + i, "Customer-" + i);
            CustomerThread ct = new CustomerThread(
                    customer,
                    order -> captured.add(order),
                    // Shared atomic counter --- simulates CoffeeShop.nextOrderId()
                    () -> "ORD-" + sharedIdCounter.incrementAndGet(),
                    menu,
                    ordersEach);

            new Thread(() -> {
                ct.run();
                allDone.countDown();
            }).start();
        }

        boolean completed = allDone.await(20, TimeUnit.SECONDS);
        assertTrue(completed, "Not all customers finished in time");

        long uniqueCount = captured.stream()
                .map(Order::getOrderId)
                .distinct()
                .count();
        assertEquals(captured.size(), uniqueCount,
                "All order IDs must be unique across concurrent customers");
    }
}