package com.eventplanner.facade;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.*;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.*;
import com.eventplanner.service.*;
import com.eventplanner.view.EventPlanningCLI;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneFourTest {
 static final String CHANGE="{\"changes\":{\"attendance\":\"150\"},\"ambiguities\":[]}";
 static final String SCENARIO="{\"affectedComponents\":[\"catering\"],\"implications\":[\"More resources needed\"],\"warnings\":[],\"recommendations\":[\"Review capacity\"]}";
 static final String COMM="{\"factOrder\":[\"name\",\"dateTime\",\"type\"],\"opening\":\"Hello friends!\",\"closing\":\"Join us!\"}";
 static final String REPORT="{\"executiveSummary\":\"Limited recorded evidence\",\"successes\":[],\"issues\":[],\"lessonsLearned\":[\"Collect feedback\"],\"recommendations\":[\"Review missing outcomes\"]}";
 Event event() {
  Event e=new Event("e","Club night","Social","Toronto",LocalDateTime.parse("2027-01-10T18:00"),80,1000);
  e.setEndDateTime(e.getDateTime().plusHours(3)); e.addConstraint("Indoor access required");
  var user=new User("u","Planner","","Organizer"); user.configurePlanning(List.of("setup"),List.of(),List.of(),0,10); e.getPlanningTeam().addMember(user);
  e.storeTasks(List.of(new Task("t","Prepare","Preparation",null,e.getDateTime().minusDays(2),e.getDateTime().minusDays(1),1,List.of(),Task.Priority.HIGH,Task.Status.TODO)));
  e.storeBudget(new Budget("b","CAD","budget",List.of(new BudgetItem("i","Food","Meals",BigDecimal.ONE,new BigDecimal("800"),BudgetItem.PriceStatus.ESTIMATED,"estimate")),List.of(),List.of(),List.of("estimate")));
  e.storeSchedule(new Schedule("s",List.of(new Session("s1","Welcome",Session.Kind.ACTIVITY,new TimeSlot(e.getDateTime(),e.getDateTime().plusHours(1)),null,"room",List.of()))));
  return e;
 }
 EventPlanningFacade facade(Event e,String... replies) {
  Queue<String> queue=new ArrayDeque<>(List.of(replies));
  var events=new EventService(); events.createEvent(e);
  return new EventPlanningFacade(events,new EventPlanningAgent(prompt->{ if(queue.isEmpty()) throw new AssertionError("Unexpected AI call"); return queue.remove(); }),q->{throw new AssertionError("No external venue call");},q->{throw new AssertionError("No external catering call");});
 }
 @Test void simulationIsIndependentIncludingNestedMutableUsersAndPlans() {
  Event e=event(); var f=facade(e,CHANGE,SCENARIO); var result=f.simulateScenario("e","Attendance grows to 150");
  assertEquals(80,e.getExpectedAttendance()); assertEquals(150,result.snapshot().getExpectedAttendance());
  assertTrue(result.affectedComponents().containsAll(List.of("attendance","catering")));
  assertEquals(new BigDecimal("700.00"),result.estimatedExpenseDifference());
  var copy=result.snapshot(); copy.getPlanningTeam().getMembers().get(0).configurePlanning(List.of(),List.of(),List.of(),9,9);
  copy.storeTasks(List.of()); copy.storeBudget(new Budget("other","CAD","budget",e.getBudget().items(),List.of(),List.of(),List.of("test")));
  copy.storeSchedule(new Schedule("other",e.getSchedule().sessions())); copy.addConstraint("Simulation only");
  copy.putSpeaker(new Speaker("new","Speaker",List.of())); copy.putResourceAvailability("room",List.of());
  assertEquals(0,e.getPlanningTeam().getMembers().get(0).getExistingWorkloadHours());
  assertEquals(1,e.getTasks().size()); assertEquals("b",e.getBudget().id()); assertEquals("s",e.getSchedule().id());
  assertEquals(1,e.getConstraints().size()); assertTrue(e.getSpeakers().isEmpty()); assertTrue(e.getResourceAvailability().isEmpty());
  assertThrows(UnsupportedOperationException.class,()->copy.getBudget().items().clear());
 }
 @Test void confirmedChangesNotifyObserversRevalidateAndRetainApprovedPlans() {
  Event e=event(); var f=facade(e); int[] notified={0}; e.addObserver((event,fields)->notified[0]++);
  Budget original=e.getBudget(); f.proposeChangeFields("e",Map.of("budget","700","location","Indoor hall"));
  assertEquals(1000,e.getTotalBudget()); f.confirmChange("e",true);
  assertEquals(700,e.getTotalBudget()); assertEquals("Indoor hall",e.getLocation()); assertEquals(1,notified[0]);
  assertSame(original,e.getBudget()); assertTrue(e.getReviewWarnings().keySet().containsAll(List.of("tasks","schedule","budget")));
  assertTrue(e.getReviewWarnings().get("budget").stream().anyMatch(s->s.contains("Over budget")));
 }
 @Test void cancelledAndInvalidChangesDoNotMutate() {
  Event e=event(); var f=facade(e); f.proposeChangeFields("e",Map.of("attendance","150")); f.confirmChange("e",false);
  assertEquals(80,e.getExpectedAttendance()); assertThrows(IllegalArgumentException.class,()->f.pendingChanges("e"));
  assertThrows(IllegalArgumentException.class,()->f.proposeChangeFields("e",Map.of("budget","NaN")));
  assertThrows(IllegalArgumentException.class,()->f.proposeChangeFields("e",Map.of("dateTime","2027-02-01T18:00")));
  assertEquals(1000,e.getTotalBudget()); assertEquals(LocalDateTime.parse("2027-01-10T18:00"),e.getDateTime());
 }
 @Test void staleProposalsCannotOverwriteNewFacts() {
  Event e=event(); var f=facade(e); f.proposeChangeFields("e",Map.of("attendance","150")); e.applyChanges(Map.of("name","New name"));
  assertThrows(IllegalArgumentException.class,()->f.confirmChange("e",true)); assertEquals(80,e.getExpectedAttendance());
 }
 @Test void ambiguousInterpretationFailsBeforeMutation() {
  Event e=event(); var f=facade(e,"{\"changes\":{},\"ambiguities\":[\"Which speaker?\"]}");
  assertThrows(IllegalArgumentException.class,()->f.proposeChange("e","Speaker unavailable")); assertEquals(80,e.getExpectedAttendance());
 }
 @Test void speakerRemovalProducesRealScheduleDiagnostic() {
  Event e=event(); e.putSpeaker(new Speaker("sp","Speaker",List.of()));
  var old=e.getSchedule().sessions().get(0);
  e.storeSchedule(new Schedule("s",List.of(new Session(old.id(),old.title(),old.kind(),old.slot(),"sp",old.space(),old.resources()))));
  var f=facade(e); f.proposeChangeFields("e",Map.of("unavailableSpeaker","sp")); f.confirmChange("e",true);
  assertTrue(e.getReviewWarnings().get("schedule").stream().anyMatch(s->s.contains("unknown speaker")));
 }
 @Test void communicationUsesOnlyApprovedFactsAndSupportsEditApproval() {
  Event e=event(); var f=facade(e,COMM); var c=f.generateCommunication("e",Communication.Channel.INSTAGRAM,"friendly");
  assertFalse(c.approved()); assertTrue(c.text().contains(e.getName())); assertTrue(c.text().contains(e.getDateTime().toString()));
  assertFalse(c.text().contains("Toronto")); assertFalse(c.text().contains("1000"));
  assertEquals("Edited by planner",f.editCommunication("e",c.id(),"Edited by planner").text());
  assertTrue(f.approveCommunication("e",c.id()).approved());
  f.editCommunication("e",c.id(),"Revised"); assertFalse(f.viewCommunication("e",c.id()).approved());
 }
 @Test void changedFactsInvalidateCommunicationApproval() {
  Event e=event(); var f=facade(e,COMM); var c=f.generateCommunication("e",Communication.Channel.EMAIL,"formal"); f.approveCommunication("e",c.id());
  f.proposeChangeFields("e",Map.of("name","Changed")); f.confirmChange("e",true);
  assertFalse(f.viewCommunication("e",c.id()).approved()); assertThrows(IllegalArgumentException.class,()->f.approveCommunication("e",c.id()));
 }
 @Test void inventedFactKeysAreRejectedWithoutSavingDraft() {
  Event e=event(); var f=facade(e,"{\"factOrder\":[\"name\",\"registrationLink\",\"type\"],\"opening\":\"Hello friends!\",\"closing\":\"Join us!\"}");
  assertThrows(PlanningException.class,()->f.generateCommunication("e",Communication.Channel.EMAIL,"formal")); assertTrue(e.getCommunications().isEmpty());
 }
 @Test void faqNeverInventsUnknownDetailsOrUsesHypotheticalData() {
  Event e=event(); var f=facade(e); f.proposeChangeFields("e",Map.of("dateTime","2027-01-10T19:00"));
  assertTrue(f.askFaq("e","When?").contains("18:00"));
  for(String q:List.of("registration link?","Is the venue confirmed?","Accessibility?","Ticket price?","When and where?")) assertTrue(f.askFaq("e",q).contains("not confirmed"));
 }
 @Test void deterministicPostEventDifferencesAndStoredReport() {
  Event e=event(); var f=facade(e,REPORT);
  f.recordPostEvent("e",new PostEventData(100,new BigDecimal("850"),List.of("t"),List.of(),List.of("Smooth setup"),List.of("Long queue"),List.of("Queue")));
  var r=f.analyzePostEvent("e"); assertEquals(20,r.attendanceDifference()); assertEquals(0,new BigDecimal("50").compareTo(r.expenseDifference()));
  assertEquals(1,r.completedTasks()); assertEquals(100.0,r.completionRate()); assertEquals(List.of(r),e.getReports()); assertEquals(80,r.expectedAttendance());
 }
 @Test void missingOutcomesRemainUnknown() {
  Event e=event(); var f=facade(e,REPORT); f.recordPostEvent("e",new PostEventData(null,null,List.of(),List.of(),List.of(),List.of(),List.of()));
  var r=f.analyzePostEvent("e"); assertNull(r.attendanceDifference()); assertNull(r.expenseDifference()); assertTrue(r.missingInformation().size()>=5);
  assertEquals(0,r.completedTasks()); assertEquals(0,r.unfinishedTasks());
 }
 @Test void invalidTaskOutcomesRejected() {
  Event e=event(); var f=facade(e);
  assertThrows(IllegalArgumentException.class,()->f.recordPostEvent("e",new PostEventData(null,null,List.of("invented"),List.of(),List.of(),List.of(),List.of())));
  assertThrows(IllegalArgumentException.class,()->new PostEventData(null,null,List.of("t"),List.of("t"),List.of(),List.of(),List.of()));
 }
 @Test void aiFailureDoesNotStoreReportOrScenarioChanges() {
  Event e=event(); var events=new EventService(); events.createEvent(e);
  var f=new EventPlanningFacade(events,new EventPlanningAgent(prompt->{throw new PlanningException("AI unavailable");}));
  assertThrows(PlanningException.class,()->f.simulateScenario("e","Attendance 150")); assertEquals(80,e.getExpectedAttendance());
  f.recordPostEvent("e",new PostEventData(null,null,List.of(),List.of(),List.of(),List.of(),List.of()));
  assertThrows(PlanningException.class,()->f.analyzePostEvent("e")); assertTrue(e.getReports().isEmpty());
 }
 @Test void contingencyMappedStoredAndWeatherNotClaimed() {
  Event e=event(); var f=facade(e,"{\"risk\":\"Rain\",\"impact\":\"Outdoor activity affected\",\"proposedResponse\":\"Confirm indoor alternative\",\"resources\":[\"Indoor space\"],\"assumptions\":[]}");
  var plan=f.generateContingency("e","Bad weather"); assertEquals(List.of(plan),f.listContingencies("e")); assertTrue(plan.assumptions().get(0).contains("no weather"));
 }
 @Test void ungroundedAiWordingIsRejected() {
  Event e=event(); var f=facade(e,COMM.replace("Hello friends!","Free tickets at example.com!"));
  assertThrows(PlanningException.class,()->f.generateCommunication("e",Communication.Channel.WHATSAPP,"friendly"));
  assertTrue(e.getCommunications().isEmpty());
 }
 @Test void scenarioAnalysisFailureLeavesOriginalAndPendingChangesUntouched() {
  Event e=event(); var f=facade(e,CHANGE,"{}"); f.proposeChangeFields("e",Map.of("budget","900"));
  assertThrows(PlanningException.class,()->f.simulateScenario("e","Attendance 150"));
  assertEquals(80,e.getExpectedAttendance()); assertEquals("900",f.pendingChanges("e").changes().get("budget"));
 }
 @Test void malformedDatesAreUserSafeValidationErrors() {
  Event e=event(); var f=facade(e);
  assertThrows(IllegalArgumentException.class,()->f.proposeChangeFields("e",Map.of("dateTime","tomorrow")));
 }
 @Test void cliTraversesControllerAndFacadeWithCancellationAndHelp() {
  Event e=event(); var f=facade(e); var out=new ByteArrayOutputStream();
  String input="help\nplan update e\nstructured\n\n\nIndoor hall\n\n\n150\n700\n\nplan changes e\nplan confirm e\nno\nfaq ask e\nRegistration link?\npostevent view e\nexit\n";
  new EventPlanningCLI(new EventPlanningController(f),new Scanner(input),new PrintStream(out)).run();
  String text=out.toString(); assertTrue(text.contains("scenario simulate")); assertTrue(text.contains("Changes cancelled")); assertTrue(text.contains("not confirmed")); assertEquals(80,e.getExpectedAttendance());
 }
}
