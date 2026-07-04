package com.jobhunter.controller;

import com.jobhunter.model.ApplicationStatus;
import com.jobhunter.model.JobApplication;
import com.jobhunter.service.ApplicationService;
import com.jobhunter.service.DailyRunService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class JobController {

    private final ApplicationService applicationService;
    private final DailyRunService dailyRunService;

    public JobController(ApplicationService applicationService, DailyRunService dailyRunService) {
        this.applicationService = applicationService;
        this.dailyRunService = dailyRunService;
    }

    @GetMapping("/jobs")
    public List<JobApplication> allJobs() {
        return applicationService.all();
    }

    @GetMapping("/jobs/applied")
    public List<JobApplication> appliedJobs() {
        return applicationService.byStatus(ApplicationStatus.APPLIED);
    }

    @PostMapping("/jobs/{id}/apply")
    public ResponseEntity<?> apply(@PathVariable Long id) {
        return applicationService.apply(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Manually trigger a full search/apply/email run (same as the daily job). */
    @PostMapping("/run")
    public ResponseEntity<?> runNow() {
        return dailyRunService.run()
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(Map.of(
                        "matched", r.matchedCount(),
                        "applied", r.appliedCount(),
                        "jobs", r.matched())))
                .orElse(ResponseEntity.badRequest().body(Map.of(
                        "error", "No active resume. Upload a resume first.")));
    }
}
