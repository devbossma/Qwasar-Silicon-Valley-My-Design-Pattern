package dev.saberlabs.models;

import dev.saberlabs.prototype.CloneableOrder;
import dev.saberlabs.singleton.CoffeeShop;
import dev.saberlabs.strategy.PricingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


/**
 * Represents a customer order with a coffee, customer, and computed price.
 * Price is automatically calculated based on the customer's loyalty tier.
 * Loyalty tier is updated only when the order is fulfilled (status = READY).
 */
public class Order implements CloneableOrder {

    private final Customer customer;
    private final Coffee coffee;
    private final PricingStrategy pricingStrategy;
    private double finalPrice;
    private volatile @Nullable OrderStatus status = null;
    private volatile boolean fulfilled = false;
    private final @NotNull String orderId;


    public Order(@NotNull Customer customer, @NotNull Coffee coffee, @NotNull String orderId) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        Objects.requireNonNull(coffee, "Coffee cannot be null");
        Objects.requireNonNull(orderId, "OrderId cannot be null");
        this.customer = customer;
        this.coffee = coffee;
        this.pricingStrategy = customer.getLoyaltyTier().getStrategy();
        this.orderId = orderId;
        this.finalPrice = calculateFinalPrice();
    }

    public Order(@NotNull Customer customer, @NotNull Coffee coffee, int orderId) {
        this(customer, coffee, String.valueOf(orderId));
    }

    /**
     * Returns the customer associated with this order.
     * @return the customer
     *
     */
    public @NotNull  Customer getCustomer() {
        return customer;
    }

    /**
     * Returns the coffee associated with this order.
     * @return the coffee object associated with this order
     */
    public @NotNull Coffee getCoffee() {
        return coffee;
    }

    /**
     * Returns the final price of the order after applying the pricing strategy based on the customer's loyalty tier.
     * @return the final price in dollars
     */
    public double getFinalPrice() {
        return finalPrice;
    }

    /**
     * Sets the final price of the order. This method can be used to update the price if needed,
     * but typically the price is calculated automatically based on the customer's loyalty tier and the coffee's base cost.
     *
     * @param finalPrice the final price to set for the order
     */
    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    /**
     * Returns the unique identifier for this order.
     * @return the order ID
     */

    public @NotNull String getOrderId() {
        return orderId;
    }


    /**
     * Returns the current status of the order (e.g., PLACED, PREPARING, READY, FULFILLED, CANCELLED).
     * @return the order status
     */
    public @Nullable OrderStatus getStatus() {
        return status;
    }

    /**
     * When the order status is updated to READY, we check if it was not already fulfilled.
     * If not, we mark it as fulfilled and increment the customer's order count to potentially upgrade their loyalty tier.
     *
     * @param status the new status to set for the order
     */
    public synchronized void setStatus(@NotNull OrderStatus status) {
        Objects.requireNonNull(status, "Order status cannot be null");
        this.status = status;
        if (OrderStatus.FULFILLED.equals(status) && !fulfilled) {
            fulfilled = true;
            customer.incrementOrders();
        }
        CoffeeShop.getInstance().getNotificationService().notifyObservers(this);
    }

    /**
     * Restores persisted status without notifying observers or changing loyalty counters.
     *
     * @param status the persisted status
     */
    public synchronized void restoreStatus(@Nullable OrderStatus status) {
        this.status = status;
        fulfilled = OrderStatus.FULFILLED.equals(status);
    }

    // Use the pricing strategy to calculate the final price based on the customer's loyalty tier
    private double calculateFinalPrice() {

        return pricingStrategy.calculatePrice(coffee.getCost());
    }

    // Implement the cloneOrder method to create a deep copy of the order
    @Override
    @NotNull
    public  Order cloneOrder() {
        String orderId = CoffeeShop.getInstance().nextOrderId();
        return new Order(this.customer, this.coffee.cloneCoffee(), orderId );
    }

    // Overloaded method to clone the order for a different customer (e.g., for a friend)
    public Order cloneOrder(Customer newCustomer) {
        String orderId = CoffeeShop.getInstance().nextOrderId();
        return new Order(newCustomer, this.coffee.cloneCoffee(), orderId);
    }

    @Override
    public String toString() {
        return String.format("Order[customer=%s, coffee=%s, price=$%.2f, tier=%s, status=%s]",
                customer.getName(), coffee.getDescription(), finalPrice,
                customer.getLoyaltyTier(), status);
    }
}
