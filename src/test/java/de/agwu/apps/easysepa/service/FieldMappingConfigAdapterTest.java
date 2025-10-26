package de.agwu.apps.easysepa.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.agwu.apps.easysepa.model.config.FieldMappingConfig;
import de.agwu.apps.easysepa.model.config.FieldMappingConfigVersion;
import de.agwu.apps.easysepa.model.config.FieldMappingSelection;
import de.agwu.apps.easysepa.model.config.FieldMappingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldMappingConfigAdapterTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder()
                .registerTypeAdapter(FieldMappingConfig.class, new FieldMappingConfigAdapter())
                .create();
    }

    @Test
    void deserializesLegacyFormatWithStaticValues() {
        String legacyJson = "{" +
                "\"transactionFieldMappings\":{" +
                "\"mandateSignatureDate\":\"-- Fester Wert --\"," +
                "\"mandateId\":\"Mandate\"}," +
                "\"transactionFieldDefaultValues\":{" +
                "\"mandateSignatureDate\":\"2025-10-01\"}}";

        FieldMappingConfig config = gson.fromJson(legacyJson, FieldMappingConfig.class);

        assertEquals(FieldMappingSelection.csv("Mandate"),
                config.getTransactionFieldMappings().get("mandateId"));

        FieldMappingSelection signature = config.getTransactionFieldMappings().get("mandateSignatureDate");
        assertNotNull(signature);
        assertEquals(FieldMappingType.STATIC, signature.getType());
        assertEquals("2025-10-01", signature.getValue());
        assertEquals(FieldMappingConfigVersion.CURRENT, config.getVersion());
    }

    @Test
    void serializesStructuredMappings() {
        FieldMappingConfig config = new FieldMappingConfig("Test");
        config.getTransactionFieldMappings().put("mandateSignatureDate", FieldMappingSelection.staticValue("2025-10-01"));
        config.getTransactionFieldMappings().put("mandateId", FieldMappingSelection.csv("Mandate"));

        String json = gson.toJson(config);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(FieldMappingConfigVersion.CURRENT, root.get("version").getAsInt());
        assertFalse(root.has("transactionFieldDefaultValues"));

        JsonObject mappings = root.getAsJsonObject("transactionFieldMappings");
        assertNotNull(mappings);

        JsonObject signature = mappings.getAsJsonObject("mandateSignatureDate");
        assertEquals("static", signature.get("type").getAsString());
        assertEquals("2025-10-01", signature.get("value").getAsString());

        JsonObject mandate = mappings.getAsJsonObject("mandateId");
        assertEquals("csv", mandate.get("type").getAsString());
        assertEquals("Mandate", mandate.get("value").getAsString());
    }

    @Test
    void deserializesStructuredMappingsKeepsVersion() {
        String json = "{" +
                "\"configName\":\"Test\"," +
                "\"version\":2," +
                "\"transactionFieldMappings\":{" +
                "\"mandateSignatureDate\":{" +
                "\"type\":\"static\"," +
                "\"value\":\"2025-10-01\"}}}";

        FieldMappingConfig config = gson.fromJson(json, FieldMappingConfig.class);

        assertEquals(FieldMappingConfigVersion.CURRENT, config.getVersion());
        FieldMappingSelection selection = config.getTransactionFieldMappings().get("mandateSignatureDate");
        assertNotNull(selection);
        assertEquals(FieldMappingType.STATIC, selection.getType());
        assertEquals("2025-10-01", selection.getValue());
    }
}
