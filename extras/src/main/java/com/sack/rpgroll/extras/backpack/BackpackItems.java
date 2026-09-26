package com.sack.rpgroll.extras.backpack;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Crea y reconoce las mochilas. Una mochila es una cabeza con dos marcas en
 * su PersistentDataContainer: el nivel y, desde la primera vez que se abre,
 * el UUID de su contenido. El contenido viaja con ese UUID —como en una
 * shulker— y no depende del nombre ni de la textura.
 */
public class BackpackItems {

    /** La marca con la que RPGRoll-Items identifica sus ítems (sin depender de ese plugin). */
    public static final NamespacedKey ITEMS_ID = new NamespacedKey("rpgroll-items", "item-id");

    private final NamespacedKey tierKey;
    private final NamespacedKey idKey;
    /** Solo en las ligadas: deja que otros plugins (la subasta) las reconozcan sin leer su archivo. */
    private final NamespacedKey boundKey;
    /** Solo en bloques: quién la puso en el suelo. */
    private final NamespacedKey placerKey;
    private BackpackSettings settings = BackpackSettings.DISABLED;

    public BackpackItems(Plugin plugin) {
        this.tierKey = new NamespacedKey(plugin, "backpack_tier");
        this.idKey = new NamespacedKey(plugin, "backpack_id");
        this.boundKey = new NamespacedKey(plugin, "backpack_bound");
        this.placerKey = new NamespacedKey(plugin, "backpack_placer");
    }

    public void configure(BackpackSettings settings) {
        this.settings = settings;
    }

    // ---------------------------------------------------------------- leer

    public boolean isBackpack(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(tierKey, PersistentDataType.STRING);
    }

    public Optional<String> tierId(ItemStack item) {
        return isBackpack(item) ? tierId(item.getItemMeta().getPersistentDataContainer()) : Optional.empty();
    }

    public Optional<String> tierId(PersistentDataContainer container) {
        return Optional.ofNullable(container.get(tierKey, PersistentDataType.STRING));
    }

    public Optional<UUID> id(ItemStack item) {
        return isBackpack(item) ? id(item.getItemMeta().getPersistentDataContainer()) : Optional.empty();
    }

    public Optional<UUID> id(PersistentDataContainer container) {

        String raw = container.get(idKey, PersistentDataType.STRING);

        if (raw == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> itemsId(ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(ITEMS_ID, PersistentDataType.STRING));
    }

    // ---------------------------------------------------------------- escribir

    /** Marca un bloque (o cualquier contenedor de datos) como la mochila {@code id} de nivel {@code tierId}. */
    public void mark(PersistentDataContainer container, String tierId, UUID id, UUID placer) {
        container.set(tierKey, PersistentDataType.STRING, tierId);
        container.set(idKey, PersistentDataType.STRING, id.toString());
        container.set(placerKey, PersistentDataType.STRING, placer.toString());
    }

    public Optional<UUID> placer(PersistentDataContainer container) {
        try {
            String raw = container.get(placerKey, PersistentDataType.STRING);
            return raw == null ? Optional.empty() : Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Le da UUID a una mochila que aún no lo tiene (recién fabricada). Modifica
     * el ItemStack recibido: el llamador debe volver a ponerlo en su casilla.
     */
    public UUID ensureId(ItemStack item) {

        Optional<UUID> existing = id(item);

        if (existing.isPresent()) {
            return existing.get();
        }

        UUID id = UUID.randomUUID();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
        return id;
    }

    /**
     * Una mochila nueva del nivel indicado.
     *
     * @param id        null para una recién fabricada (recibe UUID al abrirse)
     * @param ownerName null o vacío si no está ligada
     */
    public ItemStack create(BackpackTier tier, UUID id, String ownerName) {

        ItemStack item = ItemBuilder.skull(BackpackTextures.toBase64(tier.texture())).build();
        ItemMeta meta = item.getItemMeta();

        meta.displayName(text(fill(tier.name(), tier, ownerName)));
        meta.lore(lore(tier, ownerName));
        meta.setMaxStackSize(1);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(tierKey, PersistentDataType.STRING, tier.id());
        if (id != null) {
            container.set(idKey, PersistentDataType.STRING, id.toString());
        }
        if (ownerName != null && !ownerName.isEmpty()) {
            container.set(boundKey, PersistentDataType.BYTE, (byte) 1);
        }

        item.setItemMeta(meta);
        return item;
    }

    private List<Component> lore(BackpackTier tier, String ownerName) {

        List<Component> lines = new ArrayList<>();
        tier.lore().forEach(line -> lines.add(text(fill(line, tier, ownerName))));

        String ownerLine = ownerName == null || ownerName.isEmpty()
                ? settings.ownerLoreUnbound() : settings.ownerLoreBound();

        if (ownerLine != null && !ownerLine.isEmpty()) {
            lines.add(text(fill(ownerLine, tier, ownerName)));
        }

        return lines;
    }

    static String fill(String text, BackpackTier tier, String ownerName) {
        return text.replace("{slots}", String.valueOf(tier.slots()))
                .replace("{pages}", String.valueOf(BackpackPages.pages(tier.slots())))
                .replace("{tier}", tier.id())
                .replace("{owner}", ownerName == null ? "" : ownerName);
    }

    static Component text(String raw) {
        return ComponentUtils.parse(raw).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

}
