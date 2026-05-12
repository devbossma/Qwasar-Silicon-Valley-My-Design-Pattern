package dev.saberlabs.factory;

import dev.saberlabs.model.Coffee;
import dev.saberlabs.model.Cappuccino;

public class CappuccinoCreator extends CoffeeCreator {
    @Override
    public Coffee createCoffee() {
        return new Cappuccino();
    }
}
