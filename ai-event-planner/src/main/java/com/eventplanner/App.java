package com.eventplanner;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import com.eventplanner.view.gui.Dashboard;
import com.eventplanner.view.gui.GuiSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/** Desktop entry point. The CLI retains its independent session and launcher. */
public class App extends Application {
 private static Scene scene;
 private Dashboard dashboard;
 @Override public void start(Stage stage) {
  var facade=new EventPlanningFacade(new EventService());
  dashboard=new Dashboard(new GuiSession(new EventPlanningController(facade)),facade::close);
  scene=new Scene(dashboard.root(),1280,850);
  scene.getStylesheets().add(App.class.getResource("dashboard.css").toExternalForm());
  stage.setTitle("ClubPlan | Event planning workspace");stage.setMinWidth(1050);stage.setMinHeight(700);stage.setScene(scene);stage.show();
 }
 /** Retained for the unused generated FXML starter controllers. */
 static void setRoot(String fxml) throws IOException { scene.setRoot(new FXMLLoader(App.class.getResource(fxml+".fxml")).load()); }
 @Override public void stop(){if(dashboard!=null)dashboard.close();}
 public static void main(String[] args){launch(args);}
}
