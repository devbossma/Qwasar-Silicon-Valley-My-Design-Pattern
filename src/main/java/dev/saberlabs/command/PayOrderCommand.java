package dev.saberlabs.command;

import dev.saberlabs.models.Order;

public class PayOrderCommand implements Command {
    
    private final Order order;
    private boolean paid = false;

    public PayOrderCommand(Order order) {
        this.order = order;
    }

    @Override
    public void execute() {
        // Payment via Adapter will plug in here later
        paid = true;
        System.out.printf("["+getCommandName()+"] Payment of $%.2f collected from %s%n",
                order.getFinalPrice(), order.getCustomer().getName());
    }

    @Override
    public void undo() {
        paid = false;
        System.out.printf("["+getCommandName()+"] Payment of $%.2f refunded to %s%n",
                order.getFinalPrice(), order.getCustomer().getName());
    }

    @Override
    public String getCommandName() {
        return "PayOrderCommand";    }
}
