package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import java.util.List;

final class CommunicationsPanel {
 private final Dashboard d;
 CommunicationsPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var state=d.partB(id);var facts=d.session.active().changeFields();
  var channel=Ui.choice(List.of(Communication.Channel.values()));
  channel.setConverter(new StringConverter<>(){
   public String toString(Communication.Channel c){return c==null?"":switch(c){case EMAIL->"Email";case INSTAGRAM->"Instagram";case WHATSAPP->"WhatsApp / group chat";};}
   public Communication.Channel fromString(String value){return null;}
  });
  channel.setValue(Communication.Channel.valueOf(state.inputs.getOrDefault("channel","EMAIL")));
  channel.valueProperty().addListener((o,a,b)->state.inputs.put("channel",b.name()));
  // The backend accepts any nonblank tone. These are also used by the existing CLI/tests.
  var tone=Ui.choice(List.of("friendly","formal","casual"));tone.setValue(state.inputs.getOrDefault("tone","friendly"));
  tone.valueProperty().addListener((o,a,b)->state.inputs.put("tone",b));
  var drafts=d.controller.listCommunications(id);var selection=Ui.choice(drafts);
  selection.setConverter(new StringConverter<>(){
   public String toString(Communication c){return c==null?"":"Draft "+(drafts.indexOf(c)+1)+" | "+c.channel()+" | "+c.tone()+" | "+(c.approved()?"APPROVED":"DRAFT");}
   public Communication fromString(String value){return null;}
  });
  Communication selected=drafts.stream().filter(c->c.id().equals(state.selectedCommunication)).findFirst().orElse(drafts.isEmpty()?null:drafts.getLast());
  selection.setValue(selected);state.selectedCommunication=selected==null?null:selected.id();
  selection.valueProperty().addListener((o,a,b)->{state.selectedCommunication=b==null?null:b.id();d.refresh();});
  String key="communication:"+(selected==null?"none":selected.id());
  if(selected!=null)state.inputs.putIfAbsent(key,selected.text());
  var editor=state.area(key,"Select or generate a communication draft.");editor.setPrefRowCount(9);editor.setDisable(selected==null);
  var save=Ui.button("Save draft edits",()->d.action(()->{
   String body=GuiInputs.required(editor.getText());
   d.background("Saving communication draft",()->d.controller.editCommunication(id,selected.id(),body),result->state.inputs.remove(key));
  }));save.setDisable(selected==null);
  var approve=Ui.button("Approve draft",()->d.action(()->{
   if(!editor.getText().equals(selected.text()))throw new IllegalArgumentException("Save draft edits before approving this communication.");
   if(Ui.confirm("Approve communication?","Approve the displayed saved draft? This does not send or post it."))
    d.background("Approving communication",()->d.controller.approveCommunication(id,selected.id()));
  }));approve.setDisable(selected==null||selected.approved());
  var question=state.field("faq","e.g. When is the event?");question.setPrefWidth(500);
  question.textProperty().addListener((o,a,b)->state.faqAnswer=null);
  String answer=state.faqAnswer!=null&&facts.equals(state.faqFacts)?state.faqAnswer:"Ask a question. Missing or unconfirmed facts will be identified explicitly.";
  return Ui.page("Communications & FAQ","Drafts use approved event facts. Review all planner edits before approval. Approval does not send or publish anything.",
   Ui.card("Generate communication",Ui.row(Ui.field("Channel",channel),Ui.field("Tone",tone),Ui.button("Generate communication draft",()->{
    var c=channel.getValue();String t=tone.getValue();
    d.background("Generating communication draft",()->d.controller.generateCommunication(id,c,t),result->state.selectedCommunication=result.id());
   }))),
   Ui.card("Stored communications",selection,Ui.badge(selected==null?"NO DRAFT":selected.approved()?"APPROVED":"DRAFT"),Ui.note("Editing and saving returns this communication to DRAFT."),editor,Ui.row(save,approve),Ui.technical(selected==null?"No communication selected.":selected.toString())),
   Ui.card("Attendee FAQ",Ui.row(question,Ui.button("Ask attendee FAQ",()->d.action(()->{
    String q=GuiInputs.required(question.getText());
    d.background("Answering attendee FAQ",()->d.controller.askFaq(id,q),result->{state.faqAnswer=result;state.faqFacts=facts;});
   }))),faqAnswer(state.inputs.getOrDefault("faq",""),answer)));
 }
 private static Node faqAnswer(String question,String answer){
  var view=new javafx.scene.layout.VBox(12,Ui.field("Your question",Ui.prose(question.isBlank()?"No question yet":question)),Ui.field("Event assistant",Ui.prose(answer)));view.getStyleClass().add("faq-answer");return view;
 }
}
