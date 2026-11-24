package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.config.WorkbenchSettings.TierSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Construye y lee el ítem de una mesa; la identidad vive en su PDC y la firma impide falsificarla. */
public final class WorkbenchItems {
    private final NamespacedKey idKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey serialKey;
    private final NamespacedKey sigKey;

    public WorkbenchItems(Plugin plugin) {
        this.idKey = new NamespacedKey(plugin, "workbench_id");
        this.tierKey = new NamespacedKey(plugin, "tier");
        this.serialKey = new NamespacedKey(plugin, "serial");
        this.sigKey = new NamespacedKey(plugin, "sig");
    }

    /** La mesa con su sello. {@code tier} deben ser los ajustes del nivel del sello (si no, IllegalArgumentException). */
    public ItemStack create(WorkbenchStamp stamp, LangManager lang, TierSettings tier) {
        return build(stamp, lang, tier, true);
    }

    /** La muestra de la receta: igual pero sin línea de serie (aún no tiene); dice que se firma al fabricarla. */
    public ItemStack sample(WorkbenchStamp stamp, LangManager lang, TierSettings tier) {
        return build(stamp, lang, tier, false);
    }

    private ItemStack build(WorkbenchStamp stamp, LangManager lang, TierSettings tier, boolean serialLine) {
        if (tier.tier() != stamp.tier()) {
            throw new IllegalArgumentException("Ajustes de " + tier.tier() + " para una mesa " + stamp.tier());
        }
        ItemStack item = new ItemStack(tier.placeableBlock());
        ItemMeta meta = item.getItemMeta();
        apply(meta, stamp, lang, tier, serialLine);
        item.setItemMeta(meta);
        return item;
    }

    void apply(ItemMeta meta, WorkbenchStamp stamp, LangManager lang, TierSettings tier, boolean serialLine) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(idKey, PersistentDataType.STRING, stamp.id().toString());
        pdc.set(tierKey, PersistentDataType.INTEGER, stamp.tier().level());
        pdc.set(serialKey, PersistentDataType.LONG, stamp.serial());
        pdc.set(sigKey, PersistentDataType.STRING, stamp.signature());
        // Una mesa por ítem: apiladas, N copias compartirían id y serie.
        meta.setMaxStackSize(1);
        String tierName = stamp.tier().name().toLowerCase(Locale.ROOT);
        meta.displayName(plain(lang.component("workbench.item.name." + tierName)));
        meta.lore(List.of(
                plain(lang.component("workbench.item.lore.size", "width", tier.maxSize().width(),
                        "height", tier.maxSize().height(), "length", tier.maxSize().length())),
                serialLine ? plain(lang.component("workbench.item.lore.serial", "serial", stamp.serial()))
                        : plain(lang.component("workbench.item.lore.sample")),
                plain(lang.component("workbench.item.lore.place"))));
        if (!tier.model().isEmpty()) {
            NamespacedKey model = NamespacedKey.fromString(tier.model());
            if (model != null) {
                meta.setItemModel(model);
            }
        }
    }

    /** Vacío si falta una clave o alguna no es válida. */
    public Optional<WorkbenchStamp> read(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        try {
            String id = pdc.get(idKey, PersistentDataType.STRING);
            Integer level = pdc.get(tierKey, PersistentDataType.INTEGER);
            Long serial = pdc.get(serialKey, PersistentDataType.LONG);
            String sig = pdc.get(sigKey, PersistentDataType.STRING);
            if (id == null || level == null || serial == null || sig == null) {
                return Optional.empty();
            }
            Optional<WorkbenchTier> tier = WorkbenchTier.ofLevel(level);
            if (tier.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new WorkbenchStamp(UUID.fromString(id), tier.get(), serial, sig));
        } catch (IllegalArgumentException e) {
            // Tipo guardado distinto (ítem falsificado con /give) o UUID inválido.
            return Optional.empty();
        }
    }

    public boolean isGenuine(WorkbenchStamp stamp, SerialSigner signer) {
        return signer.verify(stamp.id(), stamp.tier(), stamp.serial(), stamp.signature());
    }

    private static Component plain(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }
}
