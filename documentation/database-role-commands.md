# PostgreSQL Role Switch Commands

Use these commands to open your RaceTrack database and switch a user's role between `coach` and `athlete`.

## 1. Open PostgreSQL

```bash
psql -h localhost -p 5432 -U racetrackuser -d racetrack
```

## 2. Find the user

```sql
SELECT id, email, role
FROM users
ORDER BY email;
```

## 3. Set user to coach

```sql
UPDATE users
SET role = 'coach'
WHERE email = 'jclaassen@carroll.edu';
```

## 4. Set user back to athlete

```sql
UPDATE users
SET role = 'athlete'
WHERE email = 'jclaassen@carroll.edu';
```

## 5. Verify the change

```sql
SELECT id, email, role
FROM users
WHERE email = 'jclaassen@carroll.edu';
```

## 6. Exit PostgreSQL

```sql
\q
```
