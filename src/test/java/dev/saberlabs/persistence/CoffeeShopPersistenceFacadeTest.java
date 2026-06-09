package dev.saberlabs.persistence;

import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.LoyaltyTier;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.persistence.records.RestoredCoffeeShopState;
import dev.saberlabs.persistence.repositories.implementations.memory.InMemoryCustomerRepository;
import dev.saberlabs.persistence.repositories.implementations.memory.InMemoryOrderRepository;
import dev.saberlabs.singleton.CoffeeShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CoffeeShopPersistenceFacade")
class CoffeeShopPersistenceFacadeTest {

    private CoffeeShop shop;
    private CoffeeShopFacade facade;
    private CoffeeShopPersistenceFacade persistence;
    private InMemoryOrderRepository orderRepo;
    private InMemoryCustomerRepository customerRepo;

    @BeforeEach
    void setUp() {
        shop = CoffeeShop.getInstance();
        shop.clearOrders();

        orderRepo = new InMemoryOrderRepository();
        customerRepo = new InMemoryCustomerRepository();
        persistence = new CoffeeShopPersistenceFacade(shop, customerRepo, orderRepo);

        facade = new CoffeeShopFacade(
                new PayPalAdapter(new PayPalPaymentService("test@mail.com", "pass")));
    }

    // ── Save State ──

    @Nested
    @DisplayName("saveState()")
    class SaveStateTests {

        @Test
        @DisplayName("saves all orders and customers to repositories")
        void savesAllOrdersAndCustomers() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            Order order = facade.placeOrder(alice, new EspressoCreator());
            facade.processOrder(order);

            persistence.saveState();

            assertEquals(1, orderRepo.findAll().size());
            assertEquals(1, customerRepo.findAll().size());
        }

        @Test
        @DisplayName("saves order description with decorators")
        void savesDecoratorChain() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            Order order = facade.placeOrder(alice, new EspressoCreator(), "milk", "sugar");
            facade.processOrder(order);

            persistence.saveState();

