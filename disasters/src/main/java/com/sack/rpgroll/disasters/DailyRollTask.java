package com.sack.rpgroll.disasters;

import com.sack.rpgroll.disasters.core.Disaster;
import com.sack.rpgroll.disasters.core.DisasterManager;
import com.sack.rpgroll.disasters.engine.DisasterEngine;
import com.sack.rpgroll.disasters.integration.SeasonsIntegration;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Una vez por día de Minecraft y mundo, sortea como mucho un desastre. El día visto por última vez
 * se guarda en {@code state.yml}, para no volver a sortear el mismo día tras un reinicio.
 */
final class DailyRollTask implements Runnable {

    private final JavaPlugin plugin;
    private final DisasterManager disasters;
    private final DisasterEngine engine;
    private final File stateFile;
    private final YamlConfiguration state;
    private final Random random = new Random();

    DailyRollTask(JavaPlugin plugin, DisasterManager disasters, DisasterEngine engine) {
        this.plugin = plugin;
        this.disasters = disasters;
        this.engine = engine;
        this.stateFile = new File(plugin.getDataFolder(), "state.yml");
        this.state = YamlConfiguration.loadConfiguration(stateFile);
    }

    @Override
    public void run() {

        List<String> disabled = plugin.getConfig().getStringList("disabled-worlds");
        boolean changed = false;

        for (World world : Bukkit.getWorlds()) {
            if (disabled.contains(world.getName()) || world.getPlayers().isEmpty()) {
                continue;
            }
            long day = world.getFullTime() / 24000L;
            String path = "last-day." + world.getName();
            if (state.contains(path) && state.getLong(path) == day) {
                continue;
            }
            state.set(path, day);
            changed = true;
            roll(world);
        }

        if (changed) {
            try {
                state.save(stateFile);
            } catch (IOException e) {
                plugin.getLogger().warning("✘ No se pudo guardar state.yml: " + e.getMessage());
            }
        }
    }

    /** Se recorren en orden aleatorio y gana el primero que sale; empieza en un momento al azar del día. */
    private void roll(World world) {

        if (engine.active(world).isPresent()) {
            return;
        }

        String seasonId = SeasonsIntegration.currentSeasonId(world.getSpawnLocation());
        List<Disaster> candidates = new ArrayList<>(disasters.getAll().stream()
                .filter(disaster -> disaster.canRollIn(seasonId, world.getName())).toList());
        Collections.shuffle(candidates, random);

        for (Disaster disaster : candidates) {
            if (random.nextDouble() < disaster.dailyChance()) {
                engine.startLater(disaster, world, random.nextInt(engine.maxStartDelay()));
                return;
            }
        }
    }

}
