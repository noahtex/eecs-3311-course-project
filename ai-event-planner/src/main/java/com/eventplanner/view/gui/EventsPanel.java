package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.scene.control.*;
final class EventsPanel {
 private final Dashboard d; EventsPanel(Dashboard d){this.d=d;}
 Node build(){
  var table=Ui.table(d.controller.listEvents()); Ui.column(table,"Event",Event::getName);Ui.column(table,"Location",Event::getLocation);Ui.column(table,"Starts",Event::getDateTime);Ui.column(table,"Attendance",Event::getExpectedAttendance);
  d.remember(table,"events",Event::getId);
  var create=Ui.button("Create event",()->new Ui.Form().field("Name","").field("Type","").field("Location","").dateTime("Start date & time",null).dateTime("End date & time",null).field("Attendance","").field("Budget","")
   .show("Create event",f->{d.session.create(f.get("Name"),f.get("Type"),f.get("Location"),f.get("Start date & time"),f.get("End date & time"),f.get("Attendance"),f.get("Budget"));d.edited();}));
  var select=Ui.button("Set active event",()->d.action(()->d.session.select(Ui.selected(table).getId())));
  var delete=Ui.button("Delete selected",()->d.action(()->{Event e=Ui.selected(table);if(Ui.confirm("Delete event?","Delete "+e.getName()+" and its session data?")){d.controller.deleteEvent(e.getId());if(d.session.activeOrNullId()!=null && d.session.activeOrNullId().equals(e.getId()))d.session.select(null);}}));
  delete.getStyleClass().add("danger");
  var constraints=Ui.button("Add goal / constraint",()->d.action(()->{String id=d.id();new Ui.Form().field("Goal or constraint","").show("Event constraint",f->d.controller.addConstraint(id,f.get("Goal or constraint")));}));
  select.setDisable(table.getItems().isEmpty());delete.setDisable(table.getItems().isEmpty());constraints.setDisable(d.session.activeOrNullId()==null);
  return Ui.page("Your events","Plan your next club gathering. All dates and times are event-local; data is kept until you close the app.",Ui.metrics(Ui.metric("Events in this session",table.getItems().size()),Ui.metric("Storage","In memory")),Ui.row(create,select,delete,constraints),Ui.inspectView(table,ResultViews::event));
 }
}
