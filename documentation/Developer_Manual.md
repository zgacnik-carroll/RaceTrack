# RaceTrack - Developer Manual

> Organization: Carroll College
> 
> Last Updated: 2026-04-29  
> 
> Developers: Zack Gacnik and Jace Claassen

*Note: This application is only accessible through the Carroll HaloNet WiFi*

*Once you are connected to HaloNet, navigate to [racetrack.carroll.edu](racetrack.carroll.edu)*

---

## 1. Purpose

This manual explains the current RaceTrack codebase as it exists on 4/29/2026.
It covers:

- architecture and runtime behavior
- roles and authorization rules
- backend and frontend structure
- local development setup
- the exact Okta OIDC values IT must replace

---

## 2. Current Application Summary

RaceTrack is a server-rendered Spring Boot web application for cross country training logs.

The app has two main data areas:

- running logs
- workout logs

All routes require authentication through Okta OIDC. After Okta login, RaceTrack authorizes access by checking whether the signed-in email already exists in the local `users` table.

Important current behavior:

- users are **not auto-provisioned on first login**
- if the Okta email is missing from the local `users` table, the app redirects to `/unauthorized-user`
- coaches have UI and API tools for local RaceTrack user management
- those admin actions currently update the RaceTrack database only
- `OktaAdminClient` exists in the codebase, but `AdminService` does **not** currently call it

That means:

- adding a user in RaceTrack grants RaceTrack access for that email
- it does **not** create or update an Okta account by itself

---

## 3. Technology Stack

| Layer | Current Technology | Notes |
|---|---|---|
| Language | Java 25 | Configured in `build.gradle` toolchain |
| Framework | Spring Boot 4.0.2 | Via Gradle plugin |
| Security | Spring Security OAuth2 Client | Okta OIDC login/logout |
| View layer | Thymeleaf | Server-rendered pages and fragments |
| Frontend | Bootstrap 5.3 + vanilla JS | No SPA framework |
| Persistence | Spring Data JPA / Hibernate | PostgreSQL-backed |
| Database | PostgreSQL | `ddl-auto=update` in current config |
| Build | Gradle wrapper | `gradlew` / `gradlew.bat` |

---

## 4. Architecture Overview

### Request flow

1. Browser requests an application route.
2. `SecurityConfig` requires authentication.
3. Spring Security redirects to `/oauth2/authorization/okta`.
4. Okta authenticates the user and redirects back to `/login/oauth2/code/okta`.
5. `authenticationSuccessHandler()` checks whether the logged-in email is authorized through `UserService.isAuthorizedEmail(...)`.
6. If the email is missing from the local database, the user is redirected to `/unauthorized-user`.
7. If authorized, the request continues to the controller layer.

### High-level layers

| Layer | Main Classes | Responsibility |
|---|---|---|
| App entry | `RaceTrackApplication` | Starts Spring Boot |
| Security | `SecurityConfig` | OIDC login/logout, session-expiry handling, route protection |
| MVC controllers | `HomeController`, `RunningLogController`, `WorkoutLogController` | Page rendering and form submission |
| REST controllers | `LogApiController`, `AdminApiController` | Spreadsheet editing APIs and coach admin APIs |
| Services | `UserService`, `RunningLogService`, `WorkoutLogService`, `AdminService` | Business rules and validation |
| Integration | `OktaAdminClient` | Okta management API helper, not active in current admin flow |
| Repositories | `UserRepository`, `RunningLogRepository`, `WorkoutLogRepository` | Database access |
| Entities | `User`, `RunningLog`, `WorkoutLog` | JPA domain model |

---

## 5. Authorization and Roles

### Access control model

RaceTrack currently uses a two-step access model:

1. Okta authenticates identity.
2. RaceTrack authorizes by email against the local `users` table.

If the email does not exist in `users`, access is denied even if Okta authentication succeeded.

### Supported roles

- `athlete`
- `coach`

### Athlete capabilities

