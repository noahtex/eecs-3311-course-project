package com.eventplanner.view.gui;

import com.eventplanner.App;
import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Requires a desktop display. No AI/Places requests are made. */
@EnabledIfSystemProperty(named="gui.smoke",matches="true")
class JavaFxSmokeTest {
 private static java.util.stream.Stream<Node> nodes(Node root){
  var children=root instanceof ScrollPane scroll?java.util.stream.Stream.of(scroll.getContent()):root instanceof TitledPane pane?java.util.stream.Stream.of(pane.getContent()):root instanceof Parent parent?parent.getChildrenUnmodifiable().stream():java.util.stream.Stream.<Node>empty();
  return java.util.stream.Stream.concat(java.util.stream.Stream.of(root),children.flatMap(JavaFxSmokeTest::nodes));
 }
 private static Button button(Dashboard d,String text){return nodes(d.root()).filter(n->n instanceof Button b&&b.getText().equals(text)).map(n->(Button)n).findFirst().orElseThrow();}
 private static Label status(Dashboard d){return (Label)((HBox)((BorderPane)d.root()).getBottom()).getChildren().getLast();}
 private static void operation(Dashboard d,Runnable start) throws Exception {
  var done=new CountDownLatch(1);
  javafx.beans.value.ChangeListener<String> listener=(o,a,b)->{if(!d.busy())done.countDown();};
  fx(()->{status(d).textProperty().addListener(listener);start.run();assertTrue(d.busy());return null;});
  try{assertTrue(done.await(10,TimeUnit.SECONDS));fx(()->{assertTrue(status(d).getText().contains("completed"),status(d).getText());return null;});}
  finally{fx(()->{status(d).textProperty().removeListener(listener);return null;});}
 }
 private static void acceptNextDialog(){Platform.runLater(()->{
  for(var window:java.util.List.copyOf(javafx.stage.Window.getWindows()))if(window.getScene()!=null){
   var pane=window.getScene().lookup(".dialog-pane");if(pane instanceof DialogPane dialog){((Button)dialog.lookupButton(ButtonType.OK)).fire();return;}
  }
  fail("Expected a confirmation dialog");
 });}
 @BeforeAll static void startup() throws Exception {var ready=new CountDownLatch(1);Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});assertTrue(ready.await(15,TimeUnit.SECONDS));}
 @AfterAll static void shutdown(){Platform.exit();}
 private static <T> T fx(Callable<T> action) throws Exception {var task=new FutureTask<>(action);Platform.runLater(task);return task.get(15,TimeUnit.SECONDS);}
 private EventPlanningFacade facade(){return new EventPlanningFacade(new EventService(),new EventPlanningAgent(p->{throw new AssertionError("No AI during UI smoke test");}),q->{throw new AssertionError("No Places during UI smoke test");},q->{throw new AssertionError("No external requests");});}
 @Test void bothPriorityControlsDisplayDescriptionsAndRetainStrategyIdentifiers() throws Exception {try(var facade=facade()){
  fx(()->{var session=new GuiSession(new EventPlanningController(facade));session.create("Priority test","Social","Campus","2027-02-01T18:00","2027-02-01T21:00","80","1000");
   var dashboard=new Dashboard(session);try{
    for(String page:java.util.List.of("Blueprints","Budget & catering")){
     button(dashboard,page).fire();
     assertTrue(nodes(dashboard.root()).anyMatch(n->n instanceof Label l&&l.getText().equals("Planning Priority")));
     assertTrue(nodes(dashboard.root()).anyMatch(n->n instanceof Label l&&l.getText().equals("Choose what the AI should prioritize when generating recommendations.")));
     @SuppressWarnings("unchecked") var priority=(ComboBox<String>)nodes(dashboard.root()).filter(n->n instanceof ComboBox<?> c&&"Planning Priority".equals(c.getAccessibleText())).findFirst().orElseThrow();
     var ids=java.util.List.of("budget","community","attendance");var labels=java.util.List.of("Budget \u2014 Minimize costs","Community \u2014 Encourage engagement","Attendance \u2014 Maximize turnout");
     for(int i=0;i<ids.size();i++){priority.getSelectionModel().select(i);assertEquals(ids.get(i),priority.getValue());assertEquals(labels.get(i),priority.getConverter().toString(priority.getValue()));assertEquals(ids.get(i),com.eventplanner.strategy.PlanningStrategy.fromName(priority.getValue()).name());}
     button(dashboard,"Events").fire();button(dashboard,page).fire();
     assertTrue(nodes(dashboard.root()).anyMatch(n->n instanceof ComboBox<?> c&&"Planning Priority".equals(c.getAccessibleText())&&"attendance".equals(c.getValue())));
    }
   }finally{dashboard.close();}return null;});
 }}
 private static com.eventplanner.model.VenueOption provider(java.util.Map<String,String> attributes,String source){return new com.eventplanner.model.VenueOption("google:provider","Actual provider","Campus address",source,java.time.Instant.parse("2026-01-01T00:00:00Z"),attributes,com.eventplanner.model.Evidence.unknown(),com.eventplanner.model.Evidence.unknown(),"CAD",new com.eventplanner.model.Evidence<>(false,com.eventplanner.model.Evidence.Status.PROVIDER_REPORTED,"Provider entrance record"),com.eventplanner.model.Evidence.unknown());}
 private static String labels(Node view){return nodes(view).filter(n->n instanceof Label).map(n->((Label)n).getText()).collect(java.util.stream.Collectors.joining("\n"));}
 @Test void providerContactsAreCopyableLinksAreSafeAndUnknownVenueRowsAreCollapsed() throws Exception {
  fx(()->{var opened=new java.util.ArrayList<String>();var provider=provider(java.util.Map.of("nationalPhoneNumber","416-555-0100","websiteUri","https://example.org/provider","email","invented@example.org","types","[\"restaurant\",\"establishment\"]"),"Google Places");
   var view=ProviderViews.venue(provider,opened::add);
   var phone=nodes(view).filter(n->n instanceof TextField f&&f.getText().equals("416-555-0100")).map(n->(TextField)n).findFirst().orElseThrow();assertFalse(phone.isEditable());assertFalse(phone.isDisabled());
   nodes(view).filter(n->n instanceof Hyperlink).map(n->(Hyperlink)n).findFirst().orElseThrow().fire();assertEquals(java.util.List.of("https://example.org/provider"),opened);
   assertFalse(nodes(view).anyMatch(n->n instanceof TextField f&&f.getText().contains("invented@")));
   var details=nodes(view).filter(n->n instanceof TitledPane p&&p.getText().equals("Additional Details & Verification")).map(n->(TitledPane)n).findFirst().orElseThrow();assertFalse(details.isExpanded());
   assertTrue(labels(view).contains("Requires direct confirmation: capacity, rental price, equipment, event availability."));
   assertTrue(labels(details).contains("false"));assertTrue(labels(details).contains("Provider entrance record"));
   assertFalse(labels(view).contains("UNKNOWN"));
   var absent=ProviderViews.venue(provider(java.util.Map.of(),"Google Places"),opened::add);assertTrue(labels(absent).contains("No contact details recorded"));assertFalse(nodes(absent).anyMatch(n->n instanceof Hyperlink));
   for(String bad:java.util.List.of("javascript:alert(1)","file:///C:/test","https://user:pass@example.org","bad uri")){
    assertFalse(ProviderViews.safeWebsite(bad));assertFalse(nodes(ProviderViews.catering(provider(java.util.Map.of("websiteUri",bad),"Google Places"),opened::add)).anyMatch(n->n instanceof Hyperlink));
   }
   var manual=ProviderViews.catering(provider(java.util.Map.of("email","planner@example.org"),"Manual planner entry: provider email confirmation"),opened::add);
   assertTrue(nodes(manual).anyMatch(n->n instanceof TextField f&&f.getText().equals("planner@example.org")));assertTrue(labels(manual).contains("manually recorded"));
   return null;});
 }
 @Test void cateringKeepsAiAssumptionsSeparateAndGenericRecommendationsHaveNoInventedContacts() throws Exception {
  fx(()->{var provider=provider(java.util.Map.of("nationalPhoneNumber","416-555-0100","types","[\"restaurant\"]"),"Google Places");
   var item=new com.eventplanner.model.BudgetItem("food","Catering","Food allowance",java.math.BigDecimal.ONE,new java.math.BigDecimal("400"),com.eventplanner.model.BudgetItem.PriceStatus.ESTIMATED,"AI estimate, not a quote");
   var generic=new com.eventplanner.model.CateringOption(null,"Vegetarian buffet food category; estimated $400",java.util.List.of("Confirm allergens"));
   var event=new com.eventplanner.model.Event("e","Dinner","Social","Campus",java.time.LocalDateTime.parse("2027-02-01T18:00"),80,1000);var names=new DisplayNames(event);
   var budget=new com.eventplanner.model.Budget("budget","CAD","budget",java.util.List.of(item),java.util.List.of(generic),java.util.List.of(),java.util.List.of("Delivery excluded; confirm pricing"));
   var view=ResultViews.catering(budget,names,url->fail("No website for generic catering"));
   assertTrue(labels(view).contains("No catering provider verified"));assertTrue(labels(view).contains("estimated $400"));assertTrue(labels(view).contains("Confirm allergens"));assertTrue(labels(view).contains("Delivery excluded; confirm pricing"));
   assertFalse(nodes(view).anyMatch(n->n instanceof TextField||n instanceof Hyperlink));
   var linked=new com.eventplanner.model.Budget("budget","CAD","budget",java.util.List.of(item),java.util.List.of(new com.eventplanner.model.CateringOption(provider.id(),"Consider this provider",generic.dietaryNotes())),java.util.List.of(provider),budget.assumptions());
   var linkedView=ResultViews.catering(linked,names,url->{});assertTrue(labels(linkedView).contains("Actual provider"));assertTrue(labels(linkedView).contains("restaurant"));assertTrue(labels(linkedView).contains("not independently verified"));
   for(String clutter:java.util.List.of("Capacity","Rental cost","Equipment","UNKNOWN"))assertFalse(labels(linkedView).contains(clutter),clutter);
   assertTrue(nodes(linkedView).anyMatch(n->n instanceof TextField f&&f.getText().equals("416-555-0100")));
   var comparison=new com.eventplanner.model.VenueComparison(provider.id(),1,java.util.List.of("Essential capacity warning"),"AI suitability interpretation",java.util.List.of("Price trade-off"));
   assertTrue(labels(ResultViews.comparison(comparison,names)).contains("Essential capacity warning"));assertTrue(labels(ResultViews.comparison(comparison,names)).contains("Price trade-off"));
   return null;});
 }
 @Test void actualAppStartsLoadsStylesAndClosesNormally(@org.junit.jupiter.api.io.TempDir java.nio.file.Path data) throws Exception {
  try(var saved=new EventPlanningFacade(new EventService(),new EventPlanningAgent(p->{throw new AssertionError("No network during startup test");}),q->{throw new AssertionError();},q->{throw new AssertionError();},new com.eventplanner.storage.JsonPlanRepository(data))){saved.createEventWithEnd("Saved startup event","Social","Campus",java.time.LocalDateTime.parse("2099-01-01T18:00"),java.time.LocalDateTime.parse("2099-01-01T21:00"),80,1000);}
  String previous=System.getProperty("planit.dataDir");System.setProperty("planit.dataDir",data.toString());var app=new App();
  try{app.init();fx(()->{var stage=new Stage();try{
   app.start(stage);assertTrue(stage.isShowing());assertTrue(stage.getScene().getStylesheets().getFirst().endsWith("dashboard.css"));assertEquals("Planit",stage.getTitle());
   assertTrue(nodes(stage.getScene().getRoot()).filter(n->n instanceof TableView<?>).map(n->(TableView<?>)n).flatMap(table->table.getItems().stream()).anyMatch(value->value instanceof com.eventplanner.model.Event event&&event.getName().equals("Saved startup event")));
   assertEquals(java.util.List.of(32.0,64.0,256.0,512.0),stage.getIcons().stream().map(javafx.scene.image.Image::getWidth).toList());assertTrue(stage.getIcons().stream().noneMatch(javafx.scene.image.Image::isError));
   assertTrue(nodes(stage.getScene().getRoot()).anyMatch(n->n instanceof Label label&&label.getText().equals("Planit")));
   assertTrue(nodes(stage.getScene().getRoot()).anyMatch(n->n instanceof Label label&&label.getText().equals("From ideas to execution.")));
   var logo=nodes(stage.getScene().getRoot()).filter(n->n instanceof javafx.scene.image.ImageView).map(n->(javafx.scene.image.ImageView)n).findFirst().orElseThrow();assertTrue(logo.isPreserveRatio());assertFalse(logo.getImage().isError());assertEquals(40,logo.getFitWidth());
  }finally{stage.close();app.stop();}return null;});
   for(Thread thread:Thread.getAllStackTraces().keySet())if(thread.getName().equals("planner-requests"))thread.join(5000);
  }finally{if(previous==null)System.clearProperty("planit.dataDir");else System.setProperty("planit.dataDir",previous);}
 }
 @Test void dateTimeControlsPreserveLocalValuesAndRejectIncompleteAndReversedWindows() throws Exception {
  fx(()->{
   var value=java.time.LocalDateTime.parse("2027-03-14T02:30:12.123456789");
   var input=new DateTimeInput(value);assertEquals(value,input.value());
   input.date.getEditor().setText("not a date");assertThrows(IllegalArgumentException.class,input::value);
   input.date.getEditor().setText(input.date.getConverter().toString(value.toLocalDate()));
   input.hour.setValue(14);input.minute.setValue(45);assertEquals(value.withHour(14).withMinute(45),input.value());
   var missing=new DateTimeInput(null);assertThrows(IllegalArgumentException.class,missing::value);
   missing.date.setValue(value.toLocalDate());missing.hour.setValue(10);assertThrows(IllegalArgumentException.class,missing::value);
   missing.minute.setValue(0);assertEquals(value.toLocalDate().atTime(10,0),missing.value());
   assertThrows(IllegalArgumentException.class,()->new com.eventplanner.model.TimeSlot(input.value(),missing.value()));
   var availability=new AvailabilityInput(java.util.List.of(new com.eventplanner.model.TimeSlot(value,value.plusHours(1))));
   assertEquals(java.util.List.of(new com.eventplanner.model.TimeSlot(value,value.plusHours(1))),availability.value());
   assertTrue(new AvailabilityInput(java.util.List.of()).value().isEmpty());
   nodes(availability).filter(n->n instanceof Button b&&b.getText().equals("Add availability interval")).map(n->(Button)n).findFirst().orElseThrow().fire();
   assertThrows(IllegalArgumentException.class,availability::value);
   var form=new Ui.Form().dateTime("Start",value);assertEquals(value.toString(),form.get("Start"));
   String id=java.util.UUID.randomUUID().toString();var choice=new Ui.Form().option("Member",java.util.List.of(new DisplayNames.Option(id,"Alex")),id);assertEquals(id,choice.get("Member"));
   var event=new com.eventplanner.model.Event("event","Club","Social","Campus",value,80,1000);
   String taskId=java.util.UUID.randomUUID().toString();
   event.storeTasks(java.util.List.of(new com.eventplanner.model.Task(taskId,"Set up room","Prepare seats",null,value,value.plusHours(1),1,java.util.List.of(),com.eventplanner.model.Task.Priority.HIGH,com.eventplanner.model.Task.Status.TODO)));
   var state=new PartBState();var outcomes=new TaskOutcomesInput(event.getTasks(),state,new DisplayNames(event));
   nodes(outcomes).filter(n->n instanceof ComboBox<?>).map(n->(ComboBox<?>)n).findFirst().orElseThrow().getSelectionModel().select(2);
   assertEquals(taskId,state.inputs.get("unfinished"));assertEquals("",state.inputs.get("completed"));assertEquals(taskId,event.getTasks().getFirst().id());
   assertTrue(nodes(outcomes).anyMatch(n->n instanceof Label label&&label.getText().equals("Set up room")));
   assertFalse(nodes(outcomes).anyMatch(n->n instanceof Label label&&label.getText().contains(taskId)));
   return null;
  });
 }
 @Test void structuredBlueprintPreservesEverySectionAndExactOriginalContent() throws Exception {
  fx(()->{
   var event=new com.eventplanner.model.Event("e","Club","Social","Campus",java.time.LocalDateTime.parse("2027-02-01T18:00"),80,1000);
   var blueprint=new com.eventplanner.model.EventBlueprint("blueprint-ref","concept-ref","budget","Community night","Overview paragraph\n\nSecond paragraph",java.util.List.of("Welcome agenda"),java.util.List.of("Venue not booked"),java.util.List.of("Catering unverified"),java.util.List.of("Projector"),java.util.List.of("Confirm supplier"),java.util.List.of("All costs estimated"),java.util.List.of("Capacity unknown"),900,80,100);
   var view=ResultViews.blueprint(blueprint,new DisplayNames(event),java.util.List.of("Indoor access"));
   var labels=nodes(view).filter(n->n instanceof Label).map(n->((Label)n).getText()).toList();String rendered=String.join("\n",labels);
   for(String content:java.util.List.of("Overview paragraph","Second paragraph","Welcome agenda","Venue not booked","Catering unverified","Projector","Confirm supplier","All costs estimated","Capacity unknown","Indoor access"))assertTrue(rendered.contains(content),content);
   assertFalse(rendered.contains("blueprint-ref"));
   var original=nodes(view).filter(n->n instanceof TextArea).map(n->((TextArea)n).getText()).findFirst().orElseThrow();assertEquals(blueprint.toString(),original);
   var technical=nodes(view).filter(n->n instanceof TitledPane).map(n->(TitledPane)n).findFirst().orElseThrow();assertFalse(technical.isExpanded());
   return null;
  });
 }
 @Test void allFeaturePanelsRenderAndNavigationRetainsEvent() throws Exception {try(var facade=facade()){
  fx(()->{var session=new GuiSession(new EventPlanningController(facade));var event=session.create("Smoke event","Social","Campus","2027-02-01T18:00","2027-02-01T21:00","80","1000");
   var dashboard=new Dashboard(session);var stage=new Stage();try{
    var scene=new Scene(dashboard.root(),1280,850);scene.getStylesheets().add(App.class.getResource("dashboard.css").toExternalForm());stage.setScene(scene);stage.show();
    var root=(BorderPane)dashboard.root();var sidebar=(VBox)((ScrollPane)root.getLeft()).getContent();
    for(double width:new double[]{1280,880}){
     stage.setWidth(width);stage.setHeight(640);
     int visited=0;
     for(var node:sidebar.getChildren())if(node instanceof Button button){button.fire();root.applyCss();root.layout();assertNotNull(root.getCenter());assertEquals(event.getId(),session.activeId());visited++;}
     assertEquals(11,visited);assertTrue(((ScrollPane)root.getLeft()).isFitToWidth());
    }
    session.select(null);dashboard.refresh();assertNotNull(root.getCenter());
   }finally{stage.close();dashboard.close();}return null;});
 }}
 @Test void calendarCreateFormValidatesBeforeDelegatingAndKeepsLocalTimes() throws Exception {try(var facade=facade()){
  Dashboard d=fx(()->new Dashboard(new GuiSession(new EventPlanningController(facade))));
  try{
   operation(d,()->{var session=d.session;var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
    Platform.runLater(()->{
     DialogPane pane=null;
     try{
      pane=javafx.stage.Window.getWindows().stream().filter(w->w.getScene()!=null).map(w->w.getScene().lookup(".dialog-pane")).filter(n->n instanceof DialogPane).map(n->(DialogPane)n).findFirst().orElseThrow();
      var values=java.util.Map.of("Name","Calendar event","Type","Social","Location","Campus","Attendance","80","Budget","1000");
      nodes(pane).filter(n->n instanceof TextField field&&field.getAccessibleText()!=null&&values.containsKey(field.getAccessibleText())).map(n->(TextField)n).forEach(field->field.setText(values.get(field.getAccessibleText())));
      var dates=nodes(pane).filter(n->n instanceof DateTimeInput).map(n->(DateTimeInput)n).toList();assertEquals(2,dates.size());
      for(var date:dates){date.date.setValue(java.time.LocalDate.of(2027,3,14));date.hour.setValue(2);date.minute.setValue(30);}
      ((Button)pane.lookupButton(ButtonType.OK)).fire();
      assertTrue(session.controller().listEvents().isEmpty());
      assertTrue(nodes(pane).anyMatch(n->n instanceof Label label&&label.getStyleClass().contains("error")&&!label.getText().isBlank()));
      dates.getLast().hour.setValue(4);((Button)pane.lookupButton(ButtonType.OK)).fire();
     }catch(Throwable ex){failure.set(ex);if(pane!=null)((Button)pane.lookupButton(ButtonType.CANCEL)).fire();}
    });
    button(d,"Create event").fire();if(failure.get()!=null)throw new AssertionError(failure.get());

   });
   fx(()->{assertEquals(java.time.LocalDateTime.parse("2027-03-14T02:30"),d.session.active().getDateTime());assertEquals(java.time.LocalDateTime.parse("2027-03-14T04:30"),d.session.active().getEndDateTime());assertEquals(80,d.session.active().getExpectedAttendance());return null;});
  }finally{fx(()->{d.close();return null;});}
 }}
 @Test void placesButtonExecutesOffFxAndRetainsSearchInput() throws Exception {
  var query=new java.util.concurrent.atomic.AtomicReference<String>();var cleaned=new CountDownLatch(1);
  var facade=new EventPlanningFacade(new EventService(),new EventPlanningAgent(p->{throw new AssertionError("No AI expected");}),q->{assertFalse(Platform.isFxApplicationThread());query.set(q);return new com.eventplanner.model.ToolResult("Test",java.util.List.of(),java.util.List.of("Availability remains unknown"));},q->{throw new AssertionError("No catering expected");});
  Dashboard d=fx(()->{var session=new GuiSession(new EventPlanningController(facade));session.create("Club","Social","Campus","2027-02-01T18:00","2027-02-01T21:00","80","1000");return new Dashboard(session,()->{facade.close();cleaned.countDown();});});
  try{
   operation(d,()->{d.partB(d.id()).inputs.put("venueQuery","Accessible hall");button(d,"Venue research").fire();button(d,"Research venues").fire();button(d,"Ideas").fire();});
   fx(()->{button(d,"Venue research").fire();assertEquals("Accessible hall near Campus",query.get());assertTrue(nodes(d.root()).anyMatch(n->n instanceof TextField field&&field.getText().equals("Accessible hall")));assertEquals(java.util.List.of("Availability remains unknown"),d.session.venueWarnings(d.id()));return null;});
  }finally{fx(()->{d.close();return null;});assertTrue(cleaned.await(10,TimeUnit.SECONDS));}
 }
 @Test void persistentSavesStayOffFxAndFailedSavesRestoreTheView(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory)throws Exception{
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var cleaned=new CountDownLatch(1);
  var block=new java.util.concurrent.atomic.AtomicBoolean();var fail=new java.util.concurrent.atomic.AtomicBoolean();
  var repository=new com.eventplanner.storage.JsonPlanRepository(directory){
   @Override public void save(com.eventplanner.storage.PlanData data){
    assertFalse(Platform.isFxApplicationThread());
    if(block.get()){entered.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new AssertionError(ex);}}
    if(fail.get())throw new com.eventplanner.storage.StorageException("Save failed. Previous saved data is retained.");super.save(data);
   }
  };
  var facade=new EventPlanningFacade(new EventService(),new EventPlanningAgent(p->{throw new AssertionError("No AI");}),q->{throw new AssertionError("No Places");},q->{throw new AssertionError("No catering");},repository);
  String first=facade.createEvent("First","Social","Campus",java.time.LocalDateTime.parse("2099-01-01T18:00"),80,1000).getId();
  String second=facade.createEvent("Second","Social","Campus",java.time.LocalDateTime.parse("2099-01-02T18:00"),80,1000).getId();
  Dashboard d=fx(()->{var session=new GuiSession(new EventPlanningController(facade));session.select(first);return new Dashboard(session,()->{facade.close();cleaned.countDown();});});
  try{
   var done=new CountDownLatch(1);block.set(true);
   fx(()->{status(d).textProperty().addListener((o,a,b)->{if(!d.busy())done.countDown();});d.background("Saving constraint",()->{d.controller.addConstraint(first,"Durable constraint");return null;});return null;});
   assertTrue(entered.await(10,TimeUnit.SECONDS));
   fx(()->{assertTrue(d.busy());d.session.select(second);button(d,"Ideas").fire();assertEquals(second,d.id());return null;});
   release.countDown();assertTrue(done.await(10,TimeUnit.SECONDS));block.set(false);byte[] before=java.nio.file.Files.readAllBytes(repository.file());
   var failed=new CountDownLatch(1);fail.set(true);
   fx(()->{status(d).textProperty().addListener((o,a,b)->{if(b.startsWith("Save failed"))failed.countDown();});d.background("Saving failing constraint",()->{d.controller.addConstraint(second,"Unsaved constraint");return null;});return null;});
   assertTrue(failed.await(10,TimeUnit.SECONDS));
   fx(()->{assertFalse(d.busy());assertTrue(d.controller.viewEvent(second).getConstraints().isEmpty());assertTrue(status(d).getText().contains("Previous saved data"));return null;});
   assertArrayEquals(before,java.nio.file.Files.readAllBytes(repository.file()));
  }finally{release.countDown();fx(()->{d.close();return null;});assertTrue(cleaned.await(10,TimeUnit.SECONDS));}
  try(var saved=new com.eventplanner.storage.JsonPlanRepository(directory)){
   var data=saved.load().orElseThrow();var restored=com.eventplanner.storage.PlanDataMapper.restore(data);assertEquals(java.util.List.of("Durable constraint"),restored.stream().filter(e->e.getId().equals(first)).findFirst().orElseThrow().getConstraints());
  }
 }
 @Test void backgroundRequestsStayOffFxThreadAndRejectRepeatedSubmissions() throws Exception {try(var facade=facade()){
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var calls=new java.util.concurrent.atomic.AtomicInteger();
  Dashboard dashboard=fx(()->new Dashboard(new GuiSession(new EventPlanningController(facade))));
  try{
   fx(()->{dashboard.background("Offline background check",()->{assertFalse(Platform.isFxApplicationThread());calls.incrementAndGet();entered.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return null;});dashboard.background("Duplicate",()->{calls.incrementAndGet();return null;});assertTrue(dashboard.busy());var root=(BorderPane)dashboard.root();assertFalse(root.getLeft().isDisabled());assertFalse(root.getTop().isDisabled());
    for(var node:((VBox)((ScrollPane)root.getLeft()).getContent()).getChildren())if(node instanceof Button button)button.fire();
    assertTrue(dashboard.busy());return null;});
   assertTrue(entered.await(10,TimeUnit.SECONDS));release.countDown();
   long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(fx(dashboard::busy)&&System.nanoTime()<deadline)Thread.sleep(25);
   assertFalse(fx(dashboard::busy));assertEquals(1,calls.get());
   fx(()->{dashboard.background("Failure check",()->{throw new PlanningException("Safe offline failure");});return null;});
   deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(fx(dashboard::busy)&&System.nanoTime()<deadline)Thread.sleep(25);
   assertFalse(fx(dashboard::busy));
   fx(()->{var footer=(HBox)((BorderPane)dashboard.root()).getBottom();assertEquals("Safe offline failure",((Label)footer.getChildren().getLast()).getText());return null;});
  }finally{release.countDown();fx(()->{dashboard.close();return null;});}
 }}
 @Test void switchingEventsDoesNotRedirectResultsAndCloseWaitsForWorker() throws Exception {
  var facade=facade();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var cleaned=new CountDownLatch(1);
  var completed=new CountDownLatch(1);
  Dashboard dashboard=fx(()->{
   var session=new GuiSession(new EventPlanningController(facade));
   session.create("First","Social","Campus","2027-02-01T18:00","2027-02-01T21:00","80","1000");
   session.create("Second","Social","Campus","2027-02-02T18:00","2027-02-02T21:00","80","1000");
   return new Dashboard(session,()->{facade.close();cleaned.countDown();});
  });
  try {
   String first=fx(()->dashboard.controller.listEvents().stream().filter(e->e.getName().equals("First")).findFirst().orElseThrow().getId());
   String second=fx(()->dashboard.session.activeId());
   fx(()->{
    dashboard.session.select(first);dashboard.refresh();
    var footer=(HBox)((BorderPane)dashboard.root()).getBottom();
    ((Label)footer.getChildren().getLast()).textProperty().addListener((o,a,b)->{if(b.contains("completed"))completed.countDown();});
    dashboard.background("Venue check",()->{entered.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return new com.eventplanner.model.ToolResult("Test",java.util.List.of(),java.util.List.of("First event warning"));});
    return null;
   });
   assertTrue(entered.await(10,TimeUnit.SECONDS));
   fx(()->{dashboard.session.select(second);dashboard.refresh();assertEquals(second,dashboard.session.activeId());return null;});
   release.countDown();assertTrue(completed.await(10,TimeUnit.SECONDS));
   fx(()->{assertEquals(second,dashboard.session.activeId());assertEquals(java.util.List.of("First event warning"),dashboard.session.venueWarnings(first));assertTrue(dashboard.session.venueWarnings(second).isEmpty());return null;});
   var finishing=new CountDownLatch(1);var finish=new CountDownLatch(1);
   fx(()->{dashboard.background("Closing check",()->{finishing.countDown();assertTrue(finish.await(10,TimeUnit.SECONDS));return null;});return null;});
   assertTrue(finishing.await(10,TimeUnit.SECONDS));
   try {fx(()->{dashboard.close();dashboard.close();return null;});assertEquals(1,cleaned.getCount());}
   finally {finish.countDown();}
   assertTrue(cleaned.await(10,TimeUnit.SECONDS));
  } finally {release.countDown();fx(()->{dashboard.close();return null;});}
 }

 @Test void partBButtonsDelegateThroughBackendAndRetainEventScopedResults() throws Exception {
  var replies=new java.util.ArrayDeque<String>(java.util.List.of(
   "{\"changes\":{\"attendance\":\"150\"},\"ambiguities\":[]}",
   "{\"affectedComponents\":[\"catering\"],\"implications\":[\"More meals needed\"],\"warnings\":[\"Confirm capacity\"],\"recommendations\":[\"Review resources\"]}",
   "{\"changes\":{\"budget\":\"700\"},\"ambiguities\":[]}",
   "{\"affectedComponents\":[\"budget\"],\"implications\":[\"Review allocations\"],\"warnings\":[],\"recommendations\":[\"Obtain quotes\"]}",
   "{\"risk\":\"Speaker cancels\",\"impact\":\"Agenda gap\",\"proposedResponse\":\"Prepare a backup activity\",\"resources\":[\"Facilitator\"],\"assumptions\":[\"Availability unknown\"]}",
   "{\"factOrder\":[\"name\",\"type\",\"dateTime\"],\"opening\":\"Hello friends!\",\"closing\":\"Join us!\"}",
   "{\"executiveSummary\":\"Limited recorded evidence\",\"successes\":[],\"issues\":[],\"lessonsLearned\":[\"Record outcomes\"],\"recommendations\":[\"Collect feedback\"]}"
  ));
  var facade=new EventPlanningFacade(new EventService(),new EventPlanningAgent(prompt->{assertFalse(Platform.isFxApplicationThread());return replies.remove();}),q->{throw new AssertionError("No Places");},q->{throw new AssertionError("No catering");});
  var cleaned=new CountDownLatch(1);
  Dashboard d=fx(()->{
   var session=new GuiSession(new EventPlanningController(facade));
   var first=session.create("First","Social","Campus","2027-02-01T18:00","2027-02-01T21:00","80","1000");
   session.create("Second","Social","Campus","2027-02-02T18:00","2027-02-02T21:00","80","1000");session.select(first.getId());
   return new Dashboard(session,()->{facade.close();cleaned.countDown();});
  });
  try {
   String first=fx(d::id);String second=fx(()->d.controller.listEvents().stream().filter(e->e.getName().equals("Second")).findFirst().orElseThrow().getId());
   operation(d,()->{
    button(d,"Scenario Simulator").fire();d.partB(first).inputs.put("scenario","Attendance increases to 150");d.refresh();
    button(d,"Run simulation").fire();d.session.select(second);button(d,"Communications & FAQ").fire();
   });
   fx(()->{
    assertEquals(second,d.id());assertNull(d.partB(second).simulation);assertNotNull(d.partB(first).simulation);
    assertEquals(80,d.controller.viewEvent(first).getExpectedAttendance());
    d.session.select(first);button(d,"Scenario Simulator").fire();
    assertTrue(nodes(d.root()).anyMatch(n->n instanceof TextArea area&&area.getText().contains("HYPOTHETICAL — not applied")));
    button(d,"Clear simulation").fire();assertNull(d.partB(first).simulation);assertEquals("",d.partB(first).inputs.get("scenario"));return null;
   });
   operation(d,()->{d.partB(first).inputs.put("change","Reduce budget to 700");button(d,"Plan Updates & Contingencies").fire();button(d,"Preview changes & impacts").fire();});
   fx(()->{assertEquals(1000,d.session.active().getTotalBudget());assertEquals("700",d.controller.pendingChanges(first).changes().get("budget"));return null;});
   operation(d,()->{acceptNextDialog();button(d,"Confirm Changes").fire();});
   fx(()->{assertEquals(700,d.session.active().getTotalBudget());assertThrows(IllegalArgumentException.class,()->d.controller.pendingChanges(first));return null;});
   operation(d,()->{d.partB(first).inputs.put("risk","Speaker cancels");d.refresh();button(d,"Generate contingency").fire();});
   fx(()->{assertEquals(1,d.controller.listContingencies(first).size());return null;});
   operation(d,()->{button(d,"Communications & FAQ").fire();button(d,"Generate communication draft").fire();});
   String communication=fx(()->d.partB(first).selectedCommunication);
   operation(d,()->{
    nodes(d.root()).filter(n->n instanceof TextArea area&&!area.isDisabled()&&area.isEditable()).map(n->(TextArea)n).findFirst().orElseThrow().setText("Planner revised the draft");
    button(d,"Save draft edits").fire();
   });
   fx(()->{assertEquals("Planner revised the draft",d.controller.viewCommunication(first,communication).text());assertFalse(d.controller.viewCommunication(first,communication).approved());return null;});
   operation(d,()->{acceptNextDialog();button(d,"Approve draft").fire();});
   fx(()->{assertTrue(d.controller.viewCommunication(first,communication).approved());return null;});
   operation(d,()->{d.partB(first).inputs.put("faq","What is the ticket price?");d.refresh();button(d,"Ask attendee FAQ").fire();});
   fx(()->{assertTrue(d.partB(first).faqAnswer.contains("not confirmed"));button(d,"Post-Event Review").fire();return null;});
   operation(d,()->{acceptNextDialog();button(d,"Save actual outcomes").fire();});
   fx(()->{assertNotNull(d.session.active().getPostEventData());assertNull(d.session.active().getPostEventData().actualAttendance());return null;});
   operation(d,()->button(d,"Generate post-event analysis").fire());
   fx(()->{
    assertEquals(1,d.controller.viewPostEventReports(first).size());
    assertTrue(nodes(d.root()).anyMatch(n->n instanceof TextArea area&&area.getText().contains("Actual attendance: UNKNOWN")));
    assertTrue(replies.isEmpty());return null;
   });
  } finally {fx(()->{d.close();return null;});assertTrue(cleaned.await(10,TimeUnit.SECONDS));}
 }

}
