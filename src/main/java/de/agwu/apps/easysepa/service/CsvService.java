package de.agwu.apps.easysepa.service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Service for CSV file operations
 */
public class CsvService {

    /**
     * Read CSV headers from file
     */
    public String[] readHeaders(File file, char separator, String encoding) throws IOException, CsvException {
        CSVParser parser = new CSVParserBuilder()
            .withSeparator(separator)
            .build();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), Charset.forName(encoding)))
                .withCSVParser(parser)
                .build()) {

            return reader.readNext();
        }
    }

    /**
     * Convert separator display name to character
     */
    public char parseSeparator(String separatorDisplay) {
        return switch (separatorDisplay) {
            case "Komma (,)" -> ',';
            case "Semikolon (;)" -> ';';
            case "Tab (\\t)" -> '\t';
            case "Pipe (|)" -> '|';
            default -> ';';
        };
    }

    /**
     * Convert decimal separator display name to character
     */
    public char parseDecimalSeparator(String decimalSeparatorDisplay) {
        return switch (decimalSeparatorDisplay) {
            case "Punkt (.)" -> '.';
            case "Komma (,)" -> ',';
            default -> ',';
        };
    }

    /**
     * Normalize a decimal number from CSV format to SEPA format (dot as separator)
     * @param value The value from CSV
     * @param csvDecimalSeparator The decimal separator used in the CSV
     * @return Normalized value with dot as decimal separator
     */
    public String normalizeDecimalValue(String value, char csvDecimalSeparator) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String normalized = value.trim();

        // If CSV uses comma as decimal separator, replace it with dot
        if (csvDecimalSeparator == ',') {
            // Remove thousand separators (dots) first if present
            normalized = normalized.replace(".", "");
            // Replace decimal comma with dot
            normalized = normalized.replace(",", ".");
        } else {
            // If CSV uses dot as decimal separator, remove thousand separators (commas)
            normalized = normalized.replace(",", "");
        }

        return normalized;
    }
}
