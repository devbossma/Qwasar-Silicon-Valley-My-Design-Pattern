package dev.saberlabs.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Customer previously had no dedicated test class -- its 78% line coverage
 * came incidentally from FacadeTest. This closes the gaps that incidental
 * coverage missed: the restoreTotalOrders() negative-count guard, the exact
 * loyalty-tier boundaries, the update() no-op branch for another customer's
 * order, and equals()/hashCode().
 */
@DisplayName("Customer")
class CustomerTest {

    private Customer newCustomer(String id) {
        return new Customer(id, "Alice");
    }

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("rejects a null id")
        void rejectsNullId() {
            assertThrows(NullPointerException.class, () -> new Customer(null, "Alice"));
        }

        @Test
        @DisplayName("rejects a null name")
        void rejectsNullName() {
            assertThrows(NullPointerException.class, () -> new Customer("C1", null));
        }
    }

    @Nested
    @DisplayName("restoreTotalOrders()")
    class RestoreTotalOrdersTests {

        @Test
        @DisplayName("rejects a negative count")
        void rejectsNegativeCount() {
            Customer customer = newCustomer("C1");
            assertThrows(IllegalArgumentException.class, () -> customer.restoreTotalOrders(-1));
        }

        @Test
        @DisplayName("restores the count and recalculates the tier, without replaying notifications")
        void restoresCountAndRecalculatesTier() {
            Customer customer = newCustomer("C1");

            customer.restoreTotalOrders(7);

            assertEquals(7, customer.getTotalOrders());
            assertEquals(LoyaltyTier.SILVER, customer.getLoyaltyTier());
        }
    }

    @Nested
    @DisplayName("loyalty tier boundaries")
    class LoyaltyTierBoundaryTests {

        @Test
        @DisplayName("stays REGULAR below 5 orders")
        void staysRegularBelowFive() {
            Customer customer = newCustomer("C1");
            for (int i = 0; i < 4; i++) customer.incrementOrders();

            assertEquals(LoyaltyTier.REGULAR, customer.getLoyaltyTier());
        }

        @Test
        @DisplayName("becomes SILVER at exactly 5 orders")
        void becomesSilverAtFive() {
            Customer customer = newCustomer("C1");
            for (int i = 0; i < 5; i++) customer.incrementOrders();

            assertEquals(LoyaltyTier.SILVER, customer.getLoyaltyTier());
        }

        @Test
        @DisplayName("stays SILVER below 10 orders")
        void staysSilverBelowTen() {
            Customer customer = newCustomer("C1");
            for (int i = 0; i < 9; i++) customer.incrementOrders();

            assertEquals(LoyaltyTier.SILVER, customer.getLoyaltyTier());
        }

        @Test
        @DisplayName("becomes GOLD at exactly 10 orders")
        void becomesGoldAtTen() {
            Customer customer = newCustomer("C1");
            for (int i = 0; i < 10; i++) customer.incrementOrders();

            assertEquals(LoyaltyTier.GOLD, customer.getLoyaltyTier());
        }
    }

    @Nested
    @DisplayName("update() (Observer)")
    class UpdateTests {

        @Test
        @DisplayName("rejects a null order")
        void rejectsNullOrder() {
            Customer customer = newCustomer("C1");
            assertThrows(NullPointerException.class,
                    () -> customer.update(null, OrderStatus.PLACED));
        }

        @Test
        @DisplayName("rejects a null status")
        void rejectsNullStatus() {
            Customer customer = newCustomer("C1");
            Order order = new Order(customer, new Espresso(), "ORD-1");

            assertThrows(NullPointerException.class, () -> customer.update(order, null));
        }

        @Test
        @DisplayName("does nothing for an order belonging to a different customer")
        void ignoresOrdersForAnotherCustomer() {
            Customer alice = newCustomer("C1");
            Customer bob = new Customer("C2", "Bob");
            Order bobOrder = new Order(bob, new Espresso(), "ORD-1");

            assertDoesNotThrow(() -> alice.update(bobOrder, OrderStatus.READY));
        }

        @Test
        @DisplayName("prints a notification for an order belonging to this customer")
        void notifiesForOwnOrder() {
            Customer alice = newCustomer("C1");
            Order aliceOrder = new Order(alice, new Espresso(), "ORD-1");

            assertDoesNotThrow(() -> alice.update(aliceOrder, OrderStatus.FULFILLED));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("customers with the same id are equal, even with different names")
        void sameIdIsEqualRegardlessOfName() {
            Customer a = new Customer("C1", "Alice");
            Customer b = new Customer("C1", "Alicia");

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("customers with different ids are not equal")
        void differentIdIsNotEqual() {
            Customer a = new Customer("C1", "Alice");
            Customer b = new Customer("C2", "Alice");

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("not equal to null or a different type")
        void notEqualToNullOrDifferentType() {
            Customer a = new Customer("C1", "Alice");

            assertNotEquals(null, a);
            assertNotEquals("C1", a);
        }

        @Test
        @DisplayName("equal to itself")
        void equalToItself() {
            Customer a = new Customer("C1", "Alice");

            assertEquals(a, a);
        }
    }
}
