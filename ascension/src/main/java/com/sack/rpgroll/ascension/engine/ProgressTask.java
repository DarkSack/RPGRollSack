package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.ascension.progress.TriggerType;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lo que no llega como evento. Cada 5 segundos mira en qué bioma está cada
 * jugador (para VISIT_BIOME); cada 20, revisa lo que depende de su estado;
 * cada 5 minutos, guarda. Antes el progreso de Ascension solo se guardaba
 * al salir, así que una caída del servidor se lo llevaba.
 */
public class ProgressTask implements Runnable {

    /** En ticks: cada cuánto corre la tarea. */
    public static final long PERIOD = 100L;

    private static final int REFRESH_EVERY = 4;
    private static final int SAVE_EVERY = 60;

    private final ProgressService progress;
    private final AscensionPlayerStateManager stateManager;
    private final Map<UUID, String> lastBiome = new ConcurrentHashMap<>();
    private int runs;

    public ProgressTask(ProgressService progress, AscensionPlayerStateManager stateManager) {
        this.progress = progress;
        this.stateManager = stateManager;
    }

    @Override
    public void run() {

        runs++;
        boolean refresh = runs % REFRESH_EVERY == 0;

        for (Player player : Bukkit.getOnlinePlayers()) {

            String biome = player.getLocation().getBlock().getBiome().getKey().asString();

            // Solo al cambiar de bioma: quedarse quieto no repite trabajo.
            if (!biome.equals(lastBiome.put(player.getUniqueId(), biome))) {
                progress.handle(player, new ProgressEvent(TriggerType.VISIT_BIOME, biome, null));
            }

            if (refresh) {
                progress.refresh(player);
            }
        }

        lastBiome.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        if (runs % SAVE_EVERY == 0) {
            stateManager.saveAll();
        }
    }

}
