package dev.saberlabs.models;

import dev.saberlabs.template.CoffeePreparationTemplate;
import dev.saberlabs.template.EspressoPreparation;

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
    public String toString() {
        return getDescription() + " ($" + String.format("%.2f", getCost()) + ")";
    }

    @Override
    public Coffee cloneCoffee() {
        return new Espresso();
    }

    @Override
    public CoffeePreparationTemplate getPreparation(){
        return new EspressoPreparation();
    }
}
