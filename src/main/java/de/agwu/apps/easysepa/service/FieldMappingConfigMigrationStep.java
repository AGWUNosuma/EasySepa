package de.agwu.apps.easysepa.service;

import com.google.gson.JsonObject;
import de.agwu.apps.easysepa.model.config.FieldMappingConfig;

/**
 * Represents a single migration step for bringing a saved configuration to the latest structure.
 */
interface FieldMappingConfigMigrationStep {

    /**
     * Returns the target version this migration step upgrades to.
     */
    int targetVersion();

    /**
     * Returns whether the migration should be applied for the provided configuration.
     */
    boolean shouldApply(FieldMappingConfig config);

    /**
     * Executes the migration using the original JSON payload for context.
     */
    void migrate(FieldMappingConfig config, JsonObject originalJson);
}
