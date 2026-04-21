package com.promptbetter.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EmailValidator.
 * Format-only tests use emails with known-valid domains (gmail.com)
 * so the MX check always passes and we're really testing the regex.
 */
class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    // ── Valid formats (all use gmail.com so MX passes) ──
    @Test void valid_simple()        { assertNull(validator.validate("alice@gmail.com")); }
    @Test void valid_dots()          { assertNull(validator.validate("first.last@gmail.com")); }
    @Test void valid_plus()          { assertNull(validator.validate("user+tag@gmail.com")); }
    @Test void valid_hyphenDomain()  { assertNull(validator.validate("a@outlook.com")); }

    // ── Invalid formats (rejected before MX check) ──
    @Test void invalid_null()        { assertEquals("Email is required", validator.validate(null)); }
    @Test void invalid_blank()       { assertEquals("Email is required", validator.validate("   ")); }
    @Test void invalid_noAt()        { assertEquals("Invalid email format", validator.validate("alicegmail.com")); }
    @Test void invalid_noDomain()    { assertEquals("Invalid email format", validator.validate("alice@")); }
    @Test void invalid_noTld()       { assertEquals("Invalid email format", validator.validate("alice@domain")); }
    @Test void invalid_spaces()      { assertEquals("Invalid email format", validator.validate("alice @gmail.com")); }
    @Test void invalid_doubleAt()    { assertEquals("Invalid email format", validator.validate("a@@b.com")); }
    @Test void invalid_shortTld()    { assertEquals("Invalid email format", validator.validate("a@b.c")); }

    // ── MX lookup (unit-level) ──
    @Test
    void hasMxRecord_realDomain() {
        // gmail.com should always have MX records
        assertTrue(validator.hasMxRecord("gmail.com"));
    }
}


