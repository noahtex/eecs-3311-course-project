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
  var children=root instanceof ScrollPane scroll?java.util.stream.Stream.of(scroll.getContent()):root instanceof Parent parent?parent.getChildrenUnmodifiable().stream():java.util.stream.Stream.<Node>empty();
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
 @Test void actualAppStartsLoadsStylesAndClosesNormally() throws Exception {
  fx(()->{var app=new App();var stage=new Stage();try{app.start(stage);assertTrue(stage.isShowing());assertTrue(stage.getScene().getStylesheets().getFirst().endsWith("dashboard.css"));assertEquals("ClubPlan | Event planning workspace",stage.getTitle());}finally{stage.close();app.stop();}return null;});
 }
 @Test void allFeaturePanelsRenderAndNavigationRetainsEvent() throws Exception {try(var facade=facade()){
  fx(()->{var session=new GuiSession(new EventPlanningController(facade));var event=session.create("Smoke event","Social","Campus","2027-02-01T18:00","2027-02-01T21:00","80","1000");
   var dashboard=new Dashboard(session);var stage=new Stage();try{
    var scene=new Scene(dashboard.root(),1280,850);scene.getStylesheets().add(App.class.getResource("dashboard.css").toExternalForm());stage.setScene(scene);stage.show();
    var root=(BorderPane)dashboard.root();var sidebar=(VBox)root.getLeft();
    for(var node:sidebar.getChildren())if(node instanceof Button button){button.fire();root.applyCss();root.layout();assertNotNull(root.getCenter());assertEquals(event.getId(),session.activeId());}
    session.select(null);dashboard.refresh();assertNotNull(root.getCenter());
   }finally{stage.close();dashboard.close();}return null;});
 }}
 @Test void backgroundRequestsStayOffFxThreadAndRejectRepeatedSubmissions() throws Exception {try(var facade=facade()){
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var calls=new java.util.concurrent.atomic.AtomicInteger();
  Dashboard dashboard=fx(()->new Dashboard(new GuiSession(new EventPlanningController(facade))));
  try{
   fx(()->{dashboard.background("Offline background check",()->{assertFalse(Platform.isFxApplicationThread());calls.incrementAndGet();entered.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return null;});dashboard.background("Duplicate",()->{calls.incrementAndGet();return null;});assertTrue(dashboard.busy());var root=(BorderPane)dashboard.root();assertFalse(root.getLeft().isDisabled());assertFalse(root.getTop().isDisabled());
    for(var node:((VBox)root.getLeft()).getChildren())if(node instanceof Button button)button.fire();
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
