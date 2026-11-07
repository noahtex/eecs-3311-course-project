package com.eventplanner.ai;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;

/** Adaptee: owns the concrete official SDK interaction, not application planning logic. */
public class GeminiAPIClient {
    private static final System.Logger LOGGER = System.getLogger(GeminiAPIClient.class.getName());

    public String generateContent(String prompt) {
        // Load lazily so non-AI CLI commands remain usable without credentials.
        GeminiConfiguration configuration = GeminiConfiguration.load();
        try (Client client = Client.builder().apiKey(configuration.apiKey())
                .httpOptions(HttpOptions.builder().timeout(60_000).build()).build()) {
            var config = GenerateContentConfig.builder().responseMimeType("application/json").build();
            String text = client.models.generateContent(configuration.model(), prompt, config).text();
            if (text == null || text.isBlank()) {
                throw new PlanningException("Gemini returned no text (possibly blocked). Try revising the ideas.");
            }
            return text;
        } catch (PlanningException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOGGER.log(System.Logger.Level.WARNING, diagnostic(ex));
            // Do not attach the SDK cause: UI callers could expose its request/response data.
            throw new PlanningException("Gemini request failed. Check API credentials, model access, quota and network, then retry.");
        }
    }

    /** Allowlisted details only: arbitrary SDK messages can contain credentials or event data. */
    static String diagnostic(Throwable failure) {
        StringBuilder result = new StringBuilder("Gemini request failed; exception=");
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        for (Throwable cause = failure; cause != null && seen.size() < 8 && seen.add(cause);
                cause = cause.getCause()) {
            if (cause != failure) result.append("; causedBy=");
            result.append(cause.getClass().getName());
            if (cause instanceof ApiException api) {
                result.append("; httpStatus=").append(api.code()).append("; detail=").append(switch (api.code()) {
                    case 400 -> "invalid request or API key";
                    case 401 -> "authentication rejected";
                    case 403 -> "permission denied; check key restrictions and model access";
                    case 404 -> "resource/model unavailable for this API";
                    case 429 -> "quota or rate limit exceeded";
                    case 500, 502, 503, 504 -> "service unavailable or server error";
                    default -> "API rejected request";
                });
            } else if (cause instanceof java.net.UnknownHostException) {
                result.append("; detail=DNS lookup failed");
            } else if (cause instanceof javax.net.ssl.SSLException) {
                result.append("; detail=TLS connection failed");
            } else if (cause instanceof java.net.SocketTimeoutException) {
                result.append("; detail=network timeout");
            } else if (cause instanceof java.net.ConnectException) {
                result.append("; detail=network connection failed");
            }
        }
        return result.toString();
    }
}
