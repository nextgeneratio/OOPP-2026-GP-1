package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Shows calculated eligibility; student identity is derived from their active enrolment. */
public final class EligibilityScreen extends JPanel {
    private final AuthenticatedSession actor; private final CourseController courses;
    private final AttendanceController attendance; private final EligibilityController eligibility;
    public EligibilityScreen(AuthenticatedSession actor, CourseController courses, AttendanceController attendance,
                             EligibilityController eligibility) {
        super(new BorderLayout(12,12)); this.actor=actor; this.courses=courses; this.attendance=attendance; this.eligibility=eligibility;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20,20,20,20));
        add(new JLabel("<html><h2>Exam eligibility</h2>Eligibility is calculated from saved CA marks and attendance; it cannot be edited here.</html>"), BorderLayout.NORTH);
        javax.swing.JButton evaluate = new javax.swing.JButton("Evaluate eligibility"); evaluate.addActionListener(e -> evaluate()); add(evaluate, BorderLayout.CENTER);
    }
    private JComboBox<OfferingView> offerings() { var r=courses.offeringViewsForActor(actor); if(!r.successful()) throw new IllegalArgumentException(r.error().message()); JComboBox<OfferingView> b=new JComboBox<>(r.data().toArray(OfferingView[]::new)); b.setRenderer(new javax.swing.DefaultListCellRenderer(){ @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l,Object v,int i,boolean s,boolean f){super.getListCellRendererComponent(l,v,i,s,f);if(v instanceof OfferingView o)setText(o.course().code()+" — "+o.course().title());return this;}}); return b; }
    private void evaluate() {
        JComboBox<OfferingView> offering=offerings();
        var rosterBox = new JComboBox<lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow>();
        offering.addActionListener(e -> loadRoster(rosterBox, (OfferingView) offering.getSelectedItem()));
        loadRoster(rosterBox,(OfferingView)offering.getSelectedItem());
        JPanel p=new JPanel(new GridLayout(0,2,8,8));p.add(new JLabel("Course offering"));p.add(offering);p.add(new JLabel(actor.role()==Role.UNDERGRADUATE?"My enrolment":"Student enrolment"));p.add(rosterBox);
        if(JOptionPane.showConfirmDialog(this,p,"Evaluate eligibility",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        var o=(OfferingView)offering.getSelectedItem(); var e=(lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow)rosterBox.getSelectedItem(); if(e==null){error("No active enrolment is available.");return;}
        var result=eligibility.evaluate(actor,new EligibilityController.EligibilityRequest(e.studentId(),e.id(),o.offering().id()));
        if(!result.successful()){error(result.error().message());return;} var data=result.data();
        String reasons=data.failedConditions().isEmpty()?"None":String.join("\n",data.failedConditions());
        JOptionPane.showMessageDialog(this,"Overall: "+(data.eligible()?"Eligible":"Not eligible")+"\nCA: "+(data.caEligible()?"Eligible":"Not eligible")+"\nAttendance: "+(data.attendanceEligible()?"Eligible":"Not eligible")+"\nReasons:\n"+reasons,"Eligibility",JOptionPane.INFORMATION_MESSAGE);
    }
    private void loadRoster(JComboBox<lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow> box, OfferingView offering){box.removeAllItems();if(offering==null)return;var r=attendance.activeEnrollments(actor,offering.offering().id());if(!r.successful())return;r.data().forEach(box::addItem);box.setRenderer(new javax.swing.DefaultListCellRenderer(){@Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l,Object v,int i,boolean s,boolean f){super.getListCellRendererComponent(l,v,i,s,f);if(v instanceof lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow x)setText("Student "+x.studentId()+" — enrolment "+x.id());return this;}});}
    private void error(String text){JOptionPane.showMessageDialog(this,text,"Eligibility",JOptionPane.ERROR_MESSAGE);}
}
