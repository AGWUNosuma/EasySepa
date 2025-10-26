package de.agwu.apps.easysepa.model.config;

import java.util.Objects;

/**
 * Value object for displaying saved configurations alongside their version information.
 */
public record FieldMappingConfigSummary(String name, int version) {

    public FieldMappingConfigSummary {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String toString() {
        if (version > 0) {
            return name + " (Version " + version + ")";
        }
        return name;
    }
}
