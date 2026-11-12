package com.eventplanner.view.gui;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.*;
import java.util.Objects;

/** One application controller and active event for the lifetime of the desktop session. */
public final class GuiSession {
 private final EventPlanningController controller;
 private String activeId;
 private final java.util.Map<String,java.util.List<String>> venueWarnings=new java.util.HashMap<>();
 void venueWarnings(String id,java.util.List<String> warnings){venueWarnings.put(id,java.util.List.copyOf(warnings));}
 java.util.List<String> venueWarnings(String id){return venueWarnings.getOrDefault(id,java.util.List.of());}
 public GuiSession(EventPlanningController controller) { this.controller=Objects.requireNonNull(controller); }
 public EventPlanningController controller() { return controller; }
 public String activeOrNullId() { return activeId; }
 public String activeId() { if(activeId==null) throw new IllegalArgumentException("Create or select an event first."); return activeId; }
 public void select(String id) { if(id!=null) controller.viewEvent(id); activeId=id; }
 public Event active() { return controller.viewEvent(activeId()); }
 public Event create(String name,String type,String location,String start,String end,String attendance,String budget) {
  var from=GuiInputs.date(start); var until=GuiInputs.date(end); new TimeSlot(from,until);
  int count=GuiInputs.integer(attendance); double amount=GuiInputs.number(budget);
  Event event=controller.createEventWithEnd(name,type,location,from,until,count,amount);
  select(event.getId()); return event;
 }
 public void deleteActive() { controller.deleteEvent(activeId()); activeId=null; }
}
