package dev.saberlabs.models;

import dev.saberlabs.template.CoffeePreparationTemplate;
import dev.saberlabs.template.LattePreparation;
import org.jetbrains.annotations.NotNull;

/**
 * Concrete coffee: Latte.
 */
public class Latte implements Coffee {

    @Override
    public @NotNull String getDescription() {
        return "Latte";
    }

    @Override
    public double getCost() {
        return 4.00;
    }

    @Override
    public @NotNull Coffee cloneCoffee() {
        return new Latte();
    }

    @Override
    public String toString() {
        return getDescription() + " ($" + String.format("%.2f", getCost()) + ")";
    }

    @Override
    public @NotNull CoffeePreparationTemplate getPreparation() {
        return new LattePreparation();
    }
}
