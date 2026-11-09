package com.eventplanner.model;
import java.math.BigDecimal;
import java.util.List;
public record PostEventReport(String executiveSummary, Integer attendanceDifference, BigDecimal expenseDifference,
 int completedTasks, int unfinishedTasks, Double completionRate, List<String> successes, List<String> issues,
 List<String> lessonsLearned, List<String> recommendations, List<String> missingInformation, PostEventData outcomes,
 int expectedAttendance, BigDecimal plannedBudget) {
 public PostEventReport { successes=List.copyOf(successes); issues=List.copyOf(issues); lessonsLearned=List.copyOf(lessonsLearned); recommendations=List.copyOf(recommendations); missingInformation=List.copyOf(missingInformation); }
}
