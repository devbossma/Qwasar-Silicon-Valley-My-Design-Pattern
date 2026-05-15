package dev.saberlabs.model;

import dev.saberlabs.prototype.CloneableOrder;

/**
 * Represents a customer order with a coffee, customer, and computed price.
 * Price is automatically calculated based on the customer's loyalty tier.
 * Loyalty tier is updated only when the order is fulfilled (status = READY).
 */
public class Order implements CloneableOrder {

    private final Customer customer;
    private final Coffee coffee;
    private double finalPrice;
    private String status;

    public void setFulfilled(boolean fulfilled) {
        this.fulfilled = fulfilled;
    }

    private boolean fulfilled;

    public Order(Customer customer, Coffee coffee) {
        this.customer = customer;
        this.coffee = coffee;
        this.finalPrice = calculateFinalPrice();
        this.status = "PLACED";
        this.fulfilled = false;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("READY".equals(status) && !fulfilled) {
            fulfilled = true;
            customer.incrementOrders();
        }
    }

    private double calculateFinalPrice() {
        return customer.getLoyaltyTier()
                .getStrategy()
                .calculatePrice(coffee.getCost());
    }

    @Override
    public Order cloneOrder() {
        return new Order(this.customer, this.coffee.cloneCoffee());
    }

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
