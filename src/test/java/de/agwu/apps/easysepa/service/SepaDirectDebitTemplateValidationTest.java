package de.agwu.apps.easysepa.service;

import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import de.agwu.apps.easysepa.model.sepa.SepaTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SepaDirectDebitTemplateValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void pain00800108TemplateValidatesAgainstSchema() throws IOException {
        File outputFile = tempDir.resolve("pain008-08.xml").toFile();

        SepaXmlGenerator generator = new SepaXmlGenerator();
        generator.generateXml(outputFile, SepaFormat.PAIN_008_001_08, List.of(createDirectDebitTransaction()));

        XsdValidationService.ValidationResult result =
                new XsdValidationService().validateXml(outputFile, SepaFormat.PAIN_008_001_08);

        assertTrue(result.isValid(), () -> String.join(System.lineSeparator(), result.getErrors()));
        String xml = Files.readString(outputFile.toPath());
        assertTrue(xml.contains("<BtchBookg>true</BtchBookg>"));
    }

    @Test
    void pain00800111TemplateValidatesAgainstSchema() throws IOException {
        File outputFile = tempDir.resolve("pain008-11.xml").toFile();

        SepaXmlGenerator generator = new SepaXmlGenerator();
        generator.generateXml(outputFile, SepaFormat.PAIN_008_001_11, List.of(createDirectDebitTransaction()));

        XsdValidationService.ValidationResult result =
                new XsdValidationService().validateXml(outputFile, SepaFormat.PAIN_008_001_11);

        assertTrue(result.isValid(), () -> String.join(System.lineSeparator(), result.getErrors()));
        String xml = Files.readString(outputFile.toPath());
        assertTrue(xml.contains("<BtchBookg>true</BtchBookg>"));
    }

    private SepaTransaction createDirectDebitTransaction() {
        SepaTransaction transaction = new SepaTransaction(1);
        transaction.setField("msgId", "MSG-2025-0001");
        transaction.setField("initiatorName", "Initiator AG");
        transaction.setField("pmtInfId", "PMT-INFO-001");
        transaction.setField("batchBooking", "true");
        transaction.setField("reqdColltnDt", "2025-01-15");
        transaction.setField("creditorName", "Creditor GmbH");
        transaction.setField("creditorIBAN", "DE12500105170648489890");
        transaction.setField("creditorBIC", "MARKDEF1100");
        transaction.setField("creditorId", "DE98ZZZ09999999999");
        transaction.setField("seqTp", "FRST");
        transaction.setField("localInstrumentCode", "CORE");

        transaction.setField("endToEndId", "E2E-0001");
        transaction.setField("amount", "199.99");
        transaction.setField("mandateId", "MAND-0001");
        transaction.setField("mandateSignatureDate", "2024-12-01");
        transaction.setField("debtorName", "Max Mustermann");
        transaction.setField("debtorIBAN", "DE89370400440532013000");
        transaction.setField("debtorBIC", "COBADEFFXXX");
        transaction.setField("remittanceInfo", "Invoice 2025-0001");

        return transaction;
    }
}

