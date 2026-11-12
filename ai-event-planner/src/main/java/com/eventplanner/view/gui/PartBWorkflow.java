package com.eventplanner.view.gui;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.ScenarioResult;

/** Compose existing application operations; interpretation and impact calculations remain in the backend. */
final class PartBWorkflow {
 private final EventPlanningController controller;
 PartBWorkflow(EventPlanningController controller){this.controller=controller;}
 ScenarioResult previewChange(String id,String request){
  var result=controller.simulateScenario(id,request);
  // Propose exactly the fields that were analyzed, rather than interpreting the request twice.
  controller.proposeChangeFields(id,result.proposedChanges().changes());
  return result;
 }
}
