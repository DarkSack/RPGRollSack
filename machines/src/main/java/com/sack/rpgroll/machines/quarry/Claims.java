package com.sack.rpgroll.machines.quarry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ¿Está un área entera dentro de un claim de GriefPrevention de este jugador? Por reflexión,
 * para no depender de GriefPrevention al compilar ni exigirlo al instalar.
 * <p>
 * Los claims son rectángulos, así que basta con que las cuatro esquinas caigan en el mismo
 * claim (el de arriba, si alguna cae en un subclaim) y que su dueño sea el jugador.
 */
public final class Claims {

    private final Logger logger;
    private Object dataStore;
    private Method getClaimAt;
    private Class<?> claimClass;
    private Field parentField;
    private Method ownerMethod;
    private boolean broken;

    public Claims(Logger logger) {
        this.logger = logger;
    }

    /** Si GriefPrevention está y se le puede preguntar. */
    public boolean available() {
        return resolve();
    }

    private boolean resolve() {

        if (broken) {
            return false;
        }
        if (dataStore != null) {
            return true;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> main = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention", true, loader);
            claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim", true, loader);
            Object instance = main.getField("instance").get(null);
            Object store = main.getField("dataStore").get(instance);
            getClaimAt = store.getClass().getMethod("getClaimAt", Location.class, boolean.class, claimClass);
            parentField = claimClass.getField("parent");
            ownerMethod = claimClass.getMethod("getOwnerID");
            dataStore = store;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            logger.warning("✘ No se pudo hablar con GriefPrevention (" + e + "): las canteras no comprobarán claims.");
            return false;
        }
    }

    /**
     * true si el rectángulo [minX..maxX] × [minZ..maxZ] está entero en un claim de {@code owner};
     * false si no. Sin GriefPrevention, true.
     */
    public boolean owns(UUID owner, World world, int y, int minX, int minZ, int maxX, int maxZ) {

        if (!resolve()) {
            return true;
        }
        try {
            Object claim = null;
            int[][] corners = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}};
            for (int[] corner : corners) {
                Object here = top(getClaimAt.invoke(dataStore, new Location(world, corner[0], y, corner[1]), true, null));
                if (here == null || (claim != null && here != claim)) {
                    return false;
                }
                claim = here;
            }
            return owner.equals(ownerMethod.invoke(claim));
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.warning("✘ GriefPrevention falló al consultar un claim: " + e);
            return false;
        }
    }

    private Object top(Object claim) throws IllegalAccessException {
        Object current = claim;
        while (current != null) {
            Object parent = parentField.get(current);
            if (parent == null) {
                return current;
            }
            current = parent;
        }
        return null;
    }
}
