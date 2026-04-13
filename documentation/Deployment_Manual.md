# RaceTrack Deployment Manual

## Overview

This document describes how to deploy RaceTrack to a Linux virtual machine over SSH.

The application is a Spring Boot web app backed by PostgreSQL and authenticated through Okta OIDC. Deployment consists of:

- Installing system dependencies
- Configuring PostgreSQL
- Building the application JAR
- Configuring runtime environment variables
- Verifying Okta connectivity and redirect URIs
- Starting the application
- Seeding the initial coach record
- Running the application as a `systemd` service

This guide assumes:

- The VM is Ubuntu or another Debian-based Linux distribution
- You have SSH access to the VM
- You have permission to edit the Okta client configuration
- You want to run the app on port `8080`

## Architecture Summary

RaceTrack requires the following runtime components:

- Java 25
- PostgreSQL 16+
- Network access to Okta over HTTPS
- DNS resolution for the Okta issuer host

Optional but recommended for production:

- Nginx as a reverse proxy
- HTTPS with a real hostname
- A `systemd` service for automatic startup and restarts

## 1. Prerequisites

Install required packages:

```bash
sudo apt update
sudo apt install -y git postgresql postgresql-contrib nginx unzip
```

Install Java 25 and verify:

```bash
sudo apt install -y openjdk-25-jdk
java -version
```

The Java version should report `25.x`.

## 2. Clone the Repository

Clone the source code to the VM:

```bash
git clone https://github.com/zgacnik-carroll/RaceTrack.git
cd ~/RaceTrack
chmod +x gradlew
```

## 3. Create the PostgreSQL Database

Open PostgreSQL as the `postgres` superuser:

```bash
sudo -u postgres psql
```

Create the application database and database user:

```sql
CREATE DATABASE racetrack;
CREATE USER racetrackuser WITH ENCRYPTED PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE racetrack TO racetrackuser;
\q
```

## 4. Grant Schema Permissions

RaceTrack uses Hibernate schema creation on first startup. The database user must be able to create tables in the `public` schema.

Open PostgreSQL again:

```bash
sudo -u postgres psql -d racetrack
```

Run:

```sql
GRANT USAGE, CREATE ON SCHEMA public TO racetrackuser;
ALTER SCHEMA public OWNER TO racetrackuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO racetrackuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO racetrackuser;
\q
```

Without this step, first startup will fail with:

```text
ERROR: permission denied for schema public
```

## 5. Configure Runtime Environment Variables

Create a directory for runtime configuration:

```bash
sudo mkdir -p /etc/racetrack
```

Create an environment file:

```bash
sudo nano /etc/racetrack/racetrack.env
```

