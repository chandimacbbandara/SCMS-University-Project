<div align="center">

# Student Concern Management System (SCMS)

Role-based concern handling platform for Akademy of Knowledge Bridge.

<p>
	<img src="https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
	<img src="https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 4.0.2" />
	<img src="https://img.shields.io/badge/Thymeleaf-Server%20Rendered-005F0F?style=for-the-badge" alt="Thymeleaf" />
	<img src="https://img.shields.io/badge/SQL%20Server-Database-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white" alt="SQL Server" />
	<img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven" />
</p>

</div>

## Overview
SCMS supports end-to-end student concern handling with role-based flows for Students, Admins, and Owner users.

Implemented modules include:
- student registration with email OTP verification and admin approval
- role-based login (student, admin, owner)
- concern submission with evidence upload
- draft concern workflow (save draft, edit draft, submit later)
- concern lifecycle with status tracking and admin replies
- physical meeting proposal and slot booking flow
- feedback workflow with moderation and update/delete rules
- student community module with rules acceptance and AI moderation
- owner/admin dashboards and analytics APIs
- notification center (direct + broadcast notifications)

## Tech Stack
- Java 21
- Spring Boot 4.0.2
- Spring MVC + Thymeleaf
- Spring Data JPA (Hibernate)
- Microsoft SQL Server JDBC driver
- Spring Mail (SMTP)
- Spring Security Crypto (BCrypt)
- Google Gemini API integration for moderation
- Maven (with wrapper)

## Project Structure
```text
.
├── pom.xml
├── SCMS SQL.sql
├── src/
│   ├── main/
│   │   ├── java/Project/_6/demo/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   └── config/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── templates/
│   │       └── static/
│   └── test/
└── remove_notif.py
```

Key templates include:
- student concern draft page: `student-concern-drafts.html`
- owner FAQ management pages: `owner-faq*.html`
- admin community moderation page: `admin-community-chat.html`

## Runtime Schema Behavior
The app includes startup migration/compatibility runners in `StudentConcernManagementSystemApplication`:
- adds/aligns `ReplyID_FK` for `SCMS_Feedback`
- syncs legacy `feedback` table from `SCMS_Feedback` when needed
- ensures `Analytics_Report.EvidenceImageCount`
- ensures concern-meeting tables/columns/indexes exist
- aligns FAQ/Tips compatibility columns
- removes legacy predefined admin account (`ADMIN001` / `admin@akb.edu`) when present

The domain model currently uses:
- `SCMS_Feedback` as the JPA feedback table
- legacy `feedback` table compatibility sync for older environments
- notification soft-hide flag (`IsHidden`) for student notification cleanup

## ID Strategy Compatibility
This project handles both database modes:
- identity PK mode
- non-identity PK mode (manual next-id assignment)

For non-identity schemas, the service layer auto-assigns IDs for:
- `User`
- `Concern`
- `Admin_reply`
- `Notification`
- `Analytics_Report`

## Configuration
Main config file: `src/main/resources/application.properties`

Current defaults include:
- server port `9090`
- SQL Server datasource
- session timeout `86400` seconds
- max upload size `10MB`
- upload path mapping `/uploads/** -> file:uploads/`

Recommended production secret pattern:
```properties
spring.datasource.password=${DB_PASSWORD}
spring.mail.password=${MAIL_APP_PASSWORD}
gemini.api.key=${GEMINI_API_KEY}
```

## Local Setup

### 1) Prerequisites
- JDK 21
- Maven or Maven Wrapper
- SQL Server (local/remote/container)
- SMTP credentials (for OTP and alerts)
- Gemini API key (for AI moderation features)

### 2) Database Setup
1. Create a dedicated SQL Server database (recommended: `scmsdb`).
2. Run [SCMS SQL.sql](SCMS%20SQL.sql).
3. Grant read/write permissions to the configured user.

Notes:
- The script is useful as a base schema.
- Startup migration runners may add/adjust extra columns/tables for compatibility.

### 3) Build and Run
```bash
./mvnw clean package
./mvnw spring-boot:run
```

Or:
```bash
mvn clean package
mvn spring-boot:run
```

App URL: `http://localhost:9090`

## Linux/VPS Deployment (systemd)
Typical production flow:
1. Upload project zip to server.
2. Extract to deployment folder.
3. Build jar:
```bash
./mvnw clean package -DskipTests
```
4. Configure environment in `/etc/scms.env`:
```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://<host>:1433;databaseName=<db>;encrypt=true;trustServerCertificate=true;
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```
5. Run as a systemd service (`scms.service`) with auto-restart.

Useful commands:
```bash
sudo systemctl status scms --no-pager -l
sudo systemctl restart scms
tail -f /home/<user>/SCMS/app.log
```

## Route Quick Map

### Public/Auth
- `GET /`
- `GET /login`
- `POST /login`
- `GET /logout`
- `GET /register`
- `POST /register`
- `POST /register/send-code`
- `POST /register/verify-code`
- `GET /forgot-password`
- `POST /forgot-password/send-code`
- `POST /forgot-password/verify-code`
- `POST /forgot-password/reset`
- `GET /faq`