            String description = orderRepo.findAll().getFirst().coffee().description();
            assertTrue(description.contains("Espresso"));
            assertTrue(description.contains("Milk"));
            assertTrue(description.contains("Sugar"));
        }

        @Test
        @DisplayName("saves customer loyalty tier correctly")
        void savesLoyaltyTier() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);

            // Place 6 orders → Silver tier
            for (int i = 0; i < 6; i++) {
                Order o = facade.placeOrder(alice, new EspressoCreator());
                facade.processOrder(o);
            }

            persistence.saveState();

            assertEquals(LoyaltyTier.SILVER,
                    customerRepo.findById(alice.getId())
                            .orElseThrow().loyaltyTier());
        }

        @Test
        @DisplayName("saves final price after strategy discount")
        void savesFinalPrice() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);

            // Reach Silver (10% off)
            for (int i = 0; i < 5; i++) {
                facade.processOrder(facade.placeOrder(alice, new EspressoCreator()));
            }
            Order silverOrder = facade.placeOrder(alice, new EspressoCreator());
            facade.processOrder(silverOrder);

            persistence.saveState();

            double savedPrice = orderRepo.findById(silverOrder.getOrderId())
                    .orElseThrow().finalPrice();
            assertEquals(silverOrder.getFinalPrice(), savedPrice, 0.001);
        }

        @Test
        @DisplayName("saves order status correctly")
        void savesOrderStatus() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            Order order = facade.placeOrder(alice, new EspressoCreator());
            facade.processOrder(order);

            persistence.saveState();

            assertEquals(OrderStatus.FULFILLED,
                    orderRepo.findById(order.getOrderId())
                            .orElseThrow().status());
        }

        @Test
        @DisplayName("saveState on empty shop saves nothing")
        void saveStateOnEmptyShop() {
            persistence.saveState();

            assertTrue(orderRepo.findAll().isEmpty());
            assertTrue(customerRepo.findAll().isEmpty());
        }

        @Test
        @DisplayName("multiple saves overwrite previous state")
        void multiplesSavesOverwrite() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            facade.processOrder(facade.placeOrder(alice, new EspressoCreator()));

            persistence.saveState();
            assertEquals(1, orderRepo.findAll().size());

            facade.processOrder(facade.placeOrder(alice, new CappuccinoCreator()));
            persistence.saveState();

            assertEquals(2, orderRepo.findAll().size());
        }
    }

    // ── Restore State ──

    @Nested
    @DisplayName("restoreState()")
    class RestoreStateTests {

        @Test
        @DisplayName("restores orders and customers after simulated restart")
        void restoresAfterRestart() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            Order order = facade.placeOrder(alice, new EspressoCreator(), "milk");
            facade.processOrder(order);

            persistence.saveState();
            String savedDescription = order.getCoffee().getDescription();
            double savedPrice = order.getFinalPrice();
            int savedTotal = alice.getTotalOrders();

            // Simulate restart
            shop.clearOrders();
            assertEquals(0, shop.getOrderCount());

            RestoredCoffeeShopState restored = persistence.restoreState();

            assertEquals(1, shop.getOrderCount());
            assertEquals(savedDescription,
                    restored.orders().get(0).getCoffee().getDescription());
            assertEquals(savedPrice,
                    restored.orders().get(0).getFinalPrice(), 0.001);
            assertEquals(savedTotal,
                    restored.customers().get(0).getTotalOrders());
        }

        @Test
        @DisplayName("restores order status")
        void restoresOrderStatus() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            Order order = facade.placeOrder(alice, new EspressoCreator());
            facade.processOrder(order);

            persistence.saveState();
            shop.clearOrders();

            RestoredCoffeeShopState restored = persistence.restoreState();
            assertEquals(OrderStatus.FULFILLED,
                    restored.orders().get(0).getStatus());
        }

        @Test
        @DisplayName("restores customer loyalty tier")
        void restoresLoyaltyTier() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);

            for (int i = 0; i < 11; i++) {
                facade.processOrder(facade.placeOrder(alice, new EspressoCreator()));
            }
            assertEquals(LoyaltyTier.GOLD, alice.getLoyaltyTier());

            persistence.saveState();
            shop.clearOrders();

            RestoredCoffeeShopState restored = persistence.restoreState();
            assertEquals(LoyaltyTier.GOLD,
                    restored.customers().get(0).getLoyaltyTier());
        }

        @Test
        @DisplayName("syncs order ID counter after restore — no duplicate IDs")
        void syncsOrderCounterNoDuplicates() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            Order order = facade.placeOrder(alice, new EspressoCreator());
            facade.processOrder(order);

            persistence.saveState();
            shop.clearOrders();
            persistence.restoreState();

            // New order should have a different ID than the restored one
            Order newOrder = facade.placeOrder(alice, new CappuccinoCreator());
            assertNotEquals(order.getOrderId(), newOrder.getOrderId());
        }

        @Test
        @DisplayName("syncs customer ID counter after restore — no duplicate IDs")
        void syncsCustomerCounterNoDuplicates() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            facade.processOrder(facade.placeOrder(alice, new EspressoCreator()));

            persistence.saveState();
            shop.clearOrders();
            persistence.restoreState();

            Customer newCustomer = facade.createCustomer("Bob");
            assertNotEquals(alice.getId(), newCustomer.getId());
        }

        @Test
        @DisplayName("restores multiple customers independently")
        void restoresMultipleCustomers() {
            Customer alice = facade.createCustomer("Alice");
            Customer bob = facade.createCustomer("Bob");
            facade.registerCustomer(alice);
            facade.registerCustomer(bob);

            facade.processOrder(facade.placeOrder(alice, new EspressoCreator()));
            facade.processOrder(facade.placeOrder(bob, new CappuccinoCreator()));

            persistence.saveState();
            shop.clearOrders();

            RestoredCoffeeShopState restored = persistence.restoreState();

            assertEquals(2, restored.customers().size());
            assertEquals(2, restored.orders().size());
        }

        @Test
        @DisplayName("restores empty state safely")
        void restoresEmptyStateSafely() {
            RestoredCoffeeShopState restored = persistence.restoreState();

            assertTrue(restored.orders().isEmpty());
            assertTrue(restored.customers().isEmpty());
        }

        @Test
        @DisplayName("decorator chain survives save/restore cycle")
        void decoratorChainSurvivesRoundTrip() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            Order order = facade.placeOrder(alice, new EspressoCreator(),
                    "milk", "sugar", "whipped");
            String originalDescription = order.getCoffee().getDescription();
            double originalCost = order.getCoffee().getCost();
            facade.processOrder(order);

            persistence.saveState();
            shop.clearOrders();

            RestoredCoffeeShopState restored = persistence.restoreState();
            Order restoredOrder = restored.orders().get(0);

            assertEquals(originalDescription,
                    restoredOrder.getCoffee().getDescription());
            assertEquals(originalCost,
                    restoredOrder.getCoffee().getCost(), 0.001);
        }
    }

    // ── Clear Saved State ──

    @Nested
    @DisplayName("clearSavedState()")
    class ClearSavedStateTests {

        @Test
        @DisplayName("clears both repositories")
        void clearsBothRepositories() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            facade.processOrder(facade.placeOrder(alice, new EspressoCreator()));

            persistence.saveState();
            persistence.clearSavedState();

            assertTrue(orderRepo.findAll().isEmpty());
            assertTrue(customerRepo.findAll().isEmpty());
        }

        @Test
        @DisplayName("restoreState after clearSavedState returns empty state")
        void restoreAfterClearIsEmpty() {
            Customer alice = facade.createCustomer("Alice");
            facade.registerCustomer(alice);
            facade.processOrder(facade.placeOrder(alice, new EspressoCreator()));

            persistence.saveState();
            persistence.clearSavedState();
            shop.clearOrders();

            RestoredCoffeeShopState restored = persistence.restoreState();
            assertTrue(restored.orders().isEmpty());
            assertTrue(restored.customers().isEmpty());
        }
    }

    // ── Constructor Validation ──

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorTests {

        @Test
        @DisplayName("throws on null CoffeeShop")
        void throwsOnNullCoffeeShop() {
            assertThrows(NullPointerException.class,
                    () -> new CoffeeShopPersistenceFacade(null, customerRepo, orderRepo));
        }

        @Test
        @DisplayName("throws on null CustomerRepository")
        void throwsOnNullCustomerRepository() {
            assertThrows(NullPointerException.class,
                    () -> new CoffeeShopPersistenceFacade(shop, null, orderRepo));
        }

        @Test
        @DisplayName("throws on null OrderRepository")
        void throwsOnNullOrderRepository() {
            assertThrows(NullPointerException.class,
                    () -> new CoffeeShopPersistenceFacade(shop, customerRepo, null));
        }
    }
}
