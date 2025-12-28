package com.coderace.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security Headers Filter
 * Adds HTTP security headers to all responses for defense-in-depth
 */
@Component
public class SecurityHeadersFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                        throws IOException, ServletException {

                HttpServletResponse httpResponse = (HttpServletResponse) response;

                // Prevent MIME type sniffing
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");

                // Prevent clickjacking attacks
                httpResponse.setHeader("X-Frame-Options", "DENY");

                // Enable XSS protection in older browsers
                httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

                // Enforce HTTPS in production (will be enabled via environment variable)
                // Only add if we're in a secure context
                String protocol = request.getScheme();
                if ("https".equals(protocol)) {
                        httpResponse.setHeader("Strict-Transport-Security",
                                        "max-age=31536000; includeSubDomains; preload");
                }

                // Content Security Policy - Strict XSS protection
                // Removed unsafe-inline and unsafe-eval from script-src
                httpResponse.setHeader("Content-Security-Policy",
                                "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                                "font-src 'self' https://fonts.gstatic.com; " +
                                                "img-src 'self' data: https:; " +
                                                "connect-src 'self' ws: wss:; " + // WebSocket support
                                                "frame-ancestors 'none'");

                // Referrer policy - control information sent in Referer header
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

                // Permissions policy - disable unused browser features
                httpResponse.setHeader("Permissions-Policy",
                                "geolocation=(), microphone=(), camera=()");

                chain.doFilter(request, response);
        }
}
