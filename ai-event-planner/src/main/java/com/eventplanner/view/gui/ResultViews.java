package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import java.util.*;

/** Render model fields directly. Exact original content remains available in collapsed details. */
final class ResultViews {
 private ResultViews(){}
 private static VBox stack(Node... nodes){return new VBox(14,nodes);}
 private static Node list(String heading,List<?> values,DisplayNames names){return Ui.bullets(heading,values.stream().map(v->names.readable(String.valueOf(v))).toList());}
 static Node warnings(String heading,String text,DisplayNames names){return stack(Ui.section(heading,names.readable(text)),Ui.technical(text));}
 static Node concept(EventConcept value,DisplayNames names){return stack(Ui.badge("UNVERIFIED PROPOSAL"),Ui.section(value.title(),value.summary()),list("Themes",value.themes(),names),list("Conflicts",value.conflicts(),names),list("Compromises",value.compromises(),names),list("Uncertainties",value.uncertainties(),names),Ui.technical(value.toString()));}
 static Node blueprint(EventBlueprint b,DisplayNames names,List<String> constraints){return stack(
  Ui.badge("UNVERIFIED PROPOSAL"),Ui.section(b.title(),b.summary()),
  Ui.metrics(Ui.metric("Estimated budget",b.estimatedBudget()),Ui.metric("Planned attendees",b.plannedAttendance()),Ui.metric("Required capacity",b.requiredCapacity())),
  Ui.section("Planning approach",b.priority()+" priority · Concept: "+names.concept(b.conceptId())),
  list("Agenda",b.agenda(),names),list("Event constraints",constraints,names),list("Feasibility warnings",b.feasibilityWarnings(),names),
  list("Venue requirements / assumptions",b.venueRequirements(),names),list("Catering assumptions",b.cateringAssumptions(),names),
  list("Resources",b.resources(),names),list("Recommended checklist",b.checklist(),names),list("Assumptions",b.assumptions(),names),Ui.technical(b.toString()));}
 static Node member(User user,DisplayNames names){return stack(Ui.section(names.member(user.getId()),"Existing workload: "+user.getExistingWorkloadHours()+" hours · Capacity: "+user.getCapacityHours()+" hours"),list("Skills",user.getSkills(),names),list("Preferences",user.getPreferences(),names),availability("Availability",user.getAvailability()),Ui.technical("Member reference: "+user.getId()));}
 static Node task(Task task,DisplayNames names){return stack(Ui.row(Ui.badge(task.status().name()),Ui.badge(task.priority().name())),Ui.section(task.title(),task.description()),Ui.metrics(Ui.metric("Assigned to",names.member(task.assignedUserId())),Ui.metric("Effort",task.effortHours()+" hours")),Ui.section("Work window","Starts: "+DisplayNames.date(task.workStart())+"\nDeadline: "+DisplayNames.date(task.deadline())),list("Required skills",task.requiredSkills(),names),Ui.technical(task.toString()));}
 static Node session(Session value,DisplayNames names){return stack(Ui.badge(value.kind().name()),Ui.section(value.title(),DisplayNames.date(value.slot().start())+"\nUntil "+DisplayNames.date(value.slot().end())),Ui.metrics(Ui.metric("Speaker",names.speaker(value.speakerId())),Ui.metric("Space",names.resource(value.space()))),list("Exclusive resources",value.resources().stream().map(names::resource).toList(),names),Ui.technical(value.toString()));}
 static Node availability(String heading,List<TimeSlot> slots){return Ui.bullets(heading,slots.isEmpty()?List.of("UNKNOWN — no availability recorded"):slots.stream().map(slot->DisplayNames.date(slot.start())+" → "+DisplayNames.date(slot.end())).toList());}
 static Node event(Event event){return stack(Ui.section(event.getName(),event.getType()+" · "+event.getLocation()),Ui.metrics(Ui.metric("Expected attendees",event.getExpectedAttendance()),Ui.metric("Spending limit",DisplayNames.money(event.getTotalBudget(),event.getPlanningCurrency()))),Ui.section("Event local time","Starts: "+DisplayNames.date(event.getDateTime())+"\nEnds: "+DisplayNames.date(event.getEndDateTime())),Ui.bullets("Goals & constraints",event.getConstraints()),Ui.technical("Event reference: "+event.getId()));}
 static Node budgetItem(BudgetItem item,String currency){return stack(Ui.badge(item.priceStatus().name()),Ui.section(item.description(),"Category: "+item.category()),Ui.metrics(Ui.metric("Quantity",item.quantity()),Ui.metric("Unit price",DisplayNames.money(item.unitCost(),currency)),Ui.metric("Line total",DisplayNames.money(item.total(),currency))),Ui.section("Source / quote / assumption",item.source()),Ui.technical(item.toString()));}
 static Node catering(Budget budget,DisplayNames names,java.util.function.Consumer<String> browser){
  var box=new VBox(14);
  for(var option:budget.catering()){
   var provider=budget.providers().stream().filter(p->p.id().equals(option.providerId())).findFirst();
   var recommendation=stack(Ui.badge("AI RECOMMENDATION · NOT A SUPPLIER QUOTE"),Ui.prose(option.recommendation()),
    provider.isPresent()?ProviderViews.catering(provider.get(),browser):Ui.note("No catering provider verified"),
    list("Dietary suitability — AI assumptions; confirm with provider",option.dietaryNotes(),names),Ui.technical(option.toString()));
   box.getChildren().add(recommendation);
  }
  if(budget.catering().isEmpty())box.getChildren().add(Ui.note("No catering recommendations recorded."));
  box.getChildren().add(list("Budget assumptions",budget.assumptions(),names));
  box.getChildren().add(Ui.note("Estimated allocations are shown in the budget above. They are not provider quotes. Confirm menus, pricing, dietary suitability and allergen handling directly."));
  for(var provider:budget.providers())if(budget.catering().stream().noneMatch(o->provider.id().equals(o.providerId())))
   box.getChildren().add(Ui.card("Researched provider — not matched to a recommendation",ProviderViews.catering(provider,browser)));
  return box;
 }
 static Node comparison(VenueComparison value,DisplayNames names){return stack(Ui.section(names.venue(value.venueId()),"Known-fit score: "+value.knownFitScore()+" · AI interpretation, not a verified booking"),Ui.prose(names.readable(value.analysis())),list("Warnings",value.warnings(),names),list("Trade-offs",value.tradeOffs(),names),Ui.technical(value.toString()));}
 private record Difference(String field,String original,String simulated){}
 static Node fields(Map<String,String> values,DisplayNames names){
  var box=new VBox(10);new TreeMap<>(values).forEach((key,value)->box.getChildren().add(Ui.field(fieldName(key),Ui.prose(fieldValue(key,value,names)))));return box;
 }
 private static String fieldName(String key){return switch(key){case "dateTime"->"Start date & time";case "endDateTime"->"End date & time";case "unavailableSpeaker"->"Unavailable speaker";case "budget"->"Spending limit";default->key.isEmpty()?key:Character.toUpperCase(key.charAt(0))+key.substring(1);};}
 private static String fieldValue(String key,String value,DisplayNames names){if(value==null||value.isBlank())return "Not specified";if(key.equals("unavailableSpeaker"))return names.speaker(value);if(key.equals("dateTime")||key.equals("endDateTime")){try{return DisplayNames.date(java.time.LocalDateTime.parse(value));}catch(java.time.DateTimeException ignored){}}return names.readable(value);}
 static Node scenario(ScenarioResult value,DisplayNames names){
  var rows=new ArrayList<Difference>();var after=value.snapshot().changeFields();
  new TreeMap<>(value.proposedChanges().before()).forEach((key,before)->rows.add(new Difference(fieldName(key),fieldValue(key,before,names),fieldValue(key,after.get(key),names))));
  var table=Ui.table(rows);Ui.column(table,"Event detail",Difference::field);Ui.column(table,"Original",Difference::original);Ui.column(table,"Hypothetical",Difference::simulated);
  return stack(Ui.badge("HYPOTHETICAL · NOT APPLIED"),Ui.card("Before & after",table),Ui.card("Interpreted changes",fields(value.proposedChanges().changes(),names)),
   Ui.metrics(Ui.metric("Spending-limit difference",DisplayNames.money(value.budgetLimitDifference(),value.snapshot().getPlanningCurrency())),Ui.metric("Estimated expense difference",DisplayNames.money(value.estimatedExpenseDifference(),value.snapshot().getPlanningCurrency()))),
   Ui.note("Spending-limit difference is not a cost estimate. Expense difference is a rough attendance-proportional assumption."),
   list("Affected planning components",value.affectedComponents(),names),list("Impacts & resource implications",value.implications(),names),list("Feasibility issues & assumptions",value.warnings(),names),list("Recommended changes",value.recommendations(),names),Ui.technical(ScenarioPanel.describe(value)));
 }
 static Node contingency(ContingencyPlan value,DisplayNames names){return stack(Ui.badge("UNVERIFIED CONTINGENCY"),Ui.section("Risk",value.risk()),Ui.section("Impact",value.impact()),Ui.section("Recommended response",value.proposedResponse()),list("Resources",value.resources(),names),list("Assumptions",value.assumptions(),names),Ui.technical(value.toString()));}
 static Node report(PostEventReport report,String currency,DisplayNames names){var data=report.outcomes();return stack(
  Ui.section("Executive summary",report.executiveSummary()),
  Ui.metrics(Ui.metric("Planned attendance",report.expectedAttendance()),Ui.metric("Actual attendance",data.actualAttendance()),Ui.metric("Attendance difference",report.attendanceDifference())),
  Ui.metrics(Ui.metric("Approved planned expenses",DisplayNames.money(report.plannedBudget(),currency)),Ui.metric("Actual expenses",DisplayNames.money(data.actualExpenses(),currency)),Ui.metric("Expense variance",DisplayNames.money(report.expenseDifference(),currency))),
  Ui.note("Variances are actual minus planned. Spending limit is not planned expense. Currency follows the event configuration."),
  Ui.metrics(Ui.metric("Completed tasks",report.completedTasks()),Ui.metric("Unfinished tasks",report.unfinishedTasks()),Ui.metric("Completion rate",report.completionRate()==null?"UNKNOWN":report.completionRate()+"%")),
  Ui.note("Other task outcomes may be unrecorded. UNKNOWN values are not zero."),
  list("Completed tasks",data.completedTaskIds().stream().map(names::task).toList(),names),list("Unfinished tasks",data.unfinishedTaskIds().stream().map(names::task).toList(),names),
  list("Organizer feedback",data.organizerFeedback(),names),list("Attendee feedback",data.attendeeFeedback(),names),list("Recorded problems",data.problems(),names),list("Successes",report.successes(),names),list("Issues",report.issues(),names),list("Lessons learned",report.lessonsLearned(),names),list("Recommendations",report.recommendations(),names),list("Missing / unknown information",report.missingInformation(),names),Ui.technical(PostEventPanel.describe(report,currency)));}
}
