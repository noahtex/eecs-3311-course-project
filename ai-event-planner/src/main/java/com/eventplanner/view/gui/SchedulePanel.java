package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import java.util.*;
final class SchedulePanel {
 private final Dashboard d; SchedulePanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();Event e=d.session.active();boolean exists=d.controller.hasScheduleDraft(id)||e.getSchedule()!=null;
  List<Session> sessions=exists?d.controller.viewSchedule(id).sessions().stream().sorted(Comparator.comparing(s->s.slot().start())).toList():List.of();
  var table=Ui.table(sessions);Ui.column(table,"Session",Session::title);Ui.column(table,"Kind",Session::kind);Ui.column(table,"Start",s->s.slot().start());Ui.column(table,"End",s->s.slot().end());Ui.column(table,"Speaker",Session::speakerId);Ui.column(table,"Space",Session::space);
  var window=Ui.button("Set event end",()->new Ui.Form().field("Event end",e.getEndDateTime()).show("Schedule window starts "+e.getDateTime(),f->{d.controller.configureEventEnd(id,GuiInputs.date(f.get("Event end")));d.edited();}));
  var edit=Ui.button("Edit selected session",()->d.action(()->{Session s=Ui.selected(table);
   new Ui.Form().field("Title",s.title()).field("Kind (SETUP / ACTIVITY / BREAK / TEARDOWN)",s.kind()).field("Start",s.slot().start()).field("End",s.slot().end()).field("Speaker ID (blank = none)",s.speakerId()).field("Space resource ID",s.space()).field("Exclusive resources (comma-separated)",String.join(", ",s.resources())).show("Edit session",f->{
    String speaker=f.get("Speaker ID (blank = none)");d.controller.reviseSession(id,new Session(s.id(),f.get("Title"),GuiInputs.enumeration(Session.Kind.class,f.get("Kind (SETUP / ACTIVITY / BREAK / TEARDOWN)")),new TimeSlot(GuiInputs.date(f.get("Start")),GuiInputs.date(f.get("End"))),speaker.isBlank()?null:speaker,f.get("Space resource ID"),GuiInputs.csv(f.get("Exclusive resources (comma-separated)"))));d.edited();
   });
  }));
  boolean draft=d.controller.hasScheduleDraft(id);
  var approve=Ui.button("Approve schedule",()->d.action(()->{if(Ui.confirm("Approve schedule?","Approve this draft after reviewing conflicts?"))d.background("Approving schedule",()->{d.controller.approveSchedule(id);return null;});}));approve.setDisable(!draft);
  var discard=Ui.button("Discard draft",()->d.action(()->{if(Ui.confirm("Discard schedule draft?","Retain the approved schedule?"))d.controller.discardSchedule(id);}));discard.setDisable(!draft);
  return Ui.page("Schedule & resources","Window: "+e.getDateTime()+" to "+e.getEndDateTime()+". Setup, breaks and teardown belong inside this window. Unknown availability blocks approval.",
   Ui.row(window,Ui.button("Configure speaker",()->speaker(id)),Ui.button("Configure resource",()->resource(id))),
   Ui.card(draft?"DRAFT schedule":exists?"APPROVED schedule":"No schedule yet",Ui.row(Ui.button("Generate agenda",()->d.background("Generating agenda",()->d.controller.buildSchedule(id))),edit,Ui.button("Validate schedule",()->d.validate(id,"schedule")),approve,discard),Ui.inspect(table,Object::toString),Ui.details(exists?d.diagnostics(id,"schedule"):"Generate a schedule after configuring the event window and resource availability.")),
   Ui.card("Configured availability",Ui.details(Ui.lines("Speakers",e.getSpeakers())+"\nResources: "+e.getResourceAvailability())));
 }
 private void speaker(String id){new Ui.Form().field("Speaker ID (existing replaces profile)","").field("Name","").field("Availability (start/end per line)","").show("Configure speaker",f->{
  String speakerId=GuiInputs.required(f.get("Speaker ID (existing replaces profile)"));boolean exists=d.session.active().getSpeakers().stream().anyMatch(s->s.id().equals(speakerId));
  Speaker speaker=new Speaker(speakerId,f.get("Name"),GuiInputs.slots(f.get("Availability (start/end per line)")));
  if(!exists||Ui.confirm("Replace speaker profile?","Replace availability for "+speakerId+"?")){d.controller.configureSpeaker(id,speaker);d.edited();}
 });}
 private void resource(String id){new Ui.Form().field("Resource ID (existing replaces availability)","").field("Availability (start/end per line)","").show("Configure resource",f->{
  String resource=GuiInputs.required(f.get("Resource ID (existing replaces availability)"));var slots=GuiInputs.slots(f.get("Availability (start/end per line)"));
  boolean exists=d.session.active().getResourceAvailability().containsKey(resource.toLowerCase(Locale.ROOT));
  if(!exists||Ui.confirm("Replace resource availability?","Replace availability for "+resource+"?")){d.controller.configureResource(id,resource,slots);d.edited();}
 });}
}
