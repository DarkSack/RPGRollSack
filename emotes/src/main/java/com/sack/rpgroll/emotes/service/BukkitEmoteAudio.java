package com.sack.rpgroll.emotes.service;

import com.sack.rpgroll.emotes.catalog.EmoteMusic;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Audio real con la API de Adventure. La fuente es RECORD: el jugador lo baja con el volumen de «Discos». */
public final class BukkitEmoteAudio implements EmoteAudio {

    private static final double STOP_RADIUS_SQUARED = 64.0 * 64.0;

    private final Plugin plugin;

    public BukkitEmoteAudio(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void playMusic(Player p, EmoteMusic m) {
        Key key = key(m);
        if (key == null) {
            return;
        }
        p.getWorld().playSound(Sound.sound(key, Sound.Source.RECORD, m.volume(), 1f), p);
    }

    @Override
    public void stopMusic(Player p, EmoteMusic m) {
        Key key = key(m);
        if (key == null) {
            return;
        }
        SoundStop stop = SoundStop.namedOnSource(key, Sound.Source.RECORD);
        for (Player other : p.getWorld().getPlayers()) {
            if (other.equals(p) || other.getLocation().distanceSquared(p.getLocation()) <= STOP_RADIUS_SQUARED) {
                other.stopSound(stop);
            }
        }
    }

    @Override
    public Cancellable later(Runnable r, long ticks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, r, ticks);
        return task::cancel;
    }

    /** null si la clave no es válida: se salta la música en vez de romper el emote. */
    private Key key(EmoteMusic m) {
        try {
            return Key.key(m.sound());
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Música con clave de sonido inválida: «" + m.sound() + "».");
            return null;
        }
    }
}
