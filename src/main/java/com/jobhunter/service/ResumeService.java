package com.jobhunter.service;

import com.jobhunter.model.Resume;
import com.jobhunter.repository.ResumeRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParser resumeParser;
    private final KeywordExtractor keywordExtractor;

    public ResumeService(ResumeRepository resumeRepository,
                         ResumeParser resumeParser,
                         KeywordExtractor keywordExtractor) {
        this.resumeRepository = resumeRepository;
        this.resumeParser = resumeParser;
        this.keywordExtractor = keywordExtractor;
    }

    @Transactional
    public Resume upload(MultipartFile file, String location) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }
        String text = resumeParser.extractText(file);
        List<String> keywords = keywordExtractor.extract(text);

        // Deactivate any previously active resume; the newest upload drives search.
        resumeRepository.findFirstByActiveTrueOrderByUploadedAtDesc().ifPresent(prev -> {
            prev.setActive(false);
            resumeRepository.save(prev);
        });

        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setContentType(file.getContentType());
        resume.setExtractedText(text);
        resume.setKeywords(String.join(", ", keywords));
        resume.setSearchQuery(buildQuery(keywords));
        resume.setPreferredLocation(location);
        resume.setActive(true);
        return resumeRepository.save(resume);
    }

    private String buildQuery(List<String> keywords) {
        return keywords.stream().limit(5).reduce((a, b) -> a + " " + b).orElse("software developer");
    }

    public Optional<Resume> getActiveResume() {
        return resumeRepository.findFirstByActiveTrueOrderByUploadedAtDesc();
    }

    public List<Resume> listResumes() {
        return resumeRepository.findAll();
    }
}
