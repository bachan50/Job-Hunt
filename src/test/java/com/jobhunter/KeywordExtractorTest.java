package com.jobhunter;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobhunter.service.KeywordExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordExtractorTest {

    private final KeywordExtractor extractor = new KeywordExtractor();

    @Test
    void extractsKnownSkillsFromResumeText() {
        String resume = "Experienced Java developer skilled in Spring Boot, React and SQL. "
                + "Built microservices deployed on AWS with Docker.";
        List<String> keywords = extractor.extract(resume);
        assertThat(keywords).contains("java", "spring", "react", "sql", "aws", "docker");
    }

    @Test
    void returnsEmptyForBlankText() {
        assertThat(extractor.extract("  ")).isEmpty();
    }

    @Test
    void splitParsesCsv() {
        assertThat(KeywordExtractor.split("java, spring , react"))
                .containsExactly("java", "spring", "react");
    }
}
