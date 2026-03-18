package com.promptbetter.repository;

import com.promptbetter.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByDomain(String domain);
    Optional<Challenge> findByDomainAndLevel(String domain, int level);
    List<String> findDistinctDomains();
}