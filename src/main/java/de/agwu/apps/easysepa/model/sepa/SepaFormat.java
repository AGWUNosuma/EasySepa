package de.agwu.apps.easysepa.model.sepa;

/**
 * Enumeration of supported SEPA formats
 */
public enum SepaFormat {
    PAIN_001_001_03("pain.001.001.03", "Überweisung (Credit Transfer)", SepaFormatType.CREDIT_TRANSFER),
    PAIN_001_001_09("pain.001.001.09", "Überweisung (Credit Transfer)", SepaFormatType.CREDIT_TRANSFER),
    PAIN_008_001_11("pain.008.001.11", "Lastschrift (Direct Debit)", SepaFormatType.DIRECT_DEBIT);

    private final String code;
    private final String displayName;
    private final SepaFormatType type;

    SepaFormat(String code, String displayName, SepaFormatType type) {
        this.code = code;
        this.displayName = displayName;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SepaFormatType getType() {
        return type;
    }

    @Override
    public String toString() {
        return code + " - " + displayName;
    }

    public static SepaFormat fromCode(String code) {
        for (SepaFormat format : values()) {
            if (format.code.equals(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown SEPA format code: " + code);
    }
}
