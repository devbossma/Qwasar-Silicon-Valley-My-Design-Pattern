package dev.saberlabs.framework;

import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.models.Order;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Demo client)
 * *
 * Standalone demo showing the framework dispatching requests to a real
 * {@link CoffeeShopFacade} purely through reflection. The "order" request runs the
 * exact same lifecycle every other order in this project goes through — Factory
 * Method, Decorator, Strategy, Singleton, and the full Command chain (Prepare via
 * Template Method, Pay via Adapter, Fulfill via Observer) — so the console output
 * below looks identical to a normal {@code facade.placeOrder(...); facade.processOrder(...)}
 * call; the only difference is that {@link InteractionHandler} found the method
 * reflectively instead of the caller invoking it directly. The "feedback" request
 * shows the fallback behavior for a request type with no matching handler.
 */
public class BusinessTestClient {
    public static void main(String[] args) {
        InteractionHandler handler = new InteractionHandler();
        CoffeeShopFacade coffeeShop = new CoffeeShopFacade(
                new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass")));

        // Additional facades for demonstration purposes
        BookStoreFacade bookStore = new BookStoreFacade();
        OnlineShopFacade onlineShop = new OnlineShopFacade();

        // Demonstrate the CoffeeShopFacade handling an order and a chat request
        System.out.println("--- CoffeeShopFacade Demo ---");

        handler.handleInteraction(coffeeShop, "order", "1 Cappuccino");
        handler.handleInteraction(coffeeShop, "chat", "Hello, barista!");
        handler.handleInteraction(coffeeShop, "feedback", "Great service!");

        Order order = coffeeShop.getAllOrders().getFirst();

        System.out.println("Orders placed: " + coffeeShop.getAllOrders().size());
        System.out.println("Order status (Command + Template Method + Adapter + Observer ran): " + order.getStatus());
        System.out.println("Command history: " + coffeeShop.getInvoker().getCommandHistory().size() + " commands");
        System.out.println("Chat log: " + coffeeShop.getChatLog());

        // Demonstrate the BookStoreFacade handling an order and a chat request
        System.out.println("\n--- BookStoreFacade Demo ---");

        handler.handleInteraction(bookStore, "order", "Clean Code by Robert C. Martin");
        handler.handleInteraction(bookStore, "chat", "Hello, bookstore!");
        handler.handleInteraction(bookStore, "feedback", "Great service!");

        // Demonstrate the OnlineShopFacade handling an order and a chat request
        System.out.println("\n--- OnlineShopFacade Demo ---");

        handler.handleInteraction(onlineShop, "order", "Laptop");
        handler.handleInteraction(onlineShop, "chat", "Hello, online shop!");
        handler.handleInteraction(onlineShop, "feedback", "Great service!");
    }

    /**
     * A simple bookstore facade for demonstration purposes.
     * that demonstrates how to handle order and chat requests using custom annotations.
     * It serves as an example of how to extend the reflection framework to other business types.     *
     */
    static class BookStoreFacade implements BusinessObject {
        @OrderHandler
        public void handleOrder(String request) {
            System.out.println("BookStoreFacade handling order: " + request);
        }

        @ChatHandler
        public void handleChat(String request) {
            System.out.println("BookStoreFacade handling chat: " + request);
        }

        @Override
        public void processRequest(String request) {}
    }


    /**
     * OnlineShopFacade is a simple implementation of the BusinessObject interface.
     */
    static class OnlineShopFacade implements BusinessObject {
        @OrderHandler
        public void handleOrder(String request) {
            System.out.println("OnlineShopFacade handling order: " + request);
        }

        @ChatHandler
        public void handleChat(String request) {
            System.out.println("OnlineShopFacade handling chat: " + request);
        }

        @Override
        public void processRequest(String request) {}
    }

}
