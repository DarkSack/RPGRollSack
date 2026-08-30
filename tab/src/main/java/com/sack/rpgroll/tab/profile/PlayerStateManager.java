package com.sack.rpgroll.tab.profile;

import com.sack.rpgroll.tab.context.ContextDefinition;
import com.sack.rpgroll.tab.context.ContextResolver;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado activo (perfil resuelto) por jugador. Es el único lugar que sabe
 * "qué le corresponde mostrar a este jugador ahora" — todos los engines
 * (tablist/scoreboard/nametag/belowname/bossbar/layout) lo consultan en vez
 * de recalcular el contexto por su cuenta.
 * <p>
 * {@link #refresh(Player)} solo debe llamarse desde eventos que realmente
 * puedan cambiar el resultado (join, cambio de mundo, cambio de permisos,
 * evento de otro addon que afecte un placeholder usado en un contexto) —
 * nunca en un bucle por tick.
 */
public class PlayerStateManager {

    private final ContextResolver contextResolver;
    private final ProfileManager profileManager;

    private final Map<UUID, TABProfile> activeProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeContextIds = new ConcurrentHashMap<>();
    private final Map<String, TABProfile> runtimeProfiles = new ConcurrentHashMap<>();
    private volatile String globalOverrideProfileId;

    public PlayerStateManager(ContextResolver contextResolver, ProfileManager profileManager) {
        this.contextResolver = contextResolver;
        this.profileManager = profileManager;
    }

    /** Registra un perfil en memoria (no persiste a YAML) — para eventos temporales vía API (sección 42). */
    public void registerRuntimeProfile(TABProfile profile) {
        runtimeProfiles.put(profile.id(), profile);
    }

    /** Perfil que, si no es null, se aplica a TODOS los jugadores sin pasar por el Context Engine. */
    public void setGlobalOverride(String profileId) {
        this.globalOverrideProfileId = profileId;
    }

    public void clearGlobalOverride() {
        this.globalOverrideProfileId = null;
    }

    private Optional<TABProfile> findProfile(String id) {

        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        TABProfile runtime = runtimeProfiles.get(id);
        return runtime != null ? Optional.of(runtime) : profileManager.get(id);
    }

    /** Recalcula y guarda el perfil activo del jugador; devuelve el resultado. */
    public TABProfile refresh(Player player) {

        if (globalOverrideProfileId != null) {

            TABProfile overrideProfile = findProfile(globalOverrideProfileId).orElseGet(this::defaultProfile);
            activeProfiles.put(player.getUniqueId(), overrideProfile);
            activeContextIds.remove(player.getUniqueId());
            return overrideProfile;
        }

        Optional<ContextDefinition> context = contextResolver.resolve(player);

        TABProfile base = context
                .map(ContextDefinition::profileId)
                .flatMap(this::findProfile)
                .orElseGet(this::defaultProfile);

        TABProfile resolved = context.map(c -> applyOverrides(base, c)).orElse(base);

        activeProfiles.put(player.getUniqueId(), resolved);

        Optional<String> contextId = context.map(ContextDefinition::id);
        if (contextId.isPresent()) {
            activeContextIds.put(player.getUniqueId(), contextId.get());
        } else {
            activeContextIds.remove(player.getUniqueId());
        }

        return resolved;
    }

    private TABProfile applyOverrides(TABProfile base, ContextDefinition context) {

        return new TABProfile(
                base.id(),
                override(context.tablistOverrideId(), base.tablistId()),
                override(context.scoreboardOverrideId(), base.scoreboardId()),
                override(context.nametagOverrideId(), base.nametagId()),
                override(context.belownameOverrideId(), base.belownameId()),
                override(context.bossbarOverrideId(), base.bossbarId()),
                override(context.layoutOverrideId(), base.layoutId()),
                base.sortingId(),
                base.teamsId());
    }

    private String override(String contextValue, String baseValue) {
        return (contextValue == null || contextValue.isBlank()) ? baseValue : contextValue;
    }

    private TABProfile defaultProfile() {
        return findProfile(ProfileManager.DEFAULT_PROFILE_ID)
                .orElse(new TABProfile("default", null, null, null, null, null, null, null, null));
    }

    public Optional<TABProfile> activeProfile(Player player) {
        return Optional.ofNullable(activeProfiles.get(player.getUniqueId()));
    }

    public Optional<String> activeContextId(Player player) {
        return Optional.ofNullable(activeContextIds.get(player.getUniqueId()));
    }

    /** Fuerza un perfil específico (usado por {@code /tabadmin profile} y por la API pública). */
    public void forceProfile(Player player, TABProfile profile) {
        activeProfiles.put(player.getUniqueId(), profile);
        activeContextIds.remove(player.getUniqueId());
    }

    public void clear(Player player) {
        activeProfiles.remove(player.getUniqueId());
        activeContextIds.remove(player.getUniqueId());
    }

}
