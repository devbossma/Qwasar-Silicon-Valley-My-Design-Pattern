package dev.saberlabs.adapter;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pattern 9: ADAPTER (Adapter class)
 *
 * Adapts the PayPalPaymentService (cents + orderId) to our
 * PaymentGateway interface (dollars + orderId).
 */
public class PayPalAdapter implements PaymentGateway {

    /**
     * The PayPalPaymentService we are adapting to our PaymentGateway interface.
     * This service expects payments in cents and uses an orderId as a reference.
     */
    private final PayPalPaymentService gateway;
    private final Map<String, PaymentStatus> orderHistory = new ConcurrentHashMap<>();

    public PayPalAdapter (PayPalPaymentService payPalGateway){
        this.gateway = payPalGateway;
    }


    @Override
    public boolean processPayment(String orderId, double amountInDollars) {
        int amountInCents = (int) Math.round(amountInDollars * 100);
        boolean result = gateway.makePayment(amountInCents, orderId);
        orderHistory.put(orderId, result ? PaymentStatus.PAYMENT_COMPLETE : PaymentStatus.PAYMENT_FAILED);
        return result;
    }

    @Override
    public PaymentStatus getPaymentStatus(String orderId) {
        if (!orderHistory.containsKey(orderId)) {
            throw new RuntimeException("[Payment Exception] No payment found for order ID: " + orderId);
        }
        return orderHistory.getOrDefault(orderId, PaymentStatus.PAYMENT_FAILED);
    }

}
