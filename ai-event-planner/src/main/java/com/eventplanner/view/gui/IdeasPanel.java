package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
final class IdeasPanel {
 private final Dashboard d; IdeasPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();var names=new DisplayNames(d.session.active());var ideas=Ui.table(d.controller.listIdeas(id));Ui.column(ideas,"Organizer",idea->names.readable(idea.organizer()));Ui.column(ideas,"Idea",EventIdea::text);d.remember(ideas,"ideas",EventIdea::id);
  var concepts=Ui.table(d.controller.listConcepts(id));Ui.column(concepts,"Alternative",EventConcept::title);d.remember(concepts,"concepts",EventConcept::id);
  return Ui.page("Ideas & concepts","Collect perspectives, then use Gemini to explore alternative concepts. Generated concepts remain unverified proposals.",
   Ui.row(Ui.button("Add organizer idea",()->new Ui.Form().field("Organizer","").field("Idea","").show("Add idea",f->{d.controller.addIdea(id,f.get("Organizer"),f.get("Idea"));d.edited();})),Ui.button("Fuse ideas with Gemini",()->d.background("Fusing ideas",()->d.controller.fuseIdeas(id)))),
   Ui.card("Organizer ideas",ideas),Ui.card("Concept alternatives",Ui.inspectView(concepts,c->ResultViews.concept(c,names))));
 }
 static String describe(EventConcept c){return c.title()+"\nID: "+c.id()+"\n"+c.summary()+Ui.lines("Themes",c.themes())+Ui.lines("Conflicts",c.conflicts())+Ui.lines("Compromises",c.compromises())+Ui.lines("Uncertainties",c.uncertainties());}
}
