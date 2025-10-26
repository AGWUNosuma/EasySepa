package de.agwu.apps.easysepa.controller;

import com.opencsv.exceptions.CsvException;
import de.agwu.apps.easysepa.model.config.FieldMappingConfig;
import de.agwu.apps.easysepa.model.sepa.SepaField;
import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import de.agwu.apps.easysepa.model.sepa.TransactionValidationResult;
import de.agwu.apps.easysepa.model.sepa.definition.ISepaFieldDefinition;
import de.agwu.apps.easysepa.model.sepa.definition.SepaFieldDefinitionFactory;
import de.agwu.apps.easysepa.model.sepa.SepaFormatType;
import de.agwu.apps.easysepa.service.ConfigService;
import de.agwu.apps.easysepa.service.FieldMappingService;
import de.agwu.apps.easysepa.service.SepaTransactionBuilder;
import de.agwu.apps.easysepa.service.SepaXmlGenerator;
import de.agwu.apps.easysepa.service.XsdValidationService;
import de.agwu.apps.easysepa.util.CsvUtil;
import de.agwu.apps.easysepa.util.FieldMappingConstants;
import de.agwu.apps.easysepa.util.UiUtil;
import de.agwu.apps.easysepa.view.TransactionPreviewDialog;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainController {
    @FXML
    private ComboBox<SepaFormat> sepaFormatComboBox;

    @FXML
    private TextField filePathField;

    @FXML
    private ComboBox<String> separatorComboBox;

    @FXML
    private ComboBox<String> encodingComboBox;

    @FXML
    private ComboBox<String> decimalSeparatorComboBox;

    @FXML
    private Button loadButton;

    @FXML
    private Button detectEncodingButton;

    @FXML
    private Button generateXmlButton;

    @FXML
    private TitledPane csvConfigPane;

    @FXML
    private TitledPane headersPane;

    @FXML
    private TitledPane mappingPane;

    @FXML
    private ListView<String> headersListView;

    @FXML
    private GridPane fieldMappingGrid;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> savedConfigsComboBox;

    private File selectedFile;
    private String[] csvHeaders;
    private Map<String, Control> fieldMappingControls = new HashMap<>();
    private ISepaFieldDefinition currentFieldDefinition;

    private static final String FIXED_VALUE_OPTION = FieldMappingConstants.FIXED_VALUE_OPTION;

    // Services and Utilities
    private final CsvUtil csvUtil;
    private final FieldMappingService fieldMappingService;
    private final UiUtil uiUtil;
    private final ConfigService configService;
    private final SepaTransactionBuilder transactionBuilder;
    private final SepaXmlGenerator xmlGenerator;
    private final XsdValidationService xsdValidator;

    public MainController() {
        this(new CsvUtil(),
             new FieldMappingService(),
             new UiUtil(),
             new ConfigService(),
             new SepaTransactionBuilder(),
             new SepaXmlGenerator(),
             new XsdValidationService());
    }

    public MainController(CsvUtil csvUtil,
                          FieldMappingService fieldMappingService,
                          UiUtil uiUtil,
                          ConfigService configService,
                          SepaTransactionBuilder transactionBuilder,
                          SepaXmlGenerator xmlGenerator,
                          XsdValidationService xsdValidator) {
        this.csvUtil = csvUtil;
        this.fieldMappingService = fieldMappingService;
        this.uiUtil = uiUtil;
        this.configService = configService;
        this.transactionBuilder = transactionBuilder;
        this.xmlGenerator = xmlGenerator;
        this.xsdValidator = xsdValidator;
    }

    @FXML
    public void initialize() {
        // Initialize SEPA format options
        sepaFormatComboBox.getItems().addAll(SepaFormat.values());
        sepaFormatComboBox.setValue(SepaFormat.PAIN_001_001_03);

        // Add listener to update field mapping when format changes
        sepaFormatComboBox.setOnAction(e -> {
            if (csvHeaders != null && csvHeaders.length > 0) {
                showFieldMapping();
            }
            // Refresh config list to show only configs for selected format
            refreshConfigList();
        });

        // Initialize separator options
        separatorComboBox.getItems().addAll(
            "Komma (,)",
            "Semikolon (;)",
            "Tab (\\t)",
            "Pipe (|)"
        );
        separatorComboBox.setValue("Semikolon (;)");

        // Initialize encoding options
        encodingComboBox.getItems().addAll(
            "UTF-8",
            "ISO-8859-1",
            "Windows-1252",
            "US-ASCII"
        );
        encodingComboBox.setValue("UTF-8");

        // Initialize decimal separator options
        decimalSeparatorComboBox.getItems().addAll(
            "Punkt (.)",
            "Komma (,)"
        );
        decimalSeparatorComboBox.setValue("Komma (,)");

        // Load available configurations
        refreshConfigList();

        setCsvLoadingState(false);
        setStatus("Bitte wählen Sie eine CSV-Datei aus.", StatusType.INFO);
    }

    @FXML
    protected void onBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CSV Datei auswählen");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Dateien", "*.csv")
        );

        Stage stage = (Stage) filePathField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());
            setCsvLoadingState(false);
            statusLabel.setText("Datei ausgewählt. Klicken Sie auf 'CSV laden'.");

            // Hide headers section when new file is selected
            headersPane.setVisible(false);
            headersPane.setManaged(false);
        }
    }

    @FXML
    protected void onDetectEncoding() {
        if (selectedFile == null) {
            setStatus("Keine Datei ausgewählt!", StatusType.ERROR);
            return;
        }

        try {
            String detectedEncoding = de.agwu.apps.easysepa.util.EncodingDetector.detectEncoding(selectedFile);
            
            // Set detected encoding in ComboBox
            if (encodingComboBox.getItems().contains(detectedEncoding)) {
                encodingComboBox.setValue(detectedEncoding);
                setStatus("Encoding automatisch erkannt: " + detectedEncoding, StatusType.SUCCESS);
            } else {
                // Show preview with different encodings
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Encoding-Erkennung");
                alert.setHeaderText("Erkanntes Encoding: " + detectedEncoding);
                
                StringBuilder content = new StringBuilder();
                content.append("Vorschau der ersten Zeilen:\n\n");
                
                for (String enc : new String[]{"UTF-8", "ISO-8859-1", "Windows-1252"}) {
                    content.append("--- ").append(enc).append(" ---\n");
                    try {
                        String preview = de.agwu.apps.easysepa.util.EncodingDetector.getEncodingPreview(selectedFile, enc);
                        content.append(preview).append("\n");
                    } catch (Exception e) {
                        content.append("Fehler beim Lesen\n\n");
                    }
                }
                
                alert.setContentText(content.toString());
                alert.showAndWait();
                
                encodingComboBox.setValue(detectedEncoding);
                setStatus("Encoding erkannt: " + detectedEncoding + ". Bitte prüfen Sie die Vorschau.", StatusType.INFO);
            }
            
        } catch (IOException e) {
            setStatus("Fehler bei der Encoding-Erkennung: " + e.getMessage(), StatusType.ERROR);
        }
    }

    @FXML
    protected void onLoadCsv() {
        if (selectedFile == null) {
            setStatus("Keine Datei ausgewählt!", StatusType.ERROR);
            return;
        }

        final char separator = csvUtil.parseSeparator(separatorComboBox.getValue());
        final String encoding = encodingComboBox.getValue();

        setStatus("CSV wird geladen...", StatusType.WORKING);
        setCsvLoadingState(true);

        Task<String[]> loadTask = new Task<>() {
            @Override
            protected String[] call() throws Exception {
                return csvUtil.readHeaders(selectedFile, separator, encoding);
            }
        };

        loadTask.setOnSucceeded(event -> {
            setCsvLoadingState(false);
            csvHeaders = loadTask.getValue();

            if (csvHeaders != null && csvHeaders.length > 0) {
                headersListView.getItems().setAll(csvHeaders);
                headersPane.setVisible(true);
                headersPane.setManaged(true);

                setStatus("CSV erfolgreich geladen! " + csvHeaders.length +
                        " Spalten erkannt. Erstelle Feld-Zuordnung...", StatusType.SUCCESS);

                csvConfigPane.setExpanded(false);
                showFieldMapping();
            } else {
                csvHeaders = null;
                headersPane.setVisible(false);
                headersPane.setManaged(false);
                mappingPane.setVisible(false);
                mappingPane.setManaged(false);
                fieldMappingControls.clear();
                fieldMappingGrid.getChildren().clear();
                setStatus("CSV-Datei enthält keine Kopfzeile!", StatusType.ERROR);
            }
        });

        loadTask.setOnFailed(event -> {
            setCsvLoadingState(false);
            csvHeaders = null;
            currentFieldDefinition = null;
            headersPane.setVisible(false);
            headersPane.setManaged(false);
            mappingPane.setVisible(false);
            mappingPane.setManaged(false);
            fieldMappingControls.clear();
            fieldMappingGrid.getChildren().clear();

            Throwable exception = loadTask.getException();
            if (exception instanceof IOException ioException) {
                setStatus("Fehler beim Lesen der Datei: " + ioException.getMessage(), StatusType.ERROR);
            } else if (exception instanceof CsvException csvException) {
                setStatus("Fehler beim Parsen der CSV: " + csvException.getMessage(), StatusType.ERROR);
            } else {
                setStatus("Unbekannter Fehler beim Laden der CSV: " + exception.getMessage(), StatusType.ERROR);
            }
        });

        startBackgroundTask(loadTask);
    }

    private enum StatusType {
        SUCCESS, ERROR, INFO, WORKING
    }

    private void setStatus(String message, StatusType type) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-info", "status-working");
        switch (type) {
            case SUCCESS -> statusLabel.getStyleClass().add("status-success");
            case ERROR -> statusLabel.getStyleClass().add("status-error");
            case INFO -> statusLabel.getStyleClass().add("status-info");
            case WORKING -> statusLabel.getStyleClass().add("status-working");
        }
    }

    private void setCsvLoadingState(boolean loading) {
        boolean disable = loading || selectedFile == null;
        loadButton.setDisable(disable);
        detectEncodingButton.setDisable(disable);
    }

    private void setProcessingState(boolean processing) {
        if (generateXmlButton != null) {
            generateXmlButton.setDisable(processing);
        }
    }

    private void startBackgroundTask(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public String[] getCsvHeaders() {
        return csvHeaders;
    }

    private void showFieldMapping() {
        fieldMappingGrid.getChildren().clear();
        fieldMappingControls.clear();

        // Get field definition for selected format
        SepaFormat selectedFormat = sepaFormatComboBox.getValue();
        currentFieldDefinition = SepaFieldDefinitionFactory.create(selectedFormat);

        int row = 0;

        // Format info header
        Label formatInfo = new Label("Format: " + selectedFormat.toString());
        formatInfo.getStyleClass().add("format-info");
        fieldMappingGrid.add(formatInfo, 0, row++, 3, 1);

        // Add spacer
        row++;

        // Global/Static fields section
        Label globalHeader = new Label("Globale Felder (gelten für alle Transaktionen):");
        globalHeader.getStyleClass().add("subsection-header");
        fieldMappingGrid.add(globalHeader, 0, row++, 3, 1);

        List<SepaField> globalFields = currentFieldDefinition.getGlobalFields();
        for (SepaField field : globalFields) {
            row = addFieldMappingRow(field, row, true);
        }

        // Spacer
        row++;
        Label transactionHeader = new Label("Transaktionsfelder (aus CSV-Spalten):");
        transactionHeader.getStyleClass().add("subsection-header");
        fieldMappingGrid.add(transactionHeader, 0, row++, 3, 1);

        // Transaction fields section
        List<SepaField> transactionFields = currentFieldDefinition.getTransactionFields();
        for (SepaField field : transactionFields) {
            row = addFieldMappingRow(field, row, false);
        }

        mappingPane.setVisible(true);
        mappingPane.setManaged(true);
        statusLabel.setText("Bitte ordnen Sie die Felder zu.");
    }

    private int addFieldMappingRow(SepaField field, int row, boolean isGlobal) {
        // Create label with info button using service
        HBox labelBox = uiUtil.createLabelWithInfo(field);
        fieldMappingGrid.add(labelBox, 0, row);

        if (isGlobal) {
            // For global fields, create appropriate control with validation
            Control fieldControl = uiUtil.createDefaultValueField(field, true);
            fieldMappingControls.put(field.getFieldName(), fieldControl);
            fieldMappingGrid.add(fieldControl, 1, row, 2, 1);
        } else {
            // For transaction fields, dropdown with CSV columns + default value option
            ComboBox<String> comboBox = uiUtil.createColumnComboBox(csvHeaders);
            fieldMappingGrid.add(comboBox, 1, row);

            // Create appropriate control for default value with validation
            Control defaultField = uiUtil.createDefaultValueField(field, false);
            fieldMappingGrid.add(defaultField, 2, row);

            // Store both controls
            fieldMappingControls.put(field.getFieldName() + "_combo", comboBox);
            fieldMappingControls.put(field.getFieldName() + "_default", defaultField);

            // Auto-match similar column names using service
            String matchedHeader = fieldMappingService.findMatchingHeader(field, csvHeaders);
            if (matchedHeader != null) {
                comboBox.setValue(matchedHeader);
                defaultField.setDisable(true);
            } else {
                defaultField.setDisable(false);
            }

            // Link combo and default field
            comboBox.setOnAction(e -> {
                if (FIXED_VALUE_OPTION.equals(comboBox.getValue())) {
                    defaultField.setDisable(false);
                } else {
                    defaultField.setDisable(true);
                    // Clear the control based on its type
                    if (defaultField instanceof TextField tf) {
                        tf.clear();
                    } else if (defaultField instanceof DatePicker dp) {
                        dp.setValue(null);
                    } else if (defaultField instanceof ComboBox<?> cb) {
                        cb.setValue(null);
                    }
                }
            });
        }

        return row + 1;
    }

    @FXML
    protected void onCancelMapping() {
        mappingPane.setVisible(false);
        mappingPane.setManaged(false);
        headersPane.setVisible(false);
        headersPane.setManaged(false);
        headersListView.getItems().clear();
        csvConfigPane.setExpanded(true);
        setStatus("Zuordnung abgebrochen.", StatusType.INFO);
    }

    @FXML
    protected void onGenerateSepa() {
        if (currentFieldDefinition == null) {
            setStatus("Bitte laden Sie zuerst eine CSV-Datei.", StatusType.ERROR);
            return;
        }

        // Validate required fields
        List<String> missingFields = validateRequiredFields();

        if (!missingFields.isEmpty()) {
            setStatus("Folgende Pflichtfelder fehlen: " + String.join(", ", missingFields), StatusType.ERROR);
            return;
        }

        SepaFormat selectedFormat = sepaFormatComboBox.getValue();

        // Collect global field values
        Map<String, String> globalFieldValues = new HashMap<>();
        for (SepaField field : currentFieldDefinition.getGlobalFields()) {
            Control control = fieldMappingControls.get(field.getFieldName());
            String value = uiUtil.getControlValue(control);
            if (value != null && !value.trim().isEmpty()) {
                globalFieldValues.put(field.getFieldName(), value);
            }
        }

        // Collect transaction field mappings
        Map<String, String> columnMappings = new HashMap<>();
        Map<String, String> defaultValues = new HashMap<>();
        for (SepaField field : currentFieldDefinition.getTransactionFields()) {
            @SuppressWarnings("unchecked")
            ComboBox<String> combo = (ComboBox<String>) fieldMappingControls.get(field.getFieldName() + "_combo");
            Control defaultField = fieldMappingControls.get(field.getFieldName() + "_default");

            String selectedColumn = combo != null ? combo.getValue() : null;
            columnMappings.put(field.getFieldName(), selectedColumn);

            if (FIXED_VALUE_OPTION.equals(selectedColumn)) {
                String value = uiUtil.getControlValue(defaultField);
                if (value != null && !value.trim().isEmpty()) {
                    defaultValues.put(field.getFieldName(), value);
                }
            }
        }

        final char separator = csvUtil.parseSeparator(separatorComboBox.getValue());
        final String encoding = encodingComboBox.getValue();
        final char decimalSeparator = csvUtil.parseDecimalSeparator(decimalSeparatorComboBox.getValue());

        setProcessingState(true);
        setStatus("SEPA XML (" + selectedFormat.getCode() + ") wird vorbereitet...", StatusType.WORKING);

        Task<TransactionValidationResult> buildTask = new Task<>() {
            @Override
            protected TransactionValidationResult call() throws Exception {
                return transactionBuilder.buildTransactions(
                        selectedFile,
                        separator,
                        encoding,
                        decimalSeparator,
                        currentFieldDefinition,
                        globalFieldValues,
                        columnMappings,
                        defaultValues
                );
            }
        };

        buildTask.setOnSucceeded(event -> {
            TransactionValidationResult validationResult = buildTask.getValue();

            if (validationResult.getTotalCount() == 0) {
                setStatus("Keine Transaktionen in der CSV-Datei gefunden.", StatusType.ERROR);
                setProcessingState(false);
                return;
            }

            TransactionPreviewDialog dialog = new TransactionPreviewDialog(validationResult, currentFieldDefinition);
            Optional<File> result = dialog.showAndWait();

            if (result.isEmpty()) {
                setStatus("SEPA XML Generierung abgebrochen.", StatusType.INFO);
                setProcessingState(false);
                return;
            }

            File outputFile = result.get();
            runXmlGenerationTask(outputFile, selectedFormat, validationResult);
        });

        buildTask.setOnFailed(event -> {
            Throwable exception = buildTask.getException();
            if (exception instanceof IOException ioException) {
                setStatus("Fehler beim Lesen der Datei: " + ioException.getMessage(), StatusType.ERROR);
            } else if (exception instanceof CsvException csvException) {
                setStatus("Fehler beim Parsen der CSV: " + csvException.getMessage(), StatusType.ERROR);
            } else {
                setStatus("Fehler beim Erstellen der Transaktionen: " + exception.getMessage(), StatusType.ERROR);
            }
            setProcessingState(false);
        });

        startBackgroundTask(buildTask);
    }

    private void runXmlGenerationTask(File outputFile, SepaFormat format, TransactionValidationResult validationResult) {
        setStatus("SEPA XML (" + format.getCode() + ") wird generiert...", StatusType.WORKING);

        Task<XsdValidationService.ValidationResult> generationTask = new Task<>() {
            @Override
            protected XsdValidationService.ValidationResult call() throws Exception {
                xmlGenerator.generateXml(outputFile, format, validationResult.getValidTransactions());
                return xsdValidator.validateXml(outputFile, format);
            }
        };

        generationTask.setOnSucceeded(event -> {
            XsdValidationService.ValidationResult xsdResult = generationTask.getValue();
            String statusMsg;
            if (xsdResult.isValid()) {
                statusMsg = "SEPA XML erfolgreich gespeichert und validiert: " + outputFile.getName() +
                        " (" + validationResult.getValidTransactions().size() + " Transaktionen)";
                if (validationResult.hasInvalidTransactions()) {
                    statusMsg += " | " + validationResult.getInvalidTransactions().size() + " ungültige Zeile(n) übersprungen";
                }
                setStatus(statusMsg, StatusType.SUCCESS);
            } else {
                statusMsg = "WARNUNG: XML wurde gespeichert, ist aber NICHT XSD-konform: " + outputFile.getName();
                setStatus(statusMsg, StatusType.ERROR);
                showValidationAlert(xsdResult);
            }
            setProcessingState(false);
        });

        generationTask.setOnFailed(event -> {
            Throwable exception = generationTask.getException();
            setStatus("Fehler beim Generieren der SEPA XML: " + exception.getMessage(), StatusType.ERROR);
            setProcessingState(false);
        });

        startBackgroundTask(generationTask);
    }

    private void showValidationAlert(XsdValidationService.ValidationResult xsdResult) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("XSD Validierung fehlgeschlagen");
        alert.setHeaderText("Die generierte XML-Datei entspricht nicht dem SEPA-Standard!");
        alert.setContentText("Validierungsfehler:\n\n" + xsdResult.getErrorsAsString());
        alert.showAndWait();
    }

    private List<String> validateRequiredFields() {
        List<String> missingFields = new ArrayList<>();

        for (SepaField field : currentFieldDefinition.getGlobalFields()) {
            if (field.isRequired()) {
                Control control = fieldMappingControls.get(field.getFieldName());
                String value = uiUtil.getControlValue(control);
                if (value == null || value.trim().isEmpty()) {
                    missingFields.add(field.getDisplayName());
                }
            }
        }

        for (SepaField field : currentFieldDefinition.getTransactionFields()) {
            if (field.isRequired()) {
                ComboBox<String> combo = (ComboBox<String>) fieldMappingControls.get(field.getFieldName() + "_combo");
                Control defaultField = fieldMappingControls.get(field.getFieldName() + "_default");

                if (FIXED_VALUE_OPTION.equals(combo.getValue())) {
                    String value = uiUtil.getControlValue(defaultField);
                    if (value == null || value.trim().isEmpty()) {
                        missingFields.add(field.getDisplayName());
                    }
                }
            }
        }

        return missingFields;
    }

    @FXML
    protected void onSaveConfig() {
        if (currentFieldDefinition == null || csvHeaders == null) {
            setStatus("Keine Feldzuordnung vorhanden zum Speichern.", StatusType.ERROR);
            return;
        }

        // Prompt for configuration name
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Konfiguration speichern");
        dialog.setHeaderText("Konfigurationsname eingeben");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            return;
        }

        String configName = result.get().trim();

        // Check if config already exists
        if (configService.configExists(configName)) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Konfiguration überschreiben");
            confirmDialog.setHeaderText("Eine Konfiguration mit diesem Namen existiert bereits.");
            confirmDialog.setContentText("Möchten Sie die bestehende Konfiguration überschreiben?");

            Optional<ButtonType> confirmResult = confirmDialog.showAndWait();
            if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
                return;
            }
        }

        // Build configuration
        FieldMappingConfig config = new FieldMappingConfig(configName);
        config.setSepaFormat(sepaFormatComboBox.getValue().getCode());
        config.setCsvSeparator(separatorComboBox.getValue());
        config.setCsvEncoding(encodingComboBox.getValue());
        config.setDecimalSeparator(decimalSeparatorComboBox.getValue());

        // Save global field mappings
        for (SepaField field : currentFieldDefinition.getGlobalFields()) {
            Control control = fieldMappingControls.get(field.getFieldName());
            String value = uiUtil.getControlValue(control);
            if (value != null && !value.trim().isEmpty()) {
                config.getGlobalFieldDefaultValues().put(field.getFieldName(), value);
            }
        }

        // Save transaction field mappings
        for (SepaField field : currentFieldDefinition.getTransactionFields()) {
            ComboBox<String> combo = (ComboBox<String>) fieldMappingControls.get(field.getFieldName() + "_combo");
            Control defaultField = fieldMappingControls.get(field.getFieldName() + "_default");

            String selectedColumn = combo.getValue();
            config.getTransactionFieldMappings().put(field.getFieldName(), selectedColumn);

            if (FIXED_VALUE_OPTION.equals(selectedColumn)) {
                String value = uiUtil.getControlValue(defaultField);
                if (value != null && !value.trim().isEmpty()) {
                    config.getTransactionFieldDefaultValues().put(field.getFieldName(), value);
                }
            }
        }

        // Save to file
        try {
            configService.saveConfig(config);
            setStatus("Konfiguration '" + configName + "' erfolgreich gespeichert.", StatusType.SUCCESS);
            refreshConfigList();
            savedConfigsComboBox.setValue(configName);
        } catch (IOException e) {
            setStatus("Fehler beim Speichern: " + e.getMessage(), StatusType.ERROR);
        }
    }

    @FXML
    protected void onLoadConfig() {
        String selectedConfig = savedConfigsComboBox.getValue();
        if (selectedConfig == null || selectedConfig.trim().isEmpty()) {
            setStatus("Bitte wählen Sie eine Konfiguration aus.", StatusType.ERROR);
            return;
        }

        try {
            FieldMappingConfig config = configService.loadConfig(selectedConfig);

            if (config == null) {
                setStatus("Fehler: Konfiguration konnte nicht geladen werden.", StatusType.ERROR);
                return;
            }

            SepaFormatType configType;
            try {
                configType = SepaFormat.resolveType(config.getSepaFormat());
            } catch (IllegalArgumentException e) {
                setStatus("Fehler: Konfiguration enthält ein unbekanntes SEPA-Format.", StatusType.ERROR);
                return;
            }

            SepaFormat selectedFormat = sepaFormatComboBox.getValue();
            if (selectedFormat != null && selectedFormat.getType() != configType) {
                setStatus("Fehler: Die Konfiguration ist für einen anderen SEPA-Typ bestimmt.", StatusType.ERROR);
                return;
            }

            // Apply CSV configuration
            if (config.getCsvSeparator() != null) {
                separatorComboBox.setValue(config.getCsvSeparator());
            }
            if (config.getCsvEncoding() != null) {
                encodingComboBox.setValue(config.getCsvEncoding());
            }
            if (config.getDecimalSeparator() != null) {
                decimalSeparatorComboBox.setValue(config.getDecimalSeparator());
            }

            // Apply SEPA format
            boolean formatMatched = false;
            for (SepaFormat format : SepaFormat.values()) {
                if (format.getCode().equals(config.getSepaFormat())) {
                    sepaFormatComboBox.setValue(format);
                    selectedFormat = format;
                    formatMatched = true;
                    break;
                }
            }

            if (!formatMatched) {
                if (selectedFormat == null || selectedFormat.getType() != configType) {
                    for (SepaFormat format : SepaFormat.values()) {
                        if (format.getType() == configType) {
                            sepaFormatComboBox.setValue(format);
                            selectedFormat = format;
                            break;
                        }
                    }
                }
            }

            // Refresh field mapping if CSV is loaded
            if (csvHeaders != null && csvHeaders.length > 0) {
                showFieldMapping();

                // Apply global field values
                for (Map.Entry<String, String> entry : config.getGlobalFieldDefaultValues().entrySet()) {
                    Control control = fieldMappingControls.get(entry.getKey());
                    if (control instanceof TextField tf) {
                        tf.setText(entry.getValue());
                    } else if (control instanceof DatePicker dp) {
                        try {
                            dp.setValue(java.time.LocalDate.parse(entry.getValue()));
                        } catch (Exception e) {
                            // Ignore parse errors
                        }
                    } else if (control instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> cb = (ComboBox<String>) control;
                        cb.setValue(entry.getValue());
                    }
                }

                // Apply transaction field mappings
                for (Map.Entry<String, String> entry : config.getTransactionFieldMappings().entrySet()) {
                    ComboBox<String> combo = (ComboBox<String>) fieldMappingControls.get(entry.getKey() + "_combo");
                    if (combo != null) {
                        combo.setValue(entry.getValue());
                    }
                }

                // Apply transaction field default values
                for (Map.Entry<String, String> entry : config.getTransactionFieldDefaultValues().entrySet()) {
                    Control control = fieldMappingControls.get(entry.getKey() + "_default");
                    if (control instanceof TextField tf) {
                        tf.setText(entry.getValue());
                    } else if (control instanceof DatePicker dp) {
                        try {
                            dp.setValue(java.time.LocalDate.parse(entry.getValue()));
                        } catch (Exception e) {
                            // Ignore parse errors
                        }
                    } else if (control instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> cb = (ComboBox<String>) control;
                        cb.setValue(entry.getValue());
                    }
                }
            }

            setStatus("Konfiguration '" + selectedConfig + "' erfolgreich geladen.", StatusType.SUCCESS);
        } catch (IOException e) {
            setStatus("Fehler beim Laden: " + e.getMessage(), StatusType.ERROR);
        }
    }

    @FXML
    protected void onDeleteConfig() {
        String selectedConfig = savedConfigsComboBox.getValue();
        if (selectedConfig == null || selectedConfig.trim().isEmpty()) {
            setStatus("Bitte wählen Sie eine Konfiguration aus.", StatusType.ERROR);
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Konfiguration löschen");
        confirmDialog.setHeaderText("Möchten Sie die Konfiguration '" + selectedConfig + "' wirklich löschen?");
        confirmDialog.setContentText("Diese Aktion kann nicht rückgängig gemacht werden.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (configService.deleteConfig(selectedConfig)) {
                setStatus("Konfiguration '" + selectedConfig + "' wurde gelöscht.", StatusType.SUCCESS);
                refreshConfigList();
            } else {
                setStatus("Fehler beim Löschen der Konfiguration.", StatusType.ERROR);
            }
        }
    }

    private void refreshConfigList() {
        savedConfigsComboBox.getItems().clear();

        // Get selected SEPA format
        SepaFormat selectedFormat = sepaFormatComboBox.getValue();
        if (selectedFormat == null) {
            return;
        }

        // Get all configs and filter by format
        List<String> allConfigs = configService.listConfigs();
        List<String> filteredConfigs = new ArrayList<>();

        for (String configName : allConfigs) {
            try {
                FieldMappingConfig config = configService.loadConfig(configName);
                if (config != null) {
                    try {
                        SepaFormatType configType = SepaFormat.resolveType(config.getSepaFormat());
                        if (configType == selectedFormat.getType()) {
                            filteredConfigs.add(configName);
                        }
                    } catch (IllegalArgumentException e) {
                        // Skip configs with invalid format codes
                    }
                }
            } catch (IOException e) {
                // Skip configs that can't be loaded
            }
        }

        savedConfigsComboBox.getItems().addAll(filteredConfigs);
    }
}
