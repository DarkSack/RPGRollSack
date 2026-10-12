package com.sack.rpgroll.pass.vote;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteServiceTest {

    @Test
    void bedrockNamesKeepTheirDotInTheFile() throws Exception {
        YamlConfiguration saved = VoteService.pendingYaml();
        saved.set(".bedrock_user", 2);

        YamlConfiguration loaded = VoteService.pendingYaml();
        loaded.loadFromString(saved.saveToString());

        assertEquals(Set.of(".bedrock_user"), loaded.getKeys(false));
        assertEquals(2, loaded.getInt(".bedrock_user"));
    }

    @Test
    void aBedrockPlayerAlsoGetsTheVotesTypedWithoutTheDot() {
        Map<String, Integer> pending = new HashMap<>(Map.of("steve", 1, ".steve", 2, "alex", 5));

        assertEquals(3, VoteService.take(pending, ".Steve"));
        assertEquals(0, VoteService.take(pending, "Steve"));
        assertEquals(Set.of("alex"), pending.keySet());
        assertTrue(VoteService.take(pending, "nadie") == 0);
    }

}
