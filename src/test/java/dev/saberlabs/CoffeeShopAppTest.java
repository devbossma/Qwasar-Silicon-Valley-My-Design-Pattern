package dev.saberlabs;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.model.*;
import dev.saberlabs.singleton.CoffeeShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for all 10 design patterns in the coffee shop application.
 */
class CoffeeShopAppTest {

    @BeforeEach
    void cleanUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    // =================================================================
    // 1a. SINGLETON
    // =================================================================
    @Nested
    @DisplayName("1a. Singleton Pattern")
    class SingletonTests {

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
            CoffeeShop shop = CoffeeShop.getInstance();
            shop.placeOrder(new Order("Test", new Espresso()));
            assertEquals(1, CoffeeShop.getInstance().getOrderCount());
        }
    }
    // =================================================================
    // 1b. SINGLETON — Thread Safety
    // =================================================================
    @Nested
    @DisplayName("1b. Singleton Pattern - Thread Safety")
    class SingletonThreadSafetyTests {

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


    // =================================================================
    // 2. FACTORY METHOD
    // =================================================================

    @Nested
    @DisplayName("2. Factory Method Pattern")
    class FactoryTests {

        @Test
        @DisplayName("EspressoCreator creates Espresso")
        void createEspresso() {
            CoffeeCreator creator = new EspressoCreator();
            Coffee c = creator.createCoffee();
            assertInstanceOf(Espresso.class, c);
            assertEquals(2.50, c.getCost());
        }

        @Test
        @DisplayName("CappuccinoCreator creates Cappuccino")
        void createCappuccino() {
            CoffeeCreator creator = new CappuccinoCreator();
            Coffee c = creator.createCoffee();
            assertInstanceOf(Cappuccino.class, c);
            assertEquals(3.50, c.getCost());
        }

        @Test
        @DisplayName("LatteCreator creates Latte")
        void createLatte() {
            CoffeeCreator creator = new LatteCreator();
            Coffee c = creator.createCoffee();
            assertInstanceOf(Latte.class, c);
            assertEquals(4.00, c.getCost());
        }
    }

    // =================================================================
    // 3. DECORATOR
    // =================================================================
    @Nested
    @DisplayName("3. Decorator Pattern")
    class DecoratorTests {

        @Test
        @DisplayName("MilkDecorator adds $0.50")
        void milk() {
            Coffee c = new MilkDecorator(new Espresso());
            assertEquals(3.00, c.getCost(), 0.001);
            assertTrue(c.getDescription().contains("Milk"));
        }

        @Test
        @DisplayName("SugarDecorator adds $0.25")
        void sugar() {
            Coffee c = new SugarDecorator(new Espresso());
            assertEquals(2.75, c.getCost(), 0.001);
        }

        @Test
        @DisplayName("WhippedCreamDecorator adds $0.75")
        void whippedCream() {
            Coffee c = new WhippedCreamDecorator(new Espresso());
            assertEquals(3.25, c.getCost(), 0.001);
        }

        @Test
        @DisplayName("stacking multiple decorators accumulates cost and description")
        void stacked() {
            Coffee c = new WhippedCreamDecorator(
                    new MilkDecorator(
                            new SugarDecorator(new Espresso())));
            // 2.50 + 0.25 + 0.50 + 0.75 = 4.00
            assertEquals(4.00, c.getCost(), 0.001);
            assertTrue(c.getDescription().contains("Espresso"));
            assertTrue(c.getDescription().contains("Sugar"));
            assertTrue(c.getDescription().contains("Milk"));
            assertTrue(c.getDescription().contains("Whipped Cream"));
        }
    }

}