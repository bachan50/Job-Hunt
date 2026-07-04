package com.jobhunter.model;

/**
 * A raw job posting returned by a {@link com.jobhunter.service.JobSource}.
 * This is a transport object; matched postings are persisted as
 * {@link JobApplication} entities.
 */
public record JobPosting(
        String title,
        String company,
        String location,
        String url,
        String description,
        String source) {
}
