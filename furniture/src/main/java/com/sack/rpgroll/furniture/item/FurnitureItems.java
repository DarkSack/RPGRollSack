package com.sack.rpgroll.furniture.item;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.furniture.FurnitureKeys;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.core.FurnitureManager;
import com.sack.rpgroll.furniture.core.Surface;
import com.sack.rpgroll.furniture.function.FurnitureFunctions;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * El ítem de un mueble (el que se coloca) y el que dibuja su ItemDisplay.
 * <p>
 * Los dos llevan el mismo material y el {@code item_model} de la variante: Geyser reconoce el
 * ítem de Bedrock por esa pareja, así que el mueble colocado se ve igual en los dos clientes.
 */
public final class FurnitureItems {

    /** Lo que identifica un ítem de mueble. */
    public record Ref(String furnitureId, String variant) {
    }

    private final FurnitureKeys keys;
    private final FurnitureManager manager;
    private final LangManager lang;

    public FurnitureItems(FurnitureKeys keys, FurnitureManager manager, LangManager lang) {
        this.keys = keys;
        this.manager = manager;
        this.lang = lang;
    }

    public ItemStack create(FurnitureDefinition def, String variantId, int amount) {

        String variant = def.resolveVariant(variantId);
        ItemStack item = new ItemStack(def.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        meta.setItemModel(NamespacedKey.fromString(def.model(variant, 0)));
        meta.displayName(plain(ComponentUtils.parse(def.displayName(variant))));

        List<Component> lore = new ArrayList<>();
        def.lore().forEach(line -> lore.add(plain(ComponentUtils.parse(line))));
        if (!def.lore().isEmpty()) {
            lore.add(Component.empty());
        }
        lore.addAll(describe(def));
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.id, PersistentDataType.STRING, def.id());
        if (variant != null) {
            pdc.set(keys.variant, PersistentDataType.STRING, variant);
        }

        item.setItemMeta(meta);
        return item;
    }

    /** El ítem que muestra el ItemDisplay: solo el modelo, sin nombre ni PDC. */
    public ItemStack displayItem(FurnitureDefinition def, String variant, int state) {

        ItemStack item = new ItemStack(def.material());
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString(def.model(variant, state)));
        item.setItemMeta(meta);
        return item;
    }

    public Optional<Ref> read(ItemStack item) {

        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return Optional.empty();
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(keys.id, PersistentDataType.STRING);
        return id == null ? Optional.empty() : Optional.of(new Ref(id, pdc.get(keys.variant, PersistentDataType.STRING)));
    }

    public Optional<FurnitureDefinition> definition(ItemStack item) {
        return read(item).flatMap(ref -> manager.get(ref.furnitureId()));
    }

    /** Las líneas que explican qué hace el mueble y dónde va. */
    public List<Component> describe(FurnitureDefinition def) {

        List<Component> lines = new ArrayList<>();
        List<String> surfaces = new ArrayList<>();
        for (Surface surface : Surface.values()) {
            if (def.placement().allows(surface)) {
                surfaces.add(lang.raw("item.surface." + surface.name().toLowerCase()));
            }
        }
        lines.add(plain(lang.component("item.placed_on", "surfaces", String.join(", ", surfaces))));

        FurnitureFunctions f = def.functions();
        if (f.seat() != null) {
            lines.add(plain(lang.component("item.function.seat", "seats", f.seat().positions().size())));
        }
        if (f.storage() != null) {
            lines.add(plain(lang.component("item.function.storage", "slots", f.storage().rows() * 9)));
        }
        if (f.trash() != null) {
            lines.add(plain(lang.component("item.function.trash")));
        }
        if (f.hasLight()) {
            lines.add(plain(lang.component("item.function.light")));
        }
        if (f.workstation() != null) {
            lines.add(plain(lang.component("item.function.workstation")));
        }
        if (f.shelf() != null) {
            lines.add(plain(lang.component("item.function.shelf", "slots", f.shelf().slots().size())));
        }
        if (f.states() != null) {
            lines.add(plain(lang.component("item.function.states")));
        }
        if (def.variants().values().stream().anyMatch(v -> v.dye() != null)) {
            lines.add(plain(lang.component("item.function.dye")));
        }
        return lines;
    }

    private static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
