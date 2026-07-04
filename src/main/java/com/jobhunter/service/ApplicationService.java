package com.jobhunter.service;

import com.jobhunter.model.ApplicationStatus;
import com.jobhunter.model.JobApplication;
import com.jobhunter.repository.JobApplicationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records application actions and serves the applied-jobs report.
 *
 * <p>"Applying" here marks the job as {@link ApplicationStatus#APPLIED} and
 * timestamps it. Truly submitting an application on an external site needs a
 * site-specific integration with the user's credentials — hook that in here
 * (call the relevant {@link JobSource}'s apply routine) when available.
 */
@Service
public class ApplicationService {

    private final JobApplicationRepository repository;

    public ApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public List<JobApplication> all() {
        return repository.findAllByOrderByDiscoveredAtDesc();
    }

    public List<JobApplication> byStatus(ApplicationStatus status) {
        return repository.findByStatusOrderByDiscoveredAtDesc(status);
    }

    @Transactional
    public Optional<JobApplication> apply(Long id) {
        return repository.findById(id).map(app -> {
            app.setStatus(ApplicationStatus.APPLIED);
            app.setAppliedAt(Instant.now());
            return repository.save(app);
        });
    }

    @Transactional
    public Optional<JobApplication> updateStatus(Long id, ApplicationStatus status) {
        return repository.findById(id).map(app -> {
            app.setStatus(status);
            if (status == ApplicationStatus.APPLIED && app.getAppliedAt() == null) {
                app.setAppliedAt(Instant.now());
            }
            return repository.save(app);
        });
    }

    /**
     * Auto-applies to freshly matched jobs above a score threshold. Returns the
     * jobs that were marked applied. This is what the daily scheduler calls.
     */
    @Transactional
    public List<JobApplication> autoApply(List<JobApplication> matched, int minScore) {
        return matched.stream()
                .filter(app -> app.getMatchScore() >= minScore)
                .map(app -> {
                    app.setStatus(ApplicationStatus.APPLIED);
                    app.setAppliedAt(Instant.now());
                    return repository.save(app);
                })
                .toList();
    }
}
