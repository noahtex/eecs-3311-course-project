package com.eventplanner.view.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.util.stream.IntStream;

/** Local event time only: never converts through the host timezone or an Instant. */
final class DateTimeInput extends VBox {
 final DatePicker date=new DatePicker();
 final ComboBox<Integer> hour=Ui.choice(IntStream.range(0,24).boxed().toList());
 final ComboBox<Integer> minute=Ui.choice(IntStream.range(0,60).boxed().toList());
 private final Label preview=Ui.note("Choose a date and time · event local time");
 private final int second,nano;
 DateTimeInput(LocalDateTime initial){
  super(8);getStyleClass().add("date-time-input");
  second=initial==null?0:initial.getSecond();nano=initial==null?0:initial.getNano();
  date.setPromptText("Choose date");date.setAccessibleText("Event local date");
  hour.setAccessibleText("Hour, 24-hour clock");minute.setAccessibleText("Minute");
  for(var box:java.util.List.of(hour,minute)){
   box.setConverter(new javafx.util.StringConverter<>(){public String toString(Integer v){return v==null?"":String.format("%02d",v);}public Integer fromString(String s){return Integer.valueOf(s);}});
   box.setPrefWidth(76);
  }
  hour.setValue(initial==null?null:initial.getHour());minute.setValue(initial==null?null:initial.getMinute());
  date.setValue(initial==null?null:initial.toLocalDate());
  // Keep the keyboard editor in sync even before a skin/window has been attached.
  date.getEditor().setText(date.getConverter().toString(date.getValue()));
  getChildren().addAll(Ui.row(date,hour,new Label(":"),minute,Ui.note("24-hour")),preview);
  date.valueProperty().addListener((o,a,b)->{date.getEditor().setText(date.getConverter().toString(b));preview();});hour.valueProperty().addListener((o,a,b)->preview());minute.valueProperty().addListener((o,a,b)->preview());preview();
 }
 LocalDateTime value(){
  try{date.commitValue();}catch(RuntimeException ex){throw new IllegalArgumentException("Choose a valid date using the calendar or the displayed local date format.");}
  if(date.getValue()==null||hour.getValue()==null||minute.getValue()==null)throw new IllegalArgumentException("Choose a date, hour and minute for each required date/time field.");
  return LocalDateTime.of(date.getValue(),LocalTime.of(hour.getValue(),minute.getValue(),second,nano));
 }
 private void preview(){
  preview.setText(date.getValue()==null||hour.getValue()==null||minute.getValue()==null?"Choose a date and time · event local time":DisplayNames.date(LocalDateTime.of(date.getValue(),LocalTime.of(hour.getValue(),minute.getValue(),second,nano)))+" · event local time");
 }
}
