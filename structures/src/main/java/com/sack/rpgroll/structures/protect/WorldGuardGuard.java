package com.sack.rpgroll.structures.protect;

import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.storage.Box;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * WorldGuard por reflexión ({@link WorldGuardApi}, sin depender de él al compilar): regiones que cruzan la caja y,
 * para el dueño envuelto desde su {@link OfflinePlayer}, lo mismo que pregunta WorldGuard al poner o romper un bloque.
 * <p>
 * Doc oficial leída (WorldGuard 7.0.x, última etiqueta 7.0.20; fuentes de master 7.1.0-SNAPSHOT):
 * https://worldguard.enginehub.org/en/latest/developer/regions/spatial-queries/ ,
 * https://worldguard.enginehub.org/en/latest/developer/regions/protection-query/ ,
 * https://worldguard.enginehub.org/en/latest/developer/regions/flag-calculation/ y las fuentes de
 * https://github.com/EngineHub/WorldGuard :
 * <ul>
 * <li>{@code WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world))} da el
 * {@code RegionManager} (null si las regiones están apagadas en el mundo);</li>
 * <li>{@code RegionManager.getApplicableRegions(new ProtectedCuboidRegion("dummy", BlockVector3.at(..),
 * BlockVector3.at(..)), RegionQuery.QueryOption.NONE)}: las regiones que cruzan la caja, sin añadir padres que
 * no la tocan; {@code getRegion("__global__")} es la global;</li>
 * <li>{@code ProtectedRegion.getType()} ({@code RegionType.CUBOID}) y {@code contains(int, int, int)};</li>
 * <li>{@code new RegionResultSet(List<ProtectedRegion>, ProtectedRegion global)} (ordena la lista) y
 * {@code ApplicableRegionSet.queryState(RegionAssociable, StateFlag...)};</li>
 * <li>la fórmula de {@code RegionQuery.testBuild(.., associable, flags)}, la que usa
 * {@code RegionProtectionListener} con {@code Flags.BLOCK_PLACE} y {@code Flags.BLOCK_BREAK}:
 * {@code StateFlag.test(StateFlag.combine(StateFlag.denyToNone(queryState(BUILD)), queryState(flags)))};</li>
 * <li>{@code WorldGuardPlugin.inst().wrapOfflinePlayer(OfflinePlayer)}; {@code ProtectedRegion.isMember} cuenta
 * también los padres.</li>
 * </ul>
 * Naturaleza = permitido: sin regiones y con la global sin miembros, {@code FlagValueCalculator.getMembership} da
 * {@code NO_REGIONS} (la global sin miembros cuenta como PASSTHROUGH, {@code getEffectiveFlagOf}) y
 * {@code queryAllValues} cae al valor por defecto de BUILD ({@code new BuildFlag("build", true)} → ALLOW); BLOCK_PLACE
 * y BLOCK_BREAK por defecto valen null ({@code StateFlag.getDefault}), así que la combinación queda en ALLOW.
 * <p>
 * Los grupos de permisos del dueño no cuentan: el jugador envuelto desconectado no los tiene.
 */
final class WorldGuardGuard implements AreaGuard {

    private static final Optional<Denial> DENIED = Optional.of(new Denial(Hook.WORLDGUARD, "protect.worldguard"));

    private WorldGuardApi wg;
    private Object wgPlugin, buildFlag, blockFlags;
    private Method queryState, wrapOffline, getType, contains, denyToNone, combine, test;
    private Constructor<?> resultSet;
    private Class<?> stateClass;

