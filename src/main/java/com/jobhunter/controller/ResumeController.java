package com.jobhunter.controller;

import com.jobhunter.model.Resume;
import com.jobhunter.service.ResumeService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "location", required = false) String location) {
        try {
            Resume resume = resumeService.upload(file, location);
            return ResponseEntity.ok(toView(resume));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "Could not read resume: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> active() {
        return resumeService.getActiveResume()
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(toView(r)))
                .orElse(ResponseEntity.noContent().build());
    }

    private Map<String, Object> toView(Resume r) {
        List<String> keywords = (r.getKeywords() == null || r.getKeywords().isBlank())
                ? List.of()
                : List.of(r.getKeywords().split(",\\s*"));
        return Map.of(
                "id", r.getId(),
                "fileName", r.getFileName(),
                "keywords", keywords,
                "searchQuery", r.getSearchQuery() == null ? "" : r.getSearchQuery(),
                "preferredLocation", r.getPreferredLocation() == null ? "" : r.getPreferredLocation(),
                "uploadedAt", r.getUploadedAt().toString());
    }
}
