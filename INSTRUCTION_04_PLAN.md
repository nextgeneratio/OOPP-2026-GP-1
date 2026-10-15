# Implementation Plan: Instruction 04 — Quality Hardening, Unit Testing, Integration Testing and Test Readiness

## Overview
This plan outlines the systematic, phased implementation for hardening the system architecture, achieving full unit and integration test coverage, validating security/audit requirements, and preparing the codebase for final system acceptance testing.

---

## Required Test Pyramid

```text
                System / UI Tests
                     /\
                    /  \
          Integration Tests (MySQL Test DB)
                 /      \
                /        \
         Unit Tests (Domain, Service, API)
```


---

## Phase 1: Architecture Audit & Code Decoupling

### 1.1 Strict Dependency Direction Check
Verify and enforce the one-way layer hierarchy:
```text
Presentation (Swing UI)
        ↓
API / Controllers
        ↓
Business / Service Layer
        ↓
Data Access (DAO / Repositories)
        ↓
JDBC / MySQL
```

### 1.2 Isolation & Layer Boundary Validation
- [x] Ensure **no UI class** imports `java.sql.*`, `javax.sql.*`, or writes inline SQL queries.
- [x] Ensure **no DAO class** references Swing or presentation components.
- [x] Ensure **domain entities** have zero dependencies on Swing or JDBC.
- [x] Confirm **business services** can be instantiated and tested in isolation without booting the GUI.
- [x] Verify **API controllers** are thin and perform no hidden business calculations.
- [x] Verify **authorization checks** reside strictly within the business/service layer.
- [x] Ensure **centralized exception handling** is implemented (safe user messages, no raw stack traces in GUI).
- [x] Remove any hardcoded database credentials or secrets from Git-tracked source files.

---

## Phase 2: Domain Unit Tests & Boundary Values

### Directory Structure
`src/test/java/.../unit/domain`

### 2.1 Mandatory Boundary-Value Tests
- **Marks Validation:**
  - `-0.01` ➔ Rejected / Validation Exception
  - `0.00` ➔ Accepted (min valid score)
  - `100.00` ➔ Accepted (max valid score)
  - `100.01` ➔ Rejected / Validation Exception
- **Continuous Assessment (CA) Hurdle:**
  - `39.99` ➔ Fail / Ineligible
  - `40.00` ➔ Pass / Eligible
- **Attendance Thresholds:**
  - `79.99%` ➔ Fail / Ineligible
  - `80.00%` ➔ Pass / Eligible
  - `100.00%` ➔ Pass / Full attendance
  - `0` denominator (no classes held) ➔ Handled safely without division by zero errors
- **Grade Cutoff Boundaries:**
  - `34 / 35` (E / D)
  - `39 / 40` (D / D+)
  - `44 / 45` (D+ / C-)
  - `49 / 50` (C- / C)
  - `54 / 55` (C / C+)
  - `59 / 60` (C+ / B-)
  - `64 / 65` (B- / B)
  - `69 / 70` (B / B+)
  - `74 / 75` (B+ / A-)
  - `79 / 80` (A- / A)
  - `84 / 85` (A / A+)

### 2.2 Property-Style Calculation Checks
- [ ] Check percentages are always within `[0.0, 100.0]`.
- [ ] Check final marks are bounded within valid assessment limits.
- [ ] Check SGPA and CGPA values never drop below `0.00` and never exceed `4.00`.
- [ ] Check monotonicity: Increasing an individual component mark never decreases the overall weighted final mark.

### 2.3 Other Domain Invariant Tests
- [ ] Weight boundary validation (total weight per course sums to 100%).
- [ ] Course credit constraints.
- [ ] Timetable start time < end time and duration constraints.
- [ ] Medical certificate submission date window and status transitions.

---

## Phase 3: Business & Service Layer Unit Tests (Mocking DAOs)

### Directory Structure
`src/test/java/.../unit/service`

### 3.1 Authentication & Authorization Rules
- [x] Active valid credentials succeed.
- [x] Invalid password fails and triggers failed login audit record.
- [x] Inactive / deactivated user is denied access.
- [x] Role-based authorization rules (e.g., student attempting admin or lecturer tasks throws `UnauthorizedAccessException`).

