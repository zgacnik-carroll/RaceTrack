# RaceTrack Deployment Manual

## Overview

This guide describes how to deploy the current RaceTrack application to a Linux VM.

The app is a Spring Boot service backed by PostgreSQL and authenticated through Okta OIDC.

Current deployment responsibilities include:

- installing Java and PostgreSQL
- building the Spring Boot JAR
- configuring environment variables
- replacing all Okta-specific values with your own tenant and client settings
- creating local RaceTrack users so those emails are authorized after Okta login
- running the app under `systemd`
- optionally putting Nginx in front of the app

Important current behavior:

- RaceTrack does not auto-provision users after Okta login
- authentication is done by Okta, but authorization is done by local email lookup in the `users` table
- the coach UI can manage RaceTrack user records locally
- those admin actions do not currently create or update actual Okta users

---

## 1. Runtime Requirements

- Ubuntu or another Debian-based Linux distribution
- SSH access to the VM
- Java 25
- PostgreSQL
- outbound HTTPS connectivity to your Okta issuer
- permission to configure the Okta application

Optional but recommended:

- Nginx reverse proxy
- HTTPS with a real hostname
- `systemd` service

---

## 2. Install Dependencies

```bash
sudo apt update
sudo apt install -y git postgresql postgresql-contrib nginx unzip
sudo apt install -y openjdk-25-jdk
java -version
```

The Java version should report `25.0.2`.

---

## 3. Clone the Application

```bash
git clone https://github.com/zgacnik-carroll/RaceTrack.git
cd ~/RaceTrack
chmod +x gradlew
```

---

## 4. Create the Database

```bash
sudo -u postgres psql
```

**IMPORTANT: In the step below, be sure to replace the password
value with a legitimate real password (keep that password stored
somewhere safe for future reference).**

```sql
CREATE DATABASE racetrack;
CREATE USER racetrackuser WITH ENCRYPTED PASSWORD 'replace_with_strong_password';
GRANT ALL PRIVILEGES ON DATABASE racetrack TO racetrackuser;
\q
```

Grant schema rights required for first-run Hibernate table creation:

```bash
sudo -u postgres psql -d racetrack
```

```sql
GRANT USAGE, CREATE ON SCHEMA public TO racetrackuser;
ALTER SCHEMA public OWNER TO racetrackuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO racetrackuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO racetrackuser;
\q
```

---

## 5. Replace Okta Values Before First Startup

This step is mandatory.

Do not deploy the application with environment-specific Okta values from development.

### Values that must be replaced

RaceTrack expects these logical values:

- Okta OIDC client ID
- Okta OIDC client secret
- Okta issuer URI
- optional Okta management org URL
- optional Okta management API token

### Recommended deployment method

Set them through an environment file instead of hardcoding them in source.

Create the runtime config directory:

```bash
sudo mkdir -p /etc/racetrack
sudo nano /etc/racetrack/racetrack.env
```

Add:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/racetrack
SPRING_DATASOURCE_USERNAME=racetrackuser
SPRING_DATASOURCE_PASSWORD=replace_with_strong_password

SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OKTA_CLIENT_ID=<REPLACE_WITH_YOUR_OKTA_CLIENT_ID>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OKTA_CLIENT_SECRET=<REPLACE_WITH_YOUR_OKTA_CLIENT_SECRET>
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OKTA_ISSUER_URI=https://<REPLACE_WITH_YOUR_OKTA_DOMAIN>/oauth2/default

OKTA_ORG_URL=https://<REPLACE_WITH_YOUR_OKTA_DOMAIN>
OKTA_API_TOKEN=<REPLACE_WITH_YOUR_OKTA_API_TOKEN_IF_USED>

