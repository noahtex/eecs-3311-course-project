package com.eventplanner.storage;

import com.eventplanner.model.*;
import java.time.LocalDateTime;
import java.util.*;

/** Versioned allowlist: immutable domain value records are safe; aggregates use explicit DTOs. */
public record PlanData(int schemaVersion,List<EventData> events,Map<String,DraftData> drafts) {
 public static final int VERSION=1;
 public PlanData {
  if(schemaVersion!=VERSION)throw new IllegalArgumentException("Unsupported Planit data version.");
  events=List.copyOf(events);drafts=Map.copyOf(drafts);
 }
 public record MemberData(String reference,String id,String name,String email,String role,List<String> skills,List<String> preferences,List<TimeSlot> availability,double workloadHours,double capacityHours){
  public MemberData {PlanningValues.text(reference,"Member object reference");skills=List.copyOf(skills);preferences=List.copyOf(preferences);availability=List.copyOf(availability);}
 }
 public record TeamData(String id,String name,List<MemberData> members,List<String> eventIds){
  public TeamData {members=List.copyOf(members);eventIds=List.copyOf(eventIds);}
 }
 public record DraftData(List<Task> tasks,Schedule schedule,Budget budget,List<String> taskWarnings,PlanChange pendingChange){
  public DraftData {tasks=tasks==null?null:List.copyOf(tasks);taskWarnings=taskWarnings==null?null:List.copyOf(taskWarnings);}
 }
 public record EventData(String id,String name,String type,String location,LocalDateTime start,LocalDateTime end,int attendance,double spendingLimit,
  String currency,List<String> dietaryRequirements,String cateringNeeds,boolean wheelchairEntranceRequired,List<String> requiredEquipment,
  TeamData team,List<EventIdea> ideas,List<String> constraints,List<EventConcept> concepts,List<EventBlueprint> blueprints,
  List<Task> approvedTasks,Schedule approvedSchedule,Budget approvedBudget,List<Speaker> speakers,Map<String,List<TimeSlot>> resources,
  List<VenueOption> venues,List<VenueComparison> venueComparisons,Map<String,List<String>> reviewWarnings,
  List<ContingencyPlan> contingencies,List<Communication> communications,PostEventData actuals,List<PostEventReport> reports){
  public EventData {
   Objects.requireNonNull(team);Objects.requireNonNull(start);
   dietaryRequirements=List.copyOf(dietaryRequirements);requiredEquipment=List.copyOf(requiredEquipment);ideas=List.copyOf(ideas);constraints=List.copyOf(constraints);
   concepts=List.copyOf(concepts);blueprints=List.copyOf(blueprints);approvedTasks=List.copyOf(approvedTasks);speakers=List.copyOf(speakers);
   var slots=new LinkedHashMap<String,List<TimeSlot>>();resources.forEach((key,value)->slots.put(key,List.copyOf(value)));resources=Map.copyOf(slots);
   venues=List.copyOf(venues);venueComparisons=List.copyOf(venueComparisons);
   var warnings=new LinkedHashMap<String,List<String>>();reviewWarnings.forEach((key,value)->warnings.put(key,List.copyOf(value)));reviewWarnings=Map.copyOf(warnings);
   contingencies=List.copyOf(contingencies);communications=List.copyOf(communications);reports=List.copyOf(reports);
  }
 }
}
