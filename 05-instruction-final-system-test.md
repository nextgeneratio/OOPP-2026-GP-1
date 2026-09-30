# Instruction 05 — Final End-to-End System Testing, Data Insertion and Demonstration

## Ready-to-paste prompt

This is the final system testing phase for the Faculty of Technology Academic Management System.

Read:

1. The SRS.
2. `progress.md`.
3. The entire implemented project structure.
4. Existing test data/scripts.

Do not refactor the architecture during this phase unless a defect prevents execution.

## Objective

Test the whole application as a real user would use it:

```text
Login
  ↓
Role Dashboard
  ↓
Create/maintain academic data
  ↓
Configure courses/assessments
  ↓
Enter attendance
  ↓
Enter medical record
  ↓
Enter marks
  ↓
Calculate CA / attendance
  ↓
Check eligibility
  ↓
Calculate final result / grades
  ↓
Calculate SGPA / CGPA
  ↓
View notices/materials/timetable
  ↓
Generate reports
  ↓
Audit changes
  ↓
Logout
```

## 1. Prepare a clean test database

Create/reset a dedicated test database.

Execute:

1. schema.
2. seed/master data.
3. demonstration data.

Do not use a production database.

Verify database connection before opening the application.

## 2. Verify seed users

The SRS requires at least:

- 1 Admin.
- 5 Lecturers.
- 4 Technical Officers.
- 20 Undergraduates.

Verify each role has a valid account/profile link.

Do not expose or document real passwords. Use controlled test credentials.

## 3. Test Admin workflow

### 3.1 Login

- valid Admin login.
- invalid password.
- inactive account.
- blank fields.

Expected:

- successful role dashboard.
- safe invalid login error.
- no stack trace.

### 3.2 User management

Create a test lecturer/user.

Test:

- valid creation.
- duplicate username.
- duplicate email.
- invalid email.
- invalid phone.
- deactivate user.
- verify deactivated user cannot log in.

### 3.3 Student management

Create/update a student.

Test:

- duplicate student number.
- invalid batch.
- role/profile-link consistency.
- permitted fields.

### 3.4 Course management

Create a course and components.

Test:

- duplicate course code.
- invalid credits.
- invalid component data.
- successful course.

Create a semester and offering.

Verify:

- correct batch.
- correct department.
- correct semester.
- lecturer assignment.

### 3.5 Assessment setup

Create an assessment plan that totals 100%.

Then deliberately try:

- total less than 100%.
- total more than 100%.
- mark outside 0–100.

Verify safe validation.

### 3.6 Timetable

Create theory and practical timetable entries.

Try a deliberate overlap.

Expected:

`timetable entry conflicts with an existing entry`

or equivalent safe message.

### 3.7 Notices

Create, publish, and expire a notice.

Verify target roles/batch see it correctly.

### 3.8 Reports

Generate Admin-authorized reports.

Verify no out-of-scope raw data appears.

## 4. Test Lecturer workflow

Login as a lecturer assigned to selected courses.

### 4.1 Verify scope

Confirm lecturer cannot access another lecturer's unauthorized course.

Test this through both:

- UI navigation.
- direct controller/service invocation if practical.

The service layer must reject unauthorized access even if the UI is bypassed.

### 4.2 Course materials

Create a material for an assigned offering.

Verify enrolled student can see it.

Verify unrelated student cannot see it.

### 4.3 Marks

Select an assessment and enter marks.

Test:

- valid marks.
- 0.
- 100.
- negative.
- >100.
- missing assessment.
- unauthorized offering.

Verify audit records for create/update.

### 4.4 Eligibility

Run eligibility for the known Appendix A students.

Verify failed reasons are displayed.

## 5. Test Technical Officer workflow

Login as Technical Officer.

### 5.1 Attendance

Create timetable-linked attendance sessions.

Enter records.

Test:

- duplicate attendance record.
- absent student not enrolled in offering.
- invalid component.
- session outside valid semester.

Verify counts and percentages.

### 5.2 Required Appendix A cases

Load/test:

#### STU001

Theory: 13/15

Practical: 14/15

Expected: both components above 80%; attendance eligible.

#### STU002

Theory: 12/15

Practical: 12/15

Expected: exactly 80%; attendance eligible.

#### STU003

Theory: 10/15

Practical: 10/15

Expected: below 80%; attendance not eligible.

#### STU004

Theory: 13/15 plus approved medical absence.

Expected: approved medical changes denominator according to BR-009 and eligibility is recalculated.

