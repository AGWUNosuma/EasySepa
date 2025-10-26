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
import de.agwu.apps.easysepa.util.TemplateValueResolver;

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
            Map<String, TemplateValueResolver.TemplateExpression> templatedDefaults = compileDefaultTemplates(defaultValues);
            Map<String, String> staticDefaults = extractStaticDefaults(defaultValues, templatedDefaults.keySet());
            Map<String, String> resolvedGlobalValues = resolveGlobalFieldValues(globalFieldValues);

            String[] row;
            int dataRowNumber = 1;
            int transactionIndex = 1;
            while ((row = reader.readNext()) != null) {
                SepaTransaction transaction = new SepaTransaction(dataRowNumber);
                List<String> errors = new ArrayList<>();

                addGlobalFields(fieldDefinition, resolvedGlobalValues, transaction);
                populateTransactionFields(fieldDefinition, columnMappings, decimalSeparator,
                        headerIndex, row, transaction, errors, amountFields,
                        templatedDefaults, staticDefaults, transactionIndex);

                if (errors.isEmpty()) {
                    result.addValidTransaction(transaction);
                } else {
                    result.addInvalidTransaction(transaction, errors);
                }
                dataRowNumber++;
                transactionIndex++;
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
                                           char decimalSeparator,
                                           Map<String, Integer> headerIndex,
                                           String[] row,
                                           SepaTransaction transaction,
                                           List<String> errors,
                                           Map<String, Boolean> amountFields,
                                           Map<String, TemplateValueResolver.TemplateExpression> templatedDefaults,
                                           Map<String, String> staticDefaults,
                                           int transactionIndex) {

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
                TemplateValueResolver.TemplateExpression expression = templatedDefaults.get(fieldName);
                if (expression != null) {
                    value = expression.render(transactionIndex, transaction.getRowNumber());
                } else {
                    value = staticDefaults.get(fieldName);
                }
            }

            if (value != null && !value.trim().isEmpty()) {
                transaction.setField(fieldName, value);
            } else if (field.isRequired()) {
                errors.add(field.getDisplayName() + " fehlt");
            }
        }
    }

    private Map<String, TemplateValueResolver.TemplateExpression> compileDefaultTemplates(Map<String, String> defaultValues) {
        Map<String, TemplateValueResolver.TemplateExpression> compiled = new HashMap<>();
        for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
            TemplateValueResolver.compile(entry.getValue()).ifPresent(expression -> compiled.put(entry.getKey(), expression));
        }
        return compiled;
    }

    private Map<String, String> extractStaticDefaults(Map<String, String> defaultValues, java.util.Set<String> templatedKeys) {
        Map<String, String> staticDefaults = new HashMap<>();
        for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
            if (!templatedKeys.contains(entry.getKey())) {
                staticDefaults.put(entry.getKey(), entry.getValue());
            }
        }
        return staticDefaults;
    }

    private Map<String, String> resolveGlobalFieldValues(Map<String, String> globalFieldValues) {
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : globalFieldValues.entrySet()) {
            TemplateValueResolver.compile(entry.getValue())
                    .ifPresentOrElse(
                            expression -> resolved.put(entry.getKey(), expression.render(1, 1)),
                            () -> resolved.put(entry.getKey(), entry.getValue()));
        }
        return resolved;
    }
}
