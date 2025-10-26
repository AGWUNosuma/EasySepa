package de.agwu.apps.easysepa.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.agwu.apps.easysepa.model.config.FieldMappingConfig;
import de.agwu.apps.easysepa.model.config.FieldMappingConfigVersion;
import de.agwu.apps.easysepa.model.config.FieldMappingSelection;
import de.agwu.apps.easysepa.util.FieldMappingConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Upgrades legacy transaction mappings to structured selections and ensures they remain populated when loading.
 */
class TransactionFieldMappingMigrationStep implements FieldMappingConfigMigrationStep {

    private final Gson gson;

    TransactionFieldMappingMigrationStep(Gson gson) {
        this.gson = gson;
    }

    @Override
    public int targetVersion() {
        return FieldMappingConfigVersion.STRUCTURED_TRANSACTION_MAPPINGS;
    }

    @Override
    public boolean shouldApply(FieldMappingConfig config) {
        return config.getVersion() < targetVersion()
                || config.getTransactionFieldMappings() == null
                || config.getTransactionFieldMappings().isEmpty();
    }

    @Override
    public void migrate(FieldMappingConfig config, JsonObject originalJson) {
        Map<String, FieldMappingSelection> normalized = new HashMap<>();

        JsonObject mappingObject = originalJson.has("transactionFieldMappings") && originalJson.get("transactionFieldMappings").isJsonObject()
                ? originalJson.getAsJsonObject("transactionFieldMappings")
                : new JsonObject();

        JsonObject legacyDefaults = originalJson.has("transactionFieldDefaultValues") && originalJson.get("transactionFieldDefaultValues").isJsonObject()
                ? originalJson.getAsJsonObject("transactionFieldDefaultValues")
                : new JsonObject();

        for (Map.Entry<String, JsonElement> entry : mappingObject.entrySet()) {
            FieldMappingSelection selection = parseSelection(entry.getKey(), entry.getValue(), legacyDefaults);
            if (selection != null) {
                normalized.put(entry.getKey(), selection);
            }
        }

        config.setTransactionFieldMappings(normalized);
    }

    private FieldMappingSelection parseSelection(String fieldName, JsonElement valueElement, JsonObject legacyDefaults) {
        if (valueElement == null || valueElement.isJsonNull()) {
            return null;
        }

        if (valueElement.isJsonObject()) {
            FieldMappingSelection selection = gson.fromJson(valueElement, FieldMappingSelection.class);
            if (selection != null && selection.getType() != null) {
                return selection;
            }
            return null;
        }

        if (valueElement.isJsonPrimitive()) {
            String storedValue = valueElement.getAsString();
            if (FieldMappingConstants.FIXED_VALUE_OPTION.equals(storedValue)) {
                String staticValue = legacyDefaults.has(fieldName) && legacyDefaults.get(fieldName).isJsonPrimitive()
                        ? legacyDefaults.get(fieldName).getAsString()
                        : null;
                return FieldMappingSelection.staticValue(staticValue);
            }

            if (storedValue == null || storedValue.isBlank()) {
                return FieldMappingSelection.csv(null);
            }

            return FieldMappingSelection.csv(storedValue);
        }

        return null;
    }
}
