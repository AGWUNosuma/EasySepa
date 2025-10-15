package de.agwu.apps.easysepa.service;

import de.agwu.apps.easysepa.model.sepa.SepaField;

/**
 * Service for field mapping logic
 */
public class FieldMappingService {

    /**
     * Try to auto-match a CSV header to a SEPA field
     */
    public String findMatchingHeader(SepaField field, String[] csvHeaders) {
        String fieldName = field.getFieldName().toLowerCase();

        // First pass: Try exact matches
        for (String header : csvHeaders) {
            String headerLower = header.toLowerCase();
            if (headerLower.equals(fieldName)) {
                return header;
            }
        }

        // Second pass: Try specific keyword matches with priority
        for (String header : csvHeaders) {
            String headerLower = header.toLowerCase();

            // IBAN - must be exact word match to avoid false positives
            if (fieldName.contains("iban") && headerLower.equals("iban")) {
                return header;
            }

            // BIC - must be exact word match to avoid false positives
            if (fieldName.contains("bic") && headerLower.equals("bic")) {
                return header;
            }
        }

        // Third pass: Try flexible keyword matches
        for (String header : csvHeaders) {
            String headerLower = header.toLowerCase();

            // Mandate - contains check
            if (fieldName.contains("mandate") && (headerLower.contains("mandat") || headerLower.contains("mandate"))) {
                return header;
            }

            // Amount/Betrag - cross-language
            if (fieldName.contains("amount") && (headerLower.contains("betrag") || headerLower.contains("amount"))) {
                return header;
            }

            // Remittance/Verwendungszweck
            if (fieldName.contains("remittance") && (headerLower.contains("verwendungszweck") ||
                                                      headerLower.contains("verwendung") ||
                                                      headerLower.contains("zweck") ||
                                                      headerLower.contains("remittance"))) {
                return header;
            }

            // End-to-End ID / Reference
            if (fieldName.contains("endtoend") && (headerLower.contains("referenz") ||
                                                    headerLower.contains("reference") ||
                                                    headerLower.contains("endtoend"))) {
                return header;
            }

            // Name matching - last because it's very common
            if (fieldName.contains("name") && headerLower.contains("name")) {
                return header;
            }
        }

        return null; // No match found
    }

}
