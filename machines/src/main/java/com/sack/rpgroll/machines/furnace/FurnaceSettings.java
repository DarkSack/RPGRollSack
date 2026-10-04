package com.sack.rpgroll.machines.furnace;

import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Ui;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** furnaces.yml ya leído. */
public record FurnaceSettings(boolean enabled, Set<String> disabledWorlds, Set<Material> kinds, String itemModel,
        boolean frame, String frameModel, float frameScale, List<FurnaceTier> tiers) {

    /** Lo que se puede mejorar: los tres hornos vanilla. */
    public static final Set<Material> FURNACES = EnumSet.of(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER);

    public static FurnaceSettings from(FileConfiguration config, Consumer<String> warn) {

        Set<Material> kinds = EnumSet.noneOf(Material.class);
        for (String name : config.getStringList("kinds")) {
            Material material = Material.matchMaterial(name);
            if (material == null || !FURNACES.contains(material)) {
                warn.accept("furnaces.yml: '" + name + "' no es FURNACE, BLAST_FURNACE ni SMOKER");
            } else {
                kinds.add(material);
            }
        }

        List<FurnaceTier> tiers = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("tiers");
        if (section != null) {
            int level = 1;
            for (String id : section.getKeys(false)) {
                ConfigurationSection tier = section.getConfigurationSection(id);
                if (tier == null) {
                    continue;
                }
                double speed = tier.getDouble("speed", 1.0);
                double fuel = tier.getDouble("fuel", 1.0);
                double chance = tier.getDouble("double-chance", 0.0);
                if (speed <= 0 || fuel <= 0) {
                    warn.accept("furnaces.yml: el nivel '" + id + "' tiene speed o fuel <= 0; se ignora");
                    continue;
                }
                tiers.add(new FurnaceTier(id.toLowerCase(Locale.ROOT), level++, tier.getString("name", id), speed, fuel,
                        Math.clamp(chance, 0.0, 1.0),
                        Cost.from(tier.getConfigurationSection("cost"), text -> warn.accept("furnaces.yml (" + id + "): " + text))));
            }
        }

        return new FurnaceSettings(config.getBoolean("enabled", true),
                config.getStringList("disabled-worlds").stream().map(w -> w.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet()),
                kinds.isEmpty() ? FURNACES : kinds,
                config.getString("item-model", ""),
                config.getBoolean("frame.enabled", false),
                config.getString("frame.model", ""),
                (float) config.getDouble("frame.scale", 1.002),
                List.copyOf(tiers));
    }

    public Optional<FurnaceTier> tier(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return tiers.stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
    }

    /** El que sigue a {@code current} (null = horno vanilla), o vacío si ya es el último. */
    public Optional<FurnaceTier> next(FurnaceTier current) {
        int index = current == null ? 0 : tiers.indexOf(current) + 1;
        return index >= 0 && index < tiers.size() ? Optional.of(tiers.get(index)) : Optional.empty();
    }

    /** "furnace", "blast_furnace", "smoker": lo que va en {kind} de los patrones de modelo. */
    public static String kindId(Material material) {
        return material.name().toLowerCase(Locale.ROOT);
    }

    public NamespacedKey itemModel(Material kind, FurnaceTier tier) {
        return Ui.model(pattern(itemModel, kind, tier));
    }

    public NamespacedKey frameModel(Material kind, FurnaceTier tier) {
        return frame ? Ui.model(pattern(frameModel, kind, tier)) : null;
    }

    private static String pattern(String pattern, Material kind, FurnaceTier tier) {
        if (pattern == null || pattern.isBlank() || tier == null) {
            return null;
        }
        return pattern.replace("{kind}", kindId(kind)).replace("{tier}", tier.id());
    }

    public boolean allowed(String world) {
        return enabled && !disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }

    /** Un nivel de horno: cuánto más rápido cuece, cuánto rinde el combustible y la probabilidad de doble. */
    public record FurnaceTier(String id, int level, String name, double speed, double fuel, double doubleChance,
            Cost cost) {
    }
}
