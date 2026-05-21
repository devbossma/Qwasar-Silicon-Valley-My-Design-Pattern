package dev.saberlabs.command;

import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.adapter.PaymentStatus;
import dev.saberlabs.models.Order;

public class PayOrderCommand implements Command {

    private final Order order;
    private final PaymentGateway paymentGateway;
    private String orderId;

    public PayOrderCommand(Order order, PaymentGateway paymentGateway) {
        this.order = order;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public void execute() {
        orderId = "ORDER-" + order.getCustomer().getId();
        boolean result = paymentGateway.processPayment(orderId, order.getFinalPrice());
        if (!result) {
            throw new RuntimeException("[PayOrderCommand] Payment failed for: " + orderId);
        }
        System.out.printf("[PayOrderCommand] Payment of $%.2f collected from %s%n",
                order.getFinalPrice(), order.getCustomer().getName());
    }

    @Override
    public void undo() {
        System.out.printf("[PayOrderCommand] Payment of $%.2f refunded to %s%n",
                order.getFinalPrice(), order.getCustomer().getName());
    }

    @Override
    public String getCommandName() {
        return "PayOrderCommand";
    }

    public boolean isPaid() {
        return orderId != null
                && paymentGateway.getPaymentStatus(orderId) == PaymentStatus.PAYMENT_COMPLETE;
    }
}