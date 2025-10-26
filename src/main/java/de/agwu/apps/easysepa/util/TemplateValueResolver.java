package de.agwu.apps.easysepa.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Parses and evaluates dynamic placeholder expressions that can be used in default values.
 */
public final class TemplateValueResolver {

    private TemplateValueResolver() {
        // Utility class
    }

    /**
     * Compile the provided template string. Returns empty when the template does not contain placeholders.
     */
    public static Optional<TemplateExpression> compile(String template) {
        if (template == null) {
            return Optional.empty();
        }
        if (!template.contains("{") || !template.contains("}")) {
            return Optional.empty();
        }

        List<Segment> segments = new ArrayList<>();
        Map<String, Placeholder> placeholderCache = new ConcurrentHashMap<>();

        int index = 0;
        while (index < template.length()) {
            int start = template.indexOf('{', index);
            if (start == -1) {
                segments.add(new LiteralSegment(template.substring(index)));
                break;
            }

            if (start > index) {
                segments.add(new LiteralSegment(template.substring(index, start)));
            }

            int end = template.indexOf('}', start + 1);
            if (end == -1) {
                segments.add(new LiteralSegment(template.substring(start)));
                break;
            }

            String rawPlaceholder = template.substring(start + 1, end).trim();
            if (rawPlaceholder.isEmpty()) {
                segments.add(new LiteralSegment("{}"));
            } else {
                Placeholder placeholder = placeholderCache.computeIfAbsent(rawPlaceholder, TemplateValueResolver::createPlaceholder);
                segments.add(new PlaceholderSegment(placeholder));
            }
            index = end + 1;
        }

        return segments.stream().anyMatch(segment -> segment instanceof PlaceholderSegment)
                ? Optional.of(new TemplateExpression(segments))
                : Optional.empty();
    }

    public static final class TemplateExpression {
        private final List<Segment> segments;

        private TemplateExpression(List<Segment> segments) {
            this.segments = segments;
        }

        public synchronized String render(int transactionIndex, int rowNumber) {
            TemplateContext context = TemplateContext.now(transactionIndex, rowNumber);
            StringBuilder builder = new StringBuilder();
            for (Segment segment : segments) {
                segment.append(builder, context);
            }
            return builder.toString();
        }
    }

    private interface Segment {
        void append(StringBuilder builder, TemplateContext context);
    }

    private static final class LiteralSegment implements Segment {
        private final String literal;

        private LiteralSegment(String literal) {
            this.literal = literal;
        }

        @Override
        public void append(StringBuilder builder, TemplateContext context) {
            builder.append(literal);
        }
    }

    private static final class PlaceholderSegment implements Segment {
        private final Placeholder placeholder;

        private PlaceholderSegment(Placeholder placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public void append(StringBuilder builder, TemplateContext context) {
            builder.append(placeholder.resolve(context));
        }
    }

    private interface Placeholder {
        String resolve(TemplateContext context);
    }

    private static Placeholder createPlaceholder(String rawPlaceholder) {
        String key;
        String argument = "";
        int separatorIndex = rawPlaceholder.indexOf(':');
        if (separatorIndex >= 0) {
            key = rawPlaceholder.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            argument = rawPlaceholder.substring(separatorIndex + 1).trim();
        } else {
            key = rawPlaceholder.trim().toLowerCase(Locale.ROOT);
        }

        return switch (key) {
            case "id", "counter" -> new IdPlaceholder(argument);
            case "today", "date" -> new DatePlaceholder(argument);
            case "datetime", "timestamp" -> new DateTimePlaceholder(argument);
            case "time" -> new TimePlaceholder(argument);
            case "row", "rownumber" -> context -> Integer.toString(Math.max(1, context.getRowNumber()));
            case "index", "tx", "transaction" -> context -> Integer.toString(Math.max(1, context.getTransactionIndex()));
            case "uuid" -> new UuidPlaceholder(argument);
            case "random", "randomdigits" -> new RandomDigitsPlaceholder(argument);
            case "randomalpha" -> new RandomAlphaPlaceholder(argument, false);
            case "randomalnum", "randommixed" -> new RandomAlphaPlaceholder(argument, true);
            default -> new LiteralPlaceholder("{" + rawPlaceholder + "}");
        };
    }

    private static final class TemplateContext {
        private final int transactionIndex;
        private final int rowNumber;
        private final LocalDate date;
        private final LocalDateTime dateTime;
        private final LocalTime time;
        private final Map<Placeholder, String> cache = new IdentityHashMap<>();

        private TemplateContext(int transactionIndex, int rowNumber, LocalDate date,
                                LocalDateTime dateTime, LocalTime time) {
            this.transactionIndex = transactionIndex;
            this.rowNumber = rowNumber;
            this.date = date;
            this.dateTime = dateTime;
            this.time = time;
        }

        private static TemplateContext now(int transactionIndex, int rowNumber) {
            LocalDateTime now = LocalDateTime.now();
            return new TemplateContext(transactionIndex, rowNumber, now.toLocalDate(), now, now.toLocalTime());
        }

        int getTransactionIndex() {
            return transactionIndex;
        }

        int getRowNumber() {
            return rowNumber;
        }

        LocalDate getDate() {
            return date;
        }

        LocalDateTime getDateTime() {
            return dateTime;
        }

        LocalTime getTime() {
            return time;
        }

        String computeIfAbsent(Placeholder placeholder, Supplier<String> supplier) {
            return cache.computeIfAbsent(placeholder, key -> supplier.get());
        }
    }

    private static final class LiteralPlaceholder implements Placeholder {
        private final String literal;

        private LiteralPlaceholder(String literal) {
            this.literal = literal;
        }

