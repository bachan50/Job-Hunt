package com.jobhunter.service.source;

import com.jobhunter.model.JobPosting;
import com.jobhunter.service.JobSearchRequest;
import com.jobhunter.service.JobSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * A working, dependency-free job source that synthesises realistic postings
 * from the resume keywords. It lets the whole pipeline (search -> match ->
 * report -> email) run locally without any external credentials.
 *
 * <p>Replace or supplement this with a real {@link JobSource} (see
 * {@link NaukriJobSource}) when you are ready to hit live job boards.
 */
@Component
public class MockJobSource implements JobSource {

    private static final String[] COMPANIES = {
            "Infosys", "TCS", "Wipro", "Accenture", "Tech Mahindra", "Cognizant",
            "Zoho", "Freshworks", "Razorpay", "Swiggy", "Flipkart", "PhonePe"
    };

    private static final String[] SENIORITY = {"Junior", "", "Senior", "Lead"};

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public List<JobPosting> search(JobSearchRequest request) {
        List<JobPosting> results = new ArrayList<>();
        List<String> keywords = request.keywords();
        if (keywords.isEmpty()) {
            keywords = List.of("Software");
        }
        String location = request.location() == null || request.location().isBlank()
                ? "India" : request.location();

        int limit = Math.max(1, request.limit());
        int i = 0;
        while (results.size() < limit) {
            String keyword = keywords.get(i % keywords.size());
            String seniority = SENIORITY[i % SENIORITY.length];
            String company = COMPANIES[i % COMPANIES.length];
            String title = (seniority.isEmpty() ? "" : seniority + " ")
                    + capitalize(keyword) + " Developer";
            String slug = (title + "-" + company).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            String url = "https://www.example-jobs.com/job/" + slug;
            String description = "We are hiring a " + title + " at " + company + ". "
                    + "Required skills: " + String.join(", ", keywords) + ". Location: " + location + ".";
            results.add(new JobPosting(title, company, location, url, description, name()));
            i++;
        }
        return results;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
