package dev.saberlabs.singleton;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.multithread.Barista;
import dev.saberlabs.multithread.OrderQueue;
import dev.saberlabs.observer.OrderNotificationService;
import dev.saberlabs.observer.OrderObserver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pattern 1: SINGLETON
 * Ensures only one instance of CoffeeShop exists and provides a global access point to it.
 * The CoffeeShop is the single point of access for managing orders.
 * Uses Lazy initialization with double-checked locking with a private constructor.
 * *
 * Thread safety:
 * - Instance creation: double-checked locking with volatile INSTANCE
 * - Order list: Collections.synchronizedList + synchronized blocks
 * - ID generation: AtomicInteger counters
 * - Notification service: CopyOnWriteArraySet internally
 * - OrderQueue: thread-safe internally via ReentrantLock
 * - Barista thread management: synchronized lists
 */
public class CoffeeShop {

    // Volatile variable to ensure visibility of changes across threads and prevent instruction reordering issues.
    private static volatile CoffeeShop INSTANCE;

    /**
     * Thread-safe list to store orders.
     * Using Collections.synchronizedList to ensure that all operations on the list are thread-safe.
     */
    private final List<Order> orders = Collections.synchronizedList(new ArrayList<>());

    // AtomicInteger to generate unique IDs for orders and customers in a thread-safe manner
    private final AtomicInteger orderIdCounter = new AtomicInteger(0);
    private final AtomicInteger customerIdCounter = new AtomicInteger(0);

    // Shared notification service for all orders
    private final @NotNull OrderNotificationService notificationService = new OrderNotificationService();

    /**
     * Temporary work queue for barista threads (Producer-Consumer pattern).
     * Null until {@link #open(int, int)} is called.
     * Volatile --- written by main thread on open/close, read by customer/barista threads.
     */
    private volatile @Nullable OrderQueue orderQueue;

    /**
     * Active barista threads managed by the shop.
     * Synchronized lists --- baristas/threads are added/removed from the main thread
     * but read from multiple threads for shutdown coordination.
     */
    private final List<Barista> baristas = Collections.synchronizedList(new ArrayList<>());
    private final List<Thread> baristaThreads = Collections.synchronizedList(new ArrayList<>());

    // private constructor prevents external instantiation
    private CoffeeShop() { }

    // Provides global access to the singleton instance
    public static @NotNull CoffeeShop getInstance() {
        // Declaring a local variable to reduce the number of volatile reads
        CoffeeShop  coffeeShop = INSTANCE;

        // Check if the instance is null before synchronizing to improve performance
        if (coffeeShop == null) {
            // synchronize only the first time to create the instance
            synchronized (CoffeeShop.class) {
                // assign the instance to the local variable to minimize volatile reads
                coffeeShop = INSTANCE;
                // re-check to avoid multiple instantiations in multithreaded scenarios
                if (coffeeShop == null) {
                    // create the singleton instance if it doesn't exist
                    INSTANCE = coffeeShop = new CoffeeShop();
                }
            }
        }

        // return the singleton instance
        return coffeeShop;
    }

    // ================================================================
    // Shop Lifecycle (Multithreaded Mode)
    // ================================================================

    /**
     * Opens the shop for multithreaded operation.
     * Creates the OrderQueue and starts the specified number of Barista threads.
     *
     * @param queueCapacity    maximum pending orders the queue can hold, must be > 0
     * @param numberOfBaristas number of concurrent barista threads to start, must be > 0
     * @throws IllegalArgumentException if either argument is zero or negative
     */
    public void open(int queueCapacity, int numberOfBaristas) {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be > 0");
        }
        if (numberOfBaristas <= 0) {
            throw new IllegalArgumentException("Number of baristas must be > 0");
        }
        this.orderQueue = new OrderQueue(queueCapacity);

        System.out.printf("[CoffeeShop] Opening --- queue capacity: %d, baristas: %d%n",
                queueCapacity, numberOfBaristas);

        for (int i = 1; i <= numberOfBaristas; i++) {
            Barista barista = new Barista("Barista-" + i, Objects.requireNonNull(orderQueue));
            baristas.add(barista);

            Thread thread = new Thread(barista, "Barista-" + i);
            thread.setDaemon(false);
            baristaThreads.add(thread);
            thread.start();
        }

