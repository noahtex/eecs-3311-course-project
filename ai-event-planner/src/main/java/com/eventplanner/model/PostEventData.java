package com.eventplanner.model;
import java.math.BigDecimal;
import java.util.List;
public record PostEventData(Integer actualAttendance, BigDecimal actualExpenses, List<String> completedTaskIds,
 List<String> unfinishedTaskIds, List<String> organizerFeedback, List<String> attendeeFeedback, List<String> problems) {
 public PostEventData {
 if(actualAttendance!=null && actualAttendance<0 || actualExpenses!=null && actualExpenses.signum()<0) throw new IllegalArgumentException("Actual values cannot be negative.");
 completedTaskIds=List.copyOf(completedTaskIds); unfinishedTaskIds=List.copyOf(unfinishedTaskIds);
 if(completedTaskIds.stream().distinct().count()!=completedTaskIds.size() || unfinishedTaskIds.stream().distinct().count()!=unfinishedTaskIds.size() || completedTaskIds.stream().anyMatch(unfinishedTaskIds::contains)) throw new IllegalArgumentException("Task outcomes must be unique and disjoint.");
 organizerFeedback=List.copyOf(organizerFeedback); attendeeFeedback=List.copyOf(attendeeFeedback); problems=List.copyOf(problems);
 }
}
