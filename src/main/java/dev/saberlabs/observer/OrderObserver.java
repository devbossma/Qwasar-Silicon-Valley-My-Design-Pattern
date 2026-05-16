package dev.saberlabs.observer;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;

/**
 * Pattern 7: OBSERVER (Observer interface)
 *
 * Any class that wants to be notified of order status changes
 * implements this interface.
 */
public interface OrderObserver {

    void update(Order order, OrderStatus event);
}
