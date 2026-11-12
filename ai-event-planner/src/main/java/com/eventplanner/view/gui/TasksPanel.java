package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import java.util.*;
final class TasksPanel {
 private final Dashboard d; TasksPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var names=new DisplayNames(d.session.active());
  var members=Ui.table(d.controller.listMembers(id));Ui.column(members,"Team member",u->names.member(u.getId()));Ui.column(members,"Skills",u->String.join(", ",u.getSkills()));Ui.column(members,"Capacity (hours)",User::getCapacityHours);d.remember(members,"members",User::getId);
  var tasks=Ui.table(d.controller.listTasks(id));Ui.column(tasks,"Task",Task::title);Ui.column(tasks,"Description",Task::description);Ui.column(tasks,"Assigned to",t->names.member(t.assignedUserId()));Ui.column(tasks,"Deadline",Task::deadline);Ui.column(tasks,"Priority",Task::priority);Ui.column(tasks,"Status",Task::status);d.remember(tasks,"tasks",Task::id);
  boolean draft=d.controller.hasTaskDraft(id);
  var edit=Ui.button("Edit assignment",()->d.action(()->{
   Task t=Ui.selected(tasks);new Ui.Form().option("Assigned team member",names.memberOptions(t.assignedUserId()),t.assignedUserId()).dateTime("Work start",t.workStart()).dateTime("Deadline",t.deadline()).show("Edit task assignment",f->{
    String member=f.get("Assigned team member");d.controller.reviseTask(id,t.id(),member.isBlank()?null:member,GuiInputs.date(f.get("Work start")),GuiInputs.date(f.get("Deadline")));d.edited();
   });
  }));
  var status=Ui.button("Change status",()->d.action(()->{Task t=Ui.selected(tasks);new Ui.Form().field("Status",t.status()).show("Task status",f->{d.controller.updateTaskStatus(id,t.id(),GuiInputs.enumeration(Task.Status.class,f.get("Status")));d.edited();});}));
  var approve=Ui.button("Approve draft",()->d.action(()->{if(Ui.confirm("Approve tasks?","Approve the displayed draft assignments after reviewing warnings?"))d.background("Approving tasks",()->{d.controller.approveTasks(id);return null;});}));approve.setDisable(!draft);
  var discard=Ui.button("Discard draft",()->d.action(()->{if(Ui.confirm("Discard task draft?","Retain the previously approved task plan?"))d.controller.discardTasks(id);}));discard.setDisable(!draft);
  return Ui.page("Tasks & planning team","Review workloads, assignments and availability. Changes remain drafts until you explicitly approve them.",
   Ui.card("Planning team",Ui.row(Ui.button("Add team member",()->member(id,null)),Ui.button("Edit selected member",()->d.action(()->member(id,Ui.selected(members))))),Ui.inspectView(members,u->ResultViews.member(u,names))),
   Ui.card("Task plan",Ui.badge(draft?"DRAFT":"APPROVED"),Ui.row(Ui.button("Generate task proposal",()->d.background("Generating tasks",()->d.controller.generateTasks(id))),edit,status,Ui.button("Validate tasks",()->d.validate(id,"tasks")),approve,discard),Ui.inspectView(tasks,t->ResultViews.task(t,names)),ResultViews.warnings("Assignment review",d.diagnostics(id,"tasks"),names)));
 }
 private void member(String id,User existing){
  var u=existing;String memberId=u==null?UUID.randomUUID().toString():u.getId();
  new Ui.Form().field("Name",u==null?"":u.getName()).field("Skills (comma-separated)",u==null?"":String.join(", ",u.getSkills())).field("Preferences (comma-separated)",u==null?"":String.join(", ",u.getPreferences()))
   .availability("Availability",u==null?List.of():u.getAvailability()).field("Existing workload hours",u==null?"0":u.getExistingWorkloadHours()).field("Capacity hours",u==null?"":u.getCapacityHours()).technical("Member reference: "+memberId).show("Configure team member",f->{
    User user=new User(memberId,GuiInputs.required(f.get("Name")),u==null?"":u.getEmail(),u==null?"Planner":u.getRole());
    user.configurePlanning(GuiInputs.csv(f.get("Skills (comma-separated)")),GuiInputs.csv(f.get("Preferences (comma-separated)")),GuiInputs.slots(f.get("Availability")),GuiInputs.number(f.get("Existing workload hours")),GuiInputs.number(f.get("Capacity hours")));
    d.controller.configureMember(id,user);d.edited();
   });
 }
}
