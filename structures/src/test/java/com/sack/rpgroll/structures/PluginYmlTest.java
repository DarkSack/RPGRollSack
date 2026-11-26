package com.sack.rpgroll.structures;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginYmlTest {

    @Test void loadsAfterTheModulesThatGiveMaterials() throws Exception {
        try (InputStream in = PluginYmlTest.class.getResourceAsStream("/plugin.yml")) {
            assertNotNull(in);
            YamlConfiguration y = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> soft = y.getStringList("softdepend");
            // item:/mueble: de las estructuras solo se validan si esos módulos ya registraron sus Things.Give.
            assertTrue(soft.containsAll(List.of("RPGRoll-Items", "RPGRoll-Furniture")), soft.toString());
            assertTrue(soft.containsAll(List.of("SackResourcePack", "WorldEdit", "FastAsyncWorldEdit", "WorldGuard",
                    "GriefPrevention", "Vault", "floodgate")), "se conservan los de antes: " + soft);
        }
    }
}
