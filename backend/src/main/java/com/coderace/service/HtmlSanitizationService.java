package com.coderace.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

/**
 * HTML Sanitization Service
 * Protects against XSS attacks by sanitizing user-generated and LLM-generated
 * HTML content
 */
@Service
public class HtmlSanitizationService {

    private final PolicyFactory policy;

    public HtmlSanitizationService() {
        // Define allowed HTML elements for problem descriptions
        // Using simple whitelist approach for maximum compatibility
        this.policy = new HtmlPolicyBuilder()
                // Text formatting
                .allowElements("p", "br", "strong", "em", "b", "i", "u", "code", "pre", "span")

                // Lists
                .allowElements("ul", "ol", "li")

                // Headings
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6")

                // Divs for code blocks
                .allowElements("div")

                // Links (with nofollow)
                .allowElements("a")
                .allowStandardUrlProtocols()
                .allowAttributes("href").onElements("a")
                .requireRelNofollowOnLinks()

                // Tables for examples/test cases
                .allowElements("table", "thead", "tbody", "tr", "th", "td")

                .toFactory();
    }

    /**
     * Sanitize HTML content for problem descriptions
     * Removes dangerous scripts, event handlers, and malicious tags
     */
    public String sanitizeProblemDescription(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return policy.sanitize(html);
    }

    /**
     * Sanitize plain text content
     * Escapes HTML entities to prevent injection
     */
    public String sanitizePlainText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Escape HTML entities
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * Sanitize markdown-like content that may contain HTML
     */
    public String sanitizeMarkdownContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // Allow markdown-style code blocks but sanitize any embedded HTML
        return policy.sanitize(content);
    }
}
