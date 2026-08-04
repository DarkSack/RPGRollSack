package com.sack.rpgroll.fishing.command;

import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.gui.EncyclopediaGUI;
import com.sack.rpgroll.fishing.runtime.FishingProfileManager;
import com.sack.rpgroll.fishing.runtime.PlayerFishingProfile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /fishing encyclopedia|stats */
public class FishingCommand implements CommandExecutor {

    private final FishSpeciesManager speciesManager;
    private final FishingProfileManager profileManager;

    public FishingCommand(FishSpeciesManager speciesManager, FishingProfileManager profileManager) {
        this.speciesManager = speciesManager;
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "encyclopedia" -> new EncyclopediaGUI(player, speciesManager, profileManager).open();
            case "stats" -> handleStats(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Uso: /fishing <encyclopedia|stats>", NamedTextColor.RED));
    }

    private void handleStats(Player player) {

        PlayerFishingProfile profile = profileManager.getOrLoad(player);

        player.sendMessage(Component.text("Estadísticas de pesca:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Peces capturados: " + profile.totalCaught(), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Especies descubiertas: " + profile.allRecords().size() + "/"
                + speciesManager.count(), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Tesoros encontrados: " + profile.totalTreasures(), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Basura sacada: " + profile.totalJunk(), NamedTextColor.WHITE));
    }

}
