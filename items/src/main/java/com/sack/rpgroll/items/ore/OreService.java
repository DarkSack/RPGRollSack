package com.sack.rpgroll.items.ore;

import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lo que las menas necesitan en tiempo de juego: qué estado de bloque es qué
 * mena, qué pico llega a qué nivel y cuánto se ha sembrado.
 *
 * <p>Nivel de pico: el de vanilla (madera y oro 0, piedra y cobre 1, hierro
 * 2, diamante 3, netherita 4) salvo que el ítem de RPGRoll diga otro en
 * {@code custom-data.mining-tier}; {@code custom-data.mining-speed} cambia
 * también su velocidad contra las menas.
 */
public class OreService {

    /** Una mena en una de sus variantes. */
    public record OreBlock(OreDefinition ore, String variant, BlockData data) {
    }

    private static final Map<String, Integer> TIER_BY_PREFIX = Map.of(
            "WOODEN", 0, "GOLDEN", 0, "STONE", 1, "COPPER", 1, "IRON", 2, "DIAMOND", 3, "NETHERITE", 4);
    private static final Map<String, Double> SPEED_BY_PREFIX = Map.of(
            "WOODEN", 2.0, "STONE", 4.0, "COPPER", 5.0, "IRON", 6.0, "DIAMOND", 8.0, "NETHERITE", 9.0,
            "GOLDEN", 12.0);

    private final Plugin plugin;
    private final OreManager ores;
    private final ItemManager items;
    private final ItemInstanceService instances;

    private volatile Map<String, OreBlock> byState = Map.of();
    private volatile Set<Material> materials = EnumSet.noneOf(Material.class);
    private volatile Set<Instrument> reservedInstruments = EnumSet.noneOf(Instrument.class);

    private final Map<String, AtomicLong> placed = new ConcurrentHashMap<>();
    private final AtomicLong chunks = new AtomicLong();

    public OreService(Plugin plugin, OreManager ores, ItemManager items, ItemInstanceService instances) {
        this.plugin = plugin;
        this.ores = ores;
        this.items = items;
        this.instances = instances;
    }

    /** Vuelve a leer los estados de las menas (al arrancar y tras /itemadmin reload). */
    public void rebuild() {

        Map<String, OreBlock> states = new HashMap<>();
        Set<Material> blockTypes = EnumSet.noneOf(Material.class);
        Set<Instrument> instruments = new HashSet<>();

        for (OreDefinition ore : ores.getAll()) {
            for (var entry : ore.blocks().entrySet()) {

                BlockData data;
                try {
                    data = Bukkit.createBlockData(entry.getValue());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("✘ Mena '" + ore.id() + "': estado de bloque inválido '"
                            + entry.getValue() + "'");
                    continue;
                }

                OreBlock previous = states.put(data.getAsString(), new OreBlock(ore, entry.getKey(), data));
                if (previous != null) {
                    plugin.getLogger().warning("✘ Las menas '" + previous.ore().id() + "' y '" + ore.id()
                            + "' usan el mismo estado de bloque: " + entry.getValue());
                }

                blockTypes.add(data.getMaterial());
                if (data instanceof NoteBlock note) {
                    instruments.add(note.getInstrument());
                }
            }
        }

        byState = Map.copyOf(states);
        materials = blockTypes.isEmpty() ? EnumSet.noneOf(Material.class) : EnumSet.copyOf(blockTypes);
        reservedInstruments = instruments.isEmpty() ? EnumSet.noneOf(Instrument.class) : EnumSet.copyOf(instruments);
    }

    public Optional<OreBlock> at(Block block) {
        if (!materials.contains(block.getType())) {
            return Optional.empty();
        }
        return Optional.ofNullable(byState.get(block.getBlockData().getAsString()));
    }

    /** Instrumentos que usan las menas: un bloque musical colocado a mano nunca debe tenerlos. */
    public boolean isReserved(Instrument instrument) {
        return reservedInstruments.contains(instrument);
    }

    public boolean hasOres() {
        return !byState.isEmpty();
    }

    // ---------------------------------------------------------------- herramientas

    public static boolean isPickaxe(Material material) {
        return material.name().endsWith("_PICKAXE");
    }

    public static boolean isAxe(Material material) {
        return material.name().endsWith("_AXE") && !isPickaxe(material);
    }

    public int tier(ItemStack tool) {
        Integer custom = customNumber(tool, "mining-tier").map(Double::intValue).orElse(null);
        return custom != null ? custom : TIER_BY_PREFIX.getOrDefault(prefix(tool), 0);
    }

    /** Velocidad base de la herramienta (sin Eficiencia). */
    public double speed(ItemStack tool) {
        return customNumber(tool, "mining-speed").orElseGet(() -> SPEED_BY_PREFIX.getOrDefault(prefix(tool), 1.0));
    }

    private static String prefix(ItemStack tool) {
        if (tool == null) {
            return "";
        }
        String name = tool.getType().name();
        int cut = name.indexOf('_');
        return cut < 0 ? name : name.substring(0, cut);
    }

    private Optional<Double> customNumber(ItemStack tool, String key) {

        if (tool == null || tool.getType() == Material.AIR) {
            return Optional.empty();
        }

        return instances.getId(tool).flatMap(items::get).map(definition -> definition.customData().get(key))
                .flatMap(raw -> {
                    try {
                        return Optional.of(Double.parseDouble(raw.trim()));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }

    // ---------------------------------------------------------------- estadísticas

    void countPlaced(String oreId, int blocks) {
        placed.computeIfAbsent(oreId, key -> new AtomicLong()).addAndGet(blocks);
    }

    void countChunk() {
        chunks.incrementAndGet();
    }

    public long placed(String oreId) {
        AtomicLong count = placed.get(oreId.toLowerCase(Locale.ROOT));
        return count == null ? 0 : count.get();
    }

    public long chunksSown() {
        return chunks.get();
    }

    public OreManager ores() {
        return ores;
    }

}
