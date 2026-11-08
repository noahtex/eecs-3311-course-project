package com.eventplanner.tool;

import com.eventplanner.model.ToolResult;

@FunctionalInterface
public interface AITool { ToolResult execute(String query); }
