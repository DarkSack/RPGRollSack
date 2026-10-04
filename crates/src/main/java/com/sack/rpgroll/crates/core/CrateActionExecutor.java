package com.sack.rpgroll.crates.core;

import com.sack.rpgroll.crates.lucky.LuckyItems;
import com.sack.rpgroll.crates.lucky.LuckyManager;

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
    private final LuckyManager lucky;
    private final LuckyItems luckyItems;

    public CrateActionExecutor(Plugin plugin, LangManager lang, LuckyManager lucky, LuckyItems luckyItems) {
        this.plugin = plugin;
        this.lang = lang;
        this.lucky = lucky;
        this.luckyItems = luckyItems;
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
            case LUCKY_BLOCK -> executeLuckyBlock(player, action.value());
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

    private void executeLuckyBlock(Player player, String value) {

        String[] parts = value.split(",");
        var block = lucky.get(parts[0].trim().toLowerCase(java.util.Locale.ROOT));

        if (block.isEmpty()) {
            plugin.getLogger().warning("✘ LUCKY_BLOCK inválido en crate reward: " + value);
            return;
        }

        int amount = 1;
        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        while (amount > 0) {
            ItemStack item = luckyItems.create(block.get(), Math.min(amount, 64));
            amount -= item.getAmount();
            player.getInventory().addItem(item).values()
                    .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        }
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
