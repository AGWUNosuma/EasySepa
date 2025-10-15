package de.agwu.apps.easysepa.service;

import de.agwu.apps.easysepa.model.sepa.SepaTransaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple template engine for SEPA XML generation using Mustache-like syntax
 */
public class XmlTemplateEngine {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern SECTION_START_PATTERN = Pattern.compile("\\{\\{#([^}]+)\\}\\}");
    private static final Pattern SECTION_END_PATTERN = Pattern.compile("\\{\\{/([^}]+)\\}\\}");
    private static final Pattern INVERTED_SECTION_PATTERN = Pattern.compile("\\{\\{\\^([^}]+)\\}\\}");

    /**
     * Load template from resources
     */
    public String loadTemplate(String templateName) throws IOException {
        String resourcePath = "/de/agwu/apps/easysepa/templates/" + templateName + ".xml";
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Render template with data
     */
    public String render(String template, Map<String, Object> data) {
        return renderWithData(template, data);
    }

    private String renderWithData(String template, Map<String, Object> data) {
        // First, handle sections (loops and conditionals)
        template = processSections(template, data);
        
        // Then replace simple variables
        template = replaceVariables(template, data);
        
        return template;
    }

    private String processSections(String template, Map<String, Object> data) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        
        while (pos < template.length()) {
            // Look for section start {{#variable}} or inverted section {{^variable}}
            int sectionStart = findNextSection(template, pos);
            
            if (sectionStart == -1) {
                // No more sections, append rest
                result.append(template.substring(pos));
                break;
            }
            
            // Append everything before section
            result.append(template.substring(pos, sectionStart));
            
            // Determine if it's a normal or inverted section
            boolean isInverted = template.charAt(sectionStart + 2) == '^';
            int tagStart = sectionStart + (isInverted ? 3 : 3);
            int tagEnd = template.indexOf("}}", tagStart);
            String sectionName = template.substring(tagStart, tagEnd);
            
            // Find matching end tag
            String endTag = "{{/" + sectionName + "}}";
            int sectionEnd = template.indexOf(endTag, tagEnd);
            
            if (sectionEnd == -1) {
                // No matching end tag, skip
                result.append(template.substring(sectionStart, tagEnd + 2));
                pos = tagEnd + 2;
                continue;
            }
            
            // Extract section content
            String sectionContent = template.substring(tagEnd + 2, sectionEnd);
            
            // Process section based on data
            Object value = data.get(sectionName);
            
            if (isInverted) {
                // Inverted section: render if value is false, null, or empty
                if (value == null || value.equals(false) || 
                    (value instanceof String && ((String) value).isEmpty()) ||
                    (value instanceof List && ((List<?>) value).isEmpty())) {
                    result.append(renderWithData(sectionContent, data));
                }
            } else {
                // Normal section
                if (value instanceof List) {
                    // Loop over list
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) value;
                    for (Map<String, Object> item : list) {
                        // Merge parent data with item data
                        Map<String, Object> mergedData = new HashMap<>(data);
                        mergedData.putAll(item);
                        result.append(renderWithData(sectionContent, mergedData));
                    }
                } else if (value != null && !value.equals(false) && 
                           !(value instanceof String && ((String) value).isEmpty())) {
                    // Value exists and is truthy, render once
                    result.append(renderWithData(sectionContent, data));
                }
            }
            
            // Move position after section end
            pos = sectionEnd + endTag.length();
        }
        
        return result.toString();
    }

    private int findNextSection(String template, int fromIndex) {
        int normalSection = template.indexOf("{{#", fromIndex);
        int invertedSection = template.indexOf("{{^", fromIndex);
        
        if (normalSection == -1) return invertedSection;
        if (invertedSection == -1) return normalSection;
        
        return Math.min(normalSection, invertedSection);
    }

    private String replaceVariables(String template, Map<String, Object> data) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = data.get(varName);
            String replacement = value != null ? escapeXml(value.toString()) : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * Convert SepaTransaction list to template data format
     */
    public List<Map<String, Object>> convertTransactionsToData(List<SepaTransaction> transactions) {
        return transactions.stream()
                .map(this::convertTransactionToData)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertTransactionToData(SepaTransaction transaction) {
        Map<String, Object> data = new HashMap<>();
        
        // Add all transaction fields
        for (String fieldName : transaction.getAllFieldNames()) {
            String value = transaction.getField(fieldName);
            if (value != null && !value.trim().isEmpty()) {
                data.put(fieldName, value);
            }
        }
        
        return data;
    }
}
