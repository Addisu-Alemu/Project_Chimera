package com.chimera.actservice.model;

/** Classification of a financial transaction flowing through the ACT service. */
public enum TransactionType {
    PAYMENT,
    REFUND,
    SUBSCRIPTION,
    WITHDRAWAL,
    SPONSORSHIP
}
