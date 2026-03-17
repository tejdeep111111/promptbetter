package com.promptbetter.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;


@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods
@Entity // JPA annotation to specify that this class is an entity and will be mapped to a database table
@Table(name = "users") // JPA annotation to specify the name of the database table
public class User {
    @Id // JPA annotation to specify the primary key of the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA annotation to specify that the primary key will be generated automatically by the database
    private Long id;

    private String name;

    @Column(unique = true) // JPA annotation to specify that the email column must be unique in the database
    private String email;

    private String password;

    @Column(name = "created_at") // JPA annotation to specify the name of the column in the database
    private LocalDateTime createdAt = LocalDateTime.now();
}
