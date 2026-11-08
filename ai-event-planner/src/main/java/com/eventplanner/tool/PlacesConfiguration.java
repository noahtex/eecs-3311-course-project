package com.eventplanner.tool;

import com.eventplanner.ai.GeminiConfiguration;
import com.eventplanner.ai.PlanningException;
import java.nio.file.Path;
import java.util.Map;

public final class PlacesConfiguration {
    private PlacesConfiguration() { }
    public static String loadApiKey() { return loadApiKey(Path.of(".env").toAbsolutePath().normalize(), System.getenv()); }
    public static String loadApiKey(Path path, Map<String, String> environment) {
        var local = GeminiConfiguration.readLocalValues(path);
        String key = environment.getOrDefault("GOOGLE_PLACES_API_KEY", local.getOrDefault("GOOGLE_PLACES_API_KEY", "")).strip();
        if (key.isBlank() || key.equals("YOUR_GOOGLE_PLACES_API_KEY_HERE"))
            throw new PlanningException("Configure GOOGLE_PLACES_API_KEY in the environment or working-directory .env, enable Places API (New) and billing; or use venues add for manual entry.");
        return key;
    }
}
