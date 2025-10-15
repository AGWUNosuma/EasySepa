package de.agwu.apps.easysepa.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Utility to detect file encoding
 */
public class EncodingDetector {

    /**
     * Detect encoding of a file
     * Returns best guess based on byte patterns
     */
    public static String detectEncoding(File file) throws IOException {
        // Read first few bytes to check for BOM
        byte[] bom = new byte[4];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(bom);
            
            if (bytesRead >= 3) {
                // UTF-8 BOM: EF BB BF
                if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                    return "UTF-8";
                }
                
                // UTF-16 BE BOM: FE FF
                if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
                    return "UTF-16BE";
                }
                
                // UTF-16 LE BOM: FF FE
                if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
                    return "UTF-16LE";
                }
            }
        }
        
        // No BOM found, try to detect by content
        return detectByContent(file);
    }

    /**
     * Detect encoding by analyzing file content
     */
    private static String detectByContent(File file) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        try (FileInputStream fis = new FileInputStream(file)) {
            bytesRead = fis.read(buffer);
        }
        
        if (bytesRead <= 0) {
            return "UTF-8"; // Default
        }
        
        // Check for typical German encoding byte sequences
        boolean hasGermanISO = false;
        for (int i = 0; i < bytesRead; i++) {
            int b = buffer[i] & 0xFF;
            // Check for German umlauts in ISO-8859-1/Windows-1252 range
            // ä=0xE4, ö=0xF6, ü=0xFC, Ä=0xC4, Ö=0xD6, Ü=0xDC, ß=0xDF
            if (b == 0xE4 || b == 0xF6 || b == 0xFC || 
                b == 0xC4 || b == 0xD6 || b == 0xDC || b == 0xDF) {
                hasGermanISO = true;
                break;
            }
        }
        
        if (hasGermanISO) {
            // Found German umlauts in ISO-8859-1 range
            return "ISO-8859-1";
        }
        
        // Test if valid UTF-8
        if (isValidUTF8(buffer, bytesRead)) {
            return "UTF-8";
        }
        
        // Default to ISO-8859-1 for compatibility
        return "ISO-8859-1";
    }

    /**
     * Check if byte sequence is valid UTF-8
     */
    private static boolean isValidUTF8(byte[] buffer, int length) {
        int i = 0;
        while (i < length) {
            if ((buffer[i] & 0x80) == 0) {
                // Single-byte character (ASCII)
                i++;
            } else if ((buffer[i] & 0xE0) == 0xC0) {
                // Two-byte character
                if (i + 1 >= length || (buffer[i + 1] & 0xC0) != 0x80) {
                    return false;
                }
                i += 2;
            } else if ((buffer[i] & 0xF0) == 0xE0) {
                // Three-byte character
                if (i + 2 >= length || 
                    (buffer[i + 1] & 0xC0) != 0x80 || 
                    (buffer[i + 2] & 0xC0) != 0x80) {
                    return false;
                }
                i += 3;
            } else if ((buffer[i] & 0xF8) == 0xF0) {
                // Four-byte character
                if (i + 3 >= length || 
                    (buffer[i + 1] & 0xC0) != 0x80 || 
                    (buffer[i + 2] & 0xC0) != 0x80 ||
                    (buffer[i + 3] & 0xC0) != 0x80) {
                    return false;
                }
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if buffer contains German characters in given encoding
     */
    private static boolean containsGermanChars(byte[] buffer, int length, String encoding) {
        try {
            String text = new String(buffer, 0, length, Charset.forName(encoding));
            // Look for German umlauts
            return text.matches(".*[äöüÄÖÜß].*");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate that text can be properly encoded/decoded
     */
    public static boolean validateEncoding(String text, String encoding) {
        try {
            Charset charset = Charset.forName(encoding);
            byte[] encoded = text.getBytes(charset);
            String decoded = new String(encoded, charset);
            return text.equals(decoded);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get preview of file content in different encodings
     */
    public static String getEncodingPreview(File file, String encoding) throws IOException {
        StringBuilder preview = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), Charset.forName(encoding)))) {
            
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 5) {
                preview.append(line).append("\n");
                lineCount++;
            }
        }
        
        return preview.toString();
    }
}
