package dev.saberlabs.factory;


import dev.saberlabs.model.Coffee;
import dev.saberlabs.model.Latte;

public class LatteCreator extends CoffeeCreator {
    @Override
    public Coffee createCoffee() {
        return new Latte();
    }
}
