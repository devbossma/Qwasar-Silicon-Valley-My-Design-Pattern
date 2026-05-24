package dev.saberlabs.models;

import dev.saberlabs.observer.OrderNotificationService;
import dev.saberlabs.prototype.CloneableOrder;
import dev.saberlabs.singleton.CoffeeShop;
import dev.saberlabs.strategy.PricingStrategy;


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
    private OrderStatus status = null;
    private boolean fulfilled = false;
    private final int OrderId;


    public Order(Customer customer, Coffee coffee, int orderId) {
        this.customer = customer;
        this.coffee = coffee;
        this.pricingStrategy = customer.getLoyaltyTier().getStrategy();
        this.OrderId = orderId;
        this.finalPrice = calculateFinalPrice();

    }

    /**
     * Returns the customer associated with this order.
     * @return the customer
     *
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Returns the coffee associated with this order.
     * @return the coffee object associated with this order
     */
    public Coffee getCoffee() {
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
    public int getOrderId() {
        return OrderId;
    }


    /**
     * Returns the current status of the order (e.g., PLACED, PREPARING, READY, FULFILLED, CANCELLED).
     * @return the order status
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * When the order status is updated to READY, we check if it was not already fulfilled.
     * If not, we mark it as fulfilled and increment the customer's order count to potentially upgrade their loyalty tier.
     *
     * @param status the new status to set for the order
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
        if (OrderStatus.FULFILLED.equals(status) && !fulfilled) {
            fulfilled = true;
            customer.incrementOrders();
        }
        // Notify observers of the status change
        CoffeeShop.getInstance().getNotificationService().notifyObservers(this);
    }

    // Use the pricing strategy to calculate the final price based on the customer's loyalty tier
    private double calculateFinalPrice() {

        return pricingStrategy.calculatePrice(coffee.getCost());
    }

    // Implement the cloneOrder method to create a deep copy of the order
    @Override
    public Order cloneOrder() {
        int orderId = CoffeeShop.getInstance().nextOrderId();
        return new Order(this.customer, this.coffee.cloneCoffee(), orderId );
    }

    // Overloaded method to clone the order for a different customer (e.g., for a friend)
    public Order cloneOrder(Customer newCustomer) {
        int orderId = CoffeeShop.getInstance().nextOrderId();
        return new Order(newCustomer, this.coffee.cloneCoffee(), orderId);
    }

    @Override
    public String toString() {
        return String.format("Order[customer=%s, coffee=%s, price=$%.2f, tier=%s, status=%s]",
                customer.getName(), coffee.getDescription(), finalPrice,
                customer.getLoyaltyTier(), status);
    }
}
