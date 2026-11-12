package com.eventplanner.view.gui;

import com.eventplanner.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Form parsing only; planning validation belongs to the existing application layer. */
public final class GuiInputs {
 private GuiInputs() { }
 public static String required(String value) { return PlanningValues.text(value,"Required field"); }
 public static LocalDateTime date(String value) {
  try { return LocalDateTime.parse(value.strip()); }
  catch(DateTimeException ex) { throw new IllegalArgumentException("Use a valid local date/time, e.g. 2026-12-01T18:30."); }
 }
 public static int integer(String value) {
  try { int result=Integer.parseInt(value.strip()); if(result>=0) return result; } catch(NumberFormatException ignored) { }
  throw new IllegalArgumentException("Enter a non-negative whole number.");
 }
 public static BigDecimal money(String value) {
  try { var result=new BigDecimal(value.strip()); if(result.signum()>=0) return result; } catch(NumberFormatException ignored) { }
  throw new IllegalArgumentException("Enter a non-negative decimal amount.");
 }
 public static double number(String value) {
  double result=money(value).doubleValue(); if(!Double.isFinite(result)) throw new IllegalArgumentException("Number is too large."); return result;
 }
 public static List<String> csv(String value) { return value.isBlank()?List.of():Arrays.stream(value.split(",")).map(String::strip).map(GuiInputs::required).toList(); }
 public static List<TimeSlot> slots(String value) {
  if(value.isBlank()) return List.of();
  return Arrays.stream(value.strip().split("\\R")).map(line->{
   var pair=line.strip().split("/",-1); if(pair.length!=2) throw new IllegalArgumentException("Availability: one start/end ISO datetime pair per line.");
   return new TimeSlot(date(pair[0]),date(pair[1]));
  }).toList();
 }
 public static String slotsText(List<TimeSlot> values) { return String.join("\n",values.stream().map(v->v.start()+"/"+v.end()).toList()); }
 public static <T extends Enum<T>> T enumeration(Class<T> type,String value) {
  try { return Enum.valueOf(type,value.strip().toUpperCase(Locale.ROOT)); }
  catch(IllegalArgumentException ex) { throw new IllegalArgumentException("Choose one of "+Arrays.toString(type.getEnumConstants())); }
 }
 public static Boolean optionalBoolean(String value) {
  if(value.isBlank()) return null; if(value.equalsIgnoreCase("yes")) return true; if(value.equalsIgnoreCase("no")) return false;
  throw new IllegalArgumentException("Enter yes, no, or blank for unknown.");
 }
 /** Blank actuals mean unknown; zero is a recorded outcome. Lists use the backend's existing fields. */
 public static PostEventData postEventData(String attendance,String expenses,String completed,String unfinished,String organizer,String attendee,String problems){
  Integer count=null;
  if(!attendance.isBlank()){
   try{if(!attendance.strip().matches("[0-9]+"))throw new IllegalArgumentException();count=integer(attendance);}
   catch(IllegalArgumentException ex){throw new IllegalArgumentException("Actual attendance must be a non-negative whole number, or blank for unknown.");}
  }
  BigDecimal cost=null;
  if(!expenses.isBlank()){
   try{cost=money(expenses);}
   catch(IllegalArgumentException ex){throw new IllegalArgumentException("Actual expenses must be a non-negative decimal amount, or blank for unknown.");}
  }
  return new PostEventData(count,cost,csv(completed),csv(unfinished),feedback(organizer),feedback(attendee),feedback(problems));
 }
 private static List<String> feedback(String value){return value.lines().map(String::strip).filter(s->!s.isEmpty()).toList();}
}
