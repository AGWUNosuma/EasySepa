package de.agwu.apps.easysepa.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Service for field validation
 */
public class ValidationService {

    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$");
    private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final Pattern CREDITOR_ID_PATTERN = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{3,35}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Validate IBAN format
     */
    public ValidationResult validateIBAN(String iban) {
        if (iban == null || iban.trim().isEmpty()) {
            return ValidationResult.error("IBAN darf nicht leer sein");
        }

        String normalized = iban.replaceAll("\\s+", "").toUpperCase();

        if (!IBAN_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.error("Ungültiges IBAN-Format. Beispiel: DE89370400440532013000");
        }

        return ValidationResult.success();
    }

    /**
     * Validate BIC format
     */
    public ValidationResult validateBIC(String bic) {
        if (bic == null || bic.trim().isEmpty()) {
            return ValidationResult.success(); // BIC is often optional
        }

        String normalized = bic.replaceAll("\\s+", "").toUpperCase();

        if (!BIC_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.error("Ungültiges BIC-Format. Beispiel: COBADEFFXXX");
        }

        return ValidationResult.success();
    }

    /**
     * Validate amount format
     */
    public ValidationResult validateAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return ValidationResult.error("Betrag darf nicht leer sein");
        }

        if (!AMOUNT_PATTERN.matcher(amount.trim()).matches()) {
            return ValidationResult.error("Ungültiger Betrag. Format: 123.45 (nur Punkt als Dezimaltrenner)");
        }

        try {
            double value = Double.parseDouble(amount.trim());
            if (value <= 0) {
                return ValidationResult.error("Betrag muss größer als 0 sein");
            }
            if (value > 999999999.99) {
                return ValidationResult.error("Betrag ist zu groß");
            }
        } catch (NumberFormatException e) {
            return ValidationResult.error("Ungültiger Betrag");
        }

        return ValidationResult.success();
    }

    /**
     * Validate date format (YYYY-MM-DD)
     */
    public ValidationResult validateDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return ValidationResult.error("Datum darf nicht leer sein");
        }

        try {
            LocalDate.parse(date.trim(), DATE_FORMATTER);
            return ValidationResult.success();
        } catch (DateTimeParseException e) {
            return ValidationResult.error("Ungültiges Datumsformat. Format: YYYY-MM-DD (z.B. 2025-10-20)");
        }
    }

    /**
     * Validate creditor ID format (Gläubiger-ID)
     */
    public ValidationResult validateCreditorId(String creditorId) {
        if (creditorId == null || creditorId.trim().isEmpty()) {
            return ValidationResult.error("Gläubiger-ID darf nicht leer sein");
        }

        String normalized = creditorId.replaceAll("\\s+", "").toUpperCase();

        if (!CREDITOR_ID_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.error("Ungültiges Format. Beispiel: DE98ZZZ09999999999");
        }

        // German creditor ID specific validation
        if (normalized.startsWith("DE") && normalized.length() != 18) {
            return ValidationResult.error("Deutsche Gläubiger-ID muss 18 Zeichen haben");
        }

        return ValidationResult.success();
    }

    /**
     * Validate sequence type (for direct debit)
     */
    public ValidationResult validateSequenceType(String seqType) {
        if (seqType == null || seqType.trim().isEmpty()) {
            return ValidationResult.error("Sequenz-Typ darf nicht leer sein");
        }

        String normalized = seqType.trim().toUpperCase();
        if (!normalized.equals("FRST") && !normalized.equals("RCUR") &&
            !normalized.equals("OOFF") && !normalized.equals("FNAL")) {
            return ValidationResult.error("Gültige Werte: FRST, RCUR, OOFF, FNAL");
        }

        return ValidationResult.success();
    }

    /**
     * Validate generic text field (max length)
     */
    public ValidationResult validateText(String text, int maxLength, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.error(fieldName + " darf nicht leer sein");
        }

        if (text.length() > maxLength) {
            return ValidationResult.error(fieldName + " darf maximal " + maxLength + " Zeichen haben");
        }

        return ValidationResult.success();
    }

    /**
     * Result of a validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
