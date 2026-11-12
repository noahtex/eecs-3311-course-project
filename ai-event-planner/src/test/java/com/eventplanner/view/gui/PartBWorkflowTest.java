package com.eventplanner.view.gui;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PartBWorkflowTest {
 private EventPlanningFacade facade(){return new EventPlanningFacade(new EventService(),new EventPlanningAgent(p->{throw new AssertionError("Unexpected network request");}),q->{throw new AssertionError("Unexpected Places request");},q->{throw new AssertionError("Unexpected catering request");});}
 @Test void previewProposesExactlyTheAnalyzedFieldsAndUsesTheOriginatingEvent(){try(var facade=facade()){
  var original=new Event("origin","Club","Social","Campus",LocalDateTime.parse("2027-02-01T18:00"),80,1000);
  var copy=original.snapshot();copy.applyChanges(Map.of("attendance","150"));
  var result=new ScenarioResult(new PlanChange(original.changeFields(),Map.of("attendance","150")),copy,List.of("tasks"),java.math.BigDecimal.ZERO,null,List.of(),List.of(),List.of());
  var calls=new ArrayList<String>();
  var controller=new EventPlanningController(facade){
   @Override public ScenarioResult simulateScenario(String id,String request){calls.add("simulate:"+id+":"+request);return result;}
   @Override public PlanChange proposeChangeFields(String id,Map<String,String> fields){calls.add("propose:"+id);assertEquals(result.proposedChanges().changes(),fields);return result.proposedChanges();}
   @Override public PlanChange proposeChange(String id,String request){fail("Do not interpret the request twice");return null;}
   @Override public void confirmChange(String id,boolean confirmed){fail("Preview must never confirm changes");}
  };
  assertSame(result,new PartBWorkflow(controller).previewChange("origin","More attendees"));
  assertEquals(List.of("simulate:origin:More attendees","propose:origin"),calls);assertEquals(80,original.getExpectedAttendance());
 }}
 @Test void failedAnalysisDoesNotReplaceAnExistingProposal(){try(var facade=facade()){
  var controller=new EventPlanningController(facade){
   @Override public ScenarioResult simulateScenario(String id,String request){throw new IllegalArgumentException("Clarify the request");}
   @Override public PlanChange proposeChangeFields(String id,Map<String,String> fields){fail("No proposal after failed analysis");return null;}
  };
  assertThrows(IllegalArgumentException.class,()->new PartBWorkflow(controller).previewChange("origin","ambiguous"));
 }}
 @Test void actualInputsPreserveUnknownAndRecordedZero(){
  var unknown=GuiInputs.postEventData("","","","","","","");assertNull(unknown.actualAttendance());assertNull(unknown.actualExpenses());
  var zero=GuiInputs.postEventData("0","0.00","a","b"," Good work \n\n Need volunteers ","Fun","Late start");
  assertEquals(0,zero.actualAttendance());assertEquals(new java.math.BigDecimal("0.00"),zero.actualExpenses());
  assertEquals(List.of("Good work","Need volunteers"),zero.organizerFeedback());assertEquals(List.of("Fun"),zero.attendeeFeedback());
  assertEquals(List.of("a"),zero.completedTaskIds());assertEquals(List.of("b"),zero.unfinishedTaskIds());
 }
 @Test void invalidActualInputsAndConflictingTaskOutcomesAreRejected(){
  for(String value:List.of("-1","1.5","abc","2147483648","NaN")){
   var error=assertThrows(IllegalArgumentException.class,()->GuiInputs.postEventData(value,"","","","","",""));assertTrue(error.getMessage().contains("Actual attendance"));
  }
  for(String value:List.of("-1","abc","NaN","Infinity")){
   var error=assertThrows(IllegalArgumentException.class,()->GuiInputs.postEventData("",value,"","","","",""));assertTrue(error.getMessage().contains("Actual expenses"));
  }
  assertThrows(IllegalArgumentException.class,()->GuiInputs.postEventData("","","a,a","","","",""));
  assertThrows(IllegalArgumentException.class,()->GuiInputs.postEventData("","","a","a","","",""));
 }
 @Test void scenarioRendererLabelsHypotheticalAndUnknownCosts(){
  var event=new Event("e","Club","Social","Campus",LocalDateTime.parse("2027-02-01T18:00"),80,1000);
  var result=new ScenarioResult(new PlanChange(event.changeFields(),Map.of("attendance","150")),event.snapshot(),List.of(),java.math.BigDecimal.ZERO,null,List.of(),List.of(),List.of());
  String text=ScenarioPanel.describe(result);assertTrue(text.contains("HYPOTHETICAL"));assertTrue(text.contains("Original values"));assertTrue(text.contains("Simulated values"));assertTrue(text.contains("UNKNOWN"));
 }
}
