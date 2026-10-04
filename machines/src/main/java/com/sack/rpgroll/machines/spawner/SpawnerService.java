package com.sack.rpgroll.machines.spawner;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.core.Displays;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Upgrade;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Los niveles de un spawner viven en el PDC de su bloque y, recogido, en el de su ítem (con el
 * mob). Al colocarlo se aplican a mano: Minecraft no copia los datos de un ítem de spawner al
 * bloque si quien lo pone no es operador.
 */
public class SpawnerService {

    private final NamespacedKey speedKey;
    private final NamespacedKey countKey;
    private final NamespacedKey rangeKey;
    private final NamespacedKey stackKey;
    private final NamespacedKey mobKey;
    private final LangManager lang;
    private final Displays displays;
    private SpawnerSettings settings;

    public SpawnerService(Plugin plugin, LangManager lang, Displays displays, SpawnerSettings settings) {
        this.speedKey = new NamespacedKey(plugin, "spawner-speed");
        this.countKey = new NamespacedKey(plugin, "spawner-count");
        this.rangeKey = new NamespacedKey(plugin, "spawner-range");
        this.stackKey = new NamespacedKey(plugin, "spawner-stack");
        this.mobKey = new NamespacedKey(plugin, "spawner-mob");
        this.lang = lang;
        this.displays = displays;
        this.settings = settings;
    }

    public SpawnerSettings settings() {
        return settings;
    }

    public void settings(SpawnerSettings settings) {
        this.settings = settings;
    }

    public Optional<CreatureSpawner> spawner(Block block) {
        return block.getType() == Material.SPAWNER && block.getState(false) instanceof CreatureSpawner spawner
                ? Optional.of(spawner) : Optional.empty();
    }

    public SpawnerData data(CreatureSpawner spawner) {
        return read(spawner.getPersistentDataContainer());
    }

    private SpawnerData read(PersistentDataContainer pdc) {
        return new SpawnerData(pdc.getOrDefault(speedKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(countKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(rangeKey, PersistentDataType.INTEGER, 0),
                Math.max(1, pdc.getOrDefault(stackKey, PersistentDataType.INTEGER, 1)));
    }

    private void write(PersistentDataContainer pdc, SpawnerData data) {
        pdc.set(speedKey, PersistentDataType.INTEGER, data.speed());
        pdc.set(countKey, PersistentDataType.INTEGER, data.count());
        pdc.set(rangeKey, PersistentDataType.INTEGER, data.range());
        pdc.set(stackKey, PersistentDataType.INTEGER, data.stack());
    }

    /** Guarda los niveles, los aplica al spawner y pone su holograma. */
    public void apply(Block block, EntityType mob, SpawnerData data) {

        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            return;
        }
        if (mob != null) {
            spawner.setSpawnedType(mob);
        }
        write(spawner.getPersistentDataContainer(), data);

        SpawnerData.Applied values = data.applied(settings);
        // Mínimo y máximo se validan entre sí: primero se abre el hueco, luego se ajusta.
        spawner.setMaxSpawnDelay(Math.max(values.maxDelay(), spawner.getMinSpawnDelay()));
        spawner.setMinSpawnDelay(values.minDelay());
        spawner.setMaxSpawnDelay(values.maxDelay());
        if (spawner.getDelay() > values.maxDelay()) {
            spawner.setDelay(values.maxDelay());
        }
        spawner.setSpawnCount(values.spawnCount());
        spawner.setMaxNearbyEntities(values.maxNearby());
        spawner.setRequiredPlayerRange(values.range());
        spawner.update(true, false);
        hologram(block, spawner.getSpawnedType(), data);
    }

    public void hologram(Block block, EntityType mob, SpawnerData data) {

        if (!settings.hologram() || !data.upgraded()) {
            displays.remove(block, Displays.HOLOGRAM);
            return;
        }
        Component title = mob(lang.component("spawner.hologram_title", "stack", data.stack()), mob);
        Component levels = lang.component("spawner.hologram_levels",
                "speed", Ui.roman(data.speed()), "count", Ui.roman(data.count()), "range", Ui.roman(data.range()));
        displays.hologram(block, title.append(Component.newline()).append(levels), settings.hologramHeight());
    }

    /** Cambia {mob} por el nombre del mob en el idioma de cada jugador. */
    public static Component mob(Component text, EntityType mob) {
        Component name = mob == null ? Component.text("?") : Component.translatable(mob);
        return text.replaceText(TextReplacementConfig.builder().matchLiteral("{mob}").replacement(name).build());
    }

    // ---------------------------------------------------------------- ítems

    public ItemStack item(EntityType mob, SpawnerData data, int amount) {

        ItemStack item = new ItemStack(Material.SPAWNER, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mob(lang.component("spawner.item_name"), mob)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        List<Component> lore = new ArrayList<>();
        for (Upgrade upgrade : Upgrade.values()) {
            lore.add(lang.component("spawner.item_level", "upgrade", lang.raw("spawner.upgrade." + upgrade.id()),
                    "level", Ui.roman(data.level(upgrade))));
        }
        if (data.stack() > 1) {
            lore.add(lang.component("spawner.item_stack", "stack", data.stack()));
        }
        meta.lore(lore.stream().map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList());
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(mobKey, PersistentDataType.STRING, mob == null ? "" : mob.getKey().toString());
        write(pdc, data);
        item.setItemMeta(meta);
        return item;
    }

    /** El mob y los niveles de un ítem de spawner nuestro, o vacío si es otro ítem. */
    public Optional<Held> held(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String mob = pdc.get(mobKey, PersistentDataType.STRING);
        if (mob == null) {
            return Optional.empty();
        }
        return Optional.of(new Held(entity(mob), read(pdc)));
    }

    public record Held(EntityType mob, SpawnerData data) {
    }

    public static EntityType entity(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        NamespacedKey namespaced = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
        if (namespaced == null) {
            return null;
        }
        for (EntityType type : EntityType.values()) {
            if (type != EntityType.UNKNOWN && type.getKey().equals(namespaced)) {
                return type;
            }
        }
        return null;
    }
}
