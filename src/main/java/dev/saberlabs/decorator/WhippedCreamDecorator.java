package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;
import org.jetbrains.annotations.NotNull;

/**
 * Pattern 5: DECORATOR (Concrete Decorator)
 * *
 * Adds whipped cream to a coffee order.
 */
public class WhippedCreamDecorator extends CoffeeDecorator {

    private static final double WHIPPED_CREAM_COST = 0.75;

    public WhippedCreamDecorator(@NotNull  Coffee coffee) {
        super(coffee);
    }

    @Override
    public @NotNull String getDescription() {
        return decoratedCoffee.getDescription() + " + Whipped Cream";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + WHIPPED_CREAM_COST;
    }

    @Override
    public @NotNull Coffee cloneCoffee() {
        return new WhippedCreamDecorator(decoratedCoffee.cloneCoffee());
    }
}
