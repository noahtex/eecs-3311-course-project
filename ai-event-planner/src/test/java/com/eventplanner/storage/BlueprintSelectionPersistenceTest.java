package com.eventplanner.storage;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static com.eventplanner.support.BlueprintFixtures.addAlternatives;
import static com.eventplanner.support.ExecutionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class BlueprintSelectionPersistenceTest {
    @TempDir Path directory;

    private EventPlanningFacade open(EventService events, PlanRepository repository) {
        return new EventPlanningFacade(events, new EventPlanningAgent(p -> { throw new AssertionError("No AI"); }),
                q -> { throw new AssertionError("No Places"); }, q -> { throw new AssertionError("No catering"); }, repository);
    }
    private EventPlanningFacade reopen() { return open(new EventService(), new JsonPlanRepository(directory)); }
    private EventPlanningFacade seed(Event event) {
        var events = new EventService(); events.createEvent(event);
        return open(events, new JsonPlanRepository(directory));
    }

    @Test void pendingSelectionAndSeparateApprovedReferenceSurviveRestartAndSwitchPreservesPlans() {
        Event event = event(); var alternatives = addAlternatives(event);
        event.storeTasks(List.of(task("retained", "alex"))); event.storeSchedule(schedule());
        try (var facade = seed(event)) { facade.selectBlueprint(event.getId(), alternatives.getFirst().id()); }
        try (var facade = reopen()) {
            assertEquals(alternatives.getFirst().id(), facade.viewEvent(event.getId()).getSelectedBlueprintId());
            assertNull(facade.viewEvent(event.getId()).getApprovedBlueprintId());
            facade.approveBlueprint(event.getId()); facade.selectBlueprint(event.getId(), alternatives.getLast().id());
        }
        try (var facade = reopen()) {
            var restored = facade.viewEvent(event.getId());
            assertEquals(alternatives.getFirst(), restored.getApprovedBlueprint());
            assertEquals(alternatives.getLast().id(), restored.getSelectedBlueprintId());
            assertEquals(alternatives, restored.getBlueprints());
            facade.approveBlueprint(event.getId());
        }
        try (var facade = reopen()) {
            var restored = facade.viewEvent(event.getId());
            assertEquals(alternatives.getLast(), restored.getApprovedBlueprint());
            assertEquals(event.getTasks(), restored.getTasks()); assertEquals(event.getSchedule(), restored.getSchedule());
            assertTrue(restored.getReviewWarnings().keySet().containsAll(List.of("tasks", "schedule")));
        }
    }

    @Test void legacyJsonWithoutSelectionFieldsLoadsWithoutRewritingAndUpgradesOnNextSave() throws Exception {
        Event event = event(); var alternatives = addAlternatives(event);
        event.storeTasks(List.of(task("retained", "alex"))); event.storeSchedule(schedule());
        event.storePostEventData(new PostEventData(25, new java.math.BigDecimal("95.50"),List.of(),List.of(),List.of(),List.of(),List.of()));
        event.storeReport(new PostEventReport("Recorded outcomes",-5,new java.math.BigDecimal("-4.50"),0,0,0.0,
                List.of(),List.of(),List.of(),List.of(),List.of(),event.getPostEventData(),30,new java.math.BigDecimal("100.00")));
        try (var facade = seed(event)) { facade.addConstraint(event.getId(), "Keep original saved data"); }
        Path file = directory.resolve("planit.json"); var mapper = StorageJson.mapper();
        var document = mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .without(com.fasterxml.jackson.databind.cfg.JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES).readTree(Files.readAllBytes(file));
        var saved = (com.fasterxml.jackson.databind.node.ObjectNode) document.path("events").get(0);
        saved.remove(List.of("selectedBlueprintId", "approvedBlueprintId"));
        byte[] legacy = mapper.writeValueAsBytes(document); Files.write(file, legacy);
        try (var facade = reopen()) {
            var restored = facade.viewEvent(event.getId());
            assertNull(restored.getSelectedBlueprintId()); assertNull(restored.getApprovedBlueprintId());
            assertEquals(alternatives, restored.getBlueprints());
            assertEquals(event.getTasks(), restored.getTasks()); assertEquals(event.getSchedule(), restored.getSchedule());
            assertEquals(PlanDataMapper.capture(event),PlanDataMapper.capture(restored));
            assertArrayEquals(legacy, Files.readAllBytes(file));
            // Existing approved work remains usable; generating from alternatives now requires a deliberate choice.
            assertThrows(IllegalArgumentException.class, () -> facade.generateTasks(event.getId()));
            assertArrayEquals(legacy, Files.readAllBytes(file));
            facade.selectBlueprint(event.getId(), alternatives.getFirst().id()); facade.approveBlueprint(event.getId());
        }
        try (var facade = reopen()) {
            assertEquals(alternatives.getFirst(), facade.viewEvent(event.getId()).getApprovedBlueprint());
            assertEquals("Keep original saved data", facade.listConstraints(event.getId()).getFirst());
        }
        assertTrue(mapper.readTree(Files.readAllBytes(file)).path("events").get(0).has("approvedBlueprintId"));
    }

    @Test void invalidPersistedReferencesAndMissingOriginalFieldsRemainProtected() throws Exception {
        Event event = event(); var alternatives = addAlternatives(event);
        try (var facade = seed(event)) { facade.selectBlueprint(event.getId(), alternatives.getFirst().id()); facade.approveBlueprint(event.getId()); }
        Path file = directory.resolve("planit.json"); byte[] valid = Files.readAllBytes(file); var mapper = StorageJson.mapper();
        for (String field : List.of("selectedBlueprintId", "approvedBlueprintId", "name")) {
            var document = mapper.readTree(valid); var saved = (com.fasterxml.jackson.databind.node.ObjectNode) document.path("events").get(0);
            if (field.equals("name")) saved.remove(field); else saved.put(field, "foreign-blueprint");
            byte[] invalid = mapper.writeValueAsBytes(document); Files.write(file, invalid);
            assertThrows(StorageException.class, this::reopen);
            assertArrayEquals(invalid, Files.readAllBytes(file));
        }
    }

    @Test void failedApprovalSaveRollsBackSelectionApprovalAndWarnings() {
        Event event = event(); var alternatives = addAlternatives(event); var events = new EventService(); events.createEvent(event);
        event.storeTasks(List.of(task("retained", "alex"))); var fail = new AtomicBoolean();
        var repository = new JsonPlanRepository(directory) {
            @Override public void save(PlanData data) { if (fail.get()) throw new StorageException("Injected save failure"); super.save(data); }
        };
        try (var facade = open(events, repository)) {
            facade.selectBlueprint(event.getId(), alternatives.getFirst().id()); facade.approveBlueprint(event.getId());
            facade.selectBlueprint(event.getId(), alternatives.getLast().id());
            var before = PlanDataMapper.capture(facade.viewEvent(event.getId())); fail.set(true);
            assertThrows(StorageException.class, () -> facade.approveBlueprint(event.getId()));
            assertEquals(before, PlanDataMapper.capture(facade.viewEvent(event.getId())));
        }
        try (var facade = reopen()) {
            assertEquals(alternatives.getFirst(), facade.viewEvent(event.getId()).getApprovedBlueprint());
            assertEquals(alternatives.getLast().id(), facade.viewEvent(event.getId()).getSelectedBlueprintId());
        }
    }
}
