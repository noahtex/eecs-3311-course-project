package com.eventplanner;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import com.eventplanner.support.PlanningFixtures;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StandaloneCLITest {
    @TempDir Path dir;
    @Test void argumentsAndFiles() throws Exception {
        assertTrue(StandaloneCLI.parse(new String[]{"--help"}).help());
        assertThrows(IllegalArgumentException.class, () -> StandaloneCLI.parse(new String[]{"--input", "x"}));
        assertThrows(IllegalArgumentException.class, () -> StandaloneCLI.parse(new String[]{"--llm", "gemini", "--input", "x", "--input", "y"}));
        Path file = dir.resolve("task with spaces.txt");
        Files.writeString(file, "# test\naction=fuse-ideas\neventId=abc\n");
        assertEquals("abc", StandaloneCLI.readTask(file).eventId());
        Files.writeString(file, "action=fuse-ideas\neventId=x\neventId=y\n");
        assertThrows(IllegalArgumentException.class, () -> StandaloneCLI.readTask(file));
        assertEquals(3, StandaloneCLI.execute(new String[]{"--llm", "gemini", "--input", dir.resolve("missing").toString()}, System.in, System.out, System.err));
    }
    @Test void dispatchesRealFusionWithFakeAi() throws Exception {
        var service = new EventService();
        var event = PlanningFixtures.event();
        service.restoreEvents(java.util.List.of(event));
        var model = new PlanningFixtures.FakeAIModel(prompt -> PlanningFixtures.concepts(event).toString());
        var facade = new EventPlanningFacade(service, new EventPlanningAgent(model));
        Path file = dir.resolve("task.txt");
        Files.writeString(file, "action=fuse-ideas\neventId=" + event.getId() + "\n");
        var output = new ByteArrayOutputStream();
        int status = StandaloneCLI.execute(new String[]{"--llm", "gemini", "--input", file.toString()}, System.in,
                new PrintStream(output), System.err, () -> facade, () -> {});
        assertEquals(0, status);
        assertEquals(2, event.getConcepts().size());
        assertEquals(1, model.prompts.size());
        assertTrue(output.toString().contains("Saved 2 generated concepts"));
    }
}
