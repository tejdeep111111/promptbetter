package com.promptbetter.service;


import com.promptbetter.model.Challenge;
import com.promptbetter.repository.ChallengeRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChallengeService {


    private final ChallengeRepository challengeRepository;

    public ChallengeService(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    public List<String> getDomains() {
        return challengeRepository.findDistinctDomains();
    }

    public List<Challenge> getChallengeByDomainAndLevel(String domain, int level) {
        return Collections.singletonList(challengeRepository.findByDomainAndLevel(domain, level)
                .orElseThrow(() -> new RuntimeException("No challenges found for domain: " + domain + " and level: " + level)));
    }

    public List<Challenge> getChallengesByDomain(String domain) {
        return challengeRepository.findByDomain(domain);
    }
}
