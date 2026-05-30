package dev.saberlabs.prototype;

import dev.saberlabs.models.Order;

/**
 * Pattern 4: PROTOTYPE — Cloneable Order Interface
 *
 * <p>Marks an {@link dev.saberlabs.models.Order} as deep-cloneable for the same customer.
 * The companion method {@code cloneOrder(Customer)} on {@code Order} creates a copy
 * re-priced under a different customer's loyalty tier.
 */
public interface CloneableOrder extends Cloneable {
    /**
     * Creates and returns a copy of this Order.
     * This method is used to support the Prototype pattern, allowing for easy duplication of Orders.
     *
     * @return a clone of this coffee order
     */

    Order cloneOrder();
}
