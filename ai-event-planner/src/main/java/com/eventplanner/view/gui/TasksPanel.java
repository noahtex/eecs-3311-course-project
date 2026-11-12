package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import java.util.*;
final class TasksPanel {
 private final Dashboard d; TasksPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var members=Ui.table(d.controller.listMembers(id));Ui.column(members,"Member ID",User::getId);Ui.column(members,"Name",User::getName);Ui.column(members,"Skills",User::getSkills);Ui.column(members,"Capacity (h)",User::getCapacityHours);
  var tasks=Ui.table(d.controller.listTasks(id));Ui.column(tasks,"Task",Task::title);Ui.column(tasks,"Assignee ID",Task::assignedUserId);Ui.column(tasks,"Deadline",Task::deadline);Ui.column(tasks,"Priority",Task::priority);Ui.column(tasks,"Status",Task::status);
  boolean draft=d.controller.hasTaskDraft(id);
  var edit=Ui.button("Edit assignment",()->d.action(()->{
   Task t=Ui.selected(tasks);new Ui.Form().field("Member ID (blank = unassigned)",t.assignedUserId()).field("Work start",t.workStart()).field("Deadline",t.deadline()).show("Edit task assignment",f->{
    String member=f.get("Member ID (blank = unassigned)");d.controller.reviseTask(id,t.id(),member.isBlank()?null:member,GuiInputs.date(f.get("Work start")),GuiInputs.date(f.get("Deadline")));d.edited();
   });
  }));
  var status=Ui.button("Change status",()->d.action(()->{Task t=Ui.selected(tasks);new Ui.Form().field("Status (TODO / IN_PROGRESS / DONE)",t.status()).show("Task status",f->{d.controller.updateTaskStatus(id,t.id(),GuiInputs.enumeration(Task.Status.class,f.get("Status (TODO / IN_PROGRESS / DONE)")));d.edited();});}));
  var approve=Ui.button("Approve draft",()->d.action(()->{if(Ui.confirm("Approve tasks?","Approve the displayed draft assignments after reviewing warnings?"))d.background("Approving tasks",()->{d.controller.approveTasks(id);return null;});}));approve.setDisable(!draft);
  var discard=Ui.button("Discard draft",()->d.action(()->{if(Ui.confirm("Discard task draft?","Retain the previously approved task plan?"))d.controller.discardTasks(id);}));discard.setDisable(!draft);
  var editMember=Ui.button("Edit selected member",()->d.action(()->member(id,Ui.selected(members))));
  return Ui.page("Tasks & planning team","Assignments and status edits remain drafts until approval. Availability is one start/end ISO datetime pair per line; blank means unknown.",
   Ui.card("Team",Ui.row(Ui.button("Add team member",()->member(id,null)),editMember),Ui.inspect(members,u->u.getName()+" | "+u.getId()+Ui.lines("Skills",u.getSkills())+Ui.lines("Preferences",u.getPreferences())+Ui.lines("Availability",u.getAvailability())+"\nExisting workload: "+u.getExistingWorkloadHours()+" h; capacity: "+u.getCapacityHours()+" h")),
   Ui.card(draft?"DRAFT task plan":"APPROVED tasks",Ui.row(Ui.button("Generate task proposal",()->d.background("Generating tasks",()->d.controller.generateTasks(id))),edit,status,Ui.button("Validate tasks",()->d.validate(id,"tasks")),approve,discard),Ui.inspect(tasks,t->t.toString()),Ui.details(d.diagnostics(id,"tasks"))));
 }
 private void member(String id,User existing){
  var u=existing;new Ui.Form().field("Member ID",u==null?"":u.getId()).field("Name",u==null?"":u.getName()).field("Skills (comma-separated)",u==null?"":String.join(", ",u.getSkills())).field("Preferences (comma-separated)",u==null?"":String.join(", ",u.getPreferences()))
   .field("Availability (start/end per line)",u==null?"":GuiInputs.slotsText(u.getAvailability())).field("Existing workload hours",u==null?"0":u.getExistingWorkloadHours()).field("Capacity hours",u==null?"":u.getCapacityHours()).show("Configure team member",f->{
    String memberId=GuiInputs.required(f.get("Member ID"));if(u!=null&&!memberId.equals(u.getId()))throw new IllegalArgumentException("Keep the existing member ID when editing.");
    if(u==null&&d.controller.listMembers(id).stream().anyMatch(m->m.getId().equals(memberId)))throw new IllegalArgumentException("Member ID already exists. Select the member and edit instead.");
    User user=new User(memberId,GuiInputs.required(f.get("Name")),u==null?"":u.getEmail(),u==null?"Planner":u.getRole());
    user.configurePlanning(GuiInputs.csv(f.get("Skills (comma-separated)")),GuiInputs.csv(f.get("Preferences (comma-separated)")),GuiInputs.slots(f.get("Availability (start/end per line)")),GuiInputs.number(f.get("Existing workload hours")),GuiInputs.number(f.get("Capacity hours")));
    d.controller.configureMember(id,user);d.edited();
   });
 }
}
