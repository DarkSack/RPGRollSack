package com.sack.rpgroll.emotes.service;

import com.sack.rpgroll.emotes.EmotesSettings;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.catalog.EmoteMusic;
import com.sack.rpgroll.emotes.catalog.EmoteParticles;
import com.sack.rpgroll.emotes.catalog.EmoteSound;
import com.sack.rpgroll.emotes.engine.EmoteEngine;
import com.sack.rpgroll.emotes.player.EmotePlayer;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Reglas para lanzar emotes (desbloqueo, enfriamiento, combate, estado) y sesiones en marcha. */
public class EmoteService {

    /**
     * Sesión en marcha junto al jugador, para poder pararla desde {@link #clear(UUID)}. Clase y no record:
     * se compara por identidad, así dos sesiones iguales del mismo milisegundo no se confunden.
     */
    private static final class Active {
        private final EmoteSession session;
        private final Player player;
        private final EmoteMusic music;
        private boolean ended;
        private EmoteAudio.Cancellable repeat;

        private Active(EmoteSession session, Player player, EmoteMusic music) {
            this.session = session;
            this.player = player;
            this.music = music;
        }

        EmoteSession session() {
            return session;
        }

        Player player() {
            return player;
        }
    }

    private final EmoteAudio audio;
    private final EmoteCatalog catalog;
    private final EmotePlayerStore store;
    private final EmoteEngine engine;
    private final Supplier<EmotesSettings> settings;
    private final LongSupplier clock;
    // El motor garantiza el onEnd en el hilo principal; el synchronized de la música no estorba.
    private final Map<UUID, Active> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCombat = new ConcurrentHashMap<>();

    public EmoteService(EmoteCatalog catalog, EmotePlayerStore store, EmoteEngine engine,
                        Supplier<EmotesSettings> settings, LongSupplier clockMillis, EmoteAudio audio) {
        this.audio = audio;
        this.catalog = catalog;
        this.store = store;
        this.engine = engine;
        this.settings = settings;
        this.clock = clockMillis;
    }

    /**
     * Lanza un emote por id o por hueco de favoritos (texto solo de dígitos).
     *
     * @param bypassCooldown true = ignora el enfriamiento (permiso del jugador)
     */
    public LaunchResult launch(Player p, String idOrFavoriteSlot, PlayerState state, boolean bypassCooldown) {
        UUID uuid = p.getUniqueId();
        EmotePlayer data = store.get(uuid);
        String key = idOrFavoriteSlot.toLowerCase(Locale.ROOT);
        if (!key.isEmpty() && key.chars().allMatch(Character::isDigit)) {
            key = data.favorite(key.length() > 2 ? -1 : Integer.parseInt(key));
            if (key == null) {
                return LaunchResult.EMPTY_FAVORITE;
            }
        }
        Optional<Emote> found = catalog.get(key);
        if (found.isEmpty()) {
            return LaunchResult.UNKNOWN;
        }
        Emote emote = found.get();
        if (!owns(p, data, emote)) {
            return LaunchResult.LOCKED;
        }
        if (state.busy()) {
            return LaunchResult.BUSY_STATE;
        }
        EmotesSettings s = settings.get();
        long now = clock.getAsLong();
        Long combat = lastCombat.get(uuid);
        if (combat != null) {
            if (now - combat < s.combatTagSeconds() * 1000L) {
                return LaunchResult.IN_COMBAT;
            }
            lastCombat.remove(uuid, combat); // caducada: no se guarda más de la cuenta
        }
        if (!bypassCooldown && now - data.lastUseMillis() < s.cooldownSeconds() * 1000L) {
            return LaunchResult.COOLDOWN;
        }
        LaunchResult result = start(p, emote, now, false);
        if (result == LaunchResult.OK) {
            data.lastUseMillis(now);
        }
        return result;
    }

    /** Lo lanza sin mirar desbloqueo, enfriamiento, combate ni estado. No cuenta como uso. */
    public LaunchResult force(Player p, String emoteId) {
        Optional<Emote> emote = catalog.get(emoteId);
        return emote.isEmpty() ? LaunchResult.UNKNOWN : start(p, emote.get(), clock.getAsLong(), false);
    }

    /** Como {@link #force}, pero la sesión queda marcada como de un hechizo ({@code magic=true}). */
    public LaunchResult forceMagic(Player p, String emoteId) {
        Optional<Emote> emote = catalog.get(emoteId);
        return emote.isEmpty() ? LaunchResult.UNKNOWN : start(p, emote.get(), clock.getAsLong(), true);
    }

