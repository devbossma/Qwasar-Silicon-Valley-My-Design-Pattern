package dev.saberlabs.command;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;

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
    }

    @Override
    public void undo() {
        order.setStatus(previousStatus);
        System.out.println("["+getCommandName()+"] Reverted to: " + previousStatus);
    }

    @Override
    public String getCommandName() {
        return "FulfillOrderCommand";
    }
}