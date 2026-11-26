package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.structures.protect.GriefPreventionApi;
import com.sack.rpgroll.structures.protect.WorldGuardApi;
import com.sack.rpgroll.structures.storage.Box;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Terreno del dueño sobre la caja de una parcela terminada. Hilo principal. Los adaptadores van por reflexión con
 * {@link GriefPreventionApi} y {@link WorldGuardApi}. Si la reflexión no casa (método o clase que no está, acceso)
 * lanzan {@link IllegalStateException} y quien llama marca la integración rota; si el plugin lanza con ESTE terreno
 * (o el mundo no está cargado) lanzan {@link Refused} y solo falla esta parcela.
 */
public interface ParcelClaims {

    /** Vacío si se puede crear; si no, la clave de idioma del motivo. */
    Optional<String> canCreate(UUID owner, String world, Box box);

    /** Crea el terreno y devuelve su id; vacío si el plugin no lo crea. */
    Optional<String> create(UUID owner, String world, Box box, String name);

    /** Borra el terreno que devolvió {@link #create}; si ya no está, nada. */
    void remove(String world, String createdId);

    /** El plugin rechazó o lanzó con este terreno: la integración sigue sana. */
    final class Refused extends RuntimeException {
        public Refused(Throwable cause) {
            super(cause);
        }
    }

    @FunctionalInterface
    interface Reflective<T> {
        T run() throws ReflectiveOperationException;
    }

