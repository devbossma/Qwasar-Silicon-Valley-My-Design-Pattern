package dev.saberlabs.decorator;

import dev.saberlabs.model.Coffee;

/**
 * Pattern 5: DECORATOR (Concrete Decorator)
 *
 * Adds sugar to a coffee order.
 */
public class SugarDecorator extends CoffeeDecorator {

    private static final double SUGAR_COST = 0.25;

    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + " + Sugar";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + SUGAR_COST;
    }

    @Override
    public Coffee cloneOrder() {
        return new SugarDecorator(decoratedCoffee.cloneOrder());
    }
}
