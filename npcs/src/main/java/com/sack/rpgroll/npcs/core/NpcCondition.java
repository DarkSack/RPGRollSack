package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Evalúa condiciones sobre el jugador, usadas por NpcAction.CONDITIONAL.
 * <p>
 * Soporta condiciones simples ("HAS_JOB:minero") y compuestas usando
 * "&&" (todas deben cumplirse) o "||" (al menos una debe cumplirse).
 * No se permite mezclar && y || en la misma expresión — si el string
 * contiene ambos, se evalúa solo el primero que aparece de izquierda a
 * derecha y se ignora el resto (evita ambigüedad de precedencia sin
 * necesidad de implementar un parser completo de paréntesis).
 */
public class NpcCondition {

    public static boolean evaluate(Player player, String rawCondition) {

        String trimmed = rawCondition.trim();

        if (trimmed.contains("&&")) {
            return evaluateAll(player, trimmed.split("&&"));
        }

        if (trimmed.contains("||")) {
            return evaluateAny(player, trimmed.split("\\|\\|"));
        }

        return evaluateSingle(player, trimmed);
    }

    private static boolean evaluateAll(Player player, String[] conditions) {
        for (String condition : conditions) {
            if (!evaluateSingle(player, condition.trim())) {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluateAny(Player player, String[] conditions) {
        for (String condition : conditions) {
            if (evaluateSingle(player, condition.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean evaluateSingle(Player player, String rawCondition) {

        String[] parts = rawCondition.split(":", 2);

        if (parts.length < 1) {
            return false;
        }

        String type = parts[0].trim().toUpperCase();
        String param = parts.length > 1 ? parts[1].trim() : "";

        return switch (type) {
            case "HAS_JOB" -> hasJob(player, param);
            case "MIN_JOB_LEVEL" -> minJobLevel(player, param);
            case "MIN_LEVEL" -> minPlayerLevel(player, param);
            case "HAS_RACE" -> hasRace(player, param);
            case "HAS_CLASS" -> hasClass(player, param);
            case "HAS_ITEM" -> hasItem(player, param);
            default -> false;
        };
    }

    private static boolean hasJob(Player player, String jobId) {
        return getRpgPlayer(player)
                .map(rpg -> rpg.getJobs().hasJob(jobId))
                .orElse(false);
    }

    private static boolean minJobLevel(Player player, String param) {

        String[] parts = param.split(",", 2);
        if (parts.length != 2) {
            return false;
        }

        String jobId = parts[0].trim();
        int required;

        try {
            required = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return false;
        }

        return getRpgPlayer(player)
                .map(rpg -> rpg.getJobs().getLevel(jobId) >= required)
                .orElse(false);
    }

    private static boolean minPlayerLevel(Player player, String param) {

        int required;

        try {
            required = Integer.parseInt(param);
        } catch (NumberFormatException e) {
            return false;
        }

        return getRpgPlayer(player)
                .map(rpg -> rpg.getLevel() >= required)
                .orElse(false);
    }

    private static boolean hasRace(Player player, String raceId) {
        return getRpgPlayer(player)
                .map(rpg -> raceId.equalsIgnoreCase(rpg.getRace()))
                .orElse(false);
    }

    private static boolean hasClass(Player player, String classId) {
        return getRpgPlayer(player)
                .map(rpg -> classId.equalsIgnoreCase(rpg.getPlayerClass()))
                .orElse(false);
    }

    /**
     * Verifica si el jugador lleva un material específico, ya sea en la
     * mano principal o en cualquier parte del inventario.
     * Formato: "HAS_ITEM:IRON_PICKAXE" o "HAS_ITEM:IRON_PICKAXE,1" (cantidad
     * mínima).
     */
    private static boolean hasItem(Player player, String param) {

        String[] parts = param.split(",", 2);
        Material material;

        try {
            material = Material.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        int requiredAmount = 1;

        if (parts.length >= 2) {
            try {
                requiredAmount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        int total = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }

        return total >= requiredAmount;
    }

    private static Optional<RPGPlayer> getRpgPlayer(Player player) {
        return RPGRollAPI.get().getPlayer(player.getUniqueId());
    }

}