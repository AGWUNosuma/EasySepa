package de.agwu.apps.easysepa.model.config;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the source of a field mapping entry in a configuration.
 */
public enum FieldMappingType {
    @SerializedName("csv")
    CSV,
    @SerializedName("static")
    STATIC
}
