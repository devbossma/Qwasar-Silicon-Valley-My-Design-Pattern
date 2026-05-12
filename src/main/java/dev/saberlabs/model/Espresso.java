package dev.saberlabs.model;

/**
 * Concrete coffee: Espresso.
 * Implements Coffee and supports cloning (Prototype pattern).
 */
public class Espresso implements Coffee {

    @Override
    public String getDescription() {
        return "Espresso";
    }

    @Override
    public double getCost() {
        return 2.50;
    }

    @Override
    public Coffee cloneOrder() {
        return new Espresso();
    }

    @Override
    public String toString() {
        return getDescription() + " ($" + String.format("%.2f", getCost()) + ")";
    }
}