Athletes can:

- submit running logs
- submit workout logs
- edit their own running rows
- edit their own workout rows
- delete their own rows
- view coach comments on their rows
- view other athletes' logs in read-only mode through the footer athlete picker

Athletes cannot:

- edit another athlete's row
- delete another athlete's row
- save coach comments
- use coach-only admin APIs

### Coach capabilities

Coaches can:

- view any athlete's running and workout sheets
- add and edit coach comments on any row
- create local RaceTrack users
- edit local RaceTrack users
- delete local RaceTrack users
- clear all running and workout log data while preserving users

Coaches cannot:

- submit athlete forms
- edit athlete-entered running/workout fields
- delete athlete rows through the spreadsheet UI

---

## 6. Backend Classes

### `edu.carroll.racetrack.config`

#### `SecurityConfig`

Responsibilities:

- requires authentication for all routes
- configures Okta login
- configures OIDC logout to `/login?logout`
- redirects invalid sessions back through Okta login
- marks session-expiry events caused by invalid or missing CSRF tokens

### `edu.carroll.racetrack.controller`

#### `HomeController`

- serves `GET /`
- loads the authorized user
- determines athlete vs coach view
- injects athlete lists and manageable user lists
- returns `home.html` for athletes and `home_coach.html` for coaches

#### `RunningLogController`

- handles `POST /running-log`
- binds the running form
- sets the authenticated user as owner
- delegates to `RunningLogService`

#### `WorkoutLogController`

- handles `POST /workout-log`
- binds the workout form
- sets the authenticated user as owner
- delegates to `WorkoutLogService`

#### `LogApiController`

Handles spreadsheet APIs for:

- loading running logs
- loading workout logs
- updating athlete-owned running rows
- updating athlete-owned workout rows
- deleting athlete-owned rows
- saving coach comments

Request records:

- `RunningLogUpdateRequest`
- `WorkoutLogUpdateRequest`

#### `AdminApiController`

Coach-only APIs:

- `POST /api/admin/users`
- `PUT /api/admin/users/{userId}`
- `DELETE /api/admin/users/{userId}`
- `DELETE /api/admin/data`

These APIs operate through `AdminService`, which currently changes local RaceTrack data only.

Request validation currently enforces:

- non-blank `firstName`
- non-blank `lastName`
- non-blank `email`
- valid email format
- non-blank `role`

### `edu.carroll.racetrack.service`

#### `UserService`

Main responsibilities:

- authorize OIDC users by email
- load athlete lists and all-user lists
- determine coach role
- provide display names

Current behavior:

- if an email is not found locally, `403 FORBIDDEN` is returned
- on home-page resolution, stored metadata like email or full name may be refreshed

#### `RunningLogService`

Responsibilities:

- validate running input
- save new running logs
- return logs by user
- update athlete-owned rows
- delete athlete-owned rows
- save coach comments

Current validation includes:

- mileage >= 0
- hurting required
- hurting details required when hurting is `true`
- sleep between 0 and 24
- stress between 1 and 10
- plate proportion required
- got that bread required
- feel required, max 100 chars
- RPE between 0 and 10
- details required, max 2000 chars

#### `WorkoutLogService`

Responsibilities:

- validate workout input
- save new workout logs
- return logs by user
- update athlete-owned rows
- delete athlete-owned rows
- save coach comments

Current validation includes:

- workout type must be `Strength`, `Strides`, or `Workout`
- completion details required
- actual paces required
- workout description required
- text fields max 2000 chars

#### `AdminService`

Responsibilities:

- create local users
- update local users
- delete local users and their owned logs
- clear all running/workout logs while preserving users

Current behavior:

- user creation relies on a database sequence for `users.id`
- user deletion removes local user data and owned logs
- it does not currently call `OktaAdminClient`

#### `OktaAdminClient`

This component contains methods for:

- creating Okta users
- updating Okta users
- deleting Okta users

