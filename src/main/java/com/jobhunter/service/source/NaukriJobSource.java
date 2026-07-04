package com.jobhunter.service.source;

import com.jobhunter.model.JobPosting;
import com.jobhunter.service.JobSearchRequest;
import com.jobhunter.service.JobSource;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Real job-board source for naukri.com.
 *
 * <p><b>Not yet wired to the live site.</b> Naukri has no public search API and
 * scraping/auto-applying requires a logged-in session and violates their terms
 * of service in many cases (bot detection, captchas). This class is the
 * designated place to add that integration:
 *
 * <ol>
 *   <li>Enable it with {@code jobhunter.source.naukri.enabled=true}.</li>
 *   <li>Add credentials via environment variables (never commit them).</li>
 *   <li>Implement {@link #search} using an HTTP client or a headless browser
 *       (e.g. Playwright/Selenium) driving a logged-in session.</li>
 * </ol>
 *
 * Until then it is disabled and returns no results, so the pipeline falls back
 * to the other configured sources.
 */
@Component
public class NaukriJobSource implements JobSource {

    private static final Logger log = LoggerFactory.getLogger(NaukriJobSource.class);

    private final boolean enabled;

    public NaukriJobSource(@Value("${jobhunter.source.naukri.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "naukri.com";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<JobPosting> search(JobSearchRequest request) {
        if (!enabled) {
            return List.of();
        }
        // TODO: implement live search against naukri.com using a logged-in
        // session. Map each result into a JobPosting(title, company, location,
        // url, description, name()).
        log.warn("NaukriJobSource is enabled but live search is not implemented yet.");
        return List.of();
    }
}
