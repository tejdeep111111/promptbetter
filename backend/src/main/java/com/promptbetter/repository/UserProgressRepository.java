package com.promptbetter.repository;

import com.promptbetter.model.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByUserIdAndDomain(Long userId, String domain);
    List<UserProgress> findByUserId(Long userId);
}
