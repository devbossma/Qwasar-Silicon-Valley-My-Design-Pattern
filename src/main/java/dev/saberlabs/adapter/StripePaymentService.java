package dev.saberlabs.adapter;

import java.time.LocalDate;

/**
 * Pattern 9: ADAPTER (Class Adaptee)
 *
 * Simulates a Stripe payment gateway with an incompatible API.
 * It uses "cents" and a different method signature than our system expects.
 */
public class StripePaymentService {

    private final String cardNumber;
    private final String cardHolderName;
    private final String expirationMonth;
    private final String expirationYear;
    private final String cvv;

    // For testing purposes, we simulate a balance to track charges. In a real implementation, this would be managed by Stripe's API.
    private double balance = 100000.00; // Simulated balance for testing purposes, The Stripe API would handle this in a real implementation.



    public StripePaymentService(String cardNumber, String cardHolderName, String expirationMonth, String expirationYear, String cvv) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.cvv = cvv;
    }

    public boolean charge(int amountInCents, String OrderReference) {
        if(isCardValid()) {
            double amountInDollars = amountInCents / 100.0;
            if (balance >= amountInDollars) {
                balance -= amountInDollars; // Simulate charging the card
                System.out.printf("[StripePayment] Charged $%.2f for ref: %s%n", amountInDollars, OrderReference);
                return true;
            } else {
                System.out.println("[StripePayment] Insufficient funds.");
                return false;
            }
        } else {
            System.out.println("[StripePayment] Invalid card details.");
            return false;
        }
    }

    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance) {
        this.balance = balance;
    }

    private boolean isCardValid() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        int expMonth = Integer.parseInt(expirationMonth);
        int expYear = Integer.parseInt(expirationYear);

        boolean isCardNumberValid = cardNumber.matches("\\d{16}");
        boolean isCvvValid = cvv.matches("\\d{3}");
        boolean isCardHolderNameValid = !cardHolderName.trim().isEmpty();

        // Card is valid if expiration is in the future
        boolean isNotExpired = (expYear > currentYear)
                || (expYear == currentYear && expMonth > currentMonth);

        return isCardNumberValid
                && isNotExpired
                && isCvvValid
                && isCardHolderNameValid;
    }
}
