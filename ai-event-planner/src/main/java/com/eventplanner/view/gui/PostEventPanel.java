package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import java.util.*;

final class PostEventPanel {
 private final Dashboard d;
 PostEventPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var state=d.partB(id);Event event=d.session.active();var names=new DisplayNames(event);PostEventData saved=event.getPostEventData();
  if(!state.outcomesLoaded){
   if(saved!=null){
    state.inputs.put("actualAttendance",saved.actualAttendance()==null?"":saved.actualAttendance().toString());
    state.inputs.put("actualExpenses",saved.actualExpenses()==null?"":saved.actualExpenses().toPlainString());
    state.inputs.put("completed",String.join(", ",saved.completedTaskIds()));state.inputs.put("unfinished",String.join(", ",saved.unfinishedTaskIds()));
    state.inputs.put("organizer",String.join("\n",saved.organizerFeedback()));state.inputs.put("attendee",String.join("\n",saved.attendeeFeedback()));state.inputs.put("problems",String.join("\n",saved.problems()));
   }
   state.outcomesLoaded=true;
  }
  var attendance=state.field("actualAttendance","Non-negative whole number; blank = UNKNOWN");
  var expenses=state.field("actualExpenses","Non-negative amount; blank = UNKNOWN");
  var taskOutcomes=new TaskOutcomesInput(event.getTasks(),state,names);
  var organizer=state.area("organizer","One organizer feedback entry per line");
  var attendee=state.area("attendee","One attendee feedback entry per line");
  var problems=state.area("problems","One problem per line; blank = unrecorded");
  java.util.function.Supplier<PostEventData> input=()->GuiInputs.postEventData(attendance.getText(),expenses.getText(),state.inputs.getOrDefault("completed",""),state.inputs.getOrDefault("unfinished",""),organizer.getText(),attendee.getText(),problems.getText());
  var analyze=Ui.button("Generate post-event analysis",()->d.action(()->{
   if(!input.get().equals(saved))throw new IllegalArgumentException("Save actual outcomes before generating analysis. The report uses saved outcomes only.");
   d.background("Analyzing post-event outcomes",()->d.controller.analyzePostEvent(id),result->state.selectedReport=-1);
  }));analyze.setDisable(saved==null);
  var reports=d.controller.viewPostEventReports(id);var indices=new ArrayList<Integer>();for(int i=0;i<reports.size();i++)indices.add(i);
  var history=Ui.choice(indices);history.setConverter(new StringConverter<>(){
   public String toString(Integer index){return index==null?"":"Report "+(index+1)+" | "+reports.get(index).executiveSummary();}
   public Integer fromString(String value){return null;}
  });
  int selected=state.selectedReport>=0&&state.selectedReport<reports.size()?state.selectedReport:reports.size()-1;
  if(selected>=0)history.setValue(selected);history.setPromptText("No stored reports yet");
  var reportText=new javafx.scene.layout.VBox(14,selected<0?Ui.note("Save actual outcomes, then generate an analysis. Reports remain in this session."):ResultViews.report(reports.get(selected),event.getPlanningCurrency(),names));
  history.valueProperty().addListener((o,a,b)->{if(b!=null){state.selectedReport=b;reportText.getChildren().setAll(ResultViews.report(reports.get(b),event.getPlanningCurrency(),names));}});
  return Ui.page("Post-Event Review","Blank actual attendance and expenses mean UNKNOWN, never zero. Save outcomes before generating analysis. Feedback entries remain attributed to their source.",
   Ui.card("Actual outcomes in "+event.getPlanningCurrency(),new Label("Actual attendance"),attendance,new Label("Actual expenses"),expenses,
    Ui.card("Approved task outcomes",taskOutcomes),
    new Label("Organizer feedback"),organizer,new Label("Attendee feedback"),attendee,new Label("Problems encountered"),problems,
    Ui.row(Ui.button("Save actual outcomes",()->d.action(()->{
     PostEventData data=input.get();
     if(Ui.confirm("Save post-event outcomes?","Save these outcomes? Blank actual values remain UNKNOWN. Existing saved outcomes will be replaced."))
      d.background("Saving post-event outcomes",()->{d.controller.recordPostEvent(id,data);return null;});
    })),analyze),Ui.note(saved==null?"No saved outcomes yet.":"Saved attendance: "+known(saved.actualAttendance())+" | Saved expenses: "+known(saved.actualExpenses())+" "+event.getPlanningCurrency())),
   Ui.card("Stored post-event reports",history,reportText));
 }
 private static String known(Object value){return value==null?"UNKNOWN":value.toString();}
 static String describe(PostEventReport report,String currency){
  var data=report.outcomes();
  return report.executiveSummary()+"\n\nPlanned attendance: "+report.expectedAttendance()+"\nActual attendance: "+known(data.actualAttendance())
   +"\nAttendance difference (actual - planned): "+known(report.attendanceDifference())
   +"\nApproved planned expenses: "+known(report.plannedBudget())+" "+currency+"\nActual expenses: "+known(data.actualExpenses())+" "+currency
   +"\nExpense variance (actual - planned): "+known(report.expenseDifference())+" "+currency
   +"\nCompleted tasks: "+report.completedTasks()+" | Unfinished tasks: "+report.unfinishedTasks()
   +"\nTask completion rate: "+(report.completionRate()==null?"UNKNOWN":report.completionRate()+"%")
   +"\nOther task outcomes may be unrecorded; see missing information below."
   +Ui.lines("Recorded organizer feedback",data.organizerFeedback())+Ui.lines("Recorded attendee feedback",data.attendeeFeedback())
   +Ui.lines("Recorded problems",data.problems())+Ui.lines("Successes",report.successes())+Ui.lines("Issues",report.issues())
   +Ui.lines("Lessons learned",report.lessonsLearned())+Ui.lines("Recommendations",report.recommendations())+Ui.lines("Missing / unknown information",report.missingInformation());
 }
}
