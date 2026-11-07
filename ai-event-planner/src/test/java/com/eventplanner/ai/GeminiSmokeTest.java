package com.eventplanner.ai;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import com.eventplanner.support.PlanningFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in only: sends synthetic event data to Gemini and consumes API quota. */
@EnabledIfSystemProperty(named = "gemini.smoke", matches = "true")
class GeminiSmokeTest {
    @Test
    @Timeout(300)
    void realGeminiFusionAndBlueprintWorkflow() {
        var service = new EventService();
        var event = PlanningFixtures.event();
        service.createEvent(event);
        var facade = new EventPlanningFacade(service, new EventPlanningAgent(new GeminiAdapter(new GeminiAPIClient())));
        var concepts = facade.fuseIdeas(event.getId());
        assertTrue(concepts.size() >= 2);
        var blueprints = facade.generateBlueprints(event.getId(), concepts.get(0).id(), "budget");
        assertTrue(blueprints.size() >= 2);
        assertEquals(blueprints, event.getBlueprints());
        assertTrue(blueprints.get(0).feasibilityWarnings().contains(EventPlanningAgent.UNVERIFIED));
    }
}
