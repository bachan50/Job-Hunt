package com.jobhunter.repository;

import com.jobhunter.model.Resume;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findFirstByActiveTrueOrderByUploadedAtDesc();
}
