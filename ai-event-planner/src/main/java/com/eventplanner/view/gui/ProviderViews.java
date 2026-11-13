package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

/** Presents recorded provider facts separately from AI interpretation. Never infers contact data. */
final class ProviderViews {
 private ProviderViews() { }
 static boolean safeWebsite(String value) {
  if(value==null)return false;
  try{var uri=URI.create(value);return ("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))
   &&uri.getHost()!=null&&uri.getRawUserInfo()==null;}catch(IllegalArgumentException ex){return false;}
 }
 private static String attribute(VenueOption value,String key){return value.providerAttributes().getOrDefault(key,"").strip();}
 private static Node copyable(String label,String text){var field=new TextField(text);field.setEditable(false);field.setAccessibleText(label);field.setMaxWidth(Double.MAX_VALUE);return Ui.field(label,field);}
 private static VBox identity(VenueOption value,Consumer<String> browser){
  var box=new VBox(10,Ui.section(value.name(),value.address()),Ui.note("Source: "+value.source()+" · Recorded: "+value.retrievedAt()),
   Ui.note(value.source().equals("Google Places")?"Google-reported listing information; not independently verified. Contact the provider to confirm suitability.":"Recorded source information; confirm the evidence and current details with the provider."));
  String phone=attribute(value,"nationalPhoneNumber");if(phone.isEmpty())phone=attribute(value,"internationalPhoneNumber");
  if(!phone.isEmpty())box.getChildren().add(copyable("Contact phone — source-reported",phone));
  String website=attribute(value,"websiteUri");
  if(!website.isEmpty()){
   box.getChildren().add(copyable("Website — source-reported",website));
   if(safeWebsite(website)){var link=new Hyperlink("Open provider website");link.setOnAction(e->browser.accept(website));box.getChildren().add(link);}
   else box.getChildren().add(Ui.note("This website address cannot be opened safely. Check it with the provider."));
  }
  // Places does not return email. Only explicit, attributed manual records can supply it here.
  String email=attribute(value,"email");
  boolean manualEmail=!email.isEmpty()&&value.source().startsWith("Manual planner entry:");
  if(manualEmail)box.getChildren().add(copyable("Email — manually recorded; source: "+value.source(),email));
  if(phone.isEmpty()&&website.isEmpty()&&!manualEmail)
   box.getChildren().add(Ui.note("No contact details recorded. Contact information requires confirmation."));
  return box;
 }
 static Node catering(VenueOption provider,Consumer<String> browser){
  var box=identity(provider,browser);
  String category=category(provider);
  if(!category.isEmpty())box.getChildren().add(Ui.section("Provider category — source-reported",category));
  box.getChildren().add(Ui.note("A real listing does not verify catering service, menu, dietary suitability or current pricing."));
  box.getChildren().add(Ui.technical(provider.toString()));return box;
 }
 private static String category(VenueOption provider){
  String types=attribute(provider,"types");if(types.isEmpty())return "";
  try{var values=new ObjectMapper().readTree(types);if(!values.isArray())return "";
   var labels=new ArrayList<String>();for(var value:values)if(value.isTextual()&&!Set.of("point_of_interest","establishment").contains(value.asText()))labels.add(value.asText().replace('_',' '));
   return String.join(", ",labels);
  }catch(java.io.IOException ex){return "";}
 }
 static Node venue(VenueOption venue,Consumer<String> browser){
  var box=new VBox(14,Ui.badge("CANDIDATE · NOT A BOOKING"),identity(venue,browser));
  var details=new VBox(10);var missing=new ArrayList<String>();
  fact(details,missing,"Capacity",venue.capacity(),"");
  fact(details,missing,"Rental price",venue.rentalCost()," "+venue.currency());
  fact(details,missing,"Wheelchair entrance",venue.wheelchairEntrance(),"");
  fact(details,missing,"Equipment",venue.equipment(),"");
  missing.add("event availability");
  String warning="Requires direct confirmation: "+String.join(", ",missing)+".";
  box.getChildren().add(Ui.note(warning));details.getChildren().add(Ui.note(warning));
  details.getChildren().add(Ui.note("Wheelchair entrance information is not a full accessibility assessment. Provider-reported and estimated details are not independently verified facts. No booking is confirmed."));
  var expanded=new TitledPane("Additional Details & Verification",details);expanded.setExpanded(false);box.getChildren().add(expanded);
  box.getChildren().add(Ui.technical(venue.toString()));return box;
 }
 static Node venue(VenueOption venue,Consumer<String> browser,Optional<VenueComparison> comparison){
  var box=new VBox(14,venue(venue,browser));
  if(comparison.isPresent()){
   String summary=comparison.get().analysis();
   if(summary.length()>400)summary=summary.substring(0,400)+"…";
   box.getChildren().add(Ui.section("Suitability summary — AI interpretation",summary));
   box.getChildren().add(Ui.note("See Suitability comparisons below for the full analysis, all warnings and trade-offs."));
  }else box.getChildren().add(Ui.note("Suitability has not been assessed. Compare candidates against this event's requirements."));
  return box;
 }
 private static void fact(VBox details,List<String> missing,String name,Evidence<?> evidence,String suffix){
  if(evidence.value()==null){missing.add(name.toLowerCase(Locale.ROOT));return;}
  String value=evidence.value() instanceof List<?> items?(items.isEmpty()?"None recorded":String.join(", ",items.stream().map(String::valueOf).toList())):String.valueOf(evidence.value());
  details.getChildren().add(Ui.section(name,value+suffix+" · "+evidence.status()+"\nSource: "+evidence.source()));
 }
}
