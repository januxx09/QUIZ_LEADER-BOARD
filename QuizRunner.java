package com.bajaj.quiz.service;

import com.bajaj.quiz.model.SubmitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Automatically runs the quiz pipeline when the Spring Boot application starts.
 * This triggers the full flow: poll → deduplicate → aggregate → submit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizRunner implements ApplicationRunner {

    private final QuizService quizService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("QuizRunner: Starting quiz pipeline...");

        try {
            SubmitResponse result = quizService.run();

            if (result != null && result.isCorrect()) {
                log.info("SUCCESS! Leaderboard accepted by the validator.");
            } else {
                log.error("FAILED! Leaderboard was rejected. Check your logic.");
            }

        } catch (Exception e) {
            log.error("Unexpected error during quiz pipeline: {}", e.getMessage(), e);
            System.exit(1);
        }

        // Exit cleanly after run
        System.exit(0);
    }
}
