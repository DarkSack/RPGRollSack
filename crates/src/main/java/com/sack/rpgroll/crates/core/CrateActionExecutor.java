package com.sack.rpgroll.crates.core;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Ejecuta las acciones de una CrateReward sobre un jugador. Se llama una
 * única vez, cuando termina la animación de la ruleta.
 */
public class CrateActionExecutor {


    private final Plugin plugin;
    private final LangManager lang;

    public CrateActionExecutor(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void grant(Player player, CrateReward reward) {

        for (CrateAction action : reward.actions()) {
            executeOne(player, action);
        }

        if (reward.announceGlobally()) {
            Bukkit.broadcast(lang.component("executor.win_broadcast",
                    "player", player.getName(), "reward", reward.displayName()));
        }
    }

    private void executeOne(Player player, CrateAction action) {
        switch (action.type()) {
            case MESSAGE -> executeMessage(player, action.value());
            case COMMAND -> executeCommand(player, action.value());
            case GIVE_ITEM -> executeGiveItem(player, action.value());
            case SOUND -> executeSound(player, action.value());
        }
    }

    private void executeMessage(Player player, String rawMessage) {
        String parsed = rawMessage.replace("{player}", player.getName());
        player.sendMessage(ComponentUtils.parse(parsed));
    }

    private void executeCommand(Player player, String rawCommand) {
        String parsed = rawCommand.replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
    }

    private void executeGiveItem(Player player, String value) {

        String[] parts = value.split(",");

        Material material;

        try {
            material = Material.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ GIVE_ITEM inválido en crate reward: " + value);
            return;
        }

        int amount = 1;

        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack item = new ItemStack(material, amount);
        var leftover = player.getInventory().addItem(item);

        leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
    }

    private void executeSound(Player player, String value) {

        String[] parts = value.split(",");

        Sound sound;

        try {
            sound = Sound.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ SOUND inválido en crate reward: " + value);
            return;
        }

        float volume = parts.length >= 2 ? parseFloatOrDefault(parts[1], 1.0f) : 1.0f;
        float pitch = parts.length >= 3 ? parseFloatOrDefault(parts[2], 1.0f) : 1.0f;

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private float parseFloatOrDefault(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
