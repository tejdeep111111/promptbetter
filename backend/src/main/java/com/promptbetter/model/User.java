package com.promptbetter.model;

import lombok.Data;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods
@Entity // JPA annotation to specify that this class is an entity and will be mapped to a database table
@Table(name = "users") // JPA annotation to specify the name of the database table
public class User implements UserDetails {
    @Id // JPA annotation to specify the primary key of the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA annotation to specify that the primary key will be generated automatically by the database
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true) // JPA annotation to specify that the email column must be unique in the database
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at") // JPA annotation to specify the name of the column in the database
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Spring Security uses getUsername() to identify the principal.
     * We use email as the unique identifier.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Every registered user gets ROLE_USER by default.
     * Add more roles to the User entity when you need ROLE_ADMIN, etc.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // Return true for all flags — override these when you add
    // email verification, account locking, or credential expiry.
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
