package com.eventplanner.view.gui;

import com.eventplanner.model.ScenarioResult;
import javafx.scene.Node;
import java.util.Map;

final class ScenarioPanel {
 private final Dashboard d;
 ScenarioPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var state=d.partB(id);
  var request=state.area("scenario","e.g. Attendance increases to 150");
  return Ui.page("Scenario Simulator","HYPOTHETICAL — simulations never apply changes to the real event or approved plans.",
   request,Ui.row(Ui.button("Run simulation",()->d.action(()->{
    String text=GuiInputs.required(request.getText());
    d.background("Simulating hypothetical change",()->d.controller.simulateScenario(id,text),result->state.simulation=result);
   })),Ui.button("Clear simulation",()->{request.clear();state.simulation=null;d.refresh();})),
   Ui.details(state.simulation==null?"Run a simulation to inspect hypothetical impacts.":describe(state.simulation)));
 }
 static String fields(Map<String,String> values){
  return String.join("\n",new java.util.TreeMap<>(values).entrySet().stream().map(e->e.getKey()+": "+e.getValue()).toList());
 }
 static String describe(ScenarioResult result){
  return "HYPOTHETICAL — not applied\n\nOriginal values (at simulation time)\n"+fields(result.proposedChanges().before())
   +"\n\nSimulated values\n"+fields(result.snapshot().changeFields())
   +"\n\nInterpreted changes\n"+fields(result.proposedChanges().changes())
   +"\nSpending-limit difference (not a cost estimate): "+result.budgetLimitDifference()+" "+result.snapshot().getPlanningCurrency()
   +"\nEstimated expense difference (rough attendance-proportional assumption): "+(result.estimatedExpenseDifference()==null?"UNKNOWN":result.estimatedExpenseDifference())
   +Ui.lines("Affected planning components",result.affectedComponents())+Ui.lines("Estimated impacts / resource implications",result.implications())
   +Ui.lines("Feasibility issues and assumptions",result.warnings())+Ui.lines("Recommended changes",result.recommendations());
 }
}
