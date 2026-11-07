package com.eventplanner.support;

import com.eventplanner.ai.AIModel;
import com.eventplanner.model.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Fake responses exist only in test sources. Production always uses the SDK adapter. */
public final class PlanningFixtures {
    private static final ObjectMapper JSON = new ObjectMapper();
    private PlanningFixtures() { }

    public static Event event() {
        Event event = new Event("event-1", "Club gathering", "Social", "Campus",
                LocalDateTime.of(2026, 12, 1, 18, 30), 30, 500);
        event.addIdea("Alex", "An inclusive networking session");
        event.addIdea("Sam", "A quiet craft workshop");
        event.addConstraint("Step-free access; two volunteers; prioritize new members.");
        return event;
    }

    public static ObjectNode concepts(Event event) {
        ObjectNode root = JSON.createObjectNode();
        var alternatives = root.putArray("concepts");
        for (int i = 0; i < 2; i++) {
            ObjectNode concept = alternatives.addObject();
            concept.put("title", "Concept " + i).put("summary", "Combine making crafts with facilitated introductions.");
            concept.putArray("themes").add("Inclusive introductions");
            var ideas = concept.putArray("incorporatedIdeas");
            event.getOrganizerIdeas().forEach(idea -> ideas.add(idea.id()));
            concept.putArray("conflicts").add("Networking noise may disrupt quiet crafting.");
            concept.putArray("compromises").add("Separate quiet and conversation areas.");
            concept.putArray("uncertainties").add("Room availability is unknown.");
        }
        return root;
    }

    public static ObjectNode blueprints() {
        ObjectNode root = JSON.createObjectNode();
        var alternatives = root.putArray("blueprints");
        for (int i = 0; i < 2; i++) {
            ObjectNode blueprint = alternatives.addObject();
            blueprint.put("title", "Blueprint " + i).put("summary", "Proposed workshop plan.")
                    .put("estimatedBudget", 400).put("plannedAttendance", 30).put("requiredCapacity", 35);
            blueprint.putArray("agenda").add("18:30 welcome").add("18:45 workshop");
            blueprint.putArray("venueRequirements").add("Step-free room with a quiet area; availability unverified.");
            blueprint.putArray("cateringAssumptions").add("Water only; no vendor pricing verified.");
            blueprint.putArray("resources").add("Craft supplies and two volunteers");
            blueprint.putArray("checklist").add("Confirm accessible room and volunteers");
            blueprint.putArray("assumptions").add("Estimated allocation, not a quote");
            blueprint.putArray("feasibilityWarnings");
        }
        return root;
    }

    public static final class FakeAIModel implements AIModel {
        public final List<String> prompts = new ArrayList<>();
        private final Function<String, String> response;

        public FakeAIModel(Function<String, String> response) { this.response = response; }

        @Override public String generate(String prompt) {
            prompts.add(prompt);
            return response.apply(prompt);
        }
    }
}
