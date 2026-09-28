package com.sack.rpgroll.fishing.item;

import org.bukkit.NamespacedKey;

public final class FishingItemKeys {

    public static final NamespacedKey ROD_ID = new NamespacedKey("rpgrollfishing", "rod-id");
    public static final NamespacedKey BAIT_ID = new NamespacedKey("rpgrollfishing", "bait-id");

    // Un pez pescado: para que tiendas, misiones u otros addons sepan qué es y cuánto vale.
    public static final NamespacedKey SPECIES_ID = new NamespacedKey("rpgrollfishing", "species-id");
    public static final NamespacedKey QUALITY = new NamespacedKey("rpgrollfishing", "quality");
    public static final NamespacedKey WEIGHT = new NamespacedKey("rpgrollfishing", "weight");
    public static final NamespacedKey LENGTH = new NamespacedKey("rpgrollfishing", "length");
    public static final NamespacedKey PRICE = new NamespacedKey("rpgrollfishing", "price");

    private FishingItemKeys() {
    }

}
