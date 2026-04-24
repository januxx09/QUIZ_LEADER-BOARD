package com.bajaj.quiz.service;

import com.bajaj.quiz.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service that orchestrates:
 *  1. Polling the quiz API 10 times with a 5-second delay between polls
 *  2. Deduplicating events using (roundId + participant) composite key
 *  3. Aggregating scores per participant
 *  4. Building and submitting the final leaderboard (once)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${quiz.base-url:https://devapigw.vidalhealthtpa.com/srm-quiz-task}")
    private String baseUrl;

    @Value("${quiz.reg-no:YOUR_REG_NO}")
    private String regNo;

    private static final int TOTAL_POLLS = 10;
    private static final long POLL_DELAY_MS = 5000L; // 5 seconds mandatory delay

    /**
     * Full pipeline: poll → deduplicate → aggregate → submit
     */
    public SubmitResponse run() throws InterruptedException {
        log.info("=== Quiz Leaderboard System Starting ===");
        log.info("Registration Number : {}", regNo);

        // Step 1 & 2: Poll 10 times and collect all events
        List<QuizEvent> allEvents = pollAllRounds();
        log.info("Total raw events collected : {}", allEvents.size());

        // Step 3: Deduplicate using (roundId + participant) composite key
        Map<String, QuizEvent> dedupedMap = deduplicateEvents(allEvents);
        log.info("Unique events after deduplication : {}", dedupedMap.size());

        // Step 4: Aggregate scores per participant
        Map<String, Integer> scoreMap = aggregateScores(dedupedMap);

        // Step 5: Build leaderboard sorted by totalScore descending
        List<LeaderboardEntry> leaderboard = buildLeaderboard(scoreMap);

        // Step 6: Compute and log total score
        int totalScore = leaderboard.stream().mapToInt(LeaderboardEntry::getTotalScore).sum();
        log.info("=== Leaderboard ===");
        leaderboard.forEach(e ->
            log.info("  {} -> {}", e.getParticipant(), e.getTotalScore())
        );
        log.info("Total combined score: {}", totalScore);

        // Step 7: Submit leaderboard once
        return submitLeaderboard(leaderboard);
    }

    // ─────────────────────────────────────────────
    // Step 1: Poll API 10 times (poll index 0–9)
    // ─────────────────────────────────────────────
    private List<QuizEvent> pollAllRounds() throws InterruptedException {
        List<QuizEvent> allEvents = new ArrayList<>();

        for (int poll = 0; poll < TOTAL_POLLS; poll++) {
            log.info("Polling index {} of {} ...", poll, TOTAL_POLLS - 1);

            String url = baseUrl + "/quiz/messages?regNo=" + regNo + "&poll=" + poll;

            try {
                ResponseEntity<PollResponse> response =
                        restTemplate.getForEntity(url, PollResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    PollResponse body = response.getBody();
                    List<QuizEvent> events = body.getEvents();

                    if (events != null && !events.isEmpty()) {
                        log.info("  Poll {} -> {} events received (setId={})",
                                poll, events.size(), body.getSetId());
                        allEvents.addAll(events);
                    } else {
                        log.warn("  Poll {} -> No events in response", poll);
                    }
                } else {
                    log.warn("  Poll {} -> Non-2xx response: {}", poll, response.getStatusCode());
                }

            } catch (Exception e) {
                log.error("  Poll {} -> Error: {}", poll, e.getMessage());
            }

            // Mandatory 5-second delay between polls (skip after last poll)
            if (poll < TOTAL_POLLS - 1) {
                log.info("  Waiting {} ms before next poll...", POLL_DELAY_MS);
                Thread.sleep(POLL_DELAY_MS);
            }
        }

        return allEvents;
    }

    // ─────────────────────────────────────────────────────────────────
    // Step 3: Deduplicate events using (roundId + participant) as key
    // If the same key appears more than once, only the first is kept.
    // ─────────────────────────────────────────────────────────────────
    private Map<String, QuizEvent> deduplicateEvents(List<QuizEvent> allEvents) {
        Map<String, QuizEvent> dedupedMap = new LinkedHashMap<>();
        int duplicatesFound = 0;

        for (QuizEvent event : allEvents) {
            String key = event.getDeduplicationKey();

            if (dedupedMap.containsKey(key)) {
                log.debug("  Duplicate found and ignored -> key={}", key);
                duplicatesFound++;
            } else {
                dedupedMap.put(key, event);
            }
        }

        log.info("Duplicates ignored: {}", duplicatesFound);
        return dedupedMap;
    }

    // ─────────────────────────────────────────────────
    // Step 4: Sum scores per participant
    // ─────────────────────────────────────────────────
    private Map<String, Integer> aggregateScores(Map<String, QuizEvent> dedupedMap) {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();

        for (QuizEvent event : dedupedMap.values()) {
            scoreMap.merge(event.getParticipant(), event.getScore(), Integer::sum);
        }

        return scoreMap;
    }

    // ─────────────────────────────────────────────────────────────────
    // Step 5: Build leaderboard sorted by totalScore descending
    // ─────────────────────────────────────────────────────────────────
    private List<LeaderboardEntry> buildLeaderboard(Map<String, Integer> scoreMap) {
        return scoreMap.entrySet().stream()
                .map(e -> new LeaderboardEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────
    // Step 7: POST leaderboard to submit endpoint
    // ─────────────────────────────────────────────────
    private SubmitResponse submitLeaderboard(List<LeaderboardEntry> leaderboard) {
        String url = baseUrl + "/quiz/submit";

        SubmitRequest request = new SubmitRequest(regNo, leaderboard);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SubmitRequest> entity = new HttpEntity<>(request, headers);

        log.info("Submitting leaderboard to {} ...", url);

        try {
            ResponseEntity<SubmitResponse> response =
                    restTemplate.postForEntity(url, entity, SubmitResponse.class);

            SubmitResponse body = response.getBody();
            if (body != null) {
                log.info("=== Submission Result ===");
                log.info("  isCorrect      : {}", body.isCorrect());
                log.info("  isIdempotent   : {}", body.isIdempotent());
                log.info("  submittedTotal : {}", body.getSubmittedTotal());
                log.info("  expectedTotal  : {}", body.getExpectedTotal());
                log.info("  message        : {}", body.getMessage());
            }
            return body;

        } catch (Exception e) {
            log.error("Submission failed: {}", e.getMessage());
            throw new RuntimeException("Failed to submit leaderboard", e);
        }
    }
}
