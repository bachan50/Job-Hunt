package com.jobhunter.scheduler;

import com.jobhunter.service.DailyRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers a full job-search run once a day. The cron expression is configured
 * by {@code jobhunter.schedule.cron} (default 08:00 daily) and the whole
 * scheduler can be turned off with {@code jobhunter.schedule.enabled=false}.
 */
@Component
public class DailyJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyJobScheduler.class);

    private final DailyRunService dailyRunService;
    private final boolean enabled;

    public DailyJobScheduler(DailyRunService dailyRunService,
                             @Value("${jobhunter.schedule.enabled:true}") boolean enabled) {
        this.dailyRunService = dailyRunService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${jobhunter.schedule.cron:0 0 8 * * *}")
    public void runDaily() {
        if (!enabled) {
            return;
        }
        log.info("Scheduled daily job search starting...");
        dailyRunService.run().ifPresentOrElse(
                r -> log.info("Scheduled run: {} matched, {} applied.", r.matchedCount(), r.appliedCount()),
                () -> log.info("Scheduled run: nothing to do (no active resume)."));
    }
}
