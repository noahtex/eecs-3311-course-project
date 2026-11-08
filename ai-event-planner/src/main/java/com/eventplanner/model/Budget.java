package com.eventplanner.model;

import java.util.List;

public record Budget(String id, String currency, String priority, List<BudgetItem> items,
        List<CateringOption> catering, List<VenueOption> providers, List<String> assumptions) {
    public Budget {
        id = PlanningValues.text(id, "Budget ID");
        currency = PlanningValues.text(currency, "Currency");
        if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Use a three-letter currency code.");
        java.util.Currency.getInstance(currency);
        priority = PlanningValues.text(priority, "Priority");
        items = List.copyOf(items);
        catering = List.copyOf(catering);
        providers = List.copyOf(providers);
        assumptions = PlanningValues.texts(assumptions, "Budget assumptions", false);
        if (items.isEmpty()) throw new IllegalArgumentException("Budget items are required.");
        if (items.stream().map(BudgetItem::id).distinct().count() != items.size())
            throw new IllegalArgumentException("Budget item IDs must be unique.");
        for (CateringOption option : catering) {
            if (option.providerId() != null && providers.stream().noneMatch(provider -> provider.id().equals(option.providerId())))
                throw new IllegalArgumentException("Catering must reference a retrieved provider or null.");
        }
    }
}
