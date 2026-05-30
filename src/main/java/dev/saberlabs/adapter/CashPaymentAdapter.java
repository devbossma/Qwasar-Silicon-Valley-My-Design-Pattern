package dev.saberlabs.adapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Pattern 9: ADAPTER (Adapter class)
 *
 * Adapts the {@link CashPaymentService} to the {@link PaymentGateway} interface,
 * allowing cash payments to be processed through the same contract used by
 * digital payment methods (PayPal, Stripe).
 *
 * <h3>Interface Bridging</h3>
 * The CashPaymentService has an incompatible API:
 * <ul>
 *   <li>It requires {@code setAmountReceived()} to be called before processing.</li>
 *   <li>Its {@code collectCash()} method returns change (double), not success (boolean).</li>
 * </ul>
 * This adapter translates the cash register's response into the boolean result
 * that {@link PaymentGateway#processPayment(String, double)} expects.
 *
 * <h3>Change Tracking</h3>
 * Since cash transactions produce change that must be returned to the customer,
 * this adapter tracks change per transaction via {@link #getChange(String)}.
 * This is a cash-specific concern that does not exist in digital payment adapters,
 * so it lives on the adapter rather than the PaymentGateway interface.
 *
 * <h3>Usage</h3>
 * <pre>
 *     CashPaymentService cashService = new CashPaymentService();
 *     PaymentGateway gateway = new CashPaymentAdapter(cashService);
 *
 *     // Customer hands over $10 for a $3.50 order
 *     cashService.setAmountReceived(10.00);
 *     gateway.processPayment("ORDER-001", 3.50);
 *
 *     // Cashier checks change
 *     ((CashPaymentAdapter) gateway).getChange("ORDER-001"); // 6.50
 * </pre>
 */
public class CashPaymentAdapter implements PaymentGateway {
    /** The cash register service being adapted. */
    private final CashPaymentService cashService;

    /** Tracks payment status per order ID for later retrieval. */
    private final Map<String, PaymentStatus> paymentHistory = new HashMap<>();

    /** Tracks change owed per order ID for the cashier to return to the customer. */
    private final Map<String, Double> changeHistory = new HashMap<>();

    /**
     * Creates a new CashPaymentAdapter wrapping the given cash register service.
     *
     * @param cashService the cash register service to adapt
     */
    public CashPaymentAdapter(CashPaymentService cashService) {
        this.cashService = cashService;
    }

    /**
     * Processes a cash payment through the adapted cash register service.
     *
     * The amount received must be set on the {@link CashPaymentService} via
     * {@code setAmountReceived()} before calling this method. The adapter
     * translates the cash register's change-based response into a boolean:
     * - change >= 0 means success (true)
     * - change == -1 means insufficient cash (false)
     *
     * Both the payment status and any change owed are recorded per order ID.
     *
     * @param orderId          the unique order reference
     * @param amountInDollars  the total amount due
     * @return true if the payment was successful, false otherwise
     */
    @Override
    public boolean processPayment(String orderId, double amountInDollars) {
        double change = cashService.collectCash(amountInDollars);
        boolean success = change >= 0;
        paymentHistory.put(orderId, success ? PaymentStatus.PAYMENT_COMPLETE : PaymentStatus.PAYMENT_FAILED);
        changeHistory.put(orderId, success ? change : 0.00);
        return success;
    }

    /**
     * Returns the change owed to the customer for a specific transaction.
     * This is a cash-specific concern - digital payment adapters do not need this.
     *
     * @param orderId the order reference to look up
     * @return the change amount in dollars, or 0.00 if the order is unknown or failed
     */
    public double getChange(String orderId) {
        return changeHistory.getOrDefault(orderId, 0.00);
    }

    /**
     * Returns the payment status for a specific order.
     *
     * @param orderId the order reference to look up
     * @return {@link PaymentStatus#PAYMENT_COMPLETE} if paid,
     *         {@link PaymentStatus#PAYMENT_FAILED} if failed or unknown
     */
    @Override
    public PaymentStatus getPaymentStatus(String orderId) {
        return paymentHistory.getOrDefault(orderId, PaymentStatus.PAYMENT_FAILED);
    }
}
