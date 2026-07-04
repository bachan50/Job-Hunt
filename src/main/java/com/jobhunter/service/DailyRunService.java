package com.jobhunter.service;

import com.jobhunter.model.JobApplication;
import com.jobhunter.model.Resume;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** A single end-to-end run: search -> auto-apply -> email report. */
@Service
public class DailyRunService {

    private static final Logger log = LoggerFactory.getLogger(DailyRunService.class);

    private final ResumeService resumeService;
    private final JobSearchService jobSearchService;
    private final ApplicationService applicationService;
    private final EmailService emailService;
    private final int autoApplyMinScore;

    public DailyRunService(ResumeService resumeService,
                           JobSearchService jobSearchService,
                           ApplicationService applicationService,
                           EmailService emailService,
                           @Value("${jobhunter.auto-apply.min-score:50}") int autoApplyMinScore) {
        this.resumeService = resumeService;
        this.jobSearchService = jobSearchService;
        this.applicationService = applicationService;
        this.emailService = emailService;
        this.autoApplyMinScore = autoApplyMinScore;
    }

    /**
     * @return a summary of the run, or empty if there is no active resume.
     */
    public Optional<RunResult> run() {
        Optional<Resume> active = resumeService.getActiveResume();
        if (active.isEmpty()) {
            log.info("Daily run skipped: no active resume uploaded yet.");
            return Optional.empty();
        }
        Resume resume = active.get();
        List<JobApplication> matched = jobSearchService.searchForResume(resume);
        List<JobApplication> applied = applicationService.autoApply(matched, autoApplyMinScore);
        emailService.sendDailyReport(matched, applied);
        log.info("Daily run complete: {} matched, {} applied.", matched.size(), applied.size());
        return Optional.of(new RunResult(matched.size(), applied.size(), matched, applied));
    }

    public record RunResult(int matchedCount, int appliedCount,
                            List<JobApplication> matched, List<JobApplication> applied) {
    }
}
