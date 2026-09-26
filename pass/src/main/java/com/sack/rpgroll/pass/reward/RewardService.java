package com.sack.rpgroll.pass.reward;

import com.sack.rpgroll.common.character.Characters;
import com.sack.rpgroll.common.integration.VaultEconomy;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Entrega recompensas. Llaves e ítems pasan por los comandos de consola de
 * RPGRoll-Crates y RPGRoll-Items para no depender de sus clases: si el módulo
 * no está, se avisa en consola y el resto de la recompensa se entrega igual.
 */
public class RewardService {

    private final Plugin plugin;
    private final LangManager lang;
    private final Logger logger;

    public RewardService(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.logger = plugin.getLogger();
    }

    public void grant(Player player, List<Reward> rewards) {
        for (Reward reward : rewards) {
            grant(player, reward);
        }
    }

    public void grant(Player player, Reward reward) {

        switch (reward.type()) {
            case MONEY -> giveMoney(player, reward.amount());
            case EXP -> giveExperience(player, reward.amount());
            case MATERIAL -> giveMaterial(player, Material.valueOf(reward.key()), reward.amount());
            case KEY -> console(player, "RPGRoll-Crates",
                    "crate givekey " + player.getName() + " " + reward.key() + " " + reward.amount());
            case ITEM -> console(player, "RPGRoll-Items",
                    "itemadmin give " + player.getName() + " " + reward.key() + " " + reward.amount());
            case COMMAND -> console(player, null, reward.key().replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString()));
        }
    }

    /** Una línea legible para lores y mensajes. */
    public Component describe(Reward reward) {

        return switch (reward.type()) {
            case MONEY -> lang.component("reward.money", "amount", reward.amount());
            case EXP -> lang.component("reward.exp", "amount", reward.amount());
            case KEY -> lang.component("reward.key", "amount", reward.amount(), "crate", reward.key());
            case ITEM -> lang.component("reward.item", "amount", reward.amount(), "item", reward.key());
            case MATERIAL -> lang.component("reward.material", "amount", reward.amount(),
                    "material", pretty(reward.key()));
            case COMMAND -> lang.component("reward.command");
        };
    }

    public Material icon(Reward reward) {

        return switch (reward.type()) {
            case MONEY -> Material.GOLD_INGOT;
            case EXP -> Material.EXPERIENCE_BOTTLE;
            case KEY -> Material.TRIPWIRE_HOOK;
            case ITEM -> Material.NETHER_STAR;
            case MATERIAL -> Material.valueOf(reward.key());
            case COMMAND -> Material.NAME_TAG;
        };
    }

    public Material icon(List<Reward> rewards, Material fallback) {
        return rewards.isEmpty() ? fallback : icon(rewards.get(0));
    }

    private void giveMoney(Player player, int amount) {

        VaultEconomy.get().ifPresentOrElse(
                economy -> economy.depositPlayer(player, amount),
                () -> logger.warning("Vault no tiene economía: " + player.getName() + " se quedó sin " + amount));
    }

    private void giveExperience(Player player, int amount) {

        Characters.get().ifPresent(characters -> characters.addExperience(player.getUniqueId(), amount));
    }

    private void giveMaterial(Player player, Material material, int amount) {

        int left = amount;

        while (left > 0) {
            int stack = Math.min(left, material.getMaxStackSize());
            player.getInventory().addItem(new ItemStack(material, stack)).values()
                    .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
            left -= stack;
        }
    }

    private void console(Player player, String requiredPlugin, String command) {

        if (requiredPlugin != null && !Bukkit.getPluginManager().isPluginEnabled(requiredPlugin)) {
            logger.warning(requiredPlugin + " no está activo; no se entregó '" + command + "' a " + player.getName());
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private static String pretty(String materialName) {
        String lower = materialName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

}