        System.out.println("[CoffeeShop] Shop is open!");
    }

    /**
     * Closes the shop gracefully.
     * Signals all baristas to stop, waits for them to drain the queue,
     * then joins all threads before returning.
     */
    public void close() {
        System.out.println("[CoffeeShop] Closing shop...");

        // Signal all baristas to stop accepting new work
        baristas.forEach(Barista::shutdown);

        // Interrupt baristas blocked on an empty queue
        baristaThreads.forEach(Thread::interrupt);

        // Wait for every barista thread to finish
        for (Thread thread : baristaThreads) {
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[CoffeeShop] All baristas finished.");
        System.out.printf("[CoffeeShop] Total orders served: %d%n", getOrderCount());
        baristas.forEach(b ->
                System.out.printf("[CoffeeShop] %s: %d orders%n",
                        b.getName(), b.getOrdersCompleted()));

        synchronized (baristas) { baristas.clear(); }
        synchronized (baristaThreads) { baristaThreads.clear(); }
        orderQueue = null;
    }

    // ================================================================
    // Observer Management
    // ================================================================

    /**
     * Registers an observer to receive notifications about order status changes.
     *
     * @param observer the observer to register, must not be null
     */
    public void registerObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        notificationService.registerObserver(observer);
    }

    /**
     * Removes an observer from receiving order status notifications.
     *
     * @param observer the observer to remove
     */
    public void removeObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        notificationService.removeObserver(observer);
    }

    // Expose the notification service for Order to use during status changes
    public @NotNull OrderNotificationService getNotificationService() {
        return notificationService;
    }

    // ================================================================
    // Order Management
    // ================================================================

    /**
     * Places an order for CLI / single-threaded use.
     * Adds to the permanent order list and sets status to [OrderStatus.PLACED].
     * Does NOT enqueue into the OrderQueue.
     *
     * @param order the order to place
     */
    public void placeOrder(@NotNull Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        synchronized (orders) {
            orders.add(order);
        }
        order.setStatus(OrderStatus.PLACED);
        System.out.println("[CoffeeShop] New Order Placed: " + order);
    }

    /**
     * Places an order for multithreaded use.
     * Adds to the permanent order list, sets status to PLACED,
     * then enqueues into the OrderQueue for barista processing.
     * Blocks if the queue is at capacity.
     *
     * @param order the order to enqueue
     * @throws InterruptedException  if the thread is interrupted while waiting
     * @throws IllegalStateException if the shop has not been opened yet
     */
    public void enqueueOrder(@NotNull Order order) throws InterruptedException {
        Objects.requireNonNull(order, "Order cannot be null");
        if (orderQueue == null) {
            throw new IllegalStateException(
                    "Shop is not open. Call open() before enqueueOrder().");
        }
        synchronized (orders) {
            orders.add(order);
        }
        order.setStatus(OrderStatus.PLACED);
        Objects.requireNonNull(orderQueue).enqueue(order);
        System.out.println("[CoffeeShop] Order enqueued: " + order);
    }

    // ================================================================
    // Queries
    // ================================================================

    /**
     * Returns a copy of the current list of orders to prevent external modification.
     * The method is synchronized to ensure thread safety when accessing the orders list.
     * @return a list of current orders
     */
    public @NotNull List<Order> getOrders() {
        synchronized (orders) {
            return List.copyOf(orders);
        }
    }

    // Method to get the count of current orders
    public int getOrderCount() {
        synchronized (orders) {
            return orders.size();
        }
    }

    /**
     * Returns the active order queue, or null if the shop is not open.
     *
     * @return the OrderQueue, or null before open() is called
     */
    public @Nullable OrderQueue getOrderQueue() {
        return orderQueue;
    }

    /**
     * Returns an unmodifiable snapshot of the active baristas.
     *
     * @return list of baristas currently on duty
     */
    public @NotNull List<Barista> getBaristas() {
        synchronized (baristas) {
            return List.copyOf(baristas);
        }
    }


    // ================================================================
    // ID Generation (Thread-Safe)
    // ================================================================

    /**
     * Generates the next unique Order ID.
     *
     * @return the next Order ID
     */
    public @NotNull String nextOrderId() {
        return "ORD-" + orderIdCounter.incrementAndGet();
    }

    /**
     * Generates the next unique customer ID.
     * @return the next customer ID
     */
    public @NotNull String nextCustomerId() {
        return "CUST-" + customerIdCounter.incrementAndGet();
    }

    // ================================================================
    // Test / Reset Support
    // ================================================================

    /**
     * Clears all orders from the coffee shop.
     * This method is primarily for testing purposes to reset the state of the coffee shop.
     * The method is synchronized to ensure thread safety when modifying the orders list and resetting counters.
     */
    public void clearOrders() {
        synchronized (orders) {
            orders.clear();
        }
        notificationService.clearObservers();
        orderIdCounter.set(0);
        customerIdCounter.set(0);
    }
}
