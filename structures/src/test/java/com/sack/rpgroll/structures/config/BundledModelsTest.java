package com.sack.rpgroll.structures.config;

import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** El workbench.yml de fábrica apunta a modelos que el jar trae en su resourcepack/. */
class BundledModelsTest {

    private static InputStream resource(String path) {
        return BundledModelsTest.class.getClassLoader().getResourceAsStream(path);
    }

    @Test
    void everyDefaultTierModelShipsInTheEmbeddedPack() throws Exception {
        YamlConfiguration yaml;
        try (InputStream in = resource("workbench.yml")) {
            assertNotNull(in, "falta workbench.yml");
            yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertNotNull(resource("resourcepack/pack.yml"), "falta resourcepack/pack.yml");
        for (WorkbenchTier tier : WorkbenchTier.values()) {
            String model = yaml.getString("tiers." + tier.name().toLowerCase(java.util.Locale.ROOT) + ".model", "");
            assertFalse(model.isBlank(), tier + " sin modelo de fábrica");
            String[] id = model.split(":", 2);
            String definition = "resourcepack/assets/" + id[0] + "/items/" + id[1] + ".json";
            try (InputStream in = resource(definition)) {
                assertNotNull(in, tier + ": no está " + definition);
            }
        }
    }
}
