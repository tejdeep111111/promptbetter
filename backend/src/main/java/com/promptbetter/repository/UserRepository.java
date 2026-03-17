package com.promptbetter.repository;

import com.promptbetter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// This interface extends JpaRepository, providing CRUD operations for User entities.
@Repository
public interface UserRepository extends JpaRepository<User, Long> { // The primary key type is Long.
    Optional<User> findByEmail(String email); // Method to find a user by their email address.
    boolean existsByEmail(String email);
}