### Student
- `GET /student/dashboard`
- `GET /student/profile`
- `POST /student/profile/update`
- `POST /student/profile/change-password`
- `GET /student/profile/photo`
- `GET /student/photo/{userId}`
- `GET /submit-concern`
- `POST /submit-concern`
- `GET /student/concern/{id}/edit`
- `POST /student/concern/update`
- `GET /student/concern-drafts`
- `POST /student/concern/draft/submit`
- `GET /student/concern-history`
- `POST /student/concern/{id}/meeting/book`
- `POST /student/concern/{id}/meeting/decline`
- `POST /student/feedback`
- `POST /student/feedback/update`
- `POST /student/feedback/delete`
- `POST /student/concern/delete`

### Student Community
Base path: `/student/community`
- `GET /student/community`
- `GET /student/community/rules`
- `POST /student/community/rules/accept`
- `POST /student/community/posts`
- `POST /student/community/posts/{postId}/update`
- `POST /student/community/posts/{postId}/delete`
- `POST /student/community/posts/{postId}/replies`
- `POST /student/community/replies/{replyId}/update`
- `POST /student/community/replies/{replyId}/delete`
- `POST /student/community/moderate`

### Admin
Base path: `/admin`
- `GET /admin/dashboard`
- `GET /admin/edu-dashboard`
- `GET /admin/feedback`
- `GET /admin/student-review`
- `POST /admin/student-review/{userId}/approve`
- `POST /admin/student-review/{userId}/reject`
- `POST /admin/student-review/{userId}/delete`
- `GET /admin/student-review/{userId}/photo`
- `GET /admin/concern/{id}`
- `POST /admin/concern/{id}/meeting/propose`
- `POST /admin/concern/{id}/reply`
- `POST /admin/concern/{concernId}/reply/{replyId}/delete`
- `POST /admin/concern/{concernId}/reply/{replyId}/update`
- `POST /admin/concern/{id}/status`
- `POST /admin/concern/{id}/category`
- `POST /admin/concern/{id}/delete`
- `GET /admin/community`
- `POST /admin/community/post/{id}/delete`
- `POST /admin/community/reply/{id}/delete`
- `POST /admin/community/post/{id}/reply`
- `POST /admin/community/post/{id}/replies`
- `POST /admin/community/moderate`

### Owner
Base path: `/owner`
- `GET /owner/dashboard`
- `GET /owner/admin/create-page`
- `GET /owner/admin/manage`
- `POST /owner/admin/send-code`
- `POST /owner/admin/verify-code`
- `POST /owner/admin/create`
- `POST /owner/admin/{userId}/update`
- `POST /owner/admin/{userId}/delete`
- `POST /owner/admin/{userId}/email/send-code`
- `POST /owner/admin/{userId}/email/verify-code`
- `POST /owner/report/create`
- `POST /owner/report/delete/{id}`
- `GET /owner/api/report/metrics`
- `GET /owner/api/analytics/charts`
- `POST /owner/api/concerns/rejected/delete-all`
- `GET /owner/api/reports/monthly`
- `GET /owner/api/concerns/count`
- `GET /owner/api/resolution-time`
- `GET /owner/api/admins`
- `GET /owner/api/sentiment`
- `POST /owner/api/report/refresh/{id}`
- `POST /owner/api/reports/refresh-all`
- `GET /owner/notifications`
- `POST /owner/notifications/send`
- `GET /owner/notifications/delete/{id}`
- `GET /owner/notifications/update/{id}`
- `POST /owner/notifications/update/{id}`
- `GET /owner/faq`
- `POST /owner/faq/tip/create`
- `POST /owner/faq/tip/delete/{id}`
- `GET /owner/faq/tip/update/{id}`
- `POST /owner/faq/tip/update/{id}`
- `POST /owner/faq/faq/create`
- `POST /owner/faq/faq/delete/{id}`
- `GET /owner/faq/faq/update/{id}`
- `POST /owner/faq/faq/update/{id}`

### Notification API (JSON)
Base path: `/api/notifications`
- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/mark-all-read`
- `DELETE /api/notifications/{id}`

## Database Model (High Level)
Core tables used by the app:
- `[User]`
- `Student`
- `Admin`
- `Concern`
- `Admin_reply`
- `SCMS_Feedback`
- `Notification`
- `Analytics_Report`
- `Student_Community_Post`
- `Student_Community_Reply`
- `Student_Community_Rules_Acceptance`
- `Student_Community_Moderation_Log`
- `Concern_Meeting_Proposal`
- `Concern_Meeting_Slot`
- `faqs`
- `tips`

## Moderation Logic
Community and feedback moderation use layered checks:
1. local validation (PII patterns, language gate, bad-word list, structure rules)
2. Gemini moderation (with model and API-version fallback)
3. moderation logs persisted in DB

## Testing
```bash
./mvnw test
```

or

```bash
mvn test
```

Current automated test coverage is minimal (`contextLoads` smoke test), so manual functional testing is still important.

## Security Notes
Before production rollout:
- remove hardcoded owner credentials from login flow
- rotate all credentials/secrets currently present in config/history
- move secrets to environment variables or secret manager
- use a dedicated DB instead of system databases
- enable HTTPS + reverse proxy
- add rate limiting for auth/moderation endpoints
- validate uploads with stricter server-side MIME/content checks
- tune session timeout for production policies

## Useful SQL Checks
Use [SCMS SQL.sql](SCMS%20SQL.sql) verification/query packs for quick checks.

Example:
```sql
SELECT TOP 10 ConcernID, Subject, Status, CreatedTime
FROM Concern
ORDER BY ConcernID DESC;
```

