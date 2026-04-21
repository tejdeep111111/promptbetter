package com.promptbetter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the GlossaryTerm model.
 */
class GlossaryTermTest {

    @Test
    void gettersAndSetters_shouldWork() {
        GlossaryTerm term = new GlossaryTerm();
        term.setId(1L);
        term.setTermKey("few_shot");
        term.setTermDisplay("Few-Shot Prompting");
        term.setDefinition("Providing examples in the prompt");
        term.setCategory("technique");

        assertEquals(1L, term.getId());
        assertEquals("few_shot", term.getTermKey());
        assertEquals("Few-Shot Prompting", term.getTermDisplay());
        assertEquals("Providing examples in the prompt", term.getDefinition());
        assertEquals("technique", term.getCategory());
    }

    @Test
    void category_canBeNull() {
        // The category column is nullable in the entity
        GlossaryTerm term = new GlossaryTerm();
        assertNull(term.getCategory(), "Category should be null by default");
    }
}
