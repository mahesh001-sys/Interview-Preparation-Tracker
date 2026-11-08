package com.mahesh.tracker.model;

public record Question(
        Long id,
        String category,
        String questionText,
        String answerNotes,
        String difficulty,
        Boolean isCompleted
) {
}