Add the following values (FILL IN CARROLL OKTA INFO, DON'T FULL COPY AND PASTE):

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/racetrack
SPRING_DATASOURCE_USERNAME=racetrackuser
SPRING_DATASOURCE_PASSWORD=replace_with_strong_password

SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OKTA_CLIENT_ID=carroll_okta_client_id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OKTA_CLIENT_SECRET=carroll_okta_client_secret
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OKTA_ISSUER_URI=<insert Carroll Okta domain here>

SERVER_PORT=8080
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
```

Do not store production secrets directly in source-controlled config files.

## 6. Verify DNS and Okta Connectivity

Before starting the app, verify the VM can resolve and reach Okta.

Check resolver state:

```bash
resolvectl status
```

Test DNS and HTTPS:

```bash
getent hosts google.com
getent hosts <insert Carroll Okta domain here>
curl -I <insert Carroll Okta domain here>
```

If the VM cannot resolve external hosts, apply a temporary DNS override on the active interface. Example:

```bash
sudo resolvectl dns ens33 1.1.1.1 8.8.8.8
sudo resolvectl domain ens33 ~.
```

Replace `ens33` with the correct interface for your VM.

Re-test:

```bash
getent hosts integrator-9628955.okta.com
curl -I https://integrator-9628955.okta.com
```

If needed, make the DNS configuration persistent via netplan.

Example netplan section:

```yaml
network:
  version: 2
  ethernets:
    ens33:
      dhcp4: true
      nameservers:
        addresses:
          - 10.39.1.3
          - 1.1.1.1
          - 8.8.8.8
```

Apply changes:

```bash
sudo netplan apply
```

## 7. Build the Application

Build the runnable Spring Boot JAR:

```bash
cd ~/RaceTrack
./gradlew clean bootJar
ls build/libs
```

You should see a file similar to:

```text
RaceTrack-0.0.1-SNAPSHOT.jar
```

## 8. Start the Application Manually for First Run

The first successful startup allows Hibernate to create the application tables.

Load the environment and start the JAR:

```bash
set -a
source /etc/racetrack/racetrack.env
set +a
java -jar build/libs/RaceTrack-0.0.1-SNAPSHOT.jar
```

Replace the JAR name if your build output differs.

Watch for successful startup. The application should:

- Connect to PostgreSQL
- Resolve the Okta issuer
- Bind to port `8080`

If startup fails because port `8080` is already in use, identify the process:

```bash
sudo ss -ltnp | grep 8080
ps -ef | grep java
```

Stop the conflicting process, then restart the JAR.

## 9. Verify Database Tables

In a second SSH session, verify the schema was created:

```bash
sudo -u postgres psql -d racetrack -c "\dt"
```

You should see at least:

- `users`
- `running_logs`
- `workout_logs`

## 10. Seed the Initial Coach Account

After the `users` table exists, create the first coach account.

Open PostgreSQL:

```bash
sudo -u postgres psql -d racetrack
```

Insert the record:

```sql
INSERT INTO users (id, email, full_name, role)
VALUES (
  'coach-shannon-flynn',
  'sflynn@carroll.edu',
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

This step is required because RaceTrack now authorizes access by email presence in the local `users` table.

## 11. Configure Okta Redirect URIs for First Direct Access

The Okta client must allow the exact URLs used by the deployed app.

If you are testing the app directly on port `8080` before Nginx is configured, and users access the app at:

```text
http://racetrack.carroll.edu:8080
```

then the Okta application must include:

### Sign-in redirect URI

```text
http://racetrack.carroll.edu:8080/login/oauth2/code/okta
```

### Sign-out redirect URI

```text
http://racetrack.carroll.edu:8080/login?logout
```

Host, port, and scheme must match exactly.

If Okta rejects login or logout, errors will typically mention:

- `redirect_uri`
- `post_logout_redirect_uri`

Both must be present in the Okta app configuration.

## 12. Test Application Access on Port 8080

Open the application in a browser:

```text
http://racetrack.carroll.edu:8080
```

Then:

- Sign in through Okta as `sflynn@carroll.edu`
- Confirm the user lands in the app
- Confirm the user has coach access

## 13. Create a systemd Service

After manual startup works correctly, configure RaceTrack as a managed service.

Determine the Java path:

```bash
which java
whoami
ls ~/RaceTrack/build/libs
```

Create the service file:

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

Reload and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable racetrack
sudo systemctl start racetrack
sudo systemctl status racetrack
```

Tail logs:

```bash
journalctl -u racetrack -f
```

## 14. Configure Nginx for racetrack.carroll.edu

To remove `:8080` from the browser URL, place Nginx in front of the application.

In this setup:

- RaceTrack continues listening on `127.0.0.1:8080`
- Nginx listens on port `80`
- Users browse to `http://racetrack.carroll.edu`

Install Nginx if needed:

```bash
sudo apt update
sudo apt install -y nginx
```

Create the site config:

```bash
sudo tee /etc/nginx/sites-available/racetrack > /dev/null <<'EOF'
server {
    listen 80;
    server_name racetrack.carroll.edu;

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

Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/racetrack /etc/nginx/sites-enabled/racetrack
```

Optionally remove the default site:

```bash
sudo rm -f /etc/nginx/sites-enabled/default
```

Test and reload Nginx:

```bash
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl reload nginx
```

Verify locally from the VM:

```bash
curl -I http://racetrack.carroll.edu
```

## 15. Update Okta Redirect URIs for Nginx

After Nginx is in place, update the Okta app to use the no-port hostname.

The Okta application must include:

### Sign-in redirect URI

```text
http://racetrack.carroll.edu/login/oauth2/code/okta
```

### Sign-out redirect URI

```text
http://racetrack.carroll.edu/login?logout
```

These values must match exactly.

After updating Okta, test the app at:

```text
http://racetrack.carroll.edu
```

If you later add HTTPS, update the Okta redirect URIs to use `https://`.

## 16. Firewall

If `ufw` is enabled:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
sudo ufw status
```

If you are not using Nginx and are exposing the app directly, allow `8080` instead.

## 17. Day-2 Operations

### Rebuild and redeploy after code changes

```bash
cd ~/RaceTrack
git pull
./gradlew clean bootJar
sudo systemctl restart racetrack
sudo systemctl status racetrack
```

### Follow service logs

```bash
journalctl -u racetrack -f
```

### Check whether the app is listening

```bash
sudo ss -ltnp | grep 8080
```

### Check whether the seeded coach exists

```bash
sudo -u postgres psql -d racetrack -c "SELECT id, email, full_name, role FROM users;"
```

## 18. Troubleshooting

### Error: `permission denied for schema public`

Fix PostgreSQL schema permissions:

```bash
sudo -u postgres psql -d racetrack
```

```sql
GRANT USAGE, CREATE ON SCHEMA public TO racetrackuser;
ALTER SCHEMA public OWNER TO racetrackuser;
```

### Error: `UnknownHostException` for Okta issuer

The VM cannot resolve external DNS. Verify resolver state:

```bash
resolvectl status
getent hosts integrator-9628955.okta.com
```

Set working DNS servers:

```bash
sudo resolvectl dns ens33 1.1.1.1 8.8.8.8
sudo resolvectl domain ens33 ~.
```

### Error: `redirect_uri` is invalid

Add the exact login redirect URI that matches how users access the app.

Direct app access on port `8080`:

```text
http://racetrack.carroll.edu:8080/login/oauth2/code/okta
```

Nginx access on port `80`:

```text
http://racetrack.carroll.edu/login/oauth2/code/okta
```

### Error: `post_logout_redirect_uri` is invalid

Add the exact logout redirect URI that matches how users access the app.

Direct app access on port `8080`:

```text
http://racetrack.carroll.edu:8080/login?logout
```

Nginx access on port `80`:

```text
http://racetrack.carroll.edu/login?logout
```

### Error: `Port 8080 was already in use`

Identify the process:

```bash
sudo ss -ltnp | grep 8080
ps -ef | grep java
```

Kill the conflicting process, then restart the app.

If you used `./gradlew bootRun`, stop that terminal with `Ctrl+C` before killing only the Java child. For production use, prefer:

```bash
java -jar build/libs/RaceTrack-0.0.1-SNAPSHOT.jar
```

## 19. Recommended Production Improvements

For a more production-ready deployment:

- Remove hardcoded Okta values from source-controlled config
- Use a stable hostname rather than an IP
- Put the app behind Nginx
- Enable HTTPS
- Keep DNS configuration persistent
- Run only the built JAR in production, not `bootRun`
- Consider tightening Hibernate schema management after the schema is stable
