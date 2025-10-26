package de.agwu.apps.easysepa.model.config;

/**
 * Defines the known configuration versions for persisted field mappings.
 */
public final class FieldMappingConfigVersion {

    private FieldMappingConfigVersion() {
        // Utility class
    }

    /**
     * Initial version that stored transaction mappings as simple strings.
     */
    public static final int INITIAL = 1;

    /**
     * Version that stores transaction mappings as structured selections with type information.
     */
    public static final int STRUCTURED_TRANSACTION_MAPPINGS = 2;

    /**
     * Represents the latest supported configuration version.
     */
    public static final int CURRENT = STRUCTURED_TRANSACTION_MAPPINGS;
}
