package com.eventplanner.view.gui;

import com.eventplanner.model.Event;
import com.eventplanner.ai.PlanningException;
import javafx.concurrent.Task;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;
import java.util.concurrent.*;

/** Desktop view coordinator. Feature panels share this session and serialized request runner. */
public final class Dashboard implements AutoCloseable {
 final GuiSession session;
 final com.eventplanner.controller.EventPlanningController controller;
 private final BorderPane root=new BorderPane();
 private final VBox navigation=new VBox(8);
 private final ComboBox<Event> events=new ComboBox<>();
 private final java.util.Map<String,String> eventLabels=new java.util.HashMap<>();
 private final Label status=new Label("Ready. Successful changes are saved to the configured event store.");
 private final ProgressIndicator progress=new ProgressIndicator();
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"planner-requests"); t.setDaemon(false); return t;});
 private String section="Events";
 private String renderedKey;
 private final java.util.Map<String,Double> scrollPositions=new java.util.HashMap<>();
 private boolean refreshing,busy,closed;
 private final Runnable cleanup;
 private final java.util.Map<String,PartBState> partB=new java.util.HashMap<>();
 PartBState partB(String id){return partB.computeIfAbsent(id,key->new PartBState());}
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
  var brand=new Label("Planit");brand.getStyleClass().add("brand");
  var icon=new ImageView(new Image(java.util.Objects.requireNonNull(Dashboard.class.getResource(
   "/com/eventplanner/assets/planit/planit_icon_64.png"),"Missing Planit sidebar icon").toExternalForm()));
  icon.setFitWidth(40);icon.setFitHeight(40);icon.setPreserveRatio(true);icon.setSmooth(true);icon.setAccessibleText("Planit logo");
  var brandRow=new HBox(10,icon,brand);brandRow.getStyleClass().add("brand-row");
  var tagline=Ui.note("From ideas to execution.");tagline.getStyleClass().add("brand-tagline");
  var branding=new VBox(6,brandRow,tagline);branding.getStyleClass().add("brand-block");navigation.getChildren().add(branding);
  for(String name:new String[]{"Events","Ideas","Blueprints","Tasks & team","Schedule","Budget & catering","Venue research","Scenario Simulator","Plan Updates & Contingencies","Communications & FAQ","Post-Event Review"}) {
   var button=Ui.button(name,()->{ section=name; refresh(); }); button.setWrapText(true);button.setMaxWidth(Double.MAX_VALUE); navigation.getChildren().add(button);
  }
  navigation.getStyleClass().add("sidebar"); navigation.setPrefWidth(222);var sideScroll=new ScrollPane(navigation);sideScroll.setFitToWidth(true);sideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);sideScroll.setPrefWidth(238);sideScroll.getStyleClass().add("sidebar-shell");root.setLeft(sideScroll);
  events.setPromptText("Select an active event"); events.setPrefWidth(350);
  events.setConverter(new StringConverter<>() { public String toString(Event e) {return e==null?"":eventLabels.getOrDefault(e.getId(),"Unknown event");} public Event fromString(String s) {return null;} });
  events.valueProperty().addListener((o,a,b)->{if(!refreshing){session.select(b==null?null:b.getId());refresh();}});
  var heading=new Label("Event planning workspace"); heading.getStyleClass().add("header-title");
  var spacer=new Region(); HBox.setHgrow(spacer,Priority.ALWAYS);
  var header=new FlowPane(18,10,heading,Ui.field("Active event",events)); header.getStyleClass().add("header"); root.setTop(header);
  progress.setPrefSize(22,22); progress.setVisible(false); progress.setManaged(false);
  status.setWrapText(true);var footer=new HBox(12,progress,status); footer.getStyleClass().add("footer"); root.setBottom(footer); refresh();
 }
 public Parent root() {return root;}
 void refresh() {
  if(closed)return;
  if(renderedKey!=null&&root.getCenter() instanceof ScrollPane previous)scrollPositions.put(renderedKey,previous.getVvalue());renderedKey=null;
  for(Node node:navigation.getChildren())if(node instanceof Button button){button.getStyleClass().remove("active");if(button.getText().equals(section))button.getStyleClass().add("active");}
  // Never inspect mutable domain objects while the worker owns the controller.
  // Navigation and selection remain available; the chosen view loads on completion.
  if(busy){
   var waiting=Ui.page(section,"Your request is running in the background.",Ui.card("Working on your plan",Ui.badge("GENERATING"),Ui.note("You can move or resize the window, choose another event, or navigate to another page. Results will be available for the event that started this request.")));
   var scroll=new ScrollPane(waiting);scroll.setFitToWidth(true);root.setCenter(scroll);return;
  }
  refreshing=true;
  String active=null; try{active=session.activeId();}catch(IllegalArgumentException ignored){}
  var available=controller.listEvents();eventLabels.clear();
  var names=DisplayNames.labels(available,Event::getId,e->e.getName()+" | "+DisplayNames.date(e.getDateTime())+" | "+e.getLocation(),"Event");eventLabels.putAll(names);
  events.getItems().setAll(available);
  final String selected=active; events.setValue(events.getItems().stream().filter(e->e.getId().equals(selected)).findFirst().orElse(null)); refreshing=false;
  Node panel;
  if(!section.equals("Events") && events.getValue()==null)panel=Ui.page(section,"Create or select an active event to get started.");
  else panel=switch(section) {
   case "Ideas" -> new IdeasPanel(this).build(); case "Blueprints" -> new BlueprintsPanel(this).build();
   case "Tasks & team" -> new TasksPanel(this).build(); case "Schedule" -> new SchedulePanel(this).build();
   case "Budget & catering" -> new BudgetPanel(this).build(); case "Venue research" -> new VenuesPanel(this).build();
   case "Scenario Simulator" -> new ScenarioPanel(this).build();
   case "Plan Updates & Contingencies" -> new PlanUpdatesPanel(this).build();
   case "Communications & FAQ" -> new CommunicationsPanel(this).build();
   case "Post-Event Review" -> new PostEventPanel(this).build();
   default -> new EventsPanel(this).build();
  };
  var scroll=new ScrollPane(panel); scroll.setFitToWidth(true); root.setCenter(scroll); scroll.setDisable(busy);renderedKey=selected+":"+section;scroll.setVvalue(scrollPositions.getOrDefault(renderedKey,0.0));
 }
 <T> void remember(TableView<T> table,String key,java.util.function.Function<T,String> identity){
  String active=session.activeOrNullId();if(active==null)return;var state=partB(active);String selected=state.inputs.get("selection:"+key);
  table.getItems().stream().filter(value->java.util.Objects.equals(identity.apply(value),selected)).findFirst().ifPresent(value->table.getSelectionModel().select(value));
  table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null)state.inputs.put("selection:"+key,identity.apply(b));});
 }
 <T> void rememberChoice(ComboBox<T> box,String key,java.util.function.Function<T,String> identity){
  var state=partB(id());String selected=state.inputs.get("choice:"+key);
  box.getItems().stream().filter(value->java.util.Objects.equals(identity.apply(value),selected)).findFirst().ifPresent(box::setValue);
  box.valueProperty().addListener((o,a,b)->{if(b!=null)state.inputs.put("choice:"+key,identity.apply(b));});
 }
 void edited(){diagnostics.clear();refresh();}
 String id(){return session.activeId();}
 void action(Runnable work) { if(busy||closed)return;try { work.run(); if(!busy){diagnostics.clear();refresh();status.getStyleClass().remove("error"); status.setText("Changes saved. Review any draft warnings before approval.");} } catch(RuntimeException ex) {failure(ex);} }
 void background(String label,Callable<?> work) {
  background(label,work,result->{});
 }
 /** Publish results on FX, including results for an event the user has navigated away from. */
 <T> void background(String label,Callable<T> work,java.util.function.Consumer<T> success) {
  String requestEvent=session.activeOrNullId();
  if(busy||closed)return; busy=true;status.getStyleClass().remove("error"); lock(true); status.setText(label+" - please wait..."); refresh();
  Task<Completion> task=new Task<>() { @Override protected Completion call() throws Exception{
   var checks=new java.util.HashMap<String,String>();
   Object result=work.call();
   if(requestEvent!=null&&controller.listEvents().stream().anyMatch(event->event.getId().equals(requestEvent))){
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
   @SuppressWarnings("unchecked") T result=(T)task.getValue().result();
   success.accept(result);
   busy=false;lock(false);refresh();status.setText(label+" completed"+(java.util.Objects.equals(requestEvent,session.activeOrNullId())?". Review results and warnings.":" for the previously selected event. Select it to review results."));});
  task.setOnFailed(e->{if(closed)return;diagnostics.clear();busy=false;lock(false);refresh();failure(task.getException());});
  executor.execute(task);
 }
 private void lock(boolean value){if(root.getCenter()!=null)root.getCenter().setDisable(value);progress.setVisible(value);progress.setManaged(value);}
 private void failure(Throwable ex){if(!status.getStyleClass().contains("error"))status.getStyleClass().add("error");status.setText(ex instanceof IllegalArgumentException || ex instanceof PlanningException || ex instanceof com.eventplanner.storage.StorageException ? ex.getMessage():"Operation failed. Check your configuration and try again.");}
 public boolean busy(){return busy;}
 @Override public void close(){if(closed)return;closed=true;executor.submit(()->{try{cleanup.run();}catch(RuntimeException ignored){/* Never leak transport diagnostics or credentials. */}});executor.shutdown();}
}
