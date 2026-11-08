package com.eventplanner.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record BudgetItem(String id, String category, String description, BigDecimal quantity,
        BigDecimal unitCost, PriceStatus priceStatus, String source) {
    public enum PriceStatus { CONFIRMED, ESTIMATED, UNKNOWN }
    public BudgetItem {
        id = PlanningValues.text(id, "Budget item ID");
        category = PlanningValues.text(category, "Category");
        description = PlanningValues.text(description, "Description");
        source = PlanningValues.text(source, "Price source/assumption");
        Objects.requireNonNull(priceStatus, "Price status is required.");
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        if (unitCost != null && unitCost.signum() < 0) throw new IllegalArgumentException("Unit cost cannot be negative.");
        if ((priceStatus == PriceStatus.UNKNOWN) != (unitCost == null))
            throw new IllegalArgumentException("Unknown prices must have no amount; known prices require an amount.");
        if (unitCost != null) unitCost = unitCost.setScale(2, RoundingMode.HALF_UP);
    }
    /** Null means unknown, never zero. */
    public BigDecimal total() { return unitCost == null ? null : quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP); }
}
