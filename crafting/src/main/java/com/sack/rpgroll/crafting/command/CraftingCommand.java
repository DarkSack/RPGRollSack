package com.sack.rpgroll.crafting.command;

import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.gui.RecipeBookGUI;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CraftingCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("book", "discover");

    private final CustomRecipeManager recipeManager;
    private final CustomStationManager stationManager;
    private final DiscoveryService discoveryService;

    public CraftingCommand(CustomRecipeManager recipeManager, CustomStationManager stationManager,
            DiscoveryService discoveryService) {
        this.recipeManager = recipeManager;
        this.stationManager = stationManager;
        this.discoveryService = discoveryService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "book";

        switch (sub) {
            case "book" -> new RecipeBookGUI(player, recipeManager, stationManager, discoveryService).open();
            case "discover" -> sendDiscoveredCount(player);
            default -> player.sendMessage(Component.text("Subcomando desconocido.", NamedTextColor.RED));
        }

        return true;
    }

    private void sendDiscoveredCount(Player player) {

        int discovered = discoveryService.get(player.getUniqueId()).discoveredRecipeIds().size();
        int total = recipeManager.count();

        player.sendMessage(Component.text("Recetas descubiertas: " + discovered + "/" + total, NamedTextColor.AQUA));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        return List.of();
    }

}
