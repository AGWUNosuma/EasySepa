package de.agwu.apps.easysepa.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.agwu.apps.easysepa.model.config.FieldMappingConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing field mapping configuration files
 */
public class ConfigService {

    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_EXTENSION = ".json";
    private final Gson gson;

    public ConfigService() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        ensureConfigDirExists();
    }

    /**
     * Ensure config directory exists
     */
    private void ensureConfigDirExists() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    /**
     * Save a field mapping configuration
     */
    public void saveConfig(FieldMappingConfig config) throws IOException {
        String fileName = sanitizeFileName(config.getConfigName()) + CONFIG_EXTENSION;
        File configFile = new File(CONFIG_DIR, fileName);

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            gson.toJson(config, writer);
        }
    }

    /**
     * Load a field mapping configuration by name
     */
    public FieldMappingConfig loadConfig(String configName) throws IOException {
        String fileName = sanitizeFileName(configName) + CONFIG_EXTENSION;
        File configFile = new File(CONFIG_DIR, fileName);

        if (!configFile.exists()) {
            throw new IOException("Konfiguration nicht gefunden: " + configName);
        }

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, FieldMappingConfig.class);
        }
    }

    /**
     * Delete a configuration
     */
    public boolean deleteConfig(String configName) {
        String fileName = sanitizeFileName(configName) + CONFIG_EXTENSION;
        File configFile = new File(CONFIG_DIR, fileName);
        return configFile.delete();
    }

    /**
     * List all available configuration names
     */
    public List<String> listConfigs() {
        List<String> configNames = new ArrayList<>();
        File configDir = new File(CONFIG_DIR);

        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(CONFIG_EXTENSION));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    // Remove .json extension
                    configNames.add(name.substring(0, name.length() - CONFIG_EXTENSION.length()));
                }
            }
        }

        return configNames;
    }

    /**
     * Check if a configuration exists
     */
    public boolean configExists(String configName) {
        String fileName = sanitizeFileName(configName) + CONFIG_EXTENSION;
        File configFile = new File(CONFIG_DIR, fileName);
        return configFile.exists();
    }

    /**
     * Sanitize file name to prevent path traversal and invalid characters
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Konfigurationsname darf nicht leer sein");
        }
        // Replace invalid characters with underscore
        return name.replaceAll("[^a-zA-Z0-9äöüÄÖÜß_\\-]", "_");
    }
}
