package com.jobhunter.service.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.JobPosting;
import com.jobhunter.service.JobSearchRequest;
import com.jobhunter.service.JobSource;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Live job source backed by the Adzuna jobs API (https://developer.adzuna.com).
 *
 * <p>Adzuna offers a free API key and permits programmatic access, so it gives
 * real, current listings for the daily run without the ToS / bot-detection
 * problems of scraping sites like naukri.com. Enabled automatically once an
 * app id and key are configured.
 */
@Component
public class AdzunaJobSource implements JobSource {

    private static final Logger log = LoggerFactory.getLogger(AdzunaJobSource.class);

    private final String appId;
    private final String appKey;
    private final String country;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AdzunaJobSource(
            @Value("${jobhunter.source.adzuna.app-id:}") String appId,
            @Value("${jobhunter.source.adzuna.app-key:}") String appKey,
            @Value("${jobhunter.source.adzuna.country:in}") String country) {
        this.appId = appId;
        this.appKey = appKey;
        this.country = country == null || country.isBlank() ? "in" : country.trim();
    }

    @Override
    public String name() {
        return "adzuna";
    }

    @Override
    public boolean isEnabled() {
        return appId != null && !appId.isBlank() && appKey != null && !appKey.isBlank();
    }

    @Override
    public List<JobPosting> search(JobSearchRequest request) {
        if (!isEnabled()) {
            return List.of();
        }
        String what = request.query() == null || request.query().isBlank()
                ? String.join(" ", request.keywords())
                : request.query();
        int limit = Math.max(1, request.limit());

        StringBuilder url = new StringBuilder()
                .append("https://api.adzuna.com/v1/api/jobs/").append(country).append("/search/1")
                .append("?app_id=").append(enc(appId))
                .append("&app_key=").append(enc(appKey))
                .append("&results_per_page=").append(limit)
                .append("&what=").append(enc(what))
                .append("&content-type=application/json");
        if (request.location() != null && !request.location().isBlank()) {
            url.append("&where=").append(enc(request.location()));
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Adzuna API returned HTTP {}: {}", response.statusCode(),
                        truncate(response.body(), 300));
                return List.of();
            }
            return parse(response.body(), limit);
        } catch (Exception e) {
            log.error("Adzuna search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<JobPosting> parse(String body, int limit) throws Exception {
        JsonNode root = mapper.readTree(body);
        JsonNode results = root.path("results");
        List<JobPosting> postings = new ArrayList<>();
        for (JsonNode node : results) {
            if (postings.size() >= limit) {
                break;
            }
            String title = node.path("title").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            String company = node.path("company").path("display_name").asText("").trim();
            String location = node.path("location").path("display_name").asText("").trim();
            String jobUrl = node.path("redirect_url").asText("").trim();
            String description = node.path("description").asText("").trim();
            postings.add(new JobPosting(title, company, location, jobUrl, description, name()));
        }
        log.info("Adzuna returned {} postings.", postings.size());
        return postings;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
