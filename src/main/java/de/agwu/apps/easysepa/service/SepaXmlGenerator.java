package de.agwu.apps.easysepa.service;

import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import de.agwu.apps.easysepa.model.sepa.SepaTransaction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to generate SEPA XML files using templates
 */
public class SepaXmlGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final XmlTemplateEngine templateEngine;

    public SepaXmlGenerator() {
        this.templateEngine = new XmlTemplateEngine();
    }

    /**
     * Generate SEPA XML file using format-specific templates
     */
    public void generateXml(File outputFile, SepaFormat format, List<SepaTransaction> transactions) throws IOException {
        // Load template for the format
        String template = templateEngine.loadTemplate(format.getCode());
        
        // Prepare template data
        Map<String, Object> data = prepareTemplateData(format, transactions);
        
        // Render template
        String xml = templateEngine.render(template, data);
        
        // Write to file with UTF-8 encoding
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writer.write(xml);
        }
    }

    private Map<String, Object> prepareTemplateData(SepaFormat format, List<SepaTransaction> transactions) {
        Map<String, Object> data = new HashMap<>();
        
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("No transactions to process");
        }
        
        SepaTransaction firstTx = transactions.get(0);
        
        // Add global fields from first transaction
        data.put("msgId", firstTx.getField("msgId"));
        data.put("creationDateTime", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        data.put("numberOfTransactions", String.valueOf(transactions.size()));
        data.put("controlSum", calculateControlSum(transactions));
        data.put("initiatorName", firstTx.getField("initiatorName"));
        data.put("pmtInfId", firstTx.getField("pmtInfId"));
        
        // Add format-specific fields
        switch (format.getType()) {
            case CREDIT_TRANSFER -> addCreditTransferFields(data, firstTx);
            case DIRECT_DEBIT -> addDirectDebitFields(data, firstTx);
        }
        
        // Convert transactions to template data
        List<Map<String, Object>> transactionData = templateEngine.convertTransactionsToData(transactions);
        data.put("transactions", transactionData);
        
        return data;
    }

    private void addCreditTransferFields(Map<String, Object> data, SepaTransaction firstTx) {
        data.put("reqdExctnDt", firstTx.getField("reqdExctnDt"));
        data.put("debtorName", firstTx.getField("debtorName"));
        data.put("debtorIBAN", firstTx.getField("debtorIBAN"));
        
        String debtorBIC = firstTx.getField("debtorBIC");
        if (debtorBIC != null && !debtorBIC.trim().isEmpty()) {
            data.put("debtorBIC", debtorBIC);
        }
    }

    private void addDirectDebitFields(Map<String, Object> data, SepaTransaction firstTx) {
        data.put("seqTp", firstTx.getField("seqTp"));
        data.put("reqdColltnDt", firstTx.getField("reqdColltnDt"));
        data.put("creditorName", firstTx.getField("creditorName"));
        data.put("creditorIBAN", firstTx.getField("creditorIBAN"));
        data.put("creditorId", firstTx.getField("creditorId"));
        
        String creditorBIC = firstTx.getField("creditorBIC");
        if (creditorBIC != null && !creditorBIC.trim().isEmpty()) {
            data.put("creditorBIC", creditorBIC);
        }
    }

    private String calculateControlSum(List<SepaTransaction> transactions) {
        double totalAmount = 0.0;
        
        for (SepaTransaction tx : transactions) {
            String amountStr = tx.getField("amount");
            if (amountStr != null) {
                try {
                    totalAmount += Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    // Skip invalid amounts
                }
            }
        }
        
        return String.format("%.2f", totalAmount);
    }
}
