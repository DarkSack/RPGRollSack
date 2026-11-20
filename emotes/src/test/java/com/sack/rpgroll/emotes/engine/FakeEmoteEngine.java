package com.sack.rpgroll.emotes.engine;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Motor de mentira: apunta las llamadas y deja disparar el {@code onEnd} como si cerrara BetterModel. */
public class FakeEmoteEngine implements EmoteEngine {

    /** Llamadas en orden: {@code play:<model>/<animation>:<loop>} o {@code stop}. */
    public final List<String> calls = new ArrayList<>();
    private final Map<UUID, Runnable> onEnds = new HashMap<>();
    public boolean playResult = true;
    public boolean existsResult = true;

    @Override
    public boolean play(Player player, String model, String animation, boolean loop, Runnable onEnd) {
        calls.add("play:" + model + "/" + animation + ":" + loop);
        if (playResult) {
            onEnds.put(player.getUniqueId(), onEnd);
        }
        return playResult;
    }

    @Override
    public void stop(Player player) {
        calls.add("stop");
        Runnable end = onEnds.remove(player.getUniqueId());
        if (end != null) {
            end.run();
        }
    }

    /** Simula que la animación acaba o que BetterModel cierra el tracker por fuera. */
    public void fireEnd(UUID uuid) {
        Runnable end = onEnds.remove(uuid);
        if (end != null) {
            end.run();
        }
    }

    @Override
    public float lengthSeconds(String model, String animation) {
        return 1f;
    }

    @Override
    public boolean exists(String model, String animation) {
        return existsResult;
    }
}
