# Job Hunter

Upload your resume, and Job Hunter searches for matching jobs **automatically every day**,
keeps a report of matched / applied jobs, and emails you a daily summary.

- **Backend:** Java 17 + Spring Boot 3 (REST API, scheduler, JPA/H2, email)
- **Frontend:** plain HTML + CSS + JavaScript (served by Spring Boot)

## Features

- Upload a resume (PDF / DOCX / TXT); text and skill keywords are extracted automatically.
- Pluggable job sources (`JobSource`): a built-in working **mock** source plus a
  **naukri.com** adapter stub ready for a real integration.
- A daily scheduler (`0 0 8 * * *` by default) searches, scores matches against your
  resume, auto-applies to strong matches, and emails a report.
- Applied-jobs **report** in the UI and via `GET /api/jobs/applied`.
- Run on demand with the **"Run search now"** button or `POST /api/run`.

## Run locally

```bash
mvn spring-boot:run
```

Then open http://localhost:8080

The H2 console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:jobhunter`).

## Configuration

All settings live in `src/main/resources/application.properties` and can be overridden
with environment variables:

| Setting | Env var | Default |
| --- | --- | --- |
| Daily schedule (cron) | `JOBHUNTER_SCHEDULE` | `0 0 8 * * *` |
| Scheduler enabled | `JOBHUNTER_SCHEDULE_ENABLED` | `true` |
| Auto-apply min score | `JOBHUNTER_AUTOAPPLY_MIN_SCORE` | `50` |
| Default location | — | `India` |
| Email enabled | `JOBHUNTER_MAIL_ENABLED` | `false` |
| Email recipient | `JOBHUNTER_MAIL_TO` | — |
| SMTP host/user/pass | `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP |
| Enable Naukri source | `JOBHUNTER_NAUKRI_ENABLED` | `false` |

When email is disabled (the safe local default) the daily report is written to the logs
instead of being sent.

### Enabling email (Gmail example)

```bash
export JOBHUNTER_MAIL_ENABLED=true
export JOBHUNTER_MAIL_TO=you@example.com
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=<gmail app password>
mvn spring-boot:run
```

## Adding a real job source (e.g. naukri.com)

Naukri has no public search API, and scraping/auto-applying needs a logged-in session and
generally violates the site's terms (bot detection, captchas). The integration point is
`NaukriJobSource`:

1. Set `JOBHUNTER_NAUKRI_ENABLED=true`.
2. Provide credentials via environment variables (never commit them).
3. Implement `search(...)` using an HTTP client or a headless browser (Playwright/Selenium)
   driving a logged-in session, mapping each result to a `JobPosting`.

Any Spring `@Component` implementing `JobSource` is picked up automatically.

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/resume` | Upload a resume (multipart `file`, optional `location`) |
| `GET` | `/api/resume` | Get the active resume summary |
| `GET` | `/api/jobs` | All matched jobs |
| `GET` | `/api/jobs/applied` | Applied-jobs report |
| `POST` | `/api/jobs/{id}/apply` | Mark a job applied |
| `POST` | `/api/run` | Trigger a full search/apply/email run now |
