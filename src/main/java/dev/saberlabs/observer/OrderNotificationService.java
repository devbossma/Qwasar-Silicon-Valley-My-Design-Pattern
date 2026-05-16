package dev.saberlabs.observer;

import dev.saberlabs.models.Order;

import java.util.ArrayList;
import java.util.List;

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
