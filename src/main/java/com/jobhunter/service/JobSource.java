package com.jobhunter.service;

import com.jobhunter.model.JobPosting;
import java.util.List;

/**
 * A pluggable source of job postings (e.g. Naukri, LinkedIn, a job-board API).
 *
 * <p>To add a real integration, implement this interface as a Spring
 * {@code @Component}. It is automatically picked up by
 * {@link JobSearchService}. Keep network/scraping logic and any credentials
 * contained within the implementation.
 */
public interface JobSource {

    /** Human-readable name of the source, e.g. "naukri.com". */
    String name();

    /** Whether this source is currently usable (configured, reachable, etc.). */
    default boolean isEnabled() {
        return true;
    }

    /** Search this source for postings matching the request. */
    List<JobPosting> search(JobSearchRequest request);
}
