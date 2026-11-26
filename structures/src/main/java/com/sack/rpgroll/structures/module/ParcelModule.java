package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.structures.api.BuiltStructure;
import com.sack.rpgroll.structures.api.StructureModule;
import com.sack.rpgroll.structures.catalog.Problem;
import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code type: parcel}: al terminar, crea para el dueño un claim de GriefPrevention o una región de WorldGuard sobre
 * la caja ({@code claim: griefprevention | worldguard | none}). El id creado se guarda en {@code build_modules} como
 * {@code <hook>:<id>}; al borrar la construcción se borra ese terreno (el guardado, aunque el YAML cambie).
 */
public final class ParcelModule implements StructureModule {

    public static final String TYPE = "parcel";
    public static final String INACTIVE = "parcel.inactive";
    public static final String NOT_ENOUGH_BLOCKS = "parcel.not-enough-blocks";
    public static final String OVERLAPS_CLAIM = "parcel.overlaps-claim";
    public static final String OVERLAPS_REGION = "parcel.overlaps-region";
    public static final String NO_REGIONS = "parcel.no-regions";
    public static final String CLAIMS_DISABLED = "parcel.claims-disabled";
    public static final String TOO_MANY_CLAIMS = "parcel.too-many-claims";
    public static final String TOO_NARROW = "parcel.too-narrow";
    public static final String TOO_SMALL = "parcel.too-small";
    public static final String ERROR = "parcel.error";
    public static final String FAILED = "parcel.failed";

    private final Predicate<Hook> active;
    private final Function<Hook, ParcelClaims> claims;
    private final BiConsumer<Hook, String> broken;
    private final ProjectRepository repo;
    private final Consumer<Runnable> main;
    private final BiConsumer<UUID, String> tell;
    private final Logger log;

    /**
     * @param broken marca la integración rota (avisa una vez) cuando su adaptador lanza
     * @param main   vuelve al hilo principal desde el de la base de datos
     * @param tell   clave de idioma al jugador, solo si está conectado
     */
    public ParcelModule(Predicate<Hook> active, Function<Hook, ParcelClaims> claims, BiConsumer<Hook, String> broken,
                        ProjectRepository repo, Consumer<Runnable> main, BiConsumer<UUID, String> tell, Logger log) {
        this.active = active;
        this.claims = claims;
        this.broken = broken;
        this.repo = repo;
        this.main = main;
        this.tell = tell;
        this.log = log;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public List<Problem> validate(ConfigurationSection c) {
        String claim = c.getString("claim", "none").toLowerCase(Locale.ROOT);
        return claim.equals("none") || hook(claim) != null ? List.of()
                : List.of(new Problem("", "claim", "debe ser griefprevention, worldguard o none"));
    }

    @Override
    public Optional<String> precheck(UUID owner, String world, Box box, ConfigurationSection c) {
        Hook h = hook(c.getString("claim", "none"));
        return h == null ? Optional.empty() : canCreate(h, owner, world, box);
    }

    @Override
    public void onBuilt(BuiltStructure b) {
        Hook h = hook(b.config().getString("claim", "none"));
        if (h == null) {
            return;
        }
        BuildRecord r = b.record();
        Optional<String> why = canCreate(h, r.owner(), r.world(), r.box());
        if (why.isPresent()) {
            fail(r, why.get(), "no se puede crear (" + why.get() + ")", null);
            return;
        }
        Optional<String> id;
        try {
            id = claims.apply(h).create(r.owner(), r.world(), r.box(), "rpgroll_parcel_" + r.id().toString()
                    .substring(0, 8));
        } catch (ParcelClaims.Refused e) {
            fail(r, FAILED, h.plugin() + " lo rechazó", e);
            return;
        } catch (RuntimeException | LinkageError e) {
            broken.accept(h, e.toString());
            fail(r, FAILED, "falló " + h.plugin(), e);
            return;
        }
        if (id.isEmpty()) {
            fail(r, FAILED, h.plugin() + " no lo creó (¿cruza otro terreno?)", null);
            return;
        }
        String data = h.key() + ":" + id.get();
        repo.saveModuleData(r.id(), data).whenComplete((v, e) -> {
            if (e != null) {
                log.log(Level.SEVERE, "Terreno " + data + " de la construcción " + r.id() + " creado pero sin"
                        + " guardar: no se borrará solo; quitarlo a mano si se borra la construcción", e);
            }
        });
    }

    @Override
    public void onRemoved(BuiltStructure b) {
        BuildRecord r = b.record();
        repo.moduleData(r.id()).whenComplete((data, e) -> main.accept(() -> {
            if (e != null) {
                log.log(Level.WARNING, "No se pudo leer el terreno de la construcción " + r.id()
                        + "; quitarlo a mano", e);
                return;
            }
            if (data.isEmpty()) {
                return;
            }
            int sep = data.get().indexOf(':');
            Hook h = sep < 0 ? null : hook(data.get().substring(0, sep));
            if (h == null || !active.test(h)) {
                log.warning("No se pudo borrar el terreno " + data.get() + " de la construcción " + r.id()
                        + " (integración inactiva); quitarlo a mano");
                return;
            }
            try {
                claims.apply(h).remove(r.world(), data.get().substring(sep + 1));
            } catch (RuntimeException | LinkageError ex) {
                if (!(ex instanceof ParcelClaims.Refused)) {
                    broken.accept(h, ex.toString());
                }
                log.log(Level.WARNING, "No se pudo borrar el terreno " + data.get() + " de la construcción "
                        + r.id() + "; quitarlo a mano", ex);
                return;
            }
            repo.deleteModuleData(r.id()).whenComplete((v, ex) -> {
                if (ex != null) {
                    log.log(Level.WARNING, "Terreno de " + r.id() + " borrado; su fila en build_modules no", ex);
                }
            });
        }));
    }

    private Optional<String> canCreate(Hook h, UUID owner, String world, Box box) {
        if (!active.test(h)) {
            return Optional.of(INACTIVE);
        }
        try {
            return claims.apply(h).canCreate(owner, world, box);
        } catch (ParcelClaims.Refused e) {
            log.log(Level.WARNING, h.plugin() + " falló al mirar la parcela en " + world + " " + box, e);
            return Optional.of(ERROR);
        } catch (RuntimeException | LinkageError e) {
            broken.accept(h, e.toString());
            return Optional.of(ERROR);
        }
    }

    /** La construcción sigue registrada: solo se avisa. */
    private void fail(BuildRecord r, String key, String detail, Throwable e) {
        log.log(Level.WARNING, "Parcela de la construcción " + r.id() + " (" + r.structure() + ") sin terreno: "
                + detail, e);
        tell.accept(r.owner(), key);
    }

    private static Hook hook(String claim) {
        return switch (claim == null ? "" : claim.toLowerCase(Locale.ROOT)) {
            case "griefprevention" -> Hook.GRIEFPREVENTION;
            case "worldguard" -> Hook.WORLDGUARD;
            default -> null;
        };
    }
}
