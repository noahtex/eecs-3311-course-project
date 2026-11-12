package com.eventplanner.view.gui;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import java.util.*;
import java.util.function.*;

final class Ui {
 private Ui() { }
 static Label title(String value) { var label=new Label(value); label.getStyleClass().add("section-title"); return label; }
 static Label note(String value) { var label=new Label(value); label.setWrapText(true); label.getStyleClass().add("muted"); return label; }
 static VBox page(String title,String subtitle,Node... nodes) {
  var page=new VBox(16); page.getStyleClass().add("page"); page.getChildren().addAll(title(title),note(subtitle)); page.getChildren().addAll(nodes); return page;
 }
 static VBox card(String title,Node... nodes) { var card=new VBox(12); card.getStyleClass().add("card"); card.getChildren().add(title(title)); card.getChildren().addAll(nodes); return card; }
 static FlowPane row(Node... nodes) { return new FlowPane(10,10,nodes); }
 static Button button(String text,Runnable action) { var button=new Button(text); button.setOnAction(e->action.run()); return button; }
 static TextArea details(String text) { var area=new TextArea(text); area.setEditable(false); area.setWrapText(true); area.setPrefRowCount(8); return area; }
 static <T> ComboBox<T> choice(List<T> items) { var box=new ComboBox<T>(FXCollections.observableArrayList(items)); if(!items.isEmpty())box.getSelectionModel().selectFirst(); return box; }
 static boolean confirm(String title,String message) {
  var alert=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.CANCEL,ButtonType.OK); alert.setTitle(title); alert.setHeaderText(title);
  return alert.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
 }
 static <T> T selected(TableView<T> table) { var item=table.getSelectionModel().getSelectedItem(); if(item==null)throw new IllegalArgumentException("Select a row first."); return item; }
 static <T> TableView<T> table(List<T> items) {
  var table=new TableView<T>(FXCollections.observableArrayList(items)); table.setPlaceholder(new Label("No records yet.")); table.setPrefHeight(240); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN); return table;
 }
 static <T> void column(TableView<T> table,String label,Function<T,?> value) {
  var column=new TableColumn<T,String>(label); column.setCellValueFactory(cell->new ReadOnlyStringWrapper(Objects.toString(value.apply(cell.getValue()),"UNKNOWN"))); table.getColumns().add(column);
 }
 static <T> VBox inspect(TableView<T> table,Function<T,String> render) {
  var detail=details("Select a row to inspect details."); table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->detail.setText(b==null?"":render.apply(b)));
  return new VBox(10,table,detail);
 }
 static String lines(String label,List<?> values) { return "\n"+label+"\n"+(values.isEmpty()?"None recorded":String.join("\n",values.stream().map(v->"- "+v).toList()))+"\n"; }
 /** Keeps invalid input in the modal form; submit callbacks delegate to application operations. */
 static final class Form {
  private final LinkedHashMap<String,TextInputControl> fields=new LinkedHashMap<>();
  Form field(String name,Object value) {
   TextInputControl input;
   if(name.contains("Availability")||name.equals("Idea")||name.equals("Description")||name.equals("Goal or constraint")) {
    var area=new TextArea(Objects.toString(value,""));area.setPrefRowCount(3);area.setWrapText(true);input=area;
   }else input=new TextField(Objects.toString(value,""));
   if(name.equals("Attendance"))input.setPromptText("e.g., 80");
   else if(name.contains("Availability"))input.setPromptText("2026-12-01T18:00/2026-12-01T21:00 (one pair per line)");
   else if(name.toLowerCase(Locale.ROOT).matches(".*(start|end|deadline).*"))input.setPromptText("2026-12-01T18:30");
   fields.put(name,input);return this;
  }
  String get(String key) { return fields.get(key).getText().strip(); }
  void show(String title,Consumer<Form> submit) {
   var dialog=new Dialog<Void>(); dialog.setTitle(title); dialog.setHeaderText(title); dialog.setResizable(true);
   dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
   var grid=new GridPane(); grid.setHgap(16); grid.setVgap(10); int row=0;
   for(var entry:fields.entrySet()) { var label=new Label(entry.getKey()); label.setWrapText(true); label.setMaxWidth(200); grid.add(label,0,row); grid.add(entry.getValue(),1,row++); GridPane.setHgrow(entry.getValue(),Priority.ALWAYS); }
   var error=new Label(); error.setWrapText(true); error.getStyleClass().add("error");
   var content=new VBox(12,grid,error); var scroll=new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPrefViewportWidth(650); scroll.setPrefViewportHeight(Math.min(560,90+fields.size()*65));
   dialog.getDialogPane().setContent(scroll);
   dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,event->{
    try {
     if(fields.containsKey("Attendance")) {
      String attendance=get("Attendance");
      if(attendance.isEmpty())throw new IllegalArgumentException("Attendance is required. Enter a positive whole number, e.g., 80.");
      try {
       if(!attendance.matches("[0-9]+")||GuiInputs.integer(attendance)<=0)throw new IllegalArgumentException();
      } catch(IllegalArgumentException ex) {
       throw new IllegalArgumentException("Attendance must be a positive whole number between 1 and 2147483647, e.g., 80.");
      }
     }
     submit.accept(this);
    }
    catch(IllegalArgumentException ex) { error.setText(ex.getMessage()); event.consume(); }
    catch(RuntimeException ex) { error.setText("Unable to save. Check the values and try again."); event.consume(); }
   });
   dialog.showAndWait();
  }
 }
}
