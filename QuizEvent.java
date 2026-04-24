package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a single quiz event (score entry) from the API.
 * Used as the unit of deduplication via (roundId + participant) composite key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizEvent {

    private String roundId;
    private String participant;
    private int score;

    /**
     * Composite key used for deduplication.
     * Two events with the same roundId and participant are considered duplicates.
     */
    public String getDeduplicationKey() {
        return roundId + "::" + participant;
    }
}
