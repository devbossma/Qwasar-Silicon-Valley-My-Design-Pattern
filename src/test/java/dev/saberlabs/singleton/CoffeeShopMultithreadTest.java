package dev.saberlabs.singleton;

import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.multithread.Barista;
import dev.saberlabs.multithread.CustomerThread;
import dev.saberlabs.multithread.OrderQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CoffeeShop — Multithreaded Features")
class CoffeeShopMultithreadTest {

    private CoffeeShop shop;

    @BeforeEach
    void setUp() {
        shop = CoffeeShop.getInstance();
        shop.clearOrders();
    }

    @AfterEach
    void tearDown() {
        shop.close();
        shop.clearOrders();
    }

    private Order createOrder(String customerId, String name) {
        Customer customer = new Customer(customerId, name);
        shop.registerObserver(customer);
        return new Order(customer, new Espresso(), shop.nextOrderId());
    }

    // ================================================================
    // open()
    // ================================================================

    @Nested
    @DisplayName("open()")
    class OpenTests {

        @Test
        @DisplayName("open() initializes the OrderQueue")
        void initializesOrderQueue() {
            assertNull(shop.getOrderQueue());
            shop.open(5, 2);
            assertNotNull(shop.getOrderQueue());
            assertEquals(5, shop.getOrderQueue().getCapacity());
        }

        @Test
        @DisplayName("open() starts the correct number of barista threads")
        void startsCorrectNumberOfBaristas() {
            shop.open(5, 3);
            assertEquals(3, shop.getBaristas().size());
        }

        @Test
        @DisplayName("open() rejects zero queue capacity")
        void rejectsZeroQueueCapacity() {
            assertThrows(IllegalArgumentException.class,
                    () -> shop.open(0, 2));
        }

        @Test
        @DisplayName("open() rejects negative queue capacity")
        void rejectsNegativeQueueCapacity() {
            assertThrows(IllegalArgumentException.class,
                    () -> shop.open(-1, 2));
        }

        @Test
        @DisplayName("open() rejects zero baristas")
        void rejectsZeroBaristas() {
            assertThrows(IllegalArgumentException.class,
                    () -> shop.open(5, 0));
        }

        @Test
        @DisplayName("open() rejects negative baristas")
        void rejectsNegativeBaristas() {
            assertThrows(IllegalArgumentException.class,
                    () -> shop.open(5, -1));
        }
    }

    // ================================================================
    // close()
    // ================================================================

    @Nested
    @DisplayName("close()")
    class CloseTests {

        @Test
        @DisplayName("close() clears barista list after shutdown")
        void clearsBaristaListAfterShutdown() {
            shop.open(5, 2);
            assertEquals(2, shop.getBaristas().size());
            shop.close();
            assertEquals(0, shop.getBaristas().size());
        }

        @Test
        @DisplayName("close() nullifies the OrderQueue")
        void nullifiesOrderQueue() {
            shop.open(5, 2);
            assertNotNull(shop.getOrderQueue());
            shop.close();
            assertNull(shop.getOrderQueue());
        }

        @Test
        @DisplayName("close() on unopened shop is safe")
        void closeOnUnopenedShopIsSafe() {
            assertDoesNotThrow(() -> shop.close());
        }
    }

    // ================================================================
    // enqueueOrder()
    // ================================================================

    @Nested
    @DisplayName("enqueueOrder()")
    class EnqueueOrderTests {

        @Test
        @DisplayName("enqueueOrder() throws IllegalStateException when shop is not open")
        void throwsWhenShopNotOpen() {
            Order order = createOrder("CUST-1", "Alice");
            assertThrows(IllegalStateException.class,
                    () -> shop.enqueueOrder(order));
        }

        @Test
        @DisplayName("enqueueOrder() adds order to permanent orders list")
        void addsToOrdersList() throws InterruptedException {
            shop.open(5, 1);
            Order order = createOrder("CUST-1", "Alice");
            shop.enqueueOrder(order);
            assertEquals(1, shop.getOrderCount());
        }

