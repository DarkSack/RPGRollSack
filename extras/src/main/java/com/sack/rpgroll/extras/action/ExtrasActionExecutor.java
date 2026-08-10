package com.sack.rpgroll.extras.action;

import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

/** Mismo patrón de ejecución de acciones ya establecido en Crates/Mobs/Items — MESSAGE/SOUND/PARTICLE/DAMAGE/COMMAND. */
public class ExtrasActionExecutor {

    private final Plugin plugin;

    public ExtrasActionExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, List<ExtrasAction> actions) {

        for (ExtrasAction action : actions) {
            executeOne(player, action);
        }
    }

    private void executeOne(Player player, ExtrasAction action) {

        switch (action.type()) {
            case MESSAGE -> executeMessage(player, action.value());
            case SOUND -> executeSound(player, action.value());
            case PARTICLE -> executeParticle(player, action.value());
            case DAMAGE -> executeDamage(player, action.value());
            case COMMAND -> executeCommand(player, action.value());
        }
    }

    private void executeMessage(Player player, String rawMessage) {
        player.sendMessage(ComponentUtils.parse(rawMessage.replace("{player}", player.getName())));
    }

    private void executeCommand(Player player, String rawCommand) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rawCommand.replace("{player}", player.getName()));
    }

    private void executeSound(Player player, String value) {

        String[] parts = value.split(",");

        Sound sound;
        try {
            sound = Sound.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ SOUND inválido: " + value);
            return;
        }

        float volume = parts.length >= 2 ? parseFloatOrDefault(parts[1], 1.0f) : 1.0f;
        float pitch = parts.length >= 3 ? parseFloatOrDefault(parts[2], 1.0f) : 1.0f;

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void executeParticle(Player player, String value) {

        String[] parts = value.split(",");

        org.bukkit.Particle particle;
        try {
            particle = org.bukkit.Particle.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ PARTICLE inválido: " + value);
            return;
        }

        int count = parts.length >= 2 ? parseIntOrDefault(parts[1], 10) : 10;

        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.3, 0.5, 0.3, 0.01);
    }

    private void executeDamage(Player player, String value) {
        player.damage(parseDoubleOrDefault(value, 1.0));
    }

    private float parseFloatOrDefault(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parseIntOrDefault(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDoubleOrDefault(String raw, double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
