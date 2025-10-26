package de.agwu.apps.easysepa.model.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for field mappings - stores which CSV columns map to which SEPA fields
 */
public class FieldMappingConfig {

    private String configName;
    private String sepaFormat; // e.g., "pain.008.001.11"
    private String csvSeparator;
    private String csvEncoding;
    private String decimalSeparator;
    private int version;

    // Maps field name to CSV column name (or fixed value indicator)
    private Map<String, String> globalFieldMappings = new HashMap<>();
    private Map<String, FieldMappingSelection> transactionFieldMappings = new HashMap<>();

    // Maps field name to fixed/default value (if not from CSV)
    private Map<String, String> globalFieldDefaultValues = new HashMap<>();

    public FieldMappingConfig() {
    }

    public FieldMappingConfig(String configName) {
        this.configName = configName;
    }

    // Getters and Setters

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getSepaFormat() {
        return sepaFormat;
    }

    public void setSepaFormat(String sepaFormat) {
        this.sepaFormat = sepaFormat;
    }

    public String getCsvSeparator() {
        return csvSeparator;
    }

    public void setCsvSeparator(String csvSeparator) {
        this.csvSeparator = csvSeparator;
    }

    public String getCsvEncoding() {
        return csvEncoding;
    }

    public void setCsvEncoding(String csvEncoding) {
        this.csvEncoding = csvEncoding;
    }

    public String getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public Map<String, String> getGlobalFieldMappings() {
        return globalFieldMappings;
    }

    public void setGlobalFieldMappings(Map<String, String> globalFieldMappings) {
        this.globalFieldMappings = globalFieldMappings;
    }

    public Map<String, FieldMappingSelection> getTransactionFieldMappings() {
        return transactionFieldMappings;
    }

    public void setTransactionFieldMappings(Map<String, FieldMappingSelection> transactionFieldMappings) {
        this.transactionFieldMappings = transactionFieldMappings;
    }

    public Map<String, String> getGlobalFieldDefaultValues() {
        return globalFieldDefaultValues;
    }

    public void setGlobalFieldDefaultValues(Map<String, String> globalFieldDefaultValues) {
        this.globalFieldDefaultValues = globalFieldDefaultValues;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
