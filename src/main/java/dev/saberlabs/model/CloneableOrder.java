package dev.saberlabs.model;

public interface CloneableOrder extends Cloneable {
    /**
     * Creates and returns a copy of this Order.
     * This method is used to support the Prototype pattern, allowing for easy duplication of Orders.
     *
     * @return a clone of this coffee order
     */

    Order cloneOrder();
}
