package dev.saberlabs.model;

/**
 * Represents a customer order with a coffee, customer name, and computed price.
 */
public class Order implements CloneableOrder {

    private final String customerName;
    private final Coffee coffee;
    private double finalPrice;
    private String status;

    public Order(String customerName, Coffee coffee) {
        this.customerName = customerName;
        this.coffee = coffee;
        this.finalPrice = coffee.getCost();
        this.status = "PLACED";
    }

    public String getCustomerName() {
        return customerName;
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
    }

    @Override
    public String toString() {
        return String.format("Order[customer=%s, coffee=%s, price=$%.2f, status=%s]",
                customerName, coffee.getDescription(), finalPrice, status);
    }

    @Override
    public Order cloneOrder() {
        return new Order(this.customerName, this.coffee.cloneCoffee());
    }

    // Overloaded method to clone order with a new customer name
    public Order cloneOrder(String newCustomerName) {
        return new Order(newCustomerName, this.coffee.cloneCoffee());
    }
}
