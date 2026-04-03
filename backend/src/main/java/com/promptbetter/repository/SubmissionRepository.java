package com.promptbetter.repository;

import com.promptbetter.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdAndChallengeId(Long userId, Long challengeId);

    Optional<Submission> findTopByUserIdAndChallengeIdOrderByCreatedAtDesc(Long userId, Long challengeId);
}