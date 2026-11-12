package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import java.util.*;
final class SchedulePanel {
 private final Dashboard d; SchedulePanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();Event e=d.session.active();var names=new DisplayNames(e);boolean exists=d.controller.hasScheduleDraft(id)||e.getSchedule()!=null;
  List<Session> sessions=exists?d.controller.viewSchedule(id).sessions().stream().sorted(Comparator.comparing(s->s.slot().start())).toList():List.of();
  var table=Ui.table(sessions);Ui.column(table,"Session",Session::title);Ui.column(table,"Kind",Session::kind);Ui.column(table,"Starts",s->s.slot().start());Ui.column(table,"Ends",s->s.slot().end());Ui.column(table,"Speaker",s->names.speaker(s.speakerId()));Ui.column(table,"Space",s->names.resource(s.space()));d.remember(table,"schedule",Session::id);
  var window=Ui.button("Set event end",()->new Ui.Form().dateTime("Event end",e.getEndDateTime()).show("Event starts "+DisplayNames.date(e.getDateTime()),f->{var end=GuiInputs.date(f.get("Event end"));new TimeSlot(e.getDateTime(),end);d.background("Saving event window",()->{d.controller.configureEventEnd(id,end);return null;});}));
  var edit=Ui.button("Edit selected session",()->d.action(()->{Session s=Ui.selected(table);
   var resources=new ArrayList<>(names.resourceOptions(null));for(String key:s.resources())if(resources.stream().noneMatch(o->o.id().equals(key)))resources.add(new DisplayNames.Option(key,names.resource(key)));
   var form=new Ui.Form().field("Title",s.title()).field("Kind",s.kind()).dateTime("Start",s.slot().start()).dateTime("End",s.slot().end()).option("Speaker",names.speakerOptions(s.speakerId()),s.speakerId());
   form.option("Space",names.resourceOptions(s.space()),s.space()).field("New space name (optional)","").field("Additional resource names (comma-separated)","");
   form.multiOption("Exclusive resources (Ctrl-click to select)",resources,s.resources()).show("Edit session",f->{
    String speaker=f.get("Speaker");var revised=new Session(s.id(),f.get("Title"),GuiInputs.enumeration(Session.Kind.class,f.get("Kind")),new TimeSlot(GuiInputs.date(f.get("Start")),GuiInputs.date(f.get("End"))),speaker.isBlank()?null:speaker,f.get("New space name (optional)").isBlank()?f.get("Space"):f.get("New space name (optional)"),java.util.stream.Stream.concat(GuiInputs.csv(f.get("Exclusive resources (Ctrl-click to select)")).stream(),GuiInputs.csv(f.get("Additional resource names (comma-separated)")).stream()).distinct().toList());d.background("Saving agenda session",()->{d.controller.reviseSession(id,revised);return null;});
   });
  }));
  boolean draft=d.controller.hasScheduleDraft(id);
  var approve=Ui.button("Approve schedule",()->d.action(()->{if(Ui.confirm("Approve schedule?","Approve this draft after reviewing conflicts?"))d.background("Approving schedule",()->{d.controller.approveSchedule(id);return null;});}));approve.setDisable(!draft);
  var discard=Ui.button("Discard draft",()->d.action(()->{if(Ui.confirm("Discard schedule draft?","Retain the approved schedule?"))d.background("Discarding schedule draft",()->{d.controller.discardSchedule(id);return null;});}));discard.setDisable(!draft);
  var availability=new VBox(14);for(Speaker speaker:e.getSpeakers())availability.getChildren().add(ResultViews.availability(names.speaker(speaker.id()),speaker.availability()));
  e.getResourceAvailability().forEach((resource,slots)->availability.getChildren().add(ResultViews.availability(names.resource(resource),slots)));
  if(availability.getChildren().isEmpty())availability.getChildren().add(Ui.note("No speakers or resource availability configured yet."));
  return Ui.page("Schedule & resources","Build a chronological agenda. Setup, breaks and teardown stay inside the event window; unknown availability blocks approval.",
   Ui.metrics(Ui.metric("Event starts",DisplayNames.date(e.getDateTime())),Ui.metric("Event ends",DisplayNames.date(e.getEndDateTime()))),
   Ui.row(window,Ui.button("Configure speaker",()->chooseSpeaker(id,e,names)),Ui.button("Configure resource",()->chooseResource(id,e,names))),
   Ui.card("Event agenda",Ui.badge(draft?"DRAFT":exists?"APPROVED":"NOT GENERATED"),Ui.row(Ui.button("Generate agenda",()->d.background("Generating agenda",()->d.controller.buildSchedule(id))),edit,Ui.button("Validate schedule",()->d.validate(id,"schedule")),approve,discard),Ui.inspectView(table,s->ResultViews.session(s,names)),ResultViews.warnings("Schedule review",exists?d.diagnostics(id,"schedule"):"Generate a schedule after configuring the event window and availability.",names)),
   Ui.card("Configured availability",availability));
 }
 private void chooseSpeaker(String id,Event event,DisplayNames names){
  var options=new ArrayList<DisplayNames.Option>();options.add(new DisplayNames.Option("","New speaker"));for(Speaker s:event.getSpeakers())options.add(new DisplayNames.Option(s.id(),names.speaker(s.id())));
  new Ui.Form().option("Speaker profile",options,"").show("Choose speaker",f->{String selected=f.get("Speaker profile");speaker(id,event.getSpeakers().stream().filter(s->s.id().equals(selected)).findFirst().orElse(null));});
 }
 private void speaker(String id,Speaker existing){
  String speakerId=existing==null?UUID.randomUUID().toString():existing.id();
  new Ui.Form().field("Name",existing==null?"":existing.name()).availability("Availability",existing==null?List.of():existing.availability()).technical("Speaker reference: "+speakerId).show("Configure speaker",f->{
   Speaker speaker=new Speaker(speakerId,f.get("Name"),GuiInputs.slots(f.get("Availability")));
   if(existing==null||Ui.confirm("Replace speaker profile?","Replace availability for "+existing.name()+"?")){d.background("Saving speaker",()->{d.controller.configureSpeaker(id,speaker);return null;});}
  });
 }
 private void chooseResource(String id,Event event,DisplayNames names){
  var options=new ArrayList<DisplayNames.Option>();options.add(new DisplayNames.Option("","New resource"));options.addAll(names.resourceOptions(null));
  new Ui.Form().option("Resource",options,"").show("Choose resource",f->{String selected=f.get("Resource");
   var form=new Ui.Form();if(selected.isBlank())form.field("Resource name","");else form.hidden("Resource name",selected);
   form.availability("Availability",event.getResourceAvailability().getOrDefault(selected,List.of())).show("Configure resource",values->{
    String resource=GuiInputs.required(values.get("Resource name"));var slots=GuiInputs.slots(values.get("Availability"));
    boolean exists=event.getResourceAvailability().containsKey(resource.toLowerCase(Locale.ROOT));
    if(!exists||Ui.confirm("Replace resource availability?","Replace availability for "+names.resource(resource)+"?")){d.background("Saving resource availability",()->{d.controller.configureResource(id,resource,slots);return null;});}
   });
  });
 }
}