    /** Lo que lanza el propio plugin ({@link InvocationTargetException}) es {@link Refused}; lo demás, reflexión rota. */
    static <T> T call(Reflective<T> r) {
        try {
            return r.run();
        } catch (InvocationTargetException e) {
            throw new Refused(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Un claim de GriefPrevention gasta tantos bloques como el área XZ de la caja. */
    static boolean enoughBlocks(int remaining, Box box) {
        return remaining >= area(box);
    }

    private static long area(Box box) {
        return (long) (box.maxX() - box.minX() + 1) * (box.maxZ() - box.minZ() + 1);
    }

    static World world(String name) {
        World w = Bukkit.getWorld(name);
        if (w == null) {
            throw new Refused(new IllegalStateException("El mundo " + name + " no está cargado"));
        }
        return w;
    }

    /**
     * Lo que GriefPrevention mira a mano al crear un claim (pala y {@code /claim}) y {@code createClaim} no.
     *
     * @param maxClaims   {@code config_claims_maxClaimsPerPlayer}; 0 = sin límite (o el dueño se lo salta)
     * @param ownedClaims claims que ya tiene el dueño
     * @param overlaps    algún claim cruza la caja
     * @param remaining   bloques de terreno libres del dueño
     */
    record GpFacts(boolean worldEnabled, int maxClaims, int ownedClaims, int minWidth, int minArea, boolean overlaps,
                   int remaining) {
    }

    /** Las reglas de GP en el orden de la pala: mundo, número de claims, ancho, área, cruce y bloques. */
    static Optional<String> gpRules(GpFacts f, Box box) {
        int width = box.maxX() - box.minX() + 1, length = box.maxZ() - box.minZ() + 1;
        if (!f.worldEnabled()) {
            return Optional.of(ParcelModule.CLAIMS_DISABLED);
        }
        if (f.maxClaims() > 0 && f.ownedClaims() >= f.maxClaims()) {
            return Optional.of(ParcelModule.TOO_MANY_CLAIMS);
        }
        if (width < f.minWidth() || length < f.minWidth()) {
            return Optional.of(ParcelModule.TOO_NARROW);
        }
        if (area(box) < f.minArea()) {
            return Optional.of(ParcelModule.TOO_SMALL);
        }
        if (f.overlaps()) {
            return Optional.of(ParcelModule.OVERLAPS_CLAIM);
        }
        return enoughBlocks(f.remaining(), box) ? Optional.empty() : Optional.of(ParcelModule.NOT_ENOUGH_BLOCKS);
    }

    /**
     * GriefPrevention. Doc oficial leída (GriefPrevention 16.18.8, última versión publicada): fuentes de
     * https://github.com/GriefPrevention/GriefPrevention ({@code DataStore.java}, {@code CreateClaimResult.java},
     * {@code PlayerData.java}, {@code Claim.java}, {@code GriefPrevention.java}, {@code PlayerEventHandler.java}):
     * <ul>
     * <li>{@code createClaim} NO mira permisos, bloques ni tamaño mínimo (su comentario); se mira aquí lo mismo que
     * la pala ({@code PlayerEventHandler}) y {@code /claim}: {@code GriefPrevention.claimsEnabledForWorld(World)},
     * {@code config_claims_maxClaimsPerPlayer} contra {@code PlayerData.getClaims().size()} (salvo con
     * {@code griefprevention.overrideclaimcountlimit}, que solo se ve con el dueño conectado),
     * {@code config_claims_minWidth}, {@code config_claims_minArea}, claims que cruzan la caja y
     * {@code DataStore.getPlayerData(UUID).getRemainingClaimBlocks()} (desconectado: GP lo lee de su archivo);</li>
     * <li>el permiso {@code griefprevention.createclaims} no se mira: no se puede saber con el dueño desconectado;</li>
     * <li>{@code DataStore.createClaim(World, int x1, int x2, int y1, int y2, int z1, int z2, UUID ownerID, Claim
     * parent, Long id, Player creatingPlayer)} → {@code CreateClaimResult} ({@code succeeded}, {@code claim}); gasta
     * los bloques del dueño y falla si cruza otro claim. Profundidad como la pala de GP: {@code y1} = suelo de la caja
     * menos {@code config_claims_claimsExtendIntoGroundDistance} (GP lo limita a su {@code getMaxDepth}),
     * {@code y2} = {@code world.getMaxHeight()} como {@code /claim} (los claims llegan siempre hasta arriba).
     * {@code creatingPlayer} null: el evento lo admite y el dueño puede estar desconectado;</li>
     * <li>{@code Claim.getID()} ({@code Long}), {@code DataStore.getClaim(long)} y
     * {@code DataStore.deleteClaim(Claim)}.</li>
     * </ul>
     */
    static ParcelClaims griefPrevention() {
        return new ParcelClaims() {
            private GriefPreventionApi gp;
            private Method playerData, remaining, claimsOf, enabledFor, createClaim, getId, getClaim, deleteClaim;
            private Class<?> main;

            private GriefPreventionApi gp() {
                if (gp == null) {
                    try {
                        GriefPreventionApi api = GriefPreventionApi.resolve();
                        Class<?> store = api.type("DataStore"), claim = api.type("Claim"),
                                data = api.type("PlayerData");
                        main = api.type("GriefPrevention");
                        playerData = store.getMethod("getPlayerData", UUID.class);
                        remaining = data.getMethod("getRemainingClaimBlocks");
                        claimsOf = data.getMethod("getClaims");
                        enabledFor = main.getMethod("claimsEnabledForWorld", World.class);
                        createClaim = store.getMethod("createClaim", World.class, int.class, int.class, int.class,
                                int.class, int.class, int.class, UUID.class, claim, Long.class, Player.class);
                        getId = claim.getMethod("getID");
                        getClaim = store.getMethod("getClaim", long.class);
                        deleteClaim = store.getMethod("deleteClaim", claim);
                        gp = api; // al final: si algo falla, se vuelve a intentar entero
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }
                return gp;
            }

            private int config(String field) {
                return call(() -> main.getField(field).getInt(gp().instance()));
            }

            @Override
            public Optional<String> canCreate(UUID owner, String world, Box box) {
                GriefPreventionApi api = gp();
                World w = world(world);
                Player online = Bukkit.getPlayer(owner);
                boolean noLimit = online != null && online.hasPermission("griefprevention.overrideclaimcountlimit");
                return call(() -> {
                    Object data = playerData.invoke(api.dataStore(), owner);
                    return gpRules(new GpFacts((boolean) enabledFor.invoke(api.instance(), w),
                            noLimit ? 0 : config("config_claims_maxClaimsPerPlayer"),
                            ((Collection<?>) claimsOf.invoke(data)).size(), config("config_claims_minWidth"),
                            config("config_claims_minArea"), !api.crossing(world, box).isEmpty(),
                            (int) remaining.invoke(data)), box);
                });
            }

            @Override
            public Optional<String> create(UUID owner, String world, Box box, String name) {
                GriefPreventionApi api = gp();
                World w = world(world);
                int depth = config("config_claims_claimsExtendIntoGroundDistance");
                return call(() -> {
                    Object result = createClaim.invoke(api.dataStore(), w, box.minX(), box.maxX(),
                            box.minY() - depth, w.getMaxHeight(), box.minZ(), box.maxZ(), owner, null, null, null);
                    Object claim = result.getClass().getField("claim").get(result);
                    if (!result.getClass().getField("succeeded").getBoolean(result) || claim == null) {
                        return Optional.empty();
                    }
                    return Optional.of(String.valueOf(getId.invoke(claim)));
                });
            }

            @Override
            public void remove(String world, String createdId) {
                GriefPreventionApi api = gp();
                call(() -> {
                    Object claim = getClaim.invoke(api.dataStore(), Long.parseLong(createdId));
                    if (claim != null) {
                        deleteClaim.invoke(api.dataStore(), claim);
                    }
                    return null;
                });
            }
        };
    }

    /**
     * WorldGuard. Doc oficial leída (WorldGuard 7.0.18, la del servidor; fuentes de esa etiqueta en
     * https://github.com/EngineHub/WorldGuard : {@code RegionManager.java}, {@code RemovalStrategy.java},
     * {@code ProtectedRegion.java}, {@code DefaultDomain.java}) y
     * https://worldguard.enginehub.org/en/latest/developer/regions/managers/ ,
     * https://worldguard.enginehub.org/en/latest/developer/regions/protected-region/ :
     * <ul>
     * <li>{@code RegionManager.addRegion(ProtectedRegion)} (reemplaza una región con el mismo id: por eso se mira
     * antes {@code hasRegion(String)});</li>
     * <li>{@code removeRegion(String, RemovalStrategy.UNSET_PARENT_IN_CHILDREN)}: las regiones hijas que alguien
     * haya puesto dentro de la parcela se quedan, solo pierden el padre ({@code removeRegion(String)} a secas usa
     * {@code REMOVE_CHILDREN} y las borraría);</li>
     * <li>{@code new ProtectedCuboidRegion(id, min, max)} y {@code ProtectedRegion.getOwners()} →
     * {@code DefaultDomain.addPlayer(UUID)} (vale desconectado);</li>
     * <li>«Region data is automatically saved after a short delay»: no se llama a {@code save()}.</li>
     * </ul>
     */
    static ParcelClaims worldGuard() {
        return new ParcelClaims() {
            private WorldGuardApi wg;
            private Object keepChildren;
            private Method getId, getOwners, addPlayer, hasRegion, addRegion, removeRegion;

            private WorldGuardApi wg() {
                if (wg == null) {
                    try {
                        WorldGuardApi api = WorldGuardApi.resolve();
                        Class<?> region = api.regionType(), manager = api.managerType();
                        Class<?> strategy = api.type("com.sk89q.worldguard.protection.managers.RemovalStrategy");
                        getId = region.getMethod("getId");
                        getOwners = region.getMethod("getOwners");
                        addPlayer = getOwners.getReturnType().getMethod("addPlayer", UUID.class);
                        hasRegion = manager.getMethod("hasRegion", String.class);
                        addRegion = manager.getMethod("addRegion", region);
                        removeRegion = manager.getMethod("removeRegion", String.class, strategy);
                        keepChildren = strategy.getField("UNSET_PARENT_IN_CHILDREN").get(null);
                        wg = api; // al final: si algo falla, se vuelve a intentar entero
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }
                return wg;
            }

            @Override
            public Optional<String> canCreate(UUID owner, String world, Box box) {
                WorldGuardApi api = wg();
                World w = world(world);
                return call(() -> {
                    Object manager = api.manager(w);
                    if (manager == null) {
                        return Optional.of(ParcelModule.NO_REGIONS);
                    }
                    for (Object r : api.crossing(manager, box)) {
                        if (!WorldGuardApi.GLOBAL.equalsIgnoreCase((String) getId.invoke(r))) {
                            return Optional.of(ParcelModule.OVERLAPS_REGION);
                        }
                    }
                    return Optional.empty();
                });
            }

            @Override
            public Optional<String> create(UUID owner, String world, Box box, String name) {
                WorldGuardApi api = wg();
                World w = world(world);
                return call(() -> {
                    Object manager = api.manager(w);
                    if (manager == null || (boolean) hasRegion.invoke(manager, name)) {
                        return Optional.empty();
                    }
                    Object region = api.cuboid(name, box);
                    addPlayer.invoke(getOwners.invoke(region), owner);
                    addRegion.invoke(manager, region);
                    return Optional.of(name);
                });
            }

            @Override
            public void remove(String world, String createdId) {
                WorldGuardApi api = wg();
                World w = world(world);
                call(() -> {
                    Object manager = api.manager(w);
                    if (manager != null) {
                        removeRegion.invoke(manager, createdId, keepChildren);
                    }
                    return null;
                });
            }
        };
    }
}
