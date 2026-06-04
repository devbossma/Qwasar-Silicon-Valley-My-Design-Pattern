package dev.saberlabs.persistence;

import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.persistence.records.RestoredCoffeeShopState;
import dev.saberlabs.singleton.CoffeeShop;

import java.nio.file.Path;

/**
 * Simple persistence smoke tests before adding JUnit coverage.
 */
public class PersistenceDemo {

    public static void main(String[] args) {
        Path demoData = Path.of("data", "persistence-demo");
        CoffeeShop shop = CoffeeShop.getInstance();
        CoffeeShopPersistenceFacade persistence = new CoffeeShopPersistenceFacade(demoData);

        persistence.clearSavedState();
        shop.clearOrders();

        CoffeeShopFacade facade = new CoffeeShopFacade(
                new PayPalAdapter(new PayPalPaymentService("demo@mail.com", "pass")));
        Customer alice = facade.createCustomer("Alice");

        Order original = facade.placeOrder(alice, new EspressoCreator(), "milk", "sugar");
        facade.processOrder(original);

        persistence.saveState();
        int savedOrderCount = shop.getOrderCount();
        String savedDescription = original.getCoffee().getDescription();
        double savedPrice = original.getFinalPrice();

        shop.clearOrders();
        assertEquals(0, shop.getOrderCount(), "CoffeeShop should be empty after simulated restart");

        RestoredCoffeeShopState restored = persistence.restoreState();
        Order restoredOrder = restored.orders().getFirst();
        Customer restoredCustomer = restored.customers().getFirst();

        assertEquals(savedOrderCount, shop.getOrderCount(), "Restored order count should match saved order count");
        assertEquals(savedDescription, restoredOrder.getCoffee().getDescription(), "Coffee decorators should survive restore");
        assertEquals(savedPrice, restoredOrder.getFinalPrice(), "Final price should survive restore");
        assertEquals(OrderStatus.FULFILLED, restoredOrder.getStatus(), "Order status should survive restore");
        assertEquals(1, restoredCustomer.getTotalOrders(), "Customer loyalty count should survive restore");

        Order nextOrder = facade.placeOrder(restoredCustomer, new EspressoCreator());
        if (nextOrder.getOrderId().equals(restoredOrder.getOrderId())) {
            throw new IllegalStateException("Restored ID counters should not create duplicate order IDs");
        }

        System.out.println("[PersistenceDemo] PASS - coffee shop state saved and restored successfully.");
        System.out.println("[PersistenceDemo] Data directory: " + demoData.toAbsolutePath());
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + ". Expected: " + expected + ", actual: " + actual);
        }
    }

    private static void assertEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new IllegalStateException(message + ". Expected: " + expected + ", actual: " + actual);
        }
    }
}
