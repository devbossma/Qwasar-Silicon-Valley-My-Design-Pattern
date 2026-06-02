package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;
import dev.saberlabs.template.CoffeePreparationTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pattern 3: DECORATOR (The Abstract Decorator)
 * Implements the same interface as the component it decorates (Coffee).
 * Wraps a Coffee object and delegates to it, allowing subclasses
 * to add behavior (extras) on top.
 */
public abstract class CoffeeDecorator implements Coffee {

    // The decorated Coffee object that we are wrapping.
    @NotNull protected final  Coffee decoratedCoffee;

    protected CoffeeDecorator(@NotNull Coffee coffee) {
        Objects.requireNonNull(coffee, "Decorated Coffee cannot be null");
        this.decoratedCoffee = coffee;
    }

    @Override
    @NotNull
    public String getDescription() {
        return decoratedCoffee.getDescription();
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();
    }

    @Override
    @NotNull
    public CoffeePreparationTemplate getPreparation() {
        return decoratedCoffee.getPreparation();
    }
}
