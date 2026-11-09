package com.eventplanner.model;
import java.util.Map;
public record PlanChange(Map<String,String> before, Map<String,String> changes) {
 public PlanChange { before=Map.copyOf(before); changes=Map.copyOf(changes); if(changes.isEmpty()) throw new IllegalArgumentException("No definite changes. Clarify your request."); }
}
