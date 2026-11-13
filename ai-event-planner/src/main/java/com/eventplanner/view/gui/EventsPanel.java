package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
final class EventsPanel {
 private final Dashboard d; EventsPanel(Dashboard d){this.d=d;}
 Node build(){
  var table=Ui.table(d.controller.listEvents()); Ui.column(table,"Event",Event::getName);Ui.column(table,"Location",Event::getLocation);Ui.column(table,"Starts",Event::getDateTime);Ui.column(table,"Attendance",Event::getExpectedAttendance);
  d.remember(table,"events",Event::getId);
  var create=Ui.button("Create event",()->new Ui.Form().field("Name","").field("Type","").field("Location","").dateTime("Start date & time",null).dateTime("End date & time",null).field("Attendance","").field("Budget","")
   .show("Create event",f->{
    String name=GuiInputs.required(f.get("Name")),type=GuiInputs.required(f.get("Type")),location=GuiInputs.required(f.get("Location"));
    var start=GuiInputs.date(f.get("Start date & time"));var end=GuiInputs.date(f.get("End date & time"));new TimeSlot(start,end);
    int count=GuiInputs.integer(f.get("Attendance"));double amount=GuiInputs.number(f.get("Budget"));
    d.background("Saving new event",()->d.controller.createEventWithEnd(name,type,location,start,end,count,amount),event->d.session.select(event.getId()));
   }));
  var select=Ui.button("Set active event",()->d.action(()->d.session.select(Ui.selected(table).getId())));
  var delete=Ui.button("Delete selected",()->d.action(()->{Event e=Ui.selected(table);String id=e.getId();if(Ui.confirm("Delete event?","Delete "+e.getName()+" and its saved data?"))d.background("Deleting event",()->d.controller.deleteEvent(id),deleted->{if(id.equals(d.session.activeOrNullId()))d.session.select(null);});}));
  delete.getStyleClass().add("danger");
  var constraints=Ui.button("Add goal / constraint",()->d.action(()->{String id=d.id();new Ui.Form().field("Goal or constraint","").show("Event constraint",f->{String text=GuiInputs.required(f.get("Goal or constraint"));d.background("Saving event constraint",()->{d.controller.addConstraint(id,text);return null;});});}));
  select.setDisable(table.getItems().isEmpty());delete.setDisable(table.getItems().isEmpty());constraints.setDisable(d.session.activeOrNullId()==null);
  return Ui.page("Your events","Plan your next club gathering. All dates and times are event-local. Successful changes are saved automatically.",Ui.metrics(Ui.metric("Your events",table.getItems().size()),Ui.metric("Storage",d.controller.storageDescription().startsWith("Local JSON:")?"Saved locally":"In memory")),Ui.row(create,select,delete,constraints),Ui.inspectView(table,ResultViews::event));
 }
}
