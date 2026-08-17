package com.sack.rpgroll.fishing.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.gui.EncyclopediaGUI;
import com.sack.rpgroll.fishing.runtime.FishingProfileManager;
import com.sack.rpgroll.fishing.runtime.PlayerFishingProfile;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** /fishing encyclopedia|stats */
public class FishingCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("encyclopedia", "stats");

    private final FishSpeciesManager speciesManager;
    private final FishingProfileManager profileManager;
    private final LangManager lang;

    public FishingCommand(FishSpeciesManager speciesManager, FishingProfileManager profileManager, LangManager lang) {
        this.speciesManager = speciesManager;
        this.profileManager = profileManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.player.players_only");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "encyclopedia" -> new EncyclopediaGUI(player, speciesManager, profileManager, lang).open();
            case "stats" -> handleStats(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        lang.send(player, "command.player.usage");
    }

    private void handleStats(Player player) {

        PlayerFishingProfile profile = profileManager.getOrLoad(player);

        lang.send(player, "command.player.stats_title");
        lang.send(player, "command.player.stats_caught", "count", profile.totalCaught());
        lang.send(player, "command.player.stats_species", "discovered", profile.allRecords().size(),
                "total", speciesManager.count());
        lang.send(player, "command.player.stats_treasures", "count", profile.totalTreasures());
        lang.send(player, "command.player.stats_junk", "count", profile.totalJunk());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        return List.of();
    }

}
