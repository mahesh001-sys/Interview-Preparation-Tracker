package com.mahesh.tracker.model;

public record Company(
        Long id,
        String companyName,
        String jobRole,
        String jobLocation,
        String jobType,
        String applicationStatus,
        String applicationDate,
        String jobDescription,
        String websiteUrl
) {
}
