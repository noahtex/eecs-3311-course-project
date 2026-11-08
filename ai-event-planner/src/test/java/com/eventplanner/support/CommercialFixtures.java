package com.eventplanner.support;

import com.eventplanner.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class CommercialFixtures {
    private CommercialFixtures() { }
    public static BigDecimal amount(String value) { return new BigDecimal(value); }
    public static BudgetItem item(String id, String quantity, String cost, BudgetItem.PriceStatus status) {
        return new BudgetItem(id, "Catering", "Workshop refreshments", amount(quantity), cost == null ? null : amount(cost), status, "Synthetic test estimate/quote");
    }
    public static Budget budget(BudgetItem... items) {
        return new Budget("budget", "CAD", "budget", List.of(items), List.of(), List.of(), List.of("Test assumptions"));
    }
    public static <T> Evidence<T> confirmed(T value) { return new Evidence<>(value, Evidence.Status.PLANNER_CONFIRMED, "Synthetic planner source"); }
    public static VenueOption knownVenue(String id) {
        return new VenueOption(id, "Test-only Hall", "room, Toronto", "Synthetic planner source", Instant.EPOCH, Map.of(),
                confirmed(50), confirmed(amount("100")), "CAD", confirmed(true), confirmed(List.of("projector")));
    }
    public static VenueOption unknownVenue(String id) {
        return new VenueOption(id, "Test-only Unknown Hall", "Other area", "Synthetic test provider", Instant.EPOCH, Map.of(),
                Evidence.unknown(), Evidence.unknown(), "CAD", Evidence.unknown(), Evidence.unknown());
    }
    public static ToolResult providers() { return new ToolResult("Synthetic provider", List.of(unknownVenue("provider-1")), List.of("Prices and menus unknown")); }
    public static ObjectNode budgetJson() {
        var root = new ObjectMapper().createObjectNode();
        var items = root.putArray("items");
        items.addObject().put("category", "Catering").put("description", "Estimated vegetarian refreshments")
                .put("quantity", 30).put("unitCost", 5).put("priceStatus", "ESTIMATED");
        items.addObject().put("category", "Equipment").put("description", "Estimated supplies")
                .put("quantity", 1).put("unitCost", 50).put("priceStatus", "ESTIMATED");
        root.putArray("catering").addObject().putNull("providerId").put("recommendation", "Vegetarian buffet proposal; confirm allergens")
                .putArray("dietaryNotes").add("Confirm dietary suitability and cross-contact with provider");
        root.putArray("assumptions").add("Delivery and tax allocation need confirmation");
        return root;
    }
    public static ObjectNode comparisons(List<VenueOption> venues) {
        var root = new ObjectMapper().createObjectNode();
        var array = root.putArray("comparisons");
        venues.forEach(venue -> array.addObject().put("venueId", venue.id()).put("analysis", "Consider documented facts; confirm unknown availability")
                .putArray("tradeOffs").add("Confirm price, capacity and requirements against the source"));
        return root;
    }
}
