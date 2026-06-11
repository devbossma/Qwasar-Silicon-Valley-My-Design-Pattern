package dev.saberlabs.multithread;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Barista — Consumer thread in the Producer-Consumer pattern.
 *
 * Each Barista runs in its own thread, continuously pulling orders
 * from the shared {@link OrderQueue}, preparing the coffee via
 * the Template Method pattern ({@code coffee.getPreparation().prepareCoffee()}),
 * and updating the order status — which triggers Observer notifications
 * and loyalty tier recalculation.
 *
 * Shutdown contract:
 * - Call {@link #shutdown()} to signal the barista to stop.
 * - The barista finishes all orders already in the queue before exiting
 *   (graceful drain — no orders are lost).
 * - If blocked on an empty queue, interrupt the thread to unblock it.
 *
 * Thread safety:
 * - {@code running} is {@code volatile} — the shutdown signal is visible
 *   immediately across threads without synchronization.
 * - {@code ordersCompleted} is only written by this barista's own thread,
 *   so no synchronization is needed.
 */
public class Barista implements Runnable {

    @NotNull private final String name;
    @NotNull private final OrderQueue orderQueue;

    /**
     * Volatile ensures the shutdown flag written by the main thread
     * is immediately visible to this barista's thread.
     */
    private volatile boolean running = true;
    private int ordersCompleted = 0;

    /**
     * Creates a new Barista.
     *
     * @param name       display name used in logs (e.g. "Barista-1")
     * @param orderQueue the shared queue to pull orders from
     */
    public Barista(@NotNull String name, @NotNull OrderQueue orderQueue) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.orderQueue = Objects.requireNonNull(orderQueue, "Order queue cannot be null");
    }

    @Override
    public void run() {
        System.out.printf("[%s] On duty — ready to prepare orders.%n", name);

        // Keep running while active OR while there are orders left to drain
        while (running || !orderQueue.isEmpty()) {
            try {
                // CONSUMER — blocks here if queue is empty
                Order order = orderQueue.dequeue();

                // TEMPLATE METHOD — runs the coffee-specific preparation steps
                System.out.printf("[%s] Preparing: %s for %s...%n",
                        name,
                        order.getCoffee().getDescription(),
                        order.getCustomer().getName());
                order.getCoffee().getPreparation().prepareCoffee();

                // Simulate realistic preparation time (1–3 seconds)
                Thread.sleep((long) (Math.random() * 2000) + 1000);

                // OBSERVER — setStatus triggers notifyObservers via CoffeeShop
                order.setStatus(OrderStatus.READY);

                // STRATEGY + OBSERVER — FULFILLED increments loyalty tier
                order.setStatus(OrderStatus.FULFILLED);

                ordersCompleted++;
                System.out.printf("[%s] ✓ Completed order for %s (%d total)%n",
                        name,
                        order.getCustomer().getName(),
                        ordersCompleted);

            } catch (InterruptedException e) {
                // Restore interrupted status and exit gracefully
                Thread.currentThread().interrupt();
                System.out.printf("[%s] Interrupted — shutting down.%n", name);
                break;
            }
        }

        System.out.printf("[%s] Shift over — completed %d orders.%n",
                name, ordersCompleted);
    }

    /**
     * Signals this barista to stop after draining remaining orders.
     * Call this before interrupting the thread for a clean shutdown.
     */
    public void shutdown() {
        running = false;
    }

    public @NotNull String getName() { return name; }
    public int getOrdersCompleted() { return ordersCompleted; }
    public boolean isRunning() { return running; }
}
