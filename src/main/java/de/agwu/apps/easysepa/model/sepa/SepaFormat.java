package de.agwu.apps.easysepa.model.sepa;

/**
 * Enumeration of supported SEPA formats
 */
public enum SepaFormat {
    PAIN_001_001_03("pain.001.001.03", "Überweisung (Credit Transfer)", SepaFormatType.CREDIT_TRANSFER),
    PAIN_001_001_09("pain.001.001.09", "Überweisung (Credit Transfer)", SepaFormatType.CREDIT_TRANSFER),
    PAIN_008_001_08("pain.008.001.08", "Lastschrift (Direct Debit)", SepaFormatType.DIRECT_DEBIT),
    PAIN_008_001_11("pain.008.001.11", "Lastschrift (Direct Debit)", SepaFormatType.DIRECT_DEBIT);

    private static final java.util.regex.Pattern PAIN_CODE_PATTERN =
            java.util.regex.Pattern.compile("^pain\\.(\\d{3})\\.\\d{3}\\.\\d{2}$", java.util.regex.Pattern.CASE_INSENSITIVE);

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

    static {
        // Verify that all enum entries use valid SEPA format codes
        for (SepaFormat format : values()) {
            if (!isValidFormatCode(format.code)) {
                throw new IllegalStateException("Invalid SEPA format code configured: " + format.code);
            }
        }
    }

    public static SepaFormat fromCode(String code) {
        for (SepaFormat format : values()) {
            if (format.code.equals(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown SEPA format code: " + code);
    }

    public static boolean isValidFormatCode(String code) {
        if (code == null) {
            return false;
        }
        return PAIN_CODE_PATTERN.matcher(code.trim()).matches();
    }

    public static SepaFormatType resolveType(String code) {
        if (!isValidFormatCode(code)) {
            throw new IllegalArgumentException("Unsupported SEPA format code: " + code);
        }

        java.util.regex.Matcher matcher = PAIN_CODE_PATTERN.matcher(code.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported SEPA format code: " + code);
        }

        String painNumber = matcher.group(1);
        return switch (painNumber) {
            case "001" -> SepaFormatType.CREDIT_TRANSFER;
            case "008" -> SepaFormatType.DIRECT_DEBIT;
            default -> throw new IllegalArgumentException("Unsupported pain transaction type: " + code);
        };
    }
}