SERVER_PORT=8080
SERVER_FORWARD_HEADERS_STRATEGY=framework
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
```

Notes:

- if you are not using Okta management API features, `OKTA_ORG_URL` and `OKTA_API_TOKEN` can be omitted
- the current active admin flow does not call `OktaAdminClient`
- the OIDC login values are still required

---

## 6. Configure the Okta App Integration

Your Okta app must be a Web OIDC application using Authorization Code flow.

Required scopes:

- `openid`
- `profile`
- `email`

### Redirect URIs for direct VM testing

If users will access the app directly on port `8080`, add:

```text
http://your-hostname:8080/login/oauth2/code/okta
http://your-hostname:8080/login?logout
```

### Redirect URIs for reverse proxy access

If users will access the app through Nginx without `:8080`, add:

```text
http://your-hostname/login/oauth2/code/okta
http://your-hostname/login?logout
```

If HTTPS is enabled:

```text
https://your-hostname/login/oauth2/code/okta
https://your-hostname/login?logout
```

Host, scheme, and port must match exactly.

---

## 7. Verify DNS and Okta Connectivity

Before starting the app:

```bash
resolvectl status
getent hosts google.com
getent hosts <your-okta-domain>
curl -I https://<your-okta-domain>
```

If DNS fails, apply a temporary resolver override on the correct interface:

```bash
sudo resolvectl dns ens33 1.1.1.1 8.8.8.8
sudo resolvectl domain ens33 ~.
```

Then retest:

```bash
getent hosts <your-okta-domain>
curl -I https://<your-okta-domain>
```

---

## 8. Build the Application

```bash
cd ~/RaceTrack
./gradlew clean bootJar
ls build/libs
```

You should see a JAR similar to:

```text
RaceTrack-0.0.1-SNAPSHOT.jar
```

---

## 9. First Manual Startup

```bash
cd ~/RaceTrack
set -a
source /etc/racetrack/racetrack.env
set +a
java -jar build/libs/RaceTrack-0.0.1-SNAPSHOT.jar
```

Verify that startup succeeds and the app:

- connects to PostgreSQL
- resolves the Okta issuer
- binds to port `8080`

If port `8080` is already in use:

```bash
sudo ss -ltnp | grep 8080
ps -ef | grep java
```

---

## 10. Verify Tables

```bash
sudo -u postgres psql -d racetrack -c "\dt"
```

You should see at least:

- `users`
- `running_logs`
- `workout_logs`

---

## 11. Seed an Initial Authorized User

This step is required because RaceTrack authorizes by local email lookup.

Create at least one coach user before testing login:

```bash
sudo -u postgres psql -d racetrack
```

```sql
INSERT INTO users (id, email, full_name, role)
VALUES (
  '1',
  'sflynn@example.com',
  'Shannon Flynn',
  'coach'
)
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role;
\q
```

Verify:

```bash
sudo -u postgres psql -d racetrack -c "SELECT id, email, full_name, role FROM users;"
```

The Okta-authenticated email must match `users.email`.

---

## 12. Test the Application

Open the app:

```text
http://your-hostname:8080
```

Verify:

- Okta login succeeds
- the signed-in email matches a local RaceTrack user
- the coach lands on the coach home page
- the user can see athlete selection in the footer

If login succeeds at Okta but the app shows "Access to RaceTrack was not approved," the email is missing from the local `users` table.

---

## 13. Create a systemd Service

Create:

```bash
sudo nano /etc/systemd/system/racetrack.service
```

Use:

```ini
[Unit]
Description=RaceTrack Spring Boot App
After=network.target postgresql.service

[Service]
User=csadmin
WorkingDirectory=/home/csadmin/RaceTrack
EnvironmentFile=/etc/racetrack/racetrack.env
ExecStart=/usr/bin/java -jar /home/csadmin/RaceTrack/build/libs/RaceTrack-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Adjust:

- `User`
- `WorkingDirectory`
- `ExecStart`
- JAR filename

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable racetrack
sudo systemctl start racetrack
sudo systemctl status racetrack
```

Logs:

```bash
journalctl -u racetrack -f
```

---

## 14. Optional: Put Nginx in Front

Create:

```bash
sudo tee /etc/nginx/sites-available/racetrack > /dev/null <<'EOF'
server {
    listen 80;
    server_name your-hostname;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
    }
}
EOF
```

Enable:

```bash
sudo ln -s /etc/nginx/sites-available/racetrack /etc/nginx/sites-enabled/racetrack
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl reload nginx
```

Keep:

```bash
SERVER_FORWARD_HEADERS_STRATEGY=framework
```

This is required so Spring builds correct Okta callback URLs behind a proxy.

---

## 15. Troubleshooting

### `permission denied for schema public`

Reapply schema permissions:

```bash
sudo -u postgres psql -d racetrack
```

```sql
GRANT USAGE, CREATE ON SCHEMA public TO racetrackuser;
ALTER SCHEMA public OWNER TO racetrackuser;
```

### `UnknownHostException` or issuer lookup failure

The VM cannot resolve your Okta domain.

Check:

```bash
resolvectl status
getent hosts <your-okta-domain>
```

### `redirect_uri` is invalid

Add the exact login callback URI used by the deployed hostname and scheme.

### `post_logout_redirect_uri` is invalid

Add the exact logout callback URI used by the deployed hostname and scheme.

### Login succeeds but RaceTrack denies access

The email is not present in the local `users` table, or it does not match exactly.

### Coach admin action created a RaceTrack user but the person still cannot sign in

RaceTrack local user creation does not automatically create or license an Okta user. Okta-side access must still exist separately.
