package dev.saberlabs.multithread;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * CustomerThread — Producer thread in the Producer-Consumer pattern.
 *
 * <p>Simulates a customer arriving at the coffee shop, picking a random
 * coffee type (Factory Method), optionally adding extras (Decorator),
 * building an Order (Strategy auto-prices from loyalty tier), and
 * submitting it via the injected {@link OrderHandler}.
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li>{@link OrderHandler} — where the order goes (queue, shop, test spy)
 *       is injected, not hardcoded. CustomerThread has no knowledge of
 *       CoffeeShop, OrderQueue, or any routing concern.</li>
 *   <li>{@link OrderIdGenerator} — ID generation is injected so the thread
 *       has no hidden dependency on the CoffeeShop singleton.</li>
 *   <li>CustomerThread stays in its domain: Customer, Coffee, Order.
 *       Everything external is explicit in the constructor.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * Each CustomerThread writes only to its own local variables.
 * The shared Customer model is thread-safe (AtomicInteger, volatile, synchronized).
 * OrderHandler and OrderIdGenerator implementations must be thread-safe
 * if shared across multiple CustomerThreads.
 */
public class CustomerThread implements Runnable {

    /**
     * Functional interface for submitting a completed Order.
     * Implementations may enqueue, register, persist, or spy on the order.
     */
    @FunctionalInterface
    public interface OrderHandler {
        /**
         * Handles a completed order.
         *
         * @param order the order to handle
         * @throws InterruptedException if the handler blocks and is interrupted
         */
        void handle(@NotNull Order order) throws InterruptedException;
    }

    /**
     * Functional interface for generating unique order IDs.
     * Decouples ID generation from the CoffeeShop singleton.
     */
    @FunctionalInterface
    public interface OrderIdGenerator {
        /**
         * Generates the next unique order ID.
         *
         * @return a unique, non-null order ID string
         */
        @NotNull String nextId();
    }

    @NotNull private final Customer customer;
    @NotNull private final OrderHandler orderHandler;
    @NotNull private final OrderIdGenerator idGenerator;
    @NotNull private final List<CoffeeCreator> availableCreators;
    private final int numberOfOrders;
    private final Random random = new Random();

    /**
     * Creates a new CustomerThread.
     *
     * @param customer          the customer placing orders (also an OrderObserver)
     * @param orderHandler      handles each completed Order — routing is external
     * @param idGenerator       generates unique order IDs — no singleton coupling
     * @param availableCreators the coffee menu to randomly pick from
     * @param numberOfOrders    how many orders this customer will place, must be > 0
     * @throws NullPointerException     if any required parameter is null
     * @throws IllegalArgumentException if numberOfOrders is zero or negative
     */
    public CustomerThread(
            @NotNull Customer customer,
            @NotNull OrderHandler orderHandler,
            @NotNull OrderIdGenerator idGenerator,
            @NotNull List<CoffeeCreator> availableCreators,
            int numberOfOrders) {
        this.customer = Objects.requireNonNull(customer, "Customer cannot be null");
        this.orderHandler = Objects.requireNonNull(orderHandler, "Order handler cannot be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator cannot be null");
        this.availableCreators = Objects.requireNonNull(availableCreators, "Creators cannot be null");
        if (numberOfOrders <= 0) {
            throw new IllegalArgumentException("Number of orders must be > 0, got: " + numberOfOrders);
        }
        this.numberOfOrders = numberOfOrders;
    }

    @Override
    public void run() {
        System.out.printf("[%s] Arrived at the shop — placing %d order(s).%n",
                customer.getName(), numberOfOrders);

        for (int i = 0; i < numberOfOrders; i++) {
            try {
                // FACTORY METHOD — pick a random coffee type from the menu
                CoffeeCreator creator = availableCreators
                        .get(random.nextInt(availableCreators.size()));
                Coffee coffee = creator.createCoffee();

                // DECORATOR — randomly apply extras
                coffee = applyRandomExtras(coffee);

                // STRATEGY — Order constructor auto-prices from customer's loyalty tier
                // ID generation is injected — no hidden singleton dependency
                Order order = new Order(customer, coffee, idGenerator.nextId());

                System.out.printf("[%s] Order %d/%d: %s — $%.2f (%s pricing)%n",
                        customer.getName(),
                        i + 1,
                        numberOfOrders,
                        coffee.getDescription(),
                        order.getFinalPrice(),
                        customer.getLoyaltyTier());

                // PRODUCER — delegate to injected handler (queue, shop, or test spy)
                orderHandler.handle(order);

                // Simulate think time between orders
                if (i < numberOfOrders - 1) {
                    Thread.sleep((long) (Math.random() * 1500) + 500);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s] Interrupted — leaving shop.%n",
                        customer.getName());
                break;
            }
        }

        System.out.printf("[%s] All %d orders placed — waiting for notifications.%n",
                customer.getName(), numberOfOrders);
    }

    /**
     * Randomly applies 0–3 decorator extras to the coffee.
     * Each extra has an independent 50% chance of being applied.
     *
     * @param coffee the base coffee from the factory
     * @return the coffee with zero or more decorators applied
     */
    private @NotNull Coffee applyRandomExtras(@NotNull Coffee coffee) {
        if (random.nextBoolean()) coffee = new MilkDecorator(coffee);
        if (random.nextBoolean()) coffee = new SugarDecorator(coffee);
        if (random.nextBoolean()) coffee = new WhippedCreamDecorator(coffee);
        return coffee;
    }

    public @NotNull Customer getCustomer() { return customer; }
    public int getNumberOfOrders() { return numberOfOrders; }
}