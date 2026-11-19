package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crates.lucky.LuckyAction;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Un tesoro enterrado (plugins/RPGRoll-Crates/treasures/*.yml).
 * <p>
 * Se consigue su mapa; al leerlo por primera vez el mapa elige un punto en el anillo
 * [minDistance, maxDistance] alrededor de quien lo lee, en uno de sus biomas, y lo guarda. Cavando
 * hasta {@code depth} bloques bajo la superficie de ese punto aparece el cofre, con su modelo,
 * sus guardianes y su botín.
 *
 * @param actions acciones de lucky block (MONEY, XP, COMMAND, MESSAGE...) al abrir el cofre
 */
public record Treasure(
        String id,
        String displayName,
        MapItem map,
        Placement placement,
        Chest chest,
        Loot loot,
        List<LuckyAction> actions) implements RPGContent {

    public Treasure {
        Objects.requireNonNull(id, "id");
        displayName = displayName == null ? id : displayName;
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(chest, "chest");
        Objects.requireNonNull(loot, "loot");
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    /** El ítem del mapa: {@code material} con el modelo {@code model} (null = el material tal cual). */
    public record MapItem(String material, String model, String name, List<String> lore) {
        public MapItem {
            lore = lore == null ? List.of() : List.copyOf(lore);
        }
    }

    /**
     * Dónde puede caer.
     *
     * @param worlds mundos permitidos por nombre (vacío = cualquier mundo normal)
     * @param biomes claves de bioma {@code minecraft:beach} (vacío = cualquiera)
     */
    public record Placement(List<String> worlds, Set<String> biomes, int minDistance, int maxDistance, int depth) {
        public Placement {
            worlds = worlds == null ? List.of() : List.copyOf(worlds);
            biomes = biomes == null ? Set.of() : Set.copyOf(biomes);
            if (minDistance < 0 || maxDistance < minDistance) {
                throw new IllegalArgumentException("min-distance debe ir de 0 a max-distance");
            }
            if (depth < 1 || depth > 32) {
                throw new IllegalArgumentException("depth debe ir de 1 a 32");
            }
        }
    }

    /**
     * El cofre que aparece al cavar.
     *
     * @param model     modelo del resource pack para el ItemDisplay (null = un cofre vanilla)
     * @param guardians acciones de lucky block al aparecer (MOB, COMMAND con mobadmin spawn...)
     */
    public record Chest(String model, double scale, int rows, String title, List<LuckyAction> guardians) {
        public Chest {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("chest.rows debe ir de 1 a 6");
            }
            if (scale <= 0) {
                throw new IllegalArgumentException("chest.scale debe ser mayor que 0");
            }
            guardians = guardians == null ? List.of() : List.copyOf(guardians);
        }
    }

    /** El botín: entre {@code minRolls} y {@code maxRolls} tiradas sobre las entradas, por peso. */
    public record Loot(int minRolls, int maxRolls, List<Entry> entries) {
        public Loot {
            if (minRolls < 1 || maxRolls < minRolls) {
                throw new IllegalArgumentException("loot.rolls debe ser 1 o más, y el mínimo no puede pasar del máximo");
            }
            entries = entries == null ? List.of() : List.copyOf(entries);
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("loot.entries no tiene ninguna entrada con peso");
            }
        }
    }

    /**
     * Una entrada del botín.
     *
     * @param item material vanilla ({@code GOLD_INGOT}) o id de RPGRoll-Items ({@code mareas_doblon})
     */
    public record Entry(String item, int min, int max, double weight) {
        public Entry {
            Objects.requireNonNull(item, "item");
            if (min < 1 || max < min) {
                throw new IllegalArgumentException("amount de '" + item + "' debe ser 1 o más");
            }
            if (weight <= 0) {
                throw new IllegalArgumentException("weight de '" + item + "' debe ser mayor que 0");
            }
        }
    }

}
