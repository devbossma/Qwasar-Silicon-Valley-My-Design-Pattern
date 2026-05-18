package dev.saberlabs.command;


import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;

/**
 * Pattern 8: COMMAND (Concrete Command)
 *
 * Implements the Command interface to prepare an order. This command can be executed
 * The PrepareOrderCommand encapsulates the details of preparing an order.
 * to set the order status to READY and can also be undone to revert the status back to its previous state if needed.
 */
public class PrepareOrderCommand implements Command {
    private final Order order;
    private OrderStatus previousStatus;

    public PrepareOrderCommand( Order order) {
        this.order = order;
    }

    @Override
    public void execute() {
        previousStatus = order.getStatus();
        System.out.println("["+getCommandName()+"]  Preparing \"" + order.getCoffee().getDescription() + "\" For "+ order.getCustomer().getName()+"! ...");
        order.setStatus(OrderStatus.PREPARING);
        System.out.println("["+getCommandName()+"] Order is being prepared: " + order);
        order.setStatus(OrderStatus.READY);
    }

    @Override
    public void undo() {
        order.setStatus(previousStatus);
        System.out.println("["+getCommandName()+"] Reverted to: " + previousStatus);
    }

    @Override
    public String getCommandName() {
        return "PrepareOrderCommand";
    }
}
