package dev.saberlabs.observer;

import dev.saberlabs.models.Order;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

/**
 * Pattern 7: OBSERVER (Subject)
 *
 * The OrderNotificationService class implements the Observable interface and manages a list of observers.
 * It notifies all registered observers whenever there is a change in the order status.
 * This allows for a decoupled design where the OrderNotificationService does not need to know
 * about the specific observers, and observers can be added or removed dynamically at runtime.
 * */
public class OrderNotificationService implements Observable {

    private final Set<OrderObserver> observers = new CopyOnWriteArraySet<>();

    // Observer pattern methods to manage observers and notify them of status changes.
    @Override
    public void registerObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        observers.add(observer);
    }

    @Override
    public void removeObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(@NotNull Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        for (OrderObserver observer : observers) {
            observer.update(order, order.getStatus());
        }
    }

    public void clearObservers() {
        observers.clear();
    }
}
