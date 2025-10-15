package de.agwu.apps.easysepa.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import de.agwu.apps.easysepa.model.sepa.SepaField;
import de.agwu.apps.easysepa.model.sepa.SepaTransaction;
import de.agwu.apps.easysepa.model.sepa.TransactionValidationResult;
import de.agwu.apps.easysepa.model.sepa.definition.ISepaFieldDefinition;
import de.agwu.apps.easysepa.util.CsvUtil;
import de.agwu.apps.easysepa.util.FieldMappingConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Service to build SEPA transactions from CSV data
 */
public class SepaTransactionBuilder {

    private final CsvUtil csvUtil;

    public SepaTransactionBuilder() {
        this(new CsvUtil());
    }

    public SepaTransactionBuilder(CsvUtil csvUtil) {
        this.csvUtil = csvUtil;
    }

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

            String[] headers = reader.readNext();
            if (headers == null) {
                return result;
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            Map<String, Boolean> amountFields = precomputeAmountFields(fieldDefinition);

            String[] row;
            int dataRowNumber = 1;
            while ((row = reader.readNext()) != null) {
                SepaTransaction transaction = new SepaTransaction(dataRowNumber);
                List<String> errors = new ArrayList<>();

                addGlobalFields(fieldDefinition, globalFieldValues, transaction);
                populateTransactionFields(fieldDefinition, columnMappings, defaultValues, decimalSeparator,
                        headerIndex, row, transaction, errors, amountFields);

                if (errors.isEmpty()) {
                    result.addValidTransaction(transaction);
                } else {
                    result.addInvalidTransaction(transaction, errors);
                }
                dataRowNumber++;
            }
        }

        return result;
    }

    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i], i);
        }
        return headerIndex;
    }

    private Map<String, Boolean> precomputeAmountFields(ISepaFieldDefinition fieldDefinition) {
        Map<String, Boolean> amountFields = new HashMap<>();
        for (SepaField field : fieldDefinition.getTransactionFields()) {
            amountFields.put(field.getFieldName(), field.getFieldName().toLowerCase(Locale.ROOT).contains("amount"));
        }
        return amountFields;
    }

    private void addGlobalFields(ISepaFieldDefinition fieldDefinition,
                                 Map<String, String> globalFieldValues,
                                 SepaTransaction transaction) {
        for (SepaField field : fieldDefinition.getGlobalFields()) {
            String value = globalFieldValues.get(field.getFieldName());
            if (value != null) {
                transaction.setField(field.getFieldName(), value);
            }
        }
    }

    private void populateTransactionFields(ISepaFieldDefinition fieldDefinition,
                                           Map<String, String> columnMappings,
                                           Map<String, String> defaultValues,
                                           char decimalSeparator,
                                           Map<String, Integer> headerIndex,
                                           String[] row,
                                           SepaTransaction transaction,
                                           List<String> errors,
                                           Map<String, Boolean> amountFields) {

        for (SepaField field : fieldDefinition.getTransactionFields()) {
            String fieldName = field.getFieldName();
            String value = null;

            String mappedColumn = columnMappings.get(fieldName);
            if (mappedColumn != null && !FieldMappingConstants.FIXED_VALUE_OPTION.equals(mappedColumn)) {
                Integer columnIndex = headerIndex.get(mappedColumn);
                if (columnIndex != null && columnIndex < row.length) {
                    value = row[columnIndex];
                    if (value != null && Boolean.TRUE.equals(amountFields.get(fieldName))) {
                        value = csvUtil.normalizeDecimalValue(value, decimalSeparator);
                    }
                }
            } else {
                value = defaultValues.get(fieldName);
            }

            if (value != null && !value.trim().isEmpty()) {
                transaction.setField(fieldName, value);
            } else if (field.isRequired()) {
                errors.add(field.getDisplayName() + " fehlt");
            }
        }
    }
}
