package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.util.StringConverter;
final class BlueprintsPanel {
 private final Dashboard d; BlueprintsPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();Event event=d.session.active();var names=new DisplayNames(event);var concepts=Ui.choice(d.controller.listConcepts(id));concepts.setPromptText("Generate a concept in Ideas first");
  concepts.setConverter(new StringConverter<EventConcept>(){public String toString(EventConcept c){return c==null?"":names.concept(c.id());}public EventConcept fromString(String s){return null;}});
  var priority=Ui.planningPriority();
  d.rememberChoice(concepts,"blueprintConcept",EventConcept::id);d.rememberChoice(priority,"blueprintPriority",p->p);
  var generate=Ui.button("Generate alternatives",()->d.action(()->{var c=concepts.getValue();if(c==null)throw new IllegalArgumentException("Select a generated concept first.");String p=priority.getValue();d.background("Generating blueprints",()->d.controller.generateBlueprints(id,c.id(),p));}));
  var table=Ui.table(d.controller.listBlueprints(id));Ui.column(table,"Alternative",EventBlueprint::title);d.remember(table,"blueprints",EventBlueprint::id);Ui.column(table,"Priority",EventBlueprint::priority);Ui.column(table,"Estimate",EventBlueprint::estimatedBudget);
  Ui.column(table,"Execution status",b->b.id().equals(event.getApprovedBlueprintId())?"APPROVED FOR EXECUTION":b.id().equals(event.getSelectedBlueprintId())?"SELECTED - awaiting approval":"Alternative only");
  var select=Ui.button("Select blueprint",()->d.action(()->{
   EventBlueprint candidate=Ui.selected(table);d.background("Selecting blueprint",()->{d.controller.selectBlueprint(id,candidate.id());return null;});
  }));select.setDisable(table.getItems().isEmpty());
  var approve=Ui.button("Approve selected blueprint",()->d.action(()->{
   EventBlueprint candidate=d.controller.viewBlueprint(id,event.getSelectedBlueprintId());
   if(Ui.confirm("Approve execution blueprint?","Use "+candidate.title()+" as the authoritative blueprint for future task and schedule generation? Existing plans will be retained and flagged for review. Estimates remain unverified; no venue is booked."))
    d.background("Approving execution blueprint",()->{d.controller.approveBlueprint(id);return null;});
  }));approve.setDisable(event.getSelectedBlueprintId()==null||java.util.Objects.equals(event.getSelectedBlueprintId(),event.getApprovedBlueprintId()));
  String approved=event.getApprovedBlueprint()==null?"No approved execution blueprint":event.getApprovedBlueprint().title();
  String selected=event.getSelectedBlueprintId()==null?"None":d.controller.viewBlueprint(id,event.getSelectedBlueprintId()).title();
  return Ui.page("Blueprint alternatives","Generate and compare alternatives, then select and explicitly approve one execution blueprint. Approval does not verify costs or book a venue.",
   Ui.row(concepts,Ui.planningPriorityField(priority),generate),
   Ui.card("Execution blueprint",Ui.badge(event.getApprovedBlueprintId()==null?"NOT APPROVED":"APPROVED FOR EXECUTION"),Ui.note("Approved: "+approved),Ui.note("Selected for approval: "+selected),
    Ui.note("Selection alone keeps the current approved blueprint. If alternatives exist, approve one before generating tasks or a schedule. Events without blueprints use event details only."),Ui.row(select,approve)),
   Ui.inspectView(table,b->ResultViews.blueprint(b,names,event.getConstraints())));
 }
 static String describe(EventBlueprint b){return b.title()+"\nBlueprint ID: "+b.id()+"\nConcept ID: "+b.conceptId()+"\n"+b.summary()+"\nEstimated budget: "+b.estimatedBudget()+"\nAttendance: "+b.plannedAttendance()+" | Capacity requirement: "+b.requiredCapacity()+Ui.lines("Agenda",b.agenda())+Ui.lines("Feasibility warnings",b.feasibilityWarnings())+Ui.lines("Venue assumptions",b.venueRequirements())+Ui.lines("Catering assumptions",b.cateringAssumptions())+Ui.lines("Resources",b.resources())+Ui.lines("Checklist",b.checklist())+Ui.lines("Assumptions",b.assumptions());}
}
