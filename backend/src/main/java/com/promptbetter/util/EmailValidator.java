package com.promptbetter.util;

import org.springframework.stereotype.Component;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.regex.Pattern;

/**
 * Two-step email validation:
 * 1. Regex format check (RFC-5322 simplified)
 * 2. DNS MX record lookup — confirms the domain has a real mail server
 */
@Component
public class EmailValidator {

    // Covers 99.9% of real-world emails; intentionally not the full RFC regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Validates format + domain MX record.
     * @return null if valid, or a user-friendly error message if invalid.
     */
    public String validate(String email) {
        if (email == null || email.isBlank()) {
            return "Email is required";
        }

        String trimmed = email.trim().toLowerCase();

        // Step 1: format
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return "Invalid email format";
        }

        // Step 2: DNS MX lookup on the domain part
        String domain = trimmed.substring(trimmed.indexOf('@') + 1);
        if (!hasMxRecord(domain)) {
            return "Email domain \"" + domain + "\" does not appear to accept mail";
        }

        return null; // valid
    }

    /**
     * Checks whether the given domain has at least one MX (mail-exchange) DNS record.
     * Falls back gracefully: if the DNS lookup itself fails (e.g. network issue),
     * we let the email through rather than blocking the user.
     */
    boolean hasMxRecord(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            // 5-second timeout so we don't hang on slow DNS
            env.put("com.sun.jndi.dns.timeout.initial", "5000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");

            InitialDirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            Attribute mx = attrs.get("MX");
            ctx.close();
            return mx != null && mx.size() > 0;
        } catch (Exception e) {
            // DNS failure — don't block the user, let the email through
            return true;
        }
    }
}

