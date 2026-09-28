package com.sack.rpgroll.recipes.read;

import java.util.ArrayList;
import java.util.List;

/**
 * Mezclas de fermentación vanilla por si no se pueden leer del servidor. Se comprueban contra
 * las del servidor en la prueba en juego (infra/recetas/prueba).
 */
final class FallbackMixes {

    private FallbackMixes() {
    }

    /** "desde ingrediente hasta", sin el {@code minecraft:}. */
    private static final String[] TABLE = {
            "water glowstone_dust thick",
            "water redstone mundane",
            "water nether_wart awkward",
            "water fermented_spider_eye weakness",
            // Los ingredientes "de arranque" también dan una mundana si se echan al agua.
            "water breeze_rod mundane",
            "water slime_block mundane",
            "water stone mundane",
            "water cobweb mundane",
            "water magma_cream mundane",
            "water rabbit_foot mundane",
            "water sugar mundane",
            "water glistering_melon_slice mundane",
            "water spider_eye mundane",
            "water ghast_tear mundane",
            "water blaze_powder mundane",
            "awkward breeze_rod wind_charged",
            "awkward slime_block oozing",
            "awkward stone infested",
            "awkward cobweb weaving",
            "awkward golden_carrot night_vision",
            "night_vision redstone long_night_vision",
            "night_vision fermented_spider_eye invisibility",
            "long_night_vision fermented_spider_eye long_invisibility",
            "invisibility redstone long_invisibility",
            "awkward magma_cream fire_resistance",
            "fire_resistance redstone long_fire_resistance",
            "awkward rabbit_foot leaping",
            "leaping redstone long_leaping",
            "leaping glowstone_dust strong_leaping",
            "leaping fermented_spider_eye slowness",
            "long_leaping fermented_spider_eye long_slowness",
            "slowness redstone long_slowness",
            "slowness glowstone_dust strong_slowness",
            "awkward turtle_helmet turtle_master",
            "turtle_master redstone long_turtle_master",
            "turtle_master glowstone_dust strong_turtle_master",
            "swiftness fermented_spider_eye slowness",
            "long_swiftness fermented_spider_eye long_slowness",
            "awkward sugar swiftness",
            "swiftness redstone long_swiftness",
            "swiftness glowstone_dust strong_swiftness",
            "awkward pufferfish water_breathing",
            "water_breathing redstone long_water_breathing",
            "awkward glistering_melon_slice healing",
            "healing glowstone_dust strong_healing",
            "healing fermented_spider_eye harming",
            "strong_healing fermented_spider_eye strong_harming",
            "harming glowstone_dust strong_harming",
            "poison fermented_spider_eye harming",
            "long_poison fermented_spider_eye harming",
            "strong_poison fermented_spider_eye strong_harming",
            "awkward spider_eye poison",
            "poison redstone long_poison",
            "poison glowstone_dust strong_poison",
            "awkward ghast_tear regeneration",
            "regeneration redstone long_regeneration",
            "regeneration glowstone_dust strong_regeneration",
            "awkward blaze_powder strength",
            "strength redstone long_strength",
            "strength glowstone_dust strong_strength",
            "weakness redstone long_weakness",
            "awkward phantom_membrane slow_falling",
            "slow_falling redstone long_slow_falling",
    };

    static final List<BrewingReader.Mix> POTIONS = parse(TABLE);

    static final List<BrewingReader.Mix> CONTAINERS = parse(new String[] {
            "potion gunpowder splash_potion",
            "splash_potion dragon_breath lingering_potion",
    });

    private static List<BrewingReader.Mix> parse(String[] rows) {
        List<BrewingReader.Mix> mixes = new ArrayList<>(rows.length);
        for (String row : rows) {
            String[] parts = row.split(" ");
            mixes.add(new BrewingReader.Mix("minecraft:" + parts[0], List.of("minecraft:" + parts[1]),
                    "minecraft:" + parts[2]));
        }
        return List.copyOf(mixes);
    }
}
