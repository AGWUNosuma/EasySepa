package de.agwu.apps.easysepa.service;

import com.google.gson.*;
import de.agwu.apps.easysepa.model.config.FieldMappingConfig;
import de.agwu.apps.easysepa.model.config.FieldMappingSelection;
import de.agwu.apps.easysepa.model.config.FieldMappingConfigVersion;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Custom Gson adapter that keeps backwards compatibility with legacy configuration files
 * while storing field mappings using structured entries.
 */
public class FieldMappingConfigAdapter implements JsonSerializer<FieldMappingConfig>, JsonDeserializer<FieldMappingConfig> {

    private final Gson delegate;
    private final FieldMappingConfigMigrator migrator;

    public FieldMappingConfigAdapter() {
        this.delegate = new Gson();
        this.migrator = new FieldMappingConfigMigrator(List.of(new TransactionFieldMappingMigrationStep(delegate)));
    }

    @Override
    public FieldMappingConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonParseException("Expected configuration JSON object");
        }

        JsonObject original = json.getAsJsonObject();
        JsonObject sanitized = original.deepCopy();
        sanitized.add("transactionFieldMappings", new JsonObject());

        FieldMappingConfig config = delegate.fromJson(sanitized, FieldMappingConfig.class);
        migrator.upgrade(config, original);
        return config;
    }

    @Override
    public JsonElement serialize(FieldMappingConfig src, Type typeOfSrc, JsonSerializationContext context) {
        if (src.getVersion() < FieldMappingConfigVersion.CURRENT) {
            src.setVersion(FieldMappingConfigVersion.CURRENT);
        }
        JsonObject jsonObject = delegate.toJsonTree(src).getAsJsonObject();
        JsonObject mappings = new JsonObject();
        for (Map.Entry<String, FieldMappingSelection> entry : src.getTransactionFieldMappings().entrySet()) {
            FieldMappingSelection selection = entry.getValue();
            if (selection == null || selection.getType() == null) {
                continue;
            }
            mappings.add(entry.getKey(), delegate.toJsonTree(selection));
        }
        jsonObject.add("transactionFieldMappings", mappings);
        jsonObject.remove("transactionFieldDefaultValues");
        return jsonObject;
    }
}
