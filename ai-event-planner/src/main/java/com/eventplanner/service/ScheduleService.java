package com.eventplanner.service;

import com.eventplanner.model.*;
import java.util.*;

/** Resource IDs are case-insensitive, exclusive resources (including session spaces). */
public final class ScheduleService implements EventObserver {
    @Override public void onEventChanged(Event event, Set<String> fields) { if(event.getSchedule()!=null) { var issues=new ArrayList<String>(); issues.add("Schedule requires review after changes: "+fields); issues.addAll(detectConflicts(event,event.getSchedule())); event.flagReview("schedule",issues); } }
    private String key(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private Set<String> resources(Session session) {
        Set<String> result = new HashSet<>();
        result.add(key(session.space()));
        session.resources().forEach(value -> result.add(key(value)));
        return result;
    }

    public List<String> detectConflicts(Schedule schedule) {
        List<String> issues = new ArrayList<>();
        var sessions = schedule.sessions();
        for (int i = 0; i < sessions.size(); i++) {
            for (int j = i + 1; j < sessions.size(); j++) {
                Session a = sessions.get(i), b = sessions.get(j);
                if (!a.slot().overlaps(b.slot())) continue;
                String prefix = "Sessions " + a.id() + " and " + b.id() + " overlap: ";
                if (a.speakerId() != null && a.speakerId().equals(b.speakerId()))
                    issues.add(prefix + "same speaker " + a.speakerId());
                Set<String> shared = resources(a);
                shared.retainAll(resources(b));
                if (!shared.isEmpty()) issues.add(prefix + "shared space/resource " + new TreeSet<>(shared));
            }
        }
        return List.copyOf(issues);
    }

    public List<String> detectConflicts(Event event, Schedule schedule) {
        List<String> issues = new ArrayList<>(detectConflicts(schedule));
        TimeSlot bounds = event.getEndDateTime() == null ? null : new TimeSlot(event.getDateTime(), event.getEndDateTime());
        if (bounds == null) issues.add("Event end time is unknown; configure the event window.");
        for (Session session : schedule.sessions()) {
            String prefix = "Session " + session.id() + " (" + session.title() + "): ";
            if (bounds != null && !bounds.contains(session.slot())) issues.add(prefix + "outside event time boundaries.");
            if (session.speakerId() != null) {
                Speaker speaker = event.getSpeakers().stream().filter(value -> value.id().equals(session.speakerId())).findFirst().orElse(null);
                if (speaker == null) issues.add(prefix + "unknown speaker.");
                else if (speaker.availability().isEmpty()) issues.add(prefix + "speaker availability is unknown.");
                else if (!speaker.checkAvailability(session.slot())) issues.add(prefix + "speaker is unavailable.");
            }
            for (String resource : resources(session)) {
                var slots = event.getResourceAvailability().get(resource);
                if (slots == null || slots.isEmpty()) issues.add(prefix + "availability unknown for resource " + resource);
                else if (slots.stream().noneMatch(slot -> slot.contains(session.slot())))
                    issues.add(prefix + "resource unavailable: " + resource);
            }
        }
        for (Session.Kind kind : Session.Kind.values()) {
            if (schedule.sessions().stream().noneMatch(session -> session.kind() == kind)) issues.add("Agenda is missing " + kind + ".");
        }
        var setup = schedule.sessions().stream().filter(s -> s.kind() == Session.Kind.SETUP).toList();
        var teardown = schedule.sessions().stream().filter(s -> s.kind() == Session.Kind.TEARDOWN).toList();
        for (Session activity : schedule.sessions()) {
            if (activity.kind() != Session.Kind.ACTIVITY && activity.kind() != Session.Kind.BREAK) continue;
            if (setup.stream().noneMatch(s -> !s.slot().end().isAfter(activity.slot().start())))
                issues.add("Session " + activity.id() + ": must follow setup.");
            if (teardown.stream().noneMatch(s -> !s.slot().start().isBefore(activity.slot().end())))
                issues.add("Session " + activity.id() + ": must precede teardown.");
        }
        return List.copyOf(issues);
    }

    public void saveSchedule(Event event, Schedule approved) {
        var issues = detectConflicts(event, approved);
        if (!issues.isEmpty()) throw new IllegalArgumentException("Schedule cannot be approved: " + String.join(" ", issues));
        event.storeSchedule(approved);
    }
}
