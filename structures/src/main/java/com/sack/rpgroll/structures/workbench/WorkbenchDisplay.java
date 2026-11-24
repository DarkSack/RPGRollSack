package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings.TierSettings;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Cómo se ve una mesa. Con modelo (y el pack instalado): barrera más un único {@link ItemDisplay} marcado con
 * su id. Sin modelo: el {@code fallback-block} del nivel. Solo en el hilo principal.
 */
public class WorkbenchDisplay {
    private final NamespacedKey displayKey;
    private final Supplier<WorkbenchSettings> settings;
    private final LangManager lang;
    private final Logger log;
    private final Set<Material> warned = new HashSet<>();
    private final BenchChunks chunks;

    public WorkbenchDisplay(Plugin plugin, Supplier<WorkbenchSettings> settings, LangManager lang, Logger log) {
        this.displayKey = new NamespacedKey(plugin, "display");
        this.chunks = new BenchChunks(plugin);
        this.settings = settings;
        this.lang = lang;
        this.log = log;
    }

    /** El bloque que se ve sin modelo: el del nivel, o uno seguro si el material no es un bloque. */
    public Material fallback(WorkbenchTier tier) {
        TierSettings t = settings.get().tiers().get(tier);
        Material m = t.fallbackBlock();
        if (!m.isBlock() && warned.add(m)) {
            log.warning(lang.raw("workbench.warning.fallback-not-block", "material", m.name(), "tier", tier.name()));
        }
        return t.placeableBlock();
    }

    /** Modelo del nivel, si hay uno válido y el pack está instalado. */
    private NamespacedKey model(TierSettings t) {
        if (t.model().isBlank() || !Bukkit.getPluginManager().isPluginEnabled("SackResourcePack")) {
            return null;
        }
        return NamespacedKey.fromString(t.model().trim().toLowerCase(Locale.ROOT));
    }

    public boolean usesModel(WorkbenchRecord r) {
        return model(settings.get().tiers().get(r.tier())) != null;
    }

    /** El bloque que la mesa debe tener con la configuración actual: barrera con modelo, si no el del nivel. */
    public Material expectedBlock(WorkbenchRecord r) {
        return usesModel(r) ? Material.BARRIER : fallback(r.tier());
    }

    private Block block(WorkbenchRecord r) {
        World w = Bukkit.getWorld(r.world());
        return w == null ? null : w.getBlockAt(r.x(), r.y(), r.z());
    }

    /** Pone el bloque (y el display con modelo) y apunta en el PDC del chunk qué bloque se puso. */
    public void show(WorkbenchRecord r) {
        Block b = block(r);
        if (b == null) {
            return;
        }
        removeAround(b, r);
        NamespacedKey model = model(settings.get().tiers().get(r.tier()));
        Material block = model == null ? fallback(r.tier()) : Material.BARRIER;
        b.setType(block, false);
        chunks.mark(b.getChunk(), r, block);
        if (model == null) {
            return;
        }
        ItemStack shown = ItemStack.of(Material.PAPER);
        ItemMeta meta = shown.getItemMeta();
        meta.setItemModel(model);
        shown.setItemMeta(meta);
        b.getWorld().spawn(center(b), ItemDisplay.class, d -> {
            d.setItemStack(shown);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.getPersistentDataContainer().set(displayKey, PersistentDataType.STRING, r.id().toString());
            d.setPersistent(true);
        });
    }

    /** Tras mejorar: cambia el modelo o el bloque al del nuevo nivel. */
    public void refresh(WorkbenchRecord r) {
        show(r);
    }

    public void remove(WorkbenchRecord r) {
        Block b = block(r);
        if (b != null) {
            removeAround(b, r);
        }
    }

    /**
     * Al cargar las entidades: repone el display si la mesa usa modelo y le falta, y quita el que sobra si el
     * modelo se quitó de la configuración.
     */
    public void ensure(WorkbenchRecord r) {
        Block b = block(r);
        if (b == null) {
            return;
        }
        if (!usesModel(r)) {
            removeAround(b, r);
        } else if (nearby(b, r).isEmpty()) {
            show(r);
        }
    }

    /** Al cargar entidades: quita los displays de mesas que ya no están activas (huérfanas o borradas). */
    public void removeStray(Iterable<? extends Entity> entities, Predicate<UUID> active) {
        for (Entity e : entities) {
            if (!(e instanceof ItemDisplay d)) {
                continue;
            }
            String id = d.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING);
            if (id == null) {
                continue;
            }
            try {
                if (!active.test(UUID.fromString(id))) {
                    d.remove();
                }
            } catch (IllegalArgumentException ignored) {
                // id ilegible: no es nuestro formato, se deja
            }
        }
    }

    private static Location center(Block b) {
        return b.getLocation().add(0.5, 0.5, 0.5);
    }

    private java.util.Collection<ItemDisplay> nearby(Block b, WorkbenchRecord r) {
        String id = r.id().toString();
        return b.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center(b), 0.6).stream()
                .filter(d -> id.equals(d.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING)))
                .toList();
    }

    /** Nunca más de un display por mesa: se borran los que ya tengan su id antes de crear. */
    private void removeAround(Block b, WorkbenchRecord r) {
        nearby(b, r).forEach(ItemDisplay::remove);
    }
}
