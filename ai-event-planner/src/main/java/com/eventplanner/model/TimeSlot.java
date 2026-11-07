package com.eventplanner.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Half-open interval: adjacent slots do not overlap. All times are event-local. */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {
    public TimeSlot {
        Objects.requireNonNull(start, "Start is required.");
        Objects.requireNonNull(end, "End is required.");
        if (!start.isBefore(end)) throw new IllegalArgumentException("Slot end must follow start.");
    }
    public boolean overlaps(TimeSlot other) { return start.isBefore(other.end) && other.start.isBefore(end); }
    public boolean contains(TimeSlot other) { return !other.start.isBefore(start) && !other.end.isAfter(end); }
}
