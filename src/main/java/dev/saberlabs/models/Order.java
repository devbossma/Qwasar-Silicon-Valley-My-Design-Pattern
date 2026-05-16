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


    public Order(Customer customer, Coffee coffee) {
        this.customer = customer;
        this.coffee = coffee;
        this.pricingStrategy = customer.getLoyaltyTier().getStrategy();
        this.finalPrice = calculateFinalPrice();
    }

    public Customer getCustomer() {
        return customer;
    }

    public Coffee getCoffee() {
        return coffee;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }


    public OrderStatus getStatus() {
        return status;
    }

    // When the order status is updated to READY, we check if it was not already fulfilled. If not, we mark it as fulfilled and increment the customer's order count to potentially upgrade their loyalty tier.
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
        return new Order(this.customer, this.coffee.cloneCoffee());
    }

    // Overloaded method to clone the order for a different customer (e.g., for a friend)
    public Order cloneOrder(Customer newCustomer) {
        return new Order(newCustomer, this.coffee.cloneCoffee());
    }

    @Override
    public String toString() {
        return String.format("Order[customer=%s, coffee=%s, price=$%.2f, tier=%s, status=%s]",
                customer.getName(), coffee.getDescription(), finalPrice,
                customer.getLoyaltyTier(), status);
    }
}
