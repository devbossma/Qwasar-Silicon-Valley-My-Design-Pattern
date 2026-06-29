package dev.saberlabs.payment;

import dev.saberlabs.adapter.CashPaymentAdapter;
import dev.saberlabs.adapter.CashPaymentService;
import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.adapter.StripeAdapter;
import dev.saberlabs.adapter.StripePaymentService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Scanner;

/**
 * Console prompts for constructing a {@link PaymentGateway} adapter from
 * user input. Shared by any view that needs to collect payment — currently
 * {@code BaristaView}, originally written for the design-patterns project's
 * {@code CoffeeShopCLI}.
 *
 * Pure utility: no view state, no side effects beyond reading from the
 * given Scanner and printing prompts to System.out.
 */
public final class PaymentSetup {

    private PaymentSetup() {
        // utility class — no instances
    }

    /**
     * Prompts for a payment method (1=Cash, 2=PayPal, 3=Stripe) and the
     * corresponding details, then constructs the matching adapter.
     *
     * @param scanner   the console input source
     * @param amountDue the amount owed — only used by the cash flow
     * @return the configured PaymentGateway, or null if the user cancels
     *         or enters an invalid method choice
     */
    public static @Nullable PaymentGateway promptForPaymentMethod(@NotNull Scanner scanner,
                                                                  double amountDue) {
        System.out.println("  Payment method:");
        System.out.println("    1. Cash");
        System.out.println("    2. PayPal");
        System.out.println("    3. Credit Card (Stripe)");
        System.out.println("    0. Cancel");
        System.out.print("  Choice: ");

        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> setupCashPayment(scanner, amountDue);
            case "2" -> setupPayPalPayment(scanner);
            case "3" -> setupStripePayment(scanner);
            default -> null;
        };
    }

    /**
     * Prompts for cash received and constructs a CashPaymentAdapter.
     *
     * @param scanner   the console input source
     * @param amountDue the amount the customer owes
     * @return a configured CashPaymentAdapter
     */
    public static @NotNull PaymentGateway setupCashPayment(@NotNull Scanner scanner,
                                                           double amountDue) {
        System.out.printf("  Amount due: $%.2f%n", amountDue);
        System.out.print("  Cash received: $");
        double received;
        try {
            received = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Invalid amount, defaulting to exact change.");
            received = amountDue;
        }

        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(received);
        CashPaymentAdapter adapter = new CashPaymentAdapter(cashService);

        if (received >= amountDue) {
            System.out.printf("  Change to return: $%.2f%n", received - amountDue);
        } else {
            System.out.println("  Warning: insufficient cash — payment will fail.");
        }

        return adapter;
    }

    /**
     * Prompts for PayPal credentials and constructs a PayPalAdapter.
     *
     * @param scanner the console input source
     * @return a configured PayPalAdapter
     */
    public static @NotNull PaymentGateway setupPayPalPayment(@NotNull Scanner scanner) {
        System.out.print("  PayPal email: ");
        String email = scanner.nextLine().trim();
        System.out.print("  PayPal password: ");
        String password = scanner.nextLine().trim();

        PayPalPaymentService paypalService = new PayPalPaymentService(email, password);
        return new PayPalAdapter(paypalService);
    }

    /**
     * Prompts for credit card details and constructs a StripeAdapter.
     *
     * @param scanner the console input source
     * @return a configured StripeAdapter
     */
    public static @NotNull PaymentGateway setupStripePayment(@NotNull Scanner scanner) {
        System.out.print("  Card number (16 digits): ");
        String cardNumber = scanner.nextLine().trim();
        System.out.print("  Cardholder name: ");
        String cardHolder = scanner.nextLine().trim();
        System.out.print("  Expiry month (MM): ");
        String expMonth = scanner.nextLine().trim();
        System.out.print("  Expiry year (YYYY): ");
        String expYear = scanner.nextLine().trim();
        System.out.print("  CVV (3 digits): ");
        String cvv = scanner.nextLine().trim();

        StripePaymentService stripeService = new StripePaymentService(
                cardNumber, cardHolder, expMonth, expYear, cvv);
        return new StripeAdapter(stripeService);
    }
}