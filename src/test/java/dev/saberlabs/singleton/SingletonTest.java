package dev.saberlabs.singleton;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Singleton Pattern")
class SingletonTest {

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    @Test
    @DisplayName("getInstance always returns the same instance")
    void sameInstance() {
        CoffeeShop a = CoffeeShop.getInstance();
        CoffeeShop b = CoffeeShop.getInstance();
        assertSame(a, b);
    }

    @Test
    @DisplayName("orders are shared across references")
    void sharedState() {
        Customer customer = new Customer("C001", "Test");
        CoffeeShop shop = CoffeeShop.getInstance();
        shop.placeOrder(new Order(customer, new Espresso(), 0));
        assertEquals(1, CoffeeShop.getInstance().getOrderCount());
    }

    @Test
    @DisplayName("concurrent getInstance calls return the same instance")
    void threadSafeSingleton() throws InterruptedException {
        int threadCount = 100;
        CoffeeShop[] instances = new CoffeeShop[threadCount];
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> instances[index] = CoffeeShop.getInstance());
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        for (int i = 1; i < threadCount; i++) {
            assertSame(instances[0], instances[i],
                    "Thread " + i + " got a different instance");
        }
    }
}
