package com.eventplanner.ai;

/** Adapter target interface: the agent depends only on this application abstraction. */
@FunctionalInterface
public interface AIModel {
    String generate(String prompt);
}
