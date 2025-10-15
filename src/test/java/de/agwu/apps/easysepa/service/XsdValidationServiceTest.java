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

import static org.junit.jupiter.api.Assertions.*;

class XsdValidationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void validatesGeneratedXmlAgainstSchema() throws IOException {
        SepaTransaction transaction = new SepaTransaction(1);
        transaction.setField("msgId", "MSG-100");
        transaction.setField("initiatorName", "Initiator AG");
        transaction.setField("pmtInfId", "PMT-1");
        transaction.setField("reqdExctnDt", "2025-01-01");
        transaction.setField("debtorName", "Debtor GmbH");
        transaction.setField("debtorIBAN", "DE89370400440532013000");
        transaction.setField("debtorBIC", "COBADEFFXXX");
        transaction.setField("endToEndId", "E2E-1");
        transaction.setField("amount", "100.00");
        transaction.setField("creditorBIC", "MARKDEF1100");
        transaction.setField("creditorName", "Creditor GmbH");
        transaction.setField("creditorIBAN", "DE12500105170648489890");
        transaction.setField("remittanceInfo", "Invoice 1");

        File outputFile = tempDir.resolve("valid.xml").toFile();

        SepaXmlGenerator generator = new SepaXmlGenerator();
        generator.generateXml(outputFile, SepaFormat.PAIN_001_001_03, List.of(transaction));

        XsdValidationService validator = new XsdValidationService();
        XsdValidationService.ValidationResult result = validator.validateXml(outputFile, SepaFormat.PAIN_001_001_03);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void detectsInvalidXml() throws IOException {
        File invalidFile = tempDir.resolve("invalid.xml").toFile();
        Files.writeString(invalidFile.toPath(), "<Document></Document>");

        XsdValidationService validator = new XsdValidationService();
        XsdValidationService.ValidationResult result = validator.validateXml(invalidFile, SepaFormat.PAIN_001_001_03);

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }
}
