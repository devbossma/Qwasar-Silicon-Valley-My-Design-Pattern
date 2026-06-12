package dev.saberlabs;

import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.multithread.CustomerThread;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Main application demonstrating the multithreaded coffee shop.
 * *
 * Patterns in action:
 * - Singleton      → CoffeeShop is the single shared state manager
 * - Factory Method → CoffeeCreators produce random coffee types per order
 * - Decorator      → Random extras applied by CustomerThread
 * - Prototype      → Order.cloneOrder() available on any order
 * - Template Method→ Barista calls coffee.getPreparation().prepareCoffee()
 * - Strategy       → Order auto-prices from Customer's LoyaltyTier
 * - Observer       → Customer notified on PLACED / READY / FULFILLED
 * - Command        → (Available via Facade for sequential use)
 * - Adapter        → (Available via Facade for payment processing)
 * - Producer-Consumer → CustomerThreads produce, Baristas consume
 */
public class CoffeeShopMultithreadApp {

    private static final int QUEUE_CAPACITY    = 10;
    private static final int NUMBER_OF_BARISTAS = 4;
    private static final int ORDERS_PER_CUSTOMER = 5;

    public static void main(String[] args) throws InterruptedException {
        printBanner();

        CoffeeShop shop = CoffeeShop.getInstance();
        shop.clearOrders();

        // Open the shop — creates queue and starts barista threads
        shop.open(QUEUE_CAPACITY, NUMBER_OF_BARISTAS);

        // Menu — Factory Method creators
        List<CoffeeCreator> menu = List.of(
                new EspressoCreator(),
                new CappuccinoCreator(),
                new LatteCreator()
        );

        // Create customers and register as observers
        List<Customer> customers = new ArrayList<>();
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve"};

        for (String name : names) {
            Customer customer = new Customer(
                    shop.nextCustomerId(), name);
            shop.registerObserver(customer);
            customers.add(customer);
        }

        System.out.printf("%n[Main] %d customers, %d baristas, queue capacity %d%n",
                customers.size(), NUMBER_OF_BARISTAS, QUEUE_CAPACITY);
        System.out.printf("[Main] Each customer places %d orders (%d total)%n%n",
                ORDERS_PER_CUSTOMER,
                customers.size() * ORDERS_PER_CUSTOMER);

        // CountDownLatch — main thread waits for ALL customers to finish placing orders
        CountDownLatch customersFinished = getCountDownLatch(customers, shop, menu);

        // Wait for all customers to finish placing orders
        customersFinished.await();
        System.out.println("\n[Main] All customers done placing orders.");
        System.out.println("[Main] Waiting for baristas to finish...\n");

        // Wait for queue to drain completely
        while (!Objects.requireNonNull(shop.getOrderQueue()).isEmpty()) {
            Thread.sleep(500);
        }

        // Buffer — baristas may have dequeued but not yet finished preparing
        Thread.sleep(4000);

        // Close the shop gracefully
        shop.close();

        // Print final report
        printFinalReport(customers, shop);
    }

    private static @NotNull CountDownLatch getCountDownLatch(List<Customer> customers, CoffeeShop shop, List<CoffeeCreator> menu) {
        CountDownLatch customersFinished = new CountDownLatch(customers.size());

        // Launch customer threads (Producers)
        for (Customer customer : customers) {
            CustomerThread ct = new CustomerThread(
                    customer,
                    shop::enqueueOrder,
                    shop::nextOrderId,
                    menu,
                    ORDERS_PER_CUSTOMER
            );

            Thread thread = new Thread(() -> {
                ct.run();
                customersFinished.countDown(); // signal this customer is done
            }, "Customer-" + customer.getName());

            thread.start();
        }
        return customersFinished;
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("   ☕  MULTITHREADED COFFEE SHOP SIMULATION  ☕");
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println();
    }

    private static void printFinalReport(List<Customer> customers, CoffeeShop shop) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("   FINAL REPORT");
        System.out.println("══════════════════════════════════════════════════════");
        System.out.printf("   Total orders processed: %d%n%n", shop.getOrderCount());

        System.out.printf("   %-12s %-10s %-8s%n", "Customer", "Tier", "Orders");
        System.out.println("   ─────────────────────────────────");
        customers.forEach(c ->
                System.out.printf("   %-12s %-10s %-8d%n",
                        c.getName(),
                        c.getLoyaltyTier(),
                        c.getTotalOrders()));

        System.out.println();
        System.out.println("   Barista Performance:");
        System.out.println("   ─────────────────────────────────");
        shop.getBaristas().forEach(b ->
                System.out.printf("   %-14s %d orders%n",
                        b.getName(), b.getOrdersCompleted()));

        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("   Simulation complete.");
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println();
    }
}
