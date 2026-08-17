package com.sack.rpgroll.crafting.ingredient;

import com.sack.rpgroll.crafting.item.ItemIdentity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resuelve si un {@link ItemStack} concreto cumple un {@link IngredientSpec},
 * y sabe contar/consumir ingredientes a través de un inventario (mesa de
 * crafteo personalizada, contenedor de estación, etc).
 */
public class IngredientMatcher {

    private final QualityRankResolver qualityRankResolver;

    public IngredientMatcher(QualityRankResolver qualityRankResolver) {
        this.qualityRankResolver = qualityRankResolver != null ? qualityRankResolver : QualityRankResolver.ALWAYS_ZERO;
    }

    public boolean matches(ItemStack stack, IngredientSpec spec) {

        if (stack == null || stack.getType() == Material.AIR || spec == null) {
            return false;
        }

        boolean typeMatches = switch (spec.type()) {
            case ANY -> true;
            case MATERIAL -> matchesMaterial(stack, spec.value());
            case TAG -> matchesTag(stack, spec.value());
            case ITEM_ID -> ItemIdentity.matchesId(stack, spec.value());
        };

        if (!typeMatches) {
            return false;
        }

        if (spec.hasMinQuality()) {
            int required = qualityRankResolver.rankOf(spec.minQuality());
            int actual = ItemIdentity.readQuality(stack).map(qualityRankResolver::rankOf).orElse(0);
            return actual >= required;
        }

        return true;
    }

    /**
     * Igual que {@link #matches}, pero además exige que la pila alcance
     * {@code spec.amount()} — para motores que miran UN slot fijo (Yunque,
     * Fermentación, Amoladora, Cartografía, Telar) en vez de sumar por todo
     * un inventario como {@link #countAvailable}. {@code matches} por sí solo
     * nunca mira la cantidad, así que usarlo directo sobre un único slot deja
     * pasar recetas con {@code amount > 1} aunque la pila tenga menos.
     */
    public boolean matchesWithAmount(ItemStack stack, IngredientSpec spec) {
        return matches(stack, spec) && stack.getAmount() >= spec.amount();
    }

    private boolean matchesMaterial(ItemStack stack, String materialName) {
        Material material = parseMaterial(materialName);
        return material != null && stack.getType() == material;
    }

    private boolean matchesTag(ItemStack stack, String tagName) {

        NamespacedKey key = parseKey(tagName);
        if (key == null) {
            return false;
        }

        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
        return tag != null && tag.isTagged(stack.getType());
    }

    /** Cuenta cuántas unidades del ingrediente hay disponibles en un inventario. */
    public int countAvailable(Inventory inventory, IngredientSpec spec) {

        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (matches(stack, spec)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /** Devuelve true y consume si hay suficiente cantidad; no toca el inventario si falta algo. */
    public boolean tryConsume(Inventory inventory, IngredientSpec spec) {

        if (countAvailable(inventory, spec) < spec.amount()) {
            return false;
        }

        int remaining = spec.amount();
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {

            ItemStack stack = contents[i];
            if (!matches(stack, spec)) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;

            if (stack.getAmount() <= 0) {
                inventory.setItem(i, null);
            }
        }

        return true;
    }

    /**
     * Igual que {@link #tryConsume}, pero además devuelve una copia exacta de
     * lo que se sacó (mismo Material/PDC/calidad, en los tamaños de pila
     * reales que se tomaron) — para poder devolverlo tal cual si hace falta,
     * ej. un crafteo que falla con {@code fail-consumes-ingredients: false}.
     * Lista vacía y el inventario intacto si no había suficiente.
     */
    public List<ItemStack> tryConsumeAndCapture(Inventory inventory, IngredientSpec spec) {

        if (countAvailable(inventory, spec) < spec.amount()) {
            return List.of();
        }

        List<ItemStack> captured = new ArrayList<>();
        int remaining = spec.amount();
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {

            ItemStack stack = contents[i];
            if (!matches(stack, spec)) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            ItemStack taken = stack.clone();
            taken.setAmount(take);
            captured.add(taken);

            stack.setAmount(stack.getAmount() - take);
            remaining -= take;

            if (stack.getAmount() <= 0) {
                inventory.setItem(i, null);
            }
        }

        return captured;
    }

    private Material parseMaterial(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private NamespacedKey parseKey(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            return new NamespacedKey(parts[0], parts[1]);
        }

        return NamespacedKey.minecraft(trimmed);
    }

}
