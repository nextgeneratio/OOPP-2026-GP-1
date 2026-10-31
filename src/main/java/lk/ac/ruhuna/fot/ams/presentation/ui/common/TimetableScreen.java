package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.CourseCatalog;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.CourseRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.DepartmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.SemesterRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.TimetableRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.SelectionOption;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Role-scoped timetable actions use readable selector options instead of foreign-key text fields. */
public final class TimetableScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final TimetableController timetables;
    private final CourseController courses;
    private List<EntryRow> loadedEntries = List.of();
    private List<OfferingRow> scopedOfferings = List.of();

    public TimetableScreen(AuthenticatedSession actor, TimetableController timetables, CourseController courses) {
        super(new BorderLayout(12,12)); this.actor=actor; this.timetables=timetables; this.courses=courses;
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        add(new JLabel(actor.role()==Role.ADMIN||actor.role()==Role.TECHNICAL_OFFICER
                ? "<html><h2>Timetable management</h2>Create and maintain authorized department schedules.</html>"
                : "<html><h2>My timetable</h2>View entries authorized for your courses.</html>"),BorderLayout.NORTH);
        JPanel actions=new JPanel(new GridLayout(0,2,10,10));
        boolean manager=actor.role()==Role.ADMIN||actor.role()==Role.TECHNICAL_OFFICER;
        button(actions,manager?"View department timetable":"View my timetable",manager?this::view:this::viewMine);
        if(manager){button(actions,"Create timetable",this::createTimetable);button(actions,"Add timetable entry",()->entryForm(false));
            button(actions,"Update timetable entry",()->entryForm(true));
            if(actor.role()==Role.ADMIN)button(actions,"Remove timetable",this::deleteTimetable);}
        add(new JScrollPane(actions),BorderLayout.CENTER);
    }

    private void view(){
        CourseCatalog data=catalog();if(data==null)return;
        JComboBox<SelectionOption> dept=departments(data),term=semesters(data);JComboBox<Integer> level=levels();JComboBox<String> day=new JComboBox<>(new String[]{"All days","MON","TUE","WED","THU","FRI","SAT","SUN"});
        if(!form("View timetable",new String[]{"Department","Academic term","Level","Day"},dept,term,level,day))return;
        String selected=(String)day.getSelectedItem();var response=timetables.listEntries(actor,id(dept),id(term),(Integer)level.getSelectedItem(),"All days".equals(selected)?null:selected);
        if(!response.successful()){error(response.error().message());return;}display(response.data(),data,"Timetable");
    }
    private void viewMine(){var response=timetables.myEntries(actor);if(!response.successful()){error(response.error().message());return;}display(response.data(),null,"My timetable");}
    private void display(List<EntryRow> entries,CourseCatalog data,String title){
        JTextArea output=new JTextArea(18,72);output.setEditable(false);
        var authorizedOfferings=courses.offeringViewsForActor(actor);
        if(!authorizedOfferings.successful()){error(authorizedOfferings.error().message());return;}
        for(EntryRow e:entries){CourseDao.OfferingRow offering=data==null?null:data.offerings().stream().filter(o->o.id()==e.offeringId()).findFirst().orElse(null);
            CourseRow course=data==null?authorizedOfferings.data().stream().filter(o->o.offering().id()==e.offeringId()).map(lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView::course).findFirst().orElse(null)
                    :offering==null?null:data.courses().stream().filter(c->c.id()==offering.courseId()).findFirst().orElse(null);
            var component=data==null?null:data.components().stream().filter(c->c.id()==e.componentId()).findFirst().orElse(null);
            output.append((course==null?"Course":course.code()+" — "+course.title())+" | "+(component==null?"Component":component.type())
                    +" | "+e.dayOfWeek()+" "+e.start()+"–"+e.end()+" | "+e.location()+"\n");}
        if(entries.isEmpty())output.setText("No timetable entries match this selection.");
        JOptionPane.showMessageDialog(this,new JScrollPane(output),title,JOptionPane.INFORMATION_MESSAGE);
    }
    private void createTimetable(){
        CourseCatalog data=catalog();if(data==null)return;JTextField title=new JTextField(24);JComboBox<SelectionOption> dept=departments(data),term=semesters(data);JComboBox<Integer> level=levels();
        if(!form("Create timetable",new String[]{"Title","Department","Academic term","Level"},title,dept,term,level))return;
        show(timetables.create(actor,new TimetableRow(0,title.getText().trim(),id(dept),id(term),(Integer)level.getSelectedItem())));
    }
    private void entryForm(boolean update){
        CourseCatalog data=catalog();if(data==null)return;JComboBox<SelectionOption> dept=departments(data),term=semesters(data);JComboBox<Integer> level=levels();
        JComboBox<SelectionOption> timetableBox=new JComboBox<>(), offeringBox=new JComboBox<>(), componentBox=new JComboBox<>(), entryBox=new JComboBox<>();
        offeringBox.addActionListener(e->{Integer selected=selectionId(offeringBox);OfferingRow o=scopedOfferings.stream().filter(x->x.id()==selected).findFirst().orElse(null);
            if(o==null){fill(componentBox,List.of());return;}fill(componentBox,data.components().stream().filter(c->c.courseId()==o.courseId())
                    .map(c->new SelectionOption(c.id(),c.type()+" — "+c.credits()+" credits")).toList());});
        Runnable refresh=()->refreshChoices(data,dept,term,level,timetableBox,offeringBox,componentBox,entryBox,update);
        dept.addActionListener(e->refresh.run());term.addActionListener(e->refresh.run());level.addActionListener(e->refresh.run());refresh.run();
        JComboBox<String> day=new JComboBox<>(new String[]{"MON","TUE","WED","THU","FRI","SAT","SUN"});
        JTextField start=new JTextField("08:00",10),end=new JTextField("10:00",10),location=new JTextField(20);
        if(update)entryBox.addActionListener(e->{EntryRow row=entryValue(entryBox);if(row!=null){selectId(timetableBox,row.timetableId());selectId(offeringBox,row.offeringId());selectId(componentBox,row.componentId());day.setSelectedItem(row.dayOfWeek());start.setText(row.start().toString());end.setText(row.end().toString());}});
        String[] labels=update?new String[]{"Department","Academic term","Level","Entry","Timetable","Offering","Component","Day","Start HH:mm","End HH:mm","Location"}
                :new String[]{"Department","Academic term","Level","Timetable","Offering","Component","Day","Start HH:mm","End HH:mm","Location"};
        java.awt.Component[] fields=update?new java.awt.Component[]{dept,term,level,entryBox,timetableBox,offeringBox,componentBox,day,start,end,location}
                :new java.awt.Component[]{dept,term,level,timetableBox,offeringBox,componentBox,day,start,end,location};
        if(!form(update?"Update timetable entry":"Add timetable entry",labels,fields))return;
        EntryRow existing=update?entryValue(entryBox):null;if(update&&existing==null){error("Select an existing timetable entry.");return;}
        EntryRow row=new EntryRow(update?existing.id():0,id(timetableBox),id(offeringBox),id(componentBox),(String)day.getSelectedItem(),
                LocalTime.parse(start.getText().trim()),LocalTime.parse(end.getText().trim()),location.getText().trim());
        show(update?timetables.updateEntry(actor,new TimetableController.EntryRequest(row)):timetables.addEntry(actor,new TimetableController.EntryRequest(row)));
    }
    private void refreshChoices(CourseCatalog data,JComboBox<SelectionOption> dept,JComboBox<SelectionOption> term,JComboBox<Integer> level,
            JComboBox<SelectionOption> timetableBox,JComboBox<SelectionOption> offeringBox,JComboBox<SelectionOption> componentBox,
            JComboBox<SelectionOption> entryBox,boolean update){
        Integer d=selectionId(dept),s=selectionId(term);Integer l=(Integer)level.getSelectedItem();
        fill(timetableBox,List.of());fill(offeringBox,List.of());fill(componentBox,List.of());if(update)fill(entryBox,List.of());
        loadedEntries=List.of();scopedOfferings=List.of();
        if(d==null||s==null||l==null)return;
        ApiResponse<List<TimetableRow>> found=timetables.timetables(actor,d,s,l);
        if(!found.successful()){return;}
        fill(timetableBox,found.data().stream().map(t->new SelectionOption(t.id(),t.title()+" — Level "+t.level())).toList());
        List<OfferingRow> offerings=data.offerings().stream().filter(o->o.semesterId()==s&&data.batches().stream()
                .anyMatch(b->b.id()==o.batchId()&&b.departmentId()==d&&b.level()==l)).toList();
        scopedOfferings=offerings;
        fill(offeringBox,offerings.stream().map(o->new SelectionOption(o.id(),offeringLabel(data,o))).toList());
        if(update){var entries=timetables.listEntries(actor,d,s,l,null);if(entries.successful()){loadedEntries=entries.data();fill(entryBox,loadedEntries.stream()
                .map(e->new SelectionOption(e.id(),entryLabel(data,e))).toList());}}
    }
    private void deleteTimetable(){CourseCatalog data=catalog();if(data==null)return;JComboBox<SelectionOption> dept=departments(data),term=semesters(data);JComboBox<Integer> level=levels();
        if(!form("Select timetable",new String[]{"Department","Academic term","Level"},dept,term,level))return;
        var response=timetables.timetables(actor,id(dept),id(term),(Integer)level.getSelectedItem());if(!response.successful()){error(response.error().message());return;}
        JComboBox<SelectionOption> choice=options(response.data().stream().map(t->new SelectionOption(t.id(),t.title()+" — Level "+t.level())).toList());
        if(!form("Remove timetable",new String[]{"Timetable"},choice))return;
        if(JOptionPane.showConfirmDialog(this,"Remove this timetable and its entries?","Confirm removal",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)show(timetables.delete(actor,id(choice)));
    }
    private CourseCatalog catalog(){ApiResponse<CourseCatalog> r=courses.catalog(actor);if(!r.successful()){error(r.error().message());return null;}return r.data();}
    private JComboBox<SelectionOption> departments(CourseCatalog c){return options(c.departments().stream().map(d->new SelectionOption(d.id(),d.code()+" — "+d.name())).toList());}
    private JComboBox<SelectionOption> semesters(CourseCatalog c){return options(c.semesters().stream().map(s->new SelectionOption(s.id(),s.academicYear()+" — Semester "+s.semesterNumber())).toList());}
    private JComboBox<Integer> levels(){return new JComboBox<>(new Integer[]{1,2,3,4});}
    private JComboBox<SelectionOption> options(List<SelectionOption> values){JComboBox<SelectionOption>b=new JComboBox<>();fill(b,values);SelectionOption.enableSearch(b);return b;}
    private void fill(JComboBox<SelectionOption>b,List<SelectionOption>values){b.removeAllItems();values.forEach(b::addItem);if(values.isEmpty())b.addItem(new SelectionOption(0,"No matching records"));}
    private Integer selectionId(JComboBox<SelectionOption>b){Object x=b.getSelectedItem();return x instanceof SelectionOption o&&o.id()>0?o.id():null;}
    private int id(JComboBox<SelectionOption>b){Integer x=selectionId(b);if(x==null)throw new IllegalArgumentException("Choose a matching record.");return x;}
    private void selectId(JComboBox<SelectionOption>b,int id){for(int i=0;i<b.getItemCount();i++)if(b.getItemAt(i).id()==id){b.setSelectedIndex(i);return;}}
    private EntryRow entryValue(JComboBox<SelectionOption>b){Integer x=selectionId(b);if(x==null)return null;return loadedEntries.stream().filter(r->r.id()==x).findFirst().orElse(null);}
    private String offeringLabel(CourseCatalog d,OfferingRow o){CourseRow c=d.courses().stream().filter(x->x.id()==o.courseId()).findFirst().orElse(null);
        var batch=d.batches().stream().filter(x->x.id()==o.batchId()).findFirst().orElse(null);var sem=d.semesters().stream().filter(x->x.id()==o.semesterId()).findFirst().orElse(null);
        return (c==null?"Course":c.code()+" — "+c.title())+" — "+(batch==null?"Batch":batch.code()+" Level "+batch.level())+" — "+(sem==null?"Term":sem.academicYear()+" Semester "+sem.semesterNumber());}
    private String entryLabel(CourseCatalog d,EntryRow e){var c=d.components().stream().filter(x->x.id()==e.componentId()).findFirst().orElse(null);return offeringLabel(d,d.offerings().stream().filter(o->o.id()==e.offeringId()).findFirst().orElseThrow())+" — "+(c==null?"Component":c.type())+" — "+e.dayOfWeek()+" "+e.start();}
    private boolean form(String title,String[]labels,java.awt.Component...fields){JPanel p=new JPanel(new GridLayout(labels.length,2,8,8));for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));p.add(fields[i]);}JScrollPane scroll=new JScrollPane(p);scroll.setBorder(null);scroll.setPreferredSize(new java.awt.Dimension(650,Math.min(500,labels.length*48+24)));return JOptionPane.showConfirmDialog(this,scroll,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION;}
    private void button(JPanel p,String text,Runnable action){javax.swing.JButton b=new javax.swing.JButton(text);b.addActionListener(e->{try{action.run();}catch(RuntimeException x){error("Check the selected records and time values: "+x.getMessage());}});p.add(b);}
    private void show(ApiResponse<?>r){if(r.successful())JOptionPane.showMessageDialog(this,"Action completed successfully.","Timetable",JOptionPane.INFORMATION_MESSAGE);else error(r.error().message());}
    private void error(String message){JOptionPane.showMessageDialog(this,message,"Timetable",JOptionPane.ERROR_MESSAGE);}
}