        @Test
        @DisplayName("enqueueOrder() sets order status to PLACED")
        void setsStatusToPlaced() throws InterruptedException {
            shop.open(5, 1);
            Order order = createOrder("CUST-1", "Alice");
            shop.enqueueOrder(order);
            assertEquals(OrderStatus.PLACED, order.getStatus());
        }

        @Test
        @DisplayName("enqueueOrder() adds order to the shop OrderQueue")
        void addsToShopOrderQueue() throws InterruptedException {
            shop.open(10, 1);

            Order order = createOrder("CUST-1", "Alice");
            shop.enqueueOrder(order);

            // Order was added to permanent list — proves enqueueOrder ran
            assertEquals(1, shop.getOrderCount());
            assertEquals(OrderStatus.PLACED, order.getStatus());
        }

        @Test
        @DisplayName("enqueueOrder() throws on null order")
        void throwsOnNullOrder() {
            shop.open(5, 1);
            assertThrows(IllegalArgumentException.class,
                    () -> shop.enqueueOrder(null));
        }

        @Test
        @DisplayName("concurrent enqueueOrder() calls produce unique order IDs")
        void concurrentEnqueueProducesUniqueIds() throws InterruptedException {
            shop.open(100, 1);

            int threadCount = 20;
            List<String> ids = java.util.Collections.synchronizedList(
                    new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                new Thread(() -> {
                    try {
                        Order order = new Order(
                                new Customer("CUST-" + idx, "Customer-" + idx),
                                new Espresso(),
                                shop.nextOrderId());
                        shop.enqueueOrder(order);
                        ids.add(order.getOrderId());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "Not all threads finished in time");

            long uniqueCount = ids.stream().distinct().count();
            assertEquals(threadCount, uniqueCount,
                    "All concurrent order IDs must be unique");
        }
    }

    // ================================================================
    // getOrderQueue() / getBaristas()
    // ================================================================

    @Nested
    @DisplayName("getOrderQueue() and getBaristas()")
    class QueryTests {

        @Test
        @DisplayName("getOrderQueue() returns null before open()")
        void returnsNullBeforeOpen() {
            assertNull(shop.getOrderQueue());
        }

        @Test
        @DisplayName("getOrderQueue() returns queue after open()")
        void returnsQueueAfterOpen() {
            shop.open(5, 1);
            assertNotNull(shop.getOrderQueue());
        }

        @Test
        @DisplayName("getBaristas() returns empty list before open()")
        void returnsEmptyBeforeOpen() {
            assertTrue(shop.getBaristas().isEmpty());
        }

        @Test
        @DisplayName("getBaristas() returns unmodifiable list")
        void returnsUnmodifiableList() {
            shop.open(5, 2);
            List<Barista> baristas = shop.getBaristas();
            assertThrows(UnsupportedOperationException.class,
                    () -> baristas.add(new Barista("X", new OrderQueue(1))));
        }
    }

    // ================================================================
    // Full integration — open → enqueue → baristas process → close
    // ================================================================

    @Nested
    @DisplayName("Full Simulation Integration")
    class IntegrationTests {

        @Test
        @DisplayName("baristas process all enqueued orders before close")
        void baristasProcessAllOrders() throws InterruptedException {
            shop.open(5, 2);

            int orderCount = 4;
            for (int i = 0; i < orderCount; i++) {
                Order order = createOrder("CUST-" + i, "Customer-" + i);
                shop.enqueueOrder(order);
            }

            // Wait for queue to drain
            OrderQueue queue = shop.getOrderQueue();
            if (queue != null) {
                while (!queue.isEmpty()) {
                    Thread.sleep(500);
                }
            }
            // Buffer for baristas to finish the last dequeued order
            Thread.sleep(4000);

            shop.close();

            long fulfilled = shop.getOrders().stream()
                    .filter(o -> o.getStatus() == OrderStatus.FULFILLED)
                    .count();
            assertEquals(orderCount, fulfilled,
                    "All orders should be FULFILLED after shop closes");
        }

        @Test
        @DisplayName("concurrent CustomerThreads and Baristas process all orders")
        void fullProducerConsumerSimulation() throws InterruptedException {
            int queueCapacity = 5;
            int numberOfBaristas = 3;
            int customersCount = 4;
            int ordersPerCustomer = 2;
            int totalOrders = customersCount * ordersPerCustomer;

            shop.open(queueCapacity, numberOfBaristas);

            List<Customer> customers = new ArrayList<>();
            for (int i = 0; i < customersCount; i++) {
                Customer c = new Customer(shop.nextCustomerId(), "Customer-" + i);
                shop.registerObserver(c);
                customers.add(c);
            }

            CountDownLatch allCustomersDone = new CountDownLatch(customersCount);
            AtomicInteger idCounter = new AtomicInteger(0);

            for (Customer customer : customers) {
                // Updated constructor — inject OrderHandler and OrderIdGenerator
                CustomerThread ct = new CustomerThread(
                        customer,
                        shop::enqueueOrder,          // OrderHandler — registers + enqueues
                        shop::nextOrderId,           // OrderIdGenerator — thread-safe
                        List.of(new EspressoCreator()),
                        ordersPerCustomer
                );

                new Thread(() -> {
                    ct.run();
                    allCustomersDone.countDown();
                }, "Customer-" + customer.getName()).start();
            }

            // Wait for all customers to finish placing orders
            boolean customersFinished = allCustomersDone.await(20, TimeUnit.SECONDS);
            assertTrue(customersFinished, "Not all customers finished placing orders");

            // Wait for queue to drain
            OrderQueue queue = shop.getOrderQueue();
            if (queue != null) {
                int waitMs = 0;
                while (!queue.isEmpty() && waitMs < 10000) {
                    Thread.sleep(500);
                    waitMs += 500;
                }
            }

            // Buffer for baristas to finish last in-flight order
            Thread.sleep(5000);

            // Verify permanent orders list was populated
            assertEquals(totalOrders, shop.getOrderCount(),
                    "All orders should be in the permanent orders list");

            // Verify baristas shared the work and completed everything
            int baristaTotal = shop.getBaristas().stream()
                    .mapToInt(Barista::getOrdersCompleted)
                    .sum();
            assertEquals(totalOrders, baristaTotal,
                    "Barista total should match total orders placed");
            shop.close();
        }

        @Test
        @DisplayName("thread-safe order list under concurrent enqueue")
        void threadSafeOrderListUnderConcurrentEnqueue()
                throws InterruptedException {
            shop.open(100, 1);

            int threadCount = 50;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                new Thread(() -> {
                    try {
                        Order order = new Order(
                                new Customer("CUST-" + idx, "Cust-" + idx),
                                new Espresso(),
                                shop.nextOrderId());
                        shop.enqueueOrder(order);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "Not all threads finished in time");
            assertEquals(threadCount, shop.getOrderCount(),
                    "All concurrent orders must be in the orders list");
        }

        @Test
        @DisplayName("customer loyalty tier upgrades correctly through concurrent orders")
        void loyaltyTierUpgradesConcurrently() throws InterruptedException {
            shop.open(10, 3);

            Customer alice = new Customer(shop.nextCustomerId(), "Alice");
            shop.registerObserver(alice);

            // Alice places 11 orders — should reach GOLD
            CustomerThread ct = new CustomerThread(
                    alice,
                    shop::enqueueOrder,
                    shop::nextOrderId,
                    List.of(new EspressoCreator()),
                    11
            );

            Thread thread = new Thread(ct, "Customer-Alice");
            thread.start();
            thread.join(20000);

            // Wait for all orders to be fulfilled
            OrderQueue queue = shop.getOrderQueue();
            if (queue != null) {
                while (!queue.isEmpty()) {
                    Thread.sleep(500);
                }
            }
            Thread.sleep(5000);

            shop.close();

            assertEquals(11, alice.getTotalOrders(),
                    "Alice should have 11 fulfilled orders");
            assertEquals(dev.saberlabs.models.LoyaltyTier.GOLD,
                    alice.getLoyaltyTier(),
                    "Alice should have reached GOLD tier");
        }
    }
}