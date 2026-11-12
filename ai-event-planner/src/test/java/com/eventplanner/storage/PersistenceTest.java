package com.eventplanner.storage;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.AIModel;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {
 @TempDir Path directory;
 private static String id(){return UUID.randomUUID().toString();}
 private EventPlanningFacade open(){return facade(new EventService(),new JsonPlanRepository(directory),p->{throw new AssertionError("Unexpected AI call");});}
 private EventPlanningFacade facade(EventService events,PlanRepository repository,AIModel ai){return new EventPlanningFacade(events,new EventPlanningAgent(ai),q->{throw new AssertionError("Unexpected Places call");},q->{throw new AssertionError("Unexpected catering call");},repository);}
 private EventPlanningFacade seeded(Event event){var events=new EventService();events.createEvent(event);return facade(events,new JsonPlanRepository(directory),p->{throw new AssertionError("No network");});}
 private Event richEvent(){
  var start=LocalDateTime.parse("2099-05-01T18:00:12.123456789");var e=new Event(id(),"Community night","Social","Toronto",start,80,1000);e.setEndDateTime(start.plusHours(3));e.getPlanningTeam().addEvent(e);
  var member=new User(id(),"Alex","alex@example.invalid","Planner");member.configurePlanning(List.of("setup"),List.of("indoors"),List.of(new TimeSlot(start.minusDays(3),start)),0,10);e.getPlanningTeam().addMember(member);
  e.configureBudgetNeeds("CAD",List.of("vegetarian"),"Confirm allergens");e.configureVenueNeeds(true,List.of("projector"));e.addConstraint("Indoor access required");
  var idea=new EventIdea(id(),"Alex","Meet new members",start.minusDays(20));e.restoreOrganizerIdeas(List.of(idea));
  var concept=new EventConcept(id(),"Workshop","Inclusive activities",List.of("community"),List.of(idea.id()),List.of(),List.of(),List.of("Room availability unknown"));e.addConcepts(List.of(concept));
  e.addBlueprints(List.of(new EventBlueprint(id(),concept.id(),"community","Workshop alternative","A proposal",List.of("Welcome"),List.of("Accessible room"),List.of("Confirm meals"),List.of("Projector"),List.of("Confirm resources"),List.of("Costs estimated"),List.of("Availability unknown"),900,80,100)));
  var task=new Task(id(),"Set up","Prepare seats",member.getId(),start.minusDays(1),start.minusDays(1).plusHours(2),1,List.of("setup"),Task.Priority.HIGH,Task.Status.TODO);e.storeTasks(List.of(task));
  var speaker=new Speaker(id(),"Sam",List.of(new TimeSlot(start,e.getEndDateTime())));e.putSpeaker(speaker);e.putResourceAvailability("main hall",List.of(new TimeSlot(start,e.getEndDateTime())));
  e.storeSchedule(new Schedule(id(),List.of(new Session(id(),"Welcome",Session.Kind.ACTIVITY,new TimeSlot(start.plusMinutes(15),start.plusHours(1)),speaker.id(),"main hall",List.of()),
   new Session(id(),"Setup",Session.Kind.SETUP,new TimeSlot(start,start.plusMinutes(15)),null,"main hall",List.of()),
   new Session(id(),"Break",Session.Kind.BREAK,new TimeSlot(start.plusHours(1),start.plusHours(1).plusMinutes(15)),null,"main hall",List.of()),
   new Session(id(),"Teardown",Session.Kind.TEARDOWN,new TimeSlot(e.getEndDateTime().minusMinutes(15),e.getEndDateTime()),null,"main hall",List.of()))));
  var provider=new VenueOption(id(),"Local hall","Toronto","Planner evidence",Instant.parse("2026-11-11T18:00:00.123456789Z"),Map.of("provider","test"),new Evidence<>(100,Evidence.Status.PLANNER_CONFIRMED,"Organizer checked"),Evidence.unknown(),"CAD",new Evidence<>(true,Evidence.Status.PROVIDER_REPORTED,"Provider"),new Evidence<>(List.of("projector"),Evidence.Status.ESTIMATED,"Assumption"));e.addVenueOptions(List.of(provider));
  e.storeVenueComparisons(List.of(new VenueComparison(provider.id(),2,List.of("Price unknown"),"Confirm costs and availability",List.of("Good capacity, unconfirmed price"))));
  e.storeBudget(new Budget(id(),"CAD","budget",List.of(new BudgetItem(id(),"Food","Meals",BigDecimal.ONE,new BigDecimal("100.00"),BudgetItem.PriceStatus.ESTIMATED,"Estimate, not a quote")),List.of(new CateringOption(provider.id(),"Confirm menu",List.of("Confirm allergens"))),List.of(provider),List.of("Delivery not verified")));
  e.flagReview("budget",List.of("Existing warning to preserve"));e.addContingency(new ContingencyPlan("Speaker cancellation","Agenda gap","Backup activity",List.of("Facilitator"),List.of("Availability unconfirmed")));
  Map<String,String> facts=Map.of("name",e.getName(),"type",e.getType(),"dateTime",start.toString());
  e.storeCommunication(new Communication(id(),Communication.Channel.EMAIL,"friendly","Approved announcement",true,facts));e.storeCommunication(new Communication(id(),Communication.Channel.WHATSAPP,"casual","Unapproved announcement",false,facts));
  var unknown=new PostEventData(null,null,List.of(),List.of(),List.of("Organizer feedback"),List.of("Attendee feedback"),List.of("Late start"));e.storePostEventData(unknown);
  var zero=new PostEventData(0,new BigDecimal("0.00"),List.of(),List.of(task.id()),List.of(),List.of(),List.of());
  e.storeReport(new PostEventReport("Limited evidence",null,null,0,0,null,List.of(),List.of("Late start"),List.of("Collect feedback"),List.of("Record outcomes"),List.of("Actuals unknown"),unknown,80,new BigDecimal("100.00")));
  e.storeReport(new PostEventReport("Recorded zero",-80,new BigDecimal("-100.00"),0,1,0.0,List.of(),List.of(),List.of(),List.of(),List.of(),zero,80,new BigDecimal("100.00")));return e;
 }
 @Test void missingStoreStartsEmptyAndCreationReloadsExactValues(){
  String eventId,teamId;
  try(var f=open()){
   assertTrue(f.listEvents().isEmpty());assertFalse(Files.exists(directory.resolve("planit.json")));
   var start=LocalDateTime.parse("2099-03-08T02:30:12.123456789");var event=f.createEventWithEnd("Club","Social","Campus",start,start.plusHours(2),80,700.25);eventId=event.getId();teamId=event.getPlanningTeam().getId();f.addIdea(eventId,"Alex","Workshop");
  }
  try(var f=open()){
   Event event=f.viewEvent(eventId);assertEquals("Club",event.getName());assertEquals(teamId,event.getPlanningTeam().getId());assertEquals(80,event.getExpectedAttendance());assertEquals(700.25,event.getTotalBudget());assertEquals(LocalDateTime.parse("2099-03-08T02:30:12.123456789"),event.getDateTime());assertEquals(event.getDateTime().plusHours(2),event.getEndDateTime());assertEquals("Workshop",f.listIdeas(eventId).getFirst().text());
  }
 }
 @Test void fullGraphRoundTripPreservesIdsEvidenceNullsZeroAndCanonicalTeamLinks()throws Exception{
  Event original=richEvent();PlanData.EventData expected;
  try(var f=seeded(original)){f.addConstraint(original.getId(),"Saved mutation");expected=PlanDataMapper.capture(f.viewEvent(original.getId()));}
  try(var f=open()){
   Event restored=f.viewEvent(original.getId());assertEquals(expected,PlanDataMapper.capture(restored));assertSame(restored,restored.getPlanningTeam().getEvents().getFirst());
   assertEquals(restored.getPlanningTeam().getMembers().getFirst().getId(),restored.getTasks().getFirst().assignedUserId());assertEquals(restored.getSpeakers().getFirst().id(),restored.getSchedule().sessions().getFirst().speakerId());
   assertEquals(restored.getVenueOptions().getFirst().id(),restored.getVenueComparisons().getFirst().venueId());assertNull(restored.getVenueOptions().getFirst().rentalCost().value());
   assertNull(restored.getPostEventData().actualAttendance());assertEquals(0,restored.getReports().getLast().outcomes().actualAttendance());assertEquals(new BigDecimal("0.00"),restored.getReports().getLast().outcomes().actualExpenses());assertTrue(restored.getCommunications().getFirst().approved());assertFalse(restored.getCommunications().getLast().approved());
  }
  String json=Files.readString(directory.resolve("planit.json"));assertFalse(json.contains("observers"));assertFalse(json.contains("apiKey"));assertFalse(json.contains("transport"));assertFalse(json.contains("geminiConfiguration"));
 }
 @Test void draftsRemainSeparateFromApprovedPlansAndCanBeApprovedAfterRestart(){
  Event event=richEvent();String id=event.getId();Task task=event.getTasks().getFirst();Session session=event.getSchedule().sessions().getFirst();BudgetItem item=event.getBudget().items().getFirst();
  try(var f=seeded(event)){
   f.updateTaskStatus(id,task.id(),Task.Status.IN_PROGRESS);
   f.reviseSession(id,new Session(session.id(),"Revised welcome",session.kind(),session.slot(),session.speakerId(),session.space(),session.resources()));
   f.editBudget(id,new BudgetItem(item.id(),item.category(),item.description(),item.quantity(),new BigDecimal("90.00"),BudgetItem.PriceStatus.ESTIMATED,"Revised estimate"));f.proposeChangeFields(id,Map.of("budget","700"));
  }
  try(var f=open()){
   assertTrue(f.hasTaskDraft(id));assertTrue(f.hasScheduleDraft(id));assertTrue(f.hasBudgetDraft(id));
   assertEquals(Task.Status.TODO,f.viewEvent(id).getTasks().getFirst().status());assertEquals(Task.Status.IN_PROGRESS,f.listTasks(id).getFirst().status());
   assertEquals("Welcome",f.viewEvent(id).getSchedule().sessions().getFirst().title());assertEquals("Revised welcome",f.viewSchedule(id).sessions().getFirst().title());
   assertEquals(new BigDecimal("100.00"),f.viewEvent(id).getBudget().items().getFirst().unitCost());assertEquals(new BigDecimal("90.00"),f.viewBudget(id).items().getFirst().unitCost());
   assertEquals("700",f.pendingChanges(id).changes().get("budget"));assertEquals(1000,f.viewEvent(id).getTotalBudget());
   f.approveTasks(id);f.approveSchedule(id);f.approveBudget(id);f.confirmChange(id,false);
  }
  try(var f=open()){assertFalse(f.hasTaskDraft(id));assertFalse(f.hasScheduleDraft(id));assertFalse(f.hasBudgetDraft(id));assertEquals(Task.Status.IN_PROGRESS,f.viewEvent(id).getTasks().getFirst().status());assertEquals("Revised welcome",f.viewEvent(id).getSchedule().sessions().getFirst().title());assertEquals(new BigDecimal("90.00"),f.viewEvent(id).getBudget().items().getFirst().unitCost());assertThrows(IllegalArgumentException.class,()->f.pendingChanges(id));}
 }
 @Test void unknownBudgetDraftDoesNotBecomeApprovedOnReload(){
  Event event=richEvent();String id=event.getId();var item=event.getBudget().items().getFirst();
  try(var f=seeded(event)){f.editBudget(id,new BudgetItem(item.id(),item.category(),item.description(),item.quantity(),null,BudgetItem.PriceStatus.UNKNOWN,"No quote"));}
  try(var f=open()){assertTrue(f.hasBudgetDraft(id));assertNull(f.viewBudget(id).items().getFirst().unitCost());assertThrows(IllegalArgumentException.class,()->f.approveBudget(id));assertEquals(new BigDecimal("100.00"),f.viewEvent(id).getBudget().items().getFirst().unitCost());f.discardBudget(id);}
  try(var f=open()){assertFalse(f.hasBudgetDraft(id));}
 }
 @Test void confirmedChangeAndObserverRevalidationSurviveRestart(){
  Event event=richEvent();String id=event.getId();
  try(var f=seeded(event)){f.proposeChangeFields(id,Map.of("budget","700"));f.confirmChange(id,true);}
  try(var f=open()){assertEquals(700,f.viewEvent(id).getTotalBudget());f.proposeChangeFields(id,Map.of("budget","50"));f.confirmChange(id,true);assertTrue(f.viewEvent(id).getReviewWarnings().keySet().containsAll(List.of("tasks","schedule","budget","communications")));}
  try(var f=open()){assertEquals(50,f.viewEvent(id).getTotalBudget());assertEquals(new BigDecimal("100.00"),f.viewEvent(id).getBudget().items().getFirst().unitCost());assertTrue(f.viewEvent(id).getReviewWarnings().get("budget").stream().anyMatch(s->s.contains("Over budget")));assertTrue(f.viewEvent(id).getCommunications().stream().noneMatch(Communication::approved));}
 }
 @Test void hypotheticalScenarioNeverChangesSavedOriginal()throws Exception{
  var replies=new ArrayDeque<>(List.of("{\"changes\":{\"attendance\":\"150\"},\"ambiguities\":[]}","{\"affectedComponents\":[\"catering\"],\"implications\":[],\"warnings\":[],\"recommendations\":[]}"));
  Event event=richEvent();String id=event.getId();var events=new EventService();events.createEvent(event);
  try(var f=facade(events,new JsonPlanRepository(directory),p->replies.remove())){
   f.addConstraint(id,"Saved before simulation");byte[] before=Files.readAllBytes(directory.resolve("planit.json"));
   var simulation=f.simulateScenario(id,"Attendance increases to 150");assertEquals(150,simulation.snapshot().getExpectedAttendance());assertEquals(80,f.viewEvent(id).getExpectedAttendance());assertArrayEquals(before,Files.readAllBytes(directory.resolve("planit.json")));
  }
  try(var f=open()){assertEquals(80,f.viewEvent(id).getExpectedAttendance());assertThrows(IllegalArgumentException.class,()->f.pendingChanges(id));}
 }
 @Test void deletionRemovesSavedEventDraftsAndTeamLinks(){
  Event first=richEvent();String id=first.getId();var second=new Event(id(),"Second","Social","Campus",first.getDateTime(),20,500,first.getPlanningTeam());first.getPlanningTeam().addEvent(second);
  var events=new EventService();events.createEvent(first);events.createEvent(second);
  try(var f=facade(events,new JsonPlanRepository(directory),p->{throw new AssertionError();})){f.updateTaskStatus(id,first.getTasks().getFirst().id(),Task.Status.DONE);f.deleteEvent(id);}
  try(var f=open()){assertEquals(1,f.listEvents().size());assertThrows(IllegalArgumentException.class,()->f.viewEvent(id));assertSame(f.viewEvent(second.getId()),f.viewEvent(second.getId()).getPlanningTeam().getEvents().getFirst());}
 }
 @Test void malformedOrUnsupportedDataIsProtectedAndLockReleased()throws Exception{
  for(String malformed:List.of("", "{broken", "null", "{\"schemaVersion\":99,\"events\":[],\"drafts\":{}}", "{\"schemaVersion\":1,\"events\":[],\"drafts\":{}} trailing")){
   Files.writeString(directory.resolve("planit.json"),malformed);
   try(var repository=new JsonPlanRepository(directory)){
    assertThrows(StorageException.class,repository::load);assertThrows(StorageException.class,()->repository.save(new PlanData(1,List.of(),Map.of())));assertEquals(malformed,Files.readString(repository.file()));
   }
  }
 }
 @Test void failedReplacementKeepsPreviousFileAndRollsBackMutation()throws Exception{
  var fail=new AtomicBoolean();var repository=new JsonPlanRepository(directory){@Override protected void replace(Path temporary,Path destination)throws java.io.IOException{if(fail.get())throw new java.io.IOException("Injected failure");super.replace(temporary,destination);}};
  String id;byte[] before;
  try(var f=facade(new EventService(),repository,p->{throw new AssertionError();})){
   id=f.createEvent("Club","Social","Campus",LocalDateTime.parse("2099-01-01T18:00"),80,1000).getId();before=Files.readAllBytes(repository.file());fail.set(true);
   assertThrows(StorageException.class,()->f.addConstraint(id,"Must not appear"));assertTrue(f.viewEvent(id).getConstraints().isEmpty());assertArrayEquals(before,Files.readAllBytes(repository.file()));
   fail.set(false);f.addConstraint(id,"Retry succeeded");
  }
  try(var f=open()){assertEquals(List.of("Retry succeeded"),f.viewEvent(id).getConstraints());}
  try(var files=Files.list(directory)){assertTrue(files.noneMatch(p->p.toString().endsWith(".tmp")));}
 }
 @Test void secondSessionCannotWriteAndExternalFileChangesAreDetected()throws Exception{
  try(var f=open()){
   assertThrows(StorageException.class,()->new JsonPlanRepository(directory));String id=f.createEvent("Club","Social","Campus",LocalDateTime.parse("2099-01-01T18:00"),80,1000).getId();
   String external=Files.readString(directory.resolve("planit.json"))+"\n ";Files.writeString(directory.resolve("planit.json"),external);
   assertThrows(StorageException.class,()->f.addConstraint(id,"Conflict"));assertTrue(f.viewEvent(id).getConstraints().isEmpty());assertEquals(external,Files.readString(directory.resolve("planit.json")));
  }
  try(var f=open()){assertEquals(1,f.listEvents().size());}
 }
 @Test void concurrentFacadeMutationsAreSerializedAndReadOnlyCallsDoNotSave()throws Exception{
  String id;
  try(var f=open()){
   id=f.createEvent("Club","Social","Campus",LocalDateTime.parse("2099-01-01T18:00"),80,1000).getId();
   var pool=Executors.newFixedThreadPool(4);try{var futures=new ArrayList<Future<?>>();for(int i=0;i<12;i++){int n=i;futures.add(pool.submit(()->f.addConstraint(id,"Constraint "+n)));}for(var future:futures)future.get(15,TimeUnit.SECONDS);}finally{pool.shutdown();}
   byte[] before=Files.readAllBytes(directory.resolve("planit.json"));var timestamp=Files.getLastModifiedTime(directory.resolve("planit.json"));f.listEvents();f.viewEvent(id);f.listTasks(id);f.listConcepts(id);f.taskWarnings(id);f.askFaq(id,"when?");
   assertArrayEquals(before,Files.readAllBytes(directory.resolve("planit.json")));assertEquals(timestamp,Files.getLastModifiedTime(directory.resolve("planit.json")));
  }
  try(var f=open()){assertEquals(12,new HashSet<>(f.viewEvent(id).getConstraints()).size());}
 }
 @Test void generatedConceptBlueprintTaskScheduleAndBudgetDraftsAreAutomaticallySaved(){
  Event event=com.eventplanner.support.ExecutionFixtures.event();String id=event.getId();var events=new EventService();events.createEvent(event);
  var responses=new ArrayDeque<>(List.of(com.eventplanner.support.PlanningFixtures.concepts(event).toString(),com.eventplanner.support.PlanningFixtures.blueprints().toString(),com.eventplanner.support.ExecutionFixtures.tasksJson("alex").toString(),com.eventplanner.support.ExecutionFixtures.scheduleJson().toString(),com.eventplanner.support.CommercialFixtures.budgetJson().toString()));
  String conceptId,blueprintId,taskId,scheduleId,budgetId;
  try(var f=new EventPlanningFacade(events,new EventPlanningAgent(p->responses.remove()),q->com.eventplanner.support.CommercialFixtures.providers(),q->com.eventplanner.support.CommercialFixtures.providers(),new JsonPlanRepository(directory))){
   conceptId=f.fuseIdeas(id).getFirst().id();blueprintId=f.generateBlueprints(id,conceptId,"community").getFirst().id();taskId=f.generateTasks(id).getFirst().id();scheduleId=f.buildSchedule(id).id();budgetId=f.generateBudget(id,"budget").id();assertTrue(responses.isEmpty());
  }
  try(var f=open()){
   assertEquals(conceptId,f.listConcepts(id).getFirst().id());assertEquals(blueprintId,f.listBlueprints(id).getFirst().id());assertEquals(taskId,f.listTasks(id).getFirst().id());assertEquals(scheduleId,f.viewSchedule(id).id());assertEquals(budgetId,f.viewBudget(id).id());
   assertTrue(f.hasTaskDraft(id));assertTrue(f.hasScheduleDraft(id));assertTrue(f.hasBudgetDraft(id));assertTrue(f.viewEvent(id).getTasks().isEmpty());assertNull(f.viewEvent(id).getSchedule());assertNull(f.viewEvent(id).getBudget());
  }
 }
 @Test void generatedCommunicationsContingenciesAndReportsSaveThroughNormalOperations(){
  Event event=richEvent();String id=event.getId();var events=new EventService();events.createEvent(event);
  var responses=new ArrayDeque<>(List.of("{\"factOrder\":[\"name\",\"type\",\"dateTime\"],\"opening\":\"Hello friends!\",\"closing\":\"Join us!\"}","{\"risk\":\"Rain\",\"impact\":\"Outdoor access limited\",\"proposedResponse\":\"Move indoors\",\"resources\":[\"Indoor room\"],\"assumptions\":[\"No forecast retrieved\"]}","{\"executiveSummary\":\"Recorded evidence\",\"successes\":[],\"issues\":[],\"lessonsLearned\":[\"Collect feedback\"],\"recommendations\":[\"Record more outcomes\"]}"));
  String communication;
  try(var f=facade(events,new JsonPlanRepository(directory),p->responses.remove())){
   communication=f.generateCommunication(id,Communication.Channel.INSTAGRAM,"friendly").id();f.editCommunication(id,communication,"Planner revised this draft");f.approveCommunication(id,communication);f.generateContingency(id,"Rain");
   f.recordPostEvent(id,new PostEventData(0,BigDecimal.ZERO,List.of(),List.of(event.getTasks().getFirst().id()),List.of("Organizer feedback"),List.of("Attendee feedback"),List.of()));f.analyzePostEvent(id);
  }
  try(var f=open()){assertTrue(f.viewCommunication(id,communication).approved());assertEquals("Planner revised this draft",f.viewCommunication(id,communication).text());assertEquals("Rain",f.listContingencies(id).getLast().risk());assertEquals(0,f.viewEvent(id).getPostEventData().actualAttendance());assertEquals("Recorded evidence",f.viewPostEventReports(id).getLast().executiveSummary());}
 }
 @Test void semanticCorruptionAndStalePendingChangesStayProtected()throws Exception{
  Event event=richEvent();String id=event.getId();
  try(var f=seeded(event)){f.proposeChangeFields(id,Map.of("budget","700"));f.configureEventEnd(id,event.getEndDateTime().plusHours(1));}
  try(var f=open()){assertThrows(IllegalArgumentException.class,()->f.confirmChange(id,true));assertEquals(1000,f.viewEvent(id).getTotalBudget());}
  var json=StorageJson.mapper();var root=json.readTree(Files.readAllBytes(directory.resolve("planit.json")));
  ((com.fasterxml.jackson.databind.node.ArrayNode)root.path("events").get(0).path("team").path("eventIds")).add("missing-event");String corrupted=json.writeValueAsString(root);Files.writeString(directory.resolve("planit.json"),corrupted);
  try(var repository=new JsonPlanRepository(directory)){assertThrows(StorageException.class,repository::load);assertEquals(corrupted,Files.readString(repository.file()));}
 }
 @Test void sharedMemberObjectsAndSeparateEventProfilesKeepTheirIdentityRelationships(){
  var start=LocalDateTime.parse("2099-01-01T18:00");var first=new Event(id(),"First","Social","Campus",start,20,500);var second=new Event(id(),"Second","Social","Campus",start,20,500);var third=new Event(id(),"Third","Social","Campus",start,20,500);
  var member=new User(id(),"Alex","","Planner");member.configurePlanning(List.of(),List.of(),List.of(),0,10);first.getPlanningTeam().addMember(member);second.getPlanningTeam().addMember(member);
  var profile=new User(member.getId(),"Alex","","Planner");profile.configurePlanning(List.of("setup"),List.of(),List.of(),0,5);third.getPlanningTeam().addMember(profile);
  var events=new EventService();events.createEvent(first);events.createEvent(second);events.createEvent(third);
  try(var f=facade(events,new JsonPlanRepository(directory),p->{throw new AssertionError();})){f.addConstraint(first.getId(),"Persist graph");}
  try(var f=open()){
   User a=f.viewEvent(first.getId()).getPlanningTeam().getMembers().getFirst(),b=f.viewEvent(second.getId()).getPlanningTeam().getMembers().getFirst(),c=f.viewEvent(third.getId()).getPlanningTeam().getMembers().getFirst();
   assertSame(a,b);assertNotSame(a,c);assertEquals(a.getId(),c.getId());assertEquals(10,a.getCapacityHours());assertEquals(5,c.getCapacityHours());
  }
 }
}
