package dev.saberlabs.models;

import dev.saberlabs.template.CoffeePreparationTemplate;
import dev.saberlabs.template.LattePreparation;

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
    public Coffee cloneCoffee() {
        return new Latte();
    }

    @Override
    public String toString() {
        return getDescription() + " ($" + String.format("%.2f", getCost()) + ")";
    }

    @Override
    public CoffeePreparationTemplate getPreparation() {
        return new LattePreparation();
    }
}
