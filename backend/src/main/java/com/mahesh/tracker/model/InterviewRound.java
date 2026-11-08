package com.mahesh.tracker.model;

public record InterviewRound(
        Long id,
        Long companyId,
        String roundName,
        String roundType,
        String interviewDate,
        String status,
        String feedback
) {
}
