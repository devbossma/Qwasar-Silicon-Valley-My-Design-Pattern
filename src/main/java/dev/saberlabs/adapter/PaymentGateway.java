package dev.saberlabs.adapter;

/**
 * Pattern 9: ADAPTER (Target interface)
 *
 * The interface our coffee shop system expects for processing payments.
 * Adapters bridge third-party services to this interface.
 */
public interface PaymentGateway {
    boolean processPayment(String orderId, double amountInDollars);
    PaymentStatus getPaymentStatus(String orderId);
}
