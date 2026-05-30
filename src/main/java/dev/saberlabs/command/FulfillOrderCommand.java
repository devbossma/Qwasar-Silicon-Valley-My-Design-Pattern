package dev.saberlabs.command;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;

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

    public FulfillOrderCommand( Order order) {
        this.order = order;
    }

    @Override
    public void execute() {
        previousStatus = order.getStatus();
        System.out.println("["+getCommandName()+"] Order fulfilled: " + order);
        order.setStatus(OrderStatus.FULFILLED);
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