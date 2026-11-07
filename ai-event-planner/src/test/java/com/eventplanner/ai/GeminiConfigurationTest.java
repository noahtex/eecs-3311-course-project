package com.eventplanner.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeminiConfigurationTest {
    @TempDir Path directory;

    @Test
    void loadsQuotedLocalValuesWithEnvironmentPrecedenceWithoutPrintingSecrets() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "# local test only\nexport GEMINI_API_KEY='fake-local-key'\nGEMINI_MODEL=\"test-local-model\"\n");
        var local = GeminiConfiguration.load(file, Map.of());
        assertEquals("fake-local-key", local.apiKey());
        assertEquals("test-local-model", local.model());
        var environment = GeminiConfiguration.load(file, Map.of("GEMINI_API_KEY", "fake-env-key", "GEMINI_MODEL", "test-env-model"));
        assertEquals("fake-env-key", environment.apiKey());
        assertEquals("test-env-model", environment.model());
        assertFalse(environment.toString().contains("fake-env-key"));
    }

    @Test
    void rejectsMissingKeyPlaceholderAndBlankModel() {
        Path missing = directory.resolve("missing");
        assertThrows(PlanningException.class, () -> GeminiConfiguration.load(missing, Map.of()));
        assertThrows(PlanningException.class, () -> GeminiConfiguration.load(missing, Map.of("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY_HERE")));
        assertThrows(PlanningException.class, () -> GeminiConfiguration.load(missing, Map.of("GEMINI_API_KEY", "fake", "GEMINI_MODEL", " ")));
        assertEquals("gemini-flash-latest", GeminiConfiguration.load(missing, Map.of("GEMINI_API_KEY", "fake")).model());
    }

    @Test
    void adapterDelegatesToApiClientAdaptee() {
        var adapter = new GeminiAdapter(new GeminiAPIClient() {
            @Override public String generateContent(String prompt) { return "translated: " + prompt; }
        });
        assertEquals("translated: request", adapter.generate("request"));
    }
}
