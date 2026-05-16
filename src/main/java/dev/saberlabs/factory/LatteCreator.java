package dev.saberlabs.factory;


import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Latte;

public class LatteCreator extends CoffeeCreator {
    @Override
    public Coffee createCoffee() {
        return new Latte();
    }
}
