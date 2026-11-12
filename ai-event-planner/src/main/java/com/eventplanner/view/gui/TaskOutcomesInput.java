package com.eventplanner.view.gui;

import com.eventplanner.model.Task;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.*;

/** Named approved-task outcomes; the state continues storing backend task IDs. */
final class TaskOutcomesInput extends VBox {
 private enum Outcome { UNRECORDED, COMPLETED, UNFINISHED }
 private record Row(String id,ComboBox<Outcome> choice) { }
 private final List<Row> rows=new ArrayList<>();
 TaskOutcomesInput(List<Task> tasks,PartBState state,DisplayNames names){
  super(10);var completed=GuiInputs.csv(state.inputs.getOrDefault("completed",""));var unfinished=GuiInputs.csv(state.inputs.getOrDefault("unfinished",""));
  var ids=new LinkedHashSet<String>();tasks.forEach(task->ids.add(task.id()));ids.addAll(completed);ids.addAll(unfinished);
  if(ids.isEmpty())getChildren().add(Ui.note("No approved tasks to record. Task completion remains unknown."));
  for(String id:ids){
   var choice=Ui.choice(List.of(Outcome.values()));choice.setValue(completed.contains(id)?Outcome.COMPLETED:unfinished.contains(id)?Outcome.UNFINISHED:Outcome.UNRECORDED);
   choice.setAccessibleText("Outcome for "+names.task(id));rows.add(new Row(id,choice));getChildren().add(Ui.field(names.task(id),choice));
   choice.valueProperty().addListener((o,a,b)->save(state));
  }
 }
 private void save(PartBState state){
  state.inputs.put("completed",String.join(", ",rows.stream().filter(r->r.choice.getValue()==Outcome.COMPLETED).map(Row::id).toList()));
  state.inputs.put("unfinished",String.join(", ",rows.stream().filter(r->r.choice.getValue()==Outcome.UNFINISHED).map(Row::id).toList()));
 }
}
