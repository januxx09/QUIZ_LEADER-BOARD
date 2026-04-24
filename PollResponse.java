package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Maps the JSON response from GET /quiz/messages
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollResponse {

    private String regNo;
    private String setId;
    private int pollIndex;
    private List<QuizEvent> events;
}
