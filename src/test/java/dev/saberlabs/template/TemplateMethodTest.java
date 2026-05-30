package dev.saberlabs.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Template Method Pattern")
class TemplateMethodTest {

    @Test
    @DisplayName("all preparations follow the same step sequence")
    void fixedAlgorithmStructure() {
        EspressoPreparation espresso = new EspressoPreparation();
        CappuccinoPreparation cappuccino = new CappuccinoPreparation();
        LattePreparation latte = new LattePreparation();

        espresso.prepareCoffee();
        cappuccino.prepareCoffee();
        latte.prepareCoffee();

        for (var prep : List.of(espresso, cappuccino, latte)) {
            List<String> log = prep.getPreparationLog();
            assertTrue(log.getFirst().contains("Boiling water"));
            assertTrue(log.getLast().contains("is ready!"));
        }
    }

    @Test
    @DisplayName("EspressoPreparation uses correct temperature and duration")
    void espressoTemperatureAndDuration() {
        EspressoPreparation prep = new EspressoPreparation();
        prep.prepareCoffee();

        String boilStep = prep.getPreparationLog().getFirst();
        assertTrue(boilStep.contains("95°C"));
        assertTrue(boilStep.contains("25 seconds"));
    }

    @Test
    @DisplayName("CappuccinoPreparation uses correct temperature and duration")
    void cappuccinoTemperatureAndDuration() {
        CappuccinoPreparation prep = new CappuccinoPreparation();
        prep.prepareCoffee();

        String boilStep = prep.getPreparationLog().getFirst();
        assertTrue(boilStep.contains("90°C"));
        assertTrue(boilStep.contains("30 seconds"));
    }

    @Test
    @DisplayName("LattePreparation uses correct temperature and duration")
    void latteTemperatureAndDuration() {
        LattePreparation prep = new LattePreparation();
        prep.prepareCoffee();

        String boilStep = prep.getPreparationLog().getFirst();
        assertTrue(boilStep.contains("93°C"));
        assertTrue(boilStep.contains("28 seconds"));
    }

    @Test
    @DisplayName("EspressoPreparation has no condiments")
    void espressoNoCondiments() {
        EspressoPreparation prep = new EspressoPreparation();
        prep.prepareCoffee();

        assertTrue(prep.getPreparationLog().stream()
                .anyMatch(s -> s.contains("No condiments")));
    }

    @Test
    @DisplayName("CappuccinoPreparation includes brewing, steaming, and assembly")
    void cappuccinoBrewSteps() {
        CappuccinoPreparation prep = new CappuccinoPreparation();
        prep.prepareCoffee();

        List<String> log = prep.getPreparationLog();
        assertTrue(log.stream().anyMatch(s -> s.contains("brewing process for cappuccino")));
        assertTrue(log.stream().anyMatch(s -> s.contains("Steaming milk for cappuccino")));
        assertTrue(log.stream().anyMatch(s -> s.contains("Assembling the cappuccino")));
    }

    @Test
    @DisplayName("LattePreparation includes brewing, steaming, and assembly")
    void latteBrewSteps() {
        LattePreparation prep = new LattePreparation();
        prep.prepareCoffee();

        List<String> log = prep.getPreparationLog();
        assertTrue(log.stream().anyMatch(s -> s.contains("brewing process for latte")));
        assertTrue(log.stream().anyMatch(s -> s.contains("Steaming milk for latte")));
        assertTrue(log.stream().anyMatch(s -> s.contains("Assembling the latte")));
    }

    @Test
    @DisplayName("CappuccinoPreparation adds cocoa and cinnamon condiments")
    void cappuccinoCondiments() {
        CappuccinoPreparation prep = new CappuccinoPreparation();
        prep.prepareCoffee();

        assertTrue(prep.getPreparationLog().stream()
                .anyMatch(s -> s.contains("condiments for cappuccino")));
    }

    @Test
    @DisplayName("LattePreparation adds vanilla and cocoa condiments")
    void latteCondiments() {
        LattePreparation prep = new LattePreparation();
        prep.prepareCoffee();

        assertTrue(prep.getPreparationLog().stream()
                .anyMatch(s -> s.contains("condiments for latte")));
    }

    @Test
    @DisplayName("each preparation type produces a different number of steps")
    void differentStepCounts() {
        CoffeePreparationTemplate espresso = new EspressoPreparation();
        CoffeePreparationTemplate cappuccino = new CappuccinoPreparation();
        CoffeePreparationTemplate latte = new LattePreparation();

        espresso.prepareCoffee();
        cappuccino.prepareCoffee();
        latte.prepareCoffee();

        assertTrue(espresso.getPreparationLog().size() < cappuccino.getPreparationLog().size());
        assertTrue(espresso.getPreparationLog().size() < latte.getPreparationLog().size());
    }
}
