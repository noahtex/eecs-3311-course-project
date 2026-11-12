package com.eventplanner.view.gui;
import com.eventplanner.model.*;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import java.time.Instant;
import java.util.*;
final class VenuesPanel {
 private final Dashboard d; VenuesPanel(Dashboard d){this.d=d;}
 Node build(){
  String id=d.id();Event e=d.session.active();var names=new DisplayNames(e);var query=d.partB(id).field("venueQuery","e.g. accessible community hall with projector");query.setPromptText("e.g. accessible community hall with projector");query.setPrefWidth(390);
  var table=Ui.table(d.controller.listVenues(id));Ui.column(table,"Venue",VenueOption::name);Ui.column(table,"Address",VenueOption::address);Ui.column(table,"Capacity",v->fact(v.capacity()));Ui.column(table,"Rental cost",v->fact(v.rentalCost())+" "+v.currency());Ui.column(table,"Wheelchair entrance",v->fact(v.wheelchairEntrance()));Ui.column(table,"Availability",v->"UNKNOWN");d.remember(table,"venues",VenueOption::id);
  var comparisons=new javafx.scene.layout.VBox(14);for(var comparison:e.getVenueComparisons())comparisons.getChildren().add(ResultViews.comparison(comparison,names));if(e.getVenueComparisons().isEmpty())comparisons.getChildren().add(Ui.note("Compare researched candidates to inspect suitability and trade-offs."));
  return Ui.page("Venue research","Search real Google Places candidates. Candidates are not bookings; availability and unknown attributes require direct confirmation.",
   Ui.row(query,Ui.button("Research venues",()->{String q=query.getText().strip();if(q.isBlank()){d.action(()->{throw new IllegalArgumentException("Enter venue search requirements.");});return;}d.background("Researching Google Places venues",()->d.controller.researchVenues(id,q));}),Ui.button("Compare suitability",()->d.background("Comparing venues",()->d.controller.compareVenues(id)))),
   Ui.row(Ui.button("Configure venue requirements",()->new Ui.Form().field("Wheelchair entrance required (yes/no)",e.requiresWheelchairEntrance()?"yes":"no").field("Equipment (comma-separated)",String.join(", ",e.getRequiredEquipment())).show("Venue requirements",f->{Boolean required=GuiInputs.optionalBoolean(f.get("Wheelchair entrance required (yes/no)"));if(required==null)throw new IllegalArgumentException("Choose yes or no.");d.controller.configureVenueNeeds(id,required,GuiInputs.csv(f.get("Equipment (comma-separated)")));d.edited();})),Ui.button("Add manual venue",()->manual(id,e))),
   ResultViews.warnings("Search warnings",Ui.lines("Search warnings",d.session.venueWarnings(id)),names),Ui.inspectView(table,ResultViews::venue),Ui.card("Suitability comparisons",comparisons));
 }
 private static String fact(Evidence<?> value){return value.value()==null?"UNKNOWN":value.value()+" ["+value.status()+"]";}
 static String describe(VenueOption v){return v.name()+"\nID: "+v.id()+"\n"+v.address()+"\nSource: "+v.source()+"\nRetrieved: "+v.retrievedAt()+"\nCapacity: "+fact(v.capacity())+"\nCost: "+fact(v.rentalCost())+" "+v.currency()+"\nWheelchair entrance (not full accessibility): "+fact(v.wheelchairEntrance())+"\nEquipment: "+fact(v.equipment())+"\nAvailability: UNKNOWN\nProvider attributes: "+v.providerAttributes();}
 private void manual(String id,Event e){new Ui.Form().field("Venue name","").field("Address","").field("Evidence source","").option("Evidence status (PLANNER_CONFIRMED / ESTIMATED)",List.of(new DisplayNames.Option("ESTIMATED","Estimated"),new DisplayNames.Option("PLANNER_CONFIRMED","Planner confirmed")),"ESTIMATED").field("Capacity (blank = unknown)","").field("Rental cost (blank = unknown)","").field("Wheelchair entrance (yes/no/blank)","").field("Equipment (comma-separated; blank = unknown; none = empty)","").show("Add manual venue in "+e.getPlanningCurrency(),f->{
  String source=GuiInputs.required(f.get("Evidence source"));var status=GuiInputs.enumeration(Evidence.Status.class,f.get("Evidence status (PLANNER_CONFIRMED / ESTIMATED)"));
  if(status!=Evidence.Status.PLANNER_CONFIRMED&&status!=Evidence.Status.ESTIMATED)throw new IllegalArgumentException("Manual evidence must be PLANNER_CONFIRMED or ESTIMATED. Blank attributes remain UNKNOWN.");
  Integer capacity=f.get("Capacity (blank = unknown)").isBlank()?null:GuiInputs.integer(f.get("Capacity (blank = unknown)"));
  var cost=f.get("Rental cost (blank = unknown)").isBlank()?null:GuiInputs.money(f.get("Rental cost (blank = unknown)"));
  Boolean wheelchair=GuiInputs.optionalBoolean(f.get("Wheelchair entrance (yes/no/blank)"));
  String equipment=f.get("Equipment (comma-separated; blank = unknown; none = empty)");List<String> items=equipment.isBlank()?null:equipment.equalsIgnoreCase("none")?List.of():GuiInputs.csv(equipment);
  var venue=new VenueOption("manual:"+UUID.randomUUID(),f.get("Venue name"),f.get("Address"),"Manual planner entry: "+source,Instant.now(),Map.of(),evidence(capacity,status,source),evidence(cost,status,source),e.getPlanningCurrency(),evidence(wheelchair,status,source),evidence(items,status,source));
  d.controller.addVenue(id,venue);d.edited();
 });}
 private static <T> Evidence<T> evidence(T value,Evidence.Status status,String source){return value==null?Evidence.unknown():new Evidence<>(value,status,source);}
}