        @Override
        public String resolve(TemplateContext context) {
            return literal;
        }
    }

    private static final class IdPlaceholder implements Placeholder {
        private long nextValue;
        private final long step;
        private final int padding;

        private IdPlaceholder(String argument) {
            long start = 1L;
            long stepValue = 1L;
            int pad = 0;
            if (!argument.isEmpty()) {
                String[] parts = argument.split(",");
                boolean startAssigned = false;
                boolean padAssigned = false;
                for (String raw : parts) {
                    String part = raw.trim();
                    if (part.isEmpty()) {
                        continue;
                    }
                    String lower = part.toLowerCase(Locale.ROOT);
                    if (lower.startsWith("start=")) {
                        start = parseLongSafely(part.substring(6), 1L);
                        startAssigned = true;
                    } else if (lower.startsWith("step=")) {
                        stepValue = Math.max(1L, parseLongSafely(part.substring(5), 1L));
                    } else if (lower.startsWith("pad=") || lower.startsWith("width=")) {
                        pad = Math.max(0, parseIntSafely(part.substring(part.indexOf('=') + 1), 0));
                        padAssigned = true;
                    } else if (isNumeric(part)) {
                        if (!startAssigned) {
                            start = parseLongSafely(part, 1L);
                            startAssigned = true;
                        } else if (!padAssigned) {
                            pad = Math.max(0, parseIntSafely(part, 0));
                            padAssigned = true;
                        }
                    }
                }
            }
            this.nextValue = start;
            this.step = Math.max(1L, stepValue);
            this.padding = Math.max(0, pad);
        }

        @Override
        public String resolve(TemplateContext context) {
            return context.computeIfAbsent(this, () -> {
                long current = nextValue;
                nextValue += step;
                if (padding > 0) {
                    return String.format(Locale.ROOT, "%0" + padding + "d", current);
                }
                return Long.toString(current);
            });
        }
    }

    private static final class DatePlaceholder implements Placeholder {
        private final DateTimeFormatter formatter;

        private DatePlaceholder(String argument) {
            this.formatter = buildFormatter(argument, "yyyyMMdd");
        }

        @Override
        public String resolve(TemplateContext context) {
            return context.getDate().format(formatter);
        }
    }

    private static final class DateTimePlaceholder implements Placeholder {
        private final DateTimeFormatter formatter;

        private DateTimePlaceholder(String argument) {
            this.formatter = buildFormatter(argument, "yyyyMMddHHmmss");
        }

        @Override
        public String resolve(TemplateContext context) {
            return context.getDateTime().format(formatter);
        }
    }

    private static final class TimePlaceholder implements Placeholder {
        private final DateTimeFormatter formatter;

        private TimePlaceholder(String argument) {
            this.formatter = buildFormatter(argument, "HHmmss");
        }

        @Override
        public String resolve(TemplateContext context) {
            return context.getTime().format(formatter);
        }
    }

    private static final class UuidPlaceholder implements Placeholder {
        private final boolean stripDashes;
        private final int length;

        private UuidPlaceholder(String argument) {
            boolean noDashes = false;
            int requestedLength = 0;
            if (!argument.isEmpty()) {
                String[] parts = argument.split(",");
                for (String raw : parts) {
                    String part = raw.trim();
                    if (part.isEmpty()) {
                        continue;
                    }
                    String lower = part.toLowerCase(Locale.ROOT);
                    if (lower.equals("nodash") || lower.equals("strip") || lower.equals("clean")) {
                        noDashes = true;
                    } else if (lower.equals("short")) {
                        requestedLength = 8;
                    } else if (isNumeric(part)) {
                        requestedLength = Math.max(1, parseIntSafely(part, 0));
                    }
                }
            }
            this.stripDashes = noDashes || requestedLength > 0;
            this.length = requestedLength;
        }

        @Override
        public String resolve(TemplateContext context) {
            String value = UUID.randomUUID().toString();
            if (stripDashes) {
                value = value.replace("-", "");
            }
            if (length > 0 && length < value.length()) {
                return value.substring(0, length);
            }
            return value;
        }
    }

    private static final class RandomDigitsPlaceholder implements Placeholder {
        private final int length;

        private RandomDigitsPlaceholder(String argument) {
            this.length = Math.max(1, parseIntSafely(argument, 6));
        }

        @Override
        public String resolve(TemplateContext context) {
            StringBuilder builder = new StringBuilder(length);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < length; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        }
    }

    private static final class RandomAlphaPlaceholder implements Placeholder {
        private final int length;
        private final boolean alphanumeric;

        private RandomAlphaPlaceholder(String argument, boolean alphanumeric) {
            this.length = Math.max(1, parseIntSafely(argument, 6));
            this.alphanumeric = alphanumeric;
        }

        @Override
        public String resolve(TemplateContext context) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            StringBuilder builder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                if (alphanumeric && random.nextBoolean()) {
                    builder.append(random.nextInt(10));
                } else {
                    char base = random.nextBoolean() ? 'A' : 'a';
                    builder.append((char) (base + random.nextInt(26)));
                }
            }
            return builder.toString();
        }
    }

    private static DateTimeFormatter buildFormatter(String pattern, String fallback) {
        String resolvedPattern = (pattern == null || pattern.isEmpty()) ? fallback : pattern;
        try {
            return DateTimeFormatter.ofPattern(resolvedPattern).withLocale(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return DateTimeFormatter.ofPattern(fallback).withLocale(Locale.ROOT);
        }
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (i == 0 && (ch == '+' || ch == '-')) {
                if (value.length() == 1) {
                    return false;
                }
                continue;
            }
            if (!Character.isDigit(ch)) {
                return false;
            }
        }
        return true;
    }

    private static long parseLongSafely(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int parseIntSafely(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
