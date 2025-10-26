package de.agwu.apps.easysepa.model.sepa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SepaFormatTest {

    @Test
    void resolveTypeIdentifiesCreditTransfer() {
        assertEquals(SepaFormatType.CREDIT_TRANSFER, SepaFormat.resolveType("pain.001.001.03"));
        assertEquals(SepaFormatType.CREDIT_TRANSFER, SepaFormat.resolveType("pain.001.001.09"));
    }

    @Test
    void resolveTypeIdentifiesDirectDebit() {
        assertEquals(SepaFormatType.DIRECT_DEBIT, SepaFormat.resolveType("pain.008.001.08"));
        assertEquals(SepaFormatType.DIRECT_DEBIT, SepaFormat.resolveType("pain.008.001.11"));
    }

    @Test
    void invalidFormatCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SepaFormat.resolveType("pain.999.001.01"));
        assertThrows(IllegalArgumentException.class, () -> SepaFormat.resolveType("invalid"));
    }

    @Test
    void validationRecognisesSpecificationPattern() {
        assertTrue(SepaFormat.isValidFormatCode("pain.001.001.03"));
        assertTrue(SepaFormat.isValidFormatCode("pain.008.001.11"));
        assertFalse(SepaFormat.isValidFormatCode("pain.008"));
        assertFalse(SepaFormat.isValidFormatCode(null));
    }
}
