package com.sack.rpgroll.seasons.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.seasons.api.SeasonsAPI;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /seasons info [mundo] */
public class SeasonsCommand implements CommandExecutor, TabCompleter {

    private final LangManager lang;

    public SeasonsCommand(LangManager lang) {
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length >= 1 && args[0].equalsIgnoreCase("info")) {
            handleInfo(sender, args);
            return true;
        }

        lang.send(sender, "command.player.usage");
        return true;
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (!SeasonsAPI.isReady()) {
            lang.send(sender, "command.player.not_ready");
            return;
        }

        World world;

        if (args.length >= 2) {
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                lang.send(sender, "command.player.unknown_world", "world", args[1]);
                return;
            }
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            lang.send(sender, "command.player.specify_world");
            return;
        }

        var seasonOpt = SeasonsAPI.get().getCurrentSeason(world);

        if (seasonOpt.isEmpty()) {
            lang.send(sender, "command.player.no_active_season", "world", world.getName());
            return;
        }

        var season = seasonOpt.get();

        lang.send(sender, "command.player.current_season", "world", world.getName(), "season", season.displayName());

        if (sender instanceof Player player) {
            double temperature = SeasonsAPI.get().getTemperature(player.getLocation());
            lang.send(sender, "command.player.temperature", "temperature",
                    String.format(Locale.ROOT, "%.1f", temperature));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], List.of("info"));
        }

        if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.worldNames(args[1]);
        }

        return List.of();
    }

}
