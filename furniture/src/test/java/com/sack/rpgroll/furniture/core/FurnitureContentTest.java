package com.sack.rpgroll.furniture.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los muebles de fábrica (generados por infra/muebles) se leen enteros y sin un solo aviso. */
class FurnitureContentTest {

    @Test
    void factoryFurnitureLoadsWithoutWarnings() {

        File folder = new File("src/main/resources/furniture");
        assertTrue(folder.isDirectory(), "faltan los muebles de fábrica en " + folder.getAbsolutePath());

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        List<String> warnings = new ArrayList<>();
        FurnitureManager manager = new FurnitureManager();
        manager.load(folder, config.getConfigurationSection("categories"), warnings::add);

        assertEquals(List.of(), warnings);
        assertTrue(manager.count() >= 40, "solo " + manager.count() + " muebles");

        FurnitureDefinition sofa = manager.get("sofa").orElseThrow();
        assertEquals(3, sofa.functions().seat().positions().size());
        assertEquals(3, sofa.hitbox().blocks().size());
        assertEquals(16, sofa.variants().size());
        assertTrue(sofa.variantForDye(org.bukkit.DyeColor.RED).isPresent());

        FurnitureDefinition chair = manager.get("chair").orElseThrow();
        assertEquals(12, chair.variants().size());
        assertEquals("rpgroll_furniture:chair/spruce", chair.model("spruce", 0));
        assertEquals("spruce_planks",
                chair.recipe("spruce").orElseThrow().materials().keySet().iterator().next().getKey().getKey());

        FurnitureDefinition lamp = manager.get("floor_lamp").orElseThrow();
        assertEquals("rpgroll_furniture:floor_lamp/default_on", lamp.model(null, 1));
        assertEquals(15, lamp.functions().lightLevel(1));
        assertEquals(0, lamp.functions().lightLevel(0));

        FurnitureDefinition carpenter = manager.get("carpenter_table").orElseThrow();
        assertTrue(carpenter.functions().workstation().carpenter());

        // Todas las categorías de los muebles están en el config (si no, salen al final sin nombre).
        for (FurnitureDefinition def : manager.all()) {
            assertTrue(config.isConfigurationSection("categories." + def.category()),
                    def.id() + " usa la categoría " + def.category() + ", que no está en config.yml");
        }
        assertFalse(manager.categoriesWith(d -> d.recipe(d.resolveVariant(null)).isPresent()).isEmpty());
    }
}
