package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DisplayNamesTest {
 @Test void duplicatePeopleHaveDistinctLabelsButKeepTheirExactIdentifiers(){
  var event=new Event("event","Club","Social","Campus",LocalDateTime.parse("2027-03-14T02:30"),80,1000);
  String first=UUID.randomUUID().toString(),second=UUID.randomUUID().toString();
  event.getPlanningTeam().addMember(new User(first,"Alex","","Planner"));event.getPlanningTeam().addMember(new User(second,"Alex","","Planner"));
  event.putSpeaker(new Speaker(first,"Sam",List.of()));event.putSpeaker(new Speaker(second,"Sam",List.of()));
  var names=new DisplayNames(event);
  assertNotEquals(names.member(first),names.member(second));assertNotEquals(names.speaker(first),names.speaker(second));
  assertFalse(names.member(first).contains(first));assertFalse(names.speaker(first).contains(first));
  assertEquals(Set.of("",first,second),new HashSet<>(names.memberOptions(null).stream().map(DisplayNames.Option::id).toList()));
  assertEquals(first,event.getPlanningTeam().getMembers().getFirst().getId());assertEquals(first,event.getSpeakers().getFirst().id());
  assertEquals("Unknown member",names.member("missing"));assertEquals("Unknown speaker",names.speaker("missing"));assertEquals("Unassigned",names.member(null));
  assertTrue(names.memberOptions("missing").stream().anyMatch(o->o.id().equals("missing")&&o.label().equals("Unknown member")));
 }
 @Test void referencesAreResolvedWithoutChangingInputContentOrBackendDates(){
  var local=LocalDateTime.parse("2027-03-14T02:30");var event=new Event("event","Club","Social","Campus",local,80,1000);
  String member=UUID.randomUUID().toString();event.getPlanningTeam().addMember(new User(member,"Alex","","Planner"));
  var names=new DisplayNames(event);String original="Review workload for "+member+". Supplier costs remain UNKNOWN.";
  assertEquals("Review workload for Alex. Supplier costs remain UNKNOWN.",names.readable(original));
  assertTrue(original.contains(member));assertEquals(local,event.getDateTime());assertTrue(DisplayNames.date(local).contains("02:30"));
  assertEquals("UNKNOWN",DisplayNames.money(null,"CAD"));
 }
}
