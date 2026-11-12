package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.util.StringConverter;
final class BlueprintsPanel {
 private final Dashboard d; BlueprintsPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var names=new DisplayNames(d.session.active());var concepts=Ui.choice(d.controller.listConcepts(id));concepts.setPromptText("Generate a concept in Ideas first");
  concepts.setConverter(new StringConverter<EventConcept>(){public String toString(EventConcept c){return c==null?"":names.concept(c.id());}public EventConcept fromString(String s){return null;}});
  var priority=Ui.choice(java.util.List.of("budget","community","attendance"));
  d.rememberChoice(concepts,"blueprintConcept",EventConcept::id);d.rememberChoice(priority,"blueprintPriority",p->p);
  var generate=Ui.button("Generate alternatives",()->d.action(()->{var c=concepts.getValue();if(c==null)throw new IllegalArgumentException("Select a generated concept first.");String p=priority.getValue();d.background("Generating blueprints",()->d.controller.generateBlueprints(id,c.id(),p));}));
  var table=Ui.table(d.controller.listBlueprints(id));Ui.column(table,"Alternative",EventBlueprint::title);d.remember(table,"blueprints",EventBlueprint::id);Ui.column(table,"Priority",EventBlueprint::priority);Ui.column(table,"Estimate",EventBlueprint::estimatedBudget);
  return Ui.page("Blueprint alternatives","Choose a concept and planning priority. Each blueprint is a separate unverified proposal, never an approved venue booking.",Ui.row(concepts,priority,generate),Ui.inspectView(table,b->ResultViews.blueprint(b,names,d.session.active().getConstraints())));
 }
 static String describe(EventBlueprint b){return b.title()+"\nBlueprint ID: "+b.id()+"\nConcept ID: "+b.conceptId()+"\n"+b.summary()+"\nEstimated budget: "+b.estimatedBudget()+"\nAttendance: "+b.plannedAttendance()+" | Capacity requirement: "+b.requiredCapacity()+Ui.lines("Agenda",b.agenda())+Ui.lines("Feasibility warnings",b.feasibilityWarnings())+Ui.lines("Venue assumptions",b.venueRequirements())+Ui.lines("Catering assumptions",b.cateringAssumptions())+Ui.lines("Resources",b.resources())+Ui.lines("Checklist",b.checklist())+Ui.lines("Assumptions",b.assumptions());}
}
