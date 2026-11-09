package com.eventplanner.ai;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeminiLifecycleTest {
    private static final class FakeSession implements GeminiAPIClient.Session {
        int requests;
        int closes;
        boolean failRequest;
        boolean failClose;

        @Override public String generate(String prompt) {
            requests++;
            if (failRequest) throw new IllegalStateException("private-request-details");
            return "{}";
        }

        @Override public void close() {
            closes++;
            if (failClose) throw new IllegalStateException("private-close-details");
        }
    }

    @Test
    void adapterReusesLazyOwnedSessionAndClosesItExactlyOnce() {
        FakeSession session = new FakeSession();
        AtomicInteger opens = new AtomicInteger();
        var client = new GeminiAPIClient(() -> { opens.incrementAndGet(); return session; });
        var adapter = new GeminiAdapter(client);
        assertEquals(0, opens.get());
        assertEquals("{}", adapter.generate("first"));
        assertEquals("{}", adapter.generate("second"));
        assertEquals(1, opens.get());
        assertEquals(2, session.requests);
        assertEquals(0, session.closes);
        adapter.close();
        adapter.close();
        assertEquals(1, session.closes);
        assertThrows(PlanningException.class, () -> adapter.generate("after close"));
        assertEquals(2, session.requests);
    }

    @Test
    void closingUnusedClientDoesNotLoadConfigurationOrAllocateTransport() {
        var client = new GeminiAPIClient(() -> fail("Must not load credentials or create a transport"));
        client.close();
        client.close();
        assertThrows(PlanningException.class, () -> client.generateContent("after close"));
    }

    @Test
    void requestFailureStillClosesOwnedSession() {
        FakeSession session = new FakeSession();
        session.failRequest = true;
        try (var client = new GeminiAPIClient(() -> session)) {
            var failure = assertThrows(PlanningException.class, () -> client.generateContent("request"));
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("private-request-details"));
        }
        assertEquals(1, session.closes);
    }

    @Test
    void cleanupFailureIsSanitizedAndDoesNotCauseRepeatedCleanup() {
        FakeSession session = new FakeSession();
        session.failClose = true;
        var client = new GeminiAPIClient(() -> session);
        client.generateContent("request");
        var failure = assertThrows(PlanningException.class, client::close);
        assertTrue(failure.getMessage().contains("cleanup failed"));
        assertFalse(failure.getMessage().contains("private-close-details"));
        assertNull(failure.getCause());
        client.close();
        assertEquals(1, session.closes);
    }
}
