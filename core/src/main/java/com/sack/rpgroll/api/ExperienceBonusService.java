package com.sack.rpgroll.api;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

/**
 * Bonos a la experiencia de personaje que se gana jugando (mobs, recompensas
 * de quests, recetas). Los comandos de admin no pasan por aquí.
 * <p>
 * El bono total es la suma, en porcentaje, de:
 * <ul>
 * <li>el mayor permiso {@code rpgroll.exp.bonus.<n>} que tenga el jugador
 * (rangos: {@code rpgroll.exp.bonus.10} = +10%). No se acumulan entre sí,
 * así un rango que hereda de otro no suma los dos.</li>
 * <li>lo que registren los addons con {@link #registerSource}, por ejemplo
 * el prestigio de RPGRoll-Ascension.</li>
 * </ul>
 */
public final class ExperienceBonusService {

    public static final String PERMISSION_PREFIX = "rpgroll.exp.bonus.";

    private final Map<String, ToDoubleFunction<Player>> sources = new ConcurrentHashMap<>();

    /** Registra (o reemplaza) una fuente de bono en porcentaje; {@code id} la identifica para quitarla. */
    public void registerSource(String id, ToDoubleFunction<Player> bonusPercent) {
        sources.put(id, bonusPercent);
    }

    public void unregisterSource(String id) {
        sources.remove(id);
    }

    public double bonusPercent(Player player) {

        double total = permissionBonus(player);

        for (ToDoubleFunction<Player> source : sources.values()) {
            total += source.applyAsDouble(player);
        }

        return total;
    }

    /** La experiencia {@code baseExp} con el bono del jugador aplicado. Nunca baja de la base. */
    public int boost(Player player, int baseExp) {

        if (baseExp <= 0) {
            return baseExp;
        }

        double bonus = Math.max(0, bonusPercent(player));
        return (int) Math.round(baseExp * (1 + bonus / 100.0));
    }

    static double permissionBonus(Player player) {

        double best = 0;

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {

            String permission = info.getPermission().toLowerCase(Locale.ROOT);

            if (!info.getValue() || !permission.startsWith(PERMISSION_PREFIX)) {
                continue;
            }

            try {
                best = Math.max(best, Double.parseDouble(permission.substring(PERMISSION_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // rpgroll.exp.bonus.* u otro comodín: no dice cuánto, se ignora.
            }
        }

        return best;
    }

}
