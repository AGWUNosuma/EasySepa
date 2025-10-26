package de.agwu.apps.easysepa.service;

import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import de.agwu.apps.easysepa.model.sepa.SepaTransaction;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service to generate SEPA XML files using templates
 */
public class SepaXmlGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final XmlTemplateEngine templateEngine;

    public SepaXmlGenerator() {
        this(new XmlTemplateEngine());
    }

    public SepaXmlGenerator(XmlTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
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
        if (debtorBIC != null) {
            debtorBIC = debtorBIC.trim();
        }
        if (debtorBIC != null && !debtorBIC.isEmpty()) {
            data.put("debtorBIC", debtorBIC.toUpperCase(Locale.ROOT));
        }
    }

    private void addDirectDebitFields(Map<String, Object> data, SepaTransaction firstTx) {
        data.put("seqTp", firstTx.getField("seqTp"));
        data.put("reqdColltnDt", firstTx.getField("reqdColltnDt"));
        data.put("creditorName", firstTx.getField("creditorName"));
        data.put("creditorIBAN", firstTx.getField("creditorIBAN"));
        data.put("creditorId", firstTx.getField("creditorId"));

        String batchBooking = firstTx.getField("batchBooking");
        if (batchBooking != null) {
            batchBooking = batchBooking.trim();
        }
        if (batchBooking == null || batchBooking.isEmpty()) {
            batchBooking = "true";
        }
        if (!"true".equalsIgnoreCase(batchBooking) && !"false".equalsIgnoreCase(batchBooking)) {
            batchBooking = "true";
        }
        data.put("batchBooking", batchBooking.toLowerCase(Locale.ROOT));

        String localInstrument = firstTx.getField("localInstrumentCode");
        if (localInstrument != null) {
            localInstrument = localInstrument.trim();
        }
        if (localInstrument == null || localInstrument.isEmpty()) {
            localInstrument = "CORE";
        } else {
            localInstrument = localInstrument.toUpperCase(Locale.ROOT);
        }
        data.put("localInstrumentCode", localInstrument);

        String creditorBIC = firstTx.getField("creditorBIC");
        if (creditorBIC != null) {
            creditorBIC = creditorBIC.trim();
        }
        if (creditorBIC != null && !creditorBIC.isEmpty()) {
            data.put("creditorBIC", creditorBIC.toUpperCase(Locale.ROOT));
        }
    }

    private String calculateControlSum(List<SepaTransaction> transactions) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SepaTransaction tx : transactions) {
            String amountStr = tx.getField("amount");
            if (amountStr != null) {
                try {
                    BigDecimal amount = new BigDecimal(amountStr.trim());
                    totalAmount = totalAmount.add(amount);
                } catch (NumberFormatException e) {
                    // Skip invalid amounts
                }
            }
        }

        return totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
