# RaceTrack — Developer Documentation

> **Organization:** Carroll College · **Version:** 1.0.0 · **Last Updated:** 2026-03-18 · **Maintainer:** Zack Gacnik & Jace Claassen

---

## Table of Contents

1. [Project Overview & Architecture](#1-project-overview--architecture)
2. [Setup & Installation](#2-setup--installation)

---

## 1. Project Overview & Architecture

### Summary

RaceTrack is a **server-rendered Spring Boot web application** built for Carroll College's cross country program. It allows athletes to submit and manage their daily running and workout logs, and gives coaches a unified view across all athletes with the ability to leave comments on individual log entries.

All routes are protected by Okta OIDC login via Spring Security — unauthenticated users are automatically redirected to the Okta login page before accessing any part of the application. There are no public pages and no REST API clients; every interaction originates from a logged-in browser session.

### Technology Stack

| Layer | Technology                               | Notes |
|---|------------------------------------------|---|
| Language | Java 25                                   | — |
| Framework | Spring Boot 3.x                          | — |
| View Layer | Thymeleaf                                | Server-rendered HTML templates |
| Frontend | Bootstrap 5.3 + Vanilla JS               | No frontend framework |
| ORM | Spring Data JPA / Hibernate              | — |
| Database | PostgreSQL 15+                           | — |
| Authentication | Okta OIDC + Spring Security OAuth2 Login | Authorization Code flow; browser session-based |
| Build Tool | Gradle                                   | — |

### Architecture Diagram

```
┌───────────────────────────────────────────────────────┐
│                   Browser (User)                      │
└──────────────────────────┬────────────────────────────┘
                           │ HTTPS
                           │
                 ┌─────────▼──────────┐
                 │        Okta        │
                 │   (OIDC / OAuth2)  │
                 │  integrator-9628955│
                 └─────────┬──────────┘
                           │ Authorization Code → ID Token
                           │
┌──────────────────────────▼────────────────────────────┐
│               Spring Boot Application (:8080)         │
│                                                       │
│  ┌──────────────────────────────────────────────────┐ │
│  │           Spring Security Filter Chain           │ │
│  │  OAuth2 Login · OIDC Session · CSRF Protection   │ │
│  └──────────────────────────────────────────────────┘ │
│                                                       │
│  ┌─────────────────┐  ┌────────────┐  ┌───────────┐   │
│  │  MVC Controllers│  │  Service   │  │Repository │   │
│  │  (Thymeleaf     │─▶│  Layer     │─▶│  (JPA)    │   │
│  │  + AJAX/JSON)   │  │            │  │           │   │
│  └─────────────────┘  └────────────┘  └─────┬─────┘   │
│                                             │         │
└─────────────────────────────────────────────┼─────────┘
                                              │ JDBC
                               ┌──────────────▼────────────┐
                               │       PostgreSQL DB       │
                               └───────────────────────────┘
```

### Authentication & Session Flow

RaceTrack uses the **OIDC Authorization Code flow**. The browser holds an HTTP session cookie — there are no Bearer tokens or API clients.

```
1. User visits any route
       │
       ▼
2. Spring Security intercepts → all routes require authentication
       │
       ▼
3. Redirect to /oauth2/authorization/okta → Okta login page
       │
       ▼
4. User authenticates with Okta (username/password, MFA, SSO)
       │
       ▼
5. Okta redirects to /login/oauth2/code/okta with authorization code
       │
       ▼
6. Spring Security exchanges code for ID Token, creates HTTP session
       │
       ▼
7. New users auto-provisioned as "athlete" role in the users table
       │
       ▼
8. User lands on / → HomeController serves home.html or home_coach.html
       │
       ▼
9. On logout → POST /logout
          → OidcClientInitiatedLogoutSuccessHandler
          → Okta end_session endpoint
          → Redirect to /login?logout
```

### Role Model

Roles are stored in the `users` table. New users are assigned the `athlete` role automatically on first login. The `coach` role must be granted manually via a database update.

| Role | Auto-assigned? | Can do |
|---|---|---|
| `athlete` | ✅ Yes | Submit running and workout log entries via forms; edit and delete own rows inline; view own logs in spreadsheet view |
| `coach` | ❌ No — DB update required | View all athletes' running and workout logs; add/edit coach comments on any log row; cannot submit, edit, or delete log rows |

To promote a user to coach after their first login:
```sql
UPDATE users SET role = 'coach' WHERE email = 'coach@carrollu.edu';
```

### Page & Template Structure

| Template | Route | Served to | Description |
|---|---|---|---|
| `home.html` | `GET /` | Athletes | Header, log entry forms, running sheet, workout sheet |
| `home_coach.html` | `GET /` | Coaches | Header, running sheet, workout sheet (no forms); athlete picker in footer |
| `running_form.html` | Fragment | Athletes | Inline Thymeleaf fragment — daily running log submission form |
| `workout_form.html` | Fragment | Athletes | Inline Thymeleaf fragment — workout log submission form |
| `running_sheet.html` | Fragment | All | Inline Thymeleaf fragment — running log spreadsheet table; rows injected by `sheets.js` |
| `workout_sheet.html` | Fragment | All | Inline Thymeleaf fragment — workout log spreadsheet table; rows injected by `sheets.js` |

### Frontend JavaScript

| File | Purpose |
|---|---|
| `home.js` | Form/sheet visibility toggling; athlete selection for coaches; footer athlete search filtering; post-redirect-get success banner |
| `sheets.js` | Fetches log data via internal AJAX endpoints; renders spreadsheet rows; handles inline edits, deletes, and coach comment saves; date filter buttons |
| `date-inputs.js` | Defaults all `js-date-input` date fields to today; opens native picker on focus/click |
| `tooltips.js` | Initializes all Bootstrap 5 tooltips on `DOMContentLoaded` |

### Key Backend Classes

| Class | Package | Purpose |
|---|---|---|
| `RaceTrackApplication` | `com.racetrack` | Application entry point |
| `SecurityConfig` | `com.racetrack.config` | Spring Security filter chain; Okta OAuth2 login, OIDC logout handler |
| `HomeController` | `com.racetrack.controller` | `GET /` — resolves user, determines role, passes model to `home` or `home_coach` template |
| `RunningLogController` | `com.racetrack.controller` | `POST /running-log` — handles running log form submission; redirects with `?runningSuccess` |
| `WorkoutLogController` | `com.racetrack.controller` | `POST /workout-log` — handles workout log form submission; redirects with `?workoutSuccess` |
| `LogApiController` | `com.racetrack.controller` | `GET/PUT/DELETE /api/**` — internal AJAX endpoints used by `sheets.js` for inline edits, deletes, and coach comments |
| `UserService` | `com.racetrack.service` | Okta identity sync (upsert on every login); role checks; display name resolution |
| `RunningLogService` | `com.racetrack.service` | Running log persistence; athlete-owned update/delete with ownership enforcement; coach comment updates |
| `WorkoutLogService` | `com.racetrack.service` | Workout log persistence; athlete-owned update/delete with ownership enforcement; coach comment updates |
| `User` | `com.racetrack.model` | JPA entity — Okta subject as PK, `email`, `fullName`, `role` |
| `RunningLog` | `com.racetrack.model` | JPA entity — `mileage`, `hurting`, `sleepHours`, `stressLevel`, `plateProportion`, `gotThatBread`, `feel`, `rpe`, `details`, `coachComment`, `logDate` |
| `WorkoutLog` | `com.racetrack.model` | JPA entity — `workoutType`, `completionDetails`, `actualPaces`, `workoutDescription`, `coachComment`, `logDate` |
| `UserRepository` | `com.racetrack.repository` | JPA repo; `findByRoleIgnoreCaseOrderByFullNameAsc` for coach athlete list |
| `RunningLogRepository` | `com.racetrack.repository` | JPA repo; ordered by `logDate DESC`; ownership-scoped find by `id + userId` |
| `WorkoutLogRepository` | `com.racetrack.repository` | JPA repo; ordered by `logDate DESC`; ownership-scoped find by `id + userId` |

### Project Structure

```
src/
└── main/
    ├── java/com/racetrack/
    │   ├── RaceTrackApplication.java
    │   ├── config/
    │   │   └── SecurityConfig.java
    │   ├── controller/
    │   │   ├── HomeController.java
    │   │   ├── LogApiController.java
    │   │   ├── RunningLogController.java
    │   │   └── WorkoutLogController.java
    │   ├── model/
    │   │   ├── RunningLog.java
    │   │   ├── User.java
    │   │   └── WorkoutLog.java
    │   ├── repository/
    │   │   ├── RunningLogRepository.java
    │   │   ├── UserRepository.java
    │   │   └── WorkoutLogRepository.java
    │   └── service/
    │       ├── RunningLogService.java
    │       ├── UserService.java
    │       └── WorkoutLogService.java
    └── resources/
        ├── application.yaml          # Active config — Okta + server port
        ├── application.properties    # Secondary / override properties
        └── templates/
            ├── header.html           # Thymeleaf fragment — shared header bar with logo
            ├── footer.html           # Thymeleaf fragment — fixed footer with athlete picker
            ├── home.html             # Athlete home page
            ├── home_coach.html       # Coach home page
            ├── running_form.html     # Running log submission form fragment
            ├── workout_form.html     # Workout log submission form fragment
            ├── running_sheet.html    # Running log spreadsheet fragment
            └── workout_sheet.html    # Workout log spreadsheet fragment
        └── static/
            ├── css/
            │   ├── home.css
            │   ├── header.css
            │   ├── footer.css
            │   ├── running_sheet.css
            │   └── workout_sheet.css
            ├── js/
            │   ├── home.js
            │   ├── sheets.js
            │   ├── date-inputs.js
            │   └── tooltips.js
            └── images/
                └── CarrollLogo.png   # Carroll College logo used in header
```

---

## 2. Setup & Installation

### Prerequisites

Ensure the following are installed before proceeding:

- **Java 25** — [Download](https://www.oracle.com/java/technologies/downloads/)
- **Gradle 8+** *(or use the included `gradlew` wrapper — no separate install needed)*
- **PostgreSQL 15+** — [Download](https://www.postgresql.org/download/)
- **Git**
- Access to the **Okta Admin Console** for the `integrator-9628955` Okta organization — you will need the Client ID and Client Secret

### 1. Clone the Repository

```bash
git clone https://github.com/zgacnik-carroll/RaceTrack.git
cd racetrack
```

### 2. Okta Application Verification

The Okta application is already configured at `https://integrator-9628955.okta.com`. Verify the following settings are in place in the Okta Admin Console before running locally:

| Setting | Value |
|---|---|
| Application type | Web (OIDC) |
| Grant type | Authorization Code |
| Sign-in redirect URI | `http://localhost:8080/login/oauth2/code/okta` |
| Sign-out redirect URI | `http://localhost:8080/login?logout` |
| Scopes | `openid`, `profile`, `email` |

> ⚠️ When deploying to production, add the production domain equivalents of the redirect URIs above in the Okta Admin Console — do not remove the `localhost` entries while the app is still in active development.

### 3. Database Setup

Create a PostgreSQL database and dedicated user for RaceTrack:

```sql
-- Connect as superuser
CREATE DATABASE racetrack_db;
CREATE USER racetrack_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE racetrack_db TO racetrack_user;
```

### 4. Configure `application.yaml`

The active configuration file is `src/main/resources/application.yaml`. Update it with your local database credentials. The Okta credentials are already present but **must not be committed to public source control** — move them to environment variables or a local override before pushing.

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/racetrack_db
    username: racetrack_user
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update       # Use "validate" once schema is stable
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  security:
    oauth2:
      client:
        registration:
          okta:
            client-id: ${OKTA_CLIENT_ID}
            client-secret: ${OKTA_CLIENT_SECRET}
            scope:
              - openid
              - profile
              - email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            authorization-grant-type: authorization_code
        provider:
          okta:
            issuer-uri: https://integrator-9628955.okta.com/oauth2/default
```

> ⚠️ **Never commit `client-secret` in plaintext to source control.** Use environment variables (`OKTA_CLIENT_ID`, `OKTA_CLIENT_SECRET`) or a local `application-dev.yaml` file that is listed in `.gitignore`.

#### Environment Variable Reference

| Variable | Required | Description |
|---|---|---|
| `OKTA_CLIENT_ID` | ✅ | Okta application Client ID |
| `OKTA_CLIENT_SECRET` | ✅ | Okta application Client Secret |
| `DB_URL` | ✅ (prod) | JDBC connection URL for the PostgreSQL database |
| `DB_USERNAME` | ✅ (prod) | Database username |
| `DB_PASSWORD` | ✅ (prod) | Database password |

### 5. Build & Run

```bash
./gradlew bootRun
```

### 6. Verify the Application

Open a browser and navigate to:

```
http://localhost:8080/
```

You will be redirected to the Okta login page. After authenticating, you will land on the athlete home page. New users are automatically provisioned with the `athlete` role on first login.

To promote a user to the `coach` role:

```sql
UPDATE users SET role = 'coach' WHERE email = 'coach@carrollu.edu';
```

---
