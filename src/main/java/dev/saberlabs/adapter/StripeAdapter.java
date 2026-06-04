package dev.saberlabs.adapter;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pattern 9: ADAPTER (Adapter class)
 *
 * Adapts the StripePaymentService (cents + order ID) to our
 * PaymentGateway interface (dollars + order ID).
 */
public class StripeAdapter implements PaymentGateway{

    /**
     * The StripePaymentService is the adaptee that we want to use,
     * but it has a different interface than what our system expects.
     * We will adapt it to fit our PaymentGateway interface.
     */
    private final StripePaymentService stripeGateway;

    private final Map<String, PaymentStatus> paymentHistory = new ConcurrentHashMap<>();

    public StripeAdapter(StripePaymentService paymentGateway) {
        this.stripeGateway = paymentGateway;
    }

    @Override
    public boolean processPayment(String orderId, double amountInDollars) {

        // Stripe's API might require a specific format for the order reference, so we create one based on the order ID.
        String orderRef = "STRIPE-" + orderId.toUpperCase();

        // Stripe expects amounts in cents, so we convert dollars to cents.
        int amountInCents = (int) Math.round(amountInDollars * 100);

        boolean result = stripeGateway.charge(amountInCents, orderRef);
        paymentHistory.put(orderId, result ? PaymentStatus.PAYMENT_COMPLETE : PaymentStatus.PAYMENT_FAILED);
        return result;
    }

    @Override
    public PaymentStatus getPaymentStatus(String orderId) {
        if (!paymentHistory.containsKey(orderId)) {
            throw new RuntimeException("[Payment Exception] No payment found for order ID: " + orderId);
        }
        return paymentHistory.getOrDefault(orderId, PaymentStatus.PAYMENT_FAILED);
    }
}
