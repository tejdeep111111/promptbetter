package com.promptbetter.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "challenges")
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domain;

    private int level;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String scenario;

    @Column(name = "ideal_prompt", columnDefinition = "TEXT")
    private String idealPrompt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}
