# User Manual
## RaceTrack

### Document Purpose
This manual explains what RaceTrack is, what users can do inside it, and how to navigate the app effectively.
It is focused on user capabilities and workflows, not technical implementation.

---

### 1. App Overview
RaceTrack is a daily training log platform used by athletes and coaches.

The app is built around two core ideas:
- Athletes should be able to record daily training and wellness data quickly.
- Coaches should be able to review recent athlete data in a clear, spreadsheet-like format and leave feedback.

RaceTrack supports two main data categories:
- Running data (daily run + wellness context)
- Workout data (session type + completion details)

---

### 2. Who Uses RaceTrack

#### Athlete Experience
Athletes use RaceTrack to:
- Enter daily logs through two forms
- Review recent entries in table format
- Edit previously entered rows
- Delete rows that are no longer needed
- View coach comments attached to entries

#### Coach Experience
Coaches use RaceTrack to:
- Find athletes quickly using search
- Select one athlete at a time for focused review
- Switch between running and workout sheets
- Review recent athlete trends
- Add or update comments on individual rows

---

### 3. Main Interface Areas

#### Header
The header provides:
- RaceTrack branding/title
- Current signed-in user display
- Logout action

Athlete-only controls also appear in the header for switching form views:
- All Forms
- Running Log
- Workout Log

#### Form Area
The form area is where athletes enter new information.

Athletes can access:
- Daily Running Log form
- Workout Log form

Coaches do not use form entry mode and focus on spreadsheet views.

#### Spreadsheet Area
The spreadsheet area is for reviewing and managing existing entries.

Available sheets:
- Running Sheet
- Workout Sheet

These sheets are designed for fast scanning and row-level updates.

#### Footer Navigation
The footer includes:
- Running Sheet button
- Workout Sheet button

Coach-only footer controls include:
- Athlete search bar
- Athlete selection buttons
- Clear Athlete action

---

### 4. Navigation Workflows

#### Athlete Navigation Flow
Typical athlete flow:
1. Sign in and land on form mode.
2. Choose `All Forms` to view both entry forms together, or switch to a single form.
3. Submit new daily entries.
4. Open `Running Sheet` or `Workout Sheet` from the footer to review recent logs.
5. Update a row and click Save, or remove a row with Delete if needed.

#### Coach Navigation Flow
Typical coach flow:
1. Sign in and land on coach review layout.
2. Search for an athlete by name/email or select from the athlete list.
3. Review running logs first (default athlete view).
4. Switch to workout logs using the footer.
5. Enter feedback in Coach Comment for specific rows and save.
6. Clear athlete selection to return to neutral state.

---

### 5. Forms and What They Capture

#### A. Daily Running Log
The Running Log captures daily running and wellness context, including:
- Date
- Mileage
- Hurting status
- Sleep hours
- Stress level
- Plate proportions (nutrition check)
- “Got that bread” (carbohydrate/fueling check)
- Overall feel
- RPE (effort rating)
- Comments/details

Why this matters for users:
- Athletes can document both workload and recovery indicators in one place.
- Coaches can read run output in context of readiness and wellness.

#### B. Workout Log
The Workout Log captures non-daily-run training details, including:
- Date
- Workout type
- Completion details
- Actual paces
- Workout description

Why this matters for users:
- Athletes can record what was planned vs what was completed.
- Coaches can evaluate session execution and quality over time.

---

### 6. Spreadsheet Views and Data Review

#### Running Sheet
Shows each running entry with fields such as:
- Date
- Miles
- Wellness indicators
- RPE
- Athlete details
- Coach comment

Athletes can:
- Edit own rows directly in table cells
- Save row updates
- Delete rows

Coaches can:
- View athlete running rows
- Add/update coach comments
- Use filtering for faster review

#### Workout Sheet
Shows each workout entry with fields such as:
- Date
- Workout type
- Completion details
- Actual paces
- Description
- Coach comment

Athletes can:
- Edit and save own rows
- Delete own rows

Coaches can:
- Review athlete workout history
- Add/update coach comments per row

---

### 7. Filtering and Recent Data Views
Both sheets support quick date-range filtering:
- Recent 60
- Today
- Week
- Month

How users benefit:
- `Recent 60` gives broad trend context.
- `Today` isolates current-day checks.
- `Week` helps with short-cycle monitoring.
- `Month` supports medium-term pattern review.

---

### 8. Role-Based Permissions (What Each Role Can and Cannot Do)

#### Athlete
Can:
- Submit running/workout forms
- View own logs
- Edit own log rows
- Delete own log rows
- View coach comments

Cannot:
- Access or modify other athletes’ logs
- Add coach comments as a coach action

#### Coach
Can:
- View athlete logs
- Switch athletes
- Add/update coach comments
- Use search and filtering for review workflows

Cannot:
- Edit athlete-entered data fields
- Delete athlete rows

---

### 9. On-Screen Status Messages
RaceTrack shows short notification banners for key actions, such as:
- Running log saved
- Workout log saved
- Row saved
- Row deleted
- Coach comment saved
- Error notices when a save/delete action fails

These messages confirm whether an action completed successfully.

---

### 10. Practical Usage Patterns

#### Pattern 1: Daily Athlete Check-In
- Open forms
- Enter running and/or workout data
- Submit
- Review row in sheet for confirmation

#### Pattern 2: End-of-Week Athlete Cleanup
- Open Running Sheet and Workout Sheet
- Filter to `Week`
- Update missing details
- Save corrected rows

#### Pattern 3: Coach Review Block
- Search/select athlete
- Open Running Sheet and filter to `Week` or `Month`
- Add comments on relevant rows
- Switch to Workout Sheet for session-level feedback

---

### 11. Best-Use Guidelines (User-Focused)
- Enter logs close to when training happens for better accuracy.
- Use comments/details fields to capture context (surface, weather, fatigue, etc.).
- Use sheet filters before reviewing to reduce noise.
- Save row changes immediately after editing.
- Coaches should leave row-level comments tied to specific entries for clearer follow-up.

---

### 12. Summary
RaceTrack provides:
- Two core submission forms (Running + Workout)
- Two spreadsheet-style review views (Running Sheet + Workout Sheet)
- Fast navigation for both athletes and coaches
- Role-based controls for clean ownership of data
- Practical filtering and row-level actions for day-to-day training management
