package com.eventplanner.view.gui;
import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class GuiSessionTest {
 private EventPlanningFacade facade(){return new EventPlanningFacade(new EventService(),new EventPlanningAgent(prompt->{throw new AssertionError("Unexpected AI request");}),q->{throw new AssertionError("Unexpected external request");},q->{throw new AssertionError("Unexpected external request");});}
 @Test void creationDelegatesThroughSharedControllerAndRetainsSelection(){try(var facade=facade()){
  var controller=new EventPlanningController(facade);var session=new GuiSession(controller);
  var event=session.create("Club meeting","Social","Campus","2027-02-01T18:00","2027-02-01T21:00","80","1000");
  assertSame(controller,session.controller());assertSame(event,controller.viewEvent(session.activeId()));assertEquals(LocalDateTime.parse("2027-02-01T21:00"),event.getEndDateTime());
  controller.addIdea(event.getId(),"Alex","Meet new members");session.select(null);session.select(event.getId());assertEquals(1,session.active().getIdeas().size());
  session.deleteActive();assertTrue(controller.listEvents().isEmpty());assertThrows(IllegalArgumentException.class,session::activeId);
 }}
 @Test void invalidFormDoesNotCreatePartialEvent(){try(var facade=facade()){
  var session=new GuiSession(new EventPlanningController(facade));
  assertThrows(IllegalArgumentException.class,()->session.create("Club","Social","Campus","2027-02-01T18:00","2027-02-01T17:00","80","1000"));
  assertThrows(IllegalArgumentException.class,()->session.create("Club","Social","Campus","2027-02-01T18:00","2027-02-01T20:00","80","Infinity"));
  assertThrows(IllegalArgumentException.class,()->session.create("","Social","Campus","2027-02-01T18:00","2027-02-01T20:00","80","1000"));
  assertTrue(session.controller().listEvents().isEmpty());
 }}
 @Test void typedInputValidation(){
  assertEquals(80,GuiInputs.integer("80"));assertThrows(IllegalArgumentException.class,()->GuiInputs.integer("1.2"));
  assertThrows(IllegalArgumentException.class,()->GuiInputs.money("-1"));assertThrows(IllegalArgumentException.class,()->GuiInputs.date("tomorrow"));
  assertThrows(IllegalArgumentException.class,()->GuiInputs.number("1e9999"));assertThrows(IllegalArgumentException.class,()->GuiInputs.optionalBoolean("maybe"));
  assertEquals(java.util.List.of("a","b"),GuiInputs.csv("a, b"));assertNull(GuiInputs.optionalBoolean(""));
 }
 @Test void availabilitySupportsMultipleIndependentSlots(){
  var slots=GuiInputs.slots("2027-02-01T17:00/2027-02-01T18:00\n2027-02-02T17:00/2027-02-02T19:00");assertEquals(2,slots.size());
  assertEquals(slots,GuiInputs.slots(GuiInputs.slotsText(slots)));assertTrue(GuiInputs.slots("").isEmpty());
  assertThrows(IllegalArgumentException.class,()->GuiInputs.slots("2027-02-01T18:00"));
  assertThrows(IllegalArgumentException.class,()->GuiInputs.slots("2027-02-01T18:00/2027-02-01T17:00"));
 }
 @Test void invalidSelectionDoesNotLoseCurrentEvent(){try(var facade=facade()){
  var session=new GuiSession(new EventPlanningController(facade));var event=session.create("Club","Social","Campus","2027-02-01T18:00","2027-02-01T20:00","80","1000");
  assertThrows(IllegalArgumentException.class,()->session.select("missing"));assertEquals(event.getId(),session.activeId());
 }}
}
