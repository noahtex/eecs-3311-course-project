package com.eventplanner.strategy;

import com.eventplanner.model.Event;

public final class BudgetPriorityStrategy implements PlanningStrategy {
    public String name() { return "budget"; }
    public String buildGuidance(Event event) {
        return "Minimize projected spending under the budget ceiling " + event.getTotalBudget()
                + "; reuse resources, keep a contingency reserve, and prefer simple catering. Explain cost trade-offs.";
    }
}
