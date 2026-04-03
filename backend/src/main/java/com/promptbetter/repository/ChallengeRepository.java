package com.promptbetter.repository;

import com.promptbetter.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByDomain(String domain);

    Optional<Challenge> findByDomainAndLevel(String domain, int level);

    @Query("SELECT DISTINCT c.domain FROM Challenge c")
    List<String> findDistinctDomains();
}