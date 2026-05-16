package dev.saberlabs.factory;

import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Cappuccino;

public class CappuccinoCreator extends CoffeeCreator {
    @Override
    public Coffee createCoffee() {
        return new Cappuccino();
    }
}
