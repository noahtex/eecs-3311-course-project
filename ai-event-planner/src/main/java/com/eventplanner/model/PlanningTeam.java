package com.eventplanner.model;

import java.util.ArrayList;
import java.util.List;

public class PlanningTeam {

    private String id;
    private String name;
    private List<User> members;
    private List<Event> events;

    public PlanningTeam(String id, String name) {
        this.id = id;
        this.name = name;
        this.members = new ArrayList<>();
        this.events = new ArrayList<>();
    }

    public void addMember(User user) {
        if (user != null && members.stream().noneMatch(member -> member.getId().equals(user.getId()))) {
            members.add(user);
        }
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    public List<User> getMembers() {
        return List.copyOf(members);
    }

    public void addEvent(Event event) {
        if (event != null && !events.contains(event)) {
            events.add(event);
        }
    }

    public List<Event> getEvents() {
        return List.copyOf(events);
    }
    public void removeEvent(Event event){events.remove(event);}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
