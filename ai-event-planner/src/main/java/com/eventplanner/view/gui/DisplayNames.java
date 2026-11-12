package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Immutable display lookups. IDs remain the values passed to application operations. */
final class DisplayNames {
 record Option(String id,String label){@Override public String toString(){return label;}}
 private final Map<String,String> members,speakers,tasks,concepts,venues,resources;
 private final Map<String,String> references=new LinkedHashMap<>();
 DisplayNames(Event event){
  members=labels(event.getPlanningTeam().getMembers(),User::getId,User::getName,"Member");
  speakers=labels(event.getSpeakers(),Speaker::id,Speaker::name,"Speaker");
  tasks=labels(event.getTasks(),Task::id,Task::title,"Task");
  concepts=labels(event.getConcepts(),EventConcept::id,EventConcept::title,"Concept");
  venues=labels(event.getVenueOptions(),VenueOption::id,VenueOption::name,"Venue");
  var resourceNames=new LinkedHashMap<String,String>();int ordinal=0;
  for(String id:event.getResourceAvailability().keySet())resourceNames.put(id,isUuid(id)?"Resource "+(++ordinal):id);
  resources=Map.copyOf(resourceNames);
  for(var map:List.of(members,speakers,tasks,concepts,venues,resources))references.putAll(map);
 }
 static <T> Map<String,String> labels(List<T> values,Function<T,String> id,Function<T,String> name,String kind){
  var result=new LinkedHashMap<String,String>();var counts=new HashMap<String,Integer>();
  for(T value:values){String title=name.apply(value);long same=values.stream().filter(v->Objects.equals(name.apply(v),title)).count();int n=counts.merge(title,1,Integer::sum);result.put(id.apply(value),title+(same>1?" ("+kind+" "+n+")":""));}
  return result;
 }
 static String date(LocalDateTime value){return value==null?"Not set":value.format(DateTimeFormatter.ofPattern(value.getSecond()!=0||value.getNano()!=0?"EEE, MMM d, uuuu · HH:mm:ss.SSSSSSSSS":"EEE, MMM d, uuuu · HH:mm"));}
 static String money(Object value,String currency){return value==null?"UNKNOWN":value+" "+currency;}
 String member(String id){return id==null?"Unassigned":members.getOrDefault(id,"Unknown member");}
 String speaker(String id){return id==null?"No speaker":speakers.getOrDefault(id,"Unknown speaker");}
 String task(String id){return tasks.getOrDefault(id,"Unknown task");}
 String concept(String id){return concepts.getOrDefault(id,"Unknown concept");}
 String venue(String id){return venues.getOrDefault(id,"Unknown venue");}
 String resource(String id){return resources.getOrDefault(id,isUuid(id)?"Unknown resource":id);}
 List<Option> memberOptions(String current){return options(members,current,"Unassigned","Unknown member");}
 List<Option> speakerOptions(String current){return options(speakers,current,"No speaker","Unknown speaker");}
 List<Option> resourceOptions(String current){return options(resources,current,null,"Unknown resource");}
 private static List<Option> options(Map<String,String> values,String current,String empty,String fallback){
  var options=new ArrayList<Option>();if(empty!=null)options.add(new Option("",empty));values.forEach((id,name)->options.add(new Option(id,name)));
  if(current!=null&&!current.isBlank()&&!values.containsKey(current))options.add(new Option(current,fallback));return options;
 }
 String readable(String text){
  String value=text;
  for(var e:references.entrySet().stream().sorted((a,b)->Integer.compare(b.getKey().length(),a.getKey().length())).toList())value=value.replaceAll("(?<![\\p{L}\\p{N}_-])"+Pattern.quote(e.getKey())+"(?![\\p{L}\\p{N}_-])",java.util.regex.Matcher.quoteReplacement(e.getValue()));
  return value.replaceAll("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}","Unresolved reference");
 }
 private static boolean isUuid(String value){return value!=null&&value.matches("(?i).*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*");}
}