### 3.2 Academic Rules & SRS Appendix A Scenarios
- [x] **STU001:** Theory 13/15, Practical 14/15 ➔ Full eligibility.
- [x] **STU002:** Theory 12/15, Practical 12/15 ➔ Evaluated against 80% threshold.
- [x] **STU003:** Theory 10/15, Practical 10/15 ➔ Attendance failure scenario.
- [x] **STU004:** Theory 13/15 with approved medical absence ➔ Attendance adjusted to satisfy requirement.
- [x] **STU005:** Theory 8/15 with approved medical absence ➔ Even after adjustment, fails 80% threshold.

### 3.3 Enrolment, Timetable & Assessment Scenarios
- [x] Repeat enrolment logic & grade capping (if applicable per SRS).
- [x] Batch-missed enrolment scenario.
- [x] Timetable conflict check: Detect overlapping slot assignments for lecturer/student.
- [x] Incomplete assessment configuration: Attempting final grade calculation before all mandatory components are configured.


---

## Phase 4: API & Controller Tests

### Directory Structure
`src/test/java/.../unit/api`

### 4.1 Request Validation & Flow Verification
- [ ] Controller properly validates incoming payload/DTO before dispatching to service.
- [ ] Valid service invocations map correctly to response models.
- [ ] Controllers contain no hidden calculation or database logic.
- [ ] Business and security exceptions are mapped to standardized, safe error responses.

---

## Phase 5: MySQL Integration & Transaction Testing

### Directory Structure
`src/test/java/.../integration`

### 5.1 Test Database Configuration & Fixtures
- [ ] Dedicated test schema setup (e.g., `oop_sms_test`).
- [ ] Deterministic seed data loading and tear-down / rollback hooks per test suite.

### 5.2 Persistence & Constraint Testing
- [ ] Insert, Read, Update, and Soft-delete operations via DAO.
- [ ] Foreign Key (FK) constraint enforcement on orphaned references.
- [ ] Unique constraint enforcement (e.g., duplicate usernames, duplicate student enrolments).

### 5.3 Transactional Rollback Tests
- [ ] Execute multi-step operation (e.g., publish marks + write audit log + update enrolment status).
- [ ] Deliberately trigger failure at an intermediate step.
- [ ] Verify all preceding mutations rollback completely without leaving orphan rows.

### 5.4 End-to-End Calculation Verification
- [ ] Load persisted attendance records and mark sheets; verify service returns exact calculated SGPA/CGPA matching database records.

---

## Phase 6: Security, Audit Logging & Error Handling Audit

### 6.1 Security Checks
- [ ] Confirm passwords stored in the database are hashed with salt (e.g., BCrypt).
- [ ] Verify plain-text passwords never leak into logs or query strings.
- [ ] Audit that all DAO queries use `PreparedStatement` to avoid SQL injection.

### 6.2 Audit Logging
- [ ] Verify audit log entries capture actor ID, timestamp, action type, and target entity for:
  - User login attempts (success & failure).
  - Marks creation and update.
  - Attendance recording and modifications.
  - Medical certificate approval/rejection.
  - Profile and master data creation/modification/deactivation.

### 6.3 Error Handling & UX Safety
- [ ] Ensure user-facing UI displays friendly messages and never exposes technical stack traces.

---

## Phase 7: Performance Smoke Checks & Final Readiness

### 7.1 Performance Smoke Verification (< 3 Seconds Response)
- [ ] Authentication / Login response time.
- [ ] Student list query response time.
- [ ] Marks summary list response time.
- [ ] Attendance sheet view response time.
- [ ] Student grade report / result view generation time.

### 7.2 Automated Build Command Verification
- [ ] Run `mvn test` to ensure 100% test pass rate.
- [ ] Run `mvn -q test` for clean automated runs.
- [ ] Confirm no tests are bypassed using `@Disabled`.

### 7.3 Update `progress.md`
- [ ] Document unit test count, integration test count, pass rates.
- [ ] Document performance observation results and security audit sign-off.
- [ ] Document database reset and re-seed instructions.
- [ ] Mark Instruction 04 as complete and prepare for Instruction 05 (System Testing).
