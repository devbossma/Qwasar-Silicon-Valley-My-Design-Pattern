package dev.saberlabs.command;

import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.adapter.PaymentStatus;
import dev.saberlabs.models.Order;

/**
 * Pattern 8: COMMAND - Concrete Command
 *
 * <p>Processes payment for an {@link dev.saberlabs.models.Order} through a
 * {@link dev.saberlabs.adapter.PaymentGateway} (PayPal, Stripe, or Cash adapter).
 * Constructs the order reference as {@code "ORDER-<id>"} and throws a
 * {@link RuntimeException} if the gateway rejects the charge. Undo simulates a refund
 * message; the gateway has no real reversal API.
 */
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
        orderId = "ORDER-" + order.getOrderId();
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