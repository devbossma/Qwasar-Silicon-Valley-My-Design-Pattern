package dev.saberlabs.models;

import dev.saberlabs.prototype.CloneableCoffee;
import dev.saberlabs.template.CoffeePreparationTemplate;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all coffee types.
 * Serves as the Component in the Decorator pattern and
 * the product interface for the Factory Method pattern.
 */
public interface Coffee extends CloneableCoffee {

    /**
     * Returns a description of the coffee, including its type and any added ingredients (if decorated).
     * @return the coffee description
     */
    @NotNull String getDescription();

    /**
     * Returns the cost of the coffee, including any added ingredients cost (Decorators cost).
     * @return the coffee cost
     */
    double getCost();

    /**
     * Returns the preparation template associated with this coffee type, which defines the steps to prepare it.
     * @return the coffee preparation template
     */
    @NotNull  CoffeePreparationTemplate getPreparation();
}
