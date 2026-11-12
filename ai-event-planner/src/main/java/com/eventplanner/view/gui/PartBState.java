package com.eventplanner.view.gui;

import com.eventplanner.model.ScenarioResult;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.util.HashMap;
import java.util.Map;

/** Event-scoped, FX-thread-only input and transient result state. Domain history stays in the backend. */
final class PartBState {
 final Map<String,String> inputs=new HashMap<>();
 ScenarioResult simulation,changeImpact;
 String selectedCommunication;
 int selectedReport=-1;
 String faqAnswer;
 Map<String,String> faqFacts;
 boolean outcomesLoaded;

 TextArea area(String key,String prompt){
  var input=new TextArea(inputs.getOrDefault(key,""));input.setWrapText(true);input.setPrefRowCount(3);input.setPromptText(prompt);
  input.textProperty().addListener((o,a,b)->inputs.put(key,b));return input;
 }
 TextField field(String key,String prompt){
  var input=new TextField(inputs.getOrDefault(key,""));input.setPromptText(prompt);
  input.textProperty().addListener((o,a,b)->inputs.put(key,b));return input;
 }
}
