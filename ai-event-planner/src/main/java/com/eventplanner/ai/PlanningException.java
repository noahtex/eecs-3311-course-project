package com.eventplanner.ai;

/** User-safe planning failure. Raw SDK responses and credentials are never included. */
public class PlanningException extends RuntimeException {
    public PlanningException(String message) { super(message); }
}
