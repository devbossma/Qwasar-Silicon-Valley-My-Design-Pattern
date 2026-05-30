package dev.saberlabs;

import dev.saberlabs.adapter.AdapterDemo;
import dev.saberlabs.cli.CoffeeShopCLI;
import dev.saberlabs.command.CommandDemo;
import dev.saberlabs.decorator.DecoratorDemo;
import dev.saberlabs.facade.FacadeDemo;
import dev.saberlabs.factory.FactoryMethodDemo;
import dev.saberlabs.observer.ObserverDemo;
import dev.saberlabs.prototype.PrototypeDemo;
import dev.saberlabs.singleton.SingletonDemo;
import dev.saberlabs.strategy.StrategyDemo;
import dev.saberlabs.template.TemplateMethodDemo;

public class CoffeeShopApplication {

    static void main(String[] args) {
//        new CoffeeShopCLI().run();
        System.out.println("=== COFFEE SHOP — DESIGN PATTERNS DEMO ===\n");


        // ---------------------------------------------------------------
        // 1. SINGLETON — single CoffeeShop instance
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("1. SINGLETON — single CoffeeShop instance");
        System.out.println("---------------------------------------------------------------");

        SingletonDemo.run();

        // ---------------------------------------------------------------
        // 2. FACTORY METHOD — create coffees without knowing concrete classes
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("2. FACTORY METHOD — create coffees without knowing concrete classes");
        System.out.println("---------------------------------------------------------------");

        FactoryMethodDemo.run();

        // ---------------------------------------------------------------
        // 3. DECORATOR — add extras to a coffee
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("3. DECORATOR — add extras to a coffee");
        System.out.println("---------------------------------------------------------------");

        DecoratorDemo.run();

        // ---------------------------------------------------------------
        // 4. Prototype - Cloning Yassine's order
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("4. Prototype - Cloning Alice's order");
        System.out.println("---------------------------------------------------------------");

        PrototypeDemo.run();

        // ---------------------------------------------------------------
        // 5. Template Method
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("5. Template Method - Preparing Coffee");
        System.out.println("---------------------------------------------------------------");

        TemplateMethodDemo.run();

        // ---------------------------------------------------------------
        // 6. STRATEGY — different pricing strategies based on customer loyalty tiers
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("6. STRATEGY — different pricing strategies");
        System.out.println("------------------------------------------------------------");

        StrategyDemo.run();

        // ---------------------------------------------------------------
        // 7. OBSERVER — subscribe customers for notifications
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("7. OBSERVER — subscribe customers for notifications");
        System.out.println("---------------------------------------------------------------");

        ObserverDemo.run();

        // ---------------------------------------------------------------
        // 8 COMMAND - encapsulate order processing steps and support undo
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("8 COMMAND — encapsulate order processing steps and support undo");
        System.out.println("---------------------------------------------------------------");

        CommandDemo.run();

        // ---------------------------------------------------------------
        // 9 ADAPTER — provide a unified interface for different payment services
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("9 ADAPTER — provide a unified interface for different payment services");
        System.out.println("---------------------------------------------------------------");

        AdapterDemo.run();

        // ---------------------------------------------------------------
        // 10. Facade - Pattern
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("10 Facade — provide a simplified interface to a complex subsystem");
        System.out.println("---------------------------------------------------------------");

        FacadeDemo.run();





    }
}
