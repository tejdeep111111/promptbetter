package com.promptbetter.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "glossary_terms")
public class GlossaryTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Column(name = "term_key", nullable = false, unique = true)
    private String termKey;

    @Column(name = "term_display", nullable = false)
    private String termDisplay;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    private String category;
}
