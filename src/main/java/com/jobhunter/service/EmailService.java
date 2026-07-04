package com.jobhunter.service;

import com.jobhunter.model.JobApplication;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Emails the daily report. When mail is not configured
 * ({@code jobhunter.mail.enabled=false}) the report is logged instead, so the
 * app stays fully functional locally without SMTP credentials.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String to;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${jobhunter.mail.enabled:false}") boolean enabled,
                        @Value("${jobhunter.mail.to:}") String to,
                        @Value("${jobhunter.mail.from:job-hunter@localhost}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.to = to;
        this.from = from;
    }

    public void sendDailyReport(List<JobApplication> matched, List<JobApplication> applied) {
        String subject = String.format("Job Hunter: %d new match(es), %d applied",
                matched.size(), applied.size());
        String body = buildBody(matched, applied);

        if (!enabled || to == null || to.isBlank()) {
            log.info("Email disabled or no recipient set; logging report instead.\nSubject: {}\n{}",
                    subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Daily report emailed to {}", to);
        } catch (Exception e) {
            log.error("Failed to send daily report email: {}", e.getMessage(), e);
        }
    }

    private String buildBody(List<JobApplication> matched, List<JobApplication> applied) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your daily job search summary\n");
        sb.append("=============================\n\n");
        sb.append("Applied (").append(applied.size()).append("):\n");
        appendJobs(sb, applied);
        sb.append("\nOther new matches (").append(matched.size() - applied.size()).append("):\n");
        appendJobs(sb, matched.stream().filter(m -> !applied.contains(m)).toList());
        sb.append("\n-- Job Hunter");
        return sb.toString();
    }

    private void appendJobs(StringBuilder sb, List<JobApplication> jobs) {
        if (jobs.isEmpty()) {
            sb.append("  (none)\n");
            return;
        }
        for (JobApplication j : jobs) {
            sb.append(String.format("  - [%d%%] %s @ %s (%s) - %s [%s]%n",
                    j.getMatchScore(), j.getTitle(), j.getCompany(),
                    j.getLocation(), j.getUrl(), j.getSource()));
        }
    }
}
