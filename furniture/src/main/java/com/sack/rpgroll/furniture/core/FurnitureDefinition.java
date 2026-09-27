package com.sack.rpgroll.furniture.core;

import com.sack.rpgroll.furniture.function.FurnitureFunctions;

import org.bukkit.DyeColor;
import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Un mueble tal como está en {@code furniture/*.yml}.
 *
 * @param variants   las versiones en orden; vacío si el mueble tiene un solo aspecto
 * @param recipe     receta del carpintero, o null si no se fabrica (recompensa o tienda)
 * @param permission permiso para colocarlo, o null
 */
public record FurnitureDefinition(
        String id,
        String category,
        String name,
        List<String> lore,
        Material material,
        String itemModel,
        Map<String, FurnitureVariant> variants,
        Placement placement,
        Hitbox hitbox,
        DisplaySettings display,
        FurnitureFunctions functions,
        CarpenterRecipe recipe,
        String permission,
        FurnitureSounds sounds) {

    public FurnitureDefinition {
        lore = List.copyOf(lore);
        variants = Collections.unmodifiableMap(new LinkedHashMap<>(variants));
    }

    public Optional<FurnitureVariant> variant(String variantId) {
        return variantId == null ? Optional.empty() : Optional.ofNullable(variants.get(variantId));
    }

    /** La variante pedida si existe, si no la primera; null si el mueble no tiene variantes. */
    public String resolveVariant(String variantId) {

        if (variants.isEmpty()) {
            return null;
        }
        if (variantId != null && variants.containsKey(variantId)) {
            return variantId;
        }
        return variants.keySet().iterator().next();
    }

    /** La variante que aplica un tinte, si alguna lo tiene. */
    public Optional<FurnitureVariant> variantForDye(DyeColor dye) {
        return variants.values().stream().filter(v -> v.dye() == dye).findFirst();
    }

    /** El modelo de una variante (o el del mueble) en un estado. */
    public String model(String variantId, int state) {

        String base = variant(variantId).map(FurnitureVariant::itemModel).orElse(itemModel);
        return base + functions.stateSuffix(state);
    }

    public String displayName(String variantId) {
        return variant(variantId).map(FurnitureVariant::name).filter(n -> !n.isBlank()).orElse(name);
    }

    public Optional<CarpenterRecipe> recipe(String variantId) {
        return variant(variantId).map(FurnitureVariant::recipe).or(() -> Optional.ofNullable(recipe));
    }
}
