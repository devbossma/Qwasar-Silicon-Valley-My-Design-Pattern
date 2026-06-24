package dev.saberlabs.auth;

/**
 * Represents the role of a user in the coffee shop chat application.
 * *
 * Roles determine what actions a user can perform:
 * - CUSTOMER: can send messages and place orders via chat
 * - BARISTA:  can see incoming orders and respond to customers
 * - MANAGER:  can manage users and view all chat history
 */
public enum Role {
    CUSTOMER,
    BARISTA,
    MANAGER
}