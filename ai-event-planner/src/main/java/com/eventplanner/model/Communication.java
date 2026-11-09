package com.eventplanner.model;
import java.util.Map;
public record Communication(String id, Channel channel, String tone, String text, boolean approved, Map<String,String> sourceFacts) {
 public enum Channel { EMAIL, INSTAGRAM, WHATSAPP }
 public Communication { text=PlanningValues.text(text,"Draft text"); tone=PlanningValues.text(tone,"Tone"); sourceFacts=Map.copyOf(sourceFacts); }
 public Communication edit(String value) { return new Communication(id,channel,tone,value,false,sourceFacts); }
 public Communication approve() { return new Communication(id,channel,tone,text,true,sourceFacts); }
}
