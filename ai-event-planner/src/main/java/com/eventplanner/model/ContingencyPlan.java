package com.eventplanner.model;
import java.util.List;
public record ContingencyPlan(String risk, String impact, String proposedResponse, List<String> resources, List<String> assumptions) {
 public ContingencyPlan { risk=PlanningValues.text(risk,"Risk"); impact=PlanningValues.text(impact,"Impact"); proposedResponse=PlanningValues.text(proposedResponse,"Response"); resources=List.copyOf(resources); assumptions=List.copyOf(assumptions); }
}
