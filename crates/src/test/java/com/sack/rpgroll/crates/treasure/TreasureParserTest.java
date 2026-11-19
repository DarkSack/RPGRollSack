package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.crates.lucky.LuckyAction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreasureParserTest {

    private static Treasure parse(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return new TreasureParser().parse(config);
    }

    @Test
    void parsesEveryField() throws Exception {

        Treasure t = parse("""
                id: Mareas_Cofre_Capitan
                display-name: "&6Cofre del Capitán"
                map:
                  material: FILLED_MAP
                  model: "mareas:mapa_capitan"
                  name: "&eMapa del Capitán"
                  lore: ["&7Una X junto al mar"]
                placement:
                  worlds: [world]
                  biomes: [beach, Minecraft:Snowy_Beach]
                  min-distance: 200
                  max-distance: 800
                  depth: 4
                chest:
                  model: "mareas:cofre_capitan"
                  scale: 1.2
                  rows: 4
                  title: "&8Cofre del Capitán"
                  guardians:
                    - {type: MOB, entity: DROWNED, amount: 2}
                loot:
                  rolls: 2-5
                  entries:
                    - {item: GOLD_INGOT, amount: 3-8, weight: 10}
                    - {item: mareas_doblon, weight: 2}
                    - {item: DIAMOND, weight: 0}
                actions:
                  - {type: MONEY, amount: 500}
                """);

        assertEquals("mareas_cofre_capitan", t.id());
        assertEquals("&6Cofre del Capitán", t.displayName());
        assertEquals("FILLED_MAP", t.map().material());
        assertEquals("mareas:mapa_capitan", t.map().model());
        assertEquals("&eMapa del Capitán", t.map().name());
        assertEquals(List.of("&7Una X junto al mar"), t.map().lore());
        assertEquals(List.of("world"), t.placement().worlds());
        assertEquals(Set.of("minecraft:beach", "minecraft:snowy_beach"), t.placement().biomes());
        assertEquals(200, t.placement().minDistance());
        assertEquals(800, t.placement().maxDistance());
        assertEquals(4, t.placement().depth());
        assertEquals("mareas:cofre_capitan", t.chest().model());
        assertEquals(1.2, t.chest().scale(), 1e-9);
        assertEquals(4, t.chest().rows());
        assertEquals(1, t.chest().guardians().size());
        assertEquals(LuckyAction.Type.MOB, t.chest().guardians().getFirst().type());
        assertEquals(2, t.loot().minRolls());
        assertEquals(5, t.loot().maxRolls());
        assertEquals(2, t.loot().entries().size(), "la entrada de peso 0 se descarta");
        Treasure.Entry gold = t.loot().entries().getFirst();
        assertEquals("GOLD_INGOT", gold.item());
        assertEquals(3, gold.min());
        assertEquals(8, gold.max());
        Treasure.Entry doblon = t.loot().entries().get(1);
        assertEquals(1, doblon.min());
        assertEquals(1, doblon.max());
        assertEquals(LuckyAction.Type.MONEY, t.actions().getFirst().type());
    }

    @Test
    void bundledReferenceParses() throws Exception {

        java.nio.file.Path reference = java.nio.file.Path.of(
                TreasureParserTest.class.getResource("/treasures/_referencia.yml").toURI());
        Treasure t = parse(java.nio.file.Files.readString(reference, java.nio.charset.StandardCharsets.UTF_8));

        assertEquals("tesoro_de_referencia", t.id());
        assertEquals(2, t.chest().guardians().size());
        assertEquals(4, t.loot().entries().size());
        assertEquals(2, t.actions().size());
    }

    @Test
    void appliesDefaults() throws Exception {

        Treasure t = parse("""
                id: simple
                loot:
                  entries:
                    - {item: EMERALD}
                """);

        assertEquals("simple", t.displayName());
        assertEquals("PAPER", t.map().material());
        assertNull(t.map().model());
        assertTrue(t.placement().worlds().isEmpty(), "vacío = cualquier mundo normal");
        assertTrue(t.placement().biomes().isEmpty(), "vacío = cualquier bioma");
        assertEquals(120, t.placement().minDistance());
        assertEquals(500, t.placement().maxDistance());
        assertEquals(3, t.placement().depth());
        assertNull(t.chest().model());
        assertEquals(3, t.chest().rows());
        assertEquals(3, t.loot().minRolls());
        assertEquals(6, t.loot().maxRolls());
        assertTrue(t.actions().isEmpty());
    }

    @Test
    void rejectsBrokenDefinitions() {

        assertThrows(IllegalArgumentException.class, () -> parse("display-name: x"), "sin id");
        assertThrows(IllegalArgumentException.class, () -> parse("id: vacio"), "sin botín");
        assertThrows(IllegalArgumentException.class, () -> parse("""
                id: solo_ceros
                loot:
                  entries:
                    - {item: DIAMOND, weight: 0}
                """), "todas las entradas con peso 0");
        assertThrows(IllegalArgumentException.class, () -> parse("""
                id: distancias
                placement: {min-distance: 500, max-distance: 100}
                loot: {entries: [{item: DIAMOND}]}
                """), "mínimo por encima del máximo");
        assertThrows(IllegalArgumentException.class, () -> parse("""
                id: filas
                chest: {rows: 7}
                loot: {entries: [{item: DIAMOND}]}
                """), "más de 6 filas");
        assertThrows(IllegalArgumentException.class, () -> parse("""
                id: accion
                loot: {entries: [{item: DIAMOND}]}
                actions: [{type: NO_EXISTE}]
                """), "acción desconocida");
    }

}
