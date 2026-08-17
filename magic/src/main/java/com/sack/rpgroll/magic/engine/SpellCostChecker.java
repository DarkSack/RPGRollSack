package com.sack.rpgroll.magic.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.magic.core.SpellCost;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Chequea y descuenta el {@link SpellCost} de un hechizo. Mana/vida/
 * experiencia reusan el estado real del jugador de :core (CombatStats/
 * RPGPlayer) — no hay ningún estado de maná propio de Magic. El reactivo
 * descuenta un ítem real del inventario.
 * <p>
 * Nota: si el jugador ya está en el nivel máximo, {@code
 * RPGPlayer#addExperience} es un no-op — un hechizo con costo de
 * experiencia simplemente no se puede lanzar en el nivel máximo. Es una
 * limitación conocida del método de :core, no un bug de Magic.
 */
public final class SpellCostChecker {

    private SpellCostChecker() {
    }

    public static List<String> checkOnly(Player player, SpellCost cost, double costMultiplier, LangManager lang) {

        List<String> reasons = new ArrayList<>();

        int mana = (int) Math.round(cost.mana() * costMultiplier);
        int health = cost.health();
        int experience = cost.experience();

        if (mana > 0 || health > 0 || experience > 0) {

            if (!RPGRollAPI.isReady()) {
                reasons.add(lang.raw("cast.reason.rpgroll_not_ready"));
                return reasons;
            }

            var playerOpt = RPGRollAPI.get().getPlayer(player.getUniqueId());

            if (playerOpt.isEmpty()) {
                reasons.add(lang.raw("cast.reason.no_character"));
                return reasons;
            }

            RPGPlayer rpgPlayer = playerOpt.get();
            CombatStats stats = rpgPlayer.getCombatStats();

            if (mana > 0 && stats.currentMana() < mana) {
                reasons.add(lang.raw("cast.reason.not_enough_mana", "current", stats.currentMana(), "required", mana));
            }

            if (health > 0 && stats.currentHealth() <= health) {
                reasons.add(lang.raw("cast.reason.not_enough_health"));
            }

            if (experience > 0 && rpgPlayer.getExperience() < experience) {
                reasons.add(lang.raw("cast.reason.not_enough_experience"));
            }
        }

        if (cost.hasReagent()) {

            Material material = parseMaterial(cost.reagentMaterial());

            if (!hasReagent(player, material, cost.reagentAmount())) {
                reasons.add(lang.raw("cast.reason.missing_reagent", "amount", cost.reagentAmount(), "material",
                        material.name()));
            }
        }

        return reasons;
    }

    /** Asume que {@link #checkOnly} ya pasó — descuenta todo sin volver a validar. */
    public static void deduct(Player player, SpellCost cost, double costMultiplier) {

        int mana = (int) Math.round(cost.mana() * costMultiplier);
        int health = cost.health();
        int experience = cost.experience();

        if ((mana > 0 || health > 0 || experience > 0) && RPGRollAPI.isReady()) {

            var playerOpt = RPGRollAPI.get().getPlayer(player.getUniqueId());

            if (playerOpt.isPresent()) {

                RPGPlayer rpgPlayer = playerOpt.get();
                CombatStats stats = rpgPlayer.getCombatStats();

                CombatStats updated = stats;

                if (mana > 0) {
                    updated = updated.withMana(updated.currentMana() - mana);
                }

                if (health > 0) {
                    updated = updated.withHealth(updated.currentHealth() - health);
                }

                RPGPlayer updatedPlayer = rpgPlayer.updateCombatStats(updated);

                if (experience > 0) {
                    updatedPlayer = updatedPlayer.addExperience(-experience);
                }

                RPGRollAPI.get().getPlayerManager().savePlayer(updatedPlayer);
            }
        }

        if (cost.hasReagent()) {
            Material material = parseMaterial(cost.reagentMaterial());
            player.getInventory().removeItem(new ItemStack(material, cost.reagentAmount()));
        }
    }

    private static boolean hasReagent(Player player, Material material, int amount) {

        int total = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }

        return total >= amount;
    }

    private static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.AIR;
        }
    }

}
