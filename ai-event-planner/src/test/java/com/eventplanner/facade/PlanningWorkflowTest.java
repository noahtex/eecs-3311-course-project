package com.eventplanner.facade;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.service.EventService;
import com.eventplanner.support.PlanningFixtures;
import com.eventplanner.support.PlanningFixtures.FakeAIModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class PlanningWorkflowTest {
    private final java.util.List<EventPlanningFacade> facades = new java.util.ArrayList<>();
    private EventPlanningFacade own(EventPlanningFacade facade) {
        facades.add(facade);
        return facade;
    }
    @AfterEach void closeFacades() {
        // Each facade owns its agent, tools and repository, but not the shared EventService.
        facades.forEach(EventPlanningFacade::close);
    }

    @Test
    void controllerFacadeStoreResultsInSameEventAndEnforceOwnership() {
        var event = PlanningFixtures.event();
        var service = new EventService();
        service.createEvent(event);
        var model = new FakeAIModel(prompt -> prompt.contains("F01:") ? PlanningFixtures.concepts(event).toString()
                : PlanningFixtures.blueprints().toString());
        var controller = new EventPlanningController(own(new EventPlanningFacade(service, new EventPlanningAgent(model))));
        var idea = controller.addIdea(event.getId(), "Lee", "An accessible venue");
        assertTrue(controller.listIdeas(event.getId()).contains(idea));
        controller.addConstraint(event.getId(), "No alcohol");
        assertTrue(controller.listConstraints(event.getId()).contains("No alcohol"));
        var concepts = controller.fuseIdeas(event.getId());
        var blueprints = controller.generateBlueprints(event.getId(), concepts.get(0).id(), "community");
        assertEquals(concepts, event.getConcepts());
        assertEquals(concepts, controller.listConcepts(event.getId()));
        assertEquals(blueprints, event.getBlueprints());
        assertEquals(blueprints, controller.listBlueprints(event.getId()));
        assertSame(blueprints.get(0), controller.viewBlueprint(event.getId(), blueprints.get(0).id()));
        var other = controller.createEvent("Other", "Club", "Campus", event.getDateTime(), 30, 500);
        int calls = model.prompts.size();
        assertThrows(IllegalArgumentException.class, () -> controller.generateBlueprints(other.getId(), concepts.get(0).id(), "budget"));
        assertThrows(IllegalArgumentException.class, () -> controller.viewBlueprint(other.getId(), blueprints.get(0).id()));
        assertThrows(IllegalArgumentException.class, () -> controller.generateBlueprints(event.getId(), concepts.get(0).id(), "invalid"));
        assertThrows(IllegalArgumentException.class, () -> controller.fuseIdeas("missing"));
        assertThrows(IllegalArgumentException.class, () -> controller.addIdea(event.getId(), "", "Idea"));
        assertThrows(IllegalArgumentException.class, () -> controller.addIdea(event.getId(), "Lee", ""));
        assertThrows(IllegalArgumentException.class, () -> controller.addConstraint(event.getId(), ""));
        assertThrows(IllegalArgumentException.class, () -> controller.viewBlueprint(event.getId(), ""));
        assertEquals(calls, model.prompts.size());
        assertThrows(UnsupportedOperationException.class, () -> event.getBlueprints().clear());
    }

    @Test
    void invalidGenerationLeavesExistingConceptsAndBlueprintsUntouched() {
        var event = PlanningFixtures.event();
        var service = new EventService();
        service.createEvent(event);
        var good = new EventPlanningAgent(prompt -> prompt.contains("F01:") ? PlanningFixtures.concepts(event).toString()
                : PlanningFixtures.blueprints().toString());
        var facade = own(new EventPlanningFacade(service, good));
        var concepts = facade.fuseIdeas(event.getId());
        var blueprints = facade.generateBlueprints(event.getId(), concepts.get(0).id(), "budget");
        var badConcepts = PlanningFixtures.concepts(event);
        ((com.fasterxml.jackson.databind.node.ObjectNode) badConcepts.path("concepts").get(1)).remove("summary");
        var badBlueprints = PlanningFixtures.blueprints();
        ((com.fasterxml.jackson.databind.node.ObjectNode) badBlueprints.path("blueprints").get(1)).remove("agenda");
        var broken = own(new EventPlanningFacade(service, new EventPlanningAgent(prompt -> prompt.contains("F01:")
                ? badConcepts.toString() : badBlueprints.toString())));
        assertThrows(PlanningException.class, () -> broken.fuseIdeas(event.getId()));
        assertThrows(PlanningException.class, () -> broken.generateBlueprints(event.getId(), concepts.get(0).id(), "budget"));
        assertEquals(concepts, event.getConcepts());
        assertEquals(blueprints, event.getBlueprints());
    }

    @Test
    void legacyIdeaMethodsRemainCompatible() {
        var event = PlanningFixtures.event();
        event.addIdea("Legacy idea");
        event.addIdea(null);
        event.addIdea(" ");
        assertEquals(3, event.getIdeas().size());
        assertEquals("Legacy idea", event.getIdeas().get(2));
        assertEquals("Unspecified organizer", event.getOrganizerIdeas().get(2).organizer());
    }
}
