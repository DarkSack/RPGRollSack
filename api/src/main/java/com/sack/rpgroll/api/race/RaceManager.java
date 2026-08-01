package com.sack.rpgroll.api.race;

import java.util.Collection;
import java.util.Optional;

public interface RaceManager {
    Optional<Race> get(String id);

    boolean exists(String id);

    Collection<Race> getAll();

    int count();

    void reload();
}