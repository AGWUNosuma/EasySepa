package de.agwu.apps.easysepa.model.sepa;

/**
 * Type of SEPA transaction
 */
public enum SepaFormatType {
    CREDIT_TRANSFER,  // Überweisung (pain.001)
    DIRECT_DEBIT      // Lastschrift (pain.008)
}
