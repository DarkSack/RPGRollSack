package com.sack.rpgroll.structures.integration;

import java.util.UUID;

/** ¿Juega este jugador desde Bedrock? */
public interface BedrockCheck {

    boolean isBedrock(UUID player);

    /** Nadie es Bedrock (sin Floodgate). */
    static BedrockCheck none() {
        return id -> false;
    }
}
