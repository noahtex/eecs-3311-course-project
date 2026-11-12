package com.eventplanner.view.gui;

import com.eventplanner.model.TimeSlot;
import javafx.scene.layout.VBox;
import java.util.*;

/** An empty collection explicitly represents unknown availability, as in the backend. */
final class AvailabilityInput extends VBox {
 private record Entry(DateTimeInput start,DateTimeInput end,VBox view) { }
 private final List<Entry> entries=new ArrayList<>();
 private final VBox rows=new VBox(12);
 AvailabilityInput(List<TimeSlot> initial){
  super(10);getChildren().addAll(Ui.note("Event local time. No intervals means availability is unknown."),rows,Ui.secondary("Add availability interval",()->add(null)));
  initial.forEach(this::add);
 }
 private void add(TimeSlot slot){
  var start=new DateTimeInput(slot==null?null:slot.start());var end=new DateTimeInput(slot==null?null:slot.end());
  var view=Ui.card("Availability interval",Ui.field("From",start),Ui.field("Until",end));
  var entry=new Entry(start,end,view);entries.add(entry);
  view.getChildren().add(Ui.secondary("Remove interval",()->{entries.remove(entry);rows.getChildren().remove(view);}));rows.getChildren().add(view);
 }
 List<TimeSlot> value(){return entries.stream().map(row->new TimeSlot(row.start.value(),row.end.value())).toList();}
}
