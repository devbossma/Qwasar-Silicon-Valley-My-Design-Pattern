package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;
import org.jetbrains.annotations.NotNull;

/**
 * Pattern 5: DECORATOR (Concrete Decorator)
 * *
 * Adds sugar to a coffee order.
 */
public class SugarDecorator extends CoffeeDecorator {

    private static final double SUGAR_COST = 0.25;

    public SugarDecorator(@NotNull Coffee coffee) {
        super(coffee);
    }

    @Override
    public @NotNull String getDescription() {
        return decoratedCoffee.getDescription() + " + Sugar";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + SUGAR_COST;
    }

    @Override
    public @NotNull Coffee cloneCoffee() {
        return new SugarDecorator(decoratedCoffee.cloneCoffee());
    }
}
