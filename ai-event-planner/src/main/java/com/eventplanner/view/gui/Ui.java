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
 static Label note(String value) { var label=new Label(value); label.setWrapText(true);label.setMaxWidth(Double.MAX_VALUE); label.getStyleClass().add("muted"); return label; }
 static VBox page(String title,String subtitle,Node... nodes) {
  var page=new VBox(16); page.getStyleClass().add("page");page.setMinWidth(0); page.getChildren().addAll(title(title),note(subtitle)); page.getChildren().addAll(nodes); return page;
 }
 static VBox card(String title,Node... nodes) { var card=new VBox(12); card.getStyleClass().add("card"); card.getChildren().add(title(title)); card.getChildren().addAll(nodes); return card; }
 static FlowPane row(Node... nodes) { return new FlowPane(10,10,nodes); }
 static Button button(String text,Runnable action) { var button=new Button(text);button.setWrapText(true); button.setOnAction(e->action.run());
  if(!text.matches("^(Create|Generate|Run |Fuse |Research |Confirm |Save |Approve ).*"))button.getStyleClass().add("secondary");return button; }
 static Button secondary(String text,Runnable action){var button=button(text,action);if(!button.getStyleClass().contains("secondary"))button.getStyleClass().add("secondary");return button;}
 static VBox field(String label,Node input){var title=new Label(label);title.getStyleClass().add("field-label");title.setLabelFor(input);return new VBox(7,title,input);}
 static Label badge(String text){var label=new Label(text);label.getStyleClass().addAll("badge",text.contains("APPROVED")||text.equals("CONFIRMED")?"success":text.contains("UNKNOWN")||text.contains("WARNING")||text.contains("ESTIMATED")?"warning":"accent");return label;}
 static VBox metric(String label,Object value){var result=new Label(Objects.toString(value,"UNKNOWN"));result.setWrapText(true);result.getStyleClass().add("metric-value");var box=new VBox(6,note(label),result);box.getStyleClass().add("metric");return box;}
 static Node metrics(Node... nodes){var pane=row(nodes);pane.getStyleClass().add("metrics");return pane;}
 static VBox prose(String value){var box=new VBox(10);box.getStyleClass().add("prose");for(String paragraph:Objects.toString(value,"UNKNOWN").split("\\R\\s*\\R")){var label=new Label(paragraph);label.setWrapText(true);label.setMaxWidth(Double.MAX_VALUE);label.getStyleClass().add("body-text");box.getChildren().add(label);}return box;}
 static VBox section(String heading,String text){return card(heading,prose(text));}
 static VBox bullets(String heading,List<?> values){var box=card(heading);if(values.isEmpty())box.getChildren().add(note("None recorded"));else for(Object value:values){var label=new Label("\u2022 "+value);label.setWrapText(true);label.getStyleClass().add("body-text");box.getChildren().add(label);}return box;}
 static TitledPane technical(String text){var pane=new TitledPane("Technical details / original content",details(text));pane.setExpanded(false);return pane;}
 static void style(DialogPane pane){pane.getStylesheets().add(Ui.class.getResource("/com/eventplanner/dashboard.css").toExternalForm());}
 static TextArea details(String text) { var area=new TextArea(text); area.setEditable(false); area.setWrapText(true); area.setPrefRowCount(8); return area; }
 static <T> ComboBox<T> choice(List<T> items) { var box=new ComboBox<T>(FXCollections.observableArrayList(items)); if(!items.isEmpty())box.getSelectionModel().selectFirst(); return box; }
 static boolean confirm(String title,String message) {
  var alert=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.CANCEL,ButtonType.OK); alert.setTitle(title); alert.setHeaderText(title);style(alert.getDialogPane());
  return alert.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
 }
 static <T> T selected(TableView<T> table) { var item=table.getSelectionModel().getSelectedItem(); if(item==null)throw new IllegalArgumentException("Select a row first."); return item; }
 static <T> TableView<T> table(List<T> items) {
  var table=new TableView<T>(FXCollections.observableArrayList(items)); var empty=note("Nothing here yet. Add details or generate a proposal to get started.");empty.setAlignment(javafx.geometry.Pos.CENTER);empty.setPadding(new javafx.geometry.Insets(24));table.setPlaceholder(empty); table.setMinWidth(0);table.setPrefHeight(280); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN); return table;
 }
 static <T> VBox inspectView(TableView<T> table,Function<T,Node> render){
  var content=new VBox(14,note("Select a row to inspect details."));
  table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->content.getChildren().setAll(b==null?note("Select a row to inspect details."):render.apply(b)));
  if(table.getSelectionModel().getSelectedItem()!=null)content.getChildren().setAll(render.apply(table.getSelectionModel().getSelectedItem()));
  else if(!table.getItems().isEmpty())table.getSelectionModel().selectFirst();return new VBox(16,table,content);
 }
 static <T> void column(TableView<T> table,String label,Function<T,?> value) {
  var column=new TableColumn<T,String>(label); column.setCellValueFactory(cell->{Object result=value.apply(cell.getValue());return new ReadOnlyStringWrapper(result instanceof java.time.LocalDateTime time?DisplayNames.date(time):Objects.toString(result,"UNKNOWN"));});
  column.setMinWidth(100);column.setPrefWidth(170);
  column.setCellFactory(c->new TableCell<>(){private final Label text=new Label();{text.setWrapText(true);text.maxWidthProperty().bind(widthProperty().subtract(18));}
   @Override protected void updateItem(String item,boolean empty){super.updateItem(item,empty);setText(null);if(empty||item==null){setGraphic(null);return;}text.setText(item);text.getStyleClass().removeAll("status-text","unknown-text");if(item.matches("DRAFT|APPROVED|ESTIMATED|CONFIRMED|UNKNOWN|WARNING|TODO|DONE|IN_PROGRESS|HIGH|MEDIUM|LOW"))text.getStyleClass().add(item.contains("UNKNOWN")?"unknown-text":"status-text");setGraphic(text);}});
  table.getColumns().add(column);
 }
 static <T> VBox inspect(TableView<T> table,Function<T,String> render) {
  var detail=details("Select a row to inspect details."); table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->detail.setText(b==null?"":render.apply(b)));
  return new VBox(10,table,detail);
 }
 static String lines(String label,List<?> values) { return "\n"+label+"\n"+(values.isEmpty()?"None recorded":String.join("\n",values.stream().map(v->"- "+v).toList()))+"\n"; }
 /** Keeps invalid input in the modal form; submit callbacks delegate to application operations. */
 static final class Form {
  private final LinkedHashMap<String,Supplier<String>> fields=new LinkedHashMap<>();
  private final LinkedHashMap<String,Node> controls=new LinkedHashMap<>();
  Form dateTime(String name,java.time.LocalDateTime value){var input=new DateTimeInput(value);input.setAccessibleText(name);controls.put(name,input);fields.put(name,()->input.value().toString());return this;}
  Form availability(String name,List<com.eventplanner.model.TimeSlot> values){var input=new AvailabilityInput(values);controls.put(name,input);fields.put(name,()->GuiInputs.slotsText(input.value()));return this;}
  Form hidden(String name,String value){fields.put(name,()->value);return this;}
  Form technical(String value){controls.put("Reference",Ui.technical(value));return this;}
  Form option(String name,List<DisplayNames.Option> options,String selected){
   var input=choice(options);input.setMaxWidth(Double.MAX_VALUE);input.setValue(options.stream().filter(o->Objects.equals(o.id(),selected==null?"":selected)).findFirst().orElse(options.isEmpty()?null:options.getFirst()));
   controls.put(name,input);fields.put(name,()->input.getValue()==null?"":input.getValue().id());return this;
  }
  Form multiOption(String name,List<DisplayNames.Option> options,List<String> selected){
   var input=new ListView<DisplayNames.Option>(FXCollections.observableArrayList(options));input.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);input.setPrefHeight(150);
   for(int i=0;i<options.size();i++)if(selected.contains(options.get(i).id()))input.getSelectionModel().select(i);
   controls.put(name,input);fields.put(name,()->String.join(", ",input.getSelectionModel().getSelectedItems().stream().map(DisplayNames.Option::id).toList()));return this;
  }
  Form field(String name,Object value) {
   if(value instanceof Enum<?> enumeration){
    var input=new ComboBox<Enum<?>>(FXCollections.observableArrayList(enumeration.getDeclaringClass().getEnumConstants()));input.setValue(enumeration);controls.put(name,input);fields.put(name,()->input.getValue().name());return this;
   }
   TextInputControl input;
   if(name.contains("Availability")||name.equals("Idea")||name.equals("Description")||name.equals("Goal or constraint")) {
    var area=new TextArea(Objects.toString(value,""));area.setPrefRowCount(3);area.setWrapText(true);input=area;
   }else input=new TextField(Objects.toString(value,""));
   if(name.equals("Attendance"))input.setPromptText("e.g., 80");

   input.setAccessibleText(name);controls.put(name,input);fields.put(name,()->input.getText().strip());return this;
  }
  String get(String key) { try{return fields.get(key).get();}catch(IllegalArgumentException ex){throw new IllegalArgumentException(key+": "+ex.getMessage());} }
  void show(String title,Consumer<Form> submit) {
   var dialog=new Dialog<Void>(); dialog.setTitle(title); dialog.setHeaderText(title); dialog.setResizable(true);style(dialog.getDialogPane());
   dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
   var grid=new GridPane(); grid.setHgap(16); grid.setVgap(10); int row=0;
   for(var entry:controls.entrySet()) { var label=new Label(entry.getKey()); label.setWrapText(true); label.setMaxWidth(170);label.setLabelFor(entry.getValue()); grid.add(label,0,row); grid.add(entry.getValue(),1,row++); GridPane.setHgrow(entry.getValue(),Priority.ALWAYS); }
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
     if(fields.containsKey("Budget")){
      if(get("Budget").isBlank())throw new IllegalArgumentException("Budget is required. Enter a non-negative amount.");
      try{GuiInputs.number(get("Budget"));}catch(IllegalArgumentException ex){throw new IllegalArgumentException("Budget must be a finite, non-negative amount.");}
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
