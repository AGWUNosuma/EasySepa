package de.agwu.apps.easysepa.model.config;

import java.util.Objects;

/**
 * Describes how a field value should be obtained when exporting SEPA data.
 */
public class FieldMappingSelection {

    private FieldMappingType type;
    private String value;

    public FieldMappingSelection() {
    }

    public FieldMappingSelection(FieldMappingType type, String value) {
        this.type = type;
        this.value = value;
    }

    public static FieldMappingSelection csv(String columnName) {
        return new FieldMappingSelection(FieldMappingType.CSV, columnName);
    }

    public static FieldMappingSelection staticValue(String staticValue) {
        return new FieldMappingSelection(FieldMappingType.STATIC, staticValue);
    }

    public FieldMappingType getType() {
        return type;
    }

    public void setType(FieldMappingType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isStatic() {
        return FieldMappingType.STATIC.equals(type);
    }

    public boolean isCsv() {
        return FieldMappingType.CSV.equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldMappingSelection that = (FieldMappingSelection) o;
        return type == that.type && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "FieldMappingSelection{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }
}
