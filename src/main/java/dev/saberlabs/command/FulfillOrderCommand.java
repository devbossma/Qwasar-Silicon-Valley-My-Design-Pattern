package dev.saberlabs.command;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pattern 8: COMMAND - Concrete Command
 *
 * <p>Marks an {@link dev.saberlabs.models.Order} as {@code FULFILLED}, removes the customer
 * from the {@link dev.saberlabs.singleton.CoffeeShop} observer list, and stores the previous
 * status so {@link #undo()} can revert the order and re-register the customer as an observer.
 */
public class FulfillOrderCommand implements Command {

    private final Order order;
    private OrderStatus previousStatus;

    public FulfillOrderCommand(@NotNull Order order) {
        this.order = Objects.requireNonNull(order, "Order cannot be null");
    }

    @Override
    public void execute() {
        previousStatus = order.getStatus();
        order.setStatus(OrderStatus.FULFILLED);
        System.out.println("["+getCommandName()+"] Order fulfilled: " + order);
        CoffeeShop.getInstance().removeObserver(order.getCustomer());
    }

    @Override
    public void undo() {
        order.setStatus(previousStatus);
        CoffeeShop.getInstance().registerObserver(order.getCustomer());
        System.out.println("["+getCommandName()+"] Reverted to: " + previousStatus);
    }

    @Override
    public String getCommandName() {
        return "FulfillOrderCommand";
    }
}
