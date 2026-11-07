package com.eventplanner.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Minimal local KEY=value loader; environment values take precedence, no shell expansion. */
public final class GeminiConfiguration {
    private final String apiKey;
    private final String model;

    private GeminiConfiguration(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public static GeminiConfiguration load() {
        return load(Path.of(".env").toAbsolutePath().normalize(), System.getenv());
    }

    public static GeminiConfiguration load(Path path, Map<String, String> environment) {
        Map<String, String> local = new HashMap<>();
        if (Files.exists(path)) {
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    line = line.strip();
                    if (line.startsWith("\uFEFF")) line = line.substring(1).strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (line.startsWith("export ")) line = line.substring(7).strip();
                    int separator = line.indexOf('=');
                    if (separator < 1) continue;
                    String key = line.substring(0, separator).strip();
                    String value = line.substring(separator + 1).strip();
                    if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    local.put(key, value);
                }
            } catch (IOException ex) {
                throw new PlanningException("Cannot read local .env configuration. Check file access.");
            }
        }
        String apiKey = environment.getOrDefault("GEMINI_API_KEY", local.getOrDefault("GEMINI_API_KEY", "")).strip();
        String model = environment.getOrDefault("GEMINI_MODEL", local.getOrDefault("GEMINI_MODEL", "gemini-flash-latest")).strip();
        if (apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new PlanningException("Set GEMINI_API_KEY in the environment or local .env to use AI commands.");
        }
        if (model.isBlank()) throw new PlanningException("Set GEMINI_MODEL to an available Gemini model name.");
        System.getLogger(GeminiConfiguration.class.getName()).log(System.Logger.Level.DEBUG,
                "Gemini configuration: cwd={0}; envFile={1}; fileExists={2}; GEMINI_API_KEY source={3}, nonblank=true; GEMINI_MODEL source={4}",
                Path.of("").toAbsolutePath().normalize(), path.toAbsolutePath().normalize(), Files.exists(path),
                environment.containsKey("GEMINI_API_KEY") ? "environment" : ".env",
                environment.containsKey("GEMINI_MODEL") ? "environment"
                        : local.containsKey("GEMINI_MODEL") ? ".env" : "default");
        return new GeminiConfiguration(apiKey, model);
    }

    String apiKey() { return apiKey; }
    public String model() { return model; }

    @Override public String toString() { return "GeminiConfiguration[credentials redacted]"; }
}
