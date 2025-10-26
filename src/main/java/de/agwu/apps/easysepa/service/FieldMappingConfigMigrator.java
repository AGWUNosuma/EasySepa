package de.agwu.apps.easysepa.service;

import com.google.gson.JsonObject;
import de.agwu.apps.easysepa.model.config.FieldMappingConfig;
import de.agwu.apps.easysepa.model.config.FieldMappingConfigVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes a sequence of migrations to ensure loaded configurations match the current application schema.
 */
class FieldMappingConfigMigrator {

    private final List<FieldMappingConfigMigrationStep> steps;

    FieldMappingConfigMigrator(List<FieldMappingConfigMigrationStep> steps) {
        this.steps = new ArrayList<>(steps);
        this.steps.sort(Comparator.comparingInt(FieldMappingConfigMigrationStep::targetVersion));
    }

    void upgrade(FieldMappingConfig config, JsonObject originalJson) {
        for (FieldMappingConfigMigrationStep step : steps) {
            if (step.shouldApply(config)) {
                step.migrate(config, originalJson);
                if (config.getVersion() < step.targetVersion()) {
                    config.setVersion(step.targetVersion());
                }
            }
        }

        if (config.getVersion() < FieldMappingConfigVersion.CURRENT) {
            config.setVersion(FieldMappingConfigVersion.CURRENT);
        }
    }
}
