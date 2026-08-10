package dev.saberlabs.adapter;

/**
 * Pattern 9: ADAPTER (Target interface support)
 *
 * Thrown by {@link PaymentGateway#getPaymentStatus(String)} when no payment
 * has been recorded for the given order ID. Distinct from a generic
 * {@link RuntimeException} so callers can catch this specific, expected
 * condition rather than treating it as an unrecoverable bug.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String orderId) {
        super("[Payment Exception] No payment found for order ID: " + orderId);
    }
}
