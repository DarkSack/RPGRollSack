package com.sack.rpgroll.structures.protect;

import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.storage.Box;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo común de WorldGuard por reflexión (sin depender de él al compilar), para la protección de la zona y para las
 * parcelas: el {@code RegionManager} de un mundo, cuboides sobre una caja y las regiones que la cruzan.
 * <p>
 * Doc oficial leída (WorldGuard 7.0.18, la del servidor; fuentes de esa etiqueta: {@code RegionContainer.java},
 * {@code RegionManager.java}, {@code RegionQuery.java}):
 * https://worldguard.enginehub.org/en/latest/developer/regions/spatial-queries/ ,
 * https://worldguard.enginehub.org/en/latest/developer/regions/protected-region/ y las fuentes de
 * https://github.com/EngineHub/WorldGuard :
 * <ul>
 * <li>{@code WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world))} da el
 * {@code RegionManager} (null si las regiones están apagadas en el mundo);</li>
 * <li>{@code new ProtectedCuboidRegion(String id, BlockVector3 min, BlockVector3 max)} con
 * {@code BlockVector3.at(x, y, z)};</li>
 * <li>{@code RegionManager.getApplicableRegions(ProtectedRegion, RegionQuery.QueryOption.NONE)}: las regiones que
 * cruzan la caja, sin añadir padres que no la tocan; {@code getRegion("__global__")} es la global.</li>
 * </ul>
 * Todos los métodos se buscan por los tipos declarados (interfaces públicas), no por la clase de la implementación.
 */
public final class WorldGuardApi {

    public static final String GLOBAL = "__global__";

    private final ClassLoader loader;
    private final Object container, noParents;
    private final Method adaptWorld, getManager, applicable, getRegion, at;
    private final Constructor<?> cuboid;
    private final Class<?> regionClass;

    private WorldGuardApi(ClassLoader cl) throws ReflectiveOperationException {
        loader = cl;
        Class<?> wg = type("com.sk89q.worldguard.WorldGuard");
        Class<?> adapter = type("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Class<?> weWorld = type("com.sk89q.worldedit.world.World");
        Class<?> vector = type("com.sk89q.worldedit.math.BlockVector3");
        regionClass = type("com.sk89q.worldguard.protection.regions.ProtectedRegion");
        Class<?> cuboidClass = type("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion");
        Class<?> option = type("com.sk89q.worldguard.protection.regions.RegionQuery$QueryOption");
        Method getPlatform = wg.getMethod("getPlatform");
        Object platform = getPlatform.invoke(wg.getMethod("getInstance").invoke(null));
        Method getContainer = getPlatform.getReturnType().getMethod("getRegionContainer");
        container = getContainer.invoke(platform);
        getManager = getContainer.getReturnType().getMethod("get", weWorld);
        applicable = getManager.getReturnType().getMethod("getApplicableRegions", regionClass, option);
        getRegion = getManager.getReturnType().getMethod("getRegion", String.class);
        noParents = option.getField("NONE").get(null);
        adaptWorld = adapter.getMethod("adapt", World.class);
        at = vector.getMethod("at", int.class, int.class, int.class);
        cuboid = cuboidClass.getConstructor(String.class, vector, vector);
    }

    /** Con el cargador de clases del plugin; lanza si no está cargado o algo no casa. */
    public static WorldGuardApi resolve() throws ReflectiveOperationException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(Hook.WORLDGUARD.plugin());
        if (plugin == null) {
            throw new IllegalStateException("WorldGuard no está cargado");
        }
        return new WorldGuardApi(plugin.getClass().getClassLoader());
    }

    /** Clase de WorldGuard o WorldEdit por su nombre completo. */
    public Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }

    /** {@code ProtectedRegion}. */
    public Class<?> regionType() {
        return regionClass;
    }

    /** Tipo declarado de {@code RegionManager}. */
    public Class<?> managerType() {
        return getManager.getReturnType();
    }

    /** Tipo declarado de {@code ApplicableRegionSet}. */
    public Class<?> applicableType() {
        return applicable.getReturnType();
    }

    /** {@code RegionManager} del mundo; null si las regiones están apagadas ahí. */
    public Object manager(World world) throws ReflectiveOperationException {
        return getManager.invoke(container, adaptWorld.invoke(null, world));
    }

    /** {@code new ProtectedCuboidRegion(id, min, max)} sobre la caja. */
    public Object cuboid(String id, Box box) throws ReflectiveOperationException {
        return cuboid.newInstance(id, at.invoke(null, box.minX(), box.minY(), box.minZ()),
                at.invoke(null, box.maxX(), box.maxY(), box.maxZ()));
    }

    /** Regiones que cruzan la caja, sin padres que no la tocan. */
    public List<Object> crossing(Object manager, Box box) throws ReflectiveOperationException {
        List<Object> out = new ArrayList<>();
        for (Object r : (Iterable<?>) applicable.invoke(manager, cuboid("rpgrollstructures-dummy", box), noParents)) {
            out.add(r);
        }
        return out;
    }

    /** La región {@code __global__} del manager (puede ser null). */
    public Object global(Object manager) throws ReflectiveOperationException {
        return getRegion.invoke(manager, GLOBAL);
    }
}
