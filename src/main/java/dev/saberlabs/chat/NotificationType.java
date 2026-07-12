package dev.saberlabs.chat;

public enum NotificationType {
    ORDER_READY,        // "your coffee is ready for pickup"
    ORDER_FULFILLED,    // "your order has been fulfilled"
    SESSION_MATCHED,    // "you are now connected with Barista Sara"
    PAYMENT_RECEIVED,   // barista sees: "payment received from customer"
    SESSION_ENDED       // "the customer has left the conversation"
}