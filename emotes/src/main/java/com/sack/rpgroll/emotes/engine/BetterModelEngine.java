package com.sack.rpgroll.emotes.engine;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitPlayer;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Motor sobre la API de BetterModel 3.6.0: un tracker de «limb» por jugador, que se cierra al acabar la
 * animación. El cierre (propio, por {@code /bm reload} o por BetterModel) es lo que avisa del final.
 */
public final class BetterModelEngine implements EmoteEngine {

    // El cierre llega desde el ScheduledExecutorService de BetterModel (Tracker.EXECUTOR): mapa concurrente.
    private final Map<UUID, EntityTracker> trackers = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public BetterModelEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean play(Player player, String model, String animation, boolean loop, Runnable onEnd) {
        ModelRenderer renderer = BetterModel.limbOrNull(model);
        if (renderer == null || renderer.animation(animation).isEmpty()) {
            return false;
        }
        UUID id = player.getUniqueId();
        // El Consumer solo se ejecuta al construir un tracker nuevo: así se sabe si es nuestro.
        AtomicBoolean created = new AtomicBoolean();
        EntityTracker tracker = renderer.getOrCreate(new BukkitPlayer(player), TrackerModifier.DEFAULT,
                t -> created.set(true));
        if (tracker.isClosed()) {
            // El registro aún guarda uno cerrado (cierre a la vez que el relanzamiento): se crea otro.
            tracker = renderer.create(new BukkitPlayer(player), TrackerModifier.DEFAULT, t -> created.set(true));
        }
        EntityTracker mine = tracker;
        AtomicBoolean ended = new AtomicBoolean();
        mine.handleCloseEvent((t, reason) -> {
            trackers.remove(id, mine);
            if (ended.compareAndSet(false, true)) {
                runOnMainThread(onEnd);
            }
        });
        trackers.put(id, mine);

        // Un bucle cuyo .bbmodel dice «hold» (sentarse) entra una vez y se queda en su última pose.
        AnimationIterator.Type type = !loop ? AnimationIterator.Type.PLAY_ONCE
                : renderer.animation(animation).map(BlueprintAnimation::loop)
                        .filter(t -> t == AnimationIterator.Type.HOLD_ON_LAST)
                        .orElse(AnimationIterator.Type.LOOP);
        AnimationModifier modifier = AnimationModifier.builder().type(type).build();
        if (!mine.animate(animation, modifier, mine::close)) {
            ended.set(true); // no llegó a empezar: el que llama recibe false, no un onEnd
            trackers.remove(id, mine);
            if (created.get()) {
                mine.close(); // solo el nuestro; uno ajeno (p. ej. de /bm play) se deja vivo
            }
            return false;
        }
        return true;
    }

    private void runOnMainThread(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public void stop(Player player) {
        EntityTracker tracker = trackers.remove(player.getUniqueId());
        if (tracker != null) {
            tracker.close();
        }
    }

    @Override
    public float lengthSeconds(String model, String animation) {
        return BetterModel.limb(model).flatMap(r -> r.animation(animation)).map(BlueprintAnimation::length).orElse(-1f);
    }

    @Override
    public boolean exists(String model, String animation) {
        return BetterModel.limb(model).flatMap(r -> r.animation(animation)).isPresent();
    }
}
