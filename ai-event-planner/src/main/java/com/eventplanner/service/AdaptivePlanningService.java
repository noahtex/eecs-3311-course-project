package com.eventplanner.service;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.model.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.util.*;

/** F07-F10 orchestration. Numerical results and approved-fact rendering stay in Java. */
public final class AdaptivePlanningService {
 private final EventPlanningAgent agent;
 private final Map<Event,PlanChange> pending=new HashMap<>();
 private final ObjectMapper json=new ObjectMapper().registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
  .addSerializer(java.time.LocalDateTime.class,new JsonSerializer<java.time.LocalDateTime>() {
   @Override public void serialize(java.time.LocalDateTime value,com.fasterxml.jackson.core.JsonGenerator generator,SerializerProvider provider) throws java.io.IOException { generator.writeString(value.toString()); }
  }).addSerializer(java.time.Instant.class,new JsonSerializer<java.time.Instant>() {
   @Override public void serialize(java.time.Instant value,com.fasterxml.jackson.core.JsonGenerator generator,SerializerProvider provider) throws java.io.IOException { generator.writeString(value.toString()); }
  }));
 public AdaptivePlanningService(EventPlanningAgent agent) { this.agent=agent; }
 private String encode(Object value) {
  try { return json.writeValueAsString(value); } catch(Exception ex) { throw new IllegalArgumentException("Cannot encode planning context."); }
 }
 private String context(Event e) {
  var data=new LinkedHashMap<String,Object>(); data.put("event",e.changeFields()); data.put("constraints",e.getConstraints());
  data.put("approvedTasks",e.getTasks()); data.put("approvedSchedule",e.getSchedule()); data.put("approvedBudget",e.getBudget());
  data.put("speakers",e.getSpeakers()); data.put("resources",e.getResourceAvailability());
  data.put("currency",e.getPlanningCurrency()); data.put("dietaryRequirements",e.getDietaryRequirements());
  data.put("cateringNeeds",e.getCateringNeeds()); data.put("equipment",e.getRequiredEquipment());
  data.put("wheelchairEntranceRequired",e.requiresWheelchairEntrance()); data.put("venueCandidatesNotBookings",e.getVenueOptions());
  data.put("blueprintsUnapproved",e.getBlueprints()); return encode(data);
 }
 private String text(JsonNode root,String field) {
  if(!root.path(field).isTextual()) throw new PlanningException("AI response missing text field: "+field);
  return PlanningValues.text(root.path(field).textValue(),field);
 }
 private List<String> texts(JsonNode root,String field) {
  if(!root.path(field).isArray()) throw new PlanningException("AI response missing list: "+field);
  var result=new ArrayList<String>();
  for(var item:root.path(field)) { if(!item.isTextual()) throw new PlanningException("Invalid AI list: "+field); result.add(PlanningValues.text(item.textValue(),field)); }
  return List.copyOf(result);
 }
 public PlanChange interpret(Event e,String request) {
  var root=agent.milestoneOutput("Interpret a hypothetical event change. Return {\"changes\":{field:string},\"ambiguities\":[string]}. "
   +"Allowed fields: name,type,location,dateTime,endDateTime,attendance,budget,unavailableSpeaker (existing speaker ID). "
   +"Numbers must be strings, dates ISO local datetime. Do not infer unspecified values. If ambiguous or unsupported, explain in ambiguities. No changes is allowed.",
   context(e)+"\nRequest: "+encode(PlanningValues.text(request,"Request")));
  var ambiguities=texts(root,"ambiguities"); if(!ambiguities.isEmpty()) throw new IllegalArgumentException("Clarify: "+String.join("; ",ambiguities));
  if(!root.path("changes").isObject()) throw new PlanningException("AI response missing changes object.");
  Map<String,String> changes=new LinkedHashMap<>();
  root.path("changes").fields().forEachRemaining(entry->{ if(!entry.getValue().isTextual()) throw new IllegalArgumentException("Changes must contain string values."); changes.put(entry.getKey(),entry.getValue().textValue()); });
  return checked(e,changes);
 }
 private PlanChange checked(Event e,Map<String,String> changes) {
  var proposal=new PlanChange(e.changeFields(),changes); e.snapshot().applyChanges(changes); return proposal;
 }
 public PlanChange propose(Event e,String request) { var value=interpret(e,request); pending.put(e,value); return value; }
 public PlanChange proposeFields(Event e,Map<String,String> changes) { var value=checked(e,changes); pending.put(e,value); return value; }
 public PlanChange changes(Event e) { var value=pending.get(e); if(value==null) throw new IllegalArgumentException("No pending event changes."); return value; }
 public void confirm(Event e,boolean confirmed) {
  var value=changes(e);
  if(confirmed) {
   if(!value.before().equals(e.changeFields())) throw new IllegalArgumentException("Event changed since proposal. Propose again.");
   e.applyChanges(value.changes());
  }
  pending.remove(e);
 }
 public ScenarioResult simulate(Event e,String request) {
  PlanChange change=interpret(e,request); Event copy=e.snapshot(); copy.applyChanges(change.changes());
  var root=agent.milestoneOutput("F07 scenario analysis. Return {\"affectedComponents\":[string],\"implications\":[string],\"warnings\":[string],\"recommendations\":[string]}. "
   +"Discuss scheduling, resources, budget and feasibility. No changes are approved. Budget estimates must be labelled assumptions; never assert costs as confirmed.",
   "Before: "+context(e)+" After: "+context(copy));
  var affected=new LinkedHashSet<>(texts(root,"affectedComponents")); affected.addAll(change.changes().keySet());
  var warnings=new ArrayList<>(texts(root,"warnings")); warnings.add("Simulation only. Review free-text constraints; new costs and resource needs are unverified.");
  if(copy.getBudget()!=null) warnings.addAll(new BudgetService().validateBudget(copy,copy.getBudget()));
  if(copy.getSchedule()!=null) warnings.addAll(new ScheduleService().detectConflicts(copy,copy.getSchedule()));
  warnings.addAll(new TaskService().warnings(copy,copy.getTasks(),List.of()));
  BigDecimal expenseDelta=null;
  if(e.getBudget()!=null && e.getExpectedAttendance()>0 && e.getBudget().items().stream().noneMatch(i->i.priceStatus()==BudgetItem.PriceStatus.UNKNOWN)) {
   BigDecimal subtotal=new BudgetService().totalExpenses(e.getBudget());
   expenseDelta=subtotal.multiply(BigDecimal.valueOf(copy.getExpectedAttendance())).divide(BigDecimal.valueOf(e.getExpectedAttendance()),2,java.math.RoundingMode.HALF_UP).subtract(subtotal);
   warnings.add("Expense delta is a rough attendance-proportional estimate of all approved allocations; fixed costs may not scale. Obtain revised quotes.");
  }
  var implications=new ArrayList<>(texts(root,"implications"));
  implications.add("Approved cost allocations retained for comparison; revised supplier costs are unknown. Spending-limit delta is not a cost estimate.");
  return new ScenarioResult(change,copy,List.copyOf(affected),BigDecimal.valueOf(copy.getTotalBudget()).subtract(BigDecimal.valueOf(e.getTotalBudget())),expenseDelta,implications,warnings,texts(root,"recommendations"));
 }
 public ContingencyPlan contingency(Event e,String risk) {
  var root=agent.milestoneOutput("F08 contingency. Return {\"risk\":string,\"impact\":string,\"proposedResponse\":string,\"resources\":[string],\"assumptions\":[string]}. "
   +"Give practical actions, resource needs and assumptions. No weather data has been retrieved. Do not claim live forecasts or supplier availability.",context(e)+" Risk: "+encode(PlanningValues.text(risk,"Risk")));
  var assumptions=new ArrayList<>(texts(root,"assumptions")); assumptions.add("Unverified recommendation; no weather forecast or supplier availability retrieved.");
  var plan=new ContingencyPlan(text(root,"risk"),text(root,"impact"),text(root,"proposedResponse"),texts(root,"resources"),assumptions); e.addContingency(plan); return plan;
 }
 /** Location is a planning location, never a confirmed venue booking. Spending limit is not a ticket price. */
 public Map<String,String> approvedFacts(Event e) {
  return Map.of("name",e.getName(),"type",e.getType(),"dateTime",e.getDateTime().toString());
 }
 public Communication communication(Event e,Communication.Channel channel,String tone) {
  tone=PlanningValues.text(tone,"Tone");
  var facts=approvedFacts(e);
  var root=agent.milestoneOutput("F09 generate a channel-appropriate opening and closing for the requested tone. "
   +"Return {\"factOrder\":[string],\"opening\":string,\"closing\":string}. "
   +"Use each supplied fact key exactly once. Factual content is rendered separately by Java. "
   +"Opening and closing may contain only these nonfactual words (case insensitive), whitespace and punctuation: "
   +"hello hi hey dear everyone friends community event announcement reminder invitation you youre are invited join us "
   +"please welcome looking forward to seeing we hope see there thank thanks sincerely regards best warmly lets connect celebrate together. "
   +"Do not add names, numbers, links, hashtags, dates, promises or other factual claims to opening or closing.",
   encode(Map.of("approvedFacts",facts,"channel",channel.name(),"tone",tone)));
  var order=texts(root,"factOrder");
  if(order.size()!=facts.size() || !new HashSet<>(order).equals(facts.keySet())) throw new PlanningException("Communication response must preserve every approved fact.");
  String opening=safeGreeting(text(root,"opening")), closing=safeGreeting(text(root,"closing"));
  String separator=channel==Communication.Channel.EMAIL?"\n":" | ";
  String body=opening+separator+String.join(separator,order.stream().map(key->key+": "+facts.get(key)).toList())
   +separator+closing+separator+"Venue, registration, price, accessibility and contact details are not confirmed.";
  var result=new Communication(UUID.randomUUID().toString(),channel,tone,body,false,facts); e.storeCommunication(result); return result;
 }
 private String safeGreeting(String value) {
  Set<String> allowed=Set.of(("hello hi hey dear everyone friends community event announcement reminder invitation you youre are invited join us "
   +"please welcome looking forward to seeing we hope see there thank thanks sincerely regards best warmly lets connect celebrate together").split(" "));
  if(!value.matches("[a-zA-Z ,.!?'\\-]+")) throw new PlanningException("Communication contains unsupported wording.");
  for(String word:value.toLowerCase(Locale.ROOT).replace("'","").split("[^a-z]+"))
   if(!word.isEmpty() && !allowed.contains(word)) throw new PlanningException("Communication adds unsupported wording. Try another tone.");
  return value;
 }
 public Communication findCommunication(Event e,String id) {
  return e.getCommunications().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown communication ID."));
 }
 public Communication editCommunication(Event e,String id,String body) {
  var result=findCommunication(e,id).edit(body); e.storeCommunication(result); return result;
 }
 public Communication approveCommunication(Event e,String id) {
  var draft=findCommunication(e,id);
  if(!draft.sourceFacts().equals(approvedFacts(e))) throw new IllegalArgumentException("Event facts changed. Generate a fresh draft before approval.");
  var result=draft.approve(); e.storeCommunication(result); return result;
 }
 public String faq(Event e,String question) {
  String q=PlanningValues.text(question,"Question").toLowerCase(Locale.ROOT);
  // Conservative whole-question intent whitelist: mixed/unknown requests cannot leak planning assumptions.
  if(q.matches("(when|what time|what date|when is the event)\\??")) return "Event date/time: "+e.getDateTime()+" (event local time).";
  if(q.matches("(what is the event name|event name|name)\\??")) return "Event name: "+e.getName();
  if(q.matches("(what type of event|event type|type)\\??")) return "Event type: "+e.getType();
  return "That information is not confirmed. Ask the organizer for confirmation.";
 }
 public void record(Event e,PostEventData data) {
  Set<String> ids=new HashSet<>(); e.getTasks().forEach(t->ids.add(t.id()));
  if(!ids.containsAll(data.completedTaskIds())||!ids.containsAll(data.unfinishedTaskIds())) throw new IllegalArgumentException("Task outcomes must reference approved task IDs.");
  e.storePostEventData(data);
 }
 public PostEventReport analyze(Event e) {
  var data=e.getPostEventData(); if(data==null) throw new IllegalArgumentException("Record post-event outcomes first (unknown fields may be blank).");
  var missing=new ArrayList<String>();
  if(data.actualAttendance()==null) missing.add("Actual attendance unknown.");
  if(data.actualExpenses()==null) missing.add("Actual expenses unknown.");
  BigDecimal planned=null;
  if(e.getBudget()!=null && e.getBudget().currency().equals(e.getPlanningCurrency()) && e.getBudget().items().stream().noneMatch(i->i.priceStatus()==BudgetItem.PriceStatus.UNKNOWN)) planned=new BudgetService().totalExpenses(e.getBudget());
  if(planned==null) missing.add("Approved planned expense total unknown; spending limit is not planned expense.");
  if(data.organizerFeedback().isEmpty()) missing.add("Organizer feedback missing.");
  if(data.attendeeFeedback().isEmpty()) missing.add("Attendee feedback missing.");
  int completed=data.completedTaskIds().size(), unfinished=data.unfinishedTaskIds().size();
  if(completed+unfinished<e.getTasks().size()) missing.add("Some task outcomes are unrecorded.");
  if(data.problems().isEmpty()) missing.add("Problems not reported; absence of entries does not establish absence of problems.");
  var reviewContext=new LinkedHashMap<String,Object>();
  reviewContext.put("outcomes",data); reviewContext.put("missingInformation",missing);
  reviewContext.put("expectedAttendance",e.getExpectedAttendance()); reviewContext.put("plannedExpenseTotal",planned);
  reviewContext.put("approvedPlan",context(e));
  reviewContext.put("attendanceDifference",data.actualAttendance()==null?null:data.actualAttendance()-e.getExpectedAttendance());
  reviewContext.put("expenseDifference",data.actualExpenses()==null||planned==null?null:data.actualExpenses().subtract(planned));
  var root=agent.milestoneOutput("F10 qualitative review. Return {\"executiveSummary\":string,\"successes\":[string],\"issues\":[string],\"lessonsLearned\":[string],\"recommendations\":[string]}. "
   +"Use only recorded outcomes. Preserve conflicting organizer and attendee perspectives, attribute them separately. Do not invent outcomes or feedback. Identify limited evidence.",
   encode(reviewContext));
  var report=new PostEventReport(text(root,"executiveSummary"),data.actualAttendance()==null?null:data.actualAttendance()-e.getExpectedAttendance(),
   data.actualExpenses()==null||planned==null?null:data.actualExpenses().subtract(planned),completed,unfinished,
   e.getTasks().isEmpty()?null:100.0*completed/e.getTasks().size(),texts(root,"successes"),texts(root,"issues"),texts(root,"lessonsLearned"),texts(root,"recommendations"),missing,data,e.getExpectedAttendance(),planned);
  e.storeReport(report); return report;
 }
}
