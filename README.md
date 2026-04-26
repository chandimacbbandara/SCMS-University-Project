<div align="center">

# Student Concern Management System (SCMS)

Role-based student concern handling platform for Akademy of Knowledge Bridge.

[![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Server%20Rendered-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![MySQL](https://img.shields.io/badge/MySQL-Aiven_Cloud-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Gemini](https://img.shields.io/badge/Gemini-Moderation-8E75B2?style=for-the-badge&logo=googlebard&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

</div>

## Overview
SCMS is a full-stack web system for managing student concerns from submission to resolution. It includes role-based flows for Students, Admins, and Owner users, with AI-assisted moderation and priority prediction.

## Core Features Implemented
- Registration with email OTP verification and admin approval flow
- Role-based login for student, admin, and owner
- Concern lifecycle: draft, submit, edit (pending), chat, meeting scheduling, completion
- Linked concerns support for threaded concern history
- Evidence upload handling via `/uploads/**`
- Student community forum with rules acceptance, post/reply CRUD, moderation actions
- AI moderation for community and feedback content with safe local fallback rules
- AI concern priority prediction integrated into concern submit/update flows
- Admin dashboard and education dashboard with filtering and analytics
- Owner workspace for admin management, fast analytics/report APIs, notifications, FAQ/tips
- Notification center with unread tracking and broadcast support
- Feedback module tied to concern completion/replies
- Centralized Overall Student Feedback portal with individual submission, editing, and sorting
- Dynamic Homepage displaying Top 5 Student Concerns based on recent submissions

## Team and Feature Contribution
The following mapping is updated according to the TAF contribution tables.

| Registration Number | Student Name | Name of Feature |
|---|---|---|
| IT24100086 | Vaishavi. I | Student Concern & Evidence Manager |
| IT24100307 | Bandara I G C | Admin Dashboard & Inquiry Handling |
| IT24100677 | Samarathunga S.D.D. | Identity & Access Management |
| IT24100754 | Dilanya A.N.G.A | Resolution Quality & Feedback System |
| IT24101392 | Wickramasekara.J.K.A.D.N. | Notification & Status Tracking Service |
| IT24102060 | Umar Z.M.Z | Analytics & Decision Support Module |

### Main Feature Responsibilities (TAF Section 8)
1) IT24100086 - Student Concern & Evidence Manager
- Create: Students submit new concerns and upload relevant screenshots/video.
- Read: Students browse their own submission history before/after sending.
- Update: Students can modify or clarify a pending concern if new details arise.
- Delete: Students can withdraw a concern if the issue is resolved independently.

2) IT24100307 - Admin Dashboard & Inquiry Handling
- Create: Admins generate official responses and internal notes for concerns.
- Read: Admins view the list of concerns sorted by the AI assigned priority levels.
- Update: Admins edit response drafts or re-categorize ticket types.
- Delete: Admins can remove duplicate or spam tickets from the dashboard.

3) IT24100677 - Identity & Access Management
- Create: New students register accounts and set up security credentials.
- Read: Users view their profile details and account security logs.
- Update: Users update personal info or reset forgotten passwords via secure tokens.
- Delete: Admins or users can deactivate/remove accounts for security compliance.

4) IT24100754 - Resolution Quality & Feedback System
- Create: Students submit ratings (Good/Bad) and comments on the resolution quality.
- Read: Admins view feedback reports to audit the effectiveness of the department.
- Update: Students can revise their feedback if an initially bad resolution is corrected.
- Delete: System clears old feedback data after a specific audit period.

5) IT24101392 - Notification & Status Tracking Service
- Create: System generates real-time alerts when status changes (Pending -> Resolved).
- Read: Users track the live progress bar and timestamps history of their concern.
- Update: Automated updates to notification preferences.
- Delete: Users can clear their notification inbox or dismiss alerts.

6) IT24102060 - Analytics & Decision Support Module
- Create: Generate analytical reports (monthly/semester-wise) on student concerns by category, department, and urgency.
- Read: Admins view dashboards showing trends such as most frequent issues, peak complaint periods, and resolution delays.
- Update: System automatically updates analytics when new concerns or resolutions are added.
- Delete: Old or archived analytical data can be purged after institutional policy retention periods.

## AI/ML Contribution (TAF)
Feature Name: Intelligent Urgency Classification & Priority Sorting Engine

| Registration Number | Expected Individual Contribution to the AI/ML Feature |
|---|---|
| IT24100086 | Collects student management data and structures it into a consistent Excel/CSV dataset. |
| IT24100307 | Cleans the dataset by fixing missing values, format errors, and duplicates. |
| IT24100677 | Selects key features and prepares the final model-ready dataset. |
| IT24100754 | Chooses an ML algorithm and trains the sentimental analysis prediction model. |
| IT24101392 | Evaluates model accuracy and tunes parameters to improve performance. |
| IT24102060 | Integrates the trained model into the Spring Boot system to provide a demand forecast. |

### Current AI Implementation in This Repository
1) Gemini moderation pipeline
- Services: `CommunityModerationService`, `FeedbackModerationService`
- Includes local moderation rules + Gemini fallback strategy for reliability.

2) Concern priority model pipeline
- Java bridge: `ConcernPriorityService`
- Python inference: `ai-model/concern-priority/predict_priority.py`
- Model artifact: `ai-model/concern-priority/model.safetensors`

