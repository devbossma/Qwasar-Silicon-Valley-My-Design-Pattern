package dev.saberlabs.factory;

import dev.saberlabs.models.Cappuccino;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Latte;
import dev.saberlabs.template.CappuccinoPreparation;
import dev.saberlabs.template.EspressoPreparation;
import dev.saberlabs.template.LattePreparation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Factory Method Pattern")
class FactoryMethodTest {

    @Test
    @DisplayName("EspressoCreator creates Espresso")
    void createEspresso() {
        CoffeeCreator creator = new EspressoCreator();
        Coffee c = creator.createCoffee();
        assertInstanceOf(Espresso.class, c);
        assertEquals(2.50, c.getCost());
        assertInstanceOf(EspressoPreparation.class, c.getPreparation());
    }

    @Test
    @DisplayName("CappuccinoCreator creates Cappuccino")
    void createCappuccino() {
        CoffeeCreator creator = new CappuccinoCreator();
        Coffee c = creator.createCoffee();
        assertInstanceOf(Cappuccino.class, c);
        assertEquals(3.50, c.getCost());
        assertInstanceOf(CappuccinoPreparation.class, c.getPreparation());
    }

    @Test
    @DisplayName("LatteCreator creates Latte")
    void createLatte() {
        CoffeeCreator creator = new LatteCreator();
        Coffee c = creator.createCoffee();
        assertInstanceOf(Latte.class, c);
        assertEquals(4.00, c.getCost());
        assertInstanceOf(LattePreparation.class, c.getPreparation());
    }
}
