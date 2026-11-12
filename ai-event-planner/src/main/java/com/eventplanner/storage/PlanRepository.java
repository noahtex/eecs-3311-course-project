package com.eventplanner.storage;

import java.util.Optional;

/** Stores data snapshots, never live application objects. One repository is owned by one facade. */
public interface PlanRepository extends AutoCloseable {
 Optional<PlanData> load();
 void save(PlanData data);
 default String description(){return "In memory";}
 @Override default void close(){}
}
