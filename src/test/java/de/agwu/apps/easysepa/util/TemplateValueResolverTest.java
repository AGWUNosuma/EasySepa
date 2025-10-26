package de.agwu.apps.easysepa.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class TemplateValueResolverTest {

    @Test
    void compilesOnlyWhenPlaceholdersPresent() {
        assertTrue(TemplateValueResolver.compile("Invoice-{id}").isPresent());
        assertTrue(TemplateValueResolver.compile("{today}").isPresent());
        assertTrue(TemplateValueResolver.compile("Order-{uuid:nodash,8}").isPresent());
        assertTrue(TemplateValueResolver.compile("TX-{row}-{random:4}").isPresent());
        assertFalse(TemplateValueResolver.compile("Plain text without braces").isPresent());
    }

    @Test
    void generatesIncrementingIds() {
        TemplateValueResolver.TemplateExpression expression = TemplateValueResolver.compile("Prefix-{id:1000,pad=4}")
                .orElseThrow();

        String first = expression.render(1, 1);
        String second = expression.render(2, 2);
        String third = expression.render(3, 3);

        assertEquals("Prefix-1000", first);
        assertEquals("Prefix-1001", second);
        assertEquals("Prefix-1002", third);
    }

    @Test
    void mixesDateTimeAndRandomSegments() {
        TemplateValueResolver.TemplateExpression expression = TemplateValueResolver
                .compile("INV-{today}-{time}-{random:6}")
                .orElseThrow();

        String value = expression.render(1, 1);
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertTrue(value.startsWith("INV-" + today + "-"));
        String[] parts = value.split("-");
        assertEquals(4, parts.length);
        assertEquals(6, parts[3].length());
        assertTrue(parts[3].chars().allMatch(Character::isDigit));
    }

    @Test
    void reusesSameCounterWithinTemplate() {
        TemplateValueResolver.TemplateExpression expression = TemplateValueResolver
                .compile("Run-{id:5}-{id:5}")
                .orElseThrow();

        assertEquals("Run-5-5", expression.render(1, 1));
        assertEquals("Run-6-6", expression.render(2, 2));
    }

    @Test
    void fallsBackToLiteralForUnknownPlaceholder() {
        TemplateValueResolver.TemplateExpression expression = TemplateValueResolver
                .compile("Value-{unknown}")
                .orElseThrow();

        assertEquals("Value-{unknown}", expression.render(1, 1));
    }
}
