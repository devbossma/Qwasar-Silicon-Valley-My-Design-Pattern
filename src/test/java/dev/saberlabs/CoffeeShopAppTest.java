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
import dev.saberlabs.strategy.GoldMemberPricing;
import dev.saberlabs.strategy.RegularPricing;
import dev.saberlabs.strategy.SilverMemberPricing;
import dev.saberlabs.template.CappuccinoPreparation;
import dev.saberlabs.template.CoffeePreparationTemplate;
import dev.saberlabs.template.EspressoPreparation;
import dev.saberlabs.template.LattePreparation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
            Customer customer = new Customer("C001", "Test");
            CoffeeShop shop = CoffeeShop.getInstance();
            shop.placeOrder(new Order(customer, new Espresso()));
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

    // =================================================================
    // 4. Prototype
    // =================================================================

    @Nested
    @DisplayName("4. Prototype Pattern")
    class PrototypeTests {
        @Test
        @DisplayName("clone a coffee without extras")
        void CloneCoffeeWithoutExtras() {
            Coffee original = new MilkDecorator(new Espresso());
            Coffee clonedCoffee = original.cloneCoffee();

            assertNotSame(original, clonedCoffee);
            assertEquals(original.getCost(), clonedCoffee.getCost(), 0.001);
            assertEquals(original.getDescription(), clonedCoffee.getDescription());
        }

        @Test
        @DisplayName("clone a coffee with multiple decorators")
        void CloneCoffeeWithMultipleDecorators() {
            Coffee original = new WhippedCreamDecorator(
                    new MilkDecorator(
                            new SugarDecorator(new Espresso())));
            Coffee clonedCoffee = original.cloneCoffee();
            assertNotSame(original, clonedCoffee);
            assertEquals(original.getCost(), clonedCoffee.getCost(), 0.001);
            assertEquals(original.getDescription(), clonedCoffee.getDescription());        }

        @Test
        @DisplayName("modifying the clone does not affect the original")
        void modifyingCloneDoesNotAffectOriginal() {
            Coffee original = new MilkDecorator(new Espresso());
            Coffee clonedCoffee = original.cloneCoffee();
            // Add sugar to the clone
            clonedCoffee = new SugarDecorator(clonedCoffee);

            assertNotSame(original, clonedCoffee);
        }

        @Test
        @DisplayName("Clone Order for the same customer")
        void cloneOrderForTheSameCustomer() {
            Customer customer = new Customer("C002", "Yassine");
            Coffee originalCoffee = new MilkDecorator(new Espresso());
                Order originalOrder = new Order(customer, originalCoffee);
            Order clonedOrder = originalOrder.cloneOrder();

            assertSame(originalOrder.getCustomer(), clonedOrder.getCustomer());
            assertEquals(originalOrder.getCoffee().getDescription(), clonedOrder.getCoffee().getDescription());
        }

        @Test
        @DisplayName("Clone Order for a different customer same coffee")
        void cloneOrderForDifferentCustomer() {
            Customer customer = new Customer("C003", "Yassine");;
            Coffee original = new MilkDecorator(new Espresso());
            Order originalOrder = new Order(customer, original);
            Order clonedOrder = originalOrder.cloneOrder(new Customer("C004", "Ahmed"));

            assertNotSame(originalOrder.getCustomer(), clonedOrder.getCustomer());
            assertEquals(originalOrder.getCoffee().getDescription(), clonedOrder.getCoffee().getDescription());
        }
    }

    // =================================================================
    // 5. TEMPLATE METHOD
    // =================================================================
    @Nested
    @DisplayName("5. Template Method Pattern")
    class TemplateMethodTests {

        @Test
        @DisplayName("all preparations follow the same step sequence")
        void fixedAlgorithmStructure() {
            EspressoPreparation espresso = new EspressoPreparation();
            CappuccinoPreparation cappuccino = new CappuccinoPreparation();
            LattePreparation latte = new LattePreparation();

            espresso.prepareCoffee();
            cappuccino.prepareCoffee();
            latte.prepareCoffee();

            // All three must start with boiling water and end with "Coffee is ready!"
            for (var prep : List.of(espresso, cappuccino, latte)) {
                List<String> log = prep.getPreparationLog();
                assertTrue(log.getFirst().contains("Boiling water"));
                assertTrue(log.getLast().contains("is ready!"));
            }
        }

        @Test
        @DisplayName("EspressoPreparation uses correct temperature and duration")
        void espressoTemperatureAndDuration() {
            EspressoPreparation prep = new EspressoPreparation();
            prep.prepareCoffee();

            String boilStep = prep.getPreparationLog().getFirst();
            assertTrue(boilStep.contains("95°C"));
            assertTrue(boilStep.contains("25 seconds"));
        }

        @Test
        @DisplayName("CappuccinoPreparation uses correct temperature and duration")
        void cappuccinoTemperatureAndDuration() {
            CappuccinoPreparation prep = new CappuccinoPreparation();
            prep.prepareCoffee();

            String boilStep = prep.getPreparationLog().getFirst();
            assertTrue(boilStep.contains("90°C"));
            assertTrue(boilStep.contains("30 seconds"));
        }

        @Test
        @DisplayName("LattePreparation uses correct temperature and duration")
        void latteTemperatureAndDuration() {
            LattePreparation prep = new LattePreparation();
            prep.prepareCoffee();

            String boilStep = prep.getPreparationLog().getFirst();
            assertTrue(boilStep.contains("93°C"));
            assertTrue(boilStep.contains("28 seconds"));
        }

        @Test
        @DisplayName("EspressoPreparation has no condiments")
        void espressoNoCondiments() {
            EspressoPreparation prep = new EspressoPreparation();
            prep.prepareCoffee();

            assertTrue(prep.getPreparationLog().stream()
                    .anyMatch(s -> s.contains("No condiments")));
        }

        @Test
        @DisplayName("CappuccinoPreparation includes brewing, steaming, and assembly")
        void cappuccinoBrewSteps() {
            CappuccinoPreparation prep = new CappuccinoPreparation();
            prep.prepareCoffee();

            List<String> log = prep.getPreparationLog();
            assertTrue(log.stream().anyMatch(s -> s.contains("brewing process for cappuccino")));
            assertTrue(log.stream().anyMatch(s -> s.contains("Steaming milk for cappuccino")));
            assertTrue(log.stream().anyMatch(s -> s.contains("Assembling the cappuccino")));
        }

        @Test
        @DisplayName("LattePreparation includes brewing, steaming, and assembly")
        void latteBrewSteps() {
            LattePreparation prep = new LattePreparation();
            prep.prepareCoffee();

            List<String> log = prep.getPreparationLog();
            assertTrue(log.stream().anyMatch(s -> s.contains("brewing process for latte")));
            assertTrue(log.stream().anyMatch(s -> s.contains("Steaming milk for latte")));
            assertTrue(log.stream().anyMatch(s -> s.contains("Assembling the latte")));
        }

        @Test
        @DisplayName("CappuccinoPreparation adds cocoa and cinnamon condiments")
        void cappuccinoCondiments() {
            CappuccinoPreparation prep = new CappuccinoPreparation();
            prep.prepareCoffee();

            assertTrue(prep.getPreparationLog().stream()
                    .anyMatch(s -> s.contains("condiments for cappuccino")));
        }

        @Test
        @DisplayName("LattePreparation adds vanilla and cocoa condiments")
        void latteCondiments() {
            LattePreparation prep = new LattePreparation();
            prep.prepareCoffee();

            assertTrue(prep.getPreparationLog().stream()
                    .anyMatch(s -> s.contains("condiments for latte")));
        }

        @Test
        @DisplayName("each preparation type produces a different number of steps")
        void differentStepCounts() {
            CoffeePreparationTemplate espresso = new EspressoPreparation();
            CoffeePreparationTemplate cappuccino = new CappuccinoPreparation();
            CoffeePreparationTemplate latte = new LattePreparation();

            espresso.prepareCoffee();
            cappuccino.prepareCoffee();
            latte.prepareCoffee();

            // Espresso is simpler (fewer logged steps) than cappuccino and latte
            assertTrue(espresso.getPreparationLog().size() < cappuccino.getPreparationLog().size());
            assertTrue(espresso.getPreparationLog().size() < latte.getPreparationLog().size());
        }
    }

    // =================================================================
    // 6. STRATEGY - PRICING STRATEGIES
    // =================================================================

    @Nested
    @DisplayName("6. STRATEGY: Test Pricing strategies based on Customer's Loyalty Tiers")
    class StrategyTests {

        @Test
        @DisplayName("RegularPricing returns base price")
        void regularPricing() {
            Coffee coffee = new Espresso();
            RegularPricing pricing = new RegularPricing();
            double finalPrice = pricing.calculatePrice(coffee.getCost());
            assertEquals(coffee.getCost(), finalPrice, 0.001);
        }

        @Test
        @DisplayName("SilverMemberPricing applies 10% discount")
        void silverMemberPricing() {
            Coffee coffee = new Espresso();
            SilverMemberPricing pricing = new SilverMemberPricing();
            double finalPrice = pricing.calculatePrice(coffee.getCost());
            assertEquals(coffee.getCost() * 0.90, finalPrice, 0.001);
        }

        @Test
        @DisplayName("GoldMemberPricing applies 20% discount")
        void goldMemberPricing() {
            Coffee coffee = new Espresso();
            GoldMemberPricing pricing = new GoldMemberPricing();
            double finalPrice = pricing.calculatePrice(coffee.getCost());
            assertEquals(coffee.getCost() * 0.80, finalPrice, 0.001);
        }

        @Test
        @DisplayName("Customer tier upgrades automatically after reaching order thresholds")
        void tierUpgradesThroughOrders() {
            Customer customer = new Customer("C008", "Evolving Customer");

            assertEquals(LoyaltyTier.REGULAR, customer.getLoyaltyTier());

            for (int i = 0; i < 6; i++) {
                customer.incrementOrders();
            }
            assertEquals(LoyaltyTier.SILVER, customer.getLoyaltyTier());

            for (int i = 0; i < 5; i++) {
                customer.incrementOrders();
            }
            assertEquals(LoyaltyTier.GOLD, customer.getLoyaltyTier());
        }

        @Test
        @DisplayName("Customer's LoyaltyTier determines the correct pricing strategy")
        void customerLoyaltyTierDeterminesPricing() {
            Customer regularCustomer = new Customer("C005", "Regular Customer");
            Customer silverCustomer = new Customer("C006", "Silver Customer");
            for (int i = 0; i < 6; i++) {
                silverCustomer.incrementOrders();
            }
            Customer goldCustomer = new Customer("C007", "Gold Customer");
            for (int i = 0; i < 11; i++) {
                goldCustomer.incrementOrders();
            }
            Coffee coffee = new Espresso();

            Order order = new Order(regularCustomer, coffee);
            Order silverOrder = new Order(silverCustomer, coffee);
            Order goldOrder = new Order(goldCustomer, coffee);

            double expectedRegularPrice = coffee.getCost();
            double expectedSilverPrice = coffee.getCost() * 0.90;
            double expectedGoldPrice = coffee.getCost() * 0.80;

            assertEquals(expectedRegularPrice, order.getFinalPrice(), 0.001);
            assertEquals(expectedSilverPrice, silverOrder.getFinalPrice(), 0.001);
            assertEquals(expectedGoldPrice, goldOrder.getFinalPrice(), 0.001);
        }

        @Test
        @DisplayName("Strategy applies discount on decorated coffee total cost")
        void strategyAppliesOnDecoratedCoffee() {
            Customer goldCustomer = new Customer("C009", "Gold Decorated");
            for (int i = 0; i < 11; i++) {
                goldCustomer.incrementOrders();
            }

            Coffee decoratedCoffee = new MilkDecorator(new SugarDecorator(new Espresso()));
            // Base: 2.50 + 0.25 + 0.50 = 3.25, Gold: 3.25 * 0.80 = 2.60
            Order order = new Order(goldCustomer, decoratedCoffee);

            assertEquals(3.25 * 0.80, order.getFinalPrice(), 0.001);
        }

    }

}