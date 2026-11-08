package com.eventplanner.service;

import com.eventplanner.model.*;
import java.math.BigDecimal;
import java.util.*;

public final class BudgetService {
    /** Known subtotal includes estimates; unknown items are separately warned, not valued at zero. */
    public BigDecimal totalExpenses(Budget budget) {
        return budget.items().stream().map(BudgetItem::total).filter(Objects::nonNull).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
    public BigDecimal remainingBudget(Event event, Budget budget) {
        return BigDecimal.valueOf(event.getTotalBudget()).subtract(totalExpenses(budget));
    }
    public BigDecimal shortfall(Event event, Budget budget) { return remainingBudget(event, budget).negate().max(BigDecimal.ZERO); }
    public List<String> validateBudget(Event event, Budget budget) {
        List<String> issues = new ArrayList<>();
        if (!budget.currency().equals(event.getPlanningCurrency())) issues.add("Currency differs from the event spending limit; no conversion is assumed.");
        if (remainingBudget(event, budget).signum() < 0) issues.add("Over budget: known subtotal exceeds spending limit by " + shortfall(event, budget) + ".");
        for (BudgetItem item : budget.items()) {
            if (item.priceStatus() == BudgetItem.PriceStatus.UNKNOWN) issues.add("Item " + item.id() + ": price UNKNOWN; subtotal and remaining balance are incomplete.");
            if (item.priceStatus() == BudgetItem.PriceStatus.ESTIMATED) issues.add("Item " + item.id() + ": estimated price; obtain a quote.");
        }
        issues.add("Catering menus, dietary suitability, allergens, delivery, taxes and fees require provider confirmation unless explicitly documented.");
        return List.copyOf(issues);
    }
    public Budget updateAllocation(Budget budget, BudgetItem replacement) {
        if (budget.items().stream().noneMatch(item -> item.id().equals(replacement.id()))) throw new IllegalArgumentException("Unknown budget item ID.");
        return new Budget(budget.id(), budget.currency(), budget.priority(), budget.items().stream()
                .map(item -> item.id().equals(replacement.id()) ? replacement : item).toList(), budget.catering(), budget.providers(), budget.assumptions());
    }
    public void saveBudget(Event event, Budget budget) {
        if (!budget.currency().equals(event.getPlanningCurrency())) throw new IllegalArgumentException("Budget currency must match event currency.");
        if (remainingBudget(event, budget).signum() < 0) throw new IllegalArgumentException("Cannot approve an over-budget plan. Edit allocations; the event spending limit is unchanged.");
        if (budget.items().stream().anyMatch(item -> item.priceStatus() == BudgetItem.PriceStatus.UNKNOWN))
            throw new IllegalArgumentException("Resolve unknown budget amounts before approval (estimates may be explicitly approved).");
        event.storeBudget(budget);
    }
}
