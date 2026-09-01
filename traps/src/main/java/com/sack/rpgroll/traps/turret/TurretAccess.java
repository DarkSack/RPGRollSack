package com.sack.rpgroll.traps.turret;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Quién puede abrir y reabastecer una torreta.
 * <p>
 * El dueño siempre; sus compañeros de team o de guild también, para que una
 * defensa comunitaria no dependa de que su dueño esté conectado; y un admin,
 * que lo necesita para mantenimiento.
 * <p>
 * RPGRoll-Guilds es dependencia blanda: si no está instalado, la
 * comprobación cae limpiamente a "solo el dueño" en vez de fallar.
 */
public final class TurretAccess {

    private static final String ADMIN_PERMISSION = "rpgrolltraps.admin.*";

    private TurretAccess() {
    }

    /**
     * ¿Cuenta como aliado de la torreta? El dueño y quienes comparten su
     * team o guild. Sin RPGRoll-Guilds, solo el dueño.
     * <p>
     * No se usa {@link #canManage} para esto a propósito: un admin puede
     * administrar cualquier torreta, pero eso no lo vuelve aliado de todas
     * — si no, ningún admin podría probar una torreta anti-jugador.
     */
    public static boolean isAlly(Player player, PlacedTurret placed) {

        UUID owner = placed.owner();

        if (owner == null) {
            return false;
        }

        return owner.equals(player.getUniqueId()) || sharesTeamOrGuild(player.getUniqueId(), owner);
    }

    public static boolean canManage(Player player, PlacedTurret placed) {

        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }

        UUID owner = placed.owner();

        if (owner == null) {
            // Colocada por comando, sin dueño: solo admin.
            return false;
        }

        if (owner.equals(player.getUniqueId())) {
            return true;
        }

        return sharesTeamOrGuild(player.getUniqueId(), owner);
    }

    /**
     * Se resuelve por reflexión sobre la API de Guilds a propósito: así
     * Traps no necesita compilar contra ese módulo, y quien compre Traps sin
     * Guilds no ve ningún error — simplemente no hay acceso compartido.
     */
    private static boolean sharesTeamOrGuild(UUID player, UUID owner) {

        if (Bukkit.getPluginManager().getPlugin("RPGRoll-Guilds") == null) {
            return false;
        }

        try {
            Class<?> api = Class.forName("com.sack.rpgroll.guilds.GuildsAPI");

            if (!(boolean) api.getMethod("isReady").invoke(null)) {
                return false;
            }

            return sameGroup(api.getMethod("getGuildManager").invoke(null), "findByMember", player, owner)
                    || sameGroup(api.getMethod("getTeamManager").invoke(null), "getTeam", player, owner);

        } catch (ReflectiveOperationException | ClassCastException e) {
            // La API cambió o no está: se degrada a solo el dueño.
            return false;
        }
    }

    /** ¿El lookup devuelve el mismo grupo para ambos jugadores? */
    private static boolean sameGroup(Object manager, String method, UUID player, UUID owner)
            throws ReflectiveOperationException {

        var lookup = manager.getClass().getMethod(method, UUID.class);

        Object a = lookup.invoke(manager, player);
        Object b = lookup.invoke(manager, owner);

        if (!(a instanceof java.util.Optional<?> first) || !(b instanceof java.util.Optional<?> second)) {
            return false;
        }

        return first.isPresent() && second.isPresent() && first.get().equals(second.get());
    }

}
