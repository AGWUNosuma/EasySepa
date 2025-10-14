package de.agwu.apps.easysepa.model.sepa;

public class SepaField {
    private final String fieldName;
    private final String displayName;
    private final boolean required;
    private final String description;

    public SepaField(String fieldName, String displayName, boolean required, String description) {
        this.fieldName = fieldName;
        this.displayName = displayName;
        this.required = required;
        this.description = description;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName + (required ? " *" : "");
    }
}
