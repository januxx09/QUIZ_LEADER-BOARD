package com.bajaj.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload for POST /quiz/submit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequest {

    private String regNo;
    private List<LeaderboardEntry> leaderboard;
}
