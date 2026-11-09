package com.eventplanner.model;
import java.math.BigDecimal;
import java.util.List;
public record ScenarioResult(PlanChange proposedChanges, Event snapshot, List<String> affectedComponents,
 BigDecimal budgetLimitDifference, BigDecimal estimatedExpenseDifference, List<String> implications, List<String> warnings, List<String> recommendations) {
 public ScenarioResult { affectedComponents=List.copyOf(affectedComponents); implications=List.copyOf(implications); warnings=List.copyOf(warnings); recommendations=List.copyOf(recommendations); }
}
