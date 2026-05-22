package dev.saberlabs.models;

import dev.saberlabs.template.CoffeePreparationTemplate;
import dev.saberlabs.template.EspressoPreparation;

/**
 * Concrete coffee: Cappuccino.
 */
public class Cappuccino implements Coffee {

    @Override
    public String getDescription() {
        return "Cappuccino";
    }

    @Override
    public double getCost() {
        return 3.50;
    }

    @Override
    public Coffee cloneCoffee() {
        return new Cappuccino();
    }

    @Override
    public String toString() {
        return getDescription() + " ($" + String.format("%.2f", getCost()) + ")";
    }

    @Override

    public CoffeePreparationTemplate getPreparation() {
        return new EspressoPreparation();
    }
}