It is currently an available integration helper only. It is not part of the active admin execution path unless the service layer is changed to use it.

### `edu.carroll.racetrack.repository`

#### `UserRepository`

Used for:

- case-insensitive email lookup
- duplicate-email checks
- ordered athlete list loading
- ordered all-user list loading

#### `RunningLogRepository`

Used for:

- loading running logs by user
- ownership-scoped running-log lookup
- deleting running logs by user

#### `WorkoutLogRepository`

Used for:

- loading workout logs by user
- ownership-scoped workout-log lookup
- deleting workout logs by user

---

## 7. Data Model

### `User`

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key, sequence-backed |
| `email` | `String` | Authorization lookup key |
| `fullName` | `String` | Displayed in UI |
| `role` | `String` | `athlete` or `coach` |

### `RunningLog`

| Field | Type |
|---|---|
| `id` | `Long` |
| `user` | `User` |
| `mileage` | `Double` |
| `hurting` | `Boolean` |
| `painDetails` | `String` |
| `sleepHours` | `Double` |
| `stressLevel` | `Integer` |
| `plateProportion` | `Boolean` |
| `gotThatBread` | `Boolean` |
| `feel` | `String` |
| `rpe` | `Integer` |
| `details` | `String` |
| `coachComment` | `String` |
| `logDate` | `LocalDateTime` |

### `WorkoutLog`

| Field | Type |
|---|---|
| `id` | `Long` |
| `user` | `User` |
| `workoutType` | `String` |
| `completionDetails` | `String` |
| `workoutDescription` | `String` |
| `actualPaces` | `String` |
| `coachComment` | `String` |
| `logDate` | `LocalDateTime` |

Relationships:

- one `User` owns many `RunningLog` rows
- one `User` owns many `WorkoutLog` rows

---

## 8. Frontend Structure

### Templates

| Template | Purpose |
|---|---|
| `home.html` | athlete home page |
| `home_coach.html` | coach home page |
| `header.html` | shared header fragment |
| `footer.html` | shared athlete picker/footer fragment |
| `running_form.html` | athlete running form |
| `workout_form.html` | athlete workout form |
| `running_sheet.html` | running spreadsheet shell |
| `workout_sheet.html` | workout spreadsheet shell |
| `unauthorized_user.html` | shown when authenticated email is not in local DB |

### JavaScript

| File | Current purpose |
|---|---|
| `home.js` | default form mode, athlete picker, footer menus, coach admin modal flows, notices |
| `sheets.js` | loading/rendering logs, date filters, row save/delete, coach comments, spreadsheet layout |
| `input-constraints.js` | textarea character counters |
| `date-inputs.js` | date-input defaults and native picker helpers |
| `tooltips.js` | Bootstrap tooltip initialization |

### Current UI behavior to know

- athletes land on the running form by default
- coaches land on an empty state until they select an athlete
- footer athlete selection is available in both role views
- date filters are `Today`, `Week`, `Month`, and `Custom`
- the `How Feel?` running-sheet column is no longer color-coded
- coach pages now use the same mobile viewport setup as athlete pages

---

## 9. Project Structure

```text
src/
  main/
    java/edu/carroll/racetrack/
      RaceTrackApplication.java
      config/
        SecurityConfig.java
      controller/
        AdminApiController.java
        HomeController.java
        LogApiController.java
        RunningLogController.java
        WorkoutLogController.java
      model/
        RunningLog.java
        User.java
        WorkoutLog.java
      repository/
        RunningLogRepository.java
        UserRepository.java
        WorkoutLogRepository.java
      service/
        AdminService.java
        OktaAdminClient.java
        RunningLogService.java
        UserService.java
        WorkoutLogService.java
    resources/
      application.yaml
      static/
      templates/
  test/
    java/edu/carroll/racetrack/
    resources/
      application-test.yaml
documentation/
  Deployment_Manual.md
  Developer_Manual.md
  UML_Diagrams.puml
  User_Manual.md
```

