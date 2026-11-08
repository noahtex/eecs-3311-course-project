package com.eventplanner.service;

import com.eventplanner.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Scores only explicitly reported/confirmed attributes. Estimates never earn confirmed-fit points. */
public final class VenueService {
    private boolean known(Evidence<?> fact) {
        return fact.status() == Evidence.Status.PROVIDER_REPORTED || fact.status() == Evidence.Status.PLANNER_CONFIRMED;
    }
    public VenueComparison evaluate(Event event, VenueOption venue) {
        int score = 0;
        List<String> warnings = new ArrayList<>();
        if (Set.of("CLOSED_TEMPORARILY", "CLOSED_PERMANENTLY").contains(venue.providerAttributes().getOrDefault("businessStatus", ""))) {
            score -= 100; warnings.add("Provider reports this business closed; confirm before considering it.");
        }
        if (!known(venue.capacity())) warnings.add("Capacity unknown or estimated; attendance fit is not established.");
        else if (venue.capacity().value() < event.getExpectedAttendance()) { score -= 100; warnings.add("Known capacity is below expected attendance."); }
        else score += 20;
        if (!venue.currency().equals(event.getPlanningCurrency())) warnings.add("Rental currency differs; no exchange conversion is assumed.");
        else if (!known(venue.rentalCost())) warnings.add("Rental cost unknown or estimated; affordability is not established.");
        else if (venue.rentalCost().value().compareTo(BigDecimal.valueOf(event.getTotalBudget())) > 0) {
            score -= 100; warnings.add("Rental cost alone exceeds the entire event budget.");
        } else { score += 20; warnings.add("Rental cost fits total spending limit but other event expenses still need funding."); }
        if (event.requiresWheelchairEntrance()) {
            if (!known(venue.wheelchairEntrance())) warnings.add("Required wheelchair entrance is unknown or estimated.");
            else if (!venue.wheelchairEntrance().value()) { score -= 100; warnings.add("Reported wheelchair entrance does not meet requirement."); }
            else score += 10;
        }
        if (!event.getRequiredEquipment().isEmpty()) {
            if (!known(venue.equipment())) warnings.add("Required equipment is unknown or estimated.");
            else for (String required : event.getRequiredEquipment()) {
                if (venue.equipment().value().stream().anyMatch(required::equalsIgnoreCase)) score += 5;
                else { score -= 20; warnings.add("Required equipment not listed: " + required); }
            }
        }
        if (venue.address().toLowerCase(Locale.ROOT).contains(event.getLocation().toLowerCase(Locale.ROOT))) score += 5;
        else warnings.add("Address does not textually match event location preference; confirm travel/location suitability.");
        warnings.add("Event-date availability is UNKNOWN; no booking has been made. Wheelchair entrance alone does not establish full accessibility.");
        if (!event.getConstraints().isEmpty()) warnings.add("Free-text constraints require manual review; AI interpretation is unverified.");
        return new VenueComparison(venue.id(), score, warnings, "Awaiting AI interpretation", List.of("Confirm all unknown attributes before selection."));
    }
    public List<VenueComparison> rank(List<VenueComparison> comparisons) {
        return comparisons.stream().sorted(Comparator.comparingInt(VenueComparison::knownFitScore).reversed()
                .thenComparing(VenueComparison::venueId)).toList();
    }
}
