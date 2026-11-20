package com.sack.rpgroll.emotes.listener;

import com.sack.rpgroll.emotes.EmotesSettings;
import com.sack.rpgroll.emotes.catalog.CancelReason;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import com.sack.rpgroll.emotes.service.EmoteService;
import com.sack.rpgroll.emotes.service.EmoteSession;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Corta el emote en marcha por movimiento, daño o cambio de estado, y marca el combate. */
public class CancelListener implements Listener {

    private final EmoteService service;
    private final EmoteCatalog catalog;
    private final EmotePlayerStore store;
    private final Supplier<EmotesSettings> settings;
    private final LongSupplier clock;

    public CancelListener(EmoteService service, EmoteCatalog catalog, EmotePlayerStore store,
                          Supplier<EmotesSettings> settings, LongSupplier clockMillis) {
        this.service = service;
        this.catalog = catalog;
        this.store = store;
        this.settings = settings;
        this.clock = clockMillis;
    }

    /** Para el emote del jugador si el emote (y no una sesión mágica) lista ese motivo. */
    private void cancel(Player p, CancelReason reason) {
        Optional<EmoteSession> found = service.session(p.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        EmoteSession session = found.get();
        // Un emote que ya no está en el catálogo no tiene reglas: se corta, salvo si es mágico.
        boolean stop = catalog.get(session.emoteId())
                .map(e -> CancelRules.cancels(e, session, reason))
                .orElse(!session.magic());
        if (stop) {
            service.stop(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Optional<EmoteSession> session = service.session(p.getUniqueId());
        if (session.isEmpty()) {
            return;
        }
        EmoteSession s = session.get();
        // Las sesiones mágicas no se cortan al moverse; sin inicio no hay distancia que medir.
        if (s.magic() || s.start() == null) {
            return;
        }
        if (CancelRules.movedTooFar(s.start(), e.getTo(), settings.get().moveDistance())) {
            cancel(p, CancelReason.MOVE);
        }
    }

    // Solo EntityDamageEvent: ByEntity es subclase y escuchar los dos procesaría dos veces.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        Entity attacker = e instanceof EntityDamageByEntityEvent byEntity ? source(byEntity.getDamager()) : null;
        if (e.getEntity() instanceof Player victim) {
            cancel(victim, CancelReason.DAMAGE);
            if ((attacker instanceof Player || attacker instanceof Mob) && attacker != victim) {
                service.markCombat(victim.getUniqueId(), clock.getAsLong());
            }
        }
        if (attacker instanceof Player hitter && CancelRules.countsAsCombat(hitter, e.getEntity())) {
            cancel(hitter, CancelReason.ATTACK);
            service.markCombat(hitter.getUniqueId(), clock.getAsLong());
        }
    }

    /** El tirador si es un proyectil; si no, la propia entidad. */
    private static Entity source(Entity damager) {
        return damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter ? shooter : damager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMount(EntityMountEvent e) {
        if (e.getEntity() instanceof Player p) {
            cancel(p, CancelReason.MOUNT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (e.getEntered() instanceof Player p) {
            cancel(p, CancelReason.MOUNT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent e) {
        if (e.isGliding() && e.getEntity() instanceof Player p) {
            cancel(p, CancelReason.GLIDE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwim(EntityToggleSwimEvent e) {
        if (e.isSwimming() && e.getEntity() instanceof Player p) {
            cancel(p, CancelReason.SWIM);
        }
    }

    /**
     * Solo si de verdad se duerme ({@code OK}). Dormir no genera movimiento, así que corta siempre,
     * aunque el emote liste un {@code cancel} explícito sin {@code sleep}; solo las sesiones mágicas se salvan.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent e) {
        if (e.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        service.session(e.getPlayer().getUniqueId()).filter(s -> !s.magic())
                .ifPresent(s -> service.stop(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        service.stop(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        service.stop(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        service.stop(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        service.clear(e.getPlayer().getUniqueId());
        store.save(e.getPlayer().getUniqueId());
        store.unload(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        store.load(e.getPlayer().getUniqueId());
    }
}
