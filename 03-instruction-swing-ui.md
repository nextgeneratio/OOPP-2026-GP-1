# Instruction 03 — Complete Swing UI, Role Dashboards and Layer Integration

## Ready-to-paste prompt

Continue from the current repository.

Before coding:

1. Read the SRS.
2. Read `progress.md`.
3. Inspect the existing API/controller and service classes.
4. Reuse the established domain/business/data layers.
5. Do not place business logic in Swing classes.

## Goal

Build and connect the full Swing UI for every screen required by the SRS.

The UI must use:

`Swing View → API/Controller → Business Service → DAO → MySQL`

The UI is responsible for:

- collecting input.
- showing validation feedback.
- navigation.
- rendering response data.
- confirmation dialogs.
- loading/error states.

The UI is NOT responsible for:

- SQL.
- GPA/grade calculations.
- permission rules.
- complex validation.
- transactions.

## 1. Global UI design system

Use a professional academic desktop style.

### Colors

Primary: `#123B5D`
Primary hover: `#1D5A87`
Secondary: `#2F7D8C`
Accent/gold: `#D9A441`
Background: `#F5F7FA`
Surface: `#FFFFFF`
Text: `#1F2937`
Muted: `#667085`
Border: `#D0D5DD`
Success: `#2E7D32`
Warning: `#B7791F`
Error: `#C62828`
Info: `#1565C0`

Do not use arbitrary new colors in individual screens.

### Fonts

Prefer Inter; fall back safely to the system sans-serif.

- Page title: 24–28 px, bold.
- Section title: 18–20 px, semibold.
- Body: 14 px.
- Form labels: 13–14 px.
- Table: 13 px.
- Button: 14 px, semibold.
- Helper/error: 12–13 px.

### Spacing

Use a predictable spacing rhythm:

- 8 px between related controls.
- 16 px between fields/sections.
- 24 px page/card padding.
- 24–32 px between major sections.

Use `BorderLayout`, `GridBagLayout`, or suitable layout managers. Avoid absolute positioning.

## 2. Shared application shell

Create:

- `MainFrame`.
- `NavigationPanel`.
- `TopBar`.
- `StatusBar`.
- common dialog classes.
- common table/form components.

Top bar should show:

- application name.
- current user's full name.
- role.
- profile/action menu.
- logout.

Navigation should contain only permitted modules.

Still rely on service-layer authorization.

## 3. Login screen

Create a polished login screen:

- application title.
- username.
- password.
- Login button.
- error/validation area.
- connection/error state.
- keyboard-friendly Enter action.

On success:

- create authenticated session.
- route by role.
- display the correct dashboard.

On invalid login show only a safe message.

Never show SQL/stack traces.

## 4. Role dashboards

Create four dashboards.

### Admin dashboard

Show scoped summary cards for:

- users.
- departments/batches.
- courses.
- offerings.
- notices.
- timetable status.

Shortcuts:

- User Management.
- Student Management.
- Course Management.
- Timetable.
- Notices.
- Reports.

### Lecturer dashboard

Show:

- assigned course offerings.
- recent notices.
- pending assessment/mark tasks.
- attendance/eligibility academic views where permitted.

Shortcuts:

- Course Materials.
- Assessments/Marks.
- Eligibility.
- Grades/GPA.
- Reports.
- Profile.

### Technical Officer dashboard

Show:

- department.
- current attendance tasks.
- medical approval tasks.
- timetable information.

Shortcuts:

- Attendance.
- Medical.
- Timetable.
- Reports.
- Profile.

### Undergraduate dashboard

Show:

- personal name/student number.
- current courses.
- attendance summary.
- eligibility status.
- latest results/SGPA/CGPA.
- notices.
- materials/timetable.

Everything is read-only except fields explicitly permitted by the SRS.

## 5. User Management

Admin screen:

- search field.
- role filter.
- status filter.
- table of users.
- create.
- update.
- deactivate.
- profile detail form.

Critical actions require confirmation.

Deactivation must not physically erase dependent academic history.

## 6. Student Management

Fields:

- student number.
- full name.
- batch.
- contact details.
- status.
- profile picture/reference where applicable.

Validation messages should appear close to fields.

## 7. Course Management

Create a tabbed or sectioned workflow:

- Courses.
- Components.
- Semesters.
- Offerings.
- Enrolments.

Allow Admin to create/update valid academic master data.

Display duplicates and invalid credits clearly.

## 8. Attendance screen

Technical Officer view:

- semester.
- offering.
- component.
- session date.
- timetable-linked session.
- enrolled-student table/grid.
- present/absent selection.
- Save button.

