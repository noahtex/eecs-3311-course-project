package com.eventplanner.ai;

import java.util.Objects;

/** Adapter: translates the AIModel target's generation operation to the SDK client adaptee. */
public final class GeminiAdapter implements AIModel {
    private final GeminiAPIClient client;

    public GeminiAdapter(GeminiAPIClient client) { this.client = Objects.requireNonNull(client); }

    @Override public String generate(String prompt) { return client.generateContent(prompt); }
    @Override public void close() { client.close(); }
}
