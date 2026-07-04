package com.jobhunter.repository;

import com.jobhunter.model.ApplicationStatus;
import com.jobhunter.model.JobApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    List<JobApplication> findByStatusOrderByDiscoveredAtDesc(ApplicationStatus status);

    List<JobApplication> findAllByOrderByDiscoveredAtDesc();
}
