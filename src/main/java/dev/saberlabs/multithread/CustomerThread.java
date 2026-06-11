package dev.saberlabs.multithread;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * CustomerThread -- Producer thread in the Producer-Consumer pattern.
 *
 * Each customer runs in its own thread, placing one or more orders
 * into the shared {@link OrderQueue}. The customer:
 * - Picks a random coffee type using the Factory Method pattern
 * - Randomly applies Decorator extras (Milk, Sugar, WhippedCream)
 * - Creates an Order (Strategy auto-prices from loyalty tier)
 * - Enqueues the order (blocks if the queue is full)
 * - Simulates think time between orders via {@code Thread.sleep()}
 *
 * The Customer model object is kept separate from this threading wrapper --
 * Customer is a domain model and Observer; CustomerThread is the concurrency concern.
 *
 * Thread safety:
 * - Each CustomerThread writes only to its own local variables and the
 *   shared OrderQueue (which is thread-safe internally).
 * - The Customer model is shared but its mutable state (totalOrders,
 *   loyaltyTier) is protected by AtomicInteger and volatile/synchronized.
 */
public class CustomerThread implements Runnable {

    @NotNull private final Customer customer;
    @NotNull private final OrderQueue orderQueue;
    @NotNull private final List<CoffeeCreator> availableCreators;
    private final int numberOfOrders;
    private final Random random = new Random();

    /**
     * Creates a CustomerThread.
     *
     * @param customer          the customer domain object (also an OrderObserver)
     * @param orderQueue        the shared queue to place orders into
     * @param availableCreators coffee types available on the menu
     * @param numberOfOrders    how many orders this customer will place
     */
    public CustomerThread(
            @NotNull Customer customer,
            @NotNull OrderQueue orderQueue,
            @NotNull List<CoffeeCreator> availableCreators,
            int numberOfOrders) {
        this.customer = Objects.requireNonNull(customer, "Customer cannot be null");
        this.orderQueue = Objects.requireNonNull(orderQueue, "Order queue cannot be null");
        this.availableCreators = Objects.requireNonNull(availableCreators, "Creators cannot be null");
        if (numberOfOrders <= 0) {
            throw new IllegalArgumentException("Number of orders must be greater than 0");
        }
        this.numberOfOrders = numberOfOrders;
    }

    @Override
    public void run() {
        System.out.printf("[%s] Arrived at the shop -- placing %d order(s).%n",
                customer.getName(), numberOfOrders);

        for (int i = 0; i < numberOfOrders; i++) {
            try {
                // FACTORY METHOD -- pick a random coffee type
                CoffeeCreator creator = availableCreators
                        .get(random.nextInt(availableCreators.size()));
                Coffee coffee = creator.createCoffee();

                // DECORATOR -- randomly apply extras
                coffee = applyRandomExtras(coffee);

                // STRATEGY -- Order constructor auto-prices from loyalty tier
                Order order = new Order(
                        customer,
                        coffee,
                        CoffeeShop.getInstance().nextOrderId());

                System.out.printf("[%s] Order %d/%d: %s -- $%.2f (%s pricing)%n",
                        customer.getName(),
                        i + 1,
                        numberOfOrders,
                        coffee.getDescription(),
                        order.getFinalPrice(),
                        customer.getLoyaltyTier());

                // PRODUCER -- enqueue (blocks if queue is full)
                orderQueue.enqueue(order);

                // Simulate think time between orders
                if (i < numberOfOrders - 1) {
                    Thread.sleep((long) (Math.random() * 1500) + 500);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s] Interrupted -- leaving shop.%n", customer.getName());
                break;
            }
        }

        System.out.printf("[%s] All orders placed -- waiting for notifications.%n",
                customer.getName());
    }

    /**
     * Randomly applies 0–3 decorator extras to the coffee.
     * Each extra has a 50% chance of being applied independently.
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