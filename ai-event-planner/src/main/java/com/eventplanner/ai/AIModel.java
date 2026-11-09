package com.eventplanner.ai;

/** Adapter target interface: the agent depends only on this application abstraction. */
@FunctionalInterface
public interface AIModel extends AutoCloseable {
    String generate(String prompt);
    /** Models without owned resources (including lambdas/fakes) need no cleanup. */
    @Override default void close() { }
}
