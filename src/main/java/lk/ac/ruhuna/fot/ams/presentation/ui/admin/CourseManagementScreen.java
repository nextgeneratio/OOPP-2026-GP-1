package lk.ac.ruhuna.fot.ams.presentation.ui.admin;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.CourseCatalog;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingSetup;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.BatchRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.CourseRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.DepartmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.LecturerOption;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.SemesterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.SelectionOption;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Admin course management uses business labels while keeping IDs on selected option objects. */
public final class CourseManagementScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;

    public CourseManagementScreen(AuthenticatedSession actor, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.courses = courses;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Course and programme management</h2>Choose records by their course, batch, department and academic-term details.</html>"), BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        if (actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.ADMIN) {
            addAction(actions, "Create department (ICT/BST/ET)", this::createDepartment);
            addAction(actions, "Create level 1–4 batch", this::createBatch);
            addAction(actions, "Create semester", this::createSemester);
            addAction(actions, "Create course", this::createCourse);
            addAction(actions, "Edit course details", this::editCourse);
        }
        addAction(actions, "Create course offering", this::createOffering);
        addAction(actions, "Assign lecturer", this::assignLecturer);
        addAction(actions, "Browse course catalog", this::browseCatalog);
        if (actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.ADMIN) addAction(actions, "Deactivate course", this::deactivateCourse);
        add(new JScrollPane(actions), BorderLayout.CENTER);
    }

    private void createDepartment() {
        JTextField code = field("ICT, BST, or ET"); JTextField name = field("Department name");
        JTextField office = field("Office location (optional)");
        if (!form("Create department", new String[]{"Code", "Name", "Office"}, code, name, office)) return;
        show(courses.createDepartment(actor, new DepartmentRow(0, code.getText(), name.getText(), emptyToNull(office.getText()), true)));
    }

    private void createBatch() {
        CourseCatalog data = catalog(); if (data == null) return;
        JComboBox<SelectionOption> department = departments(data, null);
        JTextField year = field("Intake year"); JTextField programme = field("Programme");
        JTextField code = field("Batch code"); JComboBox<Integer> level = new JComboBox<>(new Integer[]{1,2,3,4});
        if (!form("Create batch", new String[]{"Department", "Intake year", "Programme", "Batch code", "Level"},
                department, year, programme, code, level)) return;
        show(courses.createBatch(actor, new BatchRow(0, id(department), integer(year), programme.getText(),
                code.getText(), (Integer) level.getSelectedItem())));
    }

    private void createSemester() {
        JTextField year = field("Academic year (e.g. 2026/2027)"); JComboBox<Integer> number = new JComboBox<>(new Integer[]{1,2});
        JTextField start = field("Start date yyyy-mm-dd"); JTextField end = field("End date yyyy-mm-dd");
        if (!form("Create semester", new String[]{"Academic year", "Semester", "Start", "End"}, year, number, start, end)) return;
        show(courses.createSemester(actor, new SemesterRow(0, year.getText(), (Integer) number.getSelectedItem(),
                java.time.LocalDate.parse(start.getText()), java.time.LocalDate.parse(end.getText()))));
    }

    private void createCourse() {
        CourseCatalog data = catalog(); if (data == null) return;
        JComboBox<SelectionOption> department = departments(data, null);
        JTextField code = field("Course code"); JTextField title = field("Title"); JTextField credits = field("Total credits");
        JComboBox<ComponentType> component = new JComboBox<>(ComponentType.values());
        JTextField componentCredits = field("Component credits"); JTextField sessions = field("Planned sessions");
        if (!form("Create course", new String[]{"Department", "Code", "Title", "Total credits", "Component type", "Component credits", "Planned sessions"},
                department, code, title, credits, component, componentCredits, sessions)) return;
        ComponentType selectedType = (ComponentType) component.getSelectedItem();
        List<CourseDao.ComponentRow> components = List.of(new CourseDao.ComponentRow(0, 0, selectedType,
                decimal(componentCredits), integer(sessions)));
        CourseRow course = new CourseRow(0, id(department), code.getText(), title.getText(), decimal(credits), true);
        show(courses.createCourse(actor, new CourseController.CreateCourseRequest(course, components, null)));
    }

    private void createOffering() {
        CourseCatalog data = catalog(); if (data == null) return;
        JComboBox<SelectionOption> course = courses(data, null); JComboBox<SelectionOption> batch = batches(data, null, null);
        replaceOptions(batch,List.of());
        course.addActionListener(event -> {
            CourseRow selected = data.courses().stream().filter(c -> c.id() == safeId(course)).findFirst().orElse(null);
            List<SelectionOption> matching = selected == null ? List.of() : data.batches().stream()
                    .filter(b -> b.departmentId() == selected.departmentId())
                    .map(b -> new SelectionOption(b.id(), batchLabel(data,b))).toList();
            replaceOptions(batch, matching);
        });
        JComboBox<SelectionOption> semester = semesters(data, null);
        JComboBox<SelectionOption> coordinator = lecturers(data, true, null);
        JList<SelectionOption> assigned = new JList<>(lecturerOptions(data).toArray(SelectionOption[]::new)); assigned.setVisibleRowCount(4);
        assigned.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JComboBox<String> status = new JComboBox<>(new String[]{"OPEN", "CLOSED"});
        if (!form("Create offering", new String[]{"Course", "Batch", "Academic term", "Coordinator (optional)",
                "Additional lecturers (optional)", "Status"}, course, batch, semester, coordinator,
                new JScrollPane(assigned), status)) return;
        SelectionOption selectedCoordinator=selected(coordinator);
        List<Integer> extraIds = assigned.getSelectedValuesList().stream().map(SelectionOption::id)
                .filter(id -> selectedCoordinator == null || id != selectedCoordinator.id()).distinct().toList();
        OfferingSetup setup = new OfferingSetup(id(batch), id(semester), selectedCoordinator == null ? null : selectedCoordinator.id(),
                (String) status.getSelectedItem(), extraIds);
        show(courses.createOffering(actor, new CourseController.CreateOfferingRequest(id(course), setup)));
    }

    private void assignLecturer() {
        CourseCatalog data = catalog(); if (data == null) return;
        JComboBox<SelectionOption> offering = offerings(data, null);
        JComboBox<SelectionOption> lecturer = lecturers(data, false, null);
        replaceOptions(lecturer,List.of());
        offering.addActionListener(event -> refreshAssignableLecturers(data,offering,lecturer));
        refreshAssignableLecturers(data,offering,lecturer);
        if (!form("Assign lecturer", new String[]{"Course offering", "Lecturer"}, offering, lecturer)) return;
        show(courses.assignLecturer(actor, new CourseController.LecturerAssignmentRequest(id(offering), id(lecturer))));
    }

    private void editCourse() {
        CourseCatalog data = catalog(); if (data == null) return;
        JComboBox<SelectionOption> courseBox = courses(data, null);
        JComboBox<SelectionOption> department = departments(data, null);
        JTextField code = field("Course code"); JTextField title = field("Title"); JTextField credits = field("Total credits");
        courseBox.addActionListener(event -> {
            CourseRow row = data.courses().stream().filter(c -> c.id() == id(courseBox)).findFirst().orElse(null);
            if (row != null) { code.setText(row.code()); title.setText(row.title()); credits.setText(Double.toString(row.credits())); select(department, row.departmentId()); }
        });
        if (!form("Edit course", new String[]{"Course", "Department", "Course code", "Title", "Total credits"},
                courseBox, department, code, title, credits)) return;
        CourseRow old = data.courses().stream().filter(c -> c.id() == id(courseBox)).findFirst().orElseThrow();
        show(courses.updateCourse(actor, new CourseRow(old.id(), id(department), code.getText().trim(),
                title.getText().trim(), decimal(credits), old.active())));
    }

    private void deactivateCourse() {
        CourseCatalog data = catalog(); if (data == null) return;
        JComboBox<SelectionOption> course = courses(data, null);
        if (!form("Deactivate course", new String[]{"Course"}, course)) return;
        show(courses.deactivateCourse(actor, id(course)));
    }

    private void browseCatalog() {
        CourseCatalog data = catalog(); if (data == null) return;
        StringBuilder text = new StringBuilder("DEPARTMENTS\n");
        data.departments().forEach(d -> text.append(d.code()).append(" — ").append(d.name()).append('\n'));
        text.append("\nBATCHES\n"); data.batches().forEach(b -> text.append(batchLabel(data,b)).append('\n'));
        text.append("\nCOURSES\n"); data.courses().forEach(c -> text.append(courseLabel(c)).append('\n'));
        text.append("\nSEMESTERS\n"); data.semesters().forEach(s -> text.append(semesterLabel(s)).append('\n'));
        text.append("\nOFFERINGS\n"); data.offerings().forEach(o -> text.append(offeringLabel(data,o)).append('\n'));
        JTextArea output = new JTextArea(text.toString(), 24, 78); output.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(output), "Course catalog", JOptionPane.INFORMATION_MESSAGE);
    }

    private CourseCatalog catalog() {
        ApiResponse<CourseCatalog> response = courses.catalog(actor);
        if (!response.successful()) { show(response); return null; }
        return response.data();
    }

    private JComboBox<SelectionOption> departments(CourseCatalog data, Integer selectedId) {
        return options(data.departments().stream().map(d -> new SelectionOption(d.id(), d.code()+" — "+d.name())).toList(), selectedId);
    }
    private JComboBox<SelectionOption> batches(CourseCatalog data, Integer departmentId, Integer selectedId) {
        return options(data.batches().stream().filter(b -> departmentId == null || b.departmentId() == departmentId)
                .map(b -> new SelectionOption(b.id(), batchLabel(data,b))).toList(), selectedId);
    }
    private JComboBox<SelectionOption> courses(CourseCatalog data, Integer selectedId) {
        return options(data.courses().stream().filter(CourseRow::active).map(c -> new SelectionOption(c.id(), courseLabel(c))).toList(), selectedId);
    }
    private JComboBox<SelectionOption> semesters(CourseCatalog data, Integer selectedId) {
        return options(data.semesters().stream().map(s -> new SelectionOption(s.id(), semesterLabel(s))).toList(), selectedId);
    }
    private JComboBox<SelectionOption> offerings(CourseCatalog data, Integer selectedId) {
        return options(data.offerings().stream().filter(o -> "OPEN".equals(o.status())).map(o -> new SelectionOption(o.id(), offeringLabel(data,o))).toList(), selectedId);
    }
    private JComboBox<SelectionOption> lecturers(CourseCatalog data, boolean optional, Integer selectedId) {
        return options(lecturerOptions(data, optional), selectedId);
    }
    private List<SelectionOption> lecturerOptions(CourseCatalog data) {
        return lecturerOptions(data, false);
    }
    private List<SelectionOption> lecturerOptions(CourseCatalog data, boolean optional) {
        java.util.ArrayList<SelectionOption> result = new java.util.ArrayList<>();
        if (optional) result.add(new SelectionOption(0, "No coordinator"));
        data.lecturers().forEach(l -> result.add(new SelectionOption(l.id(), lecturerLabel(l))));
        return result;
    }
    private JComboBox<SelectionOption> options(List<SelectionOption> rows, Integer selectedId) {
        JComboBox<SelectionOption> box = new JComboBox<>();
        if (rows.isEmpty()) box.addItem(new SelectionOption(0, "No records available"));
        else { box.addItem(new SelectionOption(0,"Select an option…")); rows.forEach(box::addItem); }
        SelectionOption.enableSearch(box);
        if (selectedId != null) select(box, selectedId);
        return box;
    }
    private void select(JComboBox<SelectionOption> box, int id) {
        for (int i=0;i<box.getItemCount();i++) if (box.getItemAt(i).id()==id) { box.setSelectedIndex(i); return; }
    }
    private int safeId(JComboBox<SelectionOption> box){SelectionOption option=selected(box);return option==null?0:option.id();}
    private void replaceOptions(JComboBox<SelectionOption> box,List<SelectionOption> values){box.removeAllItems();if(values.isEmpty())box.addItem(new SelectionOption(0,"No matching records"));else{box.addItem(new SelectionOption(0,"Select an option…"));values.forEach(box::addItem);}}
    private void refreshAssignableLecturers(CourseCatalog data,JComboBox<SelectionOption> offering,JComboBox<SelectionOption> lecturer){
        int offeringId=safeId(offering);if(offeringId==0){replaceOptions(lecturer,List.of());return;}
        var assigned=courses.assignedLecturers(actor,offeringId);
        if(!assigned.successful()){replaceOptions(lecturer,List.of());return;}
        java.util.Set<Integer> used=assigned.data().stream().map(LecturerOption::id).collect(java.util.stream.Collectors.toSet());
        replaceOptions(lecturer,lecturerOptions(data).stream().filter(option->!used.contains(option.id())).toList());
    }
    private SelectionOption selected(JComboBox<SelectionOption> box) {
        Object value=box.getSelectedItem(); return value instanceof SelectionOption option && option.id()>0 ? option : null;
    }
    private int id(JComboBox<SelectionOption> box) {
        SelectionOption value=selected(box); if (value==null) throw new IllegalArgumentException("Choose an available record."); return value.id();
    }
    private String courseLabel(CourseRow c) { return c.code()+" — "+c.title(); }
    private String lecturerLabel(LecturerOption l) { return l.username()+" — "+l.fullName()+" ("+l.departmentCode()+")"; }
    private String semesterLabel(SemesterRow s) { return s.academicYear()+" — Semester "+s.semesterNumber(); }
    private String batchLabel(CourseCatalog data, BatchRow b) {
        String dept=data.departments().stream().filter(d -> d.id()==b.departmentId()).map(DepartmentRow::code).findFirst().orElse("Department");
        return b.code()+" — "+dept+" — Level "+b.level()+" — "+b.intakeYear();
    }
    private String offeringLabel(CourseCatalog data, OfferingRow o) {
        String course=data.courses().stream().filter(c -> c.id()==o.courseId()).map(this::courseLabel).findFirst().orElse("Course unavailable");
        String batch=data.batches().stream().filter(b -> b.id()==o.batchId()).map(b -> batchLabel(data,b)).findFirst().orElse("Batch unavailable");
        String semester=data.semesters().stream().filter(s -> s.id()==o.semesterId()).map(this::semesterLabel).findFirst().orElse("Term unavailable");
        return course+" — "+batch+" — "+semester;
    }
    private void addAction(JPanel panel, String label, Runnable action) {
        javax.swing.JButton button=new javax.swing.JButton(label); button.addActionListener(event -> {
            try { action.run(); } catch (RuntimeException invalid) { showError("Check the selected records and entered values: "+invalid.getMessage()); }
        }); panel.add(button);
    }
    private boolean form(String title, String[] labels, java.awt.Component... fields) {
        JPanel panel=new JPanel(new GridLayout(labels.length,2,8,8));
        for(int i=0;i<labels.length;i++){panel.add(new JLabel(labels[i]));panel.add(fields[i]);}
        JScrollPane scroll=new JScrollPane(panel); scroll.setBorder(null); scroll.setPreferredSize(new java.awt.Dimension(620,Math.min(440,labels.length*48+30)));
        return JOptionPane.showConfirmDialog(this,scroll,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION;
    }
    private JTextField field(String hint){JTextField f=new JTextField();f.setToolTipText(hint);return f;}
    private int integer(JTextField f){return Integer.parseInt(f.getText().trim());}
    private double decimal(JTextField f){return Double.parseDouble(f.getText().trim());}
    private String emptyToNull(String value){return value.isBlank()?null:value.trim();}
    private void show(ApiResponse<?> response){JOptionPane.showMessageDialog(this,response.successful()?"Action completed successfully.":response.error().message(),"Course management",response.successful()?JOptionPane.INFORMATION_MESSAGE:JOptionPane.ERROR_MESSAGE);}
    private void showError(String message){JOptionPane.showMessageDialog(this,message,"Course management",JOptionPane.ERROR_MESSAGE);}
}
