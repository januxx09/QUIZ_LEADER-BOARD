package com.bajaj.quiz;

import com.bajaj.quiz.model.LeaderboardEntry;
import com.bajaj.quiz.model.QuizEvent;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the core deduplication and aggregation logic.
 * These tests run without any network calls.
 */
public class QuizLogicTest {

    // ──────────────────────────────────────────────
    // Helper: deduplication logic (mirrors QuizService)
    // ──────────────────────────────────────────────
    private Map<String, QuizEvent> deduplicate(List<QuizEvent> events) {
        Map<String, QuizEvent> map = new LinkedHashMap<>();
        for (QuizEvent e : events) {
            map.putIfAbsent(e.getDeduplicationKey(), e);
        }
        return map;
    }

    private Map<String, Integer> aggregate(Map<String, QuizEvent> deduped) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (QuizEvent e : deduped.values()) {
            scores.merge(e.getParticipant(), e.getScore(), Integer::sum);
        }
        return scores;
    }

    private List<LeaderboardEntry> buildLeaderboard(Map<String, Integer> scoreMap) {
        return scoreMap.entrySet().stream()
                .map(e -> new LeaderboardEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed())
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Test 1: Basic deduplication
    // ──────────────────────────────────────────────
    @Test
    void testDuplicatesAreIgnored() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 10),
                new QuizEvent("R1", "Bob", 20),
                new QuizEvent("R1", "Alice", 10),   // duplicate → should be ignored
                new QuizEvent("R2", "Alice", 30)    // different round → NOT a duplicate
        );

        Map<String, QuizEvent> deduped = deduplicate(events);

        // R1::Alice, R1::Bob, R2::Alice → 3 unique events
        assertEquals(3, deduped.size());
    }

    // ──────────────────────────────────────────────
    // Test 2: Aggregation correctness
    // ──────────────────────────────────────────────
    @Test
    void testScoreAggregation() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 10),
                new QuizEvent("R2", "Alice", 30),
                new QuizEvent("R1", "Bob", 20)
        );

        Map<String, QuizEvent> deduped = deduplicate(events);
        Map<String, Integer> scores = aggregate(deduped);

        assertEquals(40, scores.get("Alice"));  // 10 + 30
        assertEquals(20, scores.get("Bob"));    // 20
    }

    // ──────────────────────────────────────────────
    // Test 3: Duplicates do NOT inflate scores
    // ──────────────────────────────────────────────
    @Test
    void testDuplicatesDoNotInflateScore() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 10),
                new QuizEvent("R1", "Alice", 10),  // duplicate
                new QuizEvent("R1", "Alice", 10)   // duplicate again
        );

        Map<String, QuizEvent> deduped = deduplicate(events);
        Map<String, Integer> scores = aggregate(deduped);

        // Should be 10, not 30
        assertEquals(10, scores.get("Alice"));
    }

    // ──────────────────────────────────────────────
    // Test 4: Leaderboard is sorted descending
    // ──────────────────────────────────────────────
    @Test
    void testLeaderboardSortOrder() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 10),
                new QuizEvent("R1", "Bob", 50),
                new QuizEvent("R1", "Charlie", 30)
        );

        Map<String, QuizEvent> deduped = deduplicate(events);
        Map<String, Integer> scores = aggregate(deduped);
        List<LeaderboardEntry> leaderboard = buildLeaderboard(scores);

        assertEquals("Bob", leaderboard.get(0).getParticipant());      // 50
        assertEquals("Charlie", leaderboard.get(1).getParticipant());  // 30
        assertEquals("Alice", leaderboard.get(2).getParticipant());    // 10
    }

    // ──────────────────────────────────────────────
    // Test 5: Total score calculation
    // ──────────────────────────────────────────────
    @Test
    void testTotalScoreCalculation() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 100),
                new QuizEvent("R1", "Bob", 120)
        );

        Map<String, QuizEvent> deduped = deduplicate(events);
        Map<String, Integer> scores = aggregate(deduped);
        List<LeaderboardEntry> leaderboard = buildLeaderboard(scores);

        int total = leaderboard.stream().mapToInt(LeaderboardEntry::getTotalScore).sum();
        assertEquals(220, total);
    }

    // ──────────────────────────────────────────────
    // Test 6: Deduplication key format
    // ──────────────────────────────────────────────
    @Test
    void testDeduplicationKey() {
        QuizEvent e = new QuizEvent("R1", "Alice", 10);
        assertEquals("R1::Alice", e.getDeduplicationKey());
    }
}
