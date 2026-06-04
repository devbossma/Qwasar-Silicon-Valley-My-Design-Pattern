package dev.saberlabs.adapter;

import java.util.Scanner;

/**
 * Pattern 9: ADAPTER (Adaptee)
 *
 * Simulates a physical cash register used for in-store cash payments.
 * This service has an incompatible API with our PaymentGateway interface:
 * - It requires the cashier to set the amount received before processing.
 * - It calculates and returns change rather than a simple boolean.
 * - It tracks the total cash collected in the register.
 *
 * The CashPaymentAdapter bridges this service to our PaymentGateway interface,
 * allowing cash payments to be processed alongside digital payment methods
 * (PayPal, Stripe) without the client code knowing the difference.
 */
public class CashPaymentService {

    /** Total cash accumulated in the register across all transactions. */
    private double cashRegister = 0.00;

    /** The amount of cash the customer hands to the cashier for the current transaction. */
    private double amountReceived = 0.00;


    /**
     * Sets the amount of cash received from the customer.
     * Must be called before {@link #collectCash(double)} for each transaction.
     *
     * @param amount the cash amount the customer hands over in dollars
     */
    public synchronized void setAmountReceived(double amount) {
        this.amountReceived = amount;
    }

    /**
     * Processes a cash payment by comparing the amount due against the amount received.
     *
     * If the customer provided enough cash:
     * - The amount due is added to the cash register total.
     * - The calculated change is returned.
     *
     * If the customer did not provide enough cash:
     * - No money is added to the register.
     * - Returns -1 to indicate failure.
     *
     * @param amountDue the total amount the customer owes in dollars
     * @return the change to give back (>= 0), or -1 if insufficient cash
     */
    public synchronized double collectCash(double amountDue) {
        if (amountReceived < amountDue) {
            System.out.printf("[CashRegister] Insufficient cash. Due: $%.2f, Received: $%.2f%n",
                    amountDue, amountReceived);
            return -1;
        }
        double change = amountReceived - amountDue;
        cashRegister += amountDue;
        System.out.printf("[CashRegister] Collected $%.2f. Change: $%.2f%n", amountDue, change);
        return change;
    }

    /**
     * Returns the total cash collected in the register across all successful transactions.
     *
     * @return the cumulative cash register total in dollars
     */
    public synchronized double getCashRegisterTotal() {
        return cashRegister;
    }
}
