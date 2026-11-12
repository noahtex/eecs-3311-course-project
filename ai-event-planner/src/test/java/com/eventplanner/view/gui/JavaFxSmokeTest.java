package com.eventplanner.view.gui;

import com.eventplanner.App;
import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import javafx.application.Platform;
import javafx.scene.Scene;
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

}
