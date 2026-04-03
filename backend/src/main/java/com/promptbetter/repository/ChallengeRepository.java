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
    @Cacheable(cacheNames = "challengesByDomain", key = "#domain")
    List<Challenge> findByDomain(String domain);

    @Cacheable(cacheNames = "challengeByDomainAndLevel", key = "#domain + ':' + #level")
    Optional<Challenge> findByDomainAndLevel(String domain, int level);

    @Query("SELECT DISTINCT c.domain FROM Challenge c")
    @Cacheable(cacheNames = "domains", key = "'distinct'")
    List<String> findDistinctDomains();

    @CacheEvict(cacheNames = {"domains", "challengesByDomain", "challengeByDomainAndLevel"}, allEntries = true)
    <S extends Challenge> S save(S entity);

    @CacheEvict(cacheNames = {"domains", "challengesByDomain", "challengeByDomainAndLevel"}, allEntries = true)
    void deleteById(Long id);

    @CacheEvict(cacheNames = {"domains", "challengesByDomain", "challengeByDomainAndLevel"}, allEntries = true)
    void delete(Challenge entity);

    @CacheEvict(cacheNames = {"domains", "challengesByDomain", "challengeByDomainAndLevel"}, allEntries = true)
    void deleteAll();
}