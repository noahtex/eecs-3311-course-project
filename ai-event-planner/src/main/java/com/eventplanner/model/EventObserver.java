package com.eventplanner.model;
import java.util.Set;
@FunctionalInterface
public interface EventObserver { void onEventChanged(Event event, Set<String> fields); }