    /** true si no está bloqueado, si lo desbloqueó o si tiene permiso. */
    public boolean owns(Player p, EmotePlayer data, Emote emote) {
        return !emote.locked()
                || data.unlocked().contains(emote.id())
                || p.hasPermission("rpgrollemotes.emote." + emote.id())
                || p.hasPermission("rpgrollemotes.emote.*")
                || p.hasPermission("rpgrollemotes.admin");
    }

    public void stop(Player p) {
        Active a = active.remove(p.getUniqueId());
        if (a != null) {
            engine.stop(p);
            endMusic(a);
        }
    }

    public Optional<EmoteSession> session(UUID uuid) {
        return Optional.ofNullable(active.get(uuid)).map(Active::session);
    }

    public void markCombat(UUID uuid, long nowMillis) {
        lastCombat.put(uuid, nowMillis);
    }

    /** Al salir: para el emote y olvida todo lo del jugador. */
    public void clear(UUID uuid) {
        lastCombat.remove(uuid);
        Active a = active.remove(uuid);
        if (a != null) {
            engine.stop(a.player());
            endMusic(a);
        }
    }

    private LaunchResult start(Player p, Emote emote, long now, boolean magic) {
        UUID uuid = p.getUniqueId();
        String model = emote.model() == null || emote.model().isBlank() ? settings.get().limbModel() : emote.model();
        // Antes de cortar el emote en marcha: si el nuevo no existe, el viejo sigue.
        if (!engine.exists(model, emote.animation())) {
            return LaunchResult.ENGINE_FAILED;
        }
        Active old = active.remove(uuid);
        if (old != null) {
            engine.stop(p);
            endMusic(old);
        }
        Active mine = new Active(new EmoteSession(emote.id(), emote.loop(), p.getLocation(), now, magic), p,
                emote.music());
        active.put(uuid, mine);
        // Solo borra si sigue siendo esta sesión: un onEnd tardío no se lleva la nueva.
        Runnable onEnd = () -> {
            active.remove(uuid, mine);
            endMusic(mine);
        };
        if (!engine.play(p, model, emote.animation(), emote.loop(), onEnd)) {
            active.remove(uuid, mine);
            return LaunchResult.ENGINE_FAILED;
        }
        playEffects(p, emote);
        startMusic(mine);
        return LaunchResult.OK;
    }

    private void startMusic(Active a) {
        EmoteMusic m = a.music;
        if (m == null) {
            return;
        }
        synchronized (a) {
            if (a.ended) {
                return; // el onEnd llegó antes de empezar
            }
            audio.playMusic(a.player, m);
            if (a.session.loop() && m.lengthSeconds() > 0) {
                scheduleRepeat(a, Math.max(1L, (long) Math.ceil(m.lengthSeconds() * 20)));
            }
        }
    }

    /** Repite la pista mientras la sesión siga viva; cada vuelta programa la siguiente. */
    private void scheduleRepeat(Active a, long ticks) {
        a.repeat = audio.later(() -> {
            synchronized (a) {
                if (a.ended) {
                    return;
                }
                audio.playMusic(a.player, a.music);
                scheduleRepeat(a, ticks);
            }
        }, ticks);
    }

    /** Corta la música y la repetición de la sesión, una sola vez aunque lleguen varios finales. */
    private void endMusic(Active a) {
        synchronized (a) {
            if (a.ended) {
                return;
            }
            a.ended = true;
            if (a.repeat != null) {
                a.repeat.cancel();
            }
            if (a.music != null) {
                audio.stopMusic(a.player, a.music);
            }
        }
    }

    /** Sonido en la posición del jugador y partículas sobre su cabeza, si el emote los trae. */
    protected void playEffects(Player p, Emote emote) {
        EmoteSound sound = emote.sound();
        if (sound != null) {
            p.getWorld().playSound(p.getLocation(), sound.id(), sound.volume(), sound.pitch());
        }
        EmoteParticles particles = emote.particles();
        if (particles != null) {
            try {
                Particle type = Particle.valueOf(particles.type().toUpperCase(Locale.ROOT));
                Location head = p.getEyeLocation().add(0, 0.5, 0);
                p.getWorld().spawnParticle(type, head, particles.count(), 0.3, 0.2, 0.3, 0);
            } catch (IllegalArgumentException e) {
                // Tipo desconocido o que pide datos extra: el emote sale igual, sin partículas.
            }
        }
    }
}
