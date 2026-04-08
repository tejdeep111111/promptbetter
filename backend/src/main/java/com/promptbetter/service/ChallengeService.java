package com.promptbetter.service;


import com.promptbetter.model.Challenge;
import com.promptbetter.repository.ChallengeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChallengeService {


    private final ChallengeRepository challengeRepository;

    public ChallengeService(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    @Cacheable(value = "domains")
    public List<String> getDomains() {
        return challengeRepository.findDistinctDomains();
    }


    @Cacheable(value = "challenges", key = "#domain + '-' + #level")
    public List<Challenge> getChallengeByDomainAndLevel(String domain, int level) {
        return Collections.singletonList(challengeRepository.findByDomainAndLevel(domain, level)
                .orElseThrow(() -> new RuntimeException("No challenges found for domain: " + domain + " and level: " + level)));
    }

    @Cacheable(value = "challengesByDomain", key = "#domain")
    public List<Challenge> getChallengesByDomain(String domain) {
        return challengeRepository.findByDomain(domain);
    }
}
