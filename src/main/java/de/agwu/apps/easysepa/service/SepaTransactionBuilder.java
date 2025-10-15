package de.agwu.apps.easysepa.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import de.agwu.apps.easysepa.model.sepa.SepaField;
import de.agwu.apps.easysepa.model.sepa.SepaTransaction;
import de.agwu.apps.easysepa.model.sepa.TransactionValidationResult;
import de.agwu.apps.easysepa.model.sepa.definition.ISepaFieldDefinition;
import de.agwu.apps.easysepa.util.CsvUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to build SEPA transactions from CSV data
 */
public class SepaTransactionBuilder {

    private final CsvUtil csvUtil = new CsvUtil();

    /**
     * Build and validate SEPA transactions from CSV file
     *
     * @param csvFile CSV file
     * @param separator CSV separator
     * @param encoding CSV encoding
     * @param decimalSeparator Decimal separator used in CSV
     * @param fieldDefinition SEPA field definition
     * @param globalFieldValues Global field values (apply to all transactions)
     * @param columnMappings Map of field name to CSV column name
     * @param defaultValues Default values for fields not from CSV
     * @return Validation result with valid and invalid transactions
     */
    public TransactionValidationResult buildTransactions(
            File csvFile,
            char separator,
            String encoding,
            char decimalSeparator,
            ISepaFieldDefinition fieldDefinition,
            Map<String, String> globalFieldValues,
            Map<String, String> columnMappings,
            Map<String, String> defaultValues) throws IOException, CsvException {

        TransactionValidationResult result = new TransactionValidationResult();

        // Read CSV file
        try (FileInputStream fis = new FileInputStream(csvFile);
             InputStreamReader isr = new InputStreamReader(fis, Charset.forName(encoding));
             CSVReader reader = new CSVReaderBuilder(isr)
                     .withCSVParser(csvUtil.createParser(separator))
                     .build()) {

            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                return result;
            }

            String[] headers = allRows.get(0);

            // Process each data row
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                SepaTransaction transaction = new SepaTransaction(i);
                List<String> errors = new ArrayList<>();

                // Add global fields
                for (SepaField field : fieldDefinition.getGlobalFields()) {
                    String value = globalFieldValues.get(field.getFieldName());
                    if (value != null) {
                        transaction.setField(field.getFieldName(), value);
                    }
                }

                // Add transaction fields
                for (SepaField field : fieldDefinition.getTransactionFields()) {
                    String fieldName = field.getFieldName();
                    String value = null;

                    // Check if mapped to CSV column
                    String mappedColumn = columnMappings.get(fieldName);
                    if (mappedColumn != null && !"-- Fester Wert --".equals(mappedColumn)) {
                        // Find column index
                        int columnIndex = -1;
                        for (int j = 0; j < headers.length; j++) {
                            if (headers[j].equals(mappedColumn)) {
                                columnIndex = j;
                                break;
                            }
                        }

                        if (columnIndex >= 0 && columnIndex < row.length) {
                            value = row[columnIndex];

                            // Normalize decimal values for amount fields
                            if (fieldName.toLowerCase().contains("amount") && value != null) {
                                value = csvUtil.normalizeDecimalValue(value, decimalSeparator);
                            }
                        }
                    } else {
                        // Use default value
                        value = defaultValues.get(fieldName);
                    }

                    if (value != null && !value.trim().isEmpty()) {
                        transaction.setField(fieldName, value);
                    } else if (field.isRequired()) {
                        // Required field is missing
                        errors.add(field.getDisplayName() + " fehlt");
                    }
                }

                // Validate transaction
                if (errors.isEmpty()) {
                    result.addValidTransaction(transaction);
                } else {
                    result.addInvalidTransaction(transaction, errors);
                }
            }
        }

        return result;
    }
}
