package com.eventplanner.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record VenueOption(String id, String name, String address, String source, Instant retrievedAt,
        Map<String, String> providerAttributes, Evidence<Integer> capacity, Evidence<BigDecimal> rentalCost,
        String currency, Evidence<Boolean> wheelchairEntrance, Evidence<List<String>> equipment) {
    public VenueOption {
        id = PlanningValues.text(id, "Venue ID");
        name = PlanningValues.text(name, "Venue name");
        address = PlanningValues.text(address, "Venue address");
        source = PlanningValues.text(source, "Venue source");
        Objects.requireNonNull(retrievedAt, "Retrieval time is required.");
        providerAttributes = Map.copyOf(providerAttributes);
        Objects.requireNonNull(capacity); Objects.requireNonNull(rentalCost);
        Objects.requireNonNull(wheelchairEntrance); Objects.requireNonNull(equipment);
        if (capacity.value() != null && capacity.value() <= 0) throw new IllegalArgumentException("Capacity must be positive.");
        if (rentalCost.value() != null && rentalCost.value().signum() < 0) throw new IllegalArgumentException("Rental cost cannot be negative.");
        currency = PlanningValues.text(currency, "Venue currency");
        java.util.Currency.getInstance(currency);
        if (equipment.value() != null) equipment = new Evidence<>(PlanningValues.texts(equipment.value(), "Equipment", true), equipment.status(), equipment.source());
    }
}