---

## 10. Local Development Setup

### Prerequisites

- Java 25
- PostgreSQL
- Git
- access to an Okta OIDC app integration

### Clone

```bash
git clone https://github.com/zgacnik-carroll/RaceTrack.git
cd RaceTrack
```

### Database

**IMPORTANT: In the step below, be sure to replace the password
value with a legitimate real password (keep that password stored
somewhere safe for future reference).**

```sql
CREATE DATABASE racetrack;
CREATE USER racetrackuser WITH ENCRYPTED PASSWORD 'replace_me';
GRANT ALL PRIVILEGES ON DATABASE racetrack TO racetrackuser;
```

Also grant schema rights for first-run Hibernate table creation:

```sql
\c racetrack
GRANT USAGE, CREATE ON SCHEMA public TO racetrackuser;
ALTER SCHEMA public OWNER TO racetrackuser;
```

### Run locally

```bash
./gradlew bootRun
```

or on Windows:

```powershell
.\gradlew.bat bootRun
```

---

## 11. Very Important: Okta OIDC Values IT Must Replace

This section must be followed carefully for any non-Carroll environment.

### Current code contains environment-specific Okta values

The current `src/main/resources/application.yaml` contains Okta values tied to the current environment.

IT must replace these values before production use with their own tenant and app integration values.

### Replace these exact keys

In `src/main/resources/application.yaml`, replace:

| Key | What IT must supply |
|---|---|
| `spring.security.oauth2.client.registration.okta.client-id` | the new Okta OIDC client ID |
| `spring.security.oauth2.client.registration.okta.client-secret` | the new Okta OIDC client secret |
| `spring.security.oauth2.client.provider.okta.issuer-uri` | the new issuer URI, usually `https://<YOUR_OKTA_DOMAIN>/oauth2/default` or your chosen authorization server |
| `okta.management.org-url` | the new Okta org base URL if management API use is needed |
| `okta.management.api-token` | the new Okta API token if management API use is needed |

### Recommended approach

Do not keep real secrets hardcoded in source-controlled files.

Instead:

1. remove hardcoded real values from `application.yaml`
2. replace them with environment-variable lookups
3. inject real values through the VM or deployment environment

Recommended pattern:

```yaml
spring:
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
            issuer-uri: ${OKTA_ISSUER_URI}

okta:
  management:
    org-url: ${OKTA_ORG_URL:}
    api-token: ${OKTA_API_TOKEN:}
```

### Okta app settings IT must configure

The Okta app must be a Web OIDC app using Authorization Code flow.

At minimum, IT must configure:

- sign-in redirect URI
- sign-out redirect URI
- client ID
- client secret
- issuer URI / authorization server
- scopes: `openid`, `profile`, `email`

### Redirect URI examples

For local development:

```text
http://localhost:8080/login/oauth2/code/okta
http://localhost:8080/login?logout
```

For direct VM access on port `8080`:

```text
http://your-hostname:8080/login/oauth2/code/okta
http://your-hostname:8080/login?logout
```

For reverse-proxy access:

```text
https://your-hostname/login/oauth2/code/okta
https://your-hostname/login?logout
```

Host, scheme, and port must match exactly.

### Extremely important operational note

Okta authentication alone does not grant RaceTrack access.

The signed-in email must also already exist in RaceTrack's `users` table.

So IT or a coach must:

- create the local RaceTrack user record before first app access
- ensure the Okta-authenticated email matches the local `users.email` value

If the email does not match, the user will authenticate with Okta and still be rejected by RaceTrack.

---

## 12. Testing and Verification

Useful commands:

```bash
./gradlew test
./gradlew bootRun
```

After startup, verify:

- Okta login redirects correctly
- unauthorized users are sent to `/unauthorized-user`
- an existing athlete can submit and edit their own logs
- an athlete can view another athlete's logs read-only
- a coach can save coach comments
- a coach can create, edit, delete users, and clear log data

