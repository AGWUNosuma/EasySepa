package de.agwu.apps.easysepa.model;

import java.util.List;

/**
 * Interface for SEPA format field definitions.
 * Each SEPA format (pain.001, pain.008, etc.) should implement this interface.
 */
public interface ISepaFieldDefinition {

    /**
     * Get the SEPA format this definition applies to
     */
    SepaFormat getFormat();

    /**
     * Get global fields (constant values for all transactions)
     */
    List<SepaField> getGlobalFields();

    /**
     * Get transaction fields (from CSV rows)
     */
    List<SepaField> getTransactionFields();

    /**
     * Get all required fields (global + transaction)
     */
    default List<SepaField> getAllFields() {
        List<SepaField> all = new java.util.ArrayList<>(getGlobalFields());
        all.addAll(getTransactionFields());
        return all;
    }
}
