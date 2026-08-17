package com.sack.rpgroll.crafting.crafter;

import com.sack.rpgroll.crafting.condition.ConditionType;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeDefinition;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Un bloque Crafter craftea solo, sin ningún jugador presente — así que
 * cualquier {@code VanillaRecipeDefinition} (mesa de crafteo shaped/shapeless)
 * cuyas condiciones dependan de UN jugador concreto (nivel/raza/clase/job/
 * permiso/bioma/guild) no puede evaluarse ahí, y bloquearla es más correcto
 * que dejarla pasar sin chequeo. Las condiciones que solo dependen del mundo
 * (WORLD/HOUR_RANGE/SEASON/WEATHER) sí podrían evaluarse con el mundo del
 * bloque, pero por ahora se tratan igual de estricto — es una limitación
 * conocida y documentada, no un bug.
 */
public class CrafterAutomationEngine implements Listener {

    private static final Set<ConditionType> PLAYER_DEPENDENT = EnumSet.of(
            ConditionType.LEVEL_MIN, ConditionType.RACE, ConditionType.CLASS, ConditionType.JOB_MIN,
            ConditionType.PERMISSION, ConditionType.BIOME, ConditionType.GUILD_MEMBER);

    private final Plugin plugin;
    private final VanillaRecipeManager vanillaRecipeManager;

    public CrafterAutomationEngine(Plugin plugin, VanillaRecipeManager vanillaRecipeManager) {
        this.plugin = plugin;
        this.vanillaRecipeManager = vanillaRecipeManager;
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {

        NamespacedKey key = event.getRecipe().getKey();
        if (!key.getNamespace().equals(plugin.getName().toLowerCase(Locale.ROOT))) {
            return; // no es una de nuestras recetas registradas — no nos concierne
        }

        Optional<VanillaRecipeDefinition> defOpt = vanillaRecipeManager.get(key.getKey());
        if (defOpt.isEmpty()) {
            return;
        }

        boolean hasPlayerDependentCondition = defOpt.get().conditions().stream()
                .anyMatch(condition -> PLAYER_DEPENDENT.contains(condition.type()));

        if (hasPlayerDependentCondition) {
            event.setCancelled(true);
        }
    }

}
