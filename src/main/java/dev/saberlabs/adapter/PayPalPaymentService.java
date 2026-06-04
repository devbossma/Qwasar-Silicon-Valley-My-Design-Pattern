package dev.saberlabs.adapter;


/**
 * Pattern 9: ADAPTER (Class Adaptee)
 * *
 * Simulates a PayPal payment gateway with an incompatible API.
 * It uses "cents" and a different method signature than our system expects.
 */
public class PayPalPaymentService {

    protected final String email;
    protected final String password;
    private double balance = 100_000.00; // Simulated balance for demonstration

    public PayPalPaymentService(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Processes payment in cents with a transaction reference.
     *
     * @param amountInCents the amount in cents
     * @param reference     a transaction reference string
     * @return true if payment succeeds
     */
    public synchronized boolean makePayment(int amountInCents, String reference) {
        if(!connect()) {
            System.out.printf("[PayPalPayment] Failed to connect for %s%n", email);
            return false;
        }
        double amountInDollars = amountInCents / 100.0;
        if (amountInDollars > balance) {
            System.out.printf("[PayPalPayment] Insufficient funds for %s. Required: $%.2f, Available: $%.2f%n",
                    email, amountInDollars, balance);
            return false;
        }
        balance -= amountInDollars;
        System.out.printf("[PayPalPayment] Charged $%.2f for ref: %s from account: %s%n",
                amountInDollars, reference, email);
        return true;
    }

    public boolean connect() {
        // Simulate connecting to PayPal account
        System.out.printf("[PayPalPaymentService] Connected to PayPal account: %s%n", email);
        return true;
    }

    public synchronized double getBalance() {
        System.out.printf("[PayPalPaymentService] Getting balance: %s%n", email);
        return balance;
    }

    public synchronized void setBalance(double balance) {
        this.balance = balance;
    }
}
