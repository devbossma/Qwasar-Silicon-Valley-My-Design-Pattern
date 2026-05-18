package dev.saberlabs.command;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;


/**
 * Pattern 8: COMMAND (Concrete Command)
 *
 * Implements the Command interface to place an order. This command can be executed
 * to place an order and can also be undone to cancel the order if needed.
 * The PlaceOrderCommand encapsulates the details of placing an order.
 */
public class PlaceOrderCommand implements Command {

    private final Order order;

    public PlaceOrderCommand(Order order) {
        this.order = order;
    }

    @Override
    public void execute() {
        CoffeeShop.getInstance().registerObserver(order.getCustomer());
        CoffeeShop.getInstance().placeOrder(order);
    }

    @Override
    public void undo() {
        order.setStatus(OrderStatus.CANCELLED);
        System.out.println("["+getCommandName()+"] Order cancelled: " + order);
    }

    @Override
    public String getCommandName() {
        return "PlaceOrderCommand";
    }
}
