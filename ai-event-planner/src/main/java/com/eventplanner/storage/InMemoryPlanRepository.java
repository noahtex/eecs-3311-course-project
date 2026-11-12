package com.eventplanner.storage;

import java.util.Optional;

/** Explicit test/embedded-session alternative; the default application launchers use JSON. */
public final class InMemoryPlanRepository implements PlanRepository {
 private PlanData data;
 @Override public synchronized Optional<PlanData> load(){return Optional.ofNullable(data);}
 @Override public synchronized void save(PlanData value){data=value;}
}
