package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.scene.control.*;
final class EventsPanel {
 private final Dashboard d; EventsPanel(Dashboard d){this.d=d;}
 Node build(){
  var table=Ui.table(d.controller.listEvents()); Ui.column(table,"Event",Event::getName);Ui.column(table,"Location",Event::getLocation);Ui.column(table,"Starts",Event::getDateTime);Ui.column(table,"Attendance",Event::getExpectedAttendance);
  var create=Ui.button("Create event",()->new Ui.Form().field("Name","").field("Type","").field("Location","").field("Start (ISO local datetime)","").field("End (ISO local datetime)","").field("Attendance","").field("Budget","")
   .show("Create event",f->{d.session.create(f.get("Name"),f.get("Type"),f.get("Location"),f.get("Start (ISO local datetime)"),f.get("End (ISO local datetime)"),f.get("Attendance"),f.get("Budget"));d.edited();}));
  var select=Ui.button("Set active event",()->d.action(()->d.session.select(Ui.selected(table).getId())));
  var delete=Ui.button("Delete selected",()->d.action(()->{Event e=Ui.selected(table);if(Ui.confirm("Delete event?","Delete "+e.getName()+" and its session data?")){d.controller.deleteEvent(e.getId());if(d.session.activeOrNullId()!=null && d.session.activeOrNullId().equals(e.getId()))d.session.select(null);}}));
  delete.getStyleClass().add("danger");
  var constraints=Ui.button("Add goal / constraint",()->d.action(()->{String id=d.id();new Ui.Form().field("Goal or constraint","").show("Event constraint",f->d.controller.addConstraint(id,f.get("Goal or constraint")));}));
  return Ui.page("Your events","Plan your next club gathering. All dates and times are event-local; data is kept until you close the app.",Ui.row(create,select,delete,constraints),Ui.inspect(table,e->"ID: "+e.getId()+"\n"+e.getName()+" | "+e.getType()+"\nLocation: "+e.getLocation()+"\nStart: "+e.getDateTime()+"\nEnd: "+e.getEndDateTime()+"\nAttendance: "+e.getExpectedAttendance()+"\nSpending limit: "+e.getTotalBudget()+" "+e.getPlanningCurrency()+Ui.lines("Goals / constraints",e.getConstraints())));
 }
}
