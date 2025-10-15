package de.agwu.apps.easysepa.view;

import de.agwu.apps.easysepa.model.sepa.SepaField;
import de.agwu.apps.easysepa.model.sepa.SepaTransaction;
import de.agwu.apps.easysepa.model.sepa.TransactionValidationResult;
import de.agwu.apps.easysepa.model.sepa.definition.ISepaFieldDefinition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Optional;

/**
 * Dialog to preview SEPA transactions before saving
 */
public class TransactionPreviewDialog extends Dialog<File> {

    private final TransactionValidationResult validationResult;
    private final ISepaFieldDefinition fieldDefinition;

    public TransactionPreviewDialog(TransactionValidationResult validationResult, ISepaFieldDefinition fieldDefinition) {
        this.validationResult = validationResult;
        this.fieldDefinition = fieldDefinition;

        setTitle("SEPA Transaktionen - Vorschau");

        String headerText = validationResult.getValidTransactions().size() + " gültige Transaktion(en)";
        if (validationResult.hasInvalidTransactions()) {
            headerText += " | " + validationResult.getInvalidTransactions().size() + " ungültige Transaktion(en)";
        }
        setHeaderText(headerText);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 10;");

        // Valid transactions table
        if (!validationResult.getValidTransactions().isEmpty()) {
            content.getChildren().add(new Label("Gültige Transaktionen:"));
            TableView<SepaTransaction> validTable = createValidTransactionsTable();
            validTable.setPrefHeight(300);
            validTable.setPrefWidth(800);
            content.getChildren().add(validTable);
        }

        // Invalid transactions table
        if (validationResult.hasInvalidTransactions()) {
            content.getChildren().add(new Separator());
            Label invalidLabel = new Label("Ungültige Transaktionen (werden NICHT in die XML aufgenommen):");
            invalidLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
            content.getChildren().add(invalidLabel);

            TableView<TransactionValidationResult.InvalidTransaction> invalidTable = createInvalidTransactionsTable();
            invalidTable.setPrefHeight(200);
            invalidTable.setPrefWidth(800);
            content.getChildren().add(invalidTable);
        }

        getDialogPane().setContent(content);

        // Only show OK button if there are valid transactions
        if (validationResult.getValidTransactions().isEmpty()) {
            getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        } else {
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        }

        // Set result converter
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return showSaveDialog();
            }
            return null;
        });
    }

    private TableView<SepaTransaction> createValidTransactionsTable() {
        TableView<SepaTransaction> tableView = new TableView<>();
        tableView.setItems(FXCollections.observableArrayList(validationResult.getValidTransactions()));

        // Row number column
        TableColumn<SepaTransaction, String> rowCol = new TableColumn<>("Zeile");
        rowCol.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(param.getValue().getRowNumber())));
        rowCol.setPrefWidth(60);
        tableView.getColumns().add(rowCol);

        // Add columns for transaction fields only (not global fields)
        for (SepaField field : fieldDefinition.getTransactionFields()) {
            TableColumn<SepaTransaction, String> column = new TableColumn<>(field.getDisplayName());
            column.setCellValueFactory(param -> {
                String value = param.getValue().getField(field.getFieldName());
                return new SimpleStringProperty(value != null ? value : "");
            });
            column.setPrefWidth(150);
            tableView.getColumns().add(column);
        }

        return tableView;
    }

    private TableView<TransactionValidationResult.InvalidTransaction> createInvalidTransactionsTable() {
        TableView<TransactionValidationResult.InvalidTransaction> tableView = new TableView<>();
        tableView.setItems(FXCollections.observableArrayList(validationResult.getInvalidTransactions()));

        // Row number column
        TableColumn<TransactionValidationResult.InvalidTransaction, String> rowCol = new TableColumn<>("Zeile");
        rowCol.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(param.getValue().getTransaction().getRowNumber())));
        rowCol.setPrefWidth(60);
        tableView.getColumns().add(rowCol);

        // Errors column
        TableColumn<TransactionValidationResult.InvalidTransaction, String> errorCol = new TableColumn<>("Fehler");
        errorCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getErrorsAsString()));
        errorCol.setPrefWidth(300);
        tableView.getColumns().add(errorCol);

        // Add columns for transaction fields
        for (SepaField field : fieldDefinition.getTransactionFields()) {
            TableColumn<TransactionValidationResult.InvalidTransaction, String> column = new TableColumn<>(field.getDisplayName());
            column.setCellValueFactory(param -> {
                String value = param.getValue().getTransaction().getField(field.getFieldName());
                return new SimpleStringProperty(value != null ? value : "");
            });
            column.setPrefWidth(120);
            tableView.getColumns().add(column);
        }

        return tableView;
    }

    private File showSaveDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("SEPA XML Datei speichern");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Dateien", "*.xml")
        );
        fileChooser.setInitialFileName("sepa_" + System.currentTimeMillis() + ".xml");

        return fileChooser.showSaveDialog(getOwner());
    }
}
