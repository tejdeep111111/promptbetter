package com.promptbetter.service;

import com.promptbetter.model.Challenge;
import com.promptbetter.repository.ChallengeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChallengeServiceTest {
    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private ChallengeService challengeService;

    @Test
    void getDomains_ShouldReturnListOfDomains() {
        // Given
        List<String> expectedDomains = Arrays.asList("Code", "Test", "Deploy");
        when(challengeRepository.findDistinctDomains()).thenReturn(expectedDomains);

        // When
        List<String> actualDomains = challengeService.getDomains();

        // Then
        assertEquals(expectedDomains, actualDomains);
    }

    @Test
    void getChallengeByDomainAndLevel_ShouldReturnChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setDomain("Code");
        challenge.setLevel(1);
        challenge.setTitle("First Challenge");

        when(challengeRepository.findByDomainAndLevel("Code", 1)).thenReturn(Optional.of(challenge));

        List<Challenge> result = challengeService.getChallengeByDomainAndLevel("Code", 1);

        assertEquals(1, result.size());
        assertEquals("First Challenge", result.get(0).getTitle());
    }

    @Test
    void getChallengByDomainAndLevel_notFound_shouldThrow() {
        when(challengeRepository.findByDomainAndLevel("Code", 2)).thenReturn(Optional.empty());

        try {
            challengeService.getChallengeByDomainAndLevel("Code", 2);
        } catch (RuntimeException e) {
            assertEquals("No challenges found for domain: Code and level: 2", e.getMessage());
        }
    }

    @Test
    void getChallengesByDomain_ShouldReturnListOfChallenges() {
        Challenge challenge1 = new Challenge();
        challenge1.setId(1L);
        challenge1.setDomain("Code");
        challenge1.setLevel(1);
        challenge1.setTitle("First Challenge");

        Challenge challenge2 = new Challenge();
        challenge2.setId(2L);
        challenge2.setDomain("Code");
        challenge2.setLevel(2);
        challenge2.setTitle("Second Challenge");

        List<Challenge> expectedChallenges = Arrays.asList(challenge1, challenge2);

        when(challengeRepository.findByDomain("Code")).thenReturn(expectedChallenges);

        List<Challenge> actualChallenges = challengeService.getChallengesByDomain("Code");

        assertEquals(expectedChallenges, actualChallenges);
    }
}