## Tech Stack
- Java 21
- Spring Boot 4.0.2
- Spring MVC + Thymeleaf
- Spring Data JPA (Hibernate)
- MySQL Connector/J
- Spring Mail (SMTP)
- Spring Security Crypto (BCrypt)
- TensorFlow Java dependency (present in `pom.xml`)
- Python AI stack (`torch`, `transformers`, `safetensors`, `scikit-learn`, `joblib`)
- Maven Wrapper (`mvnw`)

## Project Structure
```text
.
├── ai-model/
│   └── concern-priority/
│       ├── model.safetensors
│       ├── predict_priority.py
│       └── requirements.txt
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
├── SCMS SQL.sql
├── pom.xml
├── remove_notif.py
└── uploads/
```

## Runtime Schema Compatibility
On startup, `StudentConcernManagementSystemApplication` includes compatibility runners to align schema safely:
- ensure `Admin_reply.Sender_Role`
- ensure `SCMS_Feedback.ReplyID_FK`
- synchronize legacy `feedback` compatibility paths
- ensure `Analytics_Report.EvidenceImageCount`
- ensure concern meeting tables and indexes:
  - `Concern_Meeting_Proposal`
  - `Concern_Meeting_Slot`
- ensure linked concern reference:
  - `Concern.Linked_ConcernID_FK`
  - related index and FK
- align FAQ/tips nullable compatibility columns
- remove legacy predefined admin account (`ADMIN001` / `admin@akb.edu`) when detected

## Configuration
Main config:
- `src/main/resources/application.properties`

Current runtime defaults include:
- server port: `9090`
- datasource: MySQL (Aiven URL pattern)
- multipart upload limit: `10MB`
- session timeout: `86400` seconds
- Gemini moderation model config
- concern priority AI script/model config

### Recommended Production Secret Pattern
```properties
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<db>
SPRING_DATASOURCE_USERNAME=<db_user>
SPRING_DATASOURCE_PASSWORD=<db_password>

SPRING_MAIL_USERNAME=<smtp_user>
SPRING_MAIL_PASSWORD=<smtp_app_password>

GEMINI_API_KEY=<gemini_key>
```

## Local Setup
### 1) Prerequisites
- JDK 21+
- Maven (or use `./mvnw`)
- MySQL database
- Python 3.9+
- SMTP credentials
- Gemini API key

### 2) Database
1. Create a database (example: `scmsdb`).
2. Run `SCMS SQL.sql` as base schema.
3. Update datasource configuration.

### 3) Optional AI Python Environment (Concern Priority)
```bash
cd ai-model/concern-priority
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 4) Build and Run
```bash
./mvnw clean package
./mvnw spring-boot:run
```

Application URL:
- `http://localhost:9090`

## Linux Deployment (systemd)
Typical flow:
1. Build artifact:
```bash
./mvnw clean package -DskipTests
```
2. Create env file (`/etc/scms.env`) with datasource/mail/gemini secrets.
3. Configure `scms.service` with auto-restart and proper user permissions.
4. Monitor service logs:
```bash
sudo systemctl status scms --no-pager -l
sudo systemctl restart scms
tail -f /home/<user>/SCMS/app.log
```

## Routes (Quick Map)
### Public/Auth
- `GET /`
- `GET /login`, `POST /login`, `GET /logout`
- `GET /register`, `POST /register`
- OTP routes: `/register/send-code`, `/register/verify-code`
- forgot-password routes: send/verify/reset
- `GET /faq`

### Student
- dashboard/profile/photo endpoints
- concern submission, edit, draft, history endpoints
- student concern chat and completion endpoints
- meeting book/decline endpoints
- feedback create/update/delete endpoints
- overall feedback view/submit/update/delete endpoints (`/student/overall-feedback`)

### Student Community (`/student/community`)
- rules, accept-rules
- posts create/update/delete
- replies create/update/delete
- moderation endpoint

### Admin (`/admin`)
- dashboard + edu dashboard
- concern detail and lifecycle actions
- reply create/update/delete
- meeting proposal
- community moderation and admin replies

### Owner (`/owner`)
- dashboard
- admin workspace (create/manage/update/delete + OTP verification)
- analytics/report APIs
- notification CRUD/send
- FAQ and tips CRUD
- student feedback portal (`/owner/student-feedback`)

### Notification API (`/api/notifications`)
- list
- unread count
- mark as read
- mark all as read
- delete

## Database Model (High Level)
Core tables currently mapped/used:
- `Users`
- `Student`
- `Admin`
- `Concern`
- `Admin_reply`
- `SCMS_Feedback`
- `OverallFeedback`
- `Notification`
- `Analytics_Report`
- `Concern_Meeting_Proposal`
- `Concern_Meeting_Slot`
- `Student_Community_Post`
- `Student_Community_Reply`
- `Student_Community_Rules_Acceptance`
- `Student_Community_Moderation_Log`
- `faqs`
- `tips`

## Testing
Current automated tests are minimal:
- `StudentConcernManagementSystemApplicationTests.contextLoads()`

Run tests:
```bash
./mvnw test
```

## Security Notes (Important)
Before production use:
- remove hardcoded owner login credentials from controller layer
- rotate all currently exposed credentials from history/config
- move all secrets to environment variables or secret manager
- enforce HTTPS via reverse proxy
- add auth rate limits and abuse controls
- harden upload validation (MIME/content checks)
- review moderation and audit logs regularly

## Useful Commands
```bash
# compile without tests
./mvnw -DskipTests compile

# run tests
./mvnw test

# run app
./mvnw spring-boot:run
```

---

Built for Akademy of Knowledge Bridge.
