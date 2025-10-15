package de.agwu.apps.easysepa.model.sepa;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of transaction validation
 */
public class TransactionValidationResult {

    private final List<SepaTransaction> validTransactions = new ArrayList<>();
    private final List<InvalidTransaction> invalidTransactions = new ArrayList<>();

    public void addValidTransaction(SepaTransaction transaction) {
        validTransactions.add(transaction);
    }

    public void addInvalidTransaction(SepaTransaction transaction, List<String> errors) {
        invalidTransactions.add(new InvalidTransaction(transaction, errors));
    }

    public List<SepaTransaction> getValidTransactions() {
        return validTransactions;
    }

    public List<InvalidTransaction> getInvalidTransactions() {
        return invalidTransactions;
    }

    public boolean hasInvalidTransactions() {
        return !invalidTransactions.isEmpty();
    }

    public int getTotalCount() {
        return validTransactions.size() + invalidTransactions.size();
    }

    /**
     * Represents an invalid transaction with its errors
     */
    public static class InvalidTransaction {
        private final SepaTransaction transaction;
        private final List<String> errors;

        public InvalidTransaction(SepaTransaction transaction, List<String> errors) {
            this.transaction = transaction;
            this.errors = errors;
        }

        public SepaTransaction getTransaction() {
            return transaction;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorsAsString() {
            return String.join(", ", errors);
        }
    }
}