#### STU005

Theory: 8/15 plus approved medical that still does not reach 80%.

Expected: remains below 80%; attendance not eligible.

Record the exact observed calculated percentages.

### 5.3 Medical records

Create:

- pending.
- approved.
- rejected.

Verify:

- pending does not change attendance.
- rejected does not change attendance.
- approved changes attendance calculation only according to the configured rule.
- invalid date range is rejected.
- future date is rejected.
- missing reason is rejected.
- approval/rejection is audited.

## 6. Test undergraduate workflow

Login as an undergraduate.

Verify the student can see only:

- own permitted profile data.
- own results.
- own SGPA/CGPA.
- own attendance/eligibility.
- enrolled course materials.
- authorized notices/timetable.

Attempt to access another student's result through the application layer.

Expected:

authorization failure.

## 7. End-to-end marks → results workflow

For at least one complete course:

1. configure assessments totaling 100%.
2. enter marks.
3. calculate CA.
4. verify CA eligibility.
5. record attendance.
6. apply any approved medical case.
7. evaluate final exam eligibility.
8. calculate final mark.
9. resolve grade.
10. resolve grade point.
11. include result in SGPA.
12. verify CGPA behavior.

Manually calculate expected values separately and compare them with the application.

Do not assume the application is correct just because it runs.

## 8. Repeat and batch-missed scenarios

Use the SRS sample data:

- repeat enrolment with `attempt_no=2` and `student_status=REPEAT`.
- batch-missed enrolment with `student_status=BATCH_MISSED`.

Verify:

- attempts are retained.
- duplicate attempt constraints work.
- all attempts remain auditable.
- configured result-selection policy is respected in CGPA.

## 9. Grade boundary test

Create final marks exactly at:

`34, 35, 39, 40, 44, 45, 49, 50, 54, 55, 59, 60, 64, 65, 69, 70, 74, 75, 79, 80, 84, 85, 100`

Verify each grade/grade point against the SRS grading table.

Record any discrepancy as a defect; do not silently change the expected value.

## 10. Report workflow

Generate:

- student attendance report.
- batch/course attendance.
- CA/final marks report.
- eligibility report.
- grade/course result report.
- SGPA/CGPA report.
- medical report.
- student profile report.

Verify:

- filtering.
- correct totals.
- authorization.
- readable output.
- print/export behavior if implemented.
- no unauthorized raw records.

## 11. Error-path system testing

Deliberately test:

- database disconnected.
- duplicate record.
- invalid input.
- missing record.
- unauthorized action.
- transaction failure.

For every case verify:

1. user sees a safe message.
2. no SQL/stack trace is shown.
3. technical error is logged.
4. application returns to a usable state.
5. no partial inconsistent database data remains.

## 12. Audit testing

After the workflows, inspect `audit_logs`.

Confirm actor + action + entity + timestamp are recorded for the sensitive changes required by the SRS.

## 13. UI/system testing

Check:

- login.
- all dashboards.
- role navigation.
- keyboard navigation.
- visible focus.
- table readability.
- validation messages.
- dialogs.
- confirmation prompts.
- no color-only status.
- logout.
- reconnect/recover from temporary database failure where practical.

## 14. Final acceptance table

Create a test execution document containing:

| Test ID | Requirement/Workflow | Preconditions | Steps | Expected | Actual | Pass/Fail | Evidence |
|---|---|---|---|---|---|---|---|

Trace each major requirement to at least one system test.

Pay special attention to:

- FR-001 to FR-014.
- FR-015/FR-016 authorization.
- BR-005 to BR-015.
- NFR-001 to NFR-014.

## 15. Final verification commands

Run:

```bash
mvn clean test
```

Then launch the application from IntelliJ.

Verify a clean startup with the test configuration.

If the project has a packaged executable/JAR, verify it also starts with the expected configuration.

## 16. Final progress.md update

At the end of system testing, update `progress.md` with:

```markdown
## Final System Test

Date:
Build result:
Unit tests:
Integration tests:
System tests:

### Passed
- ...

### Failed
- ...

### Known limitations
- ...

### Demonstration-ready features
- ...

### Evidence
- Test data:
- Screenshots:
- Test report:
- Logs:

### Final status
READY / NOT READY

### Notes
...
```

Do not mark READY if a core SRS requirement is broken.

The final output should clearly distinguish:

- implemented and tested.
- implemented but not fully tested.
- known limitation/assumption.
- future enhancement outside current scope.
