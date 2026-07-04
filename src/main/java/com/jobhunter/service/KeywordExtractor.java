package com.jobhunter.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Derives search keywords from resume text. Uses a curated skill dictionary
 * (high precision) plus a frequency fallback for words not in the dictionary.
 */
@Component
public class KeywordExtractor {

    private static final Pattern TOKEN = Pattern.compile("[^a-zA-Z0-9+#.]+");

    /** Common tech / role keywords worth matching against job titles. */
    private static final Set<String> SKILLS = Set.of(
            "java", "spring", "springboot", "python", "javascript", "typescript",
            "react", "angular", "vue", "node", "nodejs", "html", "css", "sql",
            "mysql", "postgresql", "mongodb", "aws", "azure", "gcp", "docker",
            "kubernetes", "kafka", "microservices", "rest", "graphql", "git",
            "jenkins", "ci/cd", "devops", "linux", "hibernate", "jpa", "maven",
            "gradle", "kotlin", "golang", "go", "rust", "c++", "c#", ".net",
            "django", "flask", "express", "redis", "elasticsearch", "spark",
            "hadoop", "tableau", "powerbi", "excel", "selenium", "junit",
            "testing", "qa", "android", "ios", "swift", "flutter", "php",
            "ruby", "rails", "scala", "terraform", "ansible", "machine learning",
            "data science", "nlp", "tensorflow", "pytorch", "pandas", "numpy");

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "have", "has",
            "was", "are", "were", "will", "would", "your", "you", "our", "their",
            "work", "worked", "using", "used", "experience", "years", "year",
            "team", "project", "projects", "company", "role", "responsible",
            "developed", "developer", "engineer", "engineering", "including",
            "various", "such", "also", "well", "new", "all");

    public List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase();
        List<String> ordered = new ArrayList<>();

        // 1) Multi-word and dictionary skills, preserving discovery order.
        for (String skill : SKILLS) {
            if (lower.contains(skill) && !ordered.contains(skill)) {
                ordered.add(skill);
            }
        }

        // 2) Frequency-based fallback for other meaningful tokens.
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String raw : TOKEN.split(lower)) {
            String token = raw.trim();
            if (token.length() < 3 || STOPWORDS.contains(token) || SKILLS.contains(token)) {
                continue;
            }
            if (!token.matches(".*[a-z].*")) {
                continue;
            }
            freq.merge(token, 1, Integer::sum);
        }
        freq.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .map(Map.Entry::getKey)
                .forEach(t -> {
                    if (!ordered.contains(t)) {
                        ordered.add(t);
                    }
                });

        return ordered.stream().limit(25).collect(Collectors.toList());
    }

    public static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
