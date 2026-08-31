package dev.saberlabs.framework;

import dev.saberlabs.framework.annotation.ChatHandler;
import dev.saberlabs.framework.annotation.OrderHandler;
import dev.saberlabs.framework.reflection.InteractionHandler;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Demo client)
 * *
 * Standalone demo of the assignment's literal shape: a naive, string-typed request routed to
 * a {@code String}-only handler method purely through reflection, for two toy business types.
 * The coffee shop's own {@code BusinessObject} is {@code dev.saberlabs.chat.CoffeeShopBusiness} —
 * real order placement and real chat messages, not a toy demo; see its own test class rather
 * than a third toy type here. See {@code framework/doc.md}.
 */
public class BusinessTestClient {
    public static void main(String[] args) {
        InteractionHandler handler = new InteractionHandler();

        BookStoreBusiness bookStore = new BookStoreBusiness();
        OnlineShopBusiness onlineShop = new OnlineShopBusiness();

        // The coffee shop's own BusinessObject is dev.saberlabs.chat.CoffeeShopBusiness — real
        // order placement and real chat messages, not a toy List<String> stand-in like these two.
        // See dev.saberlabs.chat.CoffeeShopBusinessTest for its coverage.

        // Demonstrate the BookStoreBusiness handling an order and a chat request
        System.out.println("--- BookStoreBusiness Demo ---");

        handler.handleInteraction(bookStore, "order", "Clean Code by Robert C. Martin");
        handler.handleInteraction(bookStore, "chat", "Hello, bookstore!");
        handler.handleInteraction(bookStore, "feedback", "Great service!");

        // Demonstrate the OnlineShopBusiness handling an order and a chat request
        System.out.println("\n--- OnlineShopBusiness Demo ---");

        handler.handleInteraction(onlineShop, "order", "Laptop");
        handler.handleInteraction(onlineShop, "chat", "Hello, online shop!");
        handler.handleInteraction(onlineShop, "feedback", "Great service!");
    }

    /**
     * A simple bookstore facade for demonstration purposes.
     * that demonstrates how to handle order and chat requests using custom annotations.
     * It serves as an example of how to extend the reflection framework to other business types.     *
     */
    public static class BookStoreBusiness implements BusinessObject {
        @OrderHandler
        public void handleOrder(String request) {
            System.out.println("BookStoreFacade handling order: " + request);
        }

        @ChatHandler
        public void handleChat(String request) {
            System.out.println("BookStoreBusiness handling chat: " + request);
        }

        @Override
        public void processRequest(String request) {
            System.out.println("BookStoreBusiness handling unknown request: " + request);
        }
    }


    /**
     * OnlineShopFacade is a simple implementation of the BusinessObject interface.
     */
    public static class OnlineShopBusiness implements BusinessObject {
        @OrderHandler
        public void handleOrder(String request) {
            System.out.println("OnlineShopBusiness handling order: " + request);
        }

        @ChatHandler
        public void handleChat(String request) {
            System.out.println("OnlineShopBusiness handling chat: " + request);
        }

        @Override
        public void processRequest(String request) {
            System.out.println("OnlineShopBusiness handling unknown request: " + request);
        }
    }

}
