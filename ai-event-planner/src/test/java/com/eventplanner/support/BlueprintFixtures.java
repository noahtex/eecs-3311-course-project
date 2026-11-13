package com.eventplanner.support;

import com.eventplanner.model.*;
import java.util.List;

/** Distinct alternatives make accidental mixing visible in execution prompts. */
public final class BlueprintFixtures {
    private BlueprintFixtures() { }

    public static List<EventBlueprint> addAlternatives(Event event) {
        if (event.getOrganizerIdeas().isEmpty()) event.addIdea("Planner", "Choose an event format");
        var concept = new EventConcept(event.getId()+"-concept", "Event concept", "Compare formats",
                List.of("Community"), List.of(event.getOrganizerIdeas().getFirst().id()),
                List.of(), List.of(), List.of("Unverified proposal"));
        event.addConcepts(List.of(concept));
        var alternatives = List.of(blueprint(event, concept, "Quiet workshop"), blueprint(event, concept, "Loud concert"));
        event.addBlueprints(alternatives);
        return alternatives;
    }

    private static EventBlueprint blueprint(Event event, EventConcept concept, String title) {
        return new EventBlueprint(event.getId()+"-"+title.replace(' ', '-'), concept.id(), "community", title,
                title+" summary", List.of(title+" agenda"), List.of(title+" venue"),
                List.of(title+" catering"), List.of(title+" resources"), List.of(title+" checklist"),
                List.of("Costs estimated"), List.of("Availability unknown"), 400, 30, 35);
    }
}
