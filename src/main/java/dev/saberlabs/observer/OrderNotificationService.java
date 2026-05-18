package dev.saberlabs.observer;

import dev.saberlabs.models.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern 7: OBSERVER (Subject)
 *
 * The OrderNotificationService class implements the Observable interface and manages a list of observers.
 * It notifies all registered observers whenever there is a change in the order status.
 * This allows for a decoupled design where the OrderNotificationService does not need to know
 * about the specific observers, and observers can be added or removed dynamically at runtime.
 * */
public class OrderNotificationService implements Observable {

    private final List<OrderObserver> observers = new ArrayList<>();

    // Observer pattern methods to manage observers and notify them of status changes.
    @Override
    public void registerObserver(OrderObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(OrderObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Order order) {
        for (OrderObserver observer : observers) {
            observer.update(order, order.getStatus());
        }
    }
}
