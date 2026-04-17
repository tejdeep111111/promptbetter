package com.promptbetter.model;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "challenges")
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private int level;

    private String hardness;

    private String topicTaught;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String task;

    @Column(columnDefinition = "TEXT")
    private String aiEvaluationGuide;

    @Column(name = "teaching_point_rule_json", columnDefinition = "TEXT")
    private String teachingPointRuleJson;

    @Column(nullable = true)
    private String constraintAsString;

    @Column(columnDefinition = "TEXT")
    private String keyTakeaway;
}
