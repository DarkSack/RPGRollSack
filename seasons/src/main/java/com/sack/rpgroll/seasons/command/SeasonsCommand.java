package com.sack.rpgroll.seasons.command;

import com.sack.rpgroll.seasons.api.SeasonsAPI;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length >= 1 && args[0].equalsIgnoreCase("info")) {
            handleInfo(sender, args);
            return true;
        }

        sender.sendMessage(Component.text("Uso: /seasons info [mundo]", NamedTextColor.RED));
        return true;
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (!SeasonsAPI.isReady()) {
            sender.sendMessage(Component.text("RPGRoll-Seasons todavía no está listo.", NamedTextColor.RED));
            return;
        }

        World world;

        if (args.length >= 2) {
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(Component.text("No existe el mundo: " + args[1], NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            sender.sendMessage(Component.text("Especificá un mundo: /seasons info <mundo>", NamedTextColor.RED));
            return;
        }

        var seasonOpt = SeasonsAPI.get().getCurrentSeason(world);

        if (seasonOpt.isEmpty()) {
            sender.sendMessage(Component.text("No hay una estación activa configurada para " + world.getName() + ".",
                    NamedTextColor.GRAY));
            return;
        }

        var season = seasonOpt.get();

        sender.sendMessage(Component.text("Estación actual en " + world.getName() + ": ", NamedTextColor.GOLD)
                .append(Component.text(season.displayName(), NamedTextColor.YELLOW)));

        if (sender instanceof Player player) {
            double temperature = SeasonsAPI.get().getTemperature(player.getLocation());
            sender.sendMessage(Component.text(
                    String.format(Locale.ROOT, "Temperatura donde estás: %.1f°C", temperature), NamedTextColor.AQUA));
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
