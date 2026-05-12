package dev.saberlabs.factory;


import dev.saberlabs.model.Coffee;
import dev.saberlabs.model.Espresso;

public class EspressoCreator extends CoffeeCreator {

    @Override
    public Coffee createCoffee() {
        return new Espresso();
    }
}
