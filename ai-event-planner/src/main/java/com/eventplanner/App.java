package com.eventplanner;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.view.gui.Dashboard;
import com.eventplanner.view.gui.GuiSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Screen;
import java.io.IOException;
import com.eventplanner.storage.StorageException;
import javafx.scene.control.Alert;

/** Desktop entry point. The CLI retains its independent session and launcher. */
public class App extends Application {
 private static Scene scene;
 private Dashboard dashboard;
 private EventPlanningFacade facade;
 private String startupError;
 /** JavaFX calls init on its launcher thread, so loading JSON never blocks the FX thread. */
 @Override public void init(){try{facade=EventPlanningFacade.persistent();}catch(StorageException ex){startupError=ex.getMessage();}}
 @Override public void start(Stage stage) {
  if(startupError!=null){var alert=new Alert(Alert.AlertType.ERROR,startupError);alert.setTitle("Planit — saved data unavailable");alert.setHeaderText("Planit could not open saved data");alert.getDialogPane().getStylesheets().add(App.class.getResource("dashboard.css").toExternalForm());alert.setResizable(true);alert.showAndWait();return;}
  if(facade==null)throw new IllegalStateException("Initialize Planit's storage before starting the GUI.");
  dashboard=new Dashboard(new GuiSession(new EventPlanningController(facade)),facade::close);
  scene=new Scene(dashboard.root(),1280,850);
  scene.getStylesheets().add(App.class.getResource("dashboard.css").toExternalForm());
  var screen=Screen.getPrimary().getVisualBounds();
  stage.setTitle("Planit");
  for(int size:new int[]{32,64,256,512})stage.getIcons().add(new Image(java.util.Objects.requireNonNull(
   App.class.getResource("assets/planit/planit_icon_"+size+".png"),"Missing Planit stage icon: "+size).toExternalForm()));
  stage.setMinWidth(Math.min(880,screen.getWidth()-32));stage.setMinHeight(Math.min(600,screen.getHeight()-32));stage.setScene(scene);
  stage.setWidth(Math.min(1280,screen.getWidth()-32));stage.setHeight(Math.min(850,screen.getHeight()-32));stage.show();
 }
 /** Retained for the unused generated FXML starter controllers. */
 static void setRoot(String fxml) throws IOException { scene.setRoot(new FXMLLoader(App.class.getResource(fxml+".fxml")).load()); }
 @Override public void stop(){if(dashboard!=null)dashboard.close();else if(facade!=null)facade.close();}
 public static void main(String[] args){launch(args);}
}
