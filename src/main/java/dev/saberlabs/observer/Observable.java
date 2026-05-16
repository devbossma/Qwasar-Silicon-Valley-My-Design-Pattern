package dev.saberlabs.observer;

import dev.saberlabs.models.Order;


/**
 * Pattern 7: OBSERVER (Observable: The Subject Interface)
 * 
 * The Subject interface defines the methods for managing observers and notifying them of changes.
 * In this context, the Subject is typically The OrderNotificationService Class.
 * it needs to notify in Our case The Customer class.
 * but it could notify various observers, (e.g., InventoryManager, ShippingService ...) about Order updates.
 * */
public interface Observable {
    
    // Method to register observers.
    void registerObserver(OrderObserver observer);
    
    // Method to remove observers.
    void removeObserver(OrderObserver observer);
    
    // Method to notify all registered observers of a change in the order status.
    void notifyObservers(Order order);
}
