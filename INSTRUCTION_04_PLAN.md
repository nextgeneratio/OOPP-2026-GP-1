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
- [x] Controller properly validates incoming payload/DTO before dispatching to service.
- [x] Valid service invocations map correctly to response models.
- [x] Controllers contain no hidden calculation or database logic.
- [x] Business and security exceptions are mapped to standardized, safe error responses.


---

## Phase 5: MySQL Integration & Transaction Testing

### Directory Structure
`src/test/java/.../integration`

### 5.1 Test Database Configuration & Fixtures
- [x] Dedicated test schema setup (e.g., `oop_sms_test`).
- [x] Deterministic seed data loading and tear-down / rollback hooks per test suite.

### 5.2 Persistence & Constraint Testing
- [x] Insert, Read, Update, and Soft-delete operations via DAO.
- [x] Foreign Key (FK) constraint enforcement on orphaned references.
- [x] Unique constraint enforcement (e.g., duplicate usernames, duplicate student enrolments).

### 5.3 Transactional Rollback Tests
- [x] Execute multi-step operation (e.g., publish marks + write audit log + update enrolment status).
- [x] Deliberately trigger failure at an intermediate step.
- [x] Verify all preceding mutations rollback completely without leaving orphan rows.

### 5.4 End-to-End Calculation Verification
- [x] Load persisted attendance records and mark sheets; verify service returns exact calculated SGPA/CGPA matching database records.

---

## Phase 6: Security, Audit Logging & Error Handling Audit

### 6.1 Security Checks
- [x] Confirm passwords stored in the database are hashed with salt (e.g., BCrypt).
- [x] Verify plain-text passwords never leak into logs or query strings.
- [x] Audit that all DAO queries use `PreparedStatement` to avoid SQL injection.

### 6.2 Audit Logging
- [x] Verify audit log entries capture actor ID, timestamp, action type, and target entity for:
  - User login attempts (success & failure).
  - Marks creation and update.
  - Attendance recording and modifications.
  - Medical certificate approval/rejection.
  - Profile and master data creation/modification/deactivation.

### 6.3 Error Handling & UX Safety
- [x] Ensure user-facing UI displays friendly messages and never exposes technical stack traces.

---

## Phase 7: Performance Smoke Checks & Final Readiness

### 7.1 Performance Smoke Verification (< 3 Seconds Response)
- [x] Authentication / Login response time.
- [x] Student list query response time.
- [x] Marks summary list response time.
- [x] Attendance sheet view response time.
- [x] Student grade report / result view generation time.

### 7.2 Automated Build Command Verification
- [x] Run `mvn test` to ensure 100% test pass rate.
- [x] Run `mvn -q test` for clean automated runs.
- [x] Confirm no tests are bypassed using `@Disabled`.

### 7.3 Update `progress.md`
- [x] Document unit test count, integration test count, pass rates.
- [x] Document performance observation results and security audit sign-off.
- [x] Document database reset and re-seed instructions.
- [x] Mark Instruction 04 as complete and prepare for Instruction 05 (System Testing).
