package de.agwu.apps.easysepa.service;

import com.opencsv.exceptions.CsvException;
import de.agwu.apps.easysepa.model.sepa.SepaField;
import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import de.agwu.apps.easysepa.model.sepa.TransactionValidationResult;
import de.agwu.apps.easysepa.model.sepa.definition.ISepaFieldDefinition;
import de.agwu.apps.easysepa.util.FieldMappingConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SepaTransactionBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsTransactionsAndCollectsErrors() throws IOException, CsvException {
        Path csvFile = tempDir.resolve("sample.csv");
        Files.writeString(csvFile, String.join(System.lineSeparator(),
                "debtorName;amount;reference",
                "Alice;1.234,56;Invoice 1",
                "Bob;;Invoice 2"
        ));

        ISepaFieldDefinition definition = new TestDefinition();

        SepaTransactionBuilder builder = new SepaTransactionBuilder();

        Map<String, String> globalFieldValues = Map.of("msgId", "MSG-1");
        Map<String, String> columnMappings = Map.of(
                "debtorName", "debtorName",
                "amount", "amount",
                "optionalReference", FieldMappingConstants.FIXED_VALUE_OPTION
        );
        Map<String, String> defaultValues = Map.of("optionalReference", "DefaultRef");

        TransactionValidationResult result = builder.buildTransactions(
                csvFile.toFile(),
                ';',
                "UTF-8",
                ',',
                definition,
                globalFieldValues,
                columnMappings,
                defaultValues
        );

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(1, result.getInvalidTransactions().size());

        var validTransaction = result.getValidTransactions().get(0);
        assertEquals("Alice", validTransaction.getField("debtorName"));
        assertEquals("1234.56", validTransaction.getField("amount"));
        assertEquals("DefaultRef", validTransaction.getField("optionalReference"));
        assertEquals("MSG-1", validTransaction.getField("msgId"));

        var invalidErrors = result.getInvalidTransactions().get(0).getErrors();
        assertFalse(invalidErrors.isEmpty());
        assertTrue(invalidErrors.get(0).contains("Amount"));
    }

    private static class TestDefinition implements ISepaFieldDefinition {
        private final List<SepaField> globalFields = List.of(
                new SepaField("msgId", "Message ID", true, "Message identifier")
        );

        private final List<SepaField> transactionFields = List.of(
                new SepaField("debtorName", "Debtor Name", true, ""),
                new SepaField("amount", "Amount", true, ""),
                new SepaField("optionalReference", "Optional Reference", false, "")
        );

        @Override
        public SepaFormat getFormat() {
            return SepaFormat.PAIN_001_001_03;
        }

        @Override
        public List<SepaField> getGlobalFields() {
            return globalFields;
        }

        @Override
        public List<SepaField> getTransactionFields() {
            return transactionFields;
        }
    }
}
