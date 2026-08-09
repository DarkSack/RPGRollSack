package com.sack.rpgroll.crafting.vanilla;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Receta registrada directamente en el sistema de crafteo nativo de Bukkit
 * (mesa de crafteo, familia de hornos, cortadora de piedra, mesa de
 * herrería) — a diferencia de {@code CustomRecipe}, que solo corre dentro de
 * una {@code CustomStation} propia. El resultado puede ser un material
 * vanilla o un ítem personalizado de RPGRoll-Items (ver {@code RecipeResult}).
 * <p>
 * Las condiciones solo se aplican a recetas de mesa de crafteo
 * ({@code CRAFTING_TABLE_SHAPED}/{@code CRAFTING_TABLE_SHAPELESS}) —
 * Bukkit no ofrece un evento "antes de mostrar el resultado" genérico para
 * hornos/cortadora/herrería, así que esas se registran sin gating por jugador.
 *
 * @param shape       patrón 3x3 (solo SHAPED)
 * @param key         letra -> material (solo SHAPED)
 * @param ingredients lista de materiales (SHAPELESS/cocción/cortadora); para
 *                    SMITHING_TRANSFORM, el primer elemento es la "adición"
 * @param templateMaterial plantilla de herrería (solo SMITHING_TRANSFORM)
 * @param baseMaterial     base a transformar (solo SMITHING_TRANSFORM)
 * @param cookingTimeTicks tiempo de cocción (familia de hornos)
 * @param experience       xp vanilla otorgada al recoger el resultado del horno
 */
public record VanillaRecipeDefinition(
        String id,
        VanillaStationType type,
        List<String> shape,
        Map<String, String> key,
        List<String> ingredients,
        String templateMaterial,
        String baseMaterial,
        int cookingTimeTicks,
        float experience,
        RecipeResult result,
        List<RecipeCondition> conditions) implements RPGContent {

    public VanillaRecipeDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(type, "type no puede ser null");
        Objects.requireNonNull(result, "result no puede ser null");
        shape = shape == null ? List.of() : List.copyOf(shape);
        key = key == null ? Map.of() : Map.copyOf(key);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        cookingTimeTicks = Math.max(20, cookingTimeTicks);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

}
