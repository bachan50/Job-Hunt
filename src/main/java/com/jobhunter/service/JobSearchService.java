package com.jobhunter.service;

import com.jobhunter.model.ApplicationStatus;
import com.jobhunter.model.JobApplication;
import com.jobhunter.model.JobPosting;
import com.jobhunter.model.Resume;
import com.jobhunter.repository.JobApplicationRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a search run: queries every enabled {@link JobSource}, scores
 * each posting against the resume keywords, de-duplicates against previous
 * runs, and persists new matches as {@link JobApplication} records.
 */
@Service
public class JobSearchService {

    private static final Logger log = LoggerFactory.getLogger(JobSearchService.class);

    private final List<JobSource> sources;
    private final JobApplicationRepository applicationRepository;
    private final int maxResults;
    private final String defaultLocation;

    public JobSearchService(List<JobSource> sources,
                            JobApplicationRepository applicationRepository,
                            @Value("${jobhunter.search.max-results:20}") int maxResults,
                            @Value("${jobhunter.search.default-location:India}") String defaultLocation) {
        this.sources = sources;
        this.applicationRepository = applicationRepository;
        this.maxResults = maxResults;
        this.defaultLocation = defaultLocation;
    }

    @Transactional
    public List<JobApplication> searchForResume(Resume resume) {
        List<String> keywords = KeywordExtractor.split(resume.getKeywords());
        String location = resume.getPreferredLocation() == null || resume.getPreferredLocation().isBlank()
                ? defaultLocation : resume.getPreferredLocation();
        JobSearchRequest request = new JobSearchRequest(
                resume.getSearchQuery(), keywords, location, maxResults);

        List<JobApplication> newlyMatched = new ArrayList<>();
        for (JobSource source : sources) {
            if (!source.isEnabled()) {
                continue;
            }
            try {
                List<JobPosting> postings = source.search(request);
                for (JobPosting posting : postings) {
                    String dedupeKey = dedupeKey(posting);
                    if (applicationRepository.existsByDedupeKey(dedupeKey)) {
                        continue;
                    }
                    JobApplication application = toApplication(resume, posting, keywords, dedupeKey);
                    newlyMatched.add(applicationRepository.save(application));
                }
            } catch (Exception e) {
                log.error("Job source '{}' failed: {}", source.name(), e.getMessage(), e);
            }
        }
        log.info("Search run for resume {} found {} new matches from {} source(s).",
                resume.getId(), newlyMatched.size(), sources.size());
        return newlyMatched;
    }

    private JobApplication toApplication(Resume resume, JobPosting posting,
                                         List<String> keywords, String dedupeKey) {
        JobApplication application = new JobApplication();
        application.setResumeId(resume.getId());
        application.setTitle(posting.title());
        application.setCompany(posting.company());
        application.setLocation(posting.location());
        application.setUrl(posting.url());
        application.setDescription(truncate(posting.description(), 1990));
        application.setSource(posting.source());
        application.setMatchScore(score(posting, keywords));
        application.setStatus(ApplicationStatus.MATCHED);
        application.setDedupeKey(dedupeKey);
        application.setDiscoveredAt(Instant.now());
        return application;
    }

    /** Fraction of resume keywords that appear in the posting, as 0-100. */
    private int score(JobPosting posting, List<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        String haystack = (posting.title() + " " + posting.description()).toLowerCase(Locale.ROOT);
        long hits = keywords.stream()
                .map(k -> k.toLowerCase(Locale.ROOT))
                .filter(haystack::contains)
                .count();
        return (int) Math.round(100.0 * hits / keywords.size());
    }

    private String dedupeKey(JobPosting posting) {
        String basis = posting.url() != null && !posting.url().isBlank()
                ? posting.url()
                : posting.source() + "|" + posting.title() + "|" + posting.company();
        return UUID.nameUUIDFromBytes(basis.toLowerCase(Locale.ROOT)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
