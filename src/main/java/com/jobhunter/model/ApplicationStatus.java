package com.jobhunter.model;

public enum ApplicationStatus {
    /** Matched by the search but not yet applied to. */
    MATCHED,
    /** An apply action was performed/recorded for this job. */
    APPLIED,
    /** Applying failed (e.g. site required manual steps / login). */
    FAILED,
    /** User chose to skip this job. */
    SKIPPED
}
