package dev.saberlabs.models;

import dev.saberlabs.template.CoffeePreparationTemplate;
import dev.saberlabs.template.EspressoPreparation;
import org.jetbrains.annotations.NotNull;

/**
 * Concrete coffee: Cappuccino.
 */
public class Cappuccino implements Coffee {

    /**
     * Returns a description of the coffee, including its type and any added ingredients (if decorated).
     *
     * @return
     */
    @Override
    public @NotNull String getDescription() {
        return "Cappuccino";
    }

    @Override
    public double getCost() {
        return 3.50;
    }

    @Override
    public @NotNull Coffee cloneCoffee() {
        return new Cappuccino();
    }

    @Override
    public String toString() {
        return getDescription() + " ($" + String.format("%.2f", getCost()) + ")";
    }

    @Override

    public @NotNull CoffeePreparationTemplate getPreparation() {
        return new EspressoPreparation();
    }
}
