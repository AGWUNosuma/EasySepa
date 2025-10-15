package de.agwu.apps.easysepa.model.sepa;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single SEPA transaction with all its fields
 */
public class SepaTransaction {

    private final Map<String, String> fields = new HashMap<>();
    private int rowNumber;

    public SepaTransaction(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public void setField(String fieldName, String value) {
        fields.put(fieldName, value);
    }

    public String getField(String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String, String> getAllFields() {
        return new HashMap<>(fields);
    }

    public java.util.Set<String> getAllFieldNames() {
        return fields.keySet();
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }
}
