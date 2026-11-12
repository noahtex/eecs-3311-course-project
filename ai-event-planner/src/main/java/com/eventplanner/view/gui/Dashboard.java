package com.eventplanner.view.gui;

import com.eventplanner.model.Event;
import com.eventplanner.ai.PlanningException;
import javafx.concurrent.Task;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.util.concurrent.*;

/** Desktop view coordinator. Feature panels share this session and serialized request runner. */
public final class Dashboard implements AutoCloseable {
 final GuiSession session;
 final com.eventplanner.controller.EventPlanningController controller;
 private final BorderPane root=new BorderPane();
 private final VBox navigation=new VBox(8);
 private final ComboBox<Event> events=new ComboBox<>();
 private final Label status=new Label("Ready. Events are stored for this session only.");
 private final ProgressIndicator progress=new ProgressIndicator();
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"planner-requests"); t.setDaemon(false); return t;});
 private String section="Events";
 private boolean refreshing,busy,closed;
 private final Runnable cleanup;
 private final java.util.Map<String,String> diagnostics=new java.util.HashMap<>();
 String diagnostics(String id,String kind){return diagnostics.getOrDefault(id+":"+kind,"Use Validate to check the current plan before approval.");}
 void validate(String id,String kind){background("Validating "+kind,()->null);}
 private String validateNow(String id,String kind){
  var warnings=switch(kind){case "tasks"->controller.taskWarnings(id);case "schedule"->controller.scheduleConflicts(id);default->controller.validateBudget(id);};
  return warnings.isEmpty()?"No validation warnings.":String.join("\n",warnings);
 }
 private record Completion(Object result,java.util.Map<String,String> diagnostics) { }
 public Dashboard(GuiSession session) { this(session,()->{}); }
 public Dashboard(GuiSession session,Runnable cleanup) {
  this.cleanup=cleanup;
  this.session=session; controller=session.controller();
  var brand=new Label("CLUB / PLAN"); brand.getStyleClass().add("brand"); navigation.getChildren().add(brand);
  for(String name:new String[]{"Events","Ideas","Blueprints","Tasks & team","Schedule","Budget & catering","Venue research"}) {
   var button=Ui.button(name,()->{ section=name; refresh(); }); button.setMaxWidth(Double.MAX_VALUE); navigation.getChildren().add(button);
  }
  navigation.getStyleClass().add("sidebar"); navigation.setPrefWidth(215); root.setLeft(navigation);
  events.setPromptText("Select an active event"); events.setPrefWidth(350);
  events.setConverter(new StringConverter<>() { public String toString(Event e) {return e==null?"":e.getName()+" | "+e.getId();} public Event fromString(String s) {return null;} });
  events.valueProperty().addListener((o,a,b)->{if(!refreshing){session.select(b==null?null:b.getId());refresh();}});
  var heading=new Label("Event planning workspace"); heading.getStyleClass().add("header-title");
  var spacer=new Region(); HBox.setHgrow(spacer,Priority.ALWAYS);
  var header=new HBox(18,heading,spacer,new Label("ACTIVE EVENT"),events); header.getStyleClass().add("header"); root.setTop(header);
  progress.setPrefSize(22,22); progress.setVisible(false); progress.setManaged(false);
  var footer=new HBox(12,progress,status); footer.getStyleClass().add("footer"); root.setBottom(footer); refresh();
 }
 public Parent root() {return root;}
 void refresh() {
  if(closed)return;
  for(Node node:navigation.getChildren())if(node instanceof Button button){button.getStyleClass().remove("active");if(button.getText().equals(section))button.getStyleClass().add("active");}
  // Never inspect mutable domain objects while the worker owns the controller.
  // Navigation and selection remain available; the chosen view loads on completion.
  if(busy){
   var waiting=Ui.page(section,"Planning is running. You can choose another event or section; its results will load when the operation finishes.");
   var scroll=new ScrollPane(waiting);scroll.setFitToWidth(true);root.setCenter(scroll);return;
  }
  refreshing=true;
  String active=null; try{active=session.activeId();}catch(IllegalArgumentException ignored){}
  events.getItems().setAll(controller.listEvents());
  final String selected=active; events.setValue(events.getItems().stream().filter(e->e.getId().equals(selected)).findFirst().orElse(null)); refreshing=false;
  Node panel;
  if(!section.equals("Events") && events.getValue()==null)panel=Ui.page(section,"Create or select an active event to get started.");
  else panel=switch(section) {
   case "Ideas" -> new IdeasPanel(this).build(); case "Blueprints" -> new BlueprintsPanel(this).build();
   case "Tasks & team" -> new TasksPanel(this).build(); case "Schedule" -> new SchedulePanel(this).build();
   case "Budget & catering" -> new BudgetPanel(this).build(); case "Venue research" -> new VenuesPanel(this).build();
   default -> new EventsPanel(this).build();
  };
  var scroll=new ScrollPane(panel); scroll.setFitToWidth(true); root.setCenter(scroll); scroll.setDisable(busy);
 }
 void edited(){diagnostics.clear();refresh();}
 String id(){return session.activeId();}
 void action(Runnable work) { if(busy||closed)return;try { work.run(); if(!busy){diagnostics.clear();refresh(); status.setText("Changes saved. Review any draft warnings before approval.");} } catch(RuntimeException ex) {failure(ex);} }
 void background(String label,Callable<?> work) {
  String requestEvent=session.activeOrNullId();
  if(busy||closed)return; busy=true; lock(true); status.setText(label+" - please wait..."); refresh();
  Task<Completion> task=new Task<>() { @Override protected Completion call() throws Exception{
   var checks=new java.util.HashMap<String,String>();
   Object result=work.call();
   if(requestEvent!=null){
    checks.put(requestEvent+":tasks",validateNow(requestEvent,"tasks"));
    var event=controller.viewEvent(requestEvent);
    if(controller.hasScheduleDraft(requestEvent)||event.getSchedule()!=null)checks.put(requestEvent+":schedule",validateNow(requestEvent,"schedule"));
    if(controller.hasBudgetDraft(requestEvent)||event.getBudget()!=null)checks.put(requestEvent+":budget",validateNow(requestEvent,"budget"));
   }
   return new Completion(result,java.util.Map.copyOf(checks));
  } };
  task.setOnSucceeded(e->{if(closed)return;
   diagnostics.clear();diagnostics.putAll(task.getValue().diagnostics());
   if(task.getValue().result() instanceof com.eventplanner.model.ToolResult result)session.venueWarnings(requestEvent,result.warnings());
   busy=false;lock(false);refresh();status.setText(label+" completed"+(java.util.Objects.equals(requestEvent,session.activeOrNullId())?". Review results and warnings.":" for the previously selected event. Select it to review results."));});
  task.setOnFailed(e->{if(closed)return;diagnostics.clear();busy=false;lock(false);refresh();failure(task.getException());});
  executor.execute(task);
 }
 private void lock(boolean value){if(root.getCenter()!=null)root.getCenter().setDisable(value);progress.setVisible(value);progress.setManaged(value);}
 private void failure(Throwable ex){status.setText(ex instanceof IllegalArgumentException || ex instanceof PlanningException ? ex.getMessage():"Operation failed. Check your configuration and try again.");}
 public boolean busy(){return busy;}
 @Override public void close(){if(closed)return;closed=true;executor.submit(()->{try{cleanup.run();}catch(RuntimeException ignored){/* Never leak transport diagnostics or credentials. */}});executor.shutdown();}
}
