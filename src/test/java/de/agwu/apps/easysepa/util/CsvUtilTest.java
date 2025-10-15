package de.agwu.apps.easysepa.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvUtilTest {

    private final CsvUtil csvUtil = new CsvUtil();

    @Test
    void normalizeDecimalValueWithCommaSeparator() {
        String normalized = csvUtil.normalizeDecimalValue("1.234,56", ',');
        assertEquals("1234.56", normalized);
    }

    @Test
    void normalizeDecimalValueWithDotSeparator() {
        String normalized = csvUtil.normalizeDecimalValue("1,234.56", '.');
        assertEquals("1234.56", normalized);
    }
}
