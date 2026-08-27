package dev.saberlabs.framework.example;

import dev.saberlabs.framework.example.annotation.ChatHandler;
import dev.saberlabs.framework.example.annotation.OrderHandler;
import dev.saberlabs.framework.example.reflection.InteractionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern 11: REFLECTION FRAMEWORK — EXAMPLE (Demo client)
 * *
 * Standalone demo of the assignment's literal shape: a naive, string-typed request routed to
 * a {@code String}-only handler method purely through reflection. This is intentionally kept
 * separate from the real application — see {@code dev.saberlabs.framework.business} and
 * {@code CoffeeShopFacade} for the typed version {@code ChatService}/the CLI/FX apps actually
 * build on, and {@code framework/doc.md} for why the split exists.
 */
public class BusinessTestClient {
    public static void main(String[] args) {
        InteractionHandler handler = new InteractionHandler();

        BookStoreBusiness bookStore = new BookStoreBusiness();
        OnlineShopBusiness onlineShop = new OnlineShopBusiness();
        CoffeeShopBusiness coffeeShop = new CoffeeShopBusiness();

        // Demonstrate the CoffeeShopBusiness handling an order and a chat request
        System.out.println("--- CoffeeShopBusiness Demo ---");
        handler.handleInteraction(coffeeShop, "order", "Cappuccino");
        handler.handleInteraction(coffeeShop, "chat", "Hello, coffee shop!");
        handler.handleInteraction(coffeeShop, "feedback", "Great service!");

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

    public static class CoffeeShopBusiness implements BusinessObject {
        List<String> orders = new ArrayList<>();
        List<String> feedbacks = new ArrayList<>();
        List<String> chats = new ArrayList<>();

        private String sendFeedback(String feedback) {
            feedbacks.add(feedback);
            return "Feedback received: " + feedback;
        }

        private String placeOrder(String order) {
            orders.add(order);
            return "Order placed: " + order;
        }

        private String sendMessage(String message) {
            chats.add(message);
            return "Message sent: " + message;
        }

        @OrderHandler
        public void handleOrder(String request) {
            System.out.println("CoffeeShopBusiness handling order: " + placeOrder(request));
        }

        @ChatHandler
        public void handleChat(String request) {
            System.out.println("CoffeeShopBusiness handling chat: " + sendMessage(request));
        }

        @Override
        public void processRequest(String request) {
            System.out.println("CoffeeShopBusiness processing request: " + sendFeedback(request));
        }
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
        public void processRequest(String request) {}
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
        public void processRequest(String request) {}
    }

}