Display calculated:

- present.
- scheduled.
- excused.
- percentage.

Lecturer/student versions are read-only and scope-limited.

Use clear text status:

`ELIGIBLE`, `NOT ELIGIBLE`, `PENDING`, etc.

Do not make status depend on color alone.

## 9. Medical management

Technical Officer screen:

- student selector.
- course/offering.
- medical date.
- start/end date.
- reason.
- evidence reference/path.
- approval status.
- approve/reject controls.

Show validation messages for:

- future dates.
- invalid date range.
- missing reason.
- duplicate/overlap.

Confirm approval/rejection actions.

## 10. Marks entry

Lecturer screen:

- offering selector.
- assessment selector.
- batch/student filter.
- marks table.
- Save/Update.

Support mark values from 0–100.

Show:

- raw mark.
- assessment weight.
- CA contribution where applicable.
- validation state.

Do not calculate final academic results inside the table model. Use the business layer.

## 11. Eligibility screen

Show:

- student.
- course.
- attendance status.
- CA status.
- final examination eligibility.
- failed conditions.

Example:

```text
Final Exam Eligibility: NOT ELIGIBLE

Reasons:
✓ CA Eligibility: PASS
✗ Theory Attendance: FAIL (73.33% < 80%)
✓ Practical Attendance: PASS
```

## 12. Grades and GPA

Create two clear screens:

### Course Results

Columns:

- Course.
- Credits.
- Final Mark.
- Grade.
- Grade Point.

No manual editing.

### GPA

Show:

- semester.
- counted courses.
- credits.
- grade points.
- weighted total.
- SGPA.
- CGPA.

Use two decimal places.

## 13. Timetable

Admin editor:

- department.
- semester.
- timetable title.
- course/offering.
- component.
- day.
- start/end.
- location.

Use a readable day/time grid.

Conflict validation must call the business layer.

## 14. Notices

Admin:

- compose.
- title.
- body.
- audience.
- publish date.
- expiry.
- publish/expire.

Users:

- filtered notice list.
- detail view.

Expired/invalid notices must be clearly indicated or excluded according to service behavior.

## 15. Course materials

Lecturer:

- offering.
- title.
- description.
- controlled file path/reference.
- add/edit/delete.

Undergraduate:

- view materials for enrolled offerings only.

Do not build a public online upload system.

## 16. Profile

Lecturer and Technical Officer:

- own permitted profile fields.
- username locked.
- password handling through a controlled change flow.

Undergraduate:

- contact details.
- profile picture/reference.
- username locked.

All edit scope enforced in the service layer.

## 17. Reports

Create a common report shell:

- filter area.
- preview table.
- print/export action.
- empty-state message.
- loading state.
- error state.

Required report categories include:

- student attendance.
- batch/course attendance.
- CA/final marks.
- eligibility.
- grades/course results.
- SGPA/CGPA.
- medical records.
- student profile.

Never expose raw records outside authorization scope.

## 18. UX and accessibility

Follow the SRS:

- keyboard navigation.
- readable labels.
- visible focus.
- sufficient contrast.
- non-colour-only status.
- confirmation before destructive/locking actions.
- clear validation near the offending field.
- consistent table headers.
- disabled state for unavailable actions.
- sensible tab order.

## 19. Error UX

The centralized error handler must convert errors to friendly dialogs/inline messages.

Examples:

- Validation → field-level message.
- Authorization → permission dialog.
- Database unavailable → safe reconnect message.
- Missing record → unavailable message.
- Unexpected failure → safe generic message + log reference.

Never display:

- SQL statements.
- database credentials.
- Java stack traces.
- internal file paths unnecessarily.

## 20. UI tests

At minimum create smoke/integration tests for:

- login screen opens.
- valid login reaches correct dashboard.
- invalid login shows safe error.
- role navigation is correct.
- course form validates bad data.
- mark form rejects <0 and >100.
- attendance screen displays correct data.
- eligibility screen displays failed reason.
- GPA screen renders calculated values.
- logout returns to login.

Use Mockito for isolated controller tests and AssertJ Swing for high-value UI workflows where practical.

## 21. UI completion gate

Before moving to the next phase:

- every required SRS screen exists.
- every role can see only appropriate navigation.
- each screen talks to an API/controller, not DAOs directly.
- no business calculations exist inside Swing.
- error handling is centralized.
- visual theme is consistent.
- keyboard/accessibility requirements are addressed.
- compile/tests pass.

Update `progress.md` with:

- screen completion status.
- integration status.
- UI tests passed/failed.
- known visual/functional issues.
- exact next phase: final quality/testing.
