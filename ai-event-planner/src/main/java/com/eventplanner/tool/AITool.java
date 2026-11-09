package com.eventplanner.tool;

import com.eventplanner.model.ToolResult;

@FunctionalInterface
public interface AITool extends AutoCloseable {
    ToolResult execute(String query);
    @Override default void close() { }
}
