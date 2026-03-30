# Student Concern Management System (SCMS)

Student Concern Management System for Akademy of Knowledge Bridge.

This project is a Java Spring Boot web application that supports:
- Student concern submission and tracking
- Admin concern handling and replies
- Owner-level admin management and analytics
- Community discussion with AI moderation
- Email verification and password reset flows
- Student notification center (personal + broadcast)

## Table of Contents
- [Overview](#overview)
- [Core Features](#core-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Role Workflows](#role-workflows)
- [Key Routes](#key-routes)
- [Database Overview](#database-overview)
- [AI Moderation](#ai-moderation)
- [Scripts and Special Notes](#scripts-and-special-notes)
- [Security Notes](#security-notes)
- [Run Tests](#run-tests)

## Overview
SCMS is a multi-role platform where students can submit concerns (with optional evidence), track resolution progress, and provide feedback after admin replies. It also includes a moderated student community area and owner-level analytics/reporting pages.

The application is server-rendered with Thymeleaf templates and uses SQL Server through Spring Data JPA.

## Core Features

### Student
- Register account with email verification
- Login/logout with session-based access
- Reset password via email OTP flow
- Submit concerns with category and optional evidence attachment
- View concern history, replies, and feedback status
- Submit/update/delete concern feedback with moderation checks
- Access community posts/replies after accepting community rules
- Live moderation check for community content
- Manage profile details and password
- View notifications and unread count

### Admin
- View dashboards with concern filters (status/time/category)
- Handle concerns: reply, update status, change category, delete
- View and manage student feedback insights
- Review and approve/reject pending student registrations
- Moderate community posts/replies and participate as admin

### Owner
- Access owner dashboard and analytics reports
- Create/manage admin accounts (with email verification)
- Refresh analytics report metrics
- Send and view broadcast notifications

## Tech Stack
- Java 21
- Spring Boot 4.0.2
- Spring MVC + Thymeleaf
- Spring Data JPA (Hibernate)
- Microsoft SQL Server (`mssql-jdbc`)
- Spring Mail (SMTP)
- Spring Security Crypto (`BCryptPasswordEncoder`)
- TensorFlow Core Platform dependency (`org.tensorflow:tensorflow-core-platform:0.5.0`)
- Google Gemini API integration for text moderation
- Maven build system

## Project Structure
```text
.
├── pom.xml
├── SCMS SQL.sql
├── src
│   ├── main
│   │   ├── java/Project/_6/demo
│   │   │   ├── controller
│   │   │   ├── service
│   │   │   ├── repository
│   │   │   ├── entity
│   │   │   ├── dto
│   │   │   └── config
│   │   └── resources
│   │       ├── application.properties
│   │       ├── templates
│   │       └── static
│   │           ├── CSS
│   │           ├── js
│   │           └── images
│   └── test
└── remove_notif.py
```

## Getting Started

### 1. Prerequisites
- Java 21 installed
- Maven installed (or use `./mvnw`)
- Microsoft SQL Server instance
- SMTP credentials for email sending
- Gemini API key (optional but recommended for AI moderation)

### 2. Database Setup
1. Create a dedicated database (recommended: do not use SQL Server system databases).
2. Execute `SCMS SQL.sql` to create required tables.
3. Ensure the database user has read/write schema privileges.

Note: Application startup includes a migration helper (`CommandLineRunner`) that adds `ReplyID_FK` to `Feedback` if missing.

### 3. Configure Application
Edit `src/main/resources/application.properties` for your environment:
- SQL Server URL/username/password
- SMTP host, sender email, and app password
- Gemini API key/model
- Server port (default `9090`)

### 4. Build and Run
Using Maven Wrapper:
```bash
./mvnw clean package
./mvnw spring-boot:run
```

Or with Maven:
```bash
mvn clean package
mvn spring-boot:run
```

Open:
- `http://localhost:9090/`

## Configuration

### Important Runtime Settings (from current project)
- `server.port=9090`
- Session timeout: 24 hours (`server.servlet.session.timeout=86400`)
- Upload limit: 10MB
- Upload path mapping: `/uploads/** -> file:uploads/`

### Suggested Environment Variable Pattern
Use placeholders in `application.properties`:
```properties
spring.datasource.password=${DB_PASSWORD}
spring.mail.password=${MAIL_APP_PASSWORD}
gemini.api.key=${GEMINI_API_KEY}
```

## Role Workflows

### Student Flow
Register -> Verify Email -> Wait for Admin Approval -> Login -> Submit Concern -> Track Status/Replies -> Submit Feedback.

### Admin Flow
Login -> Review Dashboard -> Open Concern -> Reply/Update Status -> Review Feedback -> Moderate Community.

### Owner Flow
Login -> Open Owner Dashboard -> Manage Admin Accounts -> Generate/Refresh Reports -> Broadcast Notifications.

## Key Routes

### Public
- `GET /`
- `GET /login`
- `GET /register`
- `POST /register/send-code`
- `POST /register/verify-code`
- `POST /register`
- `GET /forgot-password`
- `POST /forgot-password/send-code`
- `POST /forgot-password/verify-code`
- `POST /forgot-password/reset`

### Student
- `GET /student/dashboard`
- `GET /submit-concern`
- `POST /submit-concern`
- `GET /student/concern-history`
- `POST /student/feedback`
- `POST /student/feedback/update`
- `POST /student/feedback/delete`
- `GET /student/profile`
- `POST /student/profile/update`
- `POST /student/profile/change-password`
- `GET /student/community`
- `POST /student/community/posts`
- `POST /student/community/posts/{postId}/replies`
- `POST /student/community/moderate`

### Admin
- `GET /admin/dashboard`
- `GET /admin/edu-dashboard`
- `GET /admin/feedback`
- `GET /admin/concern/{id}`
- `POST /admin/concern/{id}/reply`
- `POST /admin/concern/{id}/status`
- `POST /admin/concern/{id}/category`
- `GET /admin/community`
- `POST /admin/community/moderate`
- `GET /admin/student-review`

### Owner
- `GET /owner/dashboard`
- `GET /owner/admin/create-page`
- `GET /owner/admin/manage`
- `POST /owner/admin/create`
- `POST /owner/report/create`
- `GET /owner/api/concerns/count`
- `GET /owner/api/resolution-time`
- `GET /owner/api/admins`
- `GET /owner/api/sentiment`
- `POST /owner/api/report/refresh/{id}`
- `POST /owner/api/reports/refresh-all`
- `GET /owner/notifications`
- `POST /owner/notifications/send`

### Notification API
- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/mark-all-read`

## Database Overview
Main tables used by the application:
- `User`
- `Student`
- `Admin`
- `Concern`
- `Admin_reply`
- `Feedback`
- `Notification`
- `Analytics_Report`
- `Student_Community_Post`
- `Student_Community_Reply`
- `Student_Community_Rules_Acceptance`
- `Student_Community_Moderation_Log`

Relationships include:
- `User` as base account
- `Student` and `Admin` linked by `UserID`
- `Concern` linked to student and optionally admin
- `Feedback` linked to concern and latest admin reply
- Community posts/replies linked to students, with admin-name support for moderator replies

## AI Moderation
Community and feedback moderation use a layered approach:
1. Local rule checks (PII patterns, banned words, language checks)
2. Gemini API moderation (`gemini-2.0-flash` with fallback model/version handling)
3. Moderation log persistence in `Student_Community_Moderation_Log`

Live moderation is exposed to the front-end for typing-time checks in community forms.

## Scripts and Special Notes
- `remove_notif.py`: utility script that removes a specific overlay block from `admin-community-chat.html`.
- Guided UI tours are implemented via:
	- `src/main/resources/static/js/student-tour.js`
	- `src/main/resources/static/js/admin-tour.js`

## Security Notes
The current repository configuration contains sensitive values in `application.properties` (database credentials, email app password, Gemini API key).

Recommended before sharing/deploying:
- Rotate all exposed secrets immediately
- Move secrets to environment variables
- Use a dedicated DB (not a system DB)
- Configure HTTPS for production
- Add rate limiting for authentication/moderation endpoints
- Validate upload MIME types server-side
- Reduce long session timeout for production use

Also note: owner login credentials are currently hardcoded in controller logic. Move these to secure storage and replace with proper role-based authentication.

## Run Tests
```bash
./mvnw test
```

or

```bash
mvn test
```

