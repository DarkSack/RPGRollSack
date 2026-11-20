package com.sack.rpgroll.emotes.magic;

import com.sack.rpgroll.common.event.magic.SpellCastEvent;
import com.sack.rpgroll.common.event.magic.SpellChannelEndEvent;
import com.sack.rpgroll.common.event.magic.SpellChannelStartEvent;
import com.sack.rpgroll.emotes.EmotesSettings;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.service.EmoteService;
import com.sack.rpgroll.emotes.service.EmoteSession;
import com.sack.rpgroll.emotes.service.LaunchResult;
import com.sack.rpgroll.emotes.service.PlayerState;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Puente con RPGRoll-Magic: canalizar reproduce un bucle y conjurar lo reemplaza. Solo escucha eventos de
 * la Lib, así que funciona haya o no Magic. Lee {@code magic.enabled} en cada evento (puede recargarse).
 * <p>
 * {@code walking-speed} sigue siendo el umbral de {@link #chooseCast}; en el juego llega como «se mueve o
 * no» (input pulsado). Un canal solo termina en Cast si el jugador estuvo quieto, por eso {@code _caminando}
 * solo aplica a los cast instantáneos.
 */
public class MagicBridge implements Listener {

    /** Ticks extra tras el tiempo de canalización antes de cortar el bucle por seguridad. */
    private static final long SAFETY_EXTRA_TICKS = 20L;

    private final EmoteService service;
    private final EmoteCatalog catalog;
    private final Supplier<EmotesSettings> settings;
    private final BiConsumer<Runnable, Long> later;
    // Sesión de canalizar de cada jugador, para no cortar un conjurar que ya la reemplazó.
    private final Map<UUID, EmoteSession> channels = new ConcurrentHashMap<>();

    /** @param later programa una tarea en el hilo principal tras N ticks (en el plugin, {@code runTaskLater}) */
    public MagicBridge(EmoteService service, EmoteCatalog catalog, Supplier<EmotesSettings> settings,
                       BiConsumer<Runnable, Long> later) {
        this.service = service;
        this.catalog = catalog;
        this.settings = settings;
        this.later = later;
    }

    /** El emote de conjuro de la escuela; con velocidad por encima de la de andar, su variante {@code _caminando}. */
    public String chooseCast(String schoolId, double horizontalSpeed) {
        EmotesSettings s = settings.get();
        String id = s.castEmoteFor(schoolId);
        String walking = id + "_caminando";
        return horizontalSpeed > s.walkingSpeed() && catalog.get(walking).isPresent() ? walking : id;
    }

    @EventHandler
    public void onChannelStart(SpellChannelStartEvent e) {
        if (!settings.get().magicEnabled()) {
            return;
        }
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        // Planeando, nadando, montado, durmiendo o muerto: el hechizo sale, pero sin emote.
        if (PlayerState.of(p).busy()) {
            return;
        }
        if (service.forceMagic(p, settings.get().channelEmoteFor(e.schoolId())) != LaunchResult.OK) {
            return;
        }
        EmoteSession mine = service.session(uuid).orElse(null);
        channels.put(uuid, mine);
        // Si Magic se apaga a mitad nunca llega el End: el bucle no puede quedarse para siempre.
        later.accept(() -> endChannel(p, mine), e.castTimeTicks() + SAFETY_EXTRA_TICKS);
    }

    @EventHandler
    public void onChannelEnd(SpellChannelEndEvent e) {
        if (!settings.get().magicEnabled()) {
            return;
        }
        endChannel(e.getPlayer(), channels.get(e.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onCast(SpellCastEvent e) {
        if (!settings.get().magicEnabled()) {
            return;
        }
        Player p = e.getPlayer();
        if (PlayerState.of(p).busy()) {
            return;
        }
        service.forceMagic(p, chooseCast(e.schoolId(), moving(p) ? Double.MAX_VALUE : 0));
    }

    /**
     * En Paper la velocidad de quien anda por el suelo da casi 0 (lo mueve el cliente), así que se mira el
     * input: alguna dirección pulsada (montado no llega aquí: cuenta como ocupado).
     */
    private static boolean moving(Player p) {
        Input in = p.getCurrentInput();
        return in != null && (in.isForward() || in.isBackward() || in.isLeft() || in.isRight());
    }

    /** Para el bucle solo si la sesión en marcha sigue siendo {@code channel}. */
    private void endChannel(Player p, EmoteSession channel) {
        UUID uuid = p.getUniqueId();
        if (channel == null) {
            return;
        }
        channels.remove(uuid, channel);
        if (service.session(uuid).orElse(null) == channel) {
            service.stop(p);
        }
    }
}
