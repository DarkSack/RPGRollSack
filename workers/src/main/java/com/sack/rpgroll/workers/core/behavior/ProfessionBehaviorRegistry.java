package com.sack.rpgroll.workers.core.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProfessionBehaviorRegistry {

    private final Map<String, ProfessionBehavior> behaviors = new HashMap<>();

    public void register(String professionId, ProfessionBehavior behavior) {
        behaviors.put(professionId, behavior);
    }

    public Optional<ProfessionBehavior> get(String professionId) {
        return Optional.ofNullable(behaviors.get(professionId));
    }

}
