package dev.saberlabs.model;

/**
 * Concrete coffee: Latte.
 */
public class Latte implements Coffee {

    @Override
    public String getDescription() {
        return "Latte";
    }

    @Override
    public double getCost() {
        return 4.00;
    }

    @Override
    public Coffee cloneOrder() {
        return new Latte();
    }

    @Override
    public String toString() {
        return getDescription() + " ($" + String.format("%.2f", getCost()) + ")";
    }
}
