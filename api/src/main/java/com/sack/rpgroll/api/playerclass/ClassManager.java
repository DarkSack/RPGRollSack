package com.sack.rpgroll.api.playerclass;

import java.util.Collection;
import java.util.Optional;

public interface ClassManager {
    Optional<PlayerClass> get(String id);

    boolean exists(String id);

    Collection<PlayerClass> getAll();

    int count();

    void reload();
}