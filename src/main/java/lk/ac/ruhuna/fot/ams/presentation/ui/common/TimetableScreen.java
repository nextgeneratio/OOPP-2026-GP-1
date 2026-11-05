package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.CourseCatalog;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.DepartmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.SemesterRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.TimetableRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Weekly timetable workflows. Only administrators can maintain schedules. */
public final class TimetableScreen extends JPanel {
    private static final String[] DAYS={"MON","TUE","WED","THU","FRI","SAT","SUN"};
    private final AuthenticatedSession actor;
    private final TimetableController timetables;
    private final CourseController courses;

    public TimetableScreen(AuthenticatedSession actor,TimetableController timetables,CourseController courses){
        super(new BorderLayout(12,12)); this.actor=actor;this.timetables=timetables;this.courses=courses;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20,20,20,20));
        boolean admin=actor.role()==Role.ADMIN;
        add(new JLabel(admin?"<html><h2>Weekly timetable management</h2>Create and maintain the complete weekly schedule for a department, term, and level.</html>":"<html><h2>Timetable</h2>View timetable entries available to your role.</html>"),BorderLayout.NORTH);
        JPanel actions=new JPanel(new java.awt.GridLayout(0,2,12,12));
        if(admin){button(actions,"Create Timetable",this::create);button(actions,"Update Timetable",this::update);button(actions,"Delete Timetable",this::delete);button(actions,"View Timetable",this::viewAdmin);}
        else button(actions,actor.role()==Role.TECHNICAL_OFFICER?"View Department Timetable":"View My Timetable",actor.role()==Role.TECHNICAL_OFFICER?this::viewOfficer:this::viewMine);
        add(actions,BorderLayout.CENTER);
    }

    private void create(){CourseCatalog catalog=catalog();if(catalog==null)return;EditorResult edit=editSlots("Create Weekly Timetable",catalog,List.of(),null);if(edit==null)return;Context context=edit.context;
        String title=context.department.label().split(" — ")[0]+" — Level "+context.level+" — "+context.semester.label();
        show(timetables.createWeekly(actor,new TimetableController.WeeklyRequest(new TimetableRow(0,title,context.department.id(),context.semester.id(),context.level),edit.slots)));
    }

    private void update(){TimetableRow selected=selectTimetable("Update Timetable");if(selected==null)return;
        var loaded=timetables.entriesForTimetable(actor,selected.id());if(!loaded.successful()){error(loaded.error().message());return;}
        CourseCatalog catalog=catalog();if(catalog==null)return;Context fixed=new Context(new SelectionOption(selected.departmentId(),departmentLabel(catalog,selected.departmentId())),new SelectionOption(selected.semesterId(),semesterLabel(catalog,selected.semesterId())),selected.level());
        EditorResult edit=editSlots("Update Weekly Timetable — "+selected.title(),catalog,loaded.data(),fixed);if(edit==null)return;
        show(timetables.updateWeekly(actor,new TimetableController.WeeklyUpdateRequest(selected.id(),edit.slots)));
    }

    private void delete(){TimetableRow selected=selectTimetable("Delete Timetable");if(selected==null)return;
        var slots=timetables.entriesForTimetable(actor,selected.id());if(!slots.successful()){error(slots.error().message());return;}
        var counts=timetables.deletionSummary(actor,selected.id());if(!counts.successful()){error(counts.error().message());return;}
        String summary=selected.title()+"\nWeekly slots: "+counts.data().activeSlots()+"\nAttendance sessions referencing slots: "+counts.data().attendanceReferences();
        int confirm=JOptionPane.showConfirmDialog(this,"Deactivate this timetable?\n\n"+summary,"Confirm timetable deactivation",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(confirm==JOptionPane.YES_OPTION)show(timetables.delete(actor,selected.id()));
    }

    private void viewAdmin(){CourseCatalog catalog=catalog();if(catalog==null)return;Context context=context(catalog,"View Timetable",null);if(context==null)return;
        var found=timetables.timetables(actor,context.department.id(),context.semester.id(),context.level);
        if(!found.successful()){error(found.error().message());return;}if(found.data().isEmpty()){info("No active timetable exists for this department, term, and level.");return;}
        TimetableRow selected=choose(found.data(),"Select Timetable");if(selected!=null)showWeek(selected);
    }

    private void viewMine(){var response=timetables.myEntries(actor);if(!response.successful()){error(response.error().message());return;}showWeek("My Timetable",response.data());}
    private void viewOfficer(){CourseCatalog c=catalog();if(c==null)return;Context x=context(c,"View Department Timetable",null);if(x==null)return;var response=timetables.timetables(actor,x.department.id(),x.semester.id(),x.level);if(!response.successful()){error(response.error().message());return;}if(response.data().isEmpty()){info("No active timetable matches this authorized department scope.");return;}TimetableRow selected=choose(response.data(),"Select Timetable");if(selected!=null)showWeek(selected);}
    private void showWeek(TimetableRow timetable){var response=timetables.entriesForTimetable(actor,timetable.id());if(!response.successful()){error(response.error().message());return;}showWeek(timetable.title(),response.data());}
    private void showWeek(String title,List<EntryRow> slots){
        String[] headers={"Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        DefaultTableModel model=new DefaultTableModel(headers,0){@Override public boolean isCellEditable(int r,int c){return false;}};
        record Range(LocalTime start,LocalTime end){}
        List<Range> ranges=slots.stream().map(e->new Range(e.start(),e.end())).distinct().sorted(Comparator.comparing(Range::start).thenComparing(Range::end)).toList();
        for(Range range:ranges){Object[] row=new Object[8];row[0]=range.start+"–"+range.end;for(int i=1;i<row.length;i++)row[i]="—";
            for(EntryRow e:slots)if(e.start().equals(range.start)&&e.end().equals(range.end)){int col=java.util.Arrays.asList(DAYS).indexOf(e.dayOfWeek())+1;if(col>0)row[col]=e.location()==null||e.location().isBlank()?"Allocated":e.location();}
            model.addRow(row);
        }
        if(slots.isEmpty())model.addRow(new Object[]{"No allocated slots","—","—","—","—","—","—","—"});
        JTable table=new JTable(model);table.setRowHeight(26);JOptionPane.showMessageDialog(this,new JScrollPane(table),title,JOptionPane.PLAIN_MESSAGE);
    }

    private record EditorResult(Context context,List<EntryRow> slots){}
    private EditorResult editSlots(String title,CourseCatalog catalog,List<EntryRow> initial,Context fixed){
        DefaultTableModel model=new DefaultTableModel(new Object[]{"Day","Start (24h)","End (24h)","Room / location (optional)"},0){
            @Override public boolean isCellEditable(int row,int col){return true;}
            @Override public Class<?> getColumnClass(int col){return String.class;}
        };
        for(EntryRow e:initial)model.addRow(new Object[]{e.dayOfWeek(),e.start().toString(),e.end().toString(),e.location()==null?"":e.location()});
        JTable table=new JTable(model);table.setRowHeight(25);table.getColumnModel().getColumn(0).setCellEditor(new javax.swing.DefaultCellEditor(new JComboBox<>(DAYS)));
        JPanel panel=new JPanel(new BorderLayout(8,8));JPanel header=new JPanel(new GridBagLayout());
        JComboBox<SelectionOption> department=options(catalog.departments().stream().map(d->new SelectionOption(d.id(),d.code()+" — "+d.name())).toList());
        JComboBox<SelectionOption> semester=options(catalog.semesters().stream().map(s->new SelectionOption(s.id(),s.academicYear()+" — Semester "+s.semesterNumber())).toList());
        JComboBox<Integer> level=new JComboBox<>(new Integer[]{1,2,3,4});
        if(fixed!=null){department.setSelectedItem(fixed.department);semester.setSelectedItem(fixed.semester);level.setSelectedItem(fixed.level);department.setEnabled(false);semester.setEnabled(false);level.setEnabled(false);}
        addField(header,0,"Department",department);addField(header,1,"Academic term",semester);addField(header,2,"Level",level);
        addField(header,3,"Weekly schedule",new JLabel("Add one row per time slot; days without rows remain unallocated."));panel.add(header,BorderLayout.NORTH);
        panel.add(new JScrollPane(table),BorderLayout.CENTER);
        JButton add=new JButton("Add Time Slot"),remove=new JButton("Remove Selected Slot");JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.add(add);controls.add(remove);panel.add(controls,BorderLayout.SOUTH);
        add.addActionListener(e->model.addRow(new Object[]{"MON","08:00","10:00",""}));remove.addActionListener(e->{int row=table.getSelectedRow();if(row>=0)model.removeRow(table.convertRowIndexToModel(row));});
        panel.setPreferredSize(new java.awt.Dimension(720,390));
        int choice=JOptionPane.showConfirmDialog(this,new JScrollPane(panel),title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);if(choice!=JOptionPane.OK_OPTION)return null;
        if(table.isEditing())table.getCellEditor().stopCellEditing();List<EntryRow> result=new ArrayList<>();
        try{for(int r=0;r<model.getRowCount();r++){String day=String.valueOf(model.getValueAt(r,0)).trim();String start=String.valueOf(model.getValueAt(r,1)).trim();String end=String.valueOf(model.getValueAt(r,2)).trim();String location=String.valueOf(model.getValueAt(r,3)).trim();
                result.add(new EntryRow(0,0,0,0,day,LocalTime.parse(start),LocalTime.parse(end),location.isBlank()?null:location));}}
        catch(RuntimeException invalid){error("Use a weekday and 24-hour times such as 08:00 and 10:00. End time must be later than start time.");return null;}
        Object d=department.getSelectedItem(),s=semester.getSelectedItem();if(!(d instanceof SelectionOption dep)||!(s instanceof SelectionOption sem)||level.getSelectedItem()==null){error("Choose a department, academic term, and level.");return null;}
        return new EditorResult(new Context(dep,sem,(Integer)level.getSelectedItem()),result);
    }

    private void addField(JPanel panel,int row,String label,java.awt.Component field){GridBagConstraints l=new GridBagConstraints();l.gridx=0;l.gridy=row;l.anchor=GridBagConstraints.LINE_START;l.insets=new Insets(4,5,4,12);panel.add(new JLabel(label),l);GridBagConstraints f=new GridBagConstraints();f.gridx=1;f.gridy=row;f.weightx=1;f.fill=GridBagConstraints.HORIZONTAL;f.insets=new Insets(4,0,4,5);panel.add(field,f);}

    private record Context(SelectionOption department,SelectionOption semester,int level){}
    private Context context(CourseCatalog catalog,String title,Context preset){
        JComboBox<SelectionOption> department=options(catalog.departments().stream().map(d->new SelectionOption(d.id(),d.code()+" — "+d.name())).toList());
        JComboBox<SelectionOption> semester=options(catalog.semesters().stream().map(s->new SelectionOption(s.id(),s.academicYear()+" — Semester "+s.semesterNumber())).toList());
        JComboBox<Integer> level=new JComboBox<>(new Integer[]{1,2,3,4});
        if(!form(title,new String[]{"Department","Academic term","Level"},department,semester,level))return null;
        SelectionOption d=(SelectionOption)department.getSelectedItem(),s=(SelectionOption)semester.getSelectedItem();if(d==null||s==null)return null;return new Context(d,s,(Integer)level.getSelectedItem());
    }
    private TimetableRow selectTimetable(String title){CourseCatalog c=catalog();if(c==null)return null;Context x=context(c,title,null);if(x==null)return null;
        var response=timetables.timetables(actor,x.department.id(),x.semester.id(),x.level);if(!response.successful()){error(response.error().message());return null;}
        if(response.data().isEmpty()){info("No active timetable matches the selected department, term, and level.");return null;}return choose(response.data(),title);
    }
    private TimetableRow choose(List<TimetableRow> choices,String title){CourseCatalog c=catalog();if(c==null)return null;List<SelectionOption> labels=choices.stream().map(t->new SelectionOption(t.id(),departmentLabel(c,t.departmentId())+" — "+semesterLabel(c,t.semesterId())+" — Level "+t.level()+" — "+t.title())).toList();
        JComboBox<SelectionOption> picker=options(labels);if(!form(title,new String[]{"Timetable"},picker))return null;int id=((SelectionOption)picker.getSelectedItem()).id();return choices.stream().filter(t->t.id()==id).findFirst().orElse(null);}
    private CourseCatalog catalog(){ApiResponse<CourseCatalog> response=courses.catalog(actor);if(!response.successful()){error(response.error().message());return null;}return response.data();}
    private String departmentLabel(CourseCatalog c,int id){return c.departments().stream().filter(d->d.id()==id).findFirst().map(d->d.code()+" — "+d.name()).orElse("Department");}
    private String semesterLabel(CourseCatalog c,int id){return c.semesters().stream().filter(s->s.id()==id).findFirst().map(s->s.academicYear()+" — Semester "+s.semesterNumber()).orElse("Academic term");}
    private JComboBox<SelectionOption> options(List<SelectionOption> list){JComboBox<SelectionOption> box=new JComboBox<>(list.toArray(SelectionOption[]::new));SelectionOption.enableSearch(box);return box;}
    private boolean form(String title,String[] labels,java.awt.Component...fields){JPanel panel=new JPanel(new GridBagLayout());for(int i=0;i<labels.length;i++){
        GridBagConstraints l=new GridBagConstraints();l.gridx=0;l.gridy=i;l.anchor=GridBagConstraints.LINE_START;l.insets=new Insets(7,6,7,12);panel.add(new JLabel(labels[i]),l);
        GridBagConstraints f=new GridBagConstraints();f.gridx=1;f.gridy=i;f.weightx=1;f.fill=GridBagConstraints.HORIZONTAL;f.insets=new Insets(7,0,7,6);panel.add(fields[i],f);}
        return JOptionPane.showConfirmDialog(this,panel,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION;}
    private String dayLabel(String day){return switch(day){case "MON"->"Monday";case "TUE"->"Tuesday";case "WED"->"Wednesday";case "THU"->"Thursday";case "FRI"->"Friday";case "SAT"->"Saturday";case "SUN"->"Sunday";default->day;};}
    private void button(JPanel panel,String label,Runnable action){JButton button=new JButton(label);button.setPreferredSize(new java.awt.Dimension(240,90));button.addActionListener(e->{try{action.run();}catch(RuntimeException failure){error(failure.getMessage());}});panel.add(button);}
    private void show(ApiResponse<?> response){if(response.successful())info("Timetable action completed successfully.");else error(response.error().message());}
    private void error(String message){JOptionPane.showMessageDialog(this,message,"Timetable",JOptionPane.ERROR_MESSAGE);}private void info(String message){JOptionPane.showMessageDialog(this,message,"Timetable",JOptionPane.INFORMATION_MESSAGE);}
}