    @Override
    public Optional<Denial> check(UUID owner, String worldName, Box box) {
        try {
            resolve();
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return Optional.of(new Denial(Hook.WORLDGUARD, AreaGuards.ERROR));
            }
            Object regions = wg.manager(world);
            if (regions == null) {
                return Optional.empty(); // regiones apagadas en ese mundo
            }
            List<Object> crossing = wg.crossing(regions, box);
            Object global = wg.global(regions);
            Object who = wrapOffline.invoke(wgPlugin, Bukkit.getOfflinePlayer(owner));
            return allowed(crossing, r -> containsBox(r, box), set -> canBuild(set, global, who))
                    ? Optional.empty() : DENIED;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Método conservador sin mirar bloque a bloque: las regiones que CONTIENEN la caja entera (más la global) se
     * evalúan juntas, y cada región que solo la cruza en parte se evalúa con ellas. Basta un no para negar. Así una
     * región de más prioridad donde el dueño es miembro no tapa a otra que asoma por fuera de ella.
     *
     * @param allows evaluación de WorldGuard de un conjunto (la global la añade quien llama)
     */
    static <R> boolean allowed(Collection<R> crossing, Predicate<R> containsBox, Predicate<List<R>> allows) {
        List<R> base = crossing.stream().filter(containsBox).toList();
        if (!allows.test(base)) {
            return false;
        }
        for (R r : crossing) {
            if (!containsBox.test(r)) {
                List<R> with = new ArrayList<>(base);
                with.add(r);
                if (!allows.test(with)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * shortcut: solo un cuboide puede contener la caja; un polígono (puede ser cóncavo) cuenta siempre como cruce
     * parcial, lo que niega de más con parcelas poligonales dentro de una ciudad. Mirar sus aristas si molesta.
     */
    private boolean containsBox(Object region, Box box) {
        try {
            return "CUBOID".equals(((Enum<?>) getType.invoke(region)).name())
                    && (boolean) contains.invoke(region, box.minX(), box.minY(), box.minZ())
                    && (boolean) contains.invoke(region, box.maxX(), box.maxY(), box.maxZ());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Como {@code RegionQuery.testBuild(.., BLOCK_PLACE, BLOCK_BREAK)}, sobre un conjunto armado aquí. */
    private boolean canBuild(List<Object> regions, Object global, Object who) {
        try {
            Object set = resultSet.newInstance(new ArrayList<>(regions), global);
            Object states = Array.newInstance(stateClass, 2);
            Array.set(states, 0, denyToNone.invoke(null, queryState.invoke(set, who, buildFlag)));
            Array.set(states, 1, queryState.invoke(set, who, blockFlags));
            Object combined = Array.newInstance(stateClass, 1);
            Array.set(combined, 0, combine.invoke(null, states));
            return (boolean) test.invoke(null, combined);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void resolve() throws ReflectiveOperationException {
        if (wg != null) {
            return;
        }
        WorldGuardApi api = WorldGuardApi.resolve();
        Class<?> wgPluginClass = api.type("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
        Class<?> results = api.type("com.sk89q.worldguard.protection.RegionResultSet");
        Class<?> associable = api.type("com.sk89q.worldguard.protection.association.RegionAssociable");
        Class<?> stateFlag = api.type("com.sk89q.worldguard.protection.flags.StateFlag");
        Class<?> state = api.type("com.sk89q.worldguard.protection.flags.StateFlag$State");
        Class<?> flags = api.type("com.sk89q.worldguard.protection.flags.Flags");
        Class<?> regionClass = api.regionType();
        queryState = api.applicableType().getMethod("queryState", associable, stateFlag.arrayType());
        resultSet = results.getConstructor(List.class, regionClass);
        getType = regionClass.getMethod("getType");
        contains = regionClass.getMethod("contains", int.class, int.class, int.class);
        denyToNone = stateFlag.getMethod("denyToNone", state);
        combine = stateFlag.getMethod("combine", state.arrayType());
        test = stateFlag.getMethod("test", state.arrayType());
        stateClass = state;
        buildFlag = flagArray(stateFlag, flags, "BUILD");
        blockFlags = flagArray(stateFlag, flags, "BLOCK_PLACE", "BLOCK_BREAK");
        wrapOffline = wgPluginClass.getMethod("wrapOfflinePlayer", OfflinePlayer.class);
        wgPlugin = wgPluginClass.getMethod("inst").invoke(null);
        wg = api; // al final: si algo falla, se vuelve a intentar entero
    }

    private static Object flagArray(Class<?> stateFlag, Class<?> flags, String... names)
            throws ReflectiveOperationException {
        Object out = Array.newInstance(stateFlag, names.length);
        for (int i = 0; i < names.length; i++) {
            Array.set(out, i, flags.getField(names[i]).get(null));
        }
        return out;
    }
}
