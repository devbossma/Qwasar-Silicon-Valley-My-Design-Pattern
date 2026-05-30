package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;

/**
 * Pattern 5: DECORATOR (Concrete Decorator)
 * *
 * Adds whipped cream to a coffee order.
 */
public class WhippedCreamDecorator extends CoffeeDecorator {

    private static final double WHIPPED_CREAM_COST = 0.75;

    public WhippedCreamDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + " + Whipped Cream";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + WHIPPED_CREAM_COST;
    }

    @Override
    public Coffee cloneCoffee() {
        return new WhippedCreamDecorator(decoratedCoffee.cloneCoffee());
    }
}
