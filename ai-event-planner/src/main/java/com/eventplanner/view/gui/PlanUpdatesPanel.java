package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import javafx.scene.Node;

final class PlanUpdatesPanel {
 private final Dashboard d;
 PlanUpdatesPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var state=d.partB(id);Event event=d.session.active();var names=new DisplayNames(event);
  var request=state.area("change","Describe a proposed real change. Nothing is applied until confirmation.");
  PlanChange pending=null;
  try{pending=d.controller.pendingChanges(id);}catch(IllegalArgumentException ignored){/* No pending proposal. */}
  final PlanChange proposal=pending;
  var confirm=Ui.button("Confirm Changes",()->d.action(()->{
   if(Ui.confirm("Confirm real event changes?","Apply these interpreted changes?\n"+names.readable(ScenarioPanel.fields(proposal.changes()))+"\nApproved plans are retained and require review."))
    d.background("Applying confirmed event changes",()->{d.controller.confirmChange(id,true);return null;},result->state.changeImpact=null);
  }));confirm.setDisable(proposal==null);
  var discard=Ui.button("Discard proposed changes",()->d.background("Discarding proposed changes",()->{d.controller.confirmChange(id,false);return null;},result->state.changeImpact=null));discard.setDisable(proposal==null);
  String preview=proposal==null?"No pending real changes.":"PENDING — not applied\nOriginal values\n"+ScenarioPanel.fields(proposal.before())+"\n\nInterpreted changes\n"+names.readable(ScenarioPanel.fields(proposal.changes()))
   +"\nTasks, schedule, budget and communications require review after confirmation.";
  if(proposal!=null&&state.changeImpact!=null&&state.changeImpact.proposedChanges().equals(proposal))preview+="\n\n"+ScenarioPanel.describe(state.changeImpact);
  var risk=state.area("risk","e.g. Speaker cancellation, bad weather, catering delay");
  var plans=Ui.table(d.controller.listContingencies(id));Ui.column(plans,"Risk",ContingencyPlan::risk);Ui.column(plans,"Impact",ContingencyPlan::impact);d.remember(plans,"contingencies",ContingencyPlan::risk);
  String warnings=event.getReviewWarnings().isEmpty()?"No Observer review warnings recorded.":String.join("\n",event.getReviewWarnings().entrySet().stream().map(e->Ui.lines(e.getKey(),e.getValue())).toList());
  return Ui.page("Plan Updates & Contingencies","Preview impacts before confirming real changes. Approved plans are never silently replaced.",
   Ui.card("Proposed real change",request,Ui.row(Ui.button("Preview changes & impacts",()->d.action(()->{
    String text=GuiInputs.required(request.getText());
    d.background("Analyzing proposed event change",()->new PartBWorkflow(d.controller).previewChange(id,text),result->state.changeImpact=result);
   })),confirm,discard),proposal==null?Ui.note("No pending real changes."):new javafx.scene.layout.VBox(14,Ui.badge("PENDING - NOT APPLIED"),Ui.card("Original event",ResultViews.fields(proposal.before(),names)),Ui.card("Proposed changes",ResultViews.fields(proposal.changes(),names)),Ui.note("Tasks, schedule, budget and communications require review after confirmation."),state.changeImpact!=null&&state.changeImpact.proposedChanges().equals(proposal)?ResultViews.scenario(state.changeImpact,names):Ui.note("No stored impact analysis for this proposal."),Ui.technical(proposal.toString()+"\n\n"+preview))),
   Ui.card("Current event details",ResultViews.event(event),ResultViews.warnings("Plan revalidation",warnings,names)),
   Ui.card("Contingency recommendations",risk,Ui.button("Generate contingency",()->d.action(()->{
    String text=GuiInputs.required(risk.getText());d.background("Generating contingency",()->d.controller.generateContingency(id,text));
   })),Ui.inspectView(plans,plan->ResultViews.contingency(plan,names))));
 }
 static String describe(ContingencyPlan plan){return "UNVERIFIED CONTINGENCY\nRisk: "+plan.risk()+"\nImpact: "+plan.impact()+"\nRecommended response: "+plan.proposedResponse()+Ui.lines("Resources",plan.resources())+Ui.lines("Assumptions",plan.assumptions());}
}
