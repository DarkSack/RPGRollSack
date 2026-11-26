package com.sack.rpgroll.structures.protect;

import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.storage.Box;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lo común de GriefPrevention por reflexión (patrón de {@code machines/quarry/Claims}), para la protección de la zona
 * y para las parcelas: el {@code DataStore} y los claims que cruzan el rectángulo XZ de una caja.
 * <p>
 * Doc oficial leída (GriefPrevention 16.18.8, última versión publicada; igual en master 18.0.0-SNAPSHOT): fuentes de
 * https://github.com/GriefPrevention/GriefPrevention ({@code GriefPrevention.java}, {@code Claim.java},
 * {@code DataStore.java}): {@code GriefPrevention.instance.dataStore}, {@code DataStore.getClaims(int chunkx, int
 * chunkz)} (claims de primer nivel de ese chunk), {@code Claim.inDataStore}, {@code Claim.children} y
 * {@code Claim.getLesserBoundaryCorner()/getGreaterBoundaryCorner()}.
 */
public final class GriefPreventionApi {

    private final ClassLoader loader;
    private final Object instance, dataStore;
    private final Method getClaims, lesser, greater;
    private final Field inDataStore, children;

    private GriefPreventionApi(ClassLoader loader) throws ReflectiveOperationException {
        this.loader = loader;
        Class<?> main = type("GriefPrevention");
        Class<?> claim = type("Claim");
        instance = main.getField("instance").get(null);
        dataStore = main.getField("dataStore").get(instance);
        getClaims = type("DataStore").getMethod("getClaims", int.class, int.class);
        lesser = claim.getMethod("getLesserBoundaryCorner");
        greater = claim.getMethod("getGreaterBoundaryCorner");
        inDataStore = claim.getField("inDataStore");
        children = claim.getField("children");
    }

    /** Con el cargador de clases del plugin; lanza si no está cargado o algo no casa. */
    public static GriefPreventionApi resolve() throws ReflectiveOperationException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(Hook.GRIEFPREVENTION.plugin());
        if (plugin == null) {
            throw new IllegalStateException("GriefPrevention no está cargado");
        }
        return new GriefPreventionApi(plugin.getClass().getClassLoader());
    }

    /** Clase de {@code me.ryanhamshire.GriefPrevention} por su nombre corto. */
    public Class<?> type(String simpleName) throws ClassNotFoundException {
        return Class.forName("me.ryanhamshire.GriefPrevention." + simpleName, true, loader);
    }

    /** {@code GriefPrevention.instance}. */
    public Object instance() {
        return instance;
    }

    public Object dataStore() {
        return dataStore;
    }

    /** Claims de primer nivel, vivos, que cruzan el rectángulo XZ de la caja en ese mundo (buscados por chunks). */
    public List<Object> crossing(String world, Box box) throws ReflectiveOperationException {
        Set<Object> claims = new LinkedHashSet<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                claims.addAll((Collection<?>) getClaims.invoke(dataStore, cx, cz));
            }
        }
        List<Object> out = new ArrayList<>();
        for (Object claim : claims) {
            if ((boolean) inDataStore.get(claim) && crosses(claim, world, box)) {
                out.add(claim);
            }
        }
        return out;
    }

    /** Subdivisiones del claim que cruzan la caja. */
    public List<Object> crossingChildren(Object claim, String world, Box box) throws ReflectiveOperationException {
        List<Object> out = new ArrayList<>();
        for (Object child : (Collection<?>) children.get(claim)) {
            if (crosses(child, world, box)) {
                out.add(child);
            }
        }
        return out;
    }

    private boolean crosses(Object claim, String world, Box box) throws ReflectiveOperationException {
        Location lo = (Location) lesser.invoke(claim), hi = (Location) greater.invoke(claim);
        World w = lo.getWorld();
        return w != null && w.getName().equals(world) && lo.getBlockX() <= box.maxX() && hi.getBlockX() >= box.minX()
                && lo.getBlockZ() <= box.maxZ() && hi.getBlockZ() >= box.minZ();
    }
}
