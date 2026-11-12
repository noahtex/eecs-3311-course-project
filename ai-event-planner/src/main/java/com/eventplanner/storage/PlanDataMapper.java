package com.eventplanner.storage;

import com.eventplanner.model.*;
import java.util.*;
import static com.eventplanner.storage.PlanData.*;

/** Rebuilds canonical event/team associations without replaying AI, approvals or change notifications. */
public final class PlanDataMapper {
 private PlanDataMapper(){}
 public static EventData capture(Event e){
  return capture(e,new IdentityHashMap<>());
 }
 public static List<EventData> captureEvents(List<Event> events){
  var references=new IdentityHashMap<User,String>();return events.stream().map(event->capture(event,references)).toList();
 }
 private static EventData capture(Event e,IdentityHashMap<User,String> references){
  PlanningTeam t=e.getPlanningTeam();
  var members=t.getMembers().stream().map(u->new MemberData(references.computeIfAbsent(u,key->"member-"+(references.size()+1)),u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getSkills(),u.getPreferences(),u.getAvailability(),u.getExistingWorkloadHours(),u.getCapacityHours())).toList();
  var team=new TeamData(t.getId(),t.getName(),members,t.getEvents().stream().map(Event::getId).toList());
  return new EventData(e.getId(),e.getName(),e.getType(),e.getLocation(),e.getDateTime(),e.getEndDateTime(),e.getExpectedAttendance(),e.getTotalBudget(),
   e.getPlanningCurrency(),e.getDietaryRequirements(),e.getCateringNeeds(),e.requiresWheelchairEntrance(),e.getRequiredEquipment(),team,e.getOrganizerIdeas(),e.getConstraints(),e.getConcepts(),e.getBlueprints(),
   e.getTasks(),e.getSchedule(),e.getBudget(),e.getSpeakers(),e.getResourceAvailability(),e.getVenueOptions(),e.getVenueComparisons(),e.getReviewWarnings(),e.getContingencies(),e.getCommunications(),e.getPostEventData(),e.getReports());
 }
 public static List<Event> restore(PlanData data){
  var events=new LinkedHashMap<String,Event>();var teams=new LinkedHashMap<String,PlanningTeam>();var teamData=new HashMap<String,TeamData>();
  var users=new HashMap<String,User>();var memberData=new HashMap<String,MemberData>();
  for(EventData saved:data.events()){
   PlanningValues.text(saved.id(),"Event ID");PlanningValues.text(saved.name(),"Event name");PlanningValues.text(saved.type(),"Event type");PlanningValues.text(saved.location(),"Event location");
   if(saved.attendance()<0||!Double.isFinite(saved.spendingLimit())||saved.spendingLimit()<0)throw new IllegalArgumentException("Invalid event values.");
   if(events.containsKey(saved.id()))throw new IllegalArgumentException("Duplicate event identity.");
   TeamData t=saved.team();PlanningValues.text(t.id(),"Team ID");PlanningValues.text(t.name(),"Team name");
   if(teamData.containsKey(t.id())&&!teamData.get(t.id()).equals(t))throw new IllegalArgumentException("Conflicting team identity.");
   teamData.put(t.id(),t);
   PlanningTeam team=teams.computeIfAbsent(t.id(),id->{
    var result=new PlanningTeam(id,t.name());var memberIds=new HashSet<String>();
    for(MemberData u:t.members()){
     if(!memberIds.add(PlanningValues.text(u.id(),"Member ID")))throw new IllegalArgumentException("Duplicate member identity.");
     if(memberData.containsKey(u.reference())&&!memberData.get(u.reference()).equals(u))throw new IllegalArgumentException("Conflicting member object reference.");
     memberData.put(u.reference(),u);
     User member=users.computeIfAbsent(u.reference(),key->{var restored=new User(u.id(),PlanningValues.text(u.name(),"Member name"),u.email(),u.role());restored.configurePlanning(u.skills(),u.preferences(),u.availability(),u.workloadHours(),u.capacityHours());return restored;});result.addMember(member);
    }
    return result;
   });
   Event event=new Event(saved.id(),saved.name(),saved.type(),saved.location(),saved.start(),saved.attendance(),saved.spendingLimit(),team);
   if(saved.end()!=null)event.setEndDateTime(saved.end());
   event.configureBudgetNeeds(saved.currency(),saved.dietaryRequirements(),saved.cateringNeeds());event.configureVenueNeeds(saved.wheelchairEntranceRequired(),saved.requiredEquipment());
   event.restoreOrganizerIdeas(saved.ideas());saved.constraints().forEach(event::addConstraint);event.addConcepts(saved.concepts());event.addBlueprints(saved.blueprints());
   event.storeTasks(saved.approvedTasks());if(saved.approvedSchedule()!=null)event.storeSchedule(saved.approvedSchedule());if(saved.approvedBudget()!=null)event.storeBudget(saved.approvedBudget());
   saved.speakers().forEach(event::putSpeaker);saved.resources().forEach(event::putResourceAvailability);event.addVenueOptions(saved.venues());event.storeVenueComparisons(saved.venueComparisons());
   saved.reviewWarnings().forEach(event::flagReview);saved.contingencies().forEach(event::addContingency);saved.communications().forEach(event::storeCommunication);
   if(saved.actuals()!=null)event.storePostEventData(saved.actuals());saved.reports().forEach(event::storeReport);events.put(event.getId(),event);
  }
  for(TeamData team:teamData.values())for(String id:team.eventIds()){
   Event target=events.get(id);if(target==null)throw new IllegalArgumentException("Unknown team event reference.");teams.get(team.id()).addEvent(target);
  }
  if(!events.keySet().containsAll(data.drafts().keySet()))throw new IllegalArgumentException("Draft references unknown event.");
  return List.copyOf(events.values());
 }
}
