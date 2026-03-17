package com.promptbetter.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_progress")
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String domain;

    @Column(name = "current_level")
    private int currentLevel = 1;

    private int xp = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}