package com.eventplanner.ai;

import com.google.genai.errors.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeminiDiagnosticsTest {
    @Test
    void reportsHttpStatusWithoutAnyServerControlledText() {
        var error = new ApiException(429, "secret-status", "Authorization: secret-key; private event data");
        String diagnostic = GeminiAPIClient.diagnostic(new RuntimeException("secret-wrapper", error));
        assertTrue(diagnostic.contains("java.lang.RuntimeException"));
        assertTrue(diagnostic.contains("ApiException"));
        assertTrue(diagnostic.contains("httpStatus=429"));
        assertTrue(diagnostic.contains("quota or rate limit"));
        assertFalse(diagnostic.contains("secret"));
        assertFalse(diagnostic.contains("Authorization"));
        assertFalse(diagnostic.contains("private event"));
    }

    @Test
    void identifiesNetworkCauseWithoutLoggingHostOrRequest() {
        String diagnostic = GeminiAPIClient.diagnostic(new RuntimeException("private prompt",
                new java.net.UnknownHostException("private-host?key=secret")));
        assertTrue(diagnostic.contains("DNS lookup failed"));
        assertFalse(diagnostic.contains("private"));
        assertFalse(diagnostic.contains("secret"));
    }

    @Test
    void boundsCyclicCauseChains() {
        var first = new RuntimeException("secret");
        var second = new RuntimeException("secret", first);
        first.initCause(second);
        assertTrue(GeminiAPIClient.diagnostic(first).length() < 300);
    }
}
