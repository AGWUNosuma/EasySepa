package de.agwu.apps.easysepa.util;

import de.agwu.apps.easysepa.model.sepa.SepaField;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility for UI component creation and management
 */
public class UiUtil {

    private final ValidationUtil validationUtil = new ValidationUtil();

    /**
     * Create an info button for a field
     */
    public Button createInfoButton(SepaField field) {
        Button infoButton = new Button("i");
        infoButton.getStyleClass().add("info-button");

        // Show description in dialog when clicked
        infoButton.setOnAction(e -> showFieldInfoDialog(field));

        return infoButton;
    }

    /**
     * Show a dialog with field information
     */
    public void showFieldInfoDialog(SepaField field) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feld-Information");
        alert.setHeaderText(field.getDisplayName() + (field.isRequired() ? " (Pflichtfeld)" : " (Optional)"));

        // Use TextArea for better formatting and scrolling
        TextArea textArea = new TextArea(field.getDescription());
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(50);
        textArea.setStyle("-fx-font-size: 12px;");

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(400);
        alert.showAndWait();
    }

    /**
     * Create a label box with label and info button
     */
    public HBox createLabelWithInfo(SepaField field) {
        HBox labelBox = new HBox(5);
        labelBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Field label with required indicator
        Label label = new Label(field.getDisplayName() + (field.isRequired() ? " *" : ""));
        if (field.isRequired()) {
            label.getStyleClass().add("field-label-required");
        }

        Button infoButton = createInfoButton(field);

        labelBox.getChildren().addAll(label, infoButton);
        return labelBox;
    }

    /**
     * Create a ComboBox with CSV headers and "Fixed Value" option
     */
    public ComboBox<String> createColumnComboBox(String[] csvHeaders) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().add(FieldMappingConstants.FIXED_VALUE_OPTION);
        comboBox.getItems().addAll(csvHeaders);
        comboBox.setValue(FieldMappingConstants.FIXED_VALUE_OPTION);
        comboBox.setPrefWidth(200);
        return comboBox;
    }

    /**
     * Create a TextField for default/fixed values with validation
     */
    public Control createDefaultValueField(SepaField field, boolean isGlobal) {
        String fieldName = field.getFieldName().toLowerCase();

        // Check if this is sequence type field - use ComboBox
        if (fieldName.equals("seqtp")) {
            return createSequenceTypeComboBox();
        }

        // Check if this is a date field (but NOT endToEndId or mandateId) - use DatePicker
        if (!fieldName.contains("endtoend") && !fieldName.contains("mandateid") &&
            (fieldName.contains("date") || fieldName.contains("dt") || fieldName.contains("datum"))) {
            return createDatePickerWithValidation(field, isGlobal);
        }

        // Regular TextField with validation
        return createTextFieldWithValidation(field, isGlobal);
    }

    /**
     * Create a DatePicker with validation
     */
    private DatePicker createDatePickerWithValidation(SepaField field, boolean isGlobal) {
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("YYYY-MM-DD");
        datePicker.setPrefWidth(isGlobal ? 300 : 200);
        datePicker.setEditable(true);

        // Set default to today if it's an execution/collection date
        String fieldName = field.getFieldName().toLowerCase();
        if (fieldName.contains("exct") || fieldName.contains("coll")) {
            datePicker.setValue(LocalDate.now().plusDays(7)); // 7 days in future as default
        }

        // Validate on value change
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                datePicker.getStyleClass().removeAll("field-invalid", "field-valid");
                datePicker.getStyleClass().add("field-valid");
            }
        });

        return datePicker;
    }

    /**
     * Create a ComboBox for sequence type
     */
    private ComboBox<String> createSequenceTypeComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll("FRST", "RCUR", "OOFF", "FNAL");
        comboBox.setPromptText("Sequenz-Typ wählen");
        comboBox.setPrefWidth(300);
        return comboBox;
    }

    /**
     * Create a TextField with validation
     */
    private TextField createTextFieldWithValidation(SepaField field, boolean isGlobal) {
        TextField textField = new TextField();
        if (isGlobal) {
            textField.setPromptText("Fester Wert eingeben...");
            textField.setPrefWidth(300);
        } else {
            textField.setPromptText("Fester Wert oder leer für CSV-Spalte");
            textField.setPrefWidth(200);
        }

        // Add validation on focus lost
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !textField.getText().trim().isEmpty()) {
                validateField(textField, field);
            }
        });

        // Add real-time validation for some fields
        String fieldName = field.getFieldName().toLowerCase();
        if (fieldName.contains("iban") || fieldName.contains("bic") ||
            fieldName.contains("amount") || fieldName.contains("creditorid")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    validateField(textField, field);
                }
            });
        }

        return textField;
    }

    /**
     * Validate a text field based on field type
     */
    private void validateField(TextField textField, SepaField field) {
        String value = textField.getText();
        String fieldName = field.getFieldName().toLowerCase();
        ValidationUtil.ValidationResult result;

        if (fieldName.contains("iban")) {
            result = validationUtil.validateIBAN(value);
        } else if (fieldName.contains("bic")) {
            result = validationUtil.validateBIC(value);
        } else if (fieldName.contains("amount")) {
            result = validationUtil.validateAmount(value);
        } else if (fieldName.contains("creditorid")) {
            result = validationUtil.validateCreditorId(value);
        } else if (!fieldName.contains("endtoend") && !fieldName.contains("mandateid") &&
                   (fieldName.contains("date") || fieldName.contains("dt"))) {
            result = validationUtil.validateDate(value);
        } else {
            // Generic text validation
            result = ValidationUtil.ValidationResult.success();
        }

        // Update visual feedback
        textField.getStyleClass().removeAll("field-invalid", "field-valid");
        if (result.isValid()) {
            textField.getStyleClass().add("field-valid");
            textField.setTooltip(null);
        } else {
            textField.getStyleClass().add("field-invalid");
            Tooltip errorTooltip = new Tooltip(result.getErrorMessage());
            errorTooltip.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
            textField.setTooltip(errorTooltip);
        }
    }

    /**
     * Get the value from a control (handles TextField, DatePicker, ComboBox)
     */
    public String getControlValue(Control control) {
        if (control instanceof TextField textField) {
            return textField.getText();
        } else if (control instanceof DatePicker datePicker) {
            LocalDate date = datePicker.getValue();
            return date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
        } else if (control instanceof ComboBox<?> comboBox) {
            Object value = comboBox.getValue();
            return value != null ? value.toString() : "";
        }
        return "";
    }
}
