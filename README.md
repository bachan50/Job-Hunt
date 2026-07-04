# Job Hunter

Upload your resume, and Job Hunter searches for matching jobs **automatically every day**,
keeps a report of matched / applied jobs, and emails you a daily summary.

- **Backend:** Java 17 + Spring Boot 3 (REST API, scheduler, JPA/H2, email)
- **Frontend:** plain HTML + CSS + JavaScript (served by Spring Boot)

## Features

- Upload a resume (PDF / DOCX / TXT); text and skill keywords are extracted automatically.
- Pluggable job sources (`JobSource`): a built-in working **mock** source, a live
  **Adzuna** API source (real jobs), and a **naukri.com** adapter stub.
- Configure Adzuna (free API key) for real listings; the mock source turns off
  automatically once Adzuna is configured.
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
| Adzuna app id | `ADZUNA_APP_ID` | — |
| Adzuna app key | `ADZUNA_APP_KEY` | — |
| Adzuna country | `ADZUNA_COUNTRY` | `in` |
| Mock source enabled | `JOBHUNTER_MOCK_ENABLED` | `true` (auto-off with Adzuna) |
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

## Live jobs via Adzuna (recommended)

[Adzuna](https://developer.adzuna.com) offers a free API key and permits programmatic
access, so it is used as the real job source:

1. Sign up at https://developer.adzuna.com/signup and copy your **App ID** and **App Key**
   from https://developer.adzuna.com/admin/access_details.
2. Run with the keys set:

   ```bash
   export ADZUNA_APP_ID=xxxx
   export ADZUNA_APP_KEY=yyyy
   export ADZUNA_COUNTRY=in   # optional; default "in" (India)
   mvn spring-boot:run
   ```

With Adzuna configured the mock source is disabled and the daily run returns real
listings. Each job's title links to the live posting so you can complete the application
there (Adzuna does not offer programmatic apply).

### About naukri.com

Naukri has no public search API, and its login is protected by anti-bot measures
(generic "invalid details" responses and looping reCAPTCHA challenges) that reject
automated/datacenter sessions **even with correct credentials**; auto-applying also
violates their terms. So Naukri is not usable from a server environment. `NaukriJobSource`
remains as an integration point if you run this tool on your own machine (residential IP,
real logged-in browser). Any Spring `@Component` implementing `JobSource` is picked up
automatically.

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/resume` | Upload a resume (multipart `file`, optional `location`) |
| `GET` | `/api/resume` | Get the active resume summary |
| `GET` | `/api/jobs` | All matched jobs |
| `GET` | `/api/jobs/applied` | Applied-jobs report |
| `POST` | `/api/jobs/{id}/apply` | Mark a job applied |
| `POST` | `/api/run` | Trigger a full search/apply/email run now |
