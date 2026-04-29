<div align="center">

# RaceTrack

**Training log management for Carroll College Cross Country**

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-6db33f?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-336791?style=flat-square&logo=postgresql)
![Gradle](https://img.shields.io/badge/Gradle-9.3.0-02303a?style=flat-square&logo=gradle)
![Okta](https://img.shields.io/badge/Auth-Okta_OIDC-00297a?style=flat-square&logo=okta)

</div>

---

## What is RaceTrack?

RaceTrack is a web application that lets Carroll College's cross country athletes log their daily training, and gives coaches a live view of the entire roster in one place.

**Athletes** submit daily running logs (mileage, wellness, RPE, notes) and workout logs (type, paces, completion details), then review and edit their history in an inline spreadsheet view.

**Coaches** can review any athlete's logs, filter by date range, and leave comments directly on individual rows without spreadsheet exports or shared documents.

All access is gated behind **Okta SSO**. Okta handles authentication, and RaceTrack authorizes access only when the signed-in email already exists in the local `users` table.

---

## Features

**For athletes**
- Submit daily running logs — mileage, injury status, sleep, stress, nutrition, feel, RPE, and comments
- Submit workout logs — type (Strength / Strides / Workout), completion details, actual paces, and description
- Review personal log history in a sortable, filterable spreadsheet view
- Edit and delete own entries inline without leaving the page
- Date filters: Today · Week · Month · Custom

**For coaches**
- View the full roster's running and workout logs from a single page
- Select any athlete from the footer to load their logs instantly
- Filter logs by date range across any athlete
- Add and edit coach comments on any log row inline
- Search athletes by name in the footer

---

## Tech Stack

| Layer | Technology                                 |
|---|--------------------------------------------|
| Language | Java 25                                    |
| Framework | Spring Boot 4.0.2                          |
| View Layer | Thymeleaf (server-rendered)                |
| Frontend | Bootstrap 5.3 + Vanilla JS                 |
| Database | PostgreSQL 15+                            |
| ORM | Spring Data JPA / Hibernate                |
| Authentication | Okta OIDC via Spring Security OAuth2 Login |
| Build | Gradle                                     |

---

## Getting Started

### Prerequisites

- Java 25
- Gradle 9.3.0 *(or use the included `./gradlew` wrapper — no install needed)*
- PostgreSQL 15+
- Okta OIDC credentials — client ID, client secret, and issuer URI

### 1. Clone

```bash
git clone https://github.com/zgacnik-carroll/RaceTrack.git
cd RaceTrack
```

### 2. Create the database

#### IMPORTANT: Replace 'your_password' with a safe, unique password.

```sql
CREATE DATABASE racetrack;
CREATE USER racetrackuser WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE racetrack TO racetrackuser;
```

Then grant schema rights for first-run Hibernate table creation:

```sql
\c racetrack
GRANT USAGE, CREATE ON SCHEMA public TO racetrackuser;
ALTER SCHEMA public OWNER TO racetrackuser;
```

### 3. Configure `application.yaml`

Edit `src/main/resources/application.yaml` and fill in your database credentials and Okta settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/racetrack
    username: racetrackuser
    password: your_password
  security:
    oauth2:
      client:
        registration:
          okta:
            client-id: ${OKTA_CLIENT_ID}
            client-secret: ${OKTA_CLIENT_SECRET}
        provider:
          okta:
            issuer-uri: ${OKTA_ISSUER_URI}
```

> For an older PostgreSQL database created before `User.id` was converted to sequence-backed numeric ids, run the one-time migration in [`documentation/Deployment_Manual.md`](./documentation/Deployment_Manual.md) before startup.
>
> ⚠️ Never commit `client-secret` in plaintext. Use environment variables or a gitignored local override file.

### 4. Run

```bash
./gradlew bootRun
```

Then open [http://localhost:8080](http://localhost:8080) — you'll be redirected to the Okta login page automatically.

---

## Roles

| Role | How assigned | Access |
|---|---|---|
| `athlete` | Coach/admin creates local RaceTrack user | Submit, view, edit, and delete own logs |
| `coach` | Coach/admin creates or updates local RaceTrack user role | View all athletes, add coach comments, and use admin tools |

To promote an existing local user to coach:

```sql
UPDATE users SET role = 'coach' WHERE email = 'coach@carrollu.edu';
```

---

## Running Tests

Tests use an in-memory H2 database and the `test` Spring profile from `src/test/resources/application-test.yaml` — no additional setup required.

```bash
./gradlew test
```

---

## Project Structure

```
src/
└── main/
    ├── java/edu/carroll/racetrack/
    │   ├── config/          # SecurityConfig (Okta OAuth2 + logout)
    │   ├── controller/      # MVC page controllers and REST API controllers
    │   ├── model/           # User, RunningLog, WorkoutLog
    │   ├── repository/      # JPA repositories
    │   └── service/         # UserService, RunningLogService, WorkoutLogService,
    │                        # AdminService, OktaAdminClient
    └── resources/
        ├── application.yaml
        ├── static/          # JavaScript and static assets
        └── templates/       # Thymeleaf templates + fragments
            ├── home.html          # Athlete view
            ├── home_coach.html    # Coach view
            ├── running_form.html
            ├── workout_form.html
            ├── running_sheet.html
            └── workout_sheet.html
```

---

## Documentation

Full developer manual — including architecture diagrams, authentication flow, database setup, PostgreSQL migration notes, deployment guidance, and pre-production checklist — is available in [`Developer_Manual.md`](./documentation/Developer_Manual.md).

Full user manual - including all the detailed uses of our web application are available at [User_Manual.md](./documentation/User_Manual.md)

---

<div align="center">
  <sub>Built for Carroll College Cross Country · Go Saints! </sub>
</div>
