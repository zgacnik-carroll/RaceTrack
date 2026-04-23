# User Manual

## RaceTrack

*Note: This application is only accessible through the Carroll HaloNet WiFi*

*Once you are connected to HaloNet, navigate to [racetrack.carroll.edu](racetrack.carroll.edu)*


### Purpose

This manual explains how the current RaceTrack application works for athletes and coaches.

It reflects the current live behavior of the codebase, including:

- login and access approval
- athlete workflows
- coach review workflows
- coach admin tools
- spreadsheet editing and date filters

---

## 1. What RaceTrack Is

RaceTrack is a training log application for recording and reviewing:

- daily running logs
- workout logs

Athletes use RaceTrack to enter their own training and wellness data.

Coaches use RaceTrack to review athlete logs, leave comments, and manage authorized RaceTrack users.

---

## 2. Signing In and Access Approval

RaceTrack uses Okta for sign-in.

Important current behavior:

- signing in through Okta does not automatically give access to RaceTrack
- the signed-in email must already exist in RaceTrack's user database

If your email is not already listed in RaceTrack, you will be taken to the "Access to RaceTrack was not approved" page and asked to contact your coach.

---

## 3. Main Areas of the App

### Header

The header shows:

- the RaceTrack title
- whether you are in Athlete or Coach view
- the signed-in user name
- logout

Athletes also see header buttons for:

- `Running Log`
- `Workout Log`
- `My Running Sheet`
- `My Workout Sheet`

Coaches see header tools for:

- `Add User`
- `Edit User`
- `Delete User`
- `Clear Data`

### Form Area

The form area is where athletes submit new entries.

Athletes can open:

- the Daily Running Log form
- the Workout Log form

Coaches do not submit athlete forms.

### Spreadsheet Area

The spreadsheet area shows saved log rows.

Available sheets:

- Running Sheet
- Workout Sheet

### Footer

The footer contains:

- a search bar for athlete names or emails
- athlete buttons
- a popup menu to open a selected athlete's Running Sheet or Workout Sheet

Current behavior:

- coaches use the footer to select athletes for review
- athletes can also use the footer to open teammates' sheets in read-only mode

---

## 4. Athlete Experience

### Default landing view

Athletes land on the `Daily Running Log` form by default.

### Athlete actions

Athletes can:

- submit a new running log
- submit a new workout log
- open their own running and workout sheets
- edit their own sheet rows
- delete their own rows
- read coach comments on their rows
- view other athletes' sheets read-only from the footer

### What athletes cannot do

Athletes cannot:

- edit another athlete's row
- delete another athlete's row
- save coach comments
- use coach admin actions

---

## 5. Coach Experience

### Default landing view

Coaches land on a review layout with an empty state message until an athlete is selected.

### Coach review actions

Coaches can:

- select or search for an athlete from the footer
- open the athlete's Running Sheet
- open the athlete's Workout Sheet
- write and save coach comments on any row

### Coach admin actions

Coaches can also:

- add a user to RaceTrack
- edit a RaceTrack user
- delete a RaceTrack user
- clear all running and workout logs while preserving user accounts

Important note:

These actions manage RaceTrack access and RaceTrack data. They do not necessarily create or update the person's Okta account.

---

## 6. Daily Running Log Form

The Daily Running Log form includes:

- Date
- Mileage
- Hurting?
- Hurting details
- Sleep
- Stress
- Plate Proportions?
- Did you get that bread?
- How feel?
- RPE
- Comments

### Special behavior

- if `Hurting?` is set to `Yes`, the hurting-details field appears and becomes required
- the `How feel?` field is plain text and is not color-coded in the spreadsheet

### Validation rules users will notice

- mileage must be 0 or greater
- sleep must be between 0 and 24
- stress must be between 1 and 10
- RPE must be between 0 and 10
- hurting details are required when hurting is `Yes`
- `How feel?` is required and limited to 100 characters
- `Comments` are required and limited to 2000 characters

---

## 7. Workout Log Form

The Workout Log form includes:

- Date
- Workout Type
- Completion
- Actual Paces
- Workout Description

### Workout Type options

- `Strength`
- `Strides`
- `Workout`

### Validation rules users will notice

- workout type is required
- completion is required
- actual paces are required
- workout description is required
- text fields are limited to 2000 characters

---

## 8. Running Sheet

The Running Sheet shows saved running logs in a spreadsheet-style table.

Typical columns include:

- Date
- Mileage
- Hurting / pain details
- Sleep
- Stress
- Plate Proportions
- Did you get that bread?
- How Feel?
- RPE
- Details
- Coach Comment

### Athlete view

When athletes open their own running sheet, they can:

- edit row values directly
- save changes with `Save Changes`
- delete rows

When athletes open another athlete's running sheet, they can only view it.

### Coach view

Coaches see the running sheet in read-only mode for athlete-entered fields.

Coaches can:

- type into the Coach Comment field
- save the comment for that row

### Wellness coloring

Some running-sheet wellness cells use color cues:

- sleep
- stress
- plate proportions
- did you get that bread

---

## 9. Workout Sheet

The Workout Sheet shows saved workout logs in a spreadsheet-style table.

Typical columns include:

- Date
- Workout Type
- Completion
- Actual Paces
- Workout Description
- Coach Comment

### Athlete view

When athletes open their own workout sheet, they can:

- edit row values
- save changes
- delete rows

When athletes open another athlete's workout sheet, they can only view it.

### Coach view

Coaches can:

- review athlete workout rows
- add or update row-level coach comments

---

## 10. Date Filters

Both sheets support the following filters:

- `Today`
- `Week`
- `Month`
- `Custom`

### How they work

- `Today` shows rows for the current date
- `Week` shows the recent 7-day window
- `Month` shows the recent 30-day window
- `Custom` lets you choose a start and end date

When a sheet is opened, it defaults to the `Week` filter.

---

## 11. Save Messages and Alerts

RaceTrack shows short on-screen notices for actions such as:

- `Running log saved.`
- `Workout log saved.`
- `Running row saved.`
- `Workout row saved.`
- `Running row deleted.`
- `Workout row deleted.`
- `Coach comment saved.`
- warning or error messages if validation fails or a request cannot be completed

These notices disappear automatically after a short time.

---

## 12. Typical Athlete Workflow

1. Sign in through Okta.
2. Land on the Daily Running Log form.
3. Enter a running log or switch to the Workout Log form.
4. Submit the entry.
5. Open `My Running Sheet` or `My Workout Sheet` from the header.
6. Edit or delete your own rows if needed.
7. Review any coach comments attached to your entries.

---

## 13. Typical Coach Workflow

1. Sign in through Okta.
2. Search for an athlete in the footer or select one from the athlete list.
3. Open that athlete's Running Sheet or Workout Sheet.
4. Review recent rows using `Week`, `Month`, or `Custom` filters.
5. Add coach comments and save them.
6. If needed, use the header admin actions to add, edit, or remove RaceTrack users.

---

## 14. Practical Notes for Users

- enter logs as close to the training day as possible
- use the Comments field to capture context that numbers alone do not show
- save spreadsheet row edits before navigating away
- coaches should keep comments specific to the row they are attached to
- if you can sign into Okta but RaceTrack denies access, contact a coach because your email likely has not been added locally

---

## 15. Summary

RaceTrack currently provides:

- athlete running and workout submission forms
- spreadsheet-style review and editing
- coach row-level commenting
- footer-based athlete selection
- coach admin tools for RaceTrack user management
- email-based authorization after Okta login

