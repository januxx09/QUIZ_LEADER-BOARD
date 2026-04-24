package com.bajaj.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single participant's final score in the leaderboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    private String participant;
    private int totalScore;
}
