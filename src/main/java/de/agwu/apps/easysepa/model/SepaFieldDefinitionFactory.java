package de.agwu.apps.easysepa.model;

/**
 * Factory to create the appropriate field definition for a given SEPA format
 */
public class SepaFieldDefinitionFactory {

    public static ISepaFieldDefinition create(SepaFormat format) {
        return switch (format.getType()) {
            case CREDIT_TRANSFER -> new Pain001FieldDefinition(format);
            case DIRECT_DEBIT -> new Pain008FieldDefinition(format);
        };
    }

    public static ISepaFieldDefinition create(String formatCode) {
        SepaFormat format = SepaFormat.fromCode(formatCode);
        return create(format);
    }
}
