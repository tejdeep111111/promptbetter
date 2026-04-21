package com.promptbetter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class UserProgressTest {
    @Test
    void defaults_shouldBeCorrect() {
        UserProgress progress = new UserProgress();

        assertEquals(1, progress.getCurrentLevel(), "Default level should be 1");
        assertEquals(0, progress.getXp(), "Default XP should be 0");
        assertNotNull(progress.getUpdatedAt(), "updatedAt should be auto-initialized");
    }

    @Test
    void gettersAndSetters_shouldWork() {
        UserProgress progress = new UserProgress();
        progress.setId(1L);
        progress.setUserId(2L);
        progress.setCurrentLevel(5);
        progress.setXp(1500);

        assertEquals(1L, progress.getId());
        assertEquals(2L, progress.getUserId());
        assertEquals(5, progress.getCurrentLevel());
        assertEquals(1500, progress.getXp());
    }
}
