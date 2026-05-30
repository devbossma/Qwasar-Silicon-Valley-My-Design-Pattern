package dev.saberlabs.adapter;

/**
 * Pattern 7: ADAPTER — Normalised Payment Result
 *
 * <p>Common status that all three adapters ({@link PayPalAdapter}, {@link StripeAdapter},
 * {@link CashPaymentAdapter}) map their provider-specific responses to, so the rest of
 * the system never depends on provider-specific return types.
 */
public enum PaymentStatus {
    PAYMENT_COMPLETE,
    PAYMENT_FAILED,
    PAYMENT_PENDING,
}
