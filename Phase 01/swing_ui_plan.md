# Swing UI Implementation Plan

Based on the instructions provided for the Faculty of Technology Academic Management System, here is a structured step-by-step plan to implement the Swing UI while strictly adhering to the architectural constraints.

## Phase 1: Setup and Shared UI Foundation
Since the project relies on an existing service/DAO layer (which we will mock or integrate if it exists), the first step is to establish the global design system and shared components.

1.  **Global Theme & Assets:**
    *   Implement a custom `UIManager` or base theme class to enforce the color palette (Primary: `#123B5D`, Accent: `#D9A441`, etc.) and fonts (Inter/System sans-serif).
    *   Set up predictable spacing constants (8px, 16px, 24px, 32px) to be used across all layouts.
2.  **Shared Application Shell:**
    *   `MainFrame`: The primary window container using `BorderLayout`.
    *   `TopBar`: Header displaying the app name, logged-in user, role, and a logout button.
    *   `NavigationPanel`: A dynamic sidebar menu that renders options based on the authenticated user's role.
    *   `StatusBar`: Footer for system connection states and generic messages.
3.  **Reusable UI Components:**
    *   Custom styled `JButton`, `JTextField`, `JTable`, and `JComboBox` to match the academic professional design.
    *   Centralized `DialogManager` for confirmation dialogs and error alerts to ensure consistent error UX.

## Phase 2: Authentication and Routing
1.  **Login Screen:**
    *   Build the `LoginView` with username/password fields and keyboard 'Enter' support.
    *   Integrate with `AuthController` -> `AuthService`.
    *   Implement safe error handling (no stack traces) and loading states.
2.  **Role-Based Routing System:**
    *   Upon successful login, initialize the user session.
    *   Dynamically load the correct Dashboard and `NavigationPanel` based on the user role (Admin, Lecturer, Tech Officer, Undergraduate).

## Phase 3: Role Dashboards
Implement the four core dashboards, ensuring they are strictly read-only summaries that fetch data via controllers.
1.  **Admin Dashboard:** System stats (users, departments, courses) and quick action shortcuts.
2.  **Lecturer Dashboard:** Assigned offerings, pending tasks, recent notices.
3.  **Technical Officer Dashboard:** Current attendance and medical approval tasks.
4.  **Undergraduate Dashboard:** Personal student summary, current courses, and quick eligibility/GPA snapshots.

## Phase 4: Core Management Modules (Admin)
1.  **User & Student Management:**
    *   Searchable grids, detail forms, and safe deactivation flows.
    *   Strict validation for duplicates and contact info.
2.  **Course Management:**
    *   Tabbed interface for Courses, Components, Semesters, Offerings, and Enrolments.
3.  **Timetable & Notices:**
    *   Grid-based timetable editor with conflict validation via the business layer.
    *   Notice composition and publishing.

## Phase 5: Academic Operations (Lecturer & Tech Officer)
1.  **Attendance Screen:**
    *   Filters by semester/offering/session.
    *   Grid for present/absent marking with calculated summaries.
2.  **Medical Management:**
    *   Form for logging medical evidence and dates.
    *   Approval workflow (Approve/Reject) with validation for overlapping dates.
3.  **Marks Entry:**
    *   Table-based marks entry (0-100 validation).
    *   Read-only columns for assessment weights and CA contribution.
4.  **Course Materials:**
    *   Simple CRUD interface for managing file paths/references.

## Phase 6: Academic Results (Read-Only Views)
1.  **Eligibility Screen:**
    *   Detailed view showing Attendance, CA status, and final exam eligibility with clear pass/fail reasons.
2.  **Grades and GPA Screens:**
    *   Course results table (Credits, Mark, Grade, Point).
    *   GPA summary table (SGPA, CGPA calculations fetched from the service layer).

## Phase 7: Reports and Profile
1.  **Reporting Shell:**
    *   Common UI wrapper with filter area, preview table, and export button.
    *   Implement the 8 required report views.
2.  **Profile Screen:**
    *   Locked username fields, password change flow for staff.
    *   Contact/picture updates for students.

## Phase 8: Testing & Polish (Completion Gate)
1.  **Error Handling & UX Audit:**
    *   Ensure all validation messages are inline and field-level.
    *   Verify keyboard navigation (Tab ordering, Enter to submit).
    *   Confirm no business logic exists in the UI layer.
2.  **UI Testing:**
    *   Write smoke tests using AssertJ Swing (or similar) for Login, Navigation, and critical forms (Marks, Attendance).
    *   Unit test isolated UI Controllers using Mockito.
3.  **Documentation:**
    *   Update `progress.md` with completion status and test results.

---
**Next Step:** I can initialize a new Java project in your workspace (since you currently don't have one active) with the required folder structure (Swing Views, Controllers, Services, DAOs) and start building Phase 1. Let me know if you are ready to proceed!
