package com.coderace.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HTML Sanitization Service
 * Validates XSS protection for problem descriptions
 */
class HtmlSanitizationServiceTest {

    private final HtmlSanitizationService sanitizer = new HtmlSanitizationService();

    @Test
    void testSanitizeBasicHTML() {
        String input = "<p>This is a <strong>problem</strong> description.</p>";
        String output = sanitizer.sanitizeProblemDescription(input);

        // Should keep safe HTML
        assertTrue(output.contains("<p>"));
        assertTrue(output.contains("<strong>"));
        assertTrue(output.contains("problem"));
    }

    @Test
    void testStripDangerousScript() {
        String input = "<p>Problem</p><script>alert('XSS')</script>";
        String output = sanitizer.sanitizeProblemDescription(input);

        // Should remove script tags
        assertFalse(output.contains("<script>"));
        assertFalse(output.contains("alert"));
        // But keep safe content
        assertTrue(output.contains("Problem"));
    }

    @Test
    void testStripEventHandlers() {
        String input = "<p onclick=\"malicious()\">Click me</p>";
        String output = sanitizer.sanitizeProblemDescription(input);

        // Should remove onclick attribute
        assertFalse(output.contains("onclick"));
        assertFalse(output.contains("malicious"));
        // But keep text
        assertTrue(output.contains("Click me"));
    }

    @Test
    void testAllowCodeBlocks() {
        String input = "<pre><code>int x = 5;</code></pre>";
        String output = sanitizer.sanitizeProblemDescription(input);

        // Should allow code formatting
        assertTrue(output.contains("<pre>"));
        assertTrue(output.contains("<code>"));
        assertTrue(output.contains("int x = 5;"));
    }

    @Test
    void testAllowSafeStyling() {
        String input = "<p><strong>Bold</strong> and <em>italic</em></p>";
        String output = sanitizer.sanitizeProblemDescription(input);

        // Should allow basic text formatting
        assertTrue(output.contains("<strong>"));
        assertTrue(output.contains("<em>"));
    }

    @Test
    void testStripIframes() {
        String input = "<p>Text</p><iframe src=\"evil.com\"></iframe>";
        String output = sanitizer.sanitizeProblemDescription(input);

        // Should remove iframe
        assertFalse(output.contains("<iframe"));
        assertFalse(output.contains("evil.com"));
        assertTrue(output.contains("Text"));
    }

    @Test
    void testSanitizePlainText() {
        String input = "<script>alert('xss')</script>Plain text";
        String output = sanitizer.sanitizePlainText(input);

        // Should escape all HTML
        assertTrue(output.contains("&lt;script&gt;"));
        assertTrue(output.contains("&lt;/script&gt;"));
    }

    @Test
    void testNullInput() {
        String output = sanitizer.sanitizeProblemDescription(null);
        assertNull(output);
    }

    @Test
    void testEmptyInput() {
        String output = sanitizer.sanitizeProblemDescription("");
        assertEquals("", output);
    }
}
